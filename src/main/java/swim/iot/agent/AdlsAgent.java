package swim.iot.agent;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Map;
import swim.api.SwimLane;
import swim.api.SwimTransient;
import swim.api.agent.AbstractAgent;
import swim.api.lane.CommandLane;
import swim.api.lane.MapLane;
import swim.api.lane.ValueLane;
import swim.concurrent.TimerRef;
import swim.iot.azure.adls.RestClient;
import swim.iot.util.EnvConfig;
import swim.structure.Record;
import swim.structure.Value;
import swim.util.OrderedMap;

public class AdlsAgent extends AbstractAgent {

  private String FILE_PATH_NAME = EnvConfig.EDGE_DEVICE_NAME;
  private Boolean initFlag = true;

  private String DIR;
  private String DIR_NAME;
  private String BASE_URL;
  private String DIR_URL;
  private String BASE_FILE_PREFIX;
  private String COUNT_FILE_PREFIX;
  private String MODEL_FILE;

  private void initFields() {
    DIR = "/" + EnvConfig.FILE_SYSTEM + "/" + FILE_PATH_NAME + "-iot-module";
    DIR_NAME = FILE_PATH_NAME + "-iot-module";
    BASE_URL = "https://" + EnvConfig.ADLS_ACCOUNT_NAME + ".dfs.core.windows.net";
    DIR_URL = BASE_URL + "/" + EnvConfig.FILE_SYSTEM + "/" + FILE_PATH_NAME + "-iot-module/";
    BASE_FILE_PREFIX = "SystemMetricsSimulation-" + FILE_PATH_NAME + "-";
    COUNT_FILE_PREFIX = DIR + "/" + BASE_FILE_PREFIX;
    MODEL_FILE = DIR + "/model.json";
  }

  /**
   * Fixed MAX History MapLane Size
   */
  private final int MAX_HISTORY_SIZE = 60;

  private final int ADLS_FILE_COUNT = 60;

  private final String BASE_MODEL_PREFIX = "{\n"
      + "    \"name\": \"SystemMetricsSimulationV1\",\n"
      + "    \"description\": \"\",\n"
      + "    \"version\": \"1.0\",\n"
      + "    \"entities\": [\n"
      + "        {\n"
      + "            \"$type\": \"LocalEntity\",\n"
      + "            \"name\": \"SystemMetricsPerformance\",\n"
      + "            \"description\": \"\",\n"
      + "            \"attributes\": [\n"
      + "                {\n"
      + "                    \"name\": \"EdgeDeviceName\",\n"
      + "                    \"dataType\": \"string\"\n"
      + "                },\n"
      + "                {\n"
      + "                    \"name\": \"CpuUsage%\",\n"
      + "                    \"dataType\": \"decimal\"\n"
      + "                },\n"
      + "                {\n"
      + "                    \"name\": \"MemUsage%\",\n"
      + "                    \"dataType\": \"decimal\"\n"
      + "                },\n"
      + "                {\n"
      + "                    \"name\": \"RecordedTime\",\n"
      + "                    \"dataType\": \"dateTimeOffset\"\n"
      + "                }\n"
      + "            ],\n";

  private final String EMPTY_PARTITIONS = "            \"partitions\": []\n";

  private final String BASE_MODEL_SUFFIX = "        }\n"
      + "    ]\n"
      + "}";

  private RestClient restClient;

  int counter = 0;

  /**
   * Set Timer to send to ADLS Gen2
   */
  TimerRef adlsTimer;

  @SwimLane("config")
  ValueLane<Value> config = this.<Value>valueLane();

  @SwimLane("model")
  ValueLane<String> model = this.<String>valueLane();

  @SwimLane("information")
  ValueLane<Value> information = this.<Value>valueLane();

  @SwimTransient
  @SwimLane("publishHistory")
  MapLane<Integer, String> publishHistory = this.<Integer, String>mapLane();

  @SwimTransient
  @SwimLane("dataHistory")
  MapLane<Long, Value> dataHistory = this.<Long, Value>mapLane()
      .didUpdate((k, n, o) -> {
        if (this.dataHistory.size() > MAX_HISTORY_SIZE) {
          for (int i = 0; i < this.dataHistory.size() - MAX_HISTORY_SIZE; i++) {
            this.dataHistory.remove(this.dataHistory.getIndex(i).getKey());
          }
        }
      });

  @SwimLane("addData")
  CommandLane<Value> addData = this.<Value>commandLane()
      .onCommand(value -> {
        dataHistory.put(System.currentTimeMillis(), value);
      });

  private void adlsTimer() {
    adlsTimer = setTimer(MAX_HISTORY_SIZE * 1000L, this::sendToADLS);
  }

