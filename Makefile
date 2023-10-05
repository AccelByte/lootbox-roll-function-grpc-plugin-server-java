# Copyright (c) 2022 AccelByte Inc. All Rights Reserved.
# This is licensed software from AccelByte Inc, for limitations
# and restrictions contact your company contract manager.

IMAGE_NAME := $(shell basename "$$(pwd)")-app
BUILDER := grpc-plugin-server-builder

SHELL := /bin/bash

.PHONY: clean build image imagex

clean:
	docker run -t --rm -u $$(id -u):$$(id -g) \
		-v $$(pwd):/data/ -w /data/ \
		-e GRADLE_USER_HOME=.gradle gradle:7.5.1-jdk17 \
		gradle --console=plain -i --no-daemon clean

build:
	docker run -t --rm -u $$(id -u):$$(id -g) \
		-v $$(pwd):/data/ -w /data/ \
		-e GRADLE_USER_HOME=.gradle gradle:7.5.1-jdk17 \
		gradle --console=plain -i --no-daemon build

image:
	docker buildx build -t ${IMAGE_NAME} --load .

imagex:
	docker buildx inspect $(BUILDER) || docker buildx create --name $(BUILDER) --use
	docker buildx build -t ${IMAGE_NAME} --platform linux/arm64/v8,linux/amd64 .
	docker buildx build -t ${IMAGE_NAME} --load .
	docker buildx rm --keep-state $(BUILDER)

imagex_push:
	@test -n "$(IMAGE_TAG)" || (echo "IMAGE_TAG is not set (e.g. 'v0.1.0', 'latest')"; exit 1)
	@test -n "$(REPO_URL)" || (echo "REPO_URL is not set"; exit 1)
	docker buildx inspect $(BUILDER) || docker buildx create --name $(BUILDER) --use
	docker buildx build -t ${REPO_URL}:${IMAGE_TAG} --platform linux/arm64/v8,linux/amd64 --push .
	docker buildx rm --keep-state $(BUILDER)

build-clidemo:
	docker build -t ${IMAGE_NAME}-clidemo -f Dockerfile.clidemo .

run-clidemo:
	docker run -t --rm -u $$(id -u):$$(id -g) \
		--env-file .env.clidemo \
		${IMAGE_NAME}-clidemo
