#!/bin/bash

mkdir -p data
cd data

java \
	-XX:+CrashOnOutOfMemoryError -XX:+AlwaysPreTouch -XX:+UseNUMA \
	-XX:+ExplicitGCInvokesConcurrent -XX:+UseCompressedOops \
	-XX:+UseShenandoahGC -XX:+UnlockExperimentalVMOptions -XX:+ShenandoahPacing \
	-XX:MinHeapFreeRatio=15 -XX:MaxHeapFreeRatio=30 \
	-jar ../server.jar
