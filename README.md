# mq

Maven Query and more

## Example usage

```bash
# Set project ID
mq id com.example:my-app
mq id com.example:my-webapp --as=war
# Add to pom.xml
# If pom.xml does not exists yet, create a new one.
mq add info.picocli:picocli
# Add test dependency
mq add --test org.junit.jupiter:junit-jupiter

# Search
mq search log4j-api

# Search artifacts by class name
mq search --class Logger
mq search -c Logger

# Search artifacts by fully qualified class name
mq search --full-class org.apache.logging.log4j.Logger
mq search -fc org.apache.logging.log4j.Logger
```

## Building 

This project requires Java 17+, and Maven 3.8.2+.
You can use [sdkman](https://sdkman.io/) to download them.

To build, run
```
mvn package
```

## TODO

1. The `add` subcommand should not proceed if `groupId:artifactId` already exists in the pom
2. Add `run` subcommand
3. Add `jshell` subcommand
