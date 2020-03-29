package com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2;

import com.github.warren_bank.exoplayer_airplay_receiver.R;
import com.github.warren_bank.exoplayer_airplay_receiver.exoplayer2.customizations.TextSynchronizer;

import android.content.Context;
import android.content.DialogInterface;

import java.util.concurrent.TimeUnit;

public class MultiFieldTimePickerDialogContainer {

    private static boolean mDialogAlreadyDismissed    = false;
    private static MultiFieldTimePickerDialog mDialog = null;

    private static boolean isDialogShowing() {
        return mDialog != null && mDialog.isShowing();
    }

    private static class TextSynchronizerListener implements MultiFieldTimePickerDialog.OnMultiFieldTimeSetListener {
        private final TextSynchronizer textSynchronizer;

        TextSynchronizerListener(TextSynchronizer textSynchronizer) {
            this.textSynchronizer = textSynchronizer;
        }

        @Override
        public void onTimeSet(boolean isNegative, int hourOfDay, int minute, int second, int milli) {
            long textOffsetMs = (milli) + (second * 1000) + (minute * 60 * 1000) + (hourOfDay * 60 * 60 * 1000);
            long textOffsetUs = (textOffsetMs * 1000);

            if (isNegative)
                textOffsetUs *= -1;

            textSynchronizer.setTextOffset(textOffsetUs);
        }

        public void onTimeSet(long textOffsetUs) {
            textSynchronizer.setTextOffset(textOffsetUs);
        }
    }

    private static void showPickerDialog(
        Context mContext,
        boolean isNegative,
        int hourOfDay, int minute, int second, int millis,
        int min, int max, int step, boolean is24hourFormat, boolean isSigned, boolean isValueChangeListener,
        MultiFieldTimePickerDialog.OnMultiFieldTimeSetListener mListener,
        DialogInterface.OnDismissListener onDismissListener
    ) {
        if (isDialogShowing()) mDialog.dismiss();

        mDialog = new MultiFieldTimePickerDialog(
            mContext,
            /* theme= */ 0,
            isNegative,
            hourOfDay, minute, second, millis,
            min, max, step, is24hourFormat, isSigned, isValueChangeListener,
            mListener
        );

        mDialog.setButton(
            DialogInterface.BUTTON_POSITIVE,
            mContext.getString(android.R.string.ok),
            (DialogInterface.OnClickListener) mDialog
        );

        mDialog.setButton(
            DialogInterface.BUTTON_NEGATIVE,
            mContext.getString(R.string.time_picker_dialog_reset),
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ((TextSynchronizerListener) mListener).onTimeSet(0l);
                }
            }
        );

        mDialog.setOnDismissListener(
            new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(final DialogInterface dialog) {
                    if (!mDialogAlreadyDismissed) {
                        mDialogAlreadyDismissed = true;
                        onDismissListener.onDismiss(dialog);
                    }
                }
            }
        );

        mDialogAlreadyDismissed = false;
        mDialog.show();
    }

    public static void show(
        Context mContext,
        TextSynchronizer textSynchronizer,
        DialogInterface.OnDismissListener onDismissListener
    ) {
        MultiFieldTimePickerDialog.OnMultiFieldTimeSetListener mListener = new TextSynchronizerListener(textSynchronizer);

        long offsetPositionUs = textSynchronizer.getTextOffset();

        boolean isNegative = (offsetPositionUs < 0);

        if (isNegative)
          offsetPositionUs *= -1;

        int hourOfDay = (int)  TimeUnit.MICROSECONDS.toHours(offsetPositionUs);
        int minute    = (int) (TimeUnit.MICROSECONDS.toMinutes(offsetPositionUs) % 60);
        int second    = (int) (TimeUnit.MICROSECONDS.toSeconds(offsetPositionUs) % 60);
        int millis    = (int) (TimeUnit.MICROSECONDS.toMillis(offsetPositionUs)  % 1000);

        int min       = 0;
        int max       = 0;   // default value will be used == 24 hours
        int step      = 100; // 100 ms

        boolean is24hourFormat = true;
        boolean isSigned = true;
        boolean isValueChangeListener = true;

        showPickerDialog(
            mContext,
            isNegative,
            hourOfDay, minute, second, millis,
            min, max, step, is24hourFormat, isSigned, isValueChangeListener,
            mListener,
            onDismissListener
        );
    }
}
