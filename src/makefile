.PHONY: all sign build

all: sign

clean:
	@echo Cleaning Module...
	
	@if [ -f pom.xml.versionsBackup ]; then \
		mv pom.xml.versionsBackup pom.xml; \
	fi
	@if [ -f build/pom.xml.versionsBackup ]; then \
		mv build/pom.xml.versionsBackup build/pom.xml; \
	fi
	@if [ -f gateway/pom.xml.versionsBackup ]; then \
		mv gateway/pom.xml.versionsBackup gateway/pom.xml; \
	fi

	@rm -f *.modl
	@mvn clean -q
	@echo Clean Module Complete.

build:
	@if [ -f pom.xml.versionsBackup ]; then \
		mv pom.xml.versionsBackup pom.xml; \
	fi
	@if [ -f build/pom.xml.versionsBackup ]; then \
		mv build/pom.xml.versionsBackup build/pom.xml; \
	fi
	@if [ -f gateway/pom.xml.versionsBackup ]; then \
		mv gateway/pom.xml.versionsBackup gateway/pom.xml; \
	fi

	@VERSION=$$(cat version) && \
	BUILD_VERSION="-SNAPSHOT"; \
	if [ "$${BUILD_CONFIG}" = "RELEASE" ]; then \
		BUILD_VERSION=""; \
		echo "Building Module in Release Configuration..."; \
	else \
		echo "Building Module in Pre-Release Configuration..."; \
	fi; \
	if [ -z $${BUILD_NUMBER} ]; then BUILD_NUMBER=1; fi && \
	mvn versions:set -DnewVersion=$${VERSION}$${BUILD_VERSION} -Dbuild.number=$${BUILD_NUMBER} -DgenerateBackupPoms=true -f pom.xml -q
	@mvn package -q

	@if [ -f pom.xml.versionsBackup ]; then \
		mv pom.xml.versionsBackup pom.xml; \
	fi
	@if [ -f build/pom.xml.versionsBackup ]; then \
		mv build/pom.xml.versionsBackup build/pom.xml; \
	fi
	@if [ -f gateway/pom.xml.versionsBackup ]; then \
		mv gateway/pom.xml.versionsBackup gateway/pom.xml; \
	fi

	@echo Build Module Complete.

FIND_DIR="."
OUTPUT_DIR="."
SECRETS_DIR="secrets"
sign: build
	@if [ ! -f "module-signer.jar" ]; then \
	    rm -rf "module-signer/"; \
		echo "Downloading and building module signer..."; \
		git clone --quiet https://github.com/inductiveautomation/module-signer; \
		$$(cd "module-signer" && mvn package -q); \
		target=$$(find "module-signer/target/" | grep "module-signer.*with-dependencies.jar"); \
		echo "target: $${target}"; \
		mv "$${target}" "module-signer.jar"; \
		rm -rf "module-signer/"; \
	fi

	@if [ ! -d "$(SECRETS_DIR)" ]; then mkdir "$(SECRETS_DIR)"; fi
	@if [ ! -f "$(SECRETS_DIR)/env" ]; then \
		./generate-secrets.sh; \
	fi

	@targets=$$(find $(FIND_DIR) -mindepth 2 -type f -name '*unsigned.modl'); \
	if [ -z "$${targets}" ]; then \
		$(MAKE) build; \
	fi

	@targets=$$(find $(FIND_DIR) -mindepth 2 -type f -name '*unsigned.modl'); \
	if [ -z "$${targets}" ]; then \
		echo "Error: No Modules To Sign"; \
		exit 1; \
	fi

	@if [ ! -d $(OUTPUT_DIR) ]; then mkdir $(OUTPUT_DIR); fi

	@echo "Signing Modules..."

	@. ./$(SECRETS_DIR)/env; \
	keystore_password=$$(cat $${keystore_password_file}) && \
	alias_name=$$(cat $${alias_name_file}) && \
	alias_password=$$(cat $${alias_password_file}) && \
	targets=$$(find $(FIND_DIR) -type f -name '*unsigned.modl') && \
	version=$$(cat "../src/version") && \
	for target in $${targets}; do \
		file_base_name=$$(basename "$${target}" | sed 's/-unsigned.modl//g') && \
#		file_name="$${file_base_name}-$${version}.modl" && \
		file_name="$${file_base_name}.modl" && \
		java -jar module-signer.jar -keystore=$${keystore_file} -keystore-pwd=$${keystore_password} -alias=$${alias_name} -alias-pwd=$${alias_password} -chain=$${chain_file} -module-in=$${target} -module-out=$(OUTPUT_DIR)/$${file_name} && \
		echo "Module Signed: $${file_name}"; \
	done

	@echo "All Modules Signed"

