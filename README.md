[![Release](https://jitpack.io/v/umjammer/java7-fs-box.svg)](https://jitpack.io/#umjammer/java7-fs-box) [![Actions Status](https://github.com/umjammer/java7-fs-box/workflows/Java%20CI/badge.svg)](https://github.com/umjammer/java7-fs-box/actions) [![Parent](https://img.shields.io/badge/Parent-vavi--apps--fuse-pink)](https://github.com/umjammer/vavi-apps-fuse)

## java7-fs-box

This project is licensed under both LGPLv3 and ASL 2.0. See file LICENSE for more details.

## What this is

This is an implementation of a Java 7
[`FileSystem`](https://docs.oracle.com/javase/7/docs/api/java/nio/file/FileSystem.html) over
[Box.com](https://box.com). This implementation is based on
[java7-fs-base](https://github.com/fge/java7-fs-base).

Note that it does not make use of the Android SDK (see
[here](https://github.com/box/box-java-sdk-v2) but of the [new
API](https://github.com/box/box-java-sdk).

## Install

### jars

 * https://jitpack.io/#umjammer/java7-fs-box

### selenium chrome driver

 * Download the [chromedriver executable](https://chromedriver.chromium.org/downloads) and locate it into some directory.
   * Don't forget to run jvm with jvm argument `-Dwebdriver.chrome.driver=/usr/local/bin/chromedriver`.

## Usage

First, get box account, then create [box app](https://app.box.com/developers/console).

Next, prepare 2 property files.

 * application credential

```shell
$ cat ${HOME}/.vavifuse/box.properties
box.clientId=your_client_id
box.clientSecret=your_client_secret
box.redirectUrl=http://localhost:30001
```

 * user credential

```shell
$ cat ${HOME}/.vavifuse/credentials.properties
box.password.xxx@yyy.zzz=your_password
```

Then write your code! Here is a short example (imports omitted for brevity):

```java
public class Main {

    public static void main(final String[] args) throws IOException {
        String email = "xxx@yyy.zzz";

        URI uri = URI.create("box:///?id=" + email);

        FileSystem fs = FileSystems.newFileSystem(uri, env);
            :
    }
}
```

### See also

https://github.com/umjammer/vavi-apps-fuse/blob/master/vavi-nio-file-gathered/src/test/java/vavi/nio/file/box/Main.java

## TODO

  * ~~dev token authenticator~~
