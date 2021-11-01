/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package com.github.fge.filesystem.box;

import vavi.net.auth.BaseLocalAppCredential;
import vavi.net.auth.oauth2.OAuth2AppCredential;
import vavi.util.properties.annotation.Env;
import vavi.util.properties.annotation.PropsEntity;


/**
 * BoxTestAppCredential.
 * <p>
 * uses environment variables instead of "~/vavifuse/box.properties"
 * <ul>
 * <li> TEST_CLIENT_ID
 * <li> TEST_CLIENT_SECRET
 * <li> TEST_REDIRECT_URL
 * <li> TEST_SCOPES
 * </ul>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/05/01 umjammer initial version <br>
 * @see "https://app.box.com/developers/console"
 */
@PropsEntity
public final class BoxTestAppCredential extends BaseLocalAppCredential implements OAuth2AppCredential {

    @Env(name = "TEST_APPLICATION_NAME")
    private String applicationName;

    @Env(name = "TEST_CLIENT_ID")
    private transient String clientId;

    @Env(name = "TEST_CLIENT_SECRET")
    private transient String clientSecret;

    @Env(name = "TEST_REDIRECT_URL")
    private String redirectUrl;

    @Env(name = "TEST_SCOPES")
    private String scope;

    @Override
    public String getApplicationName() {
        return applicationName;
    }

    @Override
    public String getScheme() {
        return "box";
    }

    @Override
    public String getClientId() {
        return clientId;
    }

    @Override
    public String getClientSecret() {
        return clientSecret;
    }

    @Override
    public String getRedirectUrl() {
        return redirectUrl;
    }

    @Override
    public String getOAuthAuthorizationUrl() {
        return null;
    }

    @Override
    public String getOAuthTokenUrl() {
        return null;
    }

    @Override
    public String getScope() {
        return scope;
    }
}

/* */
