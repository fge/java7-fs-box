package com.github.fge.filesystem.box.provider;

import com.github.fge.filesystem.provider.FileSystemProviderBase;

public final class BoxFileSystemProvider
    extends FileSystemProviderBase
{
    public BoxFileSystemProvider()
    {
        super(new BoxFileSystemRepository());
    }
}
