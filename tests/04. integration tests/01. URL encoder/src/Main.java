import com.github.warren_bank.exoplayer_airplay_receiver.utils.UriUtils;

public class Main {
  public static void run_test_001(String url_0, String TAG) {
    try {
      String url_1 = UriUtils.encodeURI(url_0);

      System.out.println(TAG + "encoded: " + url_1);
    }
    catch(Exception e) {
      System.out.println(TAG + "ERROR: " + e.getMessage());
    }
  }

  public static void run_tests(String url) {
    System.out.println("\n" + "----------------------------------------" + "\n");
    System.out.println("[subject] url: " + url);

    run_test_001(url, "[test_001] ");
  }

  public static void main(String args[]) {
    run_tests("http://a%3Ab:pass@example.com:80/foo[bar].baz?hash=%26%2f#skip");
    run_tests("http://a%3Ab:pass@example.com:80/foo%5Bbar%5D.baz?hash=%26%2f#skip");
  }
}
