./gradlew clean build

rm -rf dist/
mkdir -p dist/
tar -xf build/distributions/swim-azure-iot-module-3.10.2.tar -C dist/
sudo docker build ./ -f ./java.Dockerfile -t swimazureiot/module:1.0

# docker build command with other operation system environment
# docker build ./ -f ./java.Dockerfile --platform linux/amd64 --platform linux/arm64 --platform linux/arm/v7 -t swimazureiot/module:1.0