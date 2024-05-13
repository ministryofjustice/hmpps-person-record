test: stop-containers start-containers format
	./gradlew check

format:
	./gradlew ktlintFormat

start-containers:
	docker compose up -d

stop-containers:
	docker compose down
