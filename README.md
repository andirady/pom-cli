# pomcli

Command line tool to manipulate a POM file.

## Usage

### Setting project ID

```console
pom id com.example:my-app
pom id com.example:my-app:1.0.0
pom id com.example:my-webapp --as=war
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
```

For projects that are packaged as "pom", the dependencies will be added
to the ``dependencyManagement`` section.

If version is not specified, the latest version of the artifact will be used.
If there is a parent pom, and it already included the dependency version,
no version will be added.

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

This project requires Java 17+, Graal 21.3+ and Maven 3.8.4+.
You can use [sdkman](https://sdkman.io/) to download them.

To build, run
```console
mvn -Pgraalvm
```
