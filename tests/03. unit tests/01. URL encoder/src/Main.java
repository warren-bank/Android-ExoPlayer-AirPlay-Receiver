import android.net.Uri;

import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class Main {
  public static void run_test_001(String url_0, String TAG) {
    try {
      String url_1 = Uri.encode(url_0);

      System.out.println(TAG + "encoded: " + url_1);
    }
    catch(Exception e) {
      System.out.println(TAG + "ERROR: " + e.getMessage());
    }
  }

  public static void run_test_002(String url_0, String TAG) {
    try {
      Uri uri = Uri.parse(url_0);
      String url_1 = uri.toString();

      System.out.println(TAG + "encoded: " + url_1);
    }
    catch(Exception e) {
      System.out.println(TAG + "ERROR: " + e.getMessage());
    }
  }

  public static void run_test_003a(String url_0, String TAG) {
    try {
      Uri url = Uri.parse(url_0);

      String sep = "\n  ";
      String url_decoded = sep + url.getScheme() + sep + url.getUserInfo()        + sep + url.getHost() + sep + url.getPort() + sep + url.getPath()        + sep + url.getQuery()        + sep + url.getFragment();
      String url_encoded = sep + url.getScheme() + sep + url.getEncodedUserInfo() + sep + url.getHost() + sep + url.getPort() + sep + url.getEncodedPath() + sep + url.getEncodedQuery() + sep + url.getEncodedFragment();

      System.out.println(TAG + "decoded:" + url_decoded);
      System.out.println(TAG + "encoded:" + url_encoded);
    }
    catch(Exception e) {
      System.out.println(TAG + "ERROR: " + e.getMessage());
    }
  }

  public static void run_test_003b(String url_0, String TAG) {
    try {
      URL url = new URL(url_0);

      String sep = "\n  ";
      String url_decoded = sep + url.getProtocol() + sep + url.getUserInfo() + sep + url.getHost() + sep + url.getPort() + sep + url.getPath() + sep + url.getQuery() + sep + url.getRef();

      System.out.println(TAG + "decoded:" + url_decoded);
    }
    catch(Exception e) {
      System.out.println(TAG + "ERROR: " + e.getMessage());
    }
  }

  public static void run_test_004a(String url_0, String TAG) {
    try {
      URL url = new URL(url_0);

      URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
      String url_1 = uri.toASCIIString();

      System.out.println(TAG + "encoded: " + url_1);
    }
    catch(Exception e) {
      System.out.println(TAG + "ERROR: " + e.getMessage());
    }
  }

  // note: this method represents the current methodology used by the app to encode URLs received as input from the user
  public static void run_test_004b(String url_0, String TAG) {
    try {
      url_0 = URLDecoder.decode(url_0, "UTF-8");

      URL url = new URL(url_0);

      URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
      String url_1 = uri.toASCIIString();

      System.out.println(TAG + "encoded: " + url_1);
    }
    catch(Exception e) {
      System.out.println(TAG + "ERROR: " + e.getMessage());
    }
  }

  public static void run_test_004c(String url_0, String TAG) {
    try {
      Uri url = Uri.parse(url_0);
      URI uri = new URI(url.getScheme(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getFragment());
      String url_1 = uri.toASCIIString();

      System.out.println(TAG + "encoded: " + url_1);
    }
    catch(Exception e) {
      System.out.println(TAG + "ERROR: " + e.getMessage());
    }
  }

  public static void run_test_004d(String url_0, String TAG) {
    try {
      Uri url = Uri.parse(url_0);
      URI uri = new URI(url.getScheme(), url.getEncodedUserInfo(), url.getHost(), url.getPort(), url.getEncodedPath(), url.getEncodedQuery(), url.getEncodedFragment());
      String url_1 = uri.toASCIIString();

      System.out.println(TAG + "encoded: " + url_1);
    }
    catch(Exception e) {
      System.out.println(TAG + "ERROR: " + e.getMessage());
    }
  }

  public static void run_test_005a(String url_0, String TAG) {
    try {
      Uri url = Uri.parse(url_0);

      int iVal;
      String sVal;
      String url_1 = "";
      url_1 += url.getScheme() + "://";
      sVal = url.getEncodedUserInfo();
      if (sVal != null)
        url_1 += sVal + "@";
      url_1 += url.getHost();
      iVal = url.getPort();
      if (iVal > 0)
        url_1 += ":" + iVal;
      url_1 += url.getEncodedPath();
      sVal = url.getEncodedQuery();
      if (sVal != null)
        url_1 += "?" + sVal;
      sVal = url.getEncodedFragment();
      if (sVal != null)
        url_1 += "#" + sVal;

      System.out.println(TAG + "encoded: " + url_1);
    }
    catch(Exception e) {
      System.out.println(TAG + "ERROR: " + e.getMessage());
    }
  }

  public static void run_test_005b(String url_0, String TAG) {
    try {
      Uri url = Uri.parse(url_0);

      int iVal;
      String sVal;
      String url_1 = "";
      url_1 += url.getScheme() + "://";
      sVal = url.getUserInfo();
      if (sVal != null)
        url_1 += encodeComponent(sVal) + "@";
      url_1 += url.getHost();
      iVal = url.getPort();
      if (iVal > 0)
        url_1 += ":" + iVal;
      url_1 += encodeComponent(url.getPath());
      sVal = url.getEncodedQuery();
      if (sVal != null)
        url_1 += "?" + sVal;
      sVal = url.getEncodedFragment();
      if (sVal != null)
        url_1 += "#" + sVal;

      System.out.println(TAG + "encoded: " + url_1);
    }
    catch(Exception e) {
      System.out.println(TAG + "ERROR: " + e.getMessage());
    }
  }

  public static void run_test_005c(String url_0, String TAG) {
    try {
      Uri url = Uri.parse(url_0);

      int iVal;
      String sVal;
      String url_1 = "";
      url_1 += url.getScheme() + "://";
      sVal = url.getUserInfo();
      if (sVal != null)
        url_1 += Uri.encode(sVal, ":") + "@";
      url_1 += url.getHost();
      iVal = url.getPort();
      if (iVal > 0)
        url_1 += ":" + iVal;
      url_1 += Uri.encode(url.getPath(), "/");
      sVal = url.getEncodedQuery();
      if (sVal != null)
        url_1 += "?" + sVal;
      sVal = url.getEncodedFragment();
      if (sVal != null)
        url_1 += "#" + sVal;

      System.out.println(TAG + "encoded: " + url_1);
    }
    catch(Exception e) {
      System.out.println(TAG + "ERROR: " + e.getMessage());
    }
  }

  public static void run_test_005d(String url_0, String TAG) {
    try {
      Uri url = Uri.parse(url_0);

      int iVal;
      String sVal;
      String url_1 = "";
      url_1 += url.getScheme() + "://";
      sVal = url.getEncodedUserInfo();
      if (sVal != null)
        url_1 += sVal + "@";
      url_1 += url.getHost();
      iVal = url.getPort();
      if (iVal > 0)
        url_1 += ":" + iVal;
      url_1 += Uri.encode(url.getPath(), "/");
      sVal = url.getEncodedQuery();
      if (sVal != null)
        url_1 += "?" + sVal;
      sVal = url.getEncodedFragment();
      if (sVal != null)
        url_1 += "#" + sVal;

      System.out.println(TAG + "encoded: " + url_1);
    }
    catch(Exception e) {
      System.out.println(TAG + "ERROR: " + e.getMessage());
    }
  }

  /* ===========================================================================
    https://developer.android.com/reference/android/net/Uri#encode(java.lang.String,%20java.lang.String)
      Uri.encode()
      always allow:
        alpha = a-zA-Z
        digit = 0-9
        other = _-!.~'()*

    https://www.rfc-editor.org/rfc/rfc3986#section-2.2
      reserved:
        gen = :/?#[]@
        sub = !$&'()*+,;=
      unreserved:
        alpha = a-zA-Z
        digit = 0-9
        other = -._~

    summary:
      reserved and not always allow:
        gen = :/?#[]@
        sub = $&+,;=
   * ===========================================================================
   */
  public static void run_test_005e(String url_0, String TAG) {
    try {
      Uri url = Uri.parse(url_0);

      int iVal;
      String sVal;
      String url_1 = "";
      sVal = url.getScheme();
      if (sVal == null)
        throw new Exception("scheme is required");
      url_1 += sVal + "://";
      sVal = url.getEncodedUserInfo();
      if (sVal != null)
        url_1 += Uri.encode(sVal, "%:") + "@";
      url_1 += url.getHost();
      iVal = url.getPort();
      if (iVal > 0)
        url_1 += ":" + iVal;
      sVal = url.getEncodedPath();
      if (sVal == null)
        throw new Exception("path is required");
      url_1 += Uri.encode(sVal, "%/");
      sVal = url.getEncodedQuery();
      if (sVal != null)
        url_1 += "?" + Uri.encode(sVal, "%=&[]");
      sVal = url.getEncodedFragment();
      if (sVal != null)
        url_1 += "#" + Uri.encode(sVal, "%/");

      System.out.println(TAG + "encoded: " + url_1);
    }
    catch(Exception e) {
      System.out.println(TAG + "ERROR: " + e.getMessage());
    }
  }

  public static String encodeComponent(String s) {
    try {
      s = URLDecoder.decode(s, "UTF-8");
      s = URLEncoder.encode(s, "UTF-8");
    }
    catch(Exception e) {}
    return s;
  }

  public static void run_tests(String url) {
    System.out.println("\n" + "----------------------------------------" + "\n");
    System.out.println("[subject] url: " + url);

    run_test_001(url, "[test_001] ");
    run_test_002(url, "[test_002] ");
    run_test_003a(url, "[test_003a] ");
    run_test_003b(url, "[test_003b] ");
    run_test_004a(url, "[test_004a] ");
    run_test_004b(url, "[test_004b] ");
    run_test_004c(url, "[test_004c] ");
    run_test_004d(url, "[test_004d] ");
    run_test_005a(url, "[test_005a] ");
    run_test_005b(url, "[test_005b] ");
    run_test_005c(url, "[test_005c] ");
    run_test_005d(url, "[test_005d] ");
    run_test_005e(url, "[test_005e] ");
  }

  public static void main(String args[]) {
    run_tests("http://a%3Ab:pass@example.com:80/foo[bar].baz?hash=%26%2f#skip");
    run_tests("http://a%3Ab:pass@example.com:80/foo%5Bbar%5D.baz?hash=%26%2f#skip");
  }
}
