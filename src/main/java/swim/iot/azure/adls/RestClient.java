package swim.iot.azure.adls;

import java.io.IOException;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.Future;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;

public class RestClient {
  private static final String EMPTY_STRING = "";

  private final String accountName;

  private final byte[] accountKey;

  private final String baseUrl;

  public RestClient(String accountName, String accountKey) {
    this.accountName = accountName;
    this.accountKey = Base64.getDecoder().decode(accountKey);
    this.baseUrl = "https://" + accountName + ".dfs.core.windows.net";
  }

  public int fileList() throws Exception{
    final String fileListUrl = baseUrl + "?resource=account";
    return get(fileListUrl);
  }

  public int pathList(String path) throws Exception {
    final String pathListUrl = baseUrl + path + "?recursive=true&resource=filesystem";
    return get(pathListUrl);
  }

  public int createDirectory(String path) throws Exception{
    final String url = baseUrl + path + "?resource=directory";
    return put(url);
  }

  public int createFile(String path) throws Exception{
    final String url = baseUrl + path + "?resource=file";
    return put(url);
  }

  public int updateFile(String path, String textContent) throws Exception{
    final String appendUrl = baseUrl + path + "?action=append&position=0";
    final int responseCode = patch(appendUrl, textContent);
    if (responseCode == 202) {
      final String flushUrl = baseUrl + path + "?action=flush&position=" + textContent.length();
      return patch(flushUrl, EMPTY_STRING);
    } else {
      return responseCode;
    }
  }

  public int deleteFile(String path) throws Exception{
    final String url = baseUrl + path;
    return delete(url);
  }

  public int deleteDirectory(String path) throws Exception{
    final String url = baseUrl + path + "?recursive=true";
    return delete(url);
  }

  private String buildStringToSign(String method, URL url, Map<String, String> httpHeaders) {
    String contentLength = getStandardHeaderValue(httpHeaders, Headers.CONTENT_LENGTH);
    contentLength = contentLength.equals("0") ? EMPTY_STRING : contentLength;

    return String.join("\n",
        method,
        getStandardHeaderValue(httpHeaders, Headers.CONTENT_ENCODING),
        getStandardHeaderValue(httpHeaders, Headers.CONTENT_LANGUAGE),
        contentLength,
        getStandardHeaderValue(httpHeaders, Headers.CONTENT_MD5),
        getStandardHeaderValue(httpHeaders, Headers.CONTENT_TYPE),
        EMPTY_STRING,
        getStandardHeaderValue(httpHeaders, Headers.IF_MODIFIED_SINCE),
        getStandardHeaderValue(httpHeaders, Headers.IF_MATCH),
        getStandardHeaderValue(httpHeaders, Headers.IF_NONE_MATCH),
        getStandardHeaderValue(httpHeaders, Headers.IF_UNMODIFIED_SINCE),
        getStandardHeaderValue(httpHeaders, Headers.RANGE),
        getAdditionalXmsHeaders(httpHeaders),
        getCanonicalizedResource(url)
    );
  }

  private String getStandardHeaderValue(Map<String, String> httpHeaders, final String headerName) {
    final String headerValue = httpHeaders.get(headerName);
    return headerValue == null ? EMPTY_STRING : headerValue;
  }

  private String getAdditionalXmsHeaders(Map<String, String> httpHeaders) {
    // Add only headers that begin with 'x-ms-'
    final ArrayList<String> xmsHeaderNameArray = new ArrayList<>();
    for (String header : httpHeaders.keySet()) {
      final String lowerCaseHeader = header.toLowerCase(Locale.ROOT);
      if (lowerCaseHeader.startsWith(Headers.PREFIX_FOR_STORAGE_HEADER)) {
        xmsHeaderNameArray.add(lowerCaseHeader);
      }
    }

    if (xmsHeaderNameArray.isEmpty()) {
      return EMPTY_STRING;
    }

    Collections.sort(xmsHeaderNameArray);

    final StringBuilder canonicalizedHeaders = new StringBuilder();
    for (final String key : xmsHeaderNameArray) {
      if (canonicalizedHeaders.length() > 0) {
        canonicalizedHeaders.append('\n');
      }
      canonicalizedHeaders.append(key);
      canonicalizedHeaders.append(':');
      canonicalizedHeaders.append(httpHeaders.get(key));
    }

    return canonicalizedHeaders.toString();
  }

