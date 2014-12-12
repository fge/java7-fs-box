package com.github.fge.filesystem.box.attributes;

import com.box.sdk.BoxAPIException;
import com.box.sdk.BoxFolder;
import com.box.sdk.BoxItem;
import com.github.fge.filesystem.attributes.provider.BasicFileAttributesProvider;


import com.github.fge.filesystem.box.exceptions.BoxIOException;


import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.nio.file.attribute.FileTime;

@ParametersAreNonnullByDefault
public final class BoxBasicFileAttributesProvider
    extends BasicFileAttributesProvider
{
    private final BoxItem.Info info;
    private final boolean isFolder;

    public BoxBasicFileAttributesProvider(final BoxItem item)
        throws IOException
    {
        try {
            info = item.getInfo();
        } catch (BoxAPIException e) {
            throw BoxIOException.wrap(e);
        }
        isFolder = item instanceof BoxFolder;
    }

    @Override
    public FileTime lastModifiedTime()
    {
        return FileTime.fromMillis(info.getModifiedAt().getTime());
    }

    @Override
    public FileTime creationTime()
    {
        return FileTime.fromMillis(info.getCreatedAt().getTime());
    }

    /**
     * Tells whether the file is a regular file with opaque content.
     */
    @Override
    public boolean isRegularFile()
    {
        return !isFolder;
    }

    /**
     * Tells whether the file is a directory.
     */
    @Override
    public boolean isDirectory()
    {
        return isFolder;
    }

    /**
     * Returns the size of the file (in bytes). The size may differ from the
     * actual size on the file system due to compression, support for sparse
     * files, or other reasons. The size of files that are not {@link
     * #isRegularFile regular} files is implementation specific and
     * therefore unspecified.
     *
     * @return the file size, in bytes
     */
    @Override
    public long size()
    {
        return info.getSize();
    }
}
