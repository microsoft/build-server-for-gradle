# Build Server for Gradle

An implementation of the Build Server Protocol for Gradle.

## Requirement

JDK 17 or higher is required to build and launch the Build Server for Gradle.

## Supported Requests

Following BSP requests are supported in the current implementation:

- `build/initialize`
- `build/initialized`
- `build/shutdown`
- `build/exit`
- `buildTarget/sources`
- `buildTarget/resources`
- `buildTarget/outputPaths`
- `buildTarget/dependencyModules`
- `buildTarget/dependencySources`
- `buildTarget/compile`
- `buildTarget/cleanCache`
- `buildTarget/javacOptions`
- `buildTarget/scalacOptions`
- `buildTarget/test`
- `workspace/buildTargets`
- `workspace/reload`

## Architecture

See [ARCHITECTURE.md](./ARCHITECTURE.md)

## Launch the Build Server for Gradle

Build the distribution and run its command:

```sh
./gradlew :server:installDist
server/build/install/gradle-bsp/bin/gradle-bsp --version
server/build/install/gradle-bsp/bin/gradle-bsp
```

The command starts the BSP server on standard input and output. The distribution
contains the Gradle plugin and its init script, so the command finds them without
extra JVM options. `--help` prints the available command options. The `distZip`
and `distTar` tasks create archives with the same layout.

This repository includes `.bsp/gradle-bsp.json`. After `installDist`, BSP clients
that use this repository as the workspace root can start the server from that
file. To connect another Gradle project, copy the file to its `.bsp` directory
and replace the command with the absolute path to your distribution:

```json
{
  "name": "gradle-build-server",
  "version": "0.4.0",
  "bspVersion": "2.1.0-M4",
  "languages": ["java", "scala"],
  "argv": ["/absolute/path/to/server/build/install/gradle-bsp/bin/gradle-bsp"]
}
```

### Specify the Plugin Location

The main class of the build server is `com.microsoft.java.bs.core.Launcher`.
When you start that class directly, set the `plugin.dir` system property to the
directory with `init.gradle` and `plugin-${version}.jar`. The build puts these
files in `server/build/libs/plugins`. The distribution command sets this property
from its own location.

### Preferences

A [Preferences](./server/src/main/java/com/microsoft/java/bs/core/internal/model/Preferences.java) object can be put into the data field of the `build/initialize` request for customization. Please check the comments in the code for the meaning of each preference.

### Transport Method

The Gradle Build Server supports two types of transport methods: standard input/output and named pipe (unix socket). By default, the server uses the standard input/output transport if no arguments are specified.

To use the named pipe (unix socket) transport, start the server with the argument `--pipe=<pipeName>`. If the `--pipe=` option is provided but `pipeName` left empty, the server defaults to using the standard input/output transport.

## Contributing

This project welcomes contributions and suggestions. Most contributions require you to agree to a Contributor License Agreement (CLA) declaring that you have the right to, and actually do, grant us the rights to use your contribution. For details, visit https://cla.opensource.microsoft.com.

When you submit a pull request, a CLA bot will automatically determine whether you need to provide a CLA and decorate the PR appropriately (e.g., status check, comment). Simply follow the instructions provided by the bot. You will only need to do this once across all repos using our CLA.

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/). For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.

Please check [CONTRIBUTING.md](./CONTRIBUTING.md) for more details about how to setup and develop the project.

## Developer Documentation

Assisting documents to explaining the working of Gradle Build Server can be found in [developer documentation](./docs/developer.md). This will be helpful for you if you're planning to integrate Gradle Build Server or contribute to the project.

## Trademarks

This project may contain trademarks or logos for projects, products, or services. Authorized use of Microsoft trademarks or logos is subject to and must follow [Microsoft's Trademark & Brand Guidelines](https://www.microsoft.com/en-us/legal/intellectualproperty/trademarks/usage/general). Use of Microsoft trademarks or logos in modified versions of this project must not cause confusion or imply Microsoft sponsorship. Any use of third-party trademarks or logos are subject to those third-party's policies.

## Telemetry

The Build Server for Gradle will not send telemetry by itself. But it will send telemetry event to its client. If you don't want this behavior, set the system property `disableServerTelemetry`.
