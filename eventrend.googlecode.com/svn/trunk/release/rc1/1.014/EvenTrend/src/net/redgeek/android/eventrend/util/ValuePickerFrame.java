package net.redgeek.android.eventrend.util;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import net.redgeek.android.eventrend.R;
import net.redgeek.android.eventrend.util.ValuePickerLayout;

public class ValuePickerFrame extends FrameLayout {
  private static final OnValueChangedListener NO_OP_CHANGE_LISTENER = new OnValueChangedListener() {
    public void onValueChanged(ValuePickerFrame view, float value) {
    }
  };

  private static final OnValueSetListener NO_OP_SET_LISTENER = new OnValueSetListener() {
    public void onValueSet(ValuePickerFrame view, float value) {
    }
  };

  private static final OnValueCancelListener NO_OP_CANCEL_LISTENER = new OnValueCancelListener() {
    public void onValueCancel(ValuePickerFrame view, float value) {
    }
  };

  // state
  private Context mContext;
  private float mCurrentValue = 0;

  // ui components
  private final ValuePickerLayout mValuePickerLayout;
  private final Button mCancelButton;
  private final Button mOkButton;

  private OnValueChangedListener mOnValueChangedListener;
  private OnValueSetListener mOnValueSetListener;
  private OnValueCancelListener mOnValueCancelListener;

  public interface OnValueChangedListener {
    void onValueChanged(ValuePickerFrame view, float value);
  }

  public interface OnValueSetListener {
    void onValueSet(ValuePickerFrame view, float value);
  }

  public interface OnValueCancelListener {
    void onValueCancel(ValuePickerFrame view, float value);
  }

  public ValuePickerFrame(Context context) {
    this(context, null);
  }

  public ValuePickerFrame(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public ValuePickerFrame(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    mContext = context;

    setOnValueChangedListener(NO_OP_CHANGE_LISTENER);
    setOnValueSetListener(NO_OP_SET_LISTENER);
    setOnValueCancelListener(NO_OP_CANCEL_LISTENER);

    LayoutInflater inflater = (LayoutInflater) context
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    mValuePickerLayout = new ValuePickerLayout(context, attrs, defStyle);
    // mValuePickerLayout = (ValuePickerLayout) inflater.inflate(
    // R.layout.value_picker_layout, this, true);
    addView(mValuePickerLayout);
    mValuePickerLayout
        .setOnChangeListener(new ValuePickerLayout.OnChangedListener() {
          public void onChanged(ValuePickerLayout picker, float oldVal,
              float newVal) {
            mCurrentValue = newVal;
            onValueChanged();
          }
        });

    mCancelButton = (Button) mValuePickerLayout
        .findViewById(R.id.cancel_button);
    mCancelButton.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        onValueCancel();
      }
    });
    mOkButton = (Button) mValuePickerLayout.findViewById(R.id.ok_button);
    mOkButton.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        onValueSet();
      }
    });
  }

//  private static class SavedState extends BaseSavedState {
//    private final double mValue;
//
//    private SavedState(Parcelable superState, double value) {
//      super(superState);
//      mValue = value;
//    }
//
//    private SavedState(Parcel in) {
//      super(in);
//      mValue = in.readDouble();
//    }
//
//    public double getCurrentValue() {
//      return mValue;
//    }
//
//    @Override
//    public void writeToParcel(Parcel dest, int flags) {
//      super.writeToParcel(dest, flags);
//      dest.writeDouble(mValue);
//    }
//
//    public static final Parcelable.Creator<SavedState> CREATOR = new Creator<SavedState>() {
//      public SavedState createFromParcel(Parcel in) {
//        return new SavedState(in);
//      }
//
//      public SavedState[] newArray(int size) {
//        return new SavedState[size];
//      }
//    };
//  }
//
//  @Override
//  protected Parcelable onSaveInstanceState() {
//    Parcelable superState = super.onSaveInstanceState();
//    return new SavedState(superState, mCurrentValue);
//  }
//
//  @Override
//  protected void onRestoreInstanceState(Parcelable state) {
//    SavedState ss = (SavedState) state;
//    super.onRestoreInstanceState(ss.getSuperState());
//    setCurrentValue(ss.getCurrentValue());
//  }

  public void setOnValueChangedListener(
      OnValueChangedListener onValueChangedListener) {
    mOnValueChangedListener = onValueChangedListener;
  }

  public void setOnValueSetListener(
      OnValueSetListener onValueSetListener) {
    mOnValueSetListener = onValueSetListener;
  }

  public void setOnValueCancelListener(
      OnValueCancelListener onValueCancelListener) {
    mOnValueCancelListener = onValueCancelListener;
  }

  public Float getCurrentValue() {
    return mCurrentValue;
  }

  public void setCurrentValue(Float currentValue) {
    mCurrentValue = currentValue;
    updateValueDisplay();
  }

  private void updateValueDisplay() {
    float currentValue = mCurrentValue;
    mValuePickerLayout.setValue(currentValue, true);
    onValueChanged();
  }

  private void onValueChanged() {
    mOnValueChangedListener.onValueChanged(this, getCurrentValue());
  }
  
  private void onValueSet() {
    mOnValueSetListener.onValueSet(this, getCurrentValue());
  }

  private void onValueCancel() {
    mOnValueCancelListener.onValueCancel(this, getCurrentValue());
  }

}
