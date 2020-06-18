package com.github.fge.filesystem.box;

import com.github.fge.filesystem.provider.FileSystemFactoryProvider;

public final class BoxFileSystemFactoryProvider
    extends FileSystemFactoryProvider
{
    public BoxFileSystemFactoryProvider()
    {
        setAttributesFactory(new BoxFileAttributesFactory());
        setOptionsFactory(new BoxFileSystemOptionsFactory());
    }
}
