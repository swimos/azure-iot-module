package swim.iot;

import swim.api.plane.AbstractPlane;
import swim.api.space.Space;
import swim.iot.util.EnvConfig;
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

    System.out.println("Azure Environment Setup:");
    System.out.println("EVENT_HUB_CONNSTRING: " + EnvConfig.EVENT_HUB_CONNSTRING);
    System.out.println("EVENT_HUB_NAME: " + EnvConfig.EVENT_HUB_NAME);
    System.out.println("EDGE_DEVICE_NAME: " + EnvConfig.EDGE_DEVICE_NAME);
    System.out.println("ADLS_ACCOUNT_NAME: " + EnvConfig.ADLS_ACCOUNT_NAME);
    System.out.println("ADLS_ACCOUNT_KEY: " + EnvConfig.ADLS_ACCOUNT_KEY);
    System.out.println("FILE_SYSTEM: " + EnvConfig.FILE_SYSTEM);

    kernel.run();
  }

}
