FROM debian:bookworm-slim

COPY ./target/pom-* /usr/local/bin/pom

WORKDIR /workspace

ENTRYPOINT ["pom"]
