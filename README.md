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
- `buildTarget/compile`
- `buildTarget/cleanCache`
- `buildTarget/jvmTestEnvironment`
- `buildTarget/test`
- `buildTarget/javacOptions`
- `workspace/buildTargets`
- `workspace/reload`


## Architecture

See [ARCHITECTURE.md](./ARCHITECTURE.md)

## Launch the Build Server for Gradle
### Specify the Plugin Location
The main class of the build server is `com.microsoft.java.bs.core.Launcher`. When you launch the server, you need to specify the location of the Gradle plugin via the system property `plugin.dir`. By default, you will find the plugin jar at `server/build/libs/plugins/plugin.jar` after building the project.
### Preferences

A [Preferences](./server/src/main/java/com/microsoft/java/bs/core/internal/model/Preferences.java) object can be put into the data field of the `build/initialize` request for customization. Please check the comments in the code for the meaning of each preference.

## Contributing

This project welcomes contributions and suggestions.  Most contributions require you to agree to a
Contributor License Agreement (CLA) declaring that you have the right to, and actually do, grant us
the rights to use your contribution. For details, visit https://cla.opensource.microsoft.com.

When you submit a pull request, a CLA bot will automatically determine whether you need to provide
a CLA and decorate the PR appropriately (e.g., status check, comment). Simply follow the instructions
provided by the bot. You will only need to do this once across all repos using our CLA.

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/).
For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or
contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.

Please check [CONTRIBUTING.md](./CONTRIBUTING.md) for more details about how to setup and develop the project.

## Trademarks

This project may contain trademarks or logos for projects, products, or services. Authorized use of Microsoft 
trademarks or logos is subject to and must follow 
[Microsoft's Trademark & Brand Guidelines](https://www.microsoft.com/en-us/legal/intellectualproperty/trademarks/usage/general).
Use of Microsoft trademarks or logos in modified versions of this project must not cause confusion or imply Microsoft sponsorship.
Any use of third-party trademarks or logos are subject to those third-party's policies.

## Telemetry

The Build Server for Gradle will not send telemetry by itself. But it will send telemetry event to its client. If you don't want this behavior, set the system property `disableServerTelemetry`.
