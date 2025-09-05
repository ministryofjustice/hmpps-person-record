SHELL     := /bin/bash
JAVA_OPTS := "-Xmx4096m -XX:ParallelGCThreads=2 -XX:ConcGCThreads=2 -Djava.util.concurrent.ForkJoinPool.common.parallelism=2 -Dorg.gradle.daemon=true -Dkotlin.compiler.execution.strategy=in-process -Dorg.gradle.workers.max=1"

test: int-start-containers format
	export _JAVA_OPTIONS=${JAVA_OPTS} && ./gradlew check

test-e2e: start-all-containers format
	export _JAVA_OPTIONS=${JAVA_OPTS} && ./gradlew e2eTest

test-all: test e2e-test

format:
	./gradlew ktlintFormat

start-all-containers:
	$(MAKE) int-start-containers
	$(MAKE) e2e-start-containers

stop-all-containers:
	$(MAKE) int-stop-containers
	$(MAKE) e2e-stop-containers

restart-all-containers: stop-all-containers
	$(MAKE) int-start-containers
	$(MAKE) e2e-start-containers

run-local: int-start-containers
	./gradlew bootRun --args='--spring.profiles.active=local'

run-with-match:
ifeq (0,$(shell docker ps --filter "status=running" | grep 'hmpps-person-match' | wc -l | xargs))
	@echo "please, run make start-containers in hmpps-person-match before running this target"
else
	docker compose up -d localstack-hmpps-person-record && ./gradlew bootRun --args='--spring.profiles.active=local'
endif

int-start-containers:
ifeq (0,$(shell docker compose ps --services --filter "status=running" | grep 'hmpps-person-record' | wc -l | xargs))
	docker compose up -d
else
	@echo "Integration Containers: already running"
endif

int-stop-containers:
	docker compose down

e2e-start-containers:
ifeq (0,$(shell docker compose ps --services --filter "status=running" | grep 'hmpps-person-match' | wc -l | xargs))
	docker compose -f docker-compose-e2e-test.yml up -d
else
	@echo "E2E Containers: already running"
endif

e2e-stop-containers:
	docker compose -f docker-compose-e2e-test.yml down
