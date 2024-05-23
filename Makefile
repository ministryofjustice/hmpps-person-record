test: start-containers format
	./gradlew check

format:
	./gradlew ktlintFormat

start-containers:
	docker compose up -d

stop-containers:
	docker compose down

restart-containers: stop-containers start-containers
