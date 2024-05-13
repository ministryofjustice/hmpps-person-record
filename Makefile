test: start-containers format stop-containers
	./gradlew check

format:
	./gradlew ktlintFormat

start-containers:
	docker compose up -d

stop-containers:
	docker compose down
