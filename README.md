[![Java CI with Maven](https://github.com/andirady/pom-cli/actions/workflows/build.yml/badge.svg)](https://github.com/andirady/pom-cli/actions/workflows/build.yml)

# pom-cli 🪄

**pom-cli** lets you quickly create a ``pom.xml`` file as well as update it without manually editing the file. It's written in Java and compiled to native code with the help of graalvm.

Example flow:

```bash
# Change to the project directory
cd my_app
# Create folders
mkdir -p src/main/java/unnamed/my_app
# Creat a pom.xml with groupdId `unnamed`, artifactID `my_app` (the same as the folder name) and version `0.0.1-SNAPSHOT`.
# The pom.xml will have the `maven.compiler.source` and `maven.compiler.target` or `maven.compiler.release` set to the `java` version available in `$PATH`
# The `project.build.sourceEncoding` will also be set to UTF-8.
pom id .
# Add latest log4j-api as compile dependency
pom add org.apache.logging.log4j:log4j-api
# Create a simple java and run
mvn package
```

## Usage

### Setting project ID

```console
$ pom id com.example:my-app
jar com.example:my-app:0.0.1-SNAPSHOT
$ pom id com.example:my-app:1.0.0
jar com.example:my-app:1.0.0
$ pom id com.example:my-webapp --as=war
war com.example:my-webapp:0.0.1-SNAPSHOT
```

Set artifact ID to current directory name
```console
$ cd my-app
$ pom id .
jar unnamed:my-app:0.0.1-SNAPSHOT
$ pom id com.example:.
jar com.example:my-app:0.0.1-SNAPSHOT
$ pom id com.example:.:1.0.0
jar com.example:my-app:1.0.0
```

By default, if the ``groupd_id`` is not specified, ``unnamed`` will be used.
To set a different default ``group_id`` you can set the ``POM_CLI_DEFAULT_GROUP_ID`` environment variable.

```console
$ export POM_CLI_DEFAULT_GROUP_ID=com.example
$ cd my-app
$ pom id .
jar com.example:my-app:0.0.1-SNAPSHOT
```

If the current folder belongs to a multi-module maven project,
the ``<parent>`` element will be added to the pom. For example:
```console
$ cd hello
$ pom id --as=pom com.example:.
pom com.example:hello:0.0.1-SNAPSHOT
$ mkdir api
$ cd $_
$ pom id hello-api
jar com.example:hello-api:0.0.1-SNAPSHOT
```

If the project is meant to be standalone, then you can use the ``--standalone`` flag
```console
$ mkdir hello/sample
$ cd $_
$ pom id --standalone .
jar unnamed:sample:0.0.1-SNAPSHOT
```

The resulting ``pom.xml`` will not have the ``<parent>`` element.

### Adding dependencies

```bash
# Add compile dependency
pom add info.picocli:picocli

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

Running the following:

```bash
pom add some-api
```

would results in the target ``pom.xml`` to contain the following:

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
# Remove by spefifying the groupId and artifactId
pom remove groupId:artifactId

# Remove by specifying the artifactId
pom remove artifactId

# Use the shorter alias, rm
pom rm artifactId
```

### Setting properties

```bash
# Set single property
pom set maven.compiler.source=17

# Set multiple properties
pom set maven.compiler.source=17 maven.compiler.source=17

# Set multiple properties leveraging shell expansion
pom set maven.compiler.{source,target}=17
```

### Reading properties

```bash
# Get property from default profile
pom get maven.compiler.source

# Set property from specific profile
pom -P test get maven.compiler.source
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

### Add plugin

```console
$ # Add using full coordinate
$ pom plug com.example:hello-maven-plugin:1.0.0
🔌 com.example:hello-maven-plugin:1.0.0 plugged
$ # Auto resolve latest version
$ pom plug org.graalvm.buildtools:native-maven-plugin
🔌 org.graalvm.buildtools:native-maven-plugin:1.0.0 plugged
$ # Plug to profile
$ pom -Pnative plug org.graalvm.buildtools:native-maven-plugin
🔌 org.graalvm.buildtools:native-maven-plugin:1.0.0 plugged
$ # Add built-in plugin and auto resolve the latest version
$ pom plug maven-resources-plugin
🔌 org.apache.maven.plugins:maven-resources-plugin:1.0.0 plugged
```

## Building 

This project requires Java 21+, GraalVM CE and Maven 3.8.4+.
You can use [sdkman](https://sdkman.io/) to download them.

To build, run
```bash
mvn -Pnative
```
