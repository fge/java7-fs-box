package com.github.fge.filesystem.box.provider;

import com.github.fge.filesystem.driver.FileSystemDriver;
import com.github.fge.filesystem.provider.FileSystemRepositoryBase;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

@ParametersAreNonnullByDefault
public final class BoxFileSystemRepository
    extends FileSystemRepositoryBase
{
    public BoxFileSystemRepository()
    {
        super("box");
    }

    @Nonnull
    @Override
    public FileSystemDriver createDriver(final URI uri,
        final Map<String, ?> env)
        throws IOException
    {
        return null;
    }
}
