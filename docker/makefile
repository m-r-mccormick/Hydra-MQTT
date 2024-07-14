INPUT_DIR="../src/"


all: build

run: build
	@echo Starting Docker Container...
#	@docker-compose down
	@docker-compose up -d
	
	@echo "Waiting for container to start..."
	@counter=1; \
	until [ $$(docker-compose ps ignition | grep -w healthy | wc -l) -gt 0 ]; do \
		counter=$$((counter + 1)); \
		if [ $$counter -ge 60 ]; then \
			echo "Exceeded Timeout"; \
			exit 1; \
		fi; \
		if [ $$counter -ge 15 ]; then \
			echo "Time: $$counter"; \
		fi; \
		sleep 1; \
	done
	
	@echo Started Docker Container.

build:
	@echo Building Docker Image...
	
	$(MAKE) -C ../src clean
	
	@target=$$(find $(INPUT_DIR) -maxdepth 1 -type f -name '*.modl'); \
	if [ -z "$${target}" ]; then \
		$(MAKE) -C ../src sign; \
	fi
	
	@if [ ! -d "ignition-build/modules/" ]; then \
		mkdir "ignition-build/modules/"; \
	fi

	@mv $(INPUT_DIR)*.modl ignition-build/modules/
	@docker-compose build -q
	@rm ignition-build/modules/*
	@echo Built Docker Image

clean:
	@docker-compose down; exit 0
	@echo "Removing Volume..."
	@docker volume rm ignitiondev_data
	
