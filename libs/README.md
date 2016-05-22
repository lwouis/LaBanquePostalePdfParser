Committing dependencies on Git is usually bad practice. It's usually better to rely on build tools (e.g. Gradle or
Maven) to download the required versions off public repos.

Here however, I decided to commit a JAR file of [traprange](https://github.com/thoqbk/traprange) since this project
is not very popular, and the repo could disappear any day.

The build.gradle is still referencing the live repo, but the code is there as a JAR file in case anything goes wrong
with this repo.