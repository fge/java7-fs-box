/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package com.github.fge.filesystem.box.webhook.websocket;

import java.io.IOException;
import java.util.function.Consumer;

import vavi.nio.file.watch.webhook.Notification;
import vavi.nio.file.watch.webhook.NotificationProvider;


/**
 * BoxWebSocketNotificationProvider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/07/22 umjammer initial version <br>
 */
public class BoxWebSocketNotificationProvider implements NotificationProvider {

    @Override
    public <T> Notification<T> getNotification(Consumer<T> callback, Object... args) throws IOException {
        return Notification.class.cast(new BoxWebSocketNotification(Consumer.class.cast(callback), args));
    }
}

/* */
