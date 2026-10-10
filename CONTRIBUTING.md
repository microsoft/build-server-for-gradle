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

## IssueLens Team-Memory Queue

The opt-in [team-memory workflow](.github/workflows/team-memory-post-merge.yml)
dispatches requests after ordinary pushes to this repository's default branch,
`develop`. It preserves the existing `ISSUELENS_TEAM_MEMORY_ENABLED == 'true'`
gate and rejects branch creation, deletion, forced pushes, and mismatched
workflow/head revisions. The source does not check out code, invoke IssueLens,
or maintain the wiki directly. The
[Java Pack coordinator](https://github.com/microsoft/vscode-java-pack/blob/main/.github/workflows/team-memory-coordinator.yml)
runs on `main`, independently of the source branch, and owns validation,
invocation, and the shared wiki queue.

### Maintainer Setup

Configure these prerequisites before merging with the existing source opt-in
set to `true`: the next eligible push switches to queue dispatch immediately.
This migration does not change live opt-in values or configure credentials.

| Location | Prerequisite |
| --- | --- |
| `microsoft/build-server-for-gradle` | Variable `ISSUELENS_DISPATCH_APP_CLIENT_ID` and secret `ISSUELENS_DISPATCH_APP_PRIVATE_KEY` for a dedicated dispatch App installed only on `microsoft/vscode-java-pack`, with Contents read and Actions write. The workflow requests a token limited to that repository and those permissions. |
| `microsoft/vscode-java-pack` | Separate secrets `ISSUELENS_SOURCE_READ_APP_CLIENT_ID` and `ISSUELENS_SOURCE_READ_APP_PRIVATE_KEY` for the coordinator's source-read App, installed on this source with Actions, Contents, and Pull requests read access. The central coordinator opt-in must also be enabled. |

The source `GITHUB_TOKEN` cannot dispatch across repositories; it has no granted
permissions in this workflow. Do not reuse the hosted IssueLens App key or the
central source-read App credentials for dispatch. A successful Java Pack
own-repository run does not establish readiness for external source reads.
The coordinator's allowlist already includes `microsoft/build-server-for-gradle`;
the source request cannot expand that allowlist or grant wiki write authority.
The [shared team-memory policy](.github/issuelens/team-memory.md) remains unchanged.

The request contains only five strings: source repository, source run ID and
attempt, requested `push_before`, and source run-head `push_after`. Central
validation verifies the source run/head and authorizes ancestor reconciliation;
`push_before` is not attested original-event provenance.

Dispatch acknowledgement means only that GitHub accepted the request, not that
the coordinator queued or completed maintenance. The dispatcher makes one
bounded POST with no automatic retry. For an ambiguous dispatch failure, inspect
the central runs before retrying.

For manual merged-PR maintenance, use **Run workflow** on Java Pack's
`team-memory-coordinator.yml` at `main`, with `source_repository` set to
`microsoft/build-server-for-gradle` and `pull_request_number` set to the merged
source PR number; leave the automatic source-run/range inputs empty. There is
no local manual invocation path that bypasses the shared queue.