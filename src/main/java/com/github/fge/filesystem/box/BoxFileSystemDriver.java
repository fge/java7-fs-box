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
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchService;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
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
import com.github.fge.filesystem.driver.CachedFileSystemDriver;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;

import vavi.nio.file.Util;
import vavi.util.Debug;

import static com.github.fge.filesystem.box.BoxFileSystemProvider.ENV_USE_SYSTEM_WATCHER;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static vavi.nio.file.Util.toFilenameString;;


/**
 * Box filesystem driver
 *
 * @version 0.00 2021/10/31 umjammer update <br>
 */
@ParametersAreNonnullByDefault
public final class BoxFileSystemDriver
    extends CachedFileSystemDriver<BoxItem.Info> {

    private BoxWatchService systemWatcher;
    private BoxFolder.Info rootInfo;

    public BoxFileSystemDriver(final FileStore fileStore,
        FileSystemFactoryProvider factoryProvider,
        BoxFolder.Info rootInfo,
        Map<String, ?> env) throws IOException {

        super(fileStore, factoryProvider);
        this.rootInfo = Objects.requireNonNull(rootInfo);
        setEnv(env);

        @SuppressWarnings("unchecked")
        boolean useSystemWatcher = (Boolean) ((Map<String, Object>) env).getOrDefault(ENV_USE_SYSTEM_WATCHER, false);
        if (useSystemWatcher) {
            systemWatcher = new BoxWatchService(rootInfo);
            systemWatcher.setNotificationListener(this::processNotification);
        }
    }

    /** for system watcher */
    private void processNotification(String id, Kind<?> kind) {
        if (ENTRY_DELETE == kind) {
            try {
                Path path = cache.getEntry(e -> id.equals(e.getID()));
                cache.removeEntry(path);
            } catch (NoSuchElementException e) {
Debug.println("NOTIFICATION: already deleted: " + id);
            }
        } else {
            try {
                try {
                    Path path = cache.getEntry(e -> id.equals(e.getID()));
Debug.println("NOTIFICATION: maybe updated: " + path);
                    cache.removeEntry(path);
                    cache.getEntry(path);
                } catch (NoSuchElementException e) {
// TODO impl
//                    BoxItem.Info entry = BoxFile.client.files().getMetadata(pathString);
//                    Path path = parent.resolve(pathString);
//Debug.println("NOTIFICATION: maybe created: " + path);
//                    cache.addEntry(path, entry);
                }
            } catch (NoSuchElementException e) {
Debug.println("NOTIFICATION: parent not found: " + e);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /** */
    private static final String[] ENTRY_FIELDS = { "name", "size", "created_at", "modified_at", "permissions" };

    private static BoxFolder.Info asFolder(BoxItem.Info entry) {
        return BoxFolder.Info.class.cast(entry);
    }

    private static BoxFile.Info asFile(BoxItem.Info entry) {
        return BoxFile.Info.class.cast(entry);
    }

    @Override
    protected String getFilenameString(BoxItem.Info entry) {
        return entry.getName();
    }

    @Override
    protected boolean isFolder(BoxItem.Info entry) {
        return BoxFolder.Info.class.isInstance(entry);
    }

    @Override
    protected BoxItem.Info getRootEntry(Path root) throws IOException {
        return rootInfo;
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
                updateEntry(path, newEntry);
            }
        }, Util.BUFFER_SIZE);
    }

    @Override
    protected List<BoxItem.Info> getDirectoryEntries(BoxItem.Info dirEntry, Path dir) throws IOException {
Debug.println(Level.FINE, dirEntry.getName());
        Iterable<BoxItem.Info> i = asFolder(dirEntry).getResource().getChildren(ENTRY_FIELDS);
        return StreamSupport.stream(i.spliterator(), false).collect(Collectors.toList()); 
    }

    @Override
    protected BoxItem.Info createDirectoryEntry(BoxItem.Info parentEntry, Path dir) throws IOException {
        return asFolder(parentEntry).getResource().createFolder(toFilenameString(dir));
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

    @Override
    protected void checkAccessEntry(BoxItem.Info entry, Path path, AccessMode... modes) throws IOException {

        final Set<AccessMode> set = EnumSet.noneOf(AccessMode.class);

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

        if (set.size() > 0) {
            throw new AccessDeniedException(path + ": " + set);
        }
    }

    @Override
    public WatchService newWatchService() {
        try {
            return new BoxWatchService(rootInfo);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
