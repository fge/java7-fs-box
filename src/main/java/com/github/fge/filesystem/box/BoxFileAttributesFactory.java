package com.github.fge.filesystem.box;

import com.box.sdk.BoxItem;
import com.github.fge.filesystem.driver.ExtendedFileSystemDriverBase.ExtendsdFileAttributesFactory;

public final class BoxFileAttributesFactory
    extends ExtendsdFileAttributesFactory
{
    public BoxFileAttributesFactory()
    {
        setMetadataClass(BoxItem.Info.class);
        addImplementation("basic", BoxBasicFileAttributesProvider.class);
    }
}
