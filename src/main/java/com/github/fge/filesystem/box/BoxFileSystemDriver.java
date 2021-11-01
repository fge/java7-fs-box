package com.github.fge.filesystem.box;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.FileStore;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.ParametersAreNonnullByDefault;

import com.box.sdk.BoxAPIRequest;
import com.box.sdk.BoxAPIResponse;
import com.box.sdk.BoxFile;
import com.box.sdk.BoxFolder;
import com.box.sdk.BoxItem;
import com.box.sdk.UploadFileCallback;
import com.github.fge.filesystem.driver.CachedFileSystemDriverBase;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;

import vavi.nio.file.Util;
import vavi.util.Debug;

import static vavi.nio.file.Util.toFilenameString;

/**
 * Box filesystem driver
 *
 * @version 0.00 2021/10/31 umjammer update <br>
 */
@ParametersAreNonnullByDefault
public final class BoxFileSystemDriver
    extends CachedFileSystemDriverBase<BoxItem.Info> {

    private final BoxFolder.Info rootInfo;

    @SuppressWarnings("unchecked")
    public BoxFileSystemDriver(final FileStore fileStore,
        final FileSystemFactoryProvider factoryProvider,
        final BoxFolder.Info rootInfo,
        final Map<String, ?> env) {

    	super(fileStore, factoryProvider);
        this.rootInfo = Objects.requireNonNull(rootInfo);
        ignoreAppleDouble = (Boolean) ((Map<String, Object>) env).getOrDefault("ignoreAppleDouble", Boolean.FALSE);
    }

    @Override
    protected boolean isFolder(BoxItem.Info entry) {
        return BoxFolder.Info.class.isInstance(entry);
    }

    private static BoxFolder.Info asFolder(BoxItem.Info entry) {
        return BoxFolder.Info.class.cast(entry);
    }

    private static boolean isFile(BoxItem.Info entry) {
        return BoxFile.Info.class.isInstance(entry);
    }

    private static BoxFile.Info asFile(BoxItem.Info entry) {
        return BoxFile.Info.class.cast(entry);
    }

    @Override
    protected String getFilenameString(BoxItem.Info entry) throws IOException {
    	return entry.getName();
    }

    @Override
    protected BoxItem.Info getRootEntry() throws IOException {
    	return rootInfo;
    }

    @Override
    protected BoxItem.Info getEntry(BoxItem.Info dirEntry, Path path)throws IOException {
Debug.println(Level.FINE, dirEntry.getName() + ", " + path);
    	BoxItem.Info entry = null;
        for (int i = 0; i < path.getNameCount(); i++) {
            Path name = path.getName(i);
            Path sub = path.subpath(0, i + 1);
            Path parent = sub.getParent() != null ? sub.getParent() : path.getFileSystem().getPath("/");
            List<Path> bros = getDirectoryEntries(parent, false);
            Optional<Path> found = bros.stream().filter(p -> p.getFileName().equals(name)).findFirst();
            if (!found.isPresent()) {
                return null;
            } else {
                entry = cache.getFile(found.get()); // TODO not hidden...
            }
        }
        return entry;
    }

    @Override
    protected InputStream downloadEntry(BoxItem.Info entry, Path path, Set<? extends OpenOption> options) throws IOException {
        BoxFile file = asFile(entry).getResource();
        URL url = BoxFile.CONTENT_URL_TEMPLATE.build(file.getAPI().getBaseURL(), file.getID());
        BoxAPIRequest request = new BoxAPIRequest(file.getAPI(), url, "GET");
        BoxAPIResponse response = request.send();
        return new BufferedInputStream(new Util.InputStreamForDownloading(response.getBody(null), false) {
            @Override
            protected void onClosed() throws IOException {
                response.disconnect();
            }
        }, Util.BUFFER_SIZE);
    }

    @Override
    protected OutputStream uploadEntry(BoxItem.Info parentEntry, Path path, Set<? extends OpenOption> options) throws IOException {
        return new BufferedOutputStream(new Util.StealingOutputStreamForUploading<BoxItem.Info>() {
            @Override
            protected BoxItem.Info upload() throws IOException {
                UploadFileCallback callback = new UploadFileCallback() {
                    @Override
                    public void writeToStream(OutputStream os) throws IOException {
                        setOutputStream(os);
                    }
                };
                BoxFolder parent = asFolder(parentEntry).getResource();
                return parent.uploadFile(callback, toFilenameString(path));
            }

            @Override
            protected void onClosed(BoxItem.Info newEntry) {
                cache.addEntry(path, newEntry);
            }
        }, Util.BUFFER_SIZE);
    }

    @Override
    protected BoxItem.Info createDirectoryEntry(Path dir) throws IOException {
        BoxItem.Info parentEntry = cache.getEntry(dir.getParent());
        return asFolder(parentEntry).getResource().createFolder(toFilenameString(dir));
    }

    // TODO separate sub method or not?
    @Override
    protected void checkAccessImpl(final Path path, final AccessMode... modes)
        throws IOException
    {
        final BoxItem.Info entry = cache.getEntry(path);

        final Set<AccessMode> set = EnumSet.noneOf(AccessMode.class);

        if (!isFile(entry)) {
            return;
        } else {
            EnumSet<BoxFile.Permission> permissions = asFile(entry).getPermissions();
            if (permissions != null) {
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
        }

        if (set.size() > 0) {
            throw new AccessDeniedException(path + ": " + set);
        }
    }

    @Override
    protected List<BoxItem.Info> getDirectoryEntries(BoxItem.Info dirEntry, Path dir) throws IOException {
Debug.println(Level.FINE, dirEntry.getName());
    	Iterable<BoxItem.Info> i = asFolder(dirEntry).getResource().getChildren("name", "size", "created_at", "modified_at", "permissions");
        return StreamSupport.stream(i.spliterator(), false).collect(Collectors.toList()); 
    }

    @Override
    protected boolean hasChildren(BoxItem.Info dirEntry, Path dir) throws IOException {
    	return getDirectoryEntries(dir, false).size() > 0;
    }

    @Override
    protected void removeEntry(BoxItem.Info entry, Path path) throws IOException {
        if (isFolder(entry)) {
            asFolder(entry).getResource().delete(false);
        } else {
            asFile(entry).getResource().delete();
        }
    }

    @Override
    protected BoxItem.Info copyEntry(BoxItem.Info sourceEntry, BoxItem.Info targetParentEntry, Path source, Path target, Set<CopyOption> options) throws IOException {
        return asFile(sourceEntry).getResource().copy(asFolder(targetParentEntry).getResource(), toFilenameString(target));
    }

    @Override
    protected BoxItem.Info moveEntry(BoxItem.Info sourceEntry, BoxItem.Info targetParentEntry, Path source, Path target, boolean targetIsParent) throws IOException {
        if (targetIsParent) {
            return asFile(sourceEntry).getResource().move(asFolder(targetParentEntry).getResource());
        } else {
            return asFile(sourceEntry).getResource().move(asFolder(targetParentEntry).getResource(), toFilenameString(target));
        }
    }

    @Override
    protected BoxItem.Info moveFolderEntry(BoxItem.Info sourceEntry, BoxItem.Info targetParentEntry, Path source, Path target, boolean targetIsParent) throws IOException {
        BoxItem.Info patchedEntry = asFolder(sourceEntry).getResource().move(asFolder(targetParentEntry).getResource(), toFilenameString(target));
Debug.println(patchedEntry.getID() + ", " + patchedEntry.getParent().getName() + "/" + patchedEntry.getName());
		return patchedEntry;
    }

    @Override
    protected BoxItem.Info renameEntry(BoxItem.Info sourceEntry, BoxItem.Info targetParentEntry, Path source, Path target) throws IOException {
    	return asFile(sourceEntry).getResource().move(asFolder(targetParentEntry).getResource(), toFilenameString(target));
    }
}
