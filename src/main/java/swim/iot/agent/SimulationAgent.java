package swim.iot.agent;

import java.text.DecimalFormat;
import swim.api.SwimLane;
import swim.api.SwimTransient;
import swim.api.agent.AbstractAgent;
import swim.api.lane.MapLane;
import swim.api.lane.ValueLane;
import swim.concurrent.TimerRef;
import swim.iot.azure.SendToEventHub;
import swim.iot.util.EnvConfig;
import swim.recon.Recon;
import swim.structure.Record;

/**
 * Simulation Swim Web Agent that generate CPU and Memory usage percentage
 * Send Json structure record to Event Hub and AdlsAgent if Azure resources account
 * have been set in EnvConfig.java
 */
public class SimulationAgent extends AbstractAgent {

  /**
   * Fixed MAX History MapLane Size
   */
  private final int MAX_HISTORY_SIZE = 100;

  /**
   * Set Timer for generate random data every second
   */
  TimerRef dataTimer;

  /**
   * Set Timer to send to Azure Event Hub
   */
  TimerRef eventHubTimer;

  /**
   * Swim ValueLane stores latest cpu Usage, with Object type Double
   */
  @SwimLane("cpuPercent")
  ValueLane<Double> cpuPercent = this.<Double>valueLane();

  /**
   * Swim CPU Usage History MapLane, key is long timestamp of cpu data generated
   */
  @SwimTransient
  @SwimLane("cpuPercentHistory")
  MapLane<Long, Double> cpuPercentHistory = this.<Long, Double>mapLane()
      .didUpdate((k, n, o) -> {
//        info("cpuPercentHistory = { tm: " + k + "; cpuPercent: " + n + "}");
        if (this.cpuPercentHistory.size() > MAX_HISTORY_SIZE) {
          for (int i = 0; i < this.cpuPercentHistory.size() - MAX_HISTORY_SIZE; i++) {
            this.cpuPercentHistory.remove(this.cpuPercentHistory.getIndex(i).getKey());
          }
        }
      });

  /**
   * Swim ValueLane stores latest Machine CPU total
   */
  @SwimLane("cpuTotal")
  ValueLane<Long> cpuTotal = valueLane();

  /**
   * Swim ValueLane stores Machine Memory Usage
   */
  @SwimLane("memPercent")
  ValueLane<Double> memPercent = this.<Double>valueLane();

  /**
   * Swim Memory Usage History MapLane, key is long timestamp of memory data generated
   */
  @SwimTransient
  @SwimLane("memPercentHistory")
  MapLane<Long, Double> memPercentHistory = this.<Long, Double>mapLane()
      .didUpdate((k, n, o) -> {
//        info("memPercentHistory = { tm: " + k + "; memPercent: " + n + "}");
        if (this.memPercentHistory.size() > MAX_HISTORY_SIZE) {
          for (int i = 0; i < this.memPercentHistory.size() - MAX_HISTORY_SIZE; i++) {
            this.memPercentHistory.remove(this.memPercentHistory.getIndex(i).getKey());
          }
        }
      });

  /**
   * Swim ValueLane stores Machine Memory Total
   */
  @SwimLane("memTotal")
  ValueLane<Long> memTotal = valueLane();

  /**
   * Swim Timer to trigger @method{updateSimulation} every second
   */
  private void dataTimer() {
    dataTimer = setTimer(1000, this::updateSimulation);
  }

  /**
   * Method to simulate random CPU and Memory Percentage Usage
   */
  private void updateSimulation() {
    DecimalFormat df = new DecimalFormat("#.#");
    double cpuUsage_random = Math.round(Math.random() * 100);
    double cpuUsage = Double.parseDouble(df.format(cpuUsage_random));
    double memUsage_random = Math.round(Math.random() * 100);
    double memUsage = Double.parseDouble(df.format(memUsage_random));
    double cpuPercent_value = cpuUsage / (double) cpuTotal.get() * 100;
    double memPercent_value = memUsage / (double) memTotal.get() * 100;

    /* Set cpuPercent ValueLane with latest simulation*/
    cpuPercent.set(cpuPercent_value);
    /* Set memPercent ValueLane with latest simulation*/
    memPercent.set(memPercent_value);

    long tm = System.currentTimeMillis();
    /* Update history MapLane */
    cpuPercentHistory.put(tm, cpuPercent_value);
    memPercentHistory.put(tm, memPercent_value);

    /* Restart dataTimer to count down simulator */
    dataTimer();
    /* Send cpu and memory usage command to ADLS Agent */
    if (!EnvConfig.ADLS_ACCOUNT_NAME.isEmpty() && !EnvConfig.ADLS_ACCOUNT_KEY.isEmpty()) {
      command("/adls" + "/" + EnvConfig.ADLS_ACCOUNT_NAME, ADLS_LANE, dataGenerator());
    }
  }

  /**
   * Generate Event Timer to send data to Event Hub every 30 secs
   */
  private void eventHubTimer() {
    eventHubTimer = setTimer(30000, this::sendToEventHub);
  }

  /**
   * Method to Send data to Event Hub, and reschedule timer
   */
  private void sendToEventHub() {
    SendToEventHub.publishEvents(Recon.toString(dataGenerator()));
    info("send to event hub");
    eventHubTimer();
  }

  /**
   * Helper Method help generate Json structure data send to Azure Event Hub and AdlsAgent
   * @return Record of edgeDeviceName, cuptPct, and memPct
   */
  private Record dataGenerator() {
    Record record = Record.create(3)
        .slot("edgeDeviceName", EnvConfig.EDGE_DEVICE_NAME)
        .slot("cpuPct", cpuPercent.get())
        .slot("memPct", memPercent.get());
    return record;
  }

  /**
   * Method called when Agent stop to cancel timers
   */
  @Override
  public void willStop() {
    if (dataTimer != null) {
      dataTimer.cancel();
      dataTimer = null;
    }
    if (eventHubTimer != null) {
      eventHubTimer.cancel();
      eventHubTimer = null;
    }
  }

  /**
   * didStart() method is first method get called when a Swim Web Agent starts
   */
  @Override
  public void didStart() {
    /* Print uri pattern of this web agent, info() is one of the log print level*/
    info(Record.create(2)
        .slot("nodeUri", nodeUri().toString())
        .slot("didStart"));
    cpuTotal.set(100L);
    memTotal.set(100L);
    /* Swim timer to count down to send data to Azure Event Hub*/
    dataTimer();
    if (!EnvConfig.EVENT_HUB_CONNSTRING.isEmpty() && !EnvConfig.EVENT_HUB_NAME.isEmpty()) {
      eventHubTimer();
    }
  }

  /**
   * ADLS command Lane name that waiting receive command messages
   */
  private static final String ADLS_LANE = "addData";
}
