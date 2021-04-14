package swim.iot.agent;

import swim.api.SwimLane;
import swim.api.SwimTransient;
import swim.api.agent.AbstractAgent;
import swim.api.lane.MapLane;
import swim.api.lane.ValueLane;
import swim.concurrent.TimerRef;
import swim.structure.Record;

public class SimulationAgent extends AbstractAgent {

  /**
   * Fixed MAX History MapLane Size
   */
  private final int MAX_HISTORY_SIZE = 100;

  /**
   * Set Timer for generate random data every 15 seconds
   */
  TimerRef dataTimer;

  /**
   * Swim ValueLane stores latest cpu Usage, with Object type Double
   */
  @SwimLane("cpuPercent")
  ValueLane<Double> cpuPercent = this.<Double>valueLane();

  /**
   * Swim CPU Usage History MapLane
   */
  @SwimTransient
  @SwimLane("cpuPercentHistory")
  MapLane<Long, Double> cpuPercentHistory = this.<Long, Double>mapLane()
      .didUpdate((k, n, o) -> {
        info("cpuPercentHistory = { tm: " + k + "; cpuPercent: " + n + "}");
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
   * Swim Meta Mesh Memory Usage History MapLane
   */
  @SwimTransient
  @SwimLane("memPercentHistory")
  MapLane<Long, Double> memPercentHistory = this.<Long, Double>mapLane()
      .didUpdate((k, n, o) -> {
        info("memPercentHistory = { tm: " + k + "; memPercent: " + n + "}");
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

  private void dataTimer() {
    dataTimer = setTimer(1000, this::updateSimulation);
  }

  private void updateSimulation() {
    double cpuUsage = Math.round(Math.random() * 100 * 10.0) / 10.0;
    double memUsage = Math.round(Math.random() * 100 * 10.0) / 10.0;
    double cpuPercent_value = cpuUsage / cpuTotal.get() * 100;
    double memPercent_value = memUsage / memTotal.get() * 100;

    cpuPercent.set(cpuPercent_value);
    memPercent.set(memPercent_value);

    long tm = System.currentTimeMillis();
    cpuPercentHistory.put(tm, cpuPercent_value);
    memPercentHistory.put(tm, memPercent_value);

    dataTimer();
  }

  private Record dataGenerator() {
    Record record = Record.create(3)
        .slot("edgeDeviceHost", "localDevice")// ConfigEnv.EDGE_NAME)
        .slot("cpuPct", cpuPercent.get())
        .slot("memPct", memPercent.get());
    return record;
  }

  @Override
  public void willStop() {
    if (dataTimer != null) {
      dataTimer.cancel();
      dataTimer = null;
    }
  }

  @Override
  public void didStart() {
    info(Record.create(2)
        .slot("nodeUri", nodeUri().toString())
        .slot("didStart"));
    cpuTotal.set(100L);
    memTotal.set(100L);
    dataTimer();
  }
}
