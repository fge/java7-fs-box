package com.github.fge.filesystem.box.io;

import com.box.sdk.BoxFile;
import com.github.fge.filesystem.box.exceptions.BoxIOException;
import com.github.fge.filesystem.driver.FileSystemDriver;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Wrapper over {@link BoxFile#download(OutputStream)}
 *
 * <p>Necessary to provide an {@link InputStream} for {@link
 * Files#newInputStream(Path, OpenOption...)}.</p>
 *
 * @see FileSystemDriver#newInputStream(Path, OpenOption...)
 */
@ParametersAreNonnullByDefault
public final class BoxFileInputStream
    extends InputStream
{
    private final Future<Void> future;
    private final PipedOutputStream out;
    private final PipedInputStream in;

    public BoxFileInputStream(final Future<Void> future,
        final PipedOutputStream out)
        throws BoxIOException
    {
        this.future = Objects.requireNonNull(future);
        this.out = Objects.requireNonNull(out);
        // TODO: make pipe size constant
        in = new PipedInputStream(16384);

        try {
            out.connect(in);
        } catch (IOException e) {
            try {
                in.close();
            } catch (IOException e2) {
                e.addSuppressed(e2);
            }
            try {
                out.close();
            } catch (IOException e3) {
                e.addSuppressed(e3);
            }
            throw new BoxIOException("failed to initialize download", e);
        }
    }

    @Override
    public int read()
        throws IOException
    {
        try {
            return in.read();
        } catch (IOException e) {
            future.cancel(true);
            throw new BoxIOException("download failure", e);
        }
    }

    @Override
    public int read(final byte[] b)
        throws IOException
    {
        try {
            return in.read(b);
        } catch (IOException e) {
            future.cancel(true);
            throw new BoxIOException("download failure", e);
        }
    }

    @Override
    public int read(final byte[] b, final int off, final int len)
        throws IOException
    {
        try {
            return in.read(b, off, len);
        } catch (IOException e) {
            future.cancel(true);
            throw new BoxIOException("download failure", e);
        }
    }

    @Override
    public long skip(final long n)
        throws IOException
    {
        try {
            return in.skip(n);
        } catch (IOException e) {
            future.cancel(true);
            throw new BoxIOException("download failure", e);
        }
    }

    @Override
    public int available()
        throws IOException
    {
        try {
            return in.available();
        } catch (IOException e) {
            future.cancel(true);
            throw new BoxIOException("download failure", e);
        }
    }

    /**
     * Closes this input stream and releases any system resources associated
     * with the stream.
     * <p> The <code>close</code> method of <code>InputStream</code> does
     * nothing.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void close()
        throws IOException
    {
        IOException streamException = null;
        IOException futureException = null;

        try {
            in.close();
        } catch (IOException e) {
            streamException = e;
        }

        try {
            if (!future.isDone())
                future.cancel(true);
            future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            futureException = new BoxIOException("donwload interrupted", e);
        } catch (ExecutionException e) {
            futureException = new BoxIOException("download failure",
                e.getCause());
        } catch (CancellationException e) {
            futureException = new BoxIOException("download cancelled", e);
        }

        if (futureException != null) {
            if (streamException != null)
                futureException.addSuppressed(streamException);
            throw futureException;
        }

        if (streamException != null)
            throw streamException;
    }

    @Override
    public synchronized void mark(final int readlimit)
    {
        in.mark(readlimit);
    }

    @Override
    public synchronized void reset()
        throws IOException
    {
        try {
            in.reset();
        } catch (IOException e) {
            future.cancel(true);
            throw new BoxIOException("download failure", e);
        }
    }

    @Override
    public boolean markSupported()
    {
        return in.markSupported();
    }
}
