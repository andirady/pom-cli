.PHONY: dist copy

NAME = pom-cli
VERSION = 0.9.2

PLATFORM = linux
ARCH = amd64

distname = $(NAME)-$(VERSION)-$(PLATFORM)_$(ARCH)
basedir = target/$(distname)
exe = target/pom-$(VERSION)
completions_dir = $(basedir)/share/bash-completion/completions

dist: $(exe) copy
	tar czvf target/$(distname).tar.gz -C target $(distname) && \
	sha256sum target/$(distname).tar.gz > target/$(NAME)-$(VERSION)_checksums.txt

copy: $(basedir)
	cp $(exe) $(basedir)/bin/pom && \
	cp LICENSE $(basedir)/share/$(NAME)/ && \
	cp target/pom_completion $(completions_dir)/pom

$(basedir):
	mkdir -p $(basedir)/bin $(completions_dir) $(basedir)/share/$(NAME)

$(exe):
	mvn -Pnative -DskipTests -Drevision=$(VERSION)
