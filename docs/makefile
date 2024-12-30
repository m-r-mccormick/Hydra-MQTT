
all: venv/touchfile
	export SITE_NAME="Hydra-MQTT v`cat ../src/version`" && \
	echo "SITE_NAME: $${SITE_NAME}" && \
	. venv/bin/activate; cd mkdocs && mkdocs build --site-dir ../_site \

debug: venv/touchfile
	export SITE_NAME="Hydra-MQTT v`cat ../src/version`" && \
	echo "SITE_NAME: $${SITE_NAME}" && \
	. venv/bin/activate && cd mkdocs && mkdocs serve -a localhost:4000
	
clean:
	@rm -rf build
	@rm -rf venv

# Update venv when requirements.txt changes
venv/touchfile: requirements.txt
	# Create a virtual environment if it doesn't exist
	test -d venv || python3 -m venv venv
	# Install python dependencies from requirements.txt
	. venv/bin/activate; pip install -Ur requirements.txt
	# Signal that venv has been updated for due to requirements.txt change
	touch venv/touchfile
	# List Installed Packages
	. venv/bin/activate && pip list && sleep 2
	
