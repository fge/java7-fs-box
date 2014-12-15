package com.github.fge.filesystem.box.provider;

import com.github.fge.filesystem.box.attributes.BoxFileAttributesFactory;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;

public final class BoxFileSystemFactoryProvider
    extends FileSystemFactoryProvider
{
    public BoxFileSystemFactoryProvider()
    {
        setAttributesFactory(new BoxFileAttributesFactory());
    }
}
