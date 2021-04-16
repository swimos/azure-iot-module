package swim.iot.util;

public class EnvConfig {

  /**
   * Configuration environment variables for Event Hub
   */
  public static final String EVENT_HUB_CONNSTRING = envCorrection(System.getenv("EVENT_HUB_CONNSTRING"), "");
  public static final String EVENT_HUB_NAME = envCorrection(System.getenv("EVENT_HUB_NAME"), "");

  /**
   * Configuration environment variable for device name
   */
  public static final String EDGE_DEVICE_NAME = envCorrection(System.getenv("EDGE_NAME"), "localSimulator");

  /**
   * Configuration environment variables for ADLS Gen2
   */
  public static final String ADLS_ACCOUNT_NAME = envCorrection(System.getenv("ADLS_ACCOUNT_NAME"), "");
  public static final String ADLS_ACCOUNT_KEY = envCorrection(System.getenv("ADLS_ACCOUNT_KEY"), "");
  public static final String FILE_SYSTEM = envCorrection(System.getenv("FILE_SYSTEM"), "");

  /**
   * Helper function that standards parsing environment variables
   *
   * @param env
   * @return "" or env.trim()
   */
  private static String envCorrection(String env, String defaultValue) {
    if (env == null || env.isEmpty()) {
      return defaultValue;
    } else return env.trim();
  }
}
