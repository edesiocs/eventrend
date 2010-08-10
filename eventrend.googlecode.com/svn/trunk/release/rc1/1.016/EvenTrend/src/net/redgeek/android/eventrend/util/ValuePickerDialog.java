/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.redgeek.android.eventrend.util;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TimePicker;

import net.redgeek.android.eventrend.R;
import net.redgeek.android.eventrend.util.ValuePickerFrame.OnValueChangedListener;
import net.redgeek.android.eventrend.util.ValuePickerFrame.OnValueSetListener;
import net.redgeek.android.eventrend.util.ValuePickerFrame.OnValueCancelListener;

/**
 * A dialog that prompts the user for the time of day using a {@link TimePicker}
 * .
 */
// public class ValuePickerDialog extends Dialog implements OnClickListener,
public class ValuePickerDialog extends Dialog implements
    OnValueChangedListener, OnValueSetListener, OnValueCancelListener {

  private static final String CATEGORY = "category";
  private static final String VALUE = "value";

  public interface OnValueSetListener {
    void onValueSet(ValuePickerFrame view, float value);
  }

  private final ValuePickerFrame mValuePickerFrame;

  private OnValueSetListener mCallback;
  float mInitialValue = 0;
  String mCategory = "";

  public ValuePickerDialog(Context context, OnValueSetListener callBack,
      String category, float value) {
    this(context, android.R.style.Theme_Dialog, callBack, category, value);
  }

  public ValuePickerDialog(Context context, int theme,
      OnValueSetListener callBack, String category, float value) {
    super(context, theme);
    mCategory = category;
    mCallback = callBack;
    mInitialValue = value;

    updateTitle(category);

    LayoutInflater inflater = (LayoutInflater) context
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    View view = inflater.inflate(R.layout.value_picker_dialog, null);
    setContentView(view);
    mValuePickerFrame = (ValuePickerFrame) view
        .findViewById(R.id.value_picker_frame);

    // initialize state
    mValuePickerFrame.setCurrentValue(mInitialValue);
    mValuePickerFrame.setOnValueChangedListener(this);
    mValuePickerFrame.setOnValueSetListener(this);
    mValuePickerFrame.setOnValueCancelListener(this);
  }

  // public void onClick(DialogInterface dialog, int which) {
  // if (mCallback != null) {
  // mValuePickerFrame.clearFocus();
  // mCallback.onValueSet(mValuePickerFrame,
  // mValuePickerFrame.getCurrentValue());
  // }
  // }

  public void onValueChanged(ValuePickerFrame view, float value) {
  }

  public void onValueSet(ValuePickerFrame view, float value) {
    if (mCallback != null) {
      mValuePickerFrame.clearFocus();
      mCallback.onValueSet(mValuePickerFrame, mValuePickerFrame
          .getCurrentValue());
    }
    dismiss();
  }

  public void onValueCancel(ValuePickerFrame view, float value) {
    dismiss();
  }

  public void updateValue(float value) {
    mValuePickerFrame.setCurrentValue(value);
  }

  public void updateTitle(String category) {
    setTitle(category);
  }
  
  public void setOnValueSetListener(ValuePickerDialog.OnValueSetListener listener) {
    mCallback = listener;
  }
  
  @Override
  public Bundle onSaveInstanceState() {
    Bundle state = super.onSaveInstanceState();
    state.putFloat(VALUE, mValuePickerFrame.getCurrentValue());
    state.putString(CATEGORY, mCategory);
    return state;
  }

  @Override
  public void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    float value = savedInstanceState.getFloat(VALUE);
    mValuePickerFrame.setCurrentValue(value);
    mCategory = savedInstanceState.getString(CATEGORY);
    updateTitle(mCategory);
  }
}