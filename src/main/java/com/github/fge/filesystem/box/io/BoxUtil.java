/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package com.github.fge.filesystem.box.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import com.box.sdk.BoxAPIRequest;
import com.box.sdk.BoxAPIResponse;
import com.box.sdk.BoxFile;
import com.box.sdk.BoxFolder;
import com.box.sdk.BoxItem;
import com.box.sdk.ProgressListener;
import com.box.sdk.UploadFileCallback;

import vavi.nio.file.Util;


/**
 * BoxUtil.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/05/28 umjammer initial version <br>
 */
public class BoxUtil {

    private BoxUtil() {
    }

    /**
     * Gets downloading stream for this file.
     *
     * @param file the file will be written.
     */
    public static InputStream getInputStreamForDownload(BoxFile file) {
        return getInputStreamForDownload(file, null);
    }

    /**
     * Gets downloading stream for this file while reporting the progress to a ProgressListener.
     *
     * @param file the file will be written.
     * @param listener a listener for monitoring the download's progress.
     */
    public static InputStream getInputStreamForDownload(BoxFile file, ProgressListener listener) {
        URL url = BoxFile.CONTENT_URL_TEMPLATE.build(file.getAPI().getBaseURL(), file.getID());
        BoxAPIRequest request = new BoxAPIRequest(file.getAPI(), url, "GET");
        BoxAPIResponse response = request.send();
        return new BufferedInputStream(new Util.InputStreamForDownloading(response.getBody(listener), false) {
            @Override
            protected void onClosed() throws IOException {
                response.disconnect();
            }
        }, Util.BUFFER_SIZE);
    }

    /**
     * Gets uploading stream for a new file.
     *
     * TODO {@link java.util.concurrent.Phaser}???
     *
     * @param parent parent folder
     * @param fileName new file name
     * @param consumer for cache, new file info will be given
     */
    public static OutputStream getOutputStreamForUpload(BoxFolder parent, String fileName, Consumer<BoxItem.Info> consumer) {
        return new BufferedOutputStream(new Util.OutputStreamForUploading(null, false) {
            // TODO pool
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<BoxItem.Info> future;
            CountDownLatch latch1 = new CountDownLatch(1);
            CountDownLatch latch2 = new CountDownLatch(1);
            CountDownLatch latch3 = new CountDownLatch(1);
            void init() {
                UploadFileCallback callback = new UploadFileCallback() {
                    @Override
                    public void writeToStream(OutputStream os) throws IOException {
                        out = os;
                        latch1.countDown();
                        try { latch2.await(); } catch (InterruptedException e) { throw new IllegalStateException(e); }
                    }
                };
                future = executor.submit(() -> {
                    BoxItem.Info info = parent.uploadFile(callback, fileName);
                    latch3.countDown();
                    return info;
                });
                try { latch1.await(); } catch (InterruptedException e) { throw new IllegalStateException(e); }
            }

            @Override
            public void write(int b) throws IOException {
                if (out == null) {
                    init();
                }
                super.write(b);
            }

            @Override
            protected void onClosed() throws IOException {
                try {
                    latch2.countDown();
                    latch3.await();
                    out.close();
                    consumer.accept(future.get());
                } catch (InterruptedException | ExecutionException e) {
                    throw new IllegalStateException(e);
                }
            }
        }, Util.BUFFER_SIZE);
    }
}

/* */
