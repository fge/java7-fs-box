package com.github.fge.filesystem.box.driver;

import com.box.boxjavalibv2.BoxClient;
import com.box.boxjavalibv2.dao.BoxCollection;
import com.box.boxjavalibv2.dao.BoxFolder;
import com.box.boxjavalibv2.dao.BoxItem;
import com.box.boxjavalibv2.dao.BoxResourceType;
import com.box.boxjavalibv2.dao.BoxTypedObject;
import com.box.boxjavalibv2.exceptions.AuthFatalFailureException;
import com.box.boxjavalibv2.exceptions.BoxServerException;
import com.box.boxjavalibv2.requests.requestobjects.BoxPagingRequestObject;
import com.box.boxjavalibv2.requests.requestobjects.BoxRequestExtras;
import com.box.boxjavalibv2.resourcemanagers.IBoxFilesManager;
import com.box.boxjavalibv2.resourcemanagers.IBoxFoldersManager;
import com.box.boxjavalibv2.resourcemanagers.IBoxItemsManager;
import com.box.restclientv2.exceptions.BoxRestException;
import com.box.restclientv2.requestsbase.BoxDefaultRequestObject;
import com.github.fge.filesystem.box.filestore.BoxFileStore;
import com.github.fge.filesystem.driver.UnixLikeFileSystemDriverBase;
import com.github.fge.filesystem.exceptions.IsDirectoryException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("OverloadedVarargsMethod")
@ParametersAreNonnullByDefault
public final class BoxFileSystemDriver
    extends UnixLikeFileSystemDriverBase
{
    private static final String ROOT_DIR_ID = "0";
    private static final int PAGING_SIZE = 50;

    private final BoxClient client;

    private final IBoxFoldersManager dirManager;
    private final IBoxFilesManager fileManager;
    private final IBoxItemsManager itemManager;

    public BoxFileSystemDriver(final URI uri, final BoxClient client)
    {
        super(uri, new BoxFileStore());
        this.client = Objects.requireNonNull(client);
        dirManager = client.getFoldersManager();
        fileManager = client.getFilesManager();
        itemManager = client.getBoxItemsManager();
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
        final BoxItem item = lookupPath(path.toRealPath());

        if (!"file".equals(item.getType()))
            throw new IsDirectoryException(path.toString());

        // TODO: check that it is not altered
        final BoxDefaultRequestObject req = new BoxDefaultRequestObject();

        try {
            return fileManager.downloadFile(item.getId(), req);
        } catch (BoxRestException e) {
            throw new IOException("API error", e);
        } catch (BoxServerException e) {
            throw new IOException("Box server error", e);
        } catch (AuthFatalFailureException e) {
            throw new IOException("Authentication failure", e);
        }
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
    @Nonnull
    @Override
    public OutputStream newOutputStream(final Path path,
        final OpenOption... options)
        throws IOException
    {
        final BoxItem item = lookupPath(path.toRealPath());

        if (!"file".equals(item.getType()))
            throw new IsDirectoryException(path.toString());

        // TODO: check that it is not altered
        final BoxDefaultRequestObject req = new BoxDefaultRequestObject();
        return null;
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
        return null;
    }

    /**
     * Create a new directory from a path on this filesystem
     *
     * @param dir the directory to create
     * @param attrs the attributes with which the directory should be created
     * @throws IOException filesystem level error, or a plain I/O error
     * @see FileSystemProvider#newDirectoryStream(Path, DirectoryStream.Filter)
     */
    @Override
    public void createDirectory(final Path dir, final FileAttribute<?>... attrs)
        throws IOException
    {

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
    @Override
    public void move(final Path source, final Path target,
        final CopyOption... options)
        throws IOException
    {

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

    }

    /**
     * Read an attribute view for a given path on this filesystem
     *
     * @param path the path to read attributes from
     * @param type the class of attribute view to return
     * @param options the link options
     * @return the attributes view; {@code null} if this view is not supported
     *
     * @see FileSystemProvider#getFileAttributeView(Path, Class, LinkOption...)
     */
    @Nullable
    @Override
    public <V extends FileAttributeView> V getFileAttributeView(final Path path,
        final Class<V> type, final LinkOption... options)
    {
        return null;
    }

    /**
     * Read attributes from a path on this filesystem
     *
     * @param path the path to read attributes from
     * @param type the class of attributes to read
     * @param options the link options
     * @return the attributes
     *
     * @throws IOException filesystem level error, or a plain I/O error
     * @throws UnsupportedOperationException attribute type not supported
     * @see FileSystemProvider#readAttributes(Path, Class, LinkOption...)
     */
    @Override
    public <A extends BasicFileAttributes> A readAttributes(final Path path,
        final Class<A> type, final LinkOption... options)
        throws IOException
    {
        return null;
    }

    /**
     * Read a list of attributes from a path on this filesystem
     *
     * @param path the path to read attributes from
     * @param attributes the list of attributes to read
     * @param options the link options
     * @return the relevant attributes as a map
     *
     * @throws IOException filesystem level error, or a plain I/O error
     * @throws IllegalArgumentException malformed attributes string; or a
     * specified attribute does not exist
     * @throws UnsupportedOperationException one or more attribute(s) is/are not
     * supported
     * @see Files#readAttributes(Path, String, LinkOption...)
     * @see FileSystemProvider#readAttributes(Path, String, LinkOption...)
     */
    @Override
    public Map<String, Object> readAttributes(final Path path,
        final String attributes, final LinkOption... options)
        throws IOException
    {
        return null;
    }

    /**
     * Set an attribute for a path on this filesystem
     *
     * @param path the victim
     * @param attribute the name of the attribute to set
     * @param value the value to set
     * @param options the link options
     * @throws IOException filesystem level error, or a plain I/O error
     * @throws IllegalArgumentException malformed attribute, or the specified
     * attribute does not exist
     * @throws UnsupportedOperationException the attribute to set is not
     * supported by this filesystem
     * @throws ClassCastException attribute value is of the wrong class for the
     * specified attribute
     * @see Files#setAttribute(Path, String, Object, LinkOption...)
     * @see FileSystemProvider#setAttribute(Path, String, Object, LinkOption...)
     */
    @Override
    public void setAttribute(final Path path, final String attribute,
        final Object value, final LinkOption... options)
        throws IOException
    {

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
    }

    @Nullable
    private BoxItem lookupPath(final Path path)
        throws IOException
    {
        if (!path.isAbsolute())
            throw new IllegalStateException("path not absolute, cannot lookup");

        String id = ROOT_DIR_ID;
        BoxItem item = lookupFolder(id);

        final int nameCount = path.getNameCount();

        if (nameCount == 0)
            return item;

        final List<String> names = new ArrayList<>(nameCount);

        for (final Path element: path)
            names.add(element.toString());

        String name;
        while (!names.isEmpty() && "folder".equals(item.getType())) {
            name = names.remove(0);
            item = findItemInCollection(id, name);
            if (item == null)
                break;
            id = item.getId();
        }

        return item;
    }

    @Nullable
    private BoxItem findItemInCollection(final String id, final String name)
        throws IOException
    {
        int index = 0, nrEntries;

        BoxPagingRequestObject req;
        BoxCollection collection;
        BoxItem item;

        do {
            req = BoxPagingRequestObject.pagingRequestObject(PAGING_SIZE,
                index);
            index += PAGING_SIZE;
            try {
                collection = dirManager.getFolderItems(id, req);
            } catch (BoxRestException e) {
                throw new IOException("API error", e);
            } catch (BoxServerException e) {
                throw new IOException("Box server error", e);
            } catch (AuthFatalFailureException e) {
                throw new IOException("Authentication failure", e);
            }
            nrEntries = collection.getTotalCount();
            for (final BoxTypedObject object : collection.getEntries()) {
                switch (object.getType()) {
                    case "file": case "folder":
                        item = (BoxItem) object;
                        if (name.equals(item.getName()))
                            return item;
                    default:
                }
            }
        } while (nrEntries == PAGING_SIZE);

        return null;
    }

    private BoxItem lookupItem(final String id)
        throws IOException
    {
        final IBoxItemsManager manager = client.getBoxItemsManager();

        final BoxDefaultRequestObject req
            = new BoxDefaultRequestObject();
        final BoxRequestExtras extras = req.getRequestExtras();

        extras.addField(BoxItem.FIELD_NAME);
        extras.addField(BoxTypedObject.FIELD_TYPE);
        extras.addField(BoxItem.FIELD_SIZE);
        extras.addField(BoxTypedObject.FIELD_MODIFIED_AT);
        extras.addField(BoxTypedObject.FIELD_CREATED_AT);

        try {
            return manager.getItem(id, req, BoxResourceType.ITEM);
        } catch (BoxRestException e) {
            throw new IOException("API error", e);
        } catch (BoxServerException e) {
            throw new IOException("Box server error", e);
        } catch (AuthFatalFailureException e) {
            throw new IOException("Authentication failure", e);
        }
    }

    private BoxFolder lookupFolder(final String id)
        throws IOException
    {
        final IBoxFoldersManager manager = client.getFoldersManager();
        final BoxDefaultRequestObject req
            = new BoxDefaultRequestObject();
        final BoxRequestExtras extras = req.getRequestExtras();

        extras.addField(BoxItem.FIELD_NAME);
        extras.addField(BoxTypedObject.FIELD_TYPE);
        extras.addField(BoxItem.FIELD_SIZE);
        extras.addField(BoxTypedObject.FIELD_MODIFIED_AT);
        extras.addField(BoxTypedObject.FIELD_CREATED_AT);

        try {
            return manager.getFolder(id, req);
        } catch (BoxRestException e) {
            throw new IOException("API error", e);
        } catch (BoxServerException e) {
            throw new IOException("Box server error", e);
        } catch (AuthFatalFailureException e) {
            throw new IOException("Authentication failure", e);
        }
    }
}
