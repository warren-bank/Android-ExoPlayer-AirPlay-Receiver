package com.github.warren_bank.exoplayer_airplay_receiver.ui.exoplayer2;

import com.github.warren_bank.exoplayer_airplay_receiver.R;
import com.github.warren_bank.exoplayer_airplay_receiver.ui.exoplayer2.customizations.MyRenderersFactory;

import android.content.Context;
import android.content.DialogInterface;

import java.util.concurrent.TimeUnit;

public class MultiFieldTimePickerDialogContainer {

    private static boolean mDialogAlreadyDismissed    = false;
    private static MultiFieldTimePickerDialog mDialog = null;

    private static boolean isDialogShowing() {
        return mDialog != null && mDialog.isShowing();
    }

    private static class MyRenderersFactoryListener implements MultiFieldTimePickerDialog.OnMultiFieldTimeSetListener {
        private final MyRenderersFactory renderersFactory;

        MyRenderersFactoryListener(MyRenderersFactory renderersFactory) {
            this.renderersFactory = renderersFactory;
        }

        @Override
        public void onTimeSet(int hourOfDay, int minute, int second, int milli) {
            long textOffsetMs = (milli) + (second * 1000) + (minute * 60 * 1000) + (hourOfDay * 60 * 60 * 1000);
            long textOffsetUs = (textOffsetMs * 1000);

            renderersFactory.setTextOffset(textOffsetUs);
        }

        public void onTimeSet(long textOffsetUs) {
            renderersFactory.setTextOffset(textOffsetUs);
        }
    }

    private static void showPickerDialog(
        Context mContext,
        int hourOfDay, int minute, int second, int millis,
        int min, int max, int step, boolean is24hourFormat,
        MultiFieldTimePickerDialog.OnMultiFieldTimeSetListener mListener,
        DialogInterface.OnDismissListener onDismissListener
    ) {
        if (isDialogShowing()) mDialog.dismiss();

        mDialog = new MultiFieldTimePickerDialog(
            mContext,
            /* theme= */ 0,
            hourOfDay, minute, second, millis,
            min, max, step, is24hourFormat,
            mListener
        );

        mDialog.setButton(
            DialogInterface.BUTTON_POSITIVE,
            mContext.getText(android.R.string.ok),
            (DialogInterface.OnClickListener) mDialog
        );

        mDialog.setButton(
            DialogInterface.BUTTON_NEGATIVE,
            mContext.getText(R.string.time_picker_dialog_reset),
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ((MyRenderersFactoryListener) mListener).onTimeSet(0l);
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
        MyRenderersFactory renderersFactory,
        DialogInterface.OnDismissListener onDismissListener
    ) {
        MultiFieldTimePickerDialog.OnMultiFieldTimeSetListener mListener = new MyRenderersFactoryListener(renderersFactory);

        long offsetPositionUs = renderersFactory.getOffsetPositionUs();

        int hourOfDay = (int)  TimeUnit.MICROSECONDS.toHours(offsetPositionUs);
        int minute    = (int) (TimeUnit.MICROSECONDS.toMinutes(offsetPositionUs) % 60);
        int second    = (int) (TimeUnit.MICROSECONDS.toSeconds(offsetPositionUs) % 60);
        int millis    = (int) (TimeUnit.MICROSECONDS.toMillis(offsetPositionUs)  % 1000);

        int min       = 0;
        int max       = 0;   // default value will be used == 24 hours
        int step      = 100; // 100 ms

        boolean is24hourFormat = true;

        showPickerDialog(
            mContext,
            hourOfDay, minute, second, millis,
            min, max, step, is24hourFormat,
            mListener,
            onDismissListener
        );
    }
}
