package swim.iot;

import swim.api.plane.AbstractPlane;
import swim.api.space.Space;
import swim.kernel.Kernel;
import swim.server.ServerLoader;

/**
 * Swim Plane starts kernel and space to run Swim Agents
 */
public class SwimPlane extends AbstractPlane {

  public static void main(String[] args) {
    final Kernel kernel = ServerLoader.loadServer();
    /* Space name needs to match with fabric naming in server.recon*/
    final Space space = kernel.getSpace("iot");

    kernel.start();
    System.out.println("Running Swim Azure IoT Module SwimPlane...");

    System.out.println(System.getenv("EVENT_HUB_CONNSTRING"));
    System.out.println(System.getenv("EVENT_HUB_NAME"));
    System.out.println(System.getenv("EDGE_NAME"));
    System.out.println(System.getenv("ADLS_ACCOUNT_NAME"));
    System.out.println(System.getenv("ADLS_ACCOUNT_KEY"));
    System.out.println(System.getenv("FILE_SYSTEM"));

    kernel.run();
  }

}
