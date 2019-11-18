[![Release](https://jitpack.io/v/umjammer/java7-fs-box.svg)](https://jitpack.io/#umjammer/java7-fs-box) [![Actions Status](https://github.com/umjammer/java7-fs-box/workflows/Java%20CI/badge.svg)](https://github.com/umjammer/java7-fs-box/actions)

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

## Status

In active development.

The basic I/O operations work: you can download and upload files, create and delete entries, and a
few other things.

The status is as of yet unclear and highly tied to java7-fs-base, so please refer to this project
for more details.

## Building

Right now, this project uses the latest HEAD of java7-fs-base. You therefore need to clone it (see
link above), then build and install it in your local maven repo using:

```
# Replace ./gradlew with gradlew.bat if you run Windows
./gradlew clean test install
```

Then clone this project.

