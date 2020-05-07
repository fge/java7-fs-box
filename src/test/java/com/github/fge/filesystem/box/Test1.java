/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package com.github.fge.filesystem.box;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.github.fge.filesystem.box.provider.BoxFileSystemProvider;

import static vavi.nio.file.Base.testAll;


/**
 * Test1.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/05/01 umjammer initial version <br>
 */
class Test1 {

    @Test
    void test01() throws Exception {
        String email = System.getenv("TEST_ACCOUNT");

        Map<String, Object> env = new HashMap<>();
        env.put(BoxFileSystemProvider.ENV_APP_CREDENTIAL, new BoxTestAppCredential());
        env.put(BoxFileSystemProvider.ENV_USER_CREDENTIAL, new BoxTestUserCredential(email));

        URI uri = URI.create("box:///");

        testAll(new BoxFileSystemProvider().newFileSystem(uri, env));
    }
}

/* */
