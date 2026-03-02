[![Java CI with Maven](https://github.com/andirady/pom-cli/actions/workflows/build.yml/badge.svg)](https://github.com/andirady/pom-cli/actions/workflows/build.yml)

# pom-cli 🪄

**pom-cli** lets you quickly create and update a ``pom.xml`` file without manual XML editing. It's written in Java and compiled to native code with [GraalVM](https://www.graalvm.org/).

![Demo for spring-boot](docs/assets/terminal-recording.gif)

## Installation

### Unix-like

Run the following script:

```
curl -s https://raw.githubusercontent.com/andirady/pom-cli/refs/heads/main/install.sh | bash
```

### MacOS, Windows

Download the binaries from the [Releases](https://github.com/andirady/pom-cli/releases) page.

### Docker

You can also run pom-cli using Docker:

```bash
docker run --rm -it --user $(id -u):$(id -g) -v $PWD:/workspace ghcr.io/andirady/pom-cli [ARGS...]
```

## Usage

### Quick start

```bash
# Create pom.xml with coordinates inferred from current folder name
pom id com.example:.

# Add dependencies and plugins
pom add org.junit.jupiter:junit-jupiter --test
pom plug org.apache.maven.plugins:maven-surefire-plugin

# Inspect current project ID
pom id
```

### Setting project ID

The project ID can be set using the `id` command. This command will create a new `pom.xml` if it does not exist yet.

```bash
# Set group ID and artifact ID, version defaults to 0.0.1-SNAPSHOT
pom id com.example:my-app

# Set full ID
pom id com.example:my-app:1.0.0

# Specifies packaging, which if not specified will use `jar`
pom id com.example:my-webapp --as=war

# Set artifact ID to current directory name
pom id .

# Set or rename artifact ID to current directory name
pom id com.example:.
pom id com.example:.:1.0.0
```

By default, if the ``group_id`` is not specified, ``unnamed`` will be used,
and if the version is not specified, ``0.0.1-SNAPSHOT`` will be used.
To set different defaults, you can use:
- ``POM_CLI_DEFAULT_GROUP_ID`` for ``group_id``
- ``POM_CLI_DEFAULT_VERSION`` for version

```bash
export POM_CLI_DEFAULT_GROUP_ID=com.example
export POM_CLI_DEFAULT_VERSION=1.2.3-SNAPSHOT
cd my-app
pom id .
# The pom will have ID com.example:my-app:1.2.3-SNAPSHOT
```

If the current folder belongs to a multi-module maven project,
the ``<parent>`` element will be added to the pom. For example:
```bash
cd hello
pom id --as=pom com.example:.
# pom com.example:hello:0.0.1-SNAPSHOT
mkdir api
cd $_
pom id hello-api
# The pom will have the `<parent>` element set and `<groupId>` omitted.
```

If the project is meant to be standalone, then you can use the ``--standalone`` flag
```bash
mkdir hello/sample
cd $_
pom id --standalone .
# The pom will use the default group ID and no `<parent>` element will be added.
```

### Reading project ID

If the `pom.xml` already exists, the ID of the POM can be retrieved using `id` command without any arguments.

```bash
pom id
```

### Adding dependencies

Dependencies can be added using the `add` command.

This command will create a new `pom.xml` if it does not exist yet.
The new `pom.xml` will use the default group ID and the folder name as artifact ID.

```bash
# Add compile dependency
pom add info.picocli:picocli

# Adding dependencies leveraging shell expansion
pom add org.apache.logging:log4j-{api,core}

# Add scoped dependency
pom add --test org.junit.jupiter:junit-jupiter
pom add --provided org.junit.jupiter:junit-jupiter
pom add --runtime org.junit.jupiter:junit-jupiter

# Add from maven metadata in a jar file
pom add /path/to/file.jar

# Add by path to maven module
pom add /path/to/module
pom add /path/to/module/pom.xml
```

For projects that are packaged as "pom", the dependencies will be added
to the ``dependencyManagement`` section.

> [!NOTE]
> Some projects use `/project/dependencies` so submodules inherit dependencies,
> but this tool intentionally prefers declaring dependencies explicitly in each module.

If version is not specified, the latest version of the artifact will be used.
If there is a parent pom, and it already included the dependency version,
no version will be added.

If the ``pom.xml`` is a child pom, dependency can be added by just specifying the artifact ID.
For example, if the parent pom contains the following declaration:

```xml
  <dependencyManagement>
    <dependencies>
      ...
      <dependency>
        <groupId>com.example</groupId>
        <artifactId>some-api</artifactId>
        <version>1.0.0</version>
      </dependency>
      ...
    </dependencies>
  </dependencyManagement>
```

Running this command:

```bash
pom add some-api
```

will make the target ``pom.xml`` contain the following:

```xml
  <dependencies>
    ...
    <dependency>
      <groupId>com.example</groupId>
      <artifactId>some-api</artifactId>
    </dependency>
    ...
  </dependencies>
```

You can also add by just specifying the artifact ID if the artifact is managed by either in the `dependencyManagement`, dependency with `import` scope,
or managed by the parent.

### Removing dependencies

Dependencies can be removed from the `pom.xml` using the `remove` command.

```bash
# Remove by specifying the groupId and artifactId
pom remove groupId:artifactId

# Remove by specifying the artifactId
pom remove artifactId

# Use the shorter alias, rm
pom rm artifactId
```

### Setting parent POM

You can set the parent POM for your project using either a path to an existing parent project or by specifying the Maven coordinates.

If you set a parent, subsequent commands (such as `pom add` or `pom plug`) will properly manage dependencies and plugins according to the parent POM’s configuration.

#### Set parent by local path

```bash
# Parent is parent directory
pom parent ..

# Parent is in another directory of the parent directory
pom parent ../parent

# Parent is in a separate directory tree
pom parent /path/to/parent
```
This will set the `<parent>` element in your `pom.xml` to reference the specified parent project's POM file.
If the version of the parent was updated, calling this again will update the `<version>` of the `<parent>`.

#### Set parent by Maven coordinates

```bash
# Use latest version
pom parent groupId:artifactId

# Use specific version
pom parent groupId:artifactId:version
```
This will set the `<parent>` element in your `pom.xml` using the given group ID, artifact ID, and version.

### Setting properties

```bash
# Set single property
pom set maven.compiler.source=17

# Set multiple properties
pom set maven.compiler.source=17 maven.compiler.target=17

# Set multiple properties leveraging shell expansion
pom set maven.compiler.{source,target}=17
```

### Reading properties

```bash
# Get property from default profile
pom get maven.compiler.source

# Get property from specific profile
pom -P test get maven.compiler.source
```

### Removing properties

```bash
# Remove from default profile
pom unset property_name

# Remove from specific profile
pom -P test unset property_name
```

### Search

```bash
# Search by artifact ID
pom search log4j-api

# Search artifacts by class name
pom search --class Logger
pom search -c Logger

# Search artifacts by fully qualified class name
pom search --full-class org.apache.logging.log4j.Logger
pom search -fc org.apache.logging.log4j.Logger
```

### Adding plugins

```bash
# Add using full coordinate
pom plug com.example:hello-maven-plugin:1.0.0

# Auto resolve latest version
pom plug org.graalvm.buildtools:native-maven-plugin

# Plug to profile
pom -Pnative plug org.graalvm.buildtools:native-maven-plugin

# Add built-in plugin and auto resolve the latest version
pom plug maven-resources-plugin
```

If the current POM is a child of another POM, you can add a plugin
by simply stating the plugin's artifact ID.
```bash
# Add plugin managed by parent
pom plug spring-boot-maven-plugin
```
or, if you're starting with an empty project, you can run
```bash
pom parent parent.group:parent.artifactId:1.0.0 plug example-maven-plugin
```
e.g.:
```bash
pom parent org.springframework.boot:spring-boot-starter-parent plug spring-boot-maven-plugin
```

### Removing plugins

```bash
# Unplug by artifactId
pom unplug spring-boot-maven-plugin

# Unplug by groupId and artifactId
# Use case: multiple plugins have the same artifactId.
pom unplug org.springframework.boot:spring-boot-maven-plugin

# Unplug from a profile
pom -P dev unplug spring-boot-maven-plugin
```

## Building 

This project requires Java 21+, GraalVM CE and Maven 3.8.4+.
You can use [sdkman](https://sdkman.io/) to download them.

To build, run
```bash
mvn package
```
