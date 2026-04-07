# Mouse Monitor

A Windows desktop application for collecting mouse dynamics data for behavioural biometric research.

Built as part of an MSc thesis on intruder detection from mouse dynamics.

## Features

- **10 ms polling** via Win32 API (`GetCursorPos`, `GetAsyncKeyState`) for high-resolution mouse trajectory capture
- **Run-length encoding** — consecutive identical states are compressed into a single row with a repeat count, reducing file size without losing temporal information
- **Automatic session management** — detects idle periods (60s threshold) and splits recordings into separate sessions; resumes automatically when activity returns
- **System metadata capture** — records screen resolution, DPI, and Windows mouse speed setting per session for cross-device comparability
- **Parquet export** — converts CSV sessions to Apache Parquet with Snappy compression for efficient storage and analysis
- **System tray operation** — minimises to tray during recording, with visual status indicator
- **Privacy controls** — window titles are captured for context but can be stripped during post-processing
- **Self-updating** — checks for new versions from a remote server and installs silently

## Output Format

Each session produces a CSV file in `~/Documents/MouseMonitor/sessions/` with the following columns:

| Column   | Type   | Description                                      |
|----------|--------|--------------------------------------------------|
| start    | long   | Start timestamp of this state (ms since epoch)   |
| end      | long   | End timestamp of this state (ms since epoch)     |
| x        | int    | Cursor X position (pixels)                       |
| y        | int    | Cursor Y position (pixels)                       |
| left     | int    | Left mouse button state (0/1)                    |
| right    | int    | Right mouse button state (0/1)                   |
| middle   | int    | Middle mouse button state (0/1)                  |
| window   | string | Title of the foreground window                   |
| repeats  | int    | Number of consecutive identical 10 ms samples    |

A companion metadata JSON file records the system configuration at capture time (screen resolution, DPI, mouse speed).

## Building

Requires JDK 17+ and Gradle.

```bash
./gradlew fatJar
```

The fat JAR is output to `build/libs/MouseMonitor-1.0-SNAPSHOT-all.jar`.

To build an MSI installer:

```bash
./gradlew jpackage
```

## Tech Stack

- Kotlin / JVM 17
- JavaFX (GUI)
- JNA (Win32 native mouse polling)
- jnativehook (global input hooks)
- Apache Parquet + Avro (data export)
- OkHttp (update mechanism)

## License

This software was developed for academic research purposes.
