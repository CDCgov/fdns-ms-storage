docker-build:
	docker-compose up -d
	docker build \
		-t fdns-ms-storage \
		--network=fdns-ms-storage_default \
		--rm \
		--build-arg STORAGE_PORT=8082 \
		--build-arg STORAGE_REPO_HOST=http://minio:9000 \
		--build-arg STORAGE_REPO_ACCESS_KEY=minio \
		--build-arg STORAGE_REPO_SECRET_KEY=minio123 \
		--build-arg STORAGE_FLUENTD_HOST=fluentd \
		--build-arg STORAGE_FLUENTD_PORT=24224 \
		--build-arg STORAGE_PROXY_HOSTNAME= \
		--build-arg OAUTH2_ACCESS_TOKEN_URI= \
		--build-arg OAUTH2_PROTECTED_URIS= \
		--build-arg OAUTH2_CLIENT_ID= \
		--build-arg OAUTH2_CLIENT_SECRET= \
		--build-arg SSL_VERIFYING_DISABLE=false \
		--build-arg ERROR_INCLUDE_TRACE=false \
		.
	docker-compose down

docker-run: docker-start
docker-start:
	docker-compose up -d
	docker run -d \
		-p 8082:8082 \
		--network=fdns-ms-storage_default \
		--name=fdns-ms-storage_main \
		fdns-ms-storage

docker-stop:
	docker stop fdns-ms-storage_main || true
	docker rm fdns-ms-storage_main || true
	docker-compose down

docker-restart:
	make docker-stop 2>/dev/null || true
	make docker-start

sonarqube:
	docker-compose up -d
	docker run -d --name sonarqube -p 9001:9000 -p 9092:9092 sonarqube || true
	mvn -DSTORAGE_REPO_HOST=http://localhost:9000 -Dsonar.host.url=http://localhost:9001 clean test sonar:sonar
	docker-compose down
