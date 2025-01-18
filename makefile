all: Hydra-MQTT*.modl

clean:
	@rm -f *.modl
	@make clean --quiet -C docs
	@make clean --quiet -C src

Hydra-MQTT*.modl: module-signer.jar secrets/ src/Hydra-MQTT*unsigned.modl
	@. ./secrets/env; \
	keystore_password=$$(cat $${keystore_password_file}) && \
	alias_name=$$(cat $${alias_name_file}) && \
	alias_password=$$(cat $${alias_password_file}) && \
	targets=$$(find src/ -type f -name '*unsigned.modl') && \
	echo "$${targets}" | while IFS= read -r target; do \
		file_base_name=$$(basename "$${target}" | sed 's:^\./::g' | sed 's/-unsigned.modl//g') && \
		echo "Signing $${target}..." && \
		java -jar module-signer.jar -keystore=$${keystore_file} -keystore-pwd=$${keystore_password} \
			-alias=$${alias_name} -alias-pwd=$${alias_password} -chain=$${chain_file} \
			-module-in="$${target}" -module-out="$${file_base_name}.modl" && \
		echo "Signed $${file_base_name}.modl"; \
	done

	@rm -f src/Hydra-MQTT*-unsigned.modl

src/Hydra-MQTT*unsigned.modl:
	@make --quiet -C src

secrets/:
	mkdir -p "secrets/"
	./src/generate-secrets.sh

module-signer.jar:
	@rm -rf "module-signer/"
	@echo "Downloading and building module signer..."
	@git clone --quiet "https://github.com/inductiveautomation/module-signer"
	@cd "module-signer" && mvn package -q
	@target=$$(find "module-signer/target/" | grep "module-signer.*with-dependencies.jar"); \
	#echo "target: $${target}"; \
	mv "$${target}" "module-signer.jar"
	@rm -rf "module-signer/"
