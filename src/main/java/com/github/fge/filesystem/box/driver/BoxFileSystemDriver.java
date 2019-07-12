package com.github.fge.filesystem.box.driver;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

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

import vavi.nio.file.Cache;
import vavi.nio.file.Util;
import vavi.util.Debug;

import static vavi.nio.file.Util.toFilenameString;
import static vavi.nio.file.Util.toPathString;

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

    private boolean ignoreAppleDouble = false;
    private final BoxFolder.Info rootInfo;

    public BoxFileSystemDriver(final FileStore fileStore,
        final FileSystemFactoryProvider factoryProvider,
        final BoxFolder.Info rootInfo,
        final Map<String, ?> env)
    {
        super(fileStore, factoryProvider);
        this.rootInfo = Objects.requireNonNull(rootInfo);
        ignoreAppleDouble = (Boolean) ((Map<String, Object>) env).getOrDefault("ignoreAppleDouble", Boolean.FALSE);
    }

    private static boolean isFolder(final BoxItem.Info entry)
    {
        return BoxFolder.Info.class.isInstance(entry);
    }

    private static BoxFolder.Info asFolder(final BoxItem.Info entry)
    {
        return BoxFolder.Info.class.cast(entry);
    }

    private static boolean isFile(final BoxItem.Info entry)
    {
        return BoxFile.Info.class.isInstance(entry);
    }

    private static BoxFile.Info asFile(final BoxItem.Info entry)
    {
        return BoxFile.Info.class.cast(entry);
    }

    /** */
    private Cache<BoxItem.Info> cache = new Cache<BoxItem.Info>() {
        /**
         * TODO when the parent is not cached
         * @see #ignoreAppleDouble
         * @throws NoSuchFileException must be thrown when the path is not found in this cache
         */
        public BoxItem.Info getEntry(Path path) throws IOException {
            if (cache.containsFile(path)) {
                return cache.getFile(path);
            } else {
                if (ignoreAppleDouble && path.getFileName() != null && Util.isAppleDouble(path)) {
                    throw new NoSuchFileException("ignore apple double file: " + path);
                }

                BoxItem.Info entry;
                if (path.getNameCount() == 0) {
                    entry = rootInfo;
                } else {
                    entry = getItem(path);
                }
                if (entry == null) {
                    cache.removeEntry(path);
                    throw new NoSuchFileException(path.toString());
                }
                cache.putFile(path, entry);
                return entry;
            }
        }

        BoxItem.Info getItem(final Path path) throws IOException {
            BoxItem.Info entry = null;
            for (int i = 0; i < path.getNameCount(); i++) {
                Path name = path.getName(i);
                Path sub = path.subpath(0, i + 1);
                Path parent = sub.getParent() != null ? sub.getParent() : path.getFileSystem().getPath("/");
                List<Path> bros;
                if (!containsFile(parent) || !containsFolder(parent)) {
                    bros = getDirectoryEntries(parent);
                } else {
                    bros = getFolder(parent);
                }
                Optional<Path> found = bros.stream().filter(p -> p.getFileName().equals(name)).findFirst();
                if (!found.isPresent()) {
                    return null;
                } else {
                    entry = getEntry(found.get());
                }
            }
            return entry;
        }
    };

    @Nonnull
    @Override
    public InputStream newInputStream(final Path path,
        final Set<? extends OpenOption> options)
        throws IOException
    {
        final BoxItem.Info entry = cache.getEntry(path);

        if (isFolder(entry)) {
            throw new IsDirectoryException(path.toString());
        }

        return new BoxFileInputStream(executor, asFile(entry).getResource());
    }

    @SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
    @Nonnull
    @Override
    public OutputStream newOutputStream(final Path path,
        final Set<? extends OpenOption> options)
        throws IOException
    {
        try {
            BoxItem.Info entry = cache.getEntry(path);

            if (isFolder(entry)) {
                throw new IsDirectoryException(path.toString());
            } else {
                // TODO accept?
                throw new FileAlreadyExistsException(path.toString());
            }
        } catch (NoSuchFileException e) {
Debug.println("newOutputStream: " + e.getMessage());
        }

        BoxItem.Info parentEntry = cache.getEntry(path.getParent());
        return new BoxFileOutputStream(executor, asFolder(parentEntry).getResource(), toFilenameString(path), newEntry -> {
            try {
                cache.addEntry(path, newEntry);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    @Nonnull
    @Override
    public DirectoryStream<Path> newDirectoryStream(final Path dir,
        final DirectoryStream.Filter<? super Path> filter)
        throws IOException
    {
        return Util.newDirectoryStream(getDirectoryEntries(dir));
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path,
                                              Set<? extends OpenOption> options,
                                              FileAttribute<?>... attrs) throws IOException {
        if (options.contains(StandardOpenOption.WRITE) || options.contains(StandardOpenOption.APPEND)) {
            return new Util.SeekableByteChannelForWriting(newOutputStream(path, options)) {
                @Override
                protected long getLeftOver() throws IOException {
                    long leftover = 0;
                    if (options.contains(StandardOpenOption.APPEND)) {
                        BoxItem.Info entry = cache.getEntry(path);
                        if (entry != null && entry.getSize() >= 0) {
                            leftover = entry.getSize();
                        }
                    }
                    return leftover;
                }

                @Override
                public void close() throws IOException {
System.out.println("SeekableByteChannelForWriting::close");
                    if (written == 0) {
                        // TODO no mean
System.out.println("SeekableByteChannelForWriting::close: scpecial: " + path);
                        java.io.File file = new java.io.File(toPathString(path));
                        FileInputStream fis = new FileInputStream(file);
                        FileChannel fc = fis.getChannel();
                        fc.transferTo(0, file.length(), this);
                        fis.close();
                    }
                    super.close();
                }
            };
        } else {
            BoxItem.Info entry = cache.getEntry(path);
            if (isFolder(entry)) {
                throw new NoSuchFileException(path.toString());
            }
            return new Util.SeekableByteChannelForReading(newInputStream(path, null)) {
                @Override
                protected long getSize() throws IOException {
                    return entry.getSize();
                }
            };
        }
    }

    @Override
    public void createDirectory(final Path dir, final FileAttribute<?>... attrs)
        throws IOException
    {
        try {
            final BoxItem.Info parentEntry = cache.getEntry(dir.getParent());
            BoxItem.Info newEntry = asFolder(parentEntry).getResource().createFolder(toFilenameString(dir));
            cache.addEntry(dir, newEntry);
        } catch (BoxAPIException e) {
            throw BoxIOException.wrap(e);
        }
    }

    @Override
    public void delete(final Path path)
        throws IOException
    {
        removeEntry(path);
    }

    @Override
    public void copy(final Path source, final Path target,
        final Set<CopyOption> options)
        throws IOException
    {
        if (cache.existsEntry(target)) {
            if (options.stream().anyMatch(o -> o.equals(StandardCopyOption.REPLACE_EXISTING))) {
                removeEntry(target);
            } else {
                throw new FileAlreadyExistsException(target.toString());
            }
        }
        copyEntry(source, target);
    }

    /**
     * Move a file, or empty directory, from one path to another on this
     * file system
     *
     * @param source the source path
     * @param target the target path
     * @param options the copy options
     * @throws IOException file system level error, or a plain I/O error
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
        if (cache.existsEntry(target)) {
            if (isFolder(cache.getEntry(target))) {
                if (options.stream().anyMatch(o -> o.equals(StandardCopyOption.REPLACE_EXISTING))) {
                    // replace the target
                    if (cache.getChildCount(target) > 0) {
                        throw new DirectoryNotEmptyException(target.toString());
                    } else {
                        removeEntry(target);
                        moveEntry(source, target, false);
                    }
                } else {
                    // move into the target
                    moveEntry(source, target, true);
                }
            } else {
                if (options.stream().anyMatch(o -> o.equals(StandardCopyOption.REPLACE_EXISTING))) {
                    removeEntry(target);
                    moveEntry(source, target, false);
                } else {
                    throw new FileAlreadyExistsException(target.toString());
                }
            }
        } else {
            if (source.getParent().equals(target.getParent())) {
                // rename
                renameEntry(source, target);
            } else {
                moveEntry(source, target, false);
            }
        }
    }

    @Override
    public void checkAccess(final Path path, final AccessMode... modes)
        throws IOException
    {
        final BoxItem.Info entry = cache.getEntry(path);

        final Set<AccessMode> set = EnumSet.noneOf(AccessMode.class);

        if (!isFile(entry)) {
            return;
        } else {
            EnumSet<BoxFile.Permission> permissions = asFile(entry).getPermissions();
            for (AccessMode mode : modes) {
                switch (mode) {
                case READ:
                    if (!permissions.contains(BoxFile.Permission.CAN_DOWNLOAD)) {
                        set.add(AccessMode.READ);
                    }
                    break;
                case WRITE:
                    if (!permissions.contains(BoxFile.Permission.CAN_UPLOAD)) {
                        set.add(AccessMode.WRITE);
                    }
                    break;
                case EXECUTE:
                    if (!permissions.contains(BoxFile.Permission.CAN_DOWNLOAD)) {
                        set.add(AccessMode.EXECUTE);
                    }
                    break;
                }
            }
        }

        if (set.size() > 0) {
            throw new AccessDeniedException(path.toString() + ": " + set);
        }
    }

    @Nonnull
    @Override
    public BoxItem.Info getPathMetadata(final Path path)
        throws IOException
    {
        // TODO: when symlinks are supported this may turn out to be wrong
        return cache.getEntry(path);
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

    /** */
    private List<Path> getDirectoryEntries(Path dir) throws IOException {
System.out.println("getDirectoryEntries: " + dir);
        final BoxItem.Info entry = cache.getEntry(dir);

        if (!isFolder(entry)) {
//System.err.println(entry.name + ", " + entry.id + ", " + entry.hashCode());
            throw new NotDirectoryException(dir.toString());
        }

        List<Path> list = null;
        if (cache.containsFolder(dir)) {
            list = cache.getFolder(dir);
        } else {
            list = new ArrayList<>();

            for (final BoxItem.Info childEntry : asFolder(entry).getResource().getChildren("name", "size", "created_at", "modified_at", "permissions")) {
                Path childPath = dir.resolve(childEntry.getName());
                list.add(childPath);
                cache.putFile(childPath, childEntry);
            }
            cache.putFolder(dir, list);
        }

        return list;
    }

    /** */
    private void removeEntry(Path path) throws IOException {
        BoxItem.Info entry = cache.getEntry(path);
        if (isFolder(entry)) {
            if (cache.getChildCount(path) > 0) {
                throw new DirectoryNotEmptyException(path.toString());
            }
            asFolder(entry).getResource().delete(false);
        } else {
            asFile(entry).getResource().delete();
        }

        cache.removeEntry(path);
    }

    /** */
    private void copyEntry(final Path source, final Path target) throws IOException {
        BoxItem.Info sourceEntry = cache.getEntry(source);
        if (isFile(sourceEntry)) {
            BoxItem.Info parentEntry = cache.getEntry(source.getParent());
            BoxItem.Info newEntry = asFile(sourceEntry).getResource().copy(asFolder(parentEntry).getResource(), toFilenameString(target));
            cache.addEntry(target, newEntry);
        } else if (isFolder(sourceEntry)) {
            // TODO java spec. allows empty folder
            throw new IsDirectoryException("source can not be a folder: " + source);
        }
    }

    /**
     * @param targetIsParent if the target is folder
     */
    private void moveEntry(final Path source, final Path target, boolean targetIsParent) throws IOException {
        BoxItem.Info sourceEntry = cache.getEntry(source);
        if (isFile(sourceEntry)) {
            BoxItem.Info parentEntry = cache.getEntry(targetIsParent ? target : target.getParent());
            BoxItem.Info patchedEntry;
            if (targetIsParent) {
                patchedEntry = asFile(sourceEntry).getResource().move(asFolder(parentEntry).getResource());
            } else {
                patchedEntry = asFile(sourceEntry).getResource().move(asFolder(parentEntry).getResource(), toFilenameString(target));
            }
            cache.removeEntry(source);
            if (targetIsParent) {
                cache.addEntry(target.resolve(source.getFileName()), patchedEntry);
            } else {
                cache.addEntry(target, patchedEntry);
            }
        } else if (isFolder(sourceEntry)) {
            // TODO java spec. allows empty folder
            throw new IsDirectoryException("source can not be a folder: " + source);
        }
    }

    /** */
    private void renameEntry(final Path source, final Path target) throws IOException {
        BoxItem.Info sourceEntry = cache.getEntry(source);
//Debug.println(sourceEntry.id + ", " + sourceEntry.name);

        BoxItem.Info parentEntry = cache.getEntry(target.getParent());
        BoxItem.Info patchedEntry = asFile(sourceEntry).getResource().move(asFolder(parentEntry).getResource(), toFilenameString(target));
        cache.removeEntry(source);
        cache.addEntry(target, patchedEntry);
    }
}
