/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package com.github.fge.filesystem.box.webhook.websocket;

import java.io.IOException;
import java.net.URI;
import java.util.function.Consumer;

import javax.websocket.ClientEndpoint;
import javax.websocket.Session;

import vavi.nio.file.watch.webhook.websocket.BasicAuthorizationConfigurator;
import vavi.nio.file.watch.webhook.websocket.StringWebSocketNotification;


/**
 * BoxWebSocketNotification.
 * <p>
 * environment variables
 * <ul>
 * <li> VAVI_APPS_WEBHOOK_WEBSOCKET_BASE_URL
 * <li> VAVI_APPS_WEBHOOK_WEBSOCKET_BOX_PATH
 * </ul>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/07/22 umjammer initial version <br>
 */
@ClientEndpoint(configurator = BasicAuthorizationConfigurator.class)
public class BoxWebSocketNotification extends StringWebSocketNotification {

    private static final String websocketBaseUrl = System.getenv("VAVI_APPS_WEBHOOK_WEBSOCKET_BASE_URL");
    private static final String websocketPath = System.getenv("VAVI_APPS_WEBHOOK_WEBSOCKET_BOX_PATH");

    private static final URI uri = URI.create(websocketBaseUrl + websocketPath);

    private Consumer<String> callback;

    /**
     * @param args
     */
    public BoxWebSocketNotification(Consumer<String> callback, Object... args) throws IOException {
        super(uri, args);
        this.callback = callback;
    }

    @Override
    public void onOpenImpl(Session session) throws IOException {
    }

    @Override
    protected void onNotifyMessageImpl(String notification) throws IOException {
        callback.accept(notification);
    }

    @Override
    protected void onCloseImpl(Session session) throws IOException {
    }
}

/* */
