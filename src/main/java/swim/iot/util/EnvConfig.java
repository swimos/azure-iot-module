package swim.iot.util;

public class EnvConfig {
  public static final String EVENT_HUB_CONNSTRING = envCorrection(System.getenv("EVENT_HUB_CONNSTRING"), "");
  public static final String EVENT_HUB_NAME = envCorrection(System.getenv("EVENT_HUB_NAME"), "");

  public static final String EDGE_NAME = envCorrection(System.getenv("EDGE_NAME"), "");

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
