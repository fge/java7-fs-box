/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package com.github.fge.filesystem.box;

import vavi.net.auth.UserCredential;


/**
 * DummyUserCredential.
 *
 * uses BOX_DEVELOPER_TOKEN, all fields are ignored.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/05/02 umjammer initial version <br>
 */
public final class DummyUserCredential implements UserCredential {

    @Override
    public String getId() {
        return null;
    }

    @Override
    public String getPassword() {
        return null;
    }
}

/* */
