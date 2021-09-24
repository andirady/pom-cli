# mq

Maven Query and more

## Example usage

```bash
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

## TODO

1. The `add` subcommand should not proceed if `groupId:artifactId` already exists in the pom
2. Add `run` subcommand
3. Add `jshell` subcommand