  private void sendToADLS() {
    try {
      publish();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void publish() throws Exception{
    final long now = System.currentTimeMillis();
    final String content = generateContent();
    final String fileName = COUNT_FILE_PREFIX + counter + ".csv";
    info("fileName in publish(): " + COUNT_FILE_PREFIX);
    info("dir name in publish(): " + DIR_NAME);
    publishToAdls(content, fileName);
    Thread.sleep(500);
    info("Finish File update");
    updateInfo(content, now);

    if (counter >= ADLS_FILE_COUNT) {
      deleteOldFile(counter - ADLS_FILE_COUNT);
      Thread.sleep(30000);
    }
    updateModel(BASE_FILE_PREFIX + counter + ".csv", now);
    Thread.sleep(5000);
    counter += 1;

    adlsTimer();
  }

  private String generateContent() {
    final OrderedMap<Long, Value> data = dataHistory.snapshot();
    StringBuilder sb = new StringBuilder();
    final DateTimeFormatter df = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    sb.append("EdgeDeviceName");
    sb.append(",");
    sb.append("CpuUsage%");
    sb.append(",");
    sb.append("MemUsage%");
    sb.append(",");
    sb.append("RecordedTime");
    sb.append(System.lineSeparator());

    for(Map.Entry<Long, Value> entry : data.entrySet()) {
      final String deviceName = entry.getValue().get("edgeDeviceName").stringValue();
      final double cpuPct = entry.getValue().get("cpuPct").doubleValue();
      final double memPct = entry.getValue().get("memPct").doubleValue();
      final ZonedDateTime date = Instant.ofEpochMilli(entry.getKey()).atZone(ZoneId.systemDefault());
      sb.append(deviceName);
      sb.append(",");
      sb.append(cpuPct);
      sb.append(",");
      sb.append(memPct);
      sb.append(",");
      sb.append(df.format(date));
      sb.append(System.lineSeparator());
    }
    return sb.toString();
  }

  private void initClient() {
    if (initFlag) {
      initFields();
      initConfig();
      try {
        initModel();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      initFlag = false;
    }
  }

  private void updateInfo(String content, long time) {
    final Record rec = Record.create(3).slot("publishCount", counter)
        .slot("publishTime", time).slot("lastPublishSize", content.length());
    information.set(rec);
  }

  private void publishToAdls(String content, String fileName) throws Exception{
    final int response = restClient.createFile(fileName);
    if (response == 201) {
      restClient.updateFile(fileName, content);
    }
  }

  private void deleteOldFile(int fileNumber) throws Exception{
    final String fileName = COUNT_FILE_PREFIX + fileNumber + ".csv";
    restClient.deleteFile(fileName);
  }

  private void updateModel(String fileName, long now) throws Exception{
    if (counter >= ADLS_FILE_COUNT) {
      publishHistory.remove(counter - ADLS_FILE_COUNT);
    }

    final DateTimeFormatter df = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    final StringBuilder latest = new StringBuilder("                {\n");
    latest.append("                    \"name\": \"");
    latest.append(fileName);
    latest.append("\",\n");
    latest.append("                    \"refreshTime\": \"");

    final ZonedDateTime date = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault());
    latest.append(df.format(date));
    latest.append("\",\n");

    latest.append("                    \"location\": \"");
    latest.append(DIR_URL + fileName);
    latest.append("\"\n");
    latest.append("                }");
    publishHistory.put(counter, latest.toString());

    final StringBuilder sb = new StringBuilder();
    sb.append("            \"partitions\": [\n");

    boolean first = true;
    final Iterator<Integer> it = publishHistory.keyIterator();
    while (it.hasNext()) {
      if (!first) {
        sb.append(",\n");
      } else {
        first = false;
      }
      sb.append(publishHistory.get(it.next()));
    }
    sb.append("\n            ]\n");

    final String modelContents = BASE_MODEL_PREFIX + sb.toString() + BASE_MODEL_SUFFIX;
    model.set(modelContents);
    publishToAdls(modelContents, MODEL_FILE);
  }

  private void initConfig() {
    final Record rec = Record.create(3).slot("name", DIR_NAME)
        .slot("URL", DIR_URL)
        .slot("ContentType", "application/octet-stream");
    config.set(rec);
  }

  private void initModel() throws Exception{
    final String contents = BASE_MODEL_PREFIX + EMPTY_PARTITIONS + BASE_MODEL_SUFFIX;
    model.set(contents);
    publishToAdls(contents, MODEL_FILE);
  }

  @Override
  public void willStop() {
    if (adlsTimer != null) {
      adlsTimer.cancel();
      adlsTimer = null;
    }
  }

  @Override
  public void didStart() {
    info(Record.create(2)
        .slot("nodeUri", nodeUri().toString())
        .slot("didStart"));
    restClient = new RestClient(EnvConfig.ADLS_ACCOUNT_NAME, EnvConfig.ADLS_ACCOUNT_KEY);
    initClient();
    adlsTimer();
  }

}
