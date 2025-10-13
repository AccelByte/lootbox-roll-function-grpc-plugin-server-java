# Copyright (c) 2025 AccelByte Inc. All Rights Reserved.
# This is licensed software from AccelByte Inc, for limitations
# and restrictions contact your company contract manager.

SHELL := /bin/bash

PROJECT_NAME := $(shell basename "$$(pwd)")
GRADLE_IMAGE := gradle:7.6.4-jdk17
BUILD_CACHE_VOLUME := $(shell echo '$(PROJECT_NAME)' | sed 's/[^a-zA-Z0-9_-]//g')-build-cache

.PHONY: build

build: prepare_build_cache
	docker run -t --rm \
			-u $$(id -u):$$(id -g) \
			-v $(BUILD_CACHE_VOLUME):/tmp/build-cache \
			-v $$(pwd):/data \
			-w /data \
			$(GRADLE_IMAGE) \
			gradle \
					--gradle-user-home /tmp/build-cache/gradle \
					--project-cache-dir /tmp/build-cache/gradle \
					--console=plain \
					--info \
					--no-daemon \
					build

clean: prepare_build_cache
	docker run -t --rm \
			-u $$(id -u):$$(id -g) \
			-v $(BUILD_CACHE_VOLUME):/tmp/build-cache \
			-v $$(pwd):/data \
			-w /data \
			$(GRADLE_IMAGE) \
			gradle \
					--gradle-user-home /tmp/build-cache/gradle \
					--project-cache-dir /tmp/build-cache/gradle \
					--console=plain \
					--info \
					--no-daemon \
					clean

prepare_build_cache:
	docker run -t --rm \
			-v $(BUILD_CACHE_VOLUME):/tmp/build-cache \
			busybox:1.37.0 \
			chown $$(id -u):$$(id -g) /tmp/build-cache		# Fix /tmp/build-cache folder owned by root