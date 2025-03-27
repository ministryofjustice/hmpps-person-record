SHELL     := /bin/bash
JAVA_OPTS := "-Xmx4096m -XX:ParallelGCThreads=2 -XX:ConcGCThreads=2 -Djava.util.concurrent.ForkJoinPool.common.parallelism=2 -Dorg.gradle.daemon=true -Dkotlin.compiler.execution.strategy=in-process -Dorg.gradle.workers.max=1"

test: start-containers format
	export _JAVA_OPTIONS=${JAVA_OPTS} && ./gradlew check

recluster-test: start-containers format
	export _JAVA_OPTIONS=${JAVA_OPTS} && ./gradlew ReclusterServiceIntTest

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
