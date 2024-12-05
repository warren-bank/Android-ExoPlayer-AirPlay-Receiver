package com.github.warren_bank.exoplayer_airplay_receiver.httpcore.mpc_api;

/*
 * based on:
 *   https://github.com/mpc-hc/mpc-hc/blob/1.7.13/src/mpc-hc/WebClientSocket.cpp#L369-L417
 *
 * additional references:
 *   https://github.com/eeeeeric/mpc-hc-api
 *     client: Java
 *   https://github.com/rzcoder/mpc-hc-control
 *     client: Javascript
 *   https://github.com/burdukowsky/mpc-hc-android
 *     client: Kotlin for Android
 */

import com.github.warren_bank.exoplayer_airplay_receiver.constant.Constant;
import com.github.warren_bank.exoplayer_airplay_receiver.httpcore.mpc_api.TimeCode;
import com.github.warren_bank.exoplayer_airplay_receiver.httpcore.mpc_api.WMCommand;
import com.github.warren_bank.exoplayer_airplay_receiver.httpcore.RequestListenerThread.PlaybackInfoSource;
import com.github.warren_bank.exoplayer_airplay_receiver.utils.StringUtils;

import org.apache.http.HttpStatus;

import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

public class OnCommand {
  private static final String tag = OnCommand.class.getSimpleName();

  public static final class Result {
    public Message msg;
    public boolean stop;
    public int statusCode;

    public Result() {}
  }

