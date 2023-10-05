# Copyright (c) 2023 AccelByte Inc. All Rights Reserved.
# This is licensed software from AccelByte Inc, for limitations
# and restrictions contact your company contract manager.

SHELL := /bin/bash
IMAGE_NAME := $(shell basename "$$(pwd)")-clidemo

run: build run-only

build:
	docker build -t ${IMAGE_NAME} .

run-only:
	@test -n "$(ENV_FILE_PATH)" || (echo "ENV_FILE_PATH is not set" ; exit 1)
	docker run -t --rm -u $$(id -u):$$(id -g) \
		--env-file $(ENV_FILE_PATH) \
		${IMAGE_NAME}