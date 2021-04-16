package swim.iot.azure.adls;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.util.*;

public class QueryStringDecoder {
  private final Charset charset;
  private final String uri;
  private final int maxParams;
  private int pathEndIdx;
  private String path;
  private Map<String, List<String>> params;

  public QueryStringDecoder(String uri) {
    this(uri, Charset.forName("UTF-8"));
  }

  public QueryStringDecoder(String uri, boolean hasPath) {
    this(uri, Charset.forName("UTF-8"), hasPath);
  }

  public QueryStringDecoder(String uri, Charset charset) {
    this(uri, charset, true);
  }

  public QueryStringDecoder(String uri, Charset charset, boolean hasPath) {
    this(uri, charset, hasPath, 1024);
  }

  public QueryStringDecoder(String uri, Charset charset, boolean hasPath, int maxParams) {
    this.uri = uri;
    this.charset = charset;
    this.maxParams = maxParams;
    this.pathEndIdx = hasPath ? -1 : 0;
  }

  public QueryStringDecoder(URI uri) {
    this(uri, Charset.forName("UTF-8"));
  }

  public QueryStringDecoder(URI uri, Charset charset) {
    this(uri, charset, 1024);
  }

  public QueryStringDecoder(URI uri, Charset charset, int maxParams) {
    String rawPath = uri.getRawPath();
    if (rawPath == null) {
      rawPath = "";
    }

    final String rawQuery = uri.getRawQuery();
    this.uri = rawQuery == null ? rawPath : rawPath + '?' + rawQuery;
    this.charset = charset;
    this.maxParams = maxParams;
    this.pathEndIdx = rawPath.length();
  }

  public String toString() {
    return this.uri();
  }

  public String uri() {
    return this.uri;
  }

  public String path() {
    if (this.path == null) {
      this.path = decodeComponent(this.uri, 0, this.pathEndIdx(), this.charset, true);
    }

    return this.path;
  }

  public Map<String, List<String>> parameters() {
    if (this.params == null) {
      this.params = decodeParams(this.uri, this.pathEndIdx(), this.charset, this.maxParams);
    }

    return this.params;
  }

  public String rawPath() {
    return this.uri.substring(0, this.pathEndIdx());
  }

  public String rawQuery() {
    final int start = this.pathEndIdx() + 1;
    return start < this.uri.length() ? this.uri.substring(start) : "";
  }

  private int pathEndIdx() {
    if (this.pathEndIdx == -1) {
      this.pathEndIdx = findPathEndIndex(this.uri);
    }

    return this.pathEndIdx;
  }

  private static Map<String, List<String>> decodeParams(String s, int from, Charset charset, int paramsLimit) {
    final int len = s.length();
    if (from >= len) {
      return Collections.emptyMap();
    } else {
      if (s.charAt(from) == '?') {
        ++from;
      }

      final Map<String, List<String>> params = new LinkedHashMap<>();
      int nameStart = from;
      int valueStart = -1;

      int i;
      label40:
      for (i = from; i < len; ++i) {
        switch (s.charAt(i)) {
          case '#':
            break label40;
          case '&':
          case ';':
            if (addParam(s, nameStart, valueStart, i, params, charset)) {
              --paramsLimit;
              if (paramsLimit == 0) {
                return params;
              }
            }

            nameStart = i + 1;
            break;
          case '=':
            if (nameStart == i) {
              nameStart = i + 1;
            } else if (valueStart < nameStart) {
              valueStart = i + 1;
            }
            break;
          default:
        }
      }

      addParam(s, nameStart, valueStart, i, params, charset);
      return params;
    }
  }

  @SuppressWarnings("unchecked")
  private static boolean addParam(String s, int nameStart, int valueStart, int valueEnd, Map<String, List<String>> params, Charset charset) {
    if (nameStart >= valueEnd) {
      return false;
    } else {
      if (valueStart <= nameStart) {
        valueStart = valueEnd + 1;
      }
      final String name = decodeComponent(s, nameStart, valueStart - 1, charset, false);
      final String value = decodeComponent(s, valueStart, valueEnd, charset, false);
      List<String> values = params.get(name);
      if (values == null) {
        values = new ArrayList<>(1);
        params.put(name, values);
      }

      values.add(value);
      return true;
    }
  }

  public static String decodeComponent(String s) {
    return decodeComponent(s, Charset.forName("UTF-8"));
  }

  public static String decodeComponent(String s, Charset charset) {
    return s == null ? "" : decodeComponent(s, 0, s.length(), charset, false);
  }

  private static String decodeComponent(String s, int from, int toExcluded, Charset charset, boolean isPath) {
    final int len = toExcluded - from;
    if (len <= 0) {
      return "";
    } else {
      int firstEscaped = -1;

      for (int i = from; i < toExcluded; ++i) {
        final char c = s.charAt(i);
        if (c == '%' || c == '+' && !isPath) {
          firstEscaped = i;
          break;
        }
      }

      if (firstEscaped == -1) {
        return s.substring(from, toExcluded);
      } else {
        final CharsetDecoder decoder = charset.newDecoder();
        final int decodedCapacity = (toExcluded - firstEscaped) / 3;
        final ByteBuffer byteBuf = ByteBuffer.allocate(decodedCapacity);
        final CharBuffer charBuf = CharBuffer.allocate(decodedCapacity);
        final StringBuilder strBuf = new StringBuilder(len);
        strBuf.append(s, from, firstEscaped);

        for (int i = firstEscaped; i < toExcluded; ++i) {
          final char c = s.charAt(i);
          if (c != '%') {
            strBuf.append(c == '+' && !isPath ? ' ' : c);
          } else {
            byteBuf.clear();

            do {
              if (i + 3 > toExcluded) {
                throw new IllegalArgumentException("unterminated escape sequence at index " + i + " of: " + s);
              }

              byteBuf.put(decodeHexByte(s, i + 1));
              i += 3;
            } while (i < toExcluded && s.charAt(i) == '%');

            --i;
            byteBuf.flip();
            charBuf.clear();
            CoderResult result = decoder.reset().decode(byteBuf, charBuf, true);

            try {
              if (!result.isUnderflow()) {
                result.throwException();
              }

              result = decoder.flush(charBuf);
              if (!result.isUnderflow()) {
                result.throwException();
              }
            } catch (CharacterCodingException var16) {
              throw new IllegalStateException(var16);
            }

            strBuf.append(charBuf.flip());
          }
        }

        return strBuf.toString();
      }
    }
  }

  private static int findPathEndIndex(String uri) {
    final int len = uri.length();

    for (int i = 0; i < len; ++i) {
      final char c = uri.charAt(i);
      if (c == '?' || c == '#') {
        return i;
      }
    }

    return len;
  }

  private static int decodeHexNibble(char c) {
    if (c >= '0' && c <= '9') {
      return c - 48;
    } else if (c >= 'A' && c <= 'F') {
      return c - 55;
    } else {
      return c >= 'a' && c <= 'f' ? c - 87 : -1;
    }
  }

  private static byte decodeHexByte(CharSequence s, int pos) {
    final int hi = decodeHexNibble(s.charAt(pos));
    final int lo = decodeHexNibble(s.charAt(pos + 1));
    if (hi != -1 && lo != -1) {
      return (byte) ((hi << 4) + lo);
    } else {
      throw new IllegalArgumentException(String.format("invalid hex byte '%s' at index %d of '%s'", s.subSequence(pos, pos + 2), pos, s));
    }
  }
}
