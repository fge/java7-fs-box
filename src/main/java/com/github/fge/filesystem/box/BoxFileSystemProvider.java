package com.github.fge.filesystem.box;

import com.github.fge.filesystem.provider.FileSystemProviderBase;

public final class BoxFileSystemProvider
    extends FileSystemProviderBase
{
    public static final String PARAM_ID = "id";

    public static final String ENV_USER_CREDENTIAL = "user_credential";

    public static final String ENV_APP_CREDENTIAL = "app_credential";

    public BoxFileSystemProvider()
    {
        super(new BoxFileSystemRepository());
    }
}
