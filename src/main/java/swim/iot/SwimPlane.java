package swim.iot;

import swim.api.plane.AbstractPlane;
import swim.api.space.Space;
import swim.kernel.Kernel;
import swim.server.ServerLoader;
import swim.structure.Value;

public class SwimPlane extends AbstractPlane {

  public static void main(String[] args) {
    final Kernel kernel = ServerLoader.loadServer();
    final Space space = kernel.getSpace("iot");

    kernel.start();
    System.out.println("Running Swim Azure IoT Module SwimPlane...");

    space.command("/simulation", "WAKUP", Value.absent());
    kernel.run();
  }

}