  private String getCanonicalizedResource(URL requestURL) {
    // Resource path
    final StringBuilder canonicalizedResource = new StringBuilder("/");
    canonicalizedResource.append(this.accountName);

    // Note that AbsolutePath starts with a '/'.
    if (requestURL.getPath().length() > 0) {
      canonicalizedResource.append(requestURL.getPath());
    } else {
      canonicalizedResource.append('/');
    }

    // check for no query params and return
    if (requestURL.getQuery() == null) {
      return canonicalizedResource.toString();
    }

    final QueryStringDecoder queryDecoder = new QueryStringDecoder("?" + requestURL.getQuery());
    final Map<String, List<String>> queryParams = queryDecoder.parameters();

    final ArrayList<String> queryParamNames = new ArrayList<>(queryParams.keySet());
    Collections.sort(queryParamNames);

    for (String queryParamName : queryParamNames) {
      final List<String> queryParamValues = queryParams.get(queryParamName);
      Collections.sort(queryParamValues);
      final String queryParamValuesStr = String.join(",", queryParamValues.toArray(new String[]{}));
      canonicalizedResource.append("\n").append(queryParamName.toLowerCase(Locale.ROOT)).append(":")
          .append(queryParamValuesStr);
    }

    return canonicalizedResource.toString();
  }

