package swim.iot.agent;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import com.azure.storage.file.datalake.DataLakeFileSystemClient;
import com.azure.storage.file.datalake.DataLakeServiceClient;
import swim.api.SwimLane;
import swim.api.SwimTransient;
import swim.api.agent.AbstractAgent;
import swim.api.lane.CommandLane;
import swim.api.lane.MapLane;
import swim.concurrent.TimerRef;
import swim.iot.azure.SendToADLSGenII;
import swim.iot.util.EnvConfig;
import swim.recon.Recon;
import swim.structure.Record;
import swim.structure.Value;
import swim.util.OrderedMap;

public class AdlsAgent extends AbstractAgent {

  private SendToADLSGenII adls;
  private DataLakeServiceClient sdClient;
  private DataLakeFileSystemClient dlFC;
  /**
   * Fixed MAX History MapLane Size
   */
  private final int MAX_HISTORY_SIZE = 60;

  /**
   * Set Timer to send to ADLS Gen2
   */
  TimerRef adlsTimer;

  private void adlsTimer() {
    adlsTimer = setTimer(30000, this::sendToADLS);
  }

  private void sendToADLS() {
    // todo, read data History
    final OrderedMap<Long, Value> data = dataHistory.snapshot();
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    sb.append(System.lineSeparator());
    for(Map.Entry<Long, Value> entry : data.entrySet()) {
      sb.append("{");
      sb.append(convertTMString(entry.getKey())).append(", ").append(Recon.toString(entry.getValue()));
      sb.append("}").append(System.lineSeparator());
    }
    sb.append("}");
    info(sb.toString()); // works
//    try {
//      adls.uploadFile(dlFC, sb.toString());
//    } catch (FileNotFoundException e) {
//      e.printStackTrace();
//    }

    adlsTimer();
  }

  @SwimTransient
  @SwimLane("dataHistory")
  MapLane<Long, Value> dataHistory = this.<Long, Value>mapLane()
      .didUpdate((k, n, o) -> {
//        info("adlsHistory = { tm: " + k + "; value in adls: " + n + "}");
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

  private void initClient() {
    // fixme: ADLS connection
    //adls = new SendToADLSGenII(EnvConfig.ADLS_ACCOUNT_NAME, EnvConfig.ADLS_ACCOUNT_KEY);
    sdClient = SendToADLSGenII.getDataLakeServiceClient(EnvConfig.ADLS_ACCOUNT_NAME, EnvConfig.ADLS_ACCOUNT_KEY);
    // fixme: reactor.core.Exceptions$ReactiveException: io.netty.channel.socket.ChannelOutputShutdownException: Channel output shutdown
    //        cannot create container and directory
    dlFC = sdClient.createFileSystem(EnvConfig.FILE_SYSTEM);
    dlFC.createDirectory("my-directory");


//    sdClient.createFileSystemWithResponse(EnvConfig.FILE_SYSTEM);
    //todo: try to create container directly, avoid this line, and create directory directly
//    dlFC = adls.CreateFileSystem(sdClient);
//    adls.CreateDirectory(sdClient, EnvConfig.FILE_SYSTEM);

  }

  private String convertTMString(long tm) {
    Date date = new Date(tm);
    DateFormat formatter = new SimpleDateFormat("HH:mm:ss.SSS");
    formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    return formatter.format(date);
  }

  @Override
  public void willStop() {
    if (adlsTimer != null) {
      adlsTimer.cancel();
      adlsTimer = null;
    }
    sdClient = null;
  }

  @Override
  public void didStart() {
    info(Record.create(2)
        .slot("nodeUri", nodeUri().toString())
        .slot("didStart"));
    initClient();
    adlsTimer();
  }

}
