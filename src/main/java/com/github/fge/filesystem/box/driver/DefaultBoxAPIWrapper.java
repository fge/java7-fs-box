package com.github.fge.filesystem.box.driver;

import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxAPIException;
import com.box.sdk.BoxFile;
import com.box.sdk.BoxFolder;
import com.box.sdk.BoxItem;
import com.github.fge.filesystem.box.exceptions.BoxIOException;
import com.github.fge.filesystem.exceptions.IsDirectoryException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.Objects;

@ParametersAreNonnullByDefault
public final class DefaultBoxAPIWrapper
    implements BoxAPIWrapper
{
    // TODO: make available?
    private final BoxFolder rootFolder;

    public DefaultBoxAPIWrapper(final BoxAPIConnection api)
    {
        rootFolder = BoxFolder.getRootFolder(Objects.requireNonNull(api));
    }

    /**
     * Get an item by path
     *
     * @param path the path
     * @return the item, or {@code null} if not found
     *
     * @throws BoxIOException Box API error
     */
    @Nullable
    @Override
    public BoxItem getItem(final Path path)
        throws BoxIOException
    {
        final int nameCount = path.getNameCount();

        BoxFolder folder = rootFolder;
        BoxItem item = folder;

        String name;
        int count = 0;

        while (count < nameCount) {
            name = path.getName(count).toString();
            count++;
            item = findItemByName(folder, name);
            if (item == null)
                return null;
            if (!(item instanceof BoxFolder))
                break;
            folder = (BoxFolder) item;
        }

        return count == nameCount ? item : null;
    }

    /**
     * Get a file by its path
     *
     * @param path the path
     * @return the file
     *
     * @throws BoxIOException Box API error
     * @throws IsDirectoryException item at this path is a directory
     */
    @Nonnull
    @Override
    public BoxFile getFile(final Path path)
        throws BoxIOException, IsDirectoryException
    {
        final BoxItem item = getItem(path);

        if (!(item instanceof BoxFile))
            throw new IsDirectoryException(path.toString());

        return (BoxFile) item;
    }

    /**
     * Get a directory by its path
     *
     * @param path the path
     * @return the directory
     *
     * @throws BoxIOException Box API error
     * @throws NoSuchFileException directory does not exist
     * @throws NotDirectoryException item at this path is not a directory
     */
    @Nonnull
    @Override
    public BoxFolder getFolder(final Path path)
        throws BoxIOException, NotDirectoryException
    {
        final BoxItem item = getItem(path);

        if (!(item instanceof BoxFolder))
            throw new NotDirectoryException(path.toString());

        return (BoxFolder) item;
    }

    /**
     * Tell whether a folder is empty
     *
     * @param folder the folder
     * @return true if this folder has zero entries
     *
     * @throws BoxIOException Box API error
     */
    @Override
    public boolean folderIsEmpty(final BoxFolder folder)
        throws BoxIOException
    {
        try {
            return folder.iterator().hasNext();
        } catch (BoxAPIException e) {
            throw BoxIOException.wrap(e);
        }
    }

    /**
     * Delete an item at a given path
     *
     * @param victim the item to delete
     * @throws BoxIOException Box API error
     * @throws NoSuchFileException item does not exist
     * @throws DirectoryNotEmptyException victim is a non empty directory
     */
    @Override
    public void deleteItem(final Path victim)
        throws BoxIOException, DirectoryNotEmptyException
    {
        final BoxItem item = getItem(victim);

        if (item instanceof BoxFile) {
            try {
                ((BoxFile) item).delete();
            } catch (BoxAPIException e) {
                throw BoxIOException.wrap(e);
            }
            return;
        }

        /*
         * Not a file, therefore a directory
         */

        final BoxFolder folder = (BoxFolder) item;

        if (!folderIsEmpty(folder))
            throw new DirectoryNotEmptyException(victim.toString());

        try {
            folder.delete(false);
        } catch (BoxAPIException e) {
            throw BoxIOException.wrap(e);
        }
    }

    private static BoxItem findItemByName(final BoxFolder folder,
        final String name)
        throws BoxIOException
    {
        try {
            for (final BoxItem.Info info: folder)
                if (info.getName().equals(name))
                    return (BoxItem) info.getResource();
            return null;
        } catch (BoxAPIException e) {
            throw BoxIOException.wrap(e);
        }
    }
}
