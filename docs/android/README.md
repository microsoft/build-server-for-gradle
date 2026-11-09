# Android support

Gradle Build Server lists each Android build variant as a separate BSP target.
The Android model uses reflection, so the server has no compile dependency on
the Android Gradle Plugin. Ordinary Java and Scala Gradle projects continue to
use the same BSP connection.

## Set up

Build the server distribution with `./gradlew :server:installDist`. Put a
`.bsp/gradle-bsp.json` file at the Android project root and point its `argv`
to the installed `gradle-bsp` command. Set `ANDROID_HOME` or
`ANDROID_SDK_ROOT`, or put `adb` on `PATH`. Connect a device or start an
emulator before Run or Debug.

The [p5 example](https://github.com/aelam/vscode-bsp/tree/main/examples/multi-bsp/p5)
contains a small Android application and a BSP connection file.

## Build contexts

The optional `vscode-bsp.build-context` extension provides
`workspace/contextOptions` and `workspace/resolveContext`. It keeps the
existing variant targets and adds a device choice for application targets.
Device options use the common `execution-destination` metadata format.
The client sends the resolved `executionContext.id` with Compile, Test, Run,
or Debug. The server checks the operation and target before it executes.

The `build` policy assembles the variant before Run or Debug. The `reuse`
policy skips Gradle and uses the existing APK. Both policies install the APK
on the selected device. The server reads `output-metadata.json` to find the
APK for the exact variant and application ID.

A client without build contexts can use standard BSP Run and Debug when one
device is connected. With more than one device, it needs the build context
extension to choose one.

## Debug

The server launches the Android application with JDWP enabled. It forwards
JDWP through `adb` and returns a local DAP address from
`debugSession/start`. The BSP client connects to that address. Java source
breakpoints, stepping, stack frames, and variables work in the p5 example.

Expression evaluation, conditional breakpoints, hot code replace, and Kotlin
source mapping are not implemented. Android library and test variants do not
offer Run or Debug. APK discovery requires Gradle's output metadata.
