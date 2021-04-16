package swim.iot.azure.adls;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class Headers {

  static final DateTimeFormatter RFC_1123_GMT_DATE_FORMATTER =
      DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ROOT).withZone(ZoneId.of("GMT"));

  /**
   * The master Microsoft Azure Storage header prefix.
   */
  static final String PREFIX_FOR_STORAGE_HEADER = "x-ms-";

  /**
   * The default type for content-type and accept.
   */
  static final String UTF8_CHARSET = "UTF-8";

  /**
   * The Authorization header.
   */
  static final String AUTHORIZATION = "Authorization";

  /**
   * The header that indicates the client request ID.
   */
  static final String CLIENT_REQUEST_ID_HEADER = PREFIX_FOR_STORAGE_HEADER + "client-request-id";

  /**
   * The ContentEncoding header.
   */
  static final String CONTENT_ENCODING = "Content-Encoding";

  /**
   * The ContentLangauge header.
   */
  static final String CONTENT_LANGUAGE = "Content-Language";

  /**
   * The ContentLength header.
   */
  static final String CONTENT_LENGTH = "Content-Length";

  /**
   * The ContentMD5 header.
   */
  static final String CONTENT_MD5 = "Content-MD5";

  /**
   * The ContentType header.
   */
  static final String CONTENT_TYPE = "Content-Type";

  /**
   * The header that specifies the date.
   */
  static final String DATE = PREFIX_FOR_STORAGE_HEADER + "date";

  /**
   * The IfMatch header.
   */
  static final String IF_MATCH = "If-Match";

  /**
   * The IfModifiedSince header.
   */
  static final String IF_MODIFIED_SINCE = "If-Modified-Since";

  /**
   * The IfNoneMatch header.
   */
  static final String IF_NONE_MATCH = "If-None-Match";

  /**
   * The IfUnmodifiedSince header.
   */
  static final String IF_UNMODIFIED_SINCE = "If-Unmodified-Since";

  /**
   * The Range header.
   */
  static final String RANGE = "Range";

  /**
   * The version header.
   */
  static final String VERSION = "x-ms-version";

  /**
   * The UserAgent header.
   */
  static final String USER_AGENT = "User-Agent";

  private Headers() {

  }
}