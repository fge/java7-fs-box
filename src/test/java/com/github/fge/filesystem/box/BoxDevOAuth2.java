/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package com.github.fge.filesystem.box;

import java.io.IOException;

import com.box.sdk.BoxAPIConnection;

import vavi.net.auth.UserCredential;
import vavi.net.auth.oauth2.OAuth2;
import vavi.net.auth.oauth2.OAuth2AppCredential;


/**
 * BoxDevOAuth2.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/02/29 umjammer initial version <br>
 * @see "https://app.box.com/developers/console/app/216798/configuration"
 */
public class BoxDevOAuth2 implements OAuth2<UserCredential, BoxAPIConnection> {

    /** */
    public BoxDevOAuth2(OAuth2AppCredential appCredential) {
    }

    @Override
    public BoxAPIConnection authorize(UserCredential userCredential) throws IOException {
        return new BoxAPIConnection(System.getenv("TEST_DEVELOPER_TOKEN"));
    }
}

/* */
