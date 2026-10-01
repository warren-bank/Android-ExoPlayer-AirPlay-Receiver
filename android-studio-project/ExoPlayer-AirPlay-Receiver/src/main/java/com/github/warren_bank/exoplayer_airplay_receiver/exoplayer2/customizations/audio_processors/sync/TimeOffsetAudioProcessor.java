package com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.customizations.audio_processors.sync;

/*
 * references:
 *   https://github.com/androidx/media/blob/1.11.0/libraries/common/src/main/java/androidx/media3/common/audio/BaseAudioProcessor.java
 *
 * based on duck.ai response to the prompt:
 *   In Exoplayer, in Java, write a custom AudioProcessor named TimeOffsetAudioProcessor with methods setTimeOffset(long value) and addTimeOffset(long value) which can delay or advance the timing of the audio using an internal FIFO ring buffer
 */

import androidx.media3.common.C;
import androidx.media3.common.audio.BaseAudioProcessor;

import java.nio.ByteBuffer;

/**
 * Adds or removes audio time using a FIFO ring buffer.
 *
 * Positive offset:
 *   Delays audio by inserting silence at the beginning.
 *
 * Negative offset:
 *   Advances audio by dropping samples from the beginning.
 *
 * This processor supports:
 *   - PCM 16-bit encoding
 *   - Interleaved audio
 *   - Arbitrary channel counts
 */
public final class TimeOffsetAudioProcessor extends BaseAudioProcessor {

  private static final long MICROS_PER_SECOND = 1_000_000L;

  private final Object lock = new Object();

  private long requestedOffsetUs;
  private long appliedOffsetUs;

  private int sampleRateHz;
  private int channelCount;
  private int bytesPerFrame;

  private byte[] fifo;
  private int fifoReadPosition;
  private int fifoWritePosition;
  private int fifoSize;

  private long silenceFramesRemaining;
  private long framesToDrop;

  private boolean inputEnded;

  @Override
  protected AudioFormat onConfigure(AudioFormat inputAudioFormat) throws UnhandledAudioFormatException {
    if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
      throw new UnhandledAudioFormatException(inputAudioFormat);
    }

    sampleRateHz = inputAudioFormat.sampleRate;
    channelCount = inputAudioFormat.channelCount;
    bytesPerFrame = channelCount * 2;

    /*
     * The ring buffer is allowed to hold approximately one second of audio.
     * It grows automatically when required.
     */
    fifo = new byte[Math.max(bytesPerFrame, sampleRateHz * bytesPerFrame)];
    fifoReadPosition = 0;
    fifoWritePosition = 0;
    fifoSize = 0;

