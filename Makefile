# Makefile per nf-lims (Nextflow Plugin)

.PHONY: clean assemble test install release

clean:
	./gradlew clean
	rm -rf work/ .nextflow*

assemble:
	./gradlew assemble

test:
	./gradlew test

install:
	./install-plugin.sh

release:
	./gradlew releasePlugin
