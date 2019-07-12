package com.github.fge.filesystem.box.provider;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileStore;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxAPIException;
import com.box.sdk.BoxFolder;
import com.github.fge.filesystem.box.driver.BoxFileSystemDriver;
import com.github.fge.filesystem.box.exceptions.BoxIOException;
import com.github.fge.filesystem.box.filestore.BoxFileStore;
import com.github.fge.filesystem.driver.FileSystemDriver;
import com.github.fge.filesystem.provider.FileSystemRepositoryBase;

import vavi.net.auth.oauth2.BasicAppCredential;
import vavi.net.auth.oauth2.box.BoxLocalOAuth2;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

@ParametersAreNonnullByDefault
@PropsEntity(url = "classpath:box.properties")
public final class BoxFileSystemRepository
    extends FileSystemRepositoryBase
{
    public BoxFileSystemRepository()
    {
        super("box", new BoxFileSystemFactoryProvider());
    }

    /** should be {@link vavi.net.auth.oauth2.Authenticator} and have a constructor with args (String, String) */
    @Property(value = "vavi.net.auth.oauth2.box.BoxLocalAuthenticator")
    private String authenticatorClassName;

    /* */
    {
        try {
            PropsEntity.Util.bind(this);
Debug.println("authenticatorClassName: " + authenticatorClassName);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Nonnull
    @Override
    public FileSystemDriver createDriver(final URI uri,
        final Map<String, ?> env)
        throws IOException
    {
        Map<String, String> params = getParamsMap(uri);
        if (!params.containsKey(BoxFileSystemProvider.PARAM_ID)) {
            throw new NoSuchElementException("uri not contains a param " + BoxFileSystemProvider.PARAM_ID);
        }
        final String email = params.get(BoxFileSystemProvider.PARAM_ID);

        if (!env.containsKey(BoxFileSystemProvider.ENV_CREDENTIAL)) {
            throw new NoSuchElementException("app credential not contains a param " + BoxFileSystemProvider.ENV_CREDENTIAL);
        }
        BasicAppCredential appCredential = BasicAppCredential.class.cast(env.get(BoxFileSystemProvider.ENV_CREDENTIAL));

        try {
            final BoxAPIConnection api = new BoxLocalOAuth2(appCredential, authenticatorClassName).authorize(email);

            final BoxFolder.Info rootInfo = BoxFolder.getRootFolder(api).getInfo();
            final FileStore store = new BoxFileStore(rootInfo,
                factoryProvider.getAttributesFactory());

            return new BoxFileSystemDriver(store, factoryProvider, rootInfo, env);
        } catch (BoxAPIException e) {
            throw BoxIOException.wrap(e);
        }
    }
}
