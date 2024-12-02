SHELL := /bin/bash
test: start-containers format
	./gradlew check

format:
	./gradlew ktlintFormat

start-containers:
ifeq (0,$(shell docker compose ps --services --filter "status=running" | grep 'hmpps-person-record' | wc -l | xargs))
	docker compose up -d
else
	@echo "containers already running"
endif

stop-containers:
	docker compose down

restart-containers: stop-containers
	docker compose up -d

run-local: start-containers
	./gradlew bootRun --args='--spring.profiles.active=local'
