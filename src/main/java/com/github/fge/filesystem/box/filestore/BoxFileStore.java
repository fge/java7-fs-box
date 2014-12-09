package com.github.fge.filesystem.box.filestore;

import com.github.fge.filesystem.filestore.UnsizedFileStoreBase;

import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileAttributeView;
import java.util.Objects;

public final class BoxFileStore
    extends UnsizedFileStoreBase
{
    public BoxFileStore()
    {
        super("box", false);
    }

    /**
     * Tells whether or not this file store supports the file attributes
     * identified by the given file attribute view.
     * <p> Invoking this method to test if the file store supports {@link
     * BasicFileAttributeView} will always return {@code true}. In the case of
     * the default provider, this method cannot guarantee to give the correct
     * result when the file store is not a local storage device. The reasons for
     * this are implementation specific and therefore unspecified.
     *
     * @param type the file attribute view type
     * @return {@code true} if, and only if, the file attribute view is
     * supported
     */
    @Override
    public boolean supportsFileAttributeView(
        final Class<? extends FileAttributeView> type)
    {
        return Objects.requireNonNull(type) == BasicFileAttributeView.class;
    }

    /**
     * Tells whether or not this file store supports the file attributes
     * identified by the given file attribute view.
     * <p> Invoking this method to test if the file store supports {@link
     * BasicFileAttributeView}, identified by the name "{@code basic}" will
     * always return {@code true}. In the case of the default provider, this
     * method cannot guarantee to give the correct result when the file store is
     * not a local storage device. The reasons for this are implementation
     * specific and therefore unspecified.
     *
     * @param name the {@link FileAttributeView#name name} of file attribute
     * view
     * @return {@code true} if, and only if, the file attribute view is
     * supported
     */
    @Override
    public boolean supportsFileAttributeView(final String name)
    {
        return "basic".equals(Objects.requireNonNull(name));
    }
}
