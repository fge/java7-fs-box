package com.github.fge.filesystem.box.driver;

import com.box.sdk.BoxAPIException;
import com.box.sdk.BoxFile;
import com.box.sdk.BoxFolder;
import com.box.sdk.BoxItem;
import com.github.fge.filesystem.attributes.FileAttributesFactory;
import com.github.fge.filesystem.box.exceptions.BoxIOException;
import com.github.fge.filesystem.box.io.BoxFileInputStream;
import com.github.fge.filesystem.box.io.BoxFileOutputStream;
import com.github.fge.filesystem.driver.UnixLikeFileSystemDriverBase;
import com.github.fge.filesystem.exceptions.IsDirectoryException;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Box filesystem driver
 *
 */
@SuppressWarnings("OverloadedVarargsMethod")
@ParametersAreNonnullByDefault
public final class BoxFileSystemDriver
    extends UnixLikeFileSystemDriverBase
{
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private final BoxAPIWrapper wrapper;

    public BoxFileSystemDriver(final FileStore fileStore,
        final FileAttributesFactory attributesFactory,
        final BoxAPIWrapper wrapper)
    {
        super(fileStore, attributesFactory);
        this.wrapper = Objects.requireNonNull(wrapper);
    }

    /**
     * Obtain a new {@link InputStream} from a path for this filesystem
     *
     * @param path the path
     * @param options the set of open options
     * @return a new input stream
     *
     * @throws IOException filesystem level error, or plain I/O error
     * @see FileSystemProvider#newInputStream(Path, OpenOption...)
     */
    @Nonnull
    @Override
    public InputStream newInputStream(final Path path,
        final OpenOption... options)
        throws IOException
    {
        final Path realPath = path.toRealPath();

        final BoxFile file = wrapper.getFile(realPath);

        return new BoxFileInputStream(executor, file);
    }

    /**
     * Obtain a new {@link OutputStream} from a path for this filesystem
     *
     * @param path the path
     * @param options the set of open options
     * @return a new output stream
     *
     * @throws IOException filesystem level error, or plain I/O error
     * @see FileSystemProvider#newOutputStream(Path, OpenOption...)
     */
    @SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
    @Nonnull
    @Override
    public OutputStream newOutputStream(final Path path,
        final OpenOption... options)
        throws IOException
    {
        final Set<OpenOption> set = new HashSet<>();
        Collections.addAll(set, options);

        if (set.contains(StandardOpenOption.DELETE_ON_CLOSE))
            throw new UnsupportedOperationException();
        if (set.contains(StandardOpenOption.APPEND))
            throw new UnsupportedOperationException();

        final Path realPath = path.toRealPath();

        final OutputStream ret;
        final String target = realPath.toString();
        final BoxItem item = wrapper.getItem(realPath);
        final boolean create = item == null;

        if (create) {
            if (!set.contains(StandardOpenOption.CREATE_NEW))
                throw new NoSuchFileException(target);
            // TODO: check; parent should always exist
            final Path parent = realPath.getParent();
            final BoxFolder folder = wrapper.getFolder(parent);
            ret = new BoxFileOutputStream(executor, folder,
                realPath.getFileName().toString());
        } else {
            if (set.contains(StandardOpenOption.CREATE_NEW))
                throw new FileAlreadyExistsException(target);
            if (isDirectory(item))
                throw new IsDirectoryException(target);
            ret = new BoxFileOutputStream(executor, asFile(item));
        }

        return ret;
    }

    /**
     * Create a new directory stream from a path for this filesystem
     *
     * @param dir the directory
     * @param filter a directory entry filter
     * @return a directory stream
     *
     * @throws IOException filesystem level error, or a plain I/O error
     * @see FileSystemProvider#newDirectoryStream(Path, DirectoryStream.Filter)
     */
    @Nonnull
    @Override
    public DirectoryStream<Path> newDirectoryStream(final Path dir,
        final DirectoryStream.Filter<? super Path> filter)
        throws IOException
    {
        final Path realPath = dir.toRealPath();
        final BoxFolder folder = wrapper.getFolder(realPath);

        /*
         * TODO! Find a better way...
         *
         * The problem is that a BoxFolder's .getChildren() will do pagination
         * by itself; and this may fail with a BoxAPIException. We don't want
         * to throw that from within an Iterator, we therefore swallow
         * everything :/
         */
        final List<Path> list = new ArrayList<>();
        try {
            for (final BoxItem.Info info : folder.getChildren("name"))
                list.add(dir.resolve(info.getName()));
        } catch (BoxAPIException e) {
            throw BoxIOException.wrap(e);
        }

        //noinspection AnonymousInnerClassWithTooManyMethods
        return new DirectoryStream<Path>()
        {
            @Override
            public Iterator<Path> iterator()
            {
                return list.iterator();
            }

            @Override
            public void close()
                throws IOException
            {
            }
        };
    }

    /**
     * Create a new directory from a path on this filesystem
     *
     * @param dir the directory to create
     * @param attrs the attributes with which the directory should be created
     * @throws IOException filesystem level error, or a plain I/O error
     * @see FileSystemProvider#createDirectory(Path, FileAttribute[])
     */
    @Override
    public void createDirectory(final Path dir, final FileAttribute<?>... attrs)
        throws IOException
    {
        if (attrs.length != 0)
            throw new UnsupportedOperationException();
        final Path realPath = dir.toRealPath();
        final BoxItem item = wrapper.getItem(realPath);

        //noinspection VariableNotUsedInsideIf
        if (item != null)
            throw new FileAlreadyExistsException(dir.toString());

        final Path parent = realPath.getParent();
        final BoxFolder folder = wrapper.getFolder(parent);
        final String name = realPath.getFileName().toString();

        try {
            folder.createFolder(name);
        } catch (BoxAPIException e) {
            throw BoxIOException.wrap(e);
        }
    }

    /**
     * Delete a file, or empty directory, matching a path on this filesystem
     *
     * @param path the victim
     * @throws IOException filesystem level error, or a plain I/O error
     * @see FileSystemProvider#delete(Path)
     */
    @Override
    public void delete(final Path path)
        throws IOException
    {
        wrapper.deleteItem(path.toRealPath());
    }

    /**
     * Copy a file, or empty directory, from one path to another on this
     * filesystem
     *
     * @param source the source path
     * @param target the target path
     * @param options the copy options
     * @throws IOException filesystem level error, or a plain I/O error
     * @see FileSystemProvider#copy(Path, Path, CopyOption...)
     */
    @Override
    public void copy(final Path source, final Path target,
        final CopyOption... options)
        throws IOException
    {
        final Set<CopyOption> set = new HashSet<>();
        Collections.addAll(set, options);

        // TODO!
        if (set.contains(StandardCopyOption.COPY_ATTRIBUTES))
            throw new UnsupportedOperationException();

        /*
         * Check source validity; it must exist (obviously) and must not be a
         * non empty directory.
         */
        final Path srcPath = source.toRealPath();
        final String src = srcPath.toString();
        final BoxItem srcItem = wrapper.getItem(srcPath);

        if (srcItem == null)
            throw new NoSuchFileException(src);

        final boolean directory = isDirectory(srcItem);
        if (directory)
            if (!wrapper.folderIsEmpty(asDirectory(srcItem)))
                throw new DirectoryNotEmptyException(src);

        /*
         * Check destination validity; if it exists and we have not been
         * instructed to replace it, fail; if we _have_ been instructed to
         * replace it, check that it is either a file or a non empty directory.
         */
        final Path dstPath = target.toRealPath();
        final String dst = dstPath.toString();
        final BoxItem dstItem = wrapper.getItem(dstPath);

        //noinspection VariableNotUsedInsideIf
        if (dstItem != null) {
            if (!set.contains(StandardCopyOption.REPLACE_EXISTING))
                throw new FileAlreadyExistsException(dst);
            wrapper.deleteItem(dstPath);
        }

        // TODO: not checked whether dstPath is / here
        final BoxFolder parent = wrapper.getFolder(dstPath.getParent());
        final String name = dstPath.getFileName().toString();
        try {
            if (directory) {
                parent.createFolder(name);
            }
            else
                asFile(srcItem).copy(parent, name);
        } catch (BoxAPIException e) {
            throw BoxIOException.wrap(e);
        }
    }

    /**
     * Move a file, or empty directory, from one path to another on this
     * filesystem
     *
     * @param source the source path
     * @param target the target path
     * @param options the copy options
     * @throws IOException filesystem level error, or a plain I/O error
     * @see FileSystemProvider#move(Path, Path, CopyOption...)
     */
    // TODO: factorize code
    // TODO: far from being optimized
    @Override
    public void move(final Path source, final Path target,
        final CopyOption... options)
        throws IOException
    {
        final Set<CopyOption> set = new HashSet<>();
        Collections.addAll(set, options);

        // TODO!
        if (set.contains(StandardCopyOption.COPY_ATTRIBUTES))
            throw new UnsupportedOperationException();

        /*
         * Check source validity; it must exist (obviously) and must not be a
         * non empty directory.
         */
        final Path srcPath = source.toRealPath();
        final String src = srcPath.toString();
        final BoxItem srcItem = wrapper.getItem(srcPath);

        if (srcItem == null)
            throw new NoSuchFileException(src);

        final boolean directory = isDirectory(srcItem);
        if (directory)
            if (!wrapper.folderIsEmpty(asDirectory(srcItem)))
                throw new DirectoryNotEmptyException(src);

        /*
         * Check destination validity; if it exists and we have not been
         * instructed to replace it, fail; if we _have_ been instructed to
         * replace it, check that it is either a file or a non empty directory.
         */
        final Path dstPath = target.toRealPath();
        final String dst = dstPath.toString();
        final BoxItem dstItem = wrapper.getItem(dstPath);

        //noinspection VariableNotUsedInsideIf
        if (dstItem != null) {
            if (!set.contains(StandardCopyOption.REPLACE_EXISTING))
                throw new FileAlreadyExistsException(dst);
            wrapper.deleteItem(dstPath);
        }

        // TODO: not checked whether dstPath is / here
        final BoxFolder parent = wrapper.getFolder(dstPath.getParent());
        final String name = dstPath.getFileName().toString();
        try {
            if (directory) {
                parent.createFolder(name);
            } else {
                asFile(srcItem).copy(parent, name);
            }
            // This is the only line which is no in .copy(). Meh.
            wrapper.deleteItem(srcPath);
        } catch (BoxAPIException e) {
            throw BoxIOException.wrap(e);
        }
    }

    /**
     * Check access modes for a path on this filesystem
     * <p>If no modes are provided to check for, this simply checks for the
     * existence of the path.</p>
     *
     * @param path the path to check
     * @param modes the modes to check for, if any
     * @throws IOException filesystem level error, or a plain I/O error
     * @see FileSystemProvider#checkAccess(Path, AccessMode...)
     */
    @Override
    public void checkAccess(final Path path, final AccessMode... modes)
        throws IOException
    {
        final Path realPath = path.toRealPath();
        final String s = realPath.toString();
        final BoxItem item = wrapper.getItem(realPath);

        if (item == null)
            throw new NoSuchFileException(s);
        final Set<AccessMode> set = EnumSet.noneOf(AccessMode.class);
        Collections.addAll(set, modes);

        // TODO: access handling... But for now...
        if (isFile(item) && set.contains(AccessMode.EXECUTE))
            throw new AccessDeniedException(s);
    }

    @Nonnull
    @Override
    public BoxItem getPathMetadata(final Path path)
        throws IOException
    {
        // TODO: when symlinks are supported this may turn out to be wrong
        final Path target = path.toRealPath();
        final BoxItem item = wrapper.getItem(target);
        if (item == null)
            throw new NoSuchFileException(target.toString());
        return item;
    }

    /**
     * Closes this stream and releases any system resources associated
     * with it. If the stream is already closed then invoking this
     * method has no effect.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close()
        throws IOException
    {
        // TODO! Is there anything to be done here?
    }

    private static boolean isDirectory(final BoxItem item)
    {
        return item instanceof BoxFolder;
    }

    private static BoxFolder asDirectory(final BoxItem item)
    {
        return (BoxFolder) item;
    }

    private static boolean isFile(final BoxItem item)
    {
        return item instanceof BoxFile;
    }

    private static BoxFile asFile(final BoxItem item)
    {
        return (BoxFile) item;
    }
}
