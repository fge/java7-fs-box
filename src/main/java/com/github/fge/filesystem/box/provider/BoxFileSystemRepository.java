package com.github.fge.filesystem.box.provider;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileStore;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxAPIConnectionListener;
import com.box.sdk.BoxAPIException;
import com.box.sdk.BoxFolder;
import com.github.fge.filesystem.box.driver.BoxAPIWrapper;
import com.github.fge.filesystem.box.driver.BoxFileSystemDriver;
import com.github.fge.filesystem.box.driver.DefaultBoxAPIWrapper;
import com.github.fge.filesystem.box.exceptions.BoxIOException;
import com.github.fge.filesystem.box.filestore.BoxFileStore;
import com.github.fge.filesystem.driver.FileSystemDriver;
import com.github.fge.filesystem.provider.FileSystemRepositoryBase;

@ParametersAreNonnullByDefault
public final class BoxFileSystemRepository
    extends FileSystemRepositoryBase
{
    private static final String ACCESS_TOKEN = "accessToken";

    public BoxFileSystemRepository()
    {
        super("box", new BoxFileSystemFactoryProvider());
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

        final BoxAPIConnection api = new BoxAPIConnection(accessToken);
        api.addListener(new BoxAPIConnectionListener() {
            @Override
            public void onRefresh(BoxAPIConnection api) {
                System.out.println("refresh tocken" + api.getRefreshToken());
            }
            @Override
            public void onError(BoxAPIConnection api, BoxAPIException error) {
                error.printStackTrace();
            }
        });
        final BoxAPIWrapper wrapper = new DefaultBoxAPIWrapper(api);
        final FileStore store;

        try {
            final BoxFolder root = BoxFolder.getRootFolder(api);
            store = new BoxFileStore(root.getInfo(),
                factoryProvider.getAttributesFactory());
        } catch (BoxAPIException e) {
            throw BoxIOException.wrap(e);
        }

        return new BoxFileSystemDriver(store, factoryProvider, wrapper);
    }
}
