package com.github.warren_bank.exoplayer_airplay_receiver.httpcore.mpc_api;

/*
 * copied from:
 *   https://github.com/eeeeeric/mpc-hc-api/blob/0.1.0/src/main/java/com/eeeeeric/mpc/hc/api/TimeCodeException.java
 */

/**
 * Exception thrown by {@link TimeCode}.
 */
public class TimeCodeException extends Exception {
  private static final long serialVersionUID = -4510604601242231041L;

  public TimeCodeException(String message) {
    super(message);
  }
}
