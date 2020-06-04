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
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import com.github.fge.filesystem.box.provider.BoxFileSystemProvider;

import static vavi.nio.file.Base.testAll;


/**
 * Test1.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/05/01 umjammer initial version <br>
 */
class Test1 {

    /**
     * environment variable
     * <ul>
     * <li> TEST_ACCOUNT
     * <li> TEST_PASSWORD
     * <li> TEST_CLIENT_ID
     * <li> TEST_CLIENT_SECRET
     * <li> TEST_REDIRECT_URL
     * </ul>
     */
    @Test
    @DisabledIfEnvironmentVariable(named = "GITHUB_WORKFLOW", matches = ".*")
    void test01() throws Exception {
        String email = System.getenv("TEST_ACCOUNT");

        Map<String, Object> env = new HashMap<>();
        env.put(BoxFileSystemProvider.ENV_APP_CREDENTIAL, new BoxTestAppCredential());
        env.put(BoxFileSystemProvider.ENV_USER_CREDENTIAL, new BoxTestUserCredential(email));

        URI uri = URI.create("box:///");

        testAll(new BoxFileSystemProvider().newFileSystem(uri, env));
    }

    /**
     * environment variable
     * <ul>
     * <li> TEST_DEVELOPER_TOKEN
     * </ul>
     * @see "https://app.box.com/developers/console/app/216798/configuration"
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "TEST_DEVELOPER_TOKEN", matches = ".+")
    void test02() throws Exception {
        System.setProperty("vavi.nio.file.box.BoxFileSystemRepository.oauth2", "com.github.fge.filesystem.box.BoxDevOAuth2");

        Map<String, Object> env = new HashMap<>();
        env.put(BoxFileSystemProvider.ENV_APP_CREDENTIAL, new DummyAppCredential());
        env.put(BoxFileSystemProvider.ENV_USER_CREDENTIAL, new DummyUserCredential());

        URI uri = URI.create("box:///?id=dummy");

        testAll(new BoxFileSystemProvider().newFileSystem(uri, env));

        System.setProperty("oAuth2ClassName", "");
    }
}

/* */
