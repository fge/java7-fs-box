package com.github.fge.filesystem.box.attributes;

import java.io.IOException;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import com.box.sdk.BoxFile;
import com.box.sdk.BoxFolder;
import com.box.sdk.BoxItem;
import com.github.fge.filesystem.attributes.provider.BasicFileAttributesProvider;

@ParametersAreNonnullByDefault
public final class BoxBasicFileAttributesProvider
    extends BasicFileAttributesProvider implements PosixFileAttributes
{
    private final BoxItem.Info entry;

    public BoxBasicFileAttributesProvider(final BoxItem.Info entry)
        throws IOException
    {
        this.entry = entry;
    }

    @Override
    public FileTime lastModifiedTime()
    {
        return entry.getModifiedAt() != null ? FileTime.fromMillis(entry.getModifiedAt().getTime()) : creationTime();
    }

    @Override
    public FileTime creationTime()
    {
        return entry.getCreatedAt() != null ? FileTime.fromMillis(entry.getCreatedAt().getTime()) : UNIX_EPOCH;
    }

    /**
     * Tells whether the file is a regular file with opaque content.
     */
    @Override
    public boolean isRegularFile()
    {
        return BoxFile.Info.class.isInstance(entry);
    }

    /**
     * Tells whether the file is a directory.
     */
    @Override
    public boolean isDirectory()
    {
        return BoxFolder.Info.class.isInstance(entry);
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
        return entry.getSize();
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
        return isDirectory() ? PosixFilePermissions.fromString("rwxr-xr-x") : PosixFilePermissions.fromString("rw-r--r--");
    }
}
