package com.github.fge.filesystem.box.driver;

import com.box.sdk.BoxAPIException;
import com.box.sdk.BoxFolder;
import com.box.sdk.BoxItem;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

/**
 * Wrapper class over Box REST API calls
 *
 * <p>All API calls throw a {@link BoxAPIException} on failure, which is
 * unchecked. We don't want that for filesystem operations, so wrap calls to the
 * API and make it easier to deal with these exceptions.</p>
 */
@ParametersAreNonnullByDefault
public final class BoxUtil
{
    private BoxUtil()
    {
        throw new Error("nice try!");
    }

    /**
     * Find an item by name in a directory
     *
     * @param dir the directory to search
     * @param name the name to search
     * @return the item, or {@code null} if not found
     *
     * @throws BoxAPIException API failure
     */
    @Nullable
    public static BoxItem.Info findEntryByName(final BoxFolder dir,
        final String name)
    {
        Objects.requireNonNull(dir);
        Objects.requireNonNull(name);

        for (final BoxItem.Info info : dir)
            if (name.equals(info.getName()))
                return info;

        return null;
    }
}
