package com.github.fge.filesystem.box.attributes;

import java.io.IOException;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import com.box.sdk.BoxAPIException;
import com.box.sdk.BoxFolder;
import com.box.sdk.BoxItem;
import com.github.fge.filesystem.attributes.provider.BasicFileAttributesProvider;
import com.github.fge.filesystem.box.exceptions.BoxIOException;

@ParametersAreNonnullByDefault
public final class BoxBasicFileAttributesProvider
    extends BasicFileAttributesProvider implements PosixFileAttributes
{
    private final BoxItem.Info info;
    private final boolean isFolder;

    private Map<String, BoxItem.Info> cache = new HashMap<>();
    
    public BoxBasicFileAttributesProvider(final BoxItem item)
        throws IOException
    {
        String key = item.getID();
        if (cache.containsKey(key)) {
            info = cache.get(key);
        } else {
            try {
                info = item.getInfo();
            } catch (BoxAPIException e) {
                throw BoxIOException.wrap(e);
            }
            cache.put(key, info);
        }
        isFolder = item instanceof BoxFolder;
    }

    @Override
    public FileTime lastModifiedTime()
    {try {
        return info.getModifiedAt() != null ? FileTime.fromMillis(info.getModifiedAt().getTime()) : creationTime();
    } catch (NullPointerException e) {System.err.println("info: "+ info + ", info.getModifiedAt(): " + info.getModifiedAt());throw e;}}

    @Override
    public FileTime creationTime()
    {
        return info.getCreatedAt() != null ? FileTime.fromMillis(info.getCreatedAt().getTime()) : FileTime.fromMillis(0);
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

    /* @see java.nio.file.attribute.PosixFileAttributes#owner() */
    @Override
    public UserPrincipal owner() {
        return null;
    }

    /* @see java.nio.file.attribute.PosixFileAttributes#group() */
    @Override
    public GroupPrincipal group() {
        return null;
    }

    /* @see java.nio.file.attribute.PosixFileAttributes#permissions() */
    @Override
    public Set<PosixFilePermission> permissions() {
        return isFolder ? PosixFilePermissions.fromString("rwxr-xr-x") : PosixFilePermissions.fromString("rw-r--r--");
    }
}
