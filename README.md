# maven-non-destructive-mojo-executor
MojoExecutor implementation working around some issues with multi-threaded building in Maven

This is a maven extension that can be plugged into a maven installation's lib/ext folder and works around the problem mentioned in my [maven-mojo-jojo](https://github.com/fvanderveen/maven-mojo-jojo) repo.

## Practical

Note that this project is built against maven 3.3.9. It may work in (slightly) older versions and might keep working in newer versions, but for now I only tested this with 3.3.9.
