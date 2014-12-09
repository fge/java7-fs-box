package com.github.fge.filesystem.box.exceptions;

import java.nio.file.FileSystemException;

public final class BoxFileSystemException
    extends FileSystemException
{
    /**
     * Constructs an instance of this class. This constructor should be used
     * when an operation involving one file fails and there isn't any additional
     * information to explain the reason.
     *
     * @param file a string identifying the file or {@code null} if not known.
     */
    public BoxFileSystemException(final String file)
    {
        super(file);
    }

    /**
     * Constructs an instance of this class. This constructor should be used
     * when an operation involving two files fails, or there is additional
     * information to explain the reason.
     *
     * @param file a string identifying the file or {@code null} if not known.
     * @param other a string identifying the other file or {@code null} if there
     * isn't another file or if not known
     * @param reason a reason message with additional information or {@code
     * null}
     */
    public BoxFileSystemException(final String file, final String other,
        final String reason)
    {
        super(file, other, reason);
    }
}
