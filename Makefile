# Copyright (c) 2022 AccelByte Inc. All Rights Reserved.
# This is licensed software from AccelByte Inc, for limitations
# and restrictions contact your company contract manager.

SHELL := /bin/bash

IMAGE_NAME := $(shell basename "$$(pwd)")-app
BUILDER := extend-builder

TEST_SAMPLE_CONTAINER_NAME := sample-override-test

.PHONY: clean build

clean:
	docker run -t --rm -u $$(id -u):$$(id -g) \
		-v $$(pwd):/data/ -w /data/ \
		-e GRADLE_USER_HOME=.gradle gradle:7.6.4-jdk17 \
		gradle --console=plain -i --no-daemon clean

build:
	docker run -t --rm -u $$(id -u):$$(id -g) \
		-v $$(pwd):/data/ -w /data/ \
		-e GRADLE_USER_HOME=.gradle gradle:7.6.4-jdk17 \
		gradle --console=plain -i --no-daemon build

image:
	docker buildx build -t ${IMAGE_NAME} --load .

imagex:
	docker buildx inspect $(BUILDER) || docker buildx create --name $(BUILDER) --use
	docker buildx build -t ${IMAGE_NAME} --platform linux/amd64 .
	docker buildx build -t ${IMAGE_NAME} --load .
	docker buildx rm --keep-state $(BUILDER)

imagex_push:
	@test -n "$(IMAGE_TAG)" || (echo "IMAGE_TAG is not set (e.g. 'v0.1.0', 'latest')"; exit 1)
	@test -n "$(REPO_URL)" || (echo "REPO_URL is not set"; exit 1)
	docker buildx inspect $(BUILDER) || docker buildx create --name $(BUILDER) --use
	docker buildx build -t ${REPO_URL}:${IMAGE_TAG} --platform linux/amd64 --push .
	docker buildx rm --keep-state $(BUILDER)

ngrok:
	@test -n "$(NGROK_AUTHTOKEN)" || (echo "NGROK_AUTHTOKEN is not set" ; exit 1)
	docker run --rm -it --net=host -e NGROK_AUTHTOKEN=$(NGROK_AUTHTOKEN) ngrok/ngrok:3-alpine \
			tcp 6565	# gRPC server port

test_sample_local_hosted:
	@test -n "$(ENV_PATH)" || (echo "ENV_PATH is not set"; exit 1)
	docker build \
			--tag $(TEST_SAMPLE_CONTAINER_NAME) \
			-f test/sample/Dockerfile \
			test/sample
	docker run --rm -t \
			-u $$(id -u):$$(id -g) \
			-e GRADLE_USER_HOME=.gradle \
			--env-file $(ENV_PATH) \
			-v $$(pwd):/data \
			-w /data \
			--name $(TEST_SAMPLE_CONTAINER_NAME) \
			$(TEST_SAMPLE_CONTAINER_NAME) \
			bash ./test/sample/test-local-hosted.sh

test_sample_accelbyte_hosted:
	@test -n "$(ENV_PATH)" || (echo "ENV_PATH is not set"; exit 1)
ifeq ($(shell uname), Linux)
	$(eval DARGS := -u $$(shell id -u) --group-add $$(shell getent group docker | cut -d ':' -f 3))
endif
	docker build \
			--tag $(TEST_SAMPLE_CONTAINER_NAME) \
			-f test/sample/Dockerfile \
			test/sample
	docker run --rm -t \
			-e GRADLE_USER_HOME=.gradle \
			-e DOCKER_CONFIG=/tmp/.docker \
			--env-file $(ENV_PATH) \
			-v /var/run/docker.sock:/var/run/docker.sock \
			-v $$(pwd):/data \
			-w /data \
			--name $(TEST_SAMPLE_CONTAINER_NAME) \
			$(DARGS) \
			$(TEST_SAMPLE_CONTAINER_NAME) \
			bash ./test/sample/test-accelbyte-hosted.sh
