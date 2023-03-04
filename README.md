[![Java CI with Maven](https://github.com/andirady/pomcli/actions/workflows/build.yml/badge.svg)](https://github.com/andirady/pomcli/actions/workflows/build.yml)

# pomcli

**pomcli** lets you quickly create a ``pom.xml`` file as well as update it without manually editing the file. It's written in Java and compiled to native code with the help of graalvm.

Example flow:

```console
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
pom id com.example:my-app
pom id com.example:my-app:1.0.0
pom id com.example:my-webapp --as=war

# Set artifact ID to current directory name
pom id .
pom id com.example:.
pom id com.example:.:1.0.0
```

### Adding dependencies

```console
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

```console
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

### Setting properties

```console
# Set single property
pom set maven.compiler.source=17

# Set multiple properties
pom set maven.compiler.source=17 maven.compiler.source=17

# Set multiple properties leveraging shell expansion
pom set maven.compiler.{source,target}=17
```

### Search

```console
# Search by artifact ID
pom search log4j-api

# Search artifacts by class name
pom search --class Logger
pom search -c Logger

# Search artifacts by fully qualified class name
pom search --full-class org.apache.logging.log4j.Logger
pom search -fc org.apache.logging.log4j.Logger
```

## Building 

This project requires Java 17+, Graal 22.1+ and Maven 3.8.4+.
You can use [sdkman](https://sdkman.io/) to download them.

To build, run
```console
mvn -Pnative
```
