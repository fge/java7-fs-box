package com.github.fge.filesystem.box.provider;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileStore;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxFolder;
import com.github.fge.filesystem.box.driver.BoxFileSystemDriver;
import com.github.fge.filesystem.box.filestore.BoxFileStore;
import com.github.fge.filesystem.driver.FileSystemDriver;
import com.github.fge.filesystem.provider.FileSystemRepositoryBase;

import vavi.net.auth.oauth2.BasicAppCredential;
import vavi.net.auth.oauth2.UserCredential;
import vavi.net.auth.oauth2.box.BoxLocalAppCredential;
import vavi.net.auth.oauth2.box.BoxLocalUserCredential;
import vavi.net.auth.oauth2.box.BoxOAuth2;

@ParametersAreNonnullByDefault
public final class BoxFileSystemRepository
    extends FileSystemRepositoryBase
{
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
        // 1. user credential
        UserCredential userCredential = null;

        if (env.containsKey(BoxFileSystemProvider.ENV_USER_CREDENTIAL)) {
            userCredential = UserCredential.class.cast(env.get(BoxFileSystemProvider.ENV_USER_CREDENTIAL));
        }

        Map<String, String> params = getParamsMap(uri);
        if (userCredential == null && params.containsKey(BoxFileSystemProvider.PARAM_ID)) {
            String email = params.get(BoxFileSystemProvider.PARAM_ID);
            userCredential = new BoxLocalUserCredential(email);
        }

        if (userCredential == null) {
            throw new NoSuchElementException("uri not contains a param " + BoxFileSystemProvider.PARAM_ID + " nor " +
                                             "env not contains a param " + BoxFileSystemProvider.ENV_USER_CREDENTIAL);
        }

        // 2. app credential
        BasicAppCredential appCredential = null;

        if (env.containsKey(BoxFileSystemProvider.ENV_APP_CREDENTIAL)) {
            appCredential = BasicAppCredential.class.cast(env.get(BoxFileSystemProvider.ENV_APP_CREDENTIAL));
        }

        if (appCredential == null) {
            appCredential = new BoxLocalAppCredential(); // TODO use prop
        }

        // 3. process
        final BoxAPIConnection api = new BoxOAuth2(appCredential).authorize(userCredential);
        final BoxFolder.Info rootInfo = BoxFolder.getRootFolder(api).getInfo();
        final FileStore store = new BoxFileStore(rootInfo, factoryProvider.getAttributesFactory());
        return new BoxFileSystemDriver(store, factoryProvider, rootInfo, env);
    }
}
