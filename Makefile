# Makefile for SmartStay PMS

.PHONY: help build run stop clean test db-shell

# Default target
help:
	@echo "SmartStay PMS Commands"
	@echo "======================"
	@echo "Usage: make [target]"
	@echo ""
	@echo "Targets:"
	@echo "  build    - Start the database container and compile the JavaFX application"
	@echo "  run      - Start the DB (if not running), wait for it to be healthy, and launch the App"
	@echo "  stop     - Stop the database container"
	@echo "  clean    - Remove build artifacts and destroy the database volume"
	@echo "  test     - Run automated tests"
	@echo "  db-shell - Access the MySQL database shell inside the container"

# Build: Compiles the Java project and builds the database container
build:
	docker compose up -d mysql
	mvn clean compile

# Run: Ensures the database is healthy, then launches the JavaFX Desktop App
run:
	@docker compose up -d mysql
	@echo "Waiting for database to be ready (healthy)..."
	@bash -c 'until [ "$$(docker inspect -f "{{.State.Health.Status}}" smartstay-db)" = "healthy" ]; do \
		echo -n "."; \
		sleep 2; \
	done; echo ""'
	@echo "Database is ready. Launching SmartStay Desktop App..."
	mvn javafx:run

# Stop: Stops the database container
stop:
	docker compose stop

# Clean: Deletes target folder and removes docker volumes
clean:
	mvn clean
	docker compose down -v

# Test: Runs maven tests
test:
	mvn test

# Database shell: Connect to the running MySQL instance
db-shell:
	docker exec -it smartstay-db mysql -u root -proot smartstay
