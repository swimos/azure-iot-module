ECHO off

REM change lines below to set environment variables for app
SET EVENT_HUB_CONNSTRING=""
SET EVENT_HUB_NAME=""
SET EDGE_DEVICE_NAME="localDevice"
SET ADLS_ACCOUNT_NAME=""
SET ADLS_ACCOUNT_KEY=""
SET FILE_SYSTEM=""

ECHO on
gradlew clean run