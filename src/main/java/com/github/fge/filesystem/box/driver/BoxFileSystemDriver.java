package com.github.fge.filesystem.box.driver;

import com.box.sdk.BoxAPIException;
import com.box.sdk.BoxFile;
import com.box.sdk.BoxFolder;
import com.box.sdk.BoxItem;
import com.github.fge.filesystem.box.exceptions.BoxIOException;
import com.github.fge.filesystem.box.io.BoxFileInputStream;
import com.github.fge.filesystem.box.io.BoxFileOutputStream;
import com.github.fge.filesystem.driver.UnixLikeFileSystemDriverBase;
import com.github.fge.filesystem.exceptions.IsDirectoryException;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;

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
import java.nio.file.attribute.FileAttribute;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
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
        final FileSystemFactoryProvider factoryProvider,
        final BoxAPIWrapper wrapper)
    {
        super(fileStore, factoryProvider);
        this.wrapper = Objects.requireNonNull(wrapper);
    }

    @Nonnull
    @Override
    public InputStream newInputStream(final Path path,
        final Set<OpenOption> options)
        throws IOException
    {
        final Path realPath = path.toAbsolutePath();

        final BoxFile file = wrapper.getFile(realPath);

        return new BoxFileInputStream(executor, file);
    }

    @SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
    @Nonnull
    @Override
    public OutputStream newOutputStream(final Path path,
        final Set<OpenOption> options)
        throws IOException
    {
        final Path realPath = path.toAbsolutePath();

        final OutputStream ret;
        final String target = realPath.toString();
        final BoxItem item = wrapper.getItem(realPath);
        final boolean create = item == null;

        if (create) {
            // TODO: check; parent should always exist
            final Path parent = realPath.getParent();
            final BoxFolder folder = wrapper.getFolder(parent);
            ret = new BoxFileOutputStream(executor, folder,
                realPath.getFileName().toString());
        } else {
            if (isDirectory(item))
                throw new IsDirectoryException(target);
            ret = new BoxFileOutputStream(executor, asFile(item));
        }

        return ret;
    }

    @Nonnull
    @Override
    public DirectoryStream<Path> newDirectoryStream(final Path dir,
        final DirectoryStream.Filter<? super Path> filter)
        throws IOException
    {
        final Path realPath = dir.toAbsolutePath();
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

    @Override
    public void createDirectory(final Path dir, final FileAttribute<?>... attrs)
        throws IOException
    {
        final Path realPath = dir.toAbsolutePath();
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

    @Override
    public void delete(final Path path)
        throws IOException
    {
        wrapper.deleteItem(path.toAbsolutePath());
    }

    @Override
    public void copy(final Path source, final Path target,
        final Set<CopyOption> options)
        throws IOException
    {
        /*
         * Check source validity; it must exist (obviously) and must not be a
         * non empty directory.
         */
        final Path srcPath = source.toAbsolutePath();
        final String src = srcPath.toString();
        final BoxItem srcItem = wrapper.getItem(srcPath);

        // TODO! metadata driver, yes, again
        @SuppressWarnings("ConstantConditions")
        final boolean directory = isDirectory(srcItem);
        if (directory)
            if (!wrapper.folderIsEmpty(asDirectory(srcItem)))
                throw new DirectoryNotEmptyException(src);

        /*
         * Check destination validity; if it exists and we have not been
         * instructed to replace it, fail; if we _have_ been instructed to
         * replace it, check that it is either a file or a non empty directory.
         */
        final Path dstPath = target.toAbsolutePath();
        final String dst = dstPath.toString();
        final BoxItem dstItem = wrapper.getItem(dstPath);

        //noinspection VariableNotUsedInsideIf
        if (dstItem != null)
            wrapper.deleteItem(dstPath);

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
    // TODO: metadata driver! Again
    @Override
    public void move(final Path source, final Path target,
        final Set<CopyOption> options)
        throws IOException
    {
        /*
         * Check source validity; it must exist (obviously) and must not be a
         * non empty directory.
         */
        final Path srcPath = source.toAbsolutePath();
        final String src = srcPath.toString();
        final BoxItem srcItem = wrapper.getItem(srcPath);

        // TODO: within a driver, atomic move of non empty directories are OK
        @SuppressWarnings("ConstantConditions")
        final boolean directory = isDirectory(srcItem);
        if (directory)
            if (!wrapper.folderIsEmpty(asDirectory(srcItem)))
                throw new DirectoryNotEmptyException(src);

        /*
         * Check destination validity; if it exists and we have not been
         * instructed to replace it, fail; if we _have_ been instructed to
         * replace it, check that it is either a file or a non empty directory.
         */
        final Path dstPath = target.toAbsolutePath();
        final BoxItem dstItem = wrapper.getItem(dstPath);

        //noinspection VariableNotUsedInsideIf
        if (dstItem != null)
            wrapper.deleteItem(dstPath);

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

    @Override
    public void checkAccess(final Path path, final AccessMode... modes)
        throws IOException
    {
        final Path realPath = path.toAbsolutePath();
        final String s = realPath.toString();
        final BoxItem item = wrapper.getItem(realPath);

        if (item == null)
            throw new NoSuchFileException(s);

        final Set<AccessMode> set = EnumSet.noneOf(AccessMode.class);
        Collections.addAll(set, modes);

        // TODO: access handling, metadata driver
        if (isFile(item) && set.contains(AccessMode.EXECUTE))
            throw new AccessDeniedException(s);
    }

    @Nonnull
    @Override
    public BoxItem getPathMetadata(final Path path)
        throws IOException
    {
        // TODO: when symlinks are supported this may turn out to be wrong
        final Path target = path.toAbsolutePath();
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
