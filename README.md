# pomcli

Command line tool to manipulate a POM file.

## Example usage

```bash
# Set project ID
pom id com.example:my-app
pom id com.example:my-webapp --as=war
# Add to pom.xml
# If pom.xml does not exists yet, create a new one.
pom add info.picocli:picocli
# Add test dependency
pom add --test org.junit.jupiter:junit-jupiter
# Set single property
pom set maven.compiler.source=17
# Set multiple properties
pom set maven.compiler.source=17 maven.compiler.source=17
# Set multiple properties leveraging shell expansion
pom set maven.compiler.{source,target}=17

# Search
pom search log4j-api

# Search artifacts by class name
pom search --class Logger
pom search -c Logger

# Search artifacts by fully qualified class name
pom search --full-class org.apache.logging.log4j.Logger
pom search -fc org.apache.logging.log4j.Logger
```

## Building 

This project requires Java 17+, and Maven 3.8.4+.
You can use [sdkman](https://sdkman.io/) to download them.

To build, run
```
mvn package
```
