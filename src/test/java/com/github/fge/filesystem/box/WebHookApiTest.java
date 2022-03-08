/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package com.github.fge.filesystem.box;

import java.net.URI;

import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxFolder;
import com.box.sdk.BoxItem;
import com.box.sdk.BoxWebHook;

import vavi.net.auth.UserCredential;
import vavi.net.auth.oauth2.OAuth2AppCredential;
import vavi.net.auth.oauth2.box.BoxLocalAppCredential;
import vavi.net.auth.oauth2.box.BoxOAuth2;
import vavi.net.auth.web.box.BoxLocalUserCredential;
import vavi.util.Debug;


/**
 * WebHookApiTest. box v2
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/07/03 umjammer initial version <br>
 * @see "https://app.box.com/developers/console/app/216798/webhooks"
 * @see "https://developer.box.com/guides/webhooks/"
 */
public class WebHookApiTest {

    static String websocketBaseUrl = System.getenv("VAVI_APPS_WEBHOOK_WEBSOCKET_BASE_URL");
    static String websocketPath = System.getenv("VAVI_APPS_WEBHOOK_WEBSOCKET_BOX_PATH");
    static String email = System.getenv("TEST_ACCOUNT");

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {

        UserCredential userCredential = new BoxLocalUserCredential(email);
        OAuth2AppCredential appCredential = new BoxLocalAppCredential();

        BoxAPIConnection api = new BoxOAuth2(appCredential).authorize(userCredential);

        BoxFolder.Info rootInfo = BoxFolder.getRootFolder(api).getInfo();

        // create
        URI uri = URI.create(websocketBaseUrl + websocketPath);
        // Listen for file upload events in the specified folder
        BoxFolder rootFolder = new BoxFolder(api, rootInfo.getID());
        for (BoxItem.Info i : rootFolder.getChildren()) {
            if (i.getName().equals("TEST_WEBHOOK")) {
System.out.println("rmdir " + i.getName());
                ((BoxFolder.Info) i).getResource().delete(false);
            }
        }
System.out.println("mkdir " + "TEST_WEBHOOK");
        BoxFolder.Info newFolderInfo = rootFolder.createFolder("TEST_WEBHOOK");
        BoxFolder newFolder = newFolderInfo.getResource();
        // cannot set to root folder!
System.out.println("[create] webhook");
        BoxWebHook.Info info = BoxWebHook.create(newFolder, uri.toURL(),
            BoxWebHook.Trigger.FILE_UPLOADED,
            BoxWebHook.Trigger.FILE_DELETED,
            BoxWebHook.Trigger.FILE_RENAMED,
            BoxWebHook.Trigger.FOLDER_CREATED,
            BoxWebHook.Trigger.FOLDER_DELETED,
            BoxWebHook.Trigger.FOLDER_RENAMED
        );
Debug.println(info.getID());

        // list
System.out.println("[ls] webhook");
        BoxWebHook.Info webhookInfo = null;
        Iterable<BoxWebHook.Info> webhooks = BoxWebHook.all(api);
        for (BoxWebHook.Info i : webhooks) {
Debug.println(i.getID());
            webhookInfo = i;
        }

System.out.println("mkdir " + "TEST_WEBHOOK/" + "NEW FOLDER");
        newFolder.createFolder("NEW FOLDER");

        // update
System.out.println("[update] webhook");
        BoxWebHook webhook = new BoxWebHook(api, webhookInfo.getID());
        BoxWebHook.Info preInfo = webhook.getInfo();
        preInfo.setAddress(uri.toURL()); // same address causes NPE
        webhook.updateInfo(preInfo);

        // delete
System.out.println("[delete] webhook");
        webhook.delete();

        Thread.sleep(5000);

System.out.println("rm -rf " + newFolderInfo.getName());
        newFolder.delete(true);
    }
}

/* */
