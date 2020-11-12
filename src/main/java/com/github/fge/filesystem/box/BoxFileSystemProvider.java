package com.github.fge.filesystem.box;

import com.github.fge.filesystem.provider.FileSystemProviderBase;

public final class BoxFileSystemProvider
    extends FileSystemProviderBase
{
    public static final String PARAM_ID = "id";

    public static final String ENV_USER_CREDENTIAL = "user_credential";

    public static final String ENV_APP_CREDENTIAL = "app_credential";

    public static final String ENV_IGNORE_APPLE_DOUBLE = "ignoreAppleDouble";

    public static final String ENV_USE_SYSTEM_WATCHER = "use_system_watcher";

    public BoxFileSystemProvider()
    {
        super(new BoxFileSystemRepository());
    }
}
