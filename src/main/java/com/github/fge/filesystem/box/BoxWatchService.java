/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package com.github.fge.filesystem.box;

import java.io.IOException;

import com.box.sdk.BoxFolder;

import vavi.nio.file.watch.webhook.WebHookBaseWatchService;
import vavi.util.Debug;


/**
 * BoxWatchService.
 * <p>
 * system properties
 * <ul>
 * <li> vavi.nio.file.watch.webhook.NotificationProvider.box
 * </ul>
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/07/22 umjammer initial version <br>
 */
public class BoxWatchService extends WebHookBaseWatchService<String> {

    private static final String WEBHOOK_NOTIFICATION_PROVIDER =
            System.getProperty("vavi.nio.file.watch.webhook.NotificationProvider.box", ".box.webhook.websocket");

//    private BoxFolder.Info client;

//    private String savedStartPageToken;

    /** */
    public BoxWatchService(BoxFolder.Info client) throws IOException {
//        this.client = client;

        setupNotification(this, WEBHOOK_NOTIFICATION_PROVIDER);
    }

    @Override
    protected void onNotifyMessage(String notification) throws IOException {
Debug.println(">> notification: done");
    }

    @Override
    public void close() throws IOException {
        if (isOpen()) {
            super.close();
        }
    }
}
