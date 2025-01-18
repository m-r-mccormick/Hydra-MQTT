all: venv/touchfile
	@"./src/Pages/Getting Started/docker/build.sh"
	@if [ ! -f "../src/config" ]; then \
		make config --quiet -C "../src/"; \
	fi
	@RELEASE_VERSION=$$(cat ../src/config | grep "RELEASE_VERSION=" | sed 's/.*=//g') && \
	echo "Building docs version $${RELEASE_VERSION}..." && \
	export SITE_NAME="Hydra-MQTT v$${RELEASE_VERSION}" && \
	. venv/bin/activate; cd mkdocs && mkdocs build --site-dir ../_site -q \

debug: venv/touchfile
	@"./src/Pages/Getting Started/docker/build.sh"
	@if [ ! -f "../src/config" ]; then \
		make config --quiet -C "../src/"; \
	fi
	@RELEASE_VERSION=$$(cat ../src/config | grep "RELEASE_VERSION=" | sed 's/.*=//g') && \
	echo "Building docs version $${RELEASE_VERSION}..." && \
	export SITE_NAME="Hydra-MQTT v$${RELEASE_VERSION}" && \
	. venv/bin/activate && cd mkdocs && mkdocs serve -a localhost:4000
	
clean:
	@rm -rf build
	@rm -rf venv

# Update venv when requirements.txt changes
venv/touchfile: requirements.txt
	@echo "Setting up virtual environment..."
	# Create a virtual environment if it doesn't exist
	test -d venv || python3 -m venv venv
	# Install python dependencies from requirements.txt
	. venv/bin/activate; pip install -q -Ur requirements.txt
	# Signal that venv has been updated for due to requirements.txt change
	touch venv/touchfile
#	# List Installed Packages
#	. venv/bin/activate && pip list && sleep 2
	
