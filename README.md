# VPlay

A desktop video player built in Java using Swing and `vlcj`, featuring playback controls, seek tracking, volume adjustment, and standalone native packaging via `jpackage`.

## Features

- **Media Playback:** Play, pause, resume, and seek across local video files.
- **Audio Control:** Safe volume scaling from `0` to `100`.
- **Native VLC Integration:** Automatic discovery of system LibVLC libraries across macOS and Windows (`jna.library.path` fallback support).
- **Standalone Packaging:** Configured with Maven Shade and `jpackage` to produce a self-contained desktop application with custom branding and icons.

## Tech Stack

- **Language:** Java 27
- **GUI Framework:** Java Swing (`JFrame`, `JPanel`)
- **Media Engine:** `vlcj` (`CallbackMediaPlayerComponent`)
- **Build & Packaging:** Apache Maven (`maven-shade-plugin`), `jpackage`

## Project Structure

```text
VPlay/
├── pom.xml
└── src/
    └── main/
        ├── java/
        │   └── vplay/
        │       ├── Main.java
        │       ├── player/
        │       │   └── VideoPlayer.java
        │       └── ui/
        │           └── MainFrame.java
        └── resources/
            └── images/
                └── logo.png