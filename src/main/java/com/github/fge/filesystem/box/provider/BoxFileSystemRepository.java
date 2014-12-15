package com.github.fge.filesystem.box.provider;

import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxAPIException;
import com.box.sdk.BoxFolder;
import com.github.fge.filesystem.attributes.FileAttributesFactory;
import com.github.fge.filesystem.box.attributes.BoxFileAttributesFactory;
import com.github.fge.filesystem.box.driver.BoxAPIWrapper;
import com.github.fge.filesystem.box.driver.BoxFileSystemDriver;
import com.github.fge.filesystem.box.driver.DefaultBoxAPIWrapper;
import com.github.fge.filesystem.box.exceptions.BoxIOException;
import com.github.fge.filesystem.box.filestore.BoxFileStore;
import com.github.fge.filesystem.driver.FileSystemDriver;
import com.github.fge.filesystem.provider.FileSystemRepositoryBase;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileStore;
import java.util.Map;

@ParametersAreNonnullByDefault
public final class BoxFileSystemRepository
    extends FileSystemRepositoryBase
{
    private static final String ACCESS_TOKEN = "accessToken";

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
        final String accessToken = (String) env.get(ACCESS_TOKEN);

        if (accessToken == null)
            throw new IllegalArgumentException("access token not found");

        final FileAttributesFactory attributesFactory
            = new BoxFileAttributesFactory();
        final BoxAPIConnection api = new BoxAPIConnection(accessToken);
        final BoxAPIWrapper wrapper = new DefaultBoxAPIWrapper(api);
        final FileStore store;

        try {
            final BoxFolder root = BoxFolder.getRootFolder(api);
            store = new BoxFileStore(root.getInfo(), attributesFactory);
        } catch (BoxAPIException e) {
            throw BoxIOException.wrap(e);
        }

        return new BoxFileSystemDriver(store, attributesFactory, wrapper);
    }
}
