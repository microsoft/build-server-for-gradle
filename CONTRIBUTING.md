# Contributing

## Contributing Fixes

If you are interested in writing code to fix issues, please check the following content to see how to set up the developing environment. If you are interested in understanding how the Gradle Build Server works under the hood visit the [developer documentation](./docs/developer.md).

### Requirement

- JDK 17 or higher

### Architecture

See [ARCHITECTURE.md](./ARCHITECTURE.md).

### Build the Project

You can run `./gradlew clean build` to build the project.

### Debugging the Plugin

If you want to debug the plugin, set the system property `bsp.plugin.debug.enabled` to `true`. Then you can attach to the Gradle daemon process via port `5005`.

If you are using VS Code, launch the configuration named 'Attach Plugin' after the build server is started.