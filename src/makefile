# Build Environment Variables:
#	EVENT_NAME == "push" || "pull_request" -> Release or Prerelease
#	EVENT_NAME != "push" || "pull_request" -> Snapshot (Development Preview)
#	GIT_BRANCH == "main" -> Release
#	GIT_BRANCH != "main" -> Prerelease
# Examples
#	make                                     -> Build Snapshot Version
#	EVENT_NAME="push" make                   -> Build Prerelease Version
#	EVENT_NAME="push" GIT_BRANCH="main" make -> Build Release Version

.PHONY: all sign build
GEN_ROOT_JAVA="gateway/src/main/java/com/mrmccormick/hydra/mqtt/settings"
GEN_ROOT_RESO="gateway/src/main/resources/com/mrmccormick/hydra/mqtt/settings"

all: Hydra-MQTT-*-unsigned.modl

clean:
	@if [ -f pom.xml.versionsBackup ]; then \
		mv pom.xml.versionsBackup pom.xml; \
	fi
	@if [ -f build/pom.xml.versionsBackup ]; then \
		mv build/pom.xml.versionsBackup build/pom.xml; \
	fi
	@if [ -f gateway/pom.xml.versionsBackup ]; then \
		mv gateway/pom.xml.versionsBackup gateway/pom.xml; \
	fi

	@rm -rf "build/doc/"
	@rm -rf "build/target/"
	@rm -rf "$(GEN_ROOT_JAVA)/generated/"
	@rm -rf "$(GEN_ROOT_RESO)/generated/"
	@rm -f *.modl
	@rm -f config
	@rm -f config.log
	@mvn clean -q

Hydra-MQTT-*-unsigned.modl: config build/doc
	@if [ -f pom.xml.versionsBackup ]; then \
		mv pom.xml.versionsBackup pom.xml; \
	fi
	@if [ -f build/pom.xml.versionsBackup ]; then \
		mv build/pom.xml.versionsBackup build/pom.xml; \
	fi
	@if [ -f gateway/pom.xml.versionsBackup ]; then \
		mv gateway/pom.xml.versionsBackup gateway/pom.xml; \
	fi

	@VERSION=$$(cat config | grep "BUILD_MODULE=" | sed 's/.*=//g') && \
	echo "Building module version $${VERSION}..."; \
	mvn versions:set -DnewVersion=$${VERSION} -DgenerateBackupPoms=true -f pom.xml -q || $$(cat config.log; exit 1)
	@mvn package -q || $$(cat config.log; exit 1)
	
	@if [ -f pom.xml.versionsBackup ]; then \
		mv pom.xml.versionsBackup pom.xml; \
	fi
	@if [ -f build/pom.xml.versionsBackup ]; then \
		mv build/pom.xml.versionsBackup build/pom.xml; \
	fi
	@if [ -f gateway/pom.xml.versionsBackup ]; then \
		mv gateway/pom.xml.versionsBackup gateway/pom.xml; \
	fi

	@targets=$$(find . -mindepth 2 -type f -name '*unsigned.modl'); \
	num_targets=$$(echo "$${targets}" | wc -l); \
	if [ $${num_targets} -ne 1 ]; then \
		echo "A single target must be identified. $${num_targets} were identified:"; \
		echo "$${targets}"; \
		exit 1; \
	fi; \
	mv "$${targets}" .

	@targets=$$(find . -mindepth 1 -maxdepth 1 -type f -name '*-unsigned.modl'); \
	target_build=$$(cat config | grep "BUILD_NUMBER" | sed 's/.*=//g'); \
	target_version=$$(cat config | grep "RELEASE_TAG" | sed 's/.*=//g' | sed "s/-dev/.$${target_build}-dev/g"); \
	for target in $${targets}; do \
		new_name=$$(echo "$${target}" | sed "s/-unsigned.modl/-$${target_version}-unsigned.modl/g"); \
		mv "$${target}" "$${new_name}"; \
	done

	@echo Build Module Complete.


build/doc:
#	# Build Documentation
	@cd ../docs/ && make --silent
#	# Move documentation into module documentation folder
	@mv "../docs/_site" "build/doc"

# To mock ci variables, run: `make mock=main-push.env config`
config: version
	@./generate-config.sh $(mock) || $$(cat config.log; exit 1)


