package com.github.fge.filesystem.box.driver;

import com.box.sdk.BoxFile;
import com.box.sdk.BoxFolder;
import com.box.sdk.BoxItem;

public enum BoxType
{
    FILE,
    DIRECTORY,
    ;

    public static BoxType getType(final BoxItem.Info info)
    {
        if (info instanceof BoxFile.Info)
            return FILE;
        if (info instanceof BoxFolder.Info)
            return DIRECTORY;
        throw new IllegalStateException("how did I get there??");
    }
}
