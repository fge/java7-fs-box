package com.github.fge.filesystem.box.io;

import com.box.boxjavalibv2.dao.BoxItem;
import com.github.fge.filesystem.box.exceptions.BoxIOException;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public final class BoxFileUploadOutputStream
    extends OutputStream
{
    private final Future<BoxItem> future;
    private final PipedOutputStream out;

    public BoxFileUploadOutputStream(final Future<BoxItem> future,
        final PipedOutputStream out)
    {
        this.future = future;
        this.out = out;
    }

    @Override
    public void write(final int b)
        throws IOException
    {
        try {
            out.write(b);
        } catch (IOException e) {
            future.cancel(true);
            throw new BoxIOException("upload; write failure", e);
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
            throw new BoxIOException("upload: write failure", e);
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
            throw new BoxIOException("upload: write failure", e);
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
            throw new BoxIOException("upload: flush failure", e);
        }
    }

    @Override
    public void close()
        throws IOException
    {
        IOException streamException = null;
        IOException futureException = null;

        try {
            out.close();
        } catch (IOException e) {
            streamException = e;
        }

        if (!future.isDone())
            future.cancel(true);

        try {
            future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            futureException = new BoxIOException("upload interrupted", e);
        } catch (ExecutionException e) {
            futureException = new BoxIOException("upload threw an exception",
                e.getCause());
        } catch (CancellationException e) {
            futureException = new BoxIOException("upload was cancelled", e);
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
