#Dockerfile

FROM openjdk:11-jdk-stretch

# other choice
#FROM openjdk:11-ea-19-jdk-slim

WORKDIR /

EXPOSE 9005

COPY dist/swim-azure-iot-module-3.10.2 /app/swim-azure-iot-module-3.10.2/

WORKDIR /app/swim-azure-iot-module-3.10.2/bin
ENTRYPOINT ["./swim-azure-iot-module"]
