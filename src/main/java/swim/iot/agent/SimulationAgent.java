package swim.iot.agent;

import swim.api.agent.AbstractAgent;
import swim.structure.Record;

public class SimulationAgent extends AbstractAgent {

  @Override
  public void didStart() {
    info(Record.create(2)
        .slot("nodeUri", nodeUri().toString())
        .slot("didStart"));
  }
}
