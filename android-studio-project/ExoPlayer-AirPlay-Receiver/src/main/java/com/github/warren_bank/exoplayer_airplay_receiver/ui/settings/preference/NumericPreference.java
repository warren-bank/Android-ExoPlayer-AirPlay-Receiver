package com.github.warren_bank.exoplayer_airplay_receiver.ui.settings.preference;

// https://android.googlesource.com/platform/frameworks/base/+/HEAD/core/java/android/preference/EditTextPreference.java
//   public EditText getEditText()
//   public void setText(String text) { persistString(text); }
//   protected void onSetInitialValue { setText(getPersistedString(mText)); }
// https://android.googlesource.com/platform/frameworks/base/+/HEAD/core/java/android/preference/DialogPreference.java
// https://android.googlesource.com/platform/frameworks/base/+/HEAD/core/java/android/preference/Preference.java
//   ========
//   setters:
//   ========
//   protected boolean persistString(String value)
//   protected boolean persistInt(int value)
//   protected boolean persistFloat(float value)
//   ========
//   getters:
//   ========
//   protected String getPersistedString(String defaultReturnValue)
//   protected int getPersistedInt(int defaultReturnValue)
//   protected float getPersistedFloat(float defaultReturnValue)

// https://developer.android.com/reference/android/widget/TextView#getInputType()
// https://developer.android.com/reference/android/text/InputType
// https://developer.android.com/reference/android/text/InputType#TYPE_CLASS_NUMBER
// https://developer.android.com/reference/android/text/InputType#TYPE_NUMBER_VARIATION_NORMAL
// https://developer.android.com/reference/android/text/InputType#TYPE_NUMBER_VARIATION_PASSWORD
// https://developer.android.com/reference/android/text/InputType#TYPE_NUMBER_FLAG_SIGNED
// https://developer.android.com/reference/android/text/InputType#TYPE_NUMBER_FLAG_DECIMAL

import android.content.Context;
import android.preference.EditTextPreference;
import android.text.InputType;
import android.util.AttributeSet;

public class NumericPreference extends EditTextPreference {

  public NumericPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }
  public NumericPreference(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }
  public NumericPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
  }
  public NumericPreference(Context context) {
    super(context);
  }

  @Override
  protected boolean persistString(String value) {
    if (isNumeric()) {
      if (value.isEmpty()) value = "0";
      if (value.charAt(0) == '+') value = value.substring(1);

      if (isFloat()) {
        float fVal = 0.0f;
        try {
          fVal = Float.parseFloat(value);
        }
        catch(Exception e) {
        }
        return persistFloat(fVal);
      }
      else {
        int iVal = 0;
        try {
          iVal = Integer.parseInt(value, 10);
        }
        catch(Exception e) {
        }
        return persistInt(iVal);
      }
    }
    else {
      return super.persistString(value);
    }
  }

  @Override
  protected String getPersistedString(String defaultReturnValue) {
    if (isNumeric()) {
      String sVal = defaultReturnValue;

      if (isFloat()) {
        float fVal = getPersistedFloat(Float.MIN_VALUE);
        if (Float.compare(fVal, Float.MIN_VALUE) != 0) {
          sVal = Float.toString(fVal);
        }
      }
      else {
        int iVal = getPersistedInt(Integer.MIN_VALUE);
        if (Integer.compare(iVal, Integer.MIN_VALUE) != 0) {
          sVal = Integer.toString(iVal);
        }
      }

      return sVal;
    }
    else {
      return super.getPersistedString(defaultReturnValue);
    }
  }

  private boolean isNumeric() {
    return isNumeric(
      getInputType()
    );
  }

  private boolean isNumeric(int bits) {
    return
         hasBit(bits, InputType.TYPE_CLASS_NUMBER)
      || hasBit(bits, InputType.TYPE_NUMBER_VARIATION_NORMAL)
      || hasBit(bits, InputType.TYPE_NUMBER_VARIATION_PASSWORD)
      || hasBit(bits, InputType.TYPE_NUMBER_FLAG_SIGNED)
      || hasBit(bits, InputType.TYPE_NUMBER_FLAG_DECIMAL)
    ;
  }

  private boolean isFloat() {
    return isFloat(
      getInputType()
    );
  }

  private boolean isFloat(int bits) {
    return hasBit(bits, InputType.TYPE_NUMBER_FLAG_DECIMAL);
  }

  private int getInputType() {
    return getEditText().getInputType();
  }

  private boolean hasBit(int bits, int bit) {
    return ((bits & bit) == bit);
  }

}
