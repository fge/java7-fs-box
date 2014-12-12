package com.github.fge.filesystem.box.attributes;

import com.box.sdk.BoxItem;
import com.github.fge.filesystem.attributes.FileAttributesFactory;

public final class BoxFileAttributesFactory
    extends FileAttributesFactory
{
    public BoxFileAttributesFactory()
    {
        addImplementation("basic", BoxBasicFileAttributesProvider.class,
            BoxItem.class);
    }
}
