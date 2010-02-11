/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.content.Context;
import android.graphics.Color;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.redgeek.android.eventrend.R;

public class ValuePickerLayout extends LinearLayout implements OnFocusChangeListener, OnKeyListener {
  private Button mAddButton;
  private EditText mValueEditText;
  private OnChangedListener mListener;

  private Context mContext;

  protected float mCurrentValue;
  protected float mPreviousValue;
 
  public interface OnChangedListener {
    void onChanged(ValuePickerLayout picker, float oldVal, float newVal);
  }
  
  public ValuePickerLayout(Context context) {
    this(context, null);
  }

  public ValuePickerLayout(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public ValuePickerLayout(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs);
    mContext = context;

    setOrientation(VERTICAL);
    LayoutInflater inflater = (LayoutInflater) mContext
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    inflater.inflate(R.layout.value_picker_layout, this, true);

    mAddButton = (Button) findViewById(R.id.ok_button);

    mValueEditText = (EditText) findViewById(R.id.value_editext);
    mValueEditText.setOnFocusChangeListener(this);
    mValueEditText.setOnKeyListener(this);
    mValueEditText.setRawInputType(InputType.TYPE_CLASS_PHONE);
    mValueEditText.requestFocus();
  }

  public void setOnChangeListener(OnChangedListener listener) {
    mListener = listener;
  }
  
  protected void notifyChange() {
    if (mListener != null) {
        mListener.onChanged(this, mPreviousValue, mCurrentValue);
    }
  }
  
  public void onFocusChange(View v, boolean hasFocus) {
    if (!hasFocus) {
      validateInput(v);
    }
  }

  @Override
  public boolean onKey(View v, int unused, KeyEvent event) {
    if (event.getAction() == KeyEvent.ACTION_UP)
      validateInput(v);
    return false;
  }

  protected void updateView() {
    mValueEditText.setText(Float.toString(mCurrentValue));
    mValueEditText.selectAll();
  }
  
  public void setValue(float value, boolean updateView) {
    mPreviousValue = mCurrentValue;
    mCurrentValue = value;
    if (updateView)
      updateView();
  }

  public float getValue() {
    return mCurrentValue;
  }
  
  private void disableAdd() {
    mAddButton.setClickable(false);
    mAddButton.setTextColor(Color.LTGRAY);
  }

  private void enableAdd() {
    mAddButton.setClickable(true);
    mAddButton.setTextColor(Color.DKGRAY);
  }

  private void validateInput(View v) {
    String str = String.valueOf(((TextView) v).getText());
    if ("".equals(str)) {
      disableAdd();
    } else {
      try {
        Float d = Float.valueOf(str);
        enableAdd();
        setValue(d, false);
        notifyChange();
      } catch (Exception e) {
        disableAdd();
      }
    }
  }
}
