SHELL     := /bin/bash
JAVA_OPTS := "-Xmx4096m -XX:ParallelGCThreads=2 -XX:ConcGCThreads=2 -Djava.util.concurrent.ForkJoinPool.common.parallelism=2 -Dorg.gradle.daemon=true -Dkotlin.compiler.execution.strategy=in-process -Dorg.gradle.workers.max=1"

test: start-containers format
	export _JAVA_OPTIONS=${JAVA_OPTS} && ./gradlew check

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

run-with-match:
ifeq (0,$(shell docker ps --filter "status=running" | grep 'hmpps-person-match' | wc -l | xargs))
	@echo "please, run make start-containers in hmpps-person-match before running this target"
else
	docker compose up -d localstack-hmpps-person-record && ./gradlew bootRun --args='--spring.profiles.active=local'
endif

e2e-start-containers:
	docker compose -f docker-compose-e2e-test.yml up -d

e2e-test: e2e-test-setup format
	export _JAVA_OPTIONS=${JAVA_OPTS} && ./gradlew e2eTest

e2e-stop-containers:
	docker compose -f docker-compose-e2e-test.yml down