    return inputAudioFormat;
  }

  /**
   * Replaces the current offset.
   *
   * @param value Offset in microseconds. Positive delays audio; negative advances audio.
   */
  public void setTimeOffset(long value) {
    synchronized (lock) {
      requestedOffsetUs = value;
    }
  }

  /**
   * Adds to the current offset.
   *
   * @param value Offset delta in microseconds. Positive delays audio; negative advances audio.
   */
  public void addTimeOffset(long value) {
    synchronized (lock) {
      requestedOffsetUs = safeAdd(requestedOffsetUs, value);
    }
  }

  /**
   * Returns the currently requested offset in microseconds.
   */
  public long getTimeOffset() {
    synchronized (lock) {
      return requestedOffsetUs;
    }
  }

  @Override
  public void queueInput(ByteBuffer inputBuffer) {
    if (!inputBuffer.hasRemaining()) {
      return;
    }

    updateOffsetState();

    int bytes = inputBuffer.remaining();
    ensureCapacity(fifoSize + bytes);

    while (inputBuffer.hasRemaining()) {
      fifo[fifoWritePosition] = inputBuffer.get();
      fifoWritePosition++;
      if (fifoWritePosition == fifo.length) {
        fifoWritePosition = 0;
      }
      fifoSize++;
    }

    produceOutput();
  }

  @Override
  protected void onQueueEndOfStream() {
    inputEnded = true;
    produceOutput();
  }

  @Override
  public boolean isEnded() {
    return inputEnded && fifoSize == 0 && silenceFramesRemaining == 0;
  }

  @Override
  protected void onFlush() {
    fifoReadPosition = 0;
    fifoWritePosition = 0;
    fifoSize = 0;

    silenceFramesRemaining = 0;
    framesToDrop = 0;
    inputEnded = false;

    appliedOffsetUs = 0;

    updateOffsetState();
  }

  @Override
  protected void onReset() {
    fifo = null;
    fifoReadPosition = 0;
    fifoWritePosition = 0;
    fifoSize = 0;

    sampleRateHz = 0;
    channelCount = 0;
    bytesPerFrame = 0;

    requestedOffsetUs = 0;
    appliedOffsetUs = 0;
    silenceFramesRemaining = 0;
    framesToDrop = 0;
    inputEnded = false;
  }

  private void updateOffsetState() {
    long requested;

    synchronized (lock) {
      requested = requestedOffsetUs;
    }

    if (requested == appliedOffsetUs) {
      return;
    }

    long oldOffset = appliedOffsetUs;
    appliedOffsetUs = requested;

    long oldDelayFrames = offsetToFrames(Math.max(0, oldOffset));
    long newDelayFrames = offsetToFrames(Math.max(0, requested));

    long oldAdvanceFrames = offsetToFrames(Math.max(0, -oldOffset));
    long newAdvanceFrames = offsetToFrames(Math.max(0, -requested));

    /*
     * Adjust delay relative to the amount already emitted.
     */
    if (newDelayFrames > oldDelayFrames) {
      silenceFramesRemaining += newDelayFrames - oldDelayFrames;
    } else if (newDelayFrames < oldDelayFrames) {
      long framesToRemove = oldDelayFrames - newDelayFrames;
      silenceFramesRemaining = Math.max(0, silenceFramesRemaining - framesToRemove);
    }

    /*
     * Adjust the amount of input that should be discarded.
     */
    if (newAdvanceFrames > oldAdvanceFrames) {
      framesToDrop += newAdvanceFrames - oldAdvanceFrames;
    } else if (newAdvanceFrames < oldAdvanceFrames) {
      framesToDrop = Math.max(0, framesToDrop - (oldAdvanceFrames - newAdvanceFrames));
    }
  }

  private void produceOutput() {
    if (fifoSize == 0 && silenceFramesRemaining == 0) {
      return;
    }

    int outputBytes = 0;

    /*
     * First discard frames for a negative offset.
     */
    if (framesToDrop > 0 && fifoSize > 0) {
      long availableFrames = fifoSize / bytesPerFrame;
      long frames = Math.min(framesToDrop, availableFrames);

      skipFrames((int) frames);

      framesToDrop -= frames;
      outputBytes = 0;
    }

    /*
     * Then write silence for a positive offset.
     */
    if (silenceFramesRemaining > 0) {
      long maxFrames = Math.min(
          silenceFramesRemaining,
          Integer.MAX_VALUE / (long) bytesPerFrame);

      int bytes = (int) (maxFrames * bytesPerFrame);
      ByteBuffer output = replaceOutputBuffer(bytes);

      for (int i = 0; i < bytes; i++) {
        output.put((byte) 0);
      }

      silenceFramesRemaining -= maxFrames;

      output.flip();
      return;
    }

    /*
     * Finally output buffered PCM data.
     */
    if (fifoSize > 0) {
      int bytes = fifoSize;

      ByteBuffer output = replaceOutputBuffer(bytes);

      while (bytes > 0) {
        int contiguousBytes = Math.min(bytes, fifo.length - fifoReadPosition);

        output.put(fifo, fifoReadPosition, contiguousBytes);

        fifoReadPosition += contiguousBytes;
        if (fifoReadPosition == fifo.length) {
          fifoReadPosition = 0;
        }

        fifoSize -= contiguousBytes;
        bytes -= contiguousBytes;
      }

      output.flip();
    }
  }

  private void skipFrames(int frameCount) {
    int bytes = frameCount * bytesPerFrame;

    fifoReadPosition += bytes;
    fifoReadPosition %= fifo.length;
    fifoSize -= bytes;
  }

  private long offsetToFrames(long offsetUs) {
    if (offsetUs <= 0 || sampleRateHz <= 0) {
      return 0;
    }

    return (offsetUs * sampleRateHz) / MICROS_PER_SECOND;
  }

  private void ensureCapacity(int requiredBytes) {
    if (fifo == null) {
      fifo = new byte[Math.max(bytesPerFrame, requiredBytes)];
      return;
    }

    if (requiredBytes <= fifo.length) {
      return;
    }

    int newCapacity = fifo.length;

    while (newCapacity < requiredBytes) {
      int nextCapacity = newCapacity * 2;

      if (nextCapacity <= newCapacity) {
        newCapacity = requiredBytes;
        break;
      }

      newCapacity = nextCapacity;
    }

    byte[] newFifo = new byte[newCapacity];

    int firstPart = Math.min(fifoSize, fifo.length - fifoReadPosition);
    System.arraycopy(fifo, fifoReadPosition, newFifo, 0, firstPart);

    int secondPart = fifoSize - firstPart;
    if (secondPart > 0) {
      System.arraycopy(fifo, 0, newFifo, firstPart, secondPart);
    }

    fifo = newFifo;
    fifoReadPosition = 0;
    fifoWritePosition = fifoSize;
  }

  private static long safeAdd(long a, long b) {
    if (b > 0 && a > Long.MAX_VALUE - b) {
      return Long.MAX_VALUE;
    }

    if (b < 0 && a < Long.MIN_VALUE - b) {
      return Long.MIN_VALUE;
    }

    return a + b;
  }
}
