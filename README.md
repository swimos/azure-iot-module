# Swim IoT-Module Embedded with Azure Event Hub and ADLS Gen2  

A swim application that demonstrates how data can be published to to Azure Event Hub and ADLS Gen2. 


Data Flow: The [SimulationWebAgent](https://github.com/swimos/azure-iot-module/blob/master/src/main/java/swim/iot/agent/SimulationAgent.java) generates simulated data and sends data to EventHub and the [AdlsAgent](https://github.com/swimos/azure-iot-module/blob/master/src/main/java/swim/iot/agent/AdlsAgent.java). The `AdlsAgent` sends the data to Adls Gen2


## Run the Application
Pre-requisites: Set up the environment variables for the Event Hub and ADLS Gen2 accounts. [EvnvConfig.java](https://github.com/swimos/azure-iot-module/blob/master/src/main/java/swim/iot/util/EnvConfig.java) describes the different environment variables required to run this application.


### Run using a Shell Script
For Linux/MacOS:

```
./runLocal.sh
```


For Windows:
```
.\runLocal.bat
```

### Run with Docker

Build the docker image using the following command:

```
./buildImage.sh
```

Run the application using docker by exectuting the following command:
```
./runDocker.sh
```