  private String computeHmac256(final String stringToSign) {
    try {
      final Mac hmacSha256 = Mac.getInstance("HmacSHA256");
      hmacSha256.init(new SecretKeySpec(this.accountKey, "HmacSHA256"));
      final byte[] utf8Bytes = stringToSign.getBytes(Headers.UTF8_CHARSET);
      return Base64.getEncoder().encodeToString(hmacSha256.doFinal(utf8Bytes));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private int get(final String url) throws Exception {
    CloseableHttpAsyncClient asyncClient = HttpAsyncClients.createDefault();
    try {
      final Map<String, String> headers = commonHeaders(0);
      final String stringToSign = buildStringToSign("GET", new URL(url), headers);
      final String computedBase64Signature = computeHmac256(stringToSign);
      headers.put(Headers.AUTHORIZATION,
          "SharedKey " + this.accountName + ":" + computedBase64Signature);

      asyncClient.start();
      final HttpGet req = new HttpGet(url);

      for (String key : headers.keySet()) {
        if (!key.equals(Headers.CONTENT_LENGTH)) {
          req.addHeader(key, headers.get(key));
        }
      }

      Future<HttpResponse> future = asyncClient.execute(req, null);
      final HttpResponse response = future.get();
      final int responseCode = response.getStatusLine().getStatusCode();

      System.out.println("Get request to URL : " + url);
      System.out.println("Response Code : " + responseCode);

      final Header[] responseHeaders = response.getAllHeaders();
      for (Header header : responseHeaders) {
        System.out.println(header.getName() + ":" + header.getValue());
      }

      return responseCode;
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      asyncClient.close();
      System.out.println("Get Done");
    }
  }

  private int put(final String url) throws Exception {
    CloseableHttpAsyncClient asyncClient = HttpAsyncClients.createDefault();
    try {
      final Map<String, String> headers = commonHeaders(0);
      final String stringToSign = buildStringToSign("PUT", new URL(url), headers);
      final String computedBase64Signature = computeHmac256(stringToSign);
      headers.put(Headers.AUTHORIZATION,
          "SharedKey " + this.accountName + ":" + computedBase64Signature);

      asyncClient.start();
      final HttpPut req = new HttpPut(url);

      for (String key : headers.keySet()) {
        if (!key.equals(Headers.CONTENT_LENGTH)) {
          req.addHeader(key, headers.get(key));
        }
      }

      Future<HttpResponse> future = asyncClient.execute(req, null);
      final HttpResponse response = future.get();
      final int responseCode = response.getStatusLine().getStatusCode();

      System.out.println("Put request to URL : " + url);
      System.out.println("Response Code : " + responseCode);
      final Header[] responseHeaders = response.getAllHeaders();
      for (Header header : responseHeaders) {
        System.out.println(header.getName() + ":" + header.getValue());
      }

      return responseCode;

    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      asyncClient.close();
      System.out.println("Put Done");
    }
  }

  private int patch(final String url, String textContent) throws Exception{
    CloseableHttpAsyncClient asyncClient = HttpAsyncClients.createDefault();
    try {
      final Map<String, String> headers = commonHeaders(textContent.length());
      headers.put(Headers.CONTENT_TYPE, "text/plain");
      final String stringToSign = buildStringToSign("PATCH", new URL(url), headers);
      final String computedBase64Signature = computeHmac256(stringToSign);
      headers.put(Headers.AUTHORIZATION,
          "SharedKey " + this.accountName + ":" + computedBase64Signature);

      asyncClient.start();
      final HttpPatch req = new HttpPatch(url);

      for (String key : headers.keySet()) {
        if (!key.equals(Headers.CONTENT_LENGTH)) {
          req.addHeader(key, headers.get(key));
        }
      }

      if (textContent != null && !textContent.equals(EMPTY_STRING)) {
        req.setEntity(new StringEntity(textContent));
      }

      Future<HttpResponse> future = asyncClient.execute(req, null);
      final HttpResponse response = future.get();

      final int responseCode = response.getStatusLine().getStatusCode();

      System.out.println("Patch request to URL : " + url);
      System.out.println("Response Code : " + responseCode);
      final Header[] respHeaders = response.getAllHeaders();
      for (Header header: respHeaders) {
        System.out.println(header.getName() + ":" + header.getValue());
      }

      return responseCode;

    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      asyncClient.close();
      System.out.println("Patch Done");
    }
  }

  private int delete(final String url) throws Exception {
    CloseableHttpAsyncClient asyncClient = HttpAsyncClients.createDefault();
    try {
      final Map<String, String> headers = commonHeaders(0);
      final String stringToSign = buildStringToSign("DELETE", new URL(url), headers);
      final String computedBase64Signature = computeHmac256(stringToSign);
      headers.put(Headers.AUTHORIZATION,
          "SharedKey " + this.accountName + ":" + computedBase64Signature);

      asyncClient.start();
      final HttpDelete req = new HttpDelete(url);

      for (String key : headers.keySet()) {
        if (!key.equals(Headers.CONTENT_LENGTH)) {
          req.addHeader(key, headers.get(key));
        }
      }

      Future<HttpResponse> future = asyncClient.execute(req, null);
      final HttpResponse response = future.get();
      final int responseCode = response.getStatusLine().getStatusCode();

      System.out.println("Delete request to URL: " + url);
      System.out.println("Response Code : " + responseCode);
      final Header[] responseHeader = response.getAllHeaders();
      for (Header header: responseHeader) {
        System.out.println(header.getName() + ":" + header.getValue());
      }

      return responseCode;
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      asyncClient.close();
      System.out.println("Delete Done");
    }
  }


  private Map<String, String> commonHeaders(int contentLength) {
    final Map<String, String> headers = new HashMap<>();
    headers.put(Headers.DATE,
        Headers.RFC_1123_GMT_DATE_FORMATTER.format(OffsetDateTime.now()));
    headers.put(Headers.CONTENT_LENGTH, Integer.toString(contentLength));
    headers.put(Headers.VERSION, "2018-11-09");
    headers.put(Headers.CLIENT_REQUEST_ID_HEADER, UUID.randomUUID().toString());
    headers.put(Headers.USER_AGENT, "Azure-Storage/11.0.1 (JavaJRE 11.0.1; Linux 4.15.0-48-generic)");
    return headers;
  }

}
