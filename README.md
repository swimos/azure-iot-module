# Swim IoT-Module Embedded with Azure Event Hub and ADLS Gen2  

Generate a Swim simulation agent to send simulated CPU and Memory usage percentage to Azure Event Hub and ADLS
Gen2. Able to run with local run script `runLocal.sh` or container image. Demonstrated how to build a simple Swim
Application.

## Run
For shell script file that not able to run, please
```
chmod +x <fileName>
```

### Run with local Shell Script
Need to set up environment variables for Event Hub and ADLS Gen2 account config in `runLocal.sh` or `runLocal.bat`
based on Operating System. File `/src/main/java/swim/iot/util/EnvConfig.java` provides detail explanations of 
environment variables setup.

```
./runLocal.sh

or

.\runLocal.bat
```

### Run with Docker

Build Images

```
./buildImage.sh
```

run Image, need to change environment variable for Event Hub and ADLS Gen2
```
./runDocker.sh
```