  public static Result handle(String target, byte[] entityContent, PlaybackInfoSource playbackInfoSource) {
    Result result = new Result();

    String command_id = StringUtils.getQueryStringValue(target, entityContent, "?wm_command=");
    if (!TextUtils.isEmpty(command_id)) {
      try {
        WMCommand wmCommand = WMCommand.findValue(command_id);
        if (wmCommand == null)
          throw new Exception("WPC-API: unrecognized command id");

        switch(wmCommand) {
          case SET_VOLUME : {
              String volume = StringUtils.getQueryStringValue(target, entityContent, "&volume=");
              if (TextUtils.isEmpty(volume))
                throw new Exception("WPC-API: missing required parameter");

              int volumeInt = Integer.parseInt(volume, 10);

              if ((volumeInt < 0) || (volumeInt > 100))
                throw new Exception("WPC-API: invalid parameter value");

              float audioVolume = volumeInt / 100.0f;
              Log.d(tag, "WPC-API: volume = " + audioVolume);

              Message msg = Message.obtain();
              msg.what = Constant.Msg.Msg_Audio_Volume;
              msg.obj = audioVolume;
              result.msg = msg;
            }
            break;
          case SEEK : {
              long positionSec = -1L;

              if (positionSec < 0L) {
                String position = StringUtils.getQueryStringValue(target, entityContent, "&position=");
                if (!TextUtils.isEmpty(position)) {
                  // convert "hh:mm:ss" to (long) positionSec
                  positionSec = (long) new TimeCode(position).getTotalSeconds();
                  Log.d(tag, "WPC-API: seek position = " + positionSec + " seconds");
                }
              }

              if (positionSec < 0L) {
                String percent = StringUtils.getQueryStringValue(target, entityContent, "&percent=");
                if (!TextUtils.isEmpty(percent)) {
                  int percentInt = Integer.parseInt(percent, 10);
                  if ((percentInt >= 0) && (percentInt <= 100)) {
                    if ((playbackInfoSource != null) && playbackInfoSource.refresh() && !playbackInfoSource.isPlaybackFinished()) {
                      long durationMs  = playbackInfoSource.getDuration();
                      long durationSec = durationMs / 1000;
                      positionSec   = (long) (durationSec * (percentInt / 100.0f));
                      Log.d(tag, "WPC-API: seek position = " + percent + "%, at " + positionSec + " seconds");

                      playbackInfoSource.release();
                    }
                  }
                }
              }

              if (positionSec < 0L) {
                throw new Exception("WPC-API: missing required parameter");
              }

              Message msg = Message.obtain();
              msg.what = Constant.Msg.Msg_Video_Seek;
              msg.obj = (float) positionSec;
              result.msg = msg;
            }
            break;
          case CLOSE : {
              Message msg = Message.obtain();
              msg.what = Constant.Msg.Msg_Hide_Player;
              result.msg = msg;
            }
            break;
          case EXIT : {
              Message msg = Message.obtain();
              msg.what = Constant.Msg.Msg_Exit_Service;
              result.msg = msg;
            }
            break;
          case PLAY_PAUSE : {
              Message msg = Message.obtain();
              msg.what = Constant.Msg.Msg_Video_Pause;
              msg.obj = null;
              result.msg = msg;
            }
            break;
          case PLAY : {
              Message msg = Message.obtain();
              msg.what = Constant.Msg.Msg_Video_Pause;
              msg.obj = false;
              result.msg = msg;
            }
            break;
          case PAUSE : {
              Message msg = Message.obtain();
              msg.what = Constant.Msg.Msg_Video_Pause;
              msg.obj = true;
              result.msg = msg;
            }
            break;
          case STOP : {
              Message msg = Message.obtain();
              msg.what = Constant.Msg.Msg_Stop;
              result.msg = msg;

              result.stop = true;
            }
            break;
          case INCREASE_RATE : {
              float offset = 0.25f;

              Message msg = Message.obtain();
              msg.what = Constant.Msg.Msg_Video_Rate_Offset;
              msg.obj = offset;
              result.msg = msg;
            }
            break;
          case DECREASE_RATE : {
              float offset = -0.25f;

              Message msg = Message.obtain();
              msg.what = Constant.Msg.Msg_Video_Rate_Offset;
              msg.obj = offset;
              result.msg = msg;
            }
            break;
          case RESET_RATE : {
              float rate = 1.0f;

              Message msg = Message.obtain();
              msg.what = Constant.Msg.Msg_Video_Rate;
              msg.obj = rate;
              result.msg = msg;
            }
            break;
          case JUMP_FORWARD_SMALL : {
              long offset = 5000L; // 5,000 milliseconds = 5 seconds

              Message msg = Message.obtain();
              msg.what = Constant.Msg.Msg_Video_Seek_Offset;
              msg.obj = offset;
              result.msg = msg;
            }
            break;
          case JUMP_BACKWARD_SMALL : {
              long offset = -5000L; // -5,000 milliseconds = -5 seconds

              Message msg = Message.obtain();
              msg.what = Constant.Msg.Msg_Video_Seek_Offset;
              msg.obj = offset;
              result.msg = msg;
            }
            break;
          case JUMP_FORWARD_MEDIUM : {
              long offset = 30000L; // 30,000 milliseconds = 30 seconds

              Message msg = Message.obtain();
              msg.what = Constant.Msg.Msg_Video_Seek_Offset;
              msg.obj = offset;
              result.msg = msg;
            }
            break;
          case JUMP_BACKWARD_MEDIUM : {
              long offset = -30000L; // -30,000 milliseconds = -30 seconds

              Message msg = Message.obtain();
              msg.what = Constant.Msg.Msg_Video_Seek_Offset;
              msg.obj = offset;
              result.msg = msg;
            }
            break;
          case JUMP_FORWARD_LARGE : {
              long offset = 180000L; // 180,000 milliseconds = 3 minutes

              Message msg = Message.obtain();
              msg.what = Constant.Msg.Msg_Video_Seek_Offset;
              msg.obj = offset;
              result.msg = msg;
            }
            break;
          case JUMP_BACKWARD_LARGE : {
              long offset = -180000L; // -180,000 milliseconds = -3 minutes

              Message msg = Message.obtain();
              msg.what = Constant.Msg.Msg_Video_Seek_Offset;
              msg.obj = offset;
              result.msg = msg;
            }
            break;
          case JUMP_TO_BEGINNING : {
              float pos = 0.0f;

              Message msg = Message.obtain();
              msg.what = Constant.Msg.Msg_Video_Seek;
              msg.obj = pos;
              result.msg = msg;
            }
            break;
          case NEXT :
          case NEXT_AUDIO :
          case NEXT_AUDIO_OGM :
          case NEXT_AUDIO_DVD : {
              Message msg = Message.obtain();
              msg.what = Constant.Msg.Msg_Video_Next;
              result.msg = msg;
            }
            break;
          case PREVIOUS :
          case PREV_AUDIO :
          case PREV_AUDIO_OGM :
          case PREV_AUDIO_DVD : {
              Message msg = Message.obtain();
              msg.what = Constant.Msg.Msg_Video_Prev;
              result.msg = msg;
            }
            break;
          case VIEW_MINIMAL :
          case VIEW_COMPACT : {
              Message msg = Message.obtain();
              msg.what = Constant.Msg.Msg_Show_Player;
              msg.obj = Boolean.TRUE; //enterPipMode
              result.msg = msg;
            }
            break;
          case VIEW_NORMAL :
          case FULLSCREEN :
          case FULLSCREEN_WITHOUT_RES_CHANGE : {
              Message msg = Message.obtain();
              msg.what = Constant.Msg.Msg_Show_Player;
              msg.obj = Boolean.FALSE; //enterPipMode
              result.msg = msg;
            }
            break;
          case VOLUME_UP : {
              float offset = 0.1f;

              Message msg = Message.obtain();
              msg.what = Constant.Msg.Msg_Audio_Volume_Offset;
              msg.obj = offset;
              result.msg = msg;
            }
            break;
          case VOLUME_DOWN : {
              float offset = -0.1f;

              Message msg = Message.obtain();
              msg.what = Constant.Msg.Msg_Audio_Volume_Offset;
              msg.obj = offset;
              result.msg = msg;
            }
            break;
          case VOLUME_MUTE : {
              Message msg = Message.obtain();
              msg.what = Constant.Msg.Msg_Audio_Volume_Mute;
              msg.obj = null;
              result.msg = msg;
            }
            break;
          case VOLUME_BOOST_INCREASE : {
              float offset = 1.0f;

              Message msg = Message.obtain();
              msg.what = Constant.Msg.Msg_Audio_Volume_Offset;
              msg.obj = offset;
              result.msg = msg;
            }
            break;
          case VOLUME_BOOST_DECREASE : {
              float offset = -1.0f;

              Message msg = Message.obtain();
              msg.what = Constant.Msg.Msg_Audio_Volume_Offset;
              msg.obj = offset;
              result.msg = msg;
            }
            break;
          case VOLUME_BOOST_MIN : {
              float volume = 1.0f;

              Message msg = Message.obtain();
              msg.what = Constant.Msg.Msg_Audio_Volume;
              msg.obj = volume;
              result.msg = msg;
            }
            break;
          case VOLUME_BOOST_MAX : {
              float volume = Float.MAX_VALUE; // when processed, will be reduced to: PreferencesMgr.get_max_audio_volume_boost_db() + 1.0f

              Message msg = Message.obtain();
              msg.what = Constant.Msg.Msg_Audio_Volume;
              msg.obj = volume;
              result.msg = msg;
            }
            break;
          case ON_OFF_SUBTITLE :
          case ON_OFF_SUBTITLE_DVD : {
              Message msg = Message.obtain();
              msg.what = Constant.Msg.Msg_Text_Show;
              msg.obj = null;
              result.msg = msg;
            }
            break;
          case SHIFT_SUBTITLE_LEFT :
          case SUBTITLE_DELAY_MINUS : {
              long offsetUs = -1000000L; // -1 second in microseconds

              Message msg = Message.obtain();
              msg.what = Constant.Msg.Msg_Text_Add_Time;
              msg.obj = offsetUs;
              result.msg = msg;
            }
            break;
          case SHIFT_SUBTITLE_RIGHT :
          case SUBTITLE_DELAY_PLUS : {
              long offsetUs = 1000000L; // +1 second in microseconds

              Message msg = Message.obtain();
              msg.what = Constant.Msg.Msg_Text_Add_Time;
              msg.obj = offsetUs;
              result.msg = msg;
            }
            break;
        }
      }
      catch(Exception e) {
        result.statusCode = HttpStatus.SC_BAD_REQUEST;
        return result;
      }
    }

    result.statusCode = HttpStatus.SC_OK;
    return result;
  }

}
