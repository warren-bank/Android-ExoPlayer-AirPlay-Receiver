package com.github.warren_bank.exoplayer_airplay_receiver.httpcore.mpc_api;

/*
 * copied from:
 *   https://github.com/eeeeeric/mpc-hc-api/blob/0.1.0/src/main/java/com/eeeeeric/mpc/hc/api/TimeCode.java
 */

/**
 * An object representation of the HH:MM:SS time code.
 */
public class TimeCode implements Comparable<TimeCode> {
  /**
   * The {@link TimeCodeException} should never be thrown.
   */
  public static final TimeCode START;
  static {
    TimeCode temp = null;
    try {
      temp = new TimeCode(0);
    }
    catch (TimeCodeException e) {}
    START = temp;
  }
  private int hours;
  private int minutes;
  private int seconds;
  private int totalSeconds;

  /**
   * Create a new TimeCode object.
   *
   * @param hours
   *        The number of hours
   * @param minutes
   *        The number of minutes
   * @param seconds
   *        The number of seconds
   *
   * @throws TimeCodeException
   *         If minutes or seconds is larger than or equal to 60
   */
  public TimeCode(int hours, int minutes, int seconds) throws TimeCodeException {
    init(hours, minutes, seconds);
  }

  /**
   * Create a new TimeCode object with the given total number of seconds.
   *
   * @param seconds
   *        The total number of seconds
   *
   * @throws TimeCodeException
   *         This should never happen
   */
  public TimeCode(int seconds) throws TimeCodeException {
    int hours = seconds / 3600;
    seconds = seconds % 3600;
    int minutes = seconds / 60;
    seconds = seconds % 60;

    init(hours, minutes, seconds);
  }

  /**
   * Create a new TimeCode object from a string representation (HH:MM:SS).
   *
   * @param timeCode
   *        The string representation of a time code
   *
   * @throws TimeCodeException
   *         If the string is not a properly formatted time code
   */
  public TimeCode(String timeCode) throws TimeCodeException {
    String times[] = timeCode.split(":");
    if (times.length != 3) {
      throw new TimeCodeException("Invalid time code string.");
    }

    hours = Integer.valueOf(times[0]);
    minutes = Integer.valueOf(times[1]);
    seconds = Integer.valueOf(times[2]);
    init(hours, minutes, seconds);
  }

  /**
   * Initialize this TimeCode object with the given values.
   *
   * @param hours
   *        The number of hours
   * @param minutes
   *        The number of minutes
   * @param seconds
   *        The number of seconds
   *
   * @throws TimeCodeException
   *         If minutes or seconds is larger than or equal to 60
   *         If hours, minutes, or seconds is negative
   */
  private void init(int hours, int minutes, int seconds) throws TimeCodeException {
    if (minutes >= 60 || seconds >= 60) {
      throw new TimeCodeException("Minutes (" + minutes + ") or seconds (" + seconds + ") exceeds 60.");
    }
    else if (hours < 0 || minutes < 0 || seconds < 0) {
      throw new TimeCodeException("Hours (" + hours + "), minutes (" + minutes + "), and seconds (" + seconds + ") cannot be negative.");
    }

    this.hours = hours;
    this.minutes = minutes;
    this.seconds = seconds;
    this.totalSeconds = (hours * 3600) + (minutes * 60) + seconds;
  }

  /**
   * Get the number of hours
   *
   * @return The number of hours
   */
  public int getHours() {
    return hours;
  }

  /**
   * Get the number of minutes
   *
   * @return The number of minutes
   */
  public int getMinutes() {
    return minutes;
  }

  /**
   * Get the number of seconds
   *
   * @return The number of seconds
   */
  public int getSeconds() {
    return seconds;
  }

  /**
   * Get the total number of seconds
   *
   * @return The total number of seconds
   */
  public int getTotalSeconds() {
    return totalSeconds;
  }

  /**
   * Add two time codes together
   *
   * @param augend
   *        The first time code
   * @param addend
   *        The second time code
   *
   * @return The sum of the two time codes
   *
   * @throws TimeCodeException
   *         This should never happen
   */
  public static TimeCode plus(TimeCode augend, TimeCode addend) throws TimeCodeException {
    return new TimeCode(augend.totalSeconds + addend.totalSeconds);
  }

  /**
   * Subtract one time code from another.
   *
   * @param minuend
   *        The minuend
   * @param subtrahend
   *        The subtrahend
   *
   * @return The difference of the two time codes
   *
   * @throws TimeCodeException
   *         If the subtrahend is larger than the minuend
   */
  public static TimeCode minus(TimeCode minuend, TimeCode subtrahend) throws TimeCodeException {
    if (minuend.totalSeconds >= subtrahend.totalSeconds) {
      return new TimeCode(minuend.totalSeconds - subtrahend.totalSeconds);
    }
    else {
      throw new TimeCodeException(minuend + "is smaller than " + subtrahend);
    }
  }

  /** {@inheritDoc} */
  @Override
  public int compareTo(TimeCode o) {
    return Integer.compare(totalSeconds, o.totalSeconds);
  }

  /** {@inheritDoc} */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TimeCode timeCode = (TimeCode) o;
    return totalSeconds == timeCode.totalSeconds;
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    return totalSeconds;
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return Integer.toString(hours)   + ":" +
           Integer.toString(minutes) + ":" +
           Integer.toString(seconds);
  }
}
