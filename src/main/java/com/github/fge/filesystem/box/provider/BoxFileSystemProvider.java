package com.github.fge.filesystem.box.provider;

import com.github.fge.filesystem.provider.FileSystemProviderBase;

public final class BoxFileSystemProvider
    extends FileSystemProviderBase
{
    public static final String PARAM_ID = "id";

    public static final String ENV_CREDENTIAL = "credential";

    public BoxFileSystemProvider()
    {
        super(new BoxFileSystemRepository());
    }
}
