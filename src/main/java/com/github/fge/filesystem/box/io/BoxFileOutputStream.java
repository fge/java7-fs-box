package com.github.fge.filesystem.box.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

import com.box.sdk.BoxAPIException;
import com.box.sdk.BoxFile;
import com.box.sdk.BoxFolder;
import com.box.sdk.BoxItem;
import com.github.fge.filesystem.box.exceptions.BoxIOException;
import com.github.fge.filesystem.driver.FileSystemDriver;

/**
 * Wrapper over a file upload over the box.com API
 *
 * <p>There are two cases: either overwrite an existing file or creating a new
 * file (this is why there are two constructors).</p>
 *
 * @see Files#newOutputStream(Path, OpenOption...)
 * @see FileSystemDriver#newOutputStream(Path, java.util.Set)
 */
@ParametersAreNonnullByDefault
public final class BoxFileOutputStream
    extends OutputStream
{
    private final PipedOutputStream out;
    private final Future<BoxItem.Info> future;
    private Consumer<BoxItem.Info> consumer;

    /**
     * Build an output stream to upload content to an existing file
     *
     * @param executor the executor to use
     * @param file the file to overwrite
     * @throws BoxIOException failed to initialize the object
     */
    public BoxFileOutputStream(final ExecutorService executor,
        final BoxFile file)
        throws BoxIOException
    {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(file);

        final PipedInputStream in = new PipedInputStream(16384);

        try {
            out = new PipedOutputStream(in);
        } catch (IOException e) {
            final BoxIOException exception
                = new BoxIOException("failed to initialize upload", e);
            try {
                in.close();
            } catch (IOException e2) {
                exception.addSuppressed(e2);
            }
            throw exception;
        }

        future = executor.submit(new Callable<BoxItem.Info>()
        {
            @Override
            public BoxItem.Info call()
                throws BoxIOException
            {
                try {
                    file.uploadNewVersion(in);
                    return null;
                } catch (BoxAPIException e) {
                    final BoxIOException exception = BoxIOException.wrap(e);
                    try {
                        in.close();
                    } catch (IOException e2) {
                        e.addSuppressed(e2);
                    }
                    throw exception;
                }
            }
        });
    }

    /**
     * Build an output stream to upload content to a new file
     *
     * @param executor the executor to use
     * @param parent the directory where the file is to be created
     * @param fileName the name of the file to create
     * @throws BoxIOException failed to initialize the object
     */
    public BoxFileOutputStream(final ExecutorService executor,
        final BoxFolder parent, final String fileName, Consumer<BoxItem.Info> consumer)
        throws BoxIOException
    {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(parent);
        Objects.requireNonNull(fileName);
        this.consumer = consumer;

        final PipedInputStream in = new PipedInputStream(16384);

        try {
            out = new PipedOutputStream(in);
        } catch (IOException e) {
            final BoxIOException exception
                = new BoxIOException("failed to initialize upload", e);
            try {
                in.close();
            } catch (IOException e2) {
                exception.addSuppressed(e2);
            }
            throw exception;
        }

        future = executor.submit(new Callable<BoxItem.Info>()
        {
            @Override
            public BoxItem.Info call()
                throws BoxIOException
            {
                try {
                    BoxItem.Info info = parent.uploadFile(in, fileName);
                    return info;
                } catch (BoxAPIException e) {
                    final BoxIOException exception = BoxIOException.wrap(e);
                    try {
                        in.close();
                    } catch (IOException e2) {
                        e.addSuppressed(e2);
                    }
                    throw exception;
                }
            }
        });
    }

    @Override
    public void write(final int b)
        throws IOException
    {
        try {
            out.write(b);
        } catch (IOException e) {
            future.cancel(true);
            throw new BoxIOException("upload failed", e);
        }
    }

    @Override
    public void write(final byte[] b)
        throws IOException
    {
        try {
            out.write(b);
        } catch (IOException e) {
            future.cancel(true);
            throw new BoxIOException("upload failed", e);
        }
    }

    @Override
    public void write(final byte[] b, final int off, final int len)
        throws IOException
    {
        try {
            out.write(b, off, len);
        } catch (IOException e) {
            future.cancel(true);
            throw new BoxIOException("upload failed", e);
        }
    }

    @Override
    public void flush()
        throws IOException
    {
        try {
            out.flush();
        } catch (IOException e) {
            future.cancel(true);
            throw new BoxIOException("upload failed", e);
        }
    }

    @Override
    public void close()
        throws IOException
    {
        IOException streamException = null;
        IOException futureException = null;

        /*
         * TODO: is this necessary? Let's not take any risks...
         */
        try {
            out.flush();
        } catch (IOException e) {
            streamException = e;
        }

        try {
            out.close();
        } catch (IOException e) {
            if (streamException != null)
                streamException.addSuppressed(e);
            else
                streamException = e;
        }

        try {
            // TODO: seems a little high; make that a copy option?
            BoxItem.Info info = future.get(5L, TimeUnit.SECONDS);
            consumer.accept(info);
        } catch (InterruptedException e) {
            futureException = new BoxIOException("upload interrupted", e);
        } catch (ExecutionException e) {
            futureException = new BoxIOException("upload failed", e.getCause());
        } catch (CancellationException e) {
            futureException = new BoxIOException("upload cancelled", e);
        } catch (TimeoutException e) {
            future.cancel(true);
            futureException = new BoxIOException("upload timeout", e);
        }

        if (futureException != null) {
            if (streamException != null)
                futureException.addSuppressed(streamException);
            throw futureException;
        }

        if (streamException != null)
            throw streamException;
    }
}
