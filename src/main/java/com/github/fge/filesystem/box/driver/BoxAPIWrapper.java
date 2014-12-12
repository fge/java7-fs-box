package com.github.fge.filesystem.box.driver;

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
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;

/**
 * Wrapper class over the Box Java API
 *
 * <p>All calls to the API are wrapped so that a {@link BoxIOException} is
 * thrown instead of the (unchecked) {@link BoxAPIException}.</p>
 *
 * <p>It is the caller's responsibility to ensure that all paths are absolute
 * (see {@link Path#toRealPath(LinkOption...)}).</p>
 *
 * @see BoxFileSystemDriverV2
 */
@ParametersAreNonnullByDefault
public interface BoxAPIWrapper
{
    /**
     * Get an item by path
     *
     * @param path the path
     * @return the item, or {@code null} if not found
     * @throws BoxIOException Box API error
     */
    @Nullable
    BoxItem getItem(Path path)
        throws BoxIOException;

    /**
     * Get a file by its path
     *
     * @param path the path
     * @return the file
     * @throws BoxIOException Box API error
     * @throws NoSuchFileException file does not exist
     * @throws IsDirectoryException item at this path is a directory
     */
    @Nonnull
    BoxFile getFile(Path path)
        throws BoxIOException, NoSuchFileException, IsDirectoryException;

    /**
     * Get a directory by its path
     *
     * @param path the path
     * @return the directory
     * @throws BoxIOException Box API error
     * @throws NoSuchFileException directory does not exist
     * @throws NotDirectoryException item at this path is not a directory
     */
    @Nonnull
    BoxFolder getFolder(Path path)
        throws BoxIOException, NoSuchFileException, NotDirectoryException;

    /**
     * Tell whether a folder is empty
     *
     * @param folder the folder
     * @return true if this folder has zero entries
     * @throws BoxIOException Box API error
     */
    boolean folderIsEmpty(BoxFolder folder)
        throws BoxIOException;

    /**
     * Delete an item at a given path
     *
     * @param victim the item to delete
     * @throws BoxIOException Box API error
     * @throws NoSuchFileException item does not exist
     * @throws DirectoryNotEmptyException victim is a non empty directory
     */
    void deleteItem(Path victim)
        throws BoxIOException, NoSuchFileException, DirectoryNotEmptyException;
}
