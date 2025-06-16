# Copyright (c) 2024 AccelByte Inc. All Rights Reserved.
# This is licensed software from AccelByte Inc, for limitations
# and restrictions contact your company contract manager.

SHELL := /bin/bash

GRADLE_IMAGE := gradle:7.6.4-jdk17

IMAGE_NAME := $(shell basename "$$(pwd)")-app
BUILDER := extend-builder

.PHONY: build

build:
	docker run -t --rm \
			-u $$(id -u):$$(id -g) \
			-v $$(pwd):/data \
			-w /data \
			-e GRADLE_USER_HOME=.gradle \
			$(GRADLE_IMAGE) \
			gradle -i --no-daemon generateProto \
					|| find .gradle -type f -iname 'protoc-*.exe' -exec chmod +x {} \;		# For MacOS docker host: Workaround to make protoc-*.exe executable
	docker run -t --rm \
			-u $$(id -u):$$(id -g) \
			-v $$(pwd):/data \
			-w /data \
			-e GRADLE_USER_HOME=.gradle \
			$(GRADLE_IMAGE) \
			gradle -i --no-daemon build

clean:
	docker run -t --rm \
			-u $$(id -u):$$(id -g) \
			-v $$(pwd):/data \
			-w /data \
			-e GRADLE_USER_HOME=.gradle \
			$(GRADLE_IMAGE) \
			gradle -i --no-daemon clean

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
	@which ngrok || (echo "ngrok is not installed" ; exit 1)
	@test -n "$(NGROK_AUTHTOKEN)" || (echo "NGROK_AUTHTOKEN is not set" ; exit 1)
	ngrok tcp 6565	# gRPC server port
