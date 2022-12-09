[issue 7122](https://github.com/google/ExoPlayer/issues/7122)

- - - -

* in [SubripDecoder](https://github.com/google/ExoPlayer/blob/r2.11.3/library/core/src/main/java/com/google/android/exoplayer2/text/subrip/SubripDecoder.java)
  - correctly: the regex [`SUBRIP_TIMECODE`](https://github.com/google/ExoPlayer/blob/r2.11.3/library/core/src/main/java/com/google/android/exoplayer2/text/subrip/SubripDecoder.java#L44) makes this field optional
  - incorrectly: the function [`parseTimecode`](https://github.com/google/ExoPlayer/blob/r2.11.3/library/core/src/main/java/com/google/android/exoplayer2/text/subrip/SubripDecoder.java#L232) blindly assumes this field contains a non-null String value that can be parsed to a `Long`

__minimal code to reproduces the problem:__

```java
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Main {
  public static void main(String[] args) {
    run_test("00:00,000 --> 00:01,000");
  }

  private static final String SUBRIP_TIMECODE = "(?:(\\d+):)?(\\d+):(\\d+),(\\d+)";
  private static final Pattern SUBRIP_TIMING_LINE = Pattern.compile("\\s*(" + SUBRIP_TIMECODE + ")\\s*-->\\s*(" + SUBRIP_TIMECODE + ")\\s*");

  private static void run_test(String currentLine) {
    Matcher matcher = SUBRIP_TIMING_LINE.matcher(currentLine);
    if (matcher.matches()) {
      long start = parseTimecode(matcher, /* groupOffset= */ 1);
      long end   = parseTimecode(matcher, /* groupOffset= */ 6);

      System.out.println("start: " + start);
      System.out.println("end:   " + end);
    }
    else {
      System.out.println("no match");
    }
  }

  private static long parseTimecode(Matcher matcher, int groupOffset) {
    long timestampMs = Long.parseLong(matcher.group(groupOffset + 1)) * 60 * 60 * 1000;
    timestampMs += Long.parseLong(matcher.group(groupOffset + 2)) * 60 * 1000;
    timestampMs += Long.parseLong(matcher.group(groupOffset + 3)) * 1000;
    timestampMs += Long.parseLong(matcher.group(groupOffset + 4));
    return timestampMs * 1000;
  }
}
```

_output (stderr):_

```text
java.lang.NumberFormatException: null
  at java.base/java.lang.Long.parseLong
```

__fixed:__

```java
  private static long parseTimecode(Matcher matcher, int groupOffset) {
    long timestampMs = 0;
    String groupVal;

    // HOURS field is optional
    groupVal = matcher.group(groupOffset + 1);
    if (groupVal != null)
      timestampMs += Long.parseLong(groupVal) * 60 * 60 * 1000;

    // MINUTES field is required
    groupVal = matcher.group(groupOffset + 2);
    if (groupVal != null)
      timestampMs += Long.parseLong(groupVal) * 60 * 1000;

    // SECONDS field is required
    groupVal = matcher.group(groupOffset + 3);
    if (groupVal != null)
      timestampMs += Long.parseLong(groupVal) * 1000;

    // MILLISECONDS field is required
    groupVal = matcher.group(groupOffset + 4);
    if (groupVal != null)
      timestampMs += Long.parseLong(groupVal);

    // convert timecode from MILLISECONDS to MICROSECONDS
    return timestampMs * 1000;
  }
```

_output (stdout):_

```text
start: 0
end:   1000000
```

- - - -

[__full example:__](https://repl.it/@WarrenBank/ExoPlayer-SubripDecoder)

```java
// https://repl.it/@WarrenBank/ExoPlayer-SubripDecoder

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Main {
  public static void main(String[] args) {
    run_test("00:00:00,000 --> 00:00:01,000");
    run_test("   00:00,000 -->    00:01,000");
    run_test("   00:00     -->    00:01    ");
  }

  private static final String SUBRIP_TIMECODE = "(?:(\\d+):)?(\\d+):(\\d+)(?:,(\\d+))?";
  private static final Pattern SUBRIP_TIMING_LINE = Pattern.compile("\\s*(" + SUBRIP_TIMECODE + ")\\s*-->\\s*(" + SUBRIP_TIMECODE + ")\\s*");

  private static void run_test(String currentLine) {
    Matcher matcher = SUBRIP_TIMING_LINE.matcher(currentLine);
    if (matcher.matches()) {
      long start = parseTimecode(matcher, /* groupOffset= */ 1);
      long end   = parseTimecode(matcher, /* groupOffset= */ 6);

      System.out.println("start: " + start);
      System.out.println("end:   " + end);
    }
    else {
      System.out.println("no match");
    }
  }

  private static long parseTimecode(Matcher matcher, int groupOffset) {
    long timestampMs = 0;
    String groupVal;

    // HOURS field is optional
    groupVal = matcher.group(groupOffset + 1);
    if (groupVal != null)
      timestampMs += Long.parseLong(groupVal) * 60 * 60 * 1000;

    // MINUTES field is required
    groupVal = matcher.group(groupOffset + 2);
    if (groupVal != null)
      timestampMs += Long.parseLong(groupVal) * 60 * 1000;

    // SECONDS field is required
    groupVal = matcher.group(groupOffset + 3);
    if (groupVal != null)
      timestampMs += Long.parseLong(groupVal) * 1000;

    // MILLISECONDS field is optional
    groupVal = matcher.group(groupOffset + 4);
    if (groupVal != null)
      timestampMs += Long.parseLong(groupVal);

    // convert timecode from MILLISECONDS to MICROSECONDS
    return timestampMs * 1000;
  }
}
```

_output (stdout):_

```text
start: 0
end:   1000000
start: 0
end:   1000000
start: 0
end:   1000000
```
