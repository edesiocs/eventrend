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

package net.redgeek.android.eventrend.datum;

import java.util.Calendar;

import net.redgeek.android.eventrend.EvenTrendActivity;
import net.redgeek.android.eventrend.Preferences;
import net.redgeek.android.eventrend.R;
import net.redgeek.android.eventrend.db.CategoryDbTable;
import net.redgeek.android.eventrend.db.EntryDbTable;
import net.redgeek.android.eventrend.primitives.TimeSeriesCollector;
import net.redgeek.android.eventrend.util.DateUtil;
import net.redgeek.android.eventrend.util.DynamicSpinner;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.TableRow.LayoutParams;

public class EntryEditActivity extends EvenTrendActivity {
  // Dialog IDs
  static final int TIME_DIALOG_ID = 0;
  static final int DATE_DIALOG_ID = 1;

  // UI elements
  private DynamicSpinner mCategoryMenu;
  private LinearLayout mCategoryMenuRow;
  private TextView mCategoryChanged;
  private EditText mValueView;
  private TextView mValueChanged;
  private TextView mTimestampView;
  private TextView mTimestampChangedView;
  private Button mOk;
  private Button mDelete;
  private TextView mAggregationView;
  private Button mPickDate;
  private Button mPickTime;
  private Button mPickNow;

  // Listeners
  private OnItemSelectedListener mCategoryMenuListener;
  private View.OnClickListener mPickDateListener;
  private View.OnClickListener mPickTimeListener;
  private View.OnClickListener mPickNowListener;
  private View.OnKeyListener mValueViewListener;
  private View.OnClickListener mOkListener;
  private View.OnClickListener mDeleteListener;
  private DatePickerDialog.OnDateSetListener mDateSetListener;
  private TimePickerDialog.OnTimeSetListener mTimeSetListener;

  // Private data
  private EntryDbTable.Row mEntry;
  private Long mRowId;
  private long mCategoryId = 0;
  private float mValue = (float) 0.0;
  private boolean mTimestampChanged = false;
  private boolean mSave = false;
  private int mAggregation;

  // Prefs
  private int mHistory;
  private float mSmoothing;
  private float mSensitivity;

  // original values
  private long mOriginalCategoryId;
  private float mOriginalValue;
  private DateUtil.DateItem mOriginalTimestamp;

  // picker values
  private DateUtil.DateItem mPickerTimestamp;
  private Calendar mCal;

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    setContentView(R.layout.entry_edit);

    setupData(icicle);
    setupUI();
    populateFields(mEntry);
    saveOriginalValues();

    setTimestamp();

  }

  private void setupData(Bundle icicle) {
    if (icicle != null) {
      mRowId = icicle.getLong(EntryDbTable.KEY_ROWID);
      if (mRowId < 0)
        mRowId = null;
    }
    if (mRowId == null || mRowId < 0) {
      Bundle extras = getIntent().getExtras();
      if (extras != null) {
        mRowId = extras.getLong(EntryDbTable.KEY_ROWID);
      }
    }

    if (mRowId != null) {
      mEntry = getDbh().fetchEntry(mRowId);
      mCategoryId = mEntry.getCategoryId();
    }

    mCal = Calendar.getInstance();
    mOriginalTimestamp = new DateUtil.DateItem();
    mPickerTimestamp = new DateUtil.DateItem();
  }

  private void setupPrefs() {
    mHistory = Preferences.getHistory(getCtx());
    mSmoothing = Preferences.getSmoothingConstant(getCtx());
    mSensitivity = Preferences.getStdDevSensitivity(getCtx());
  }

  private void setupUI() {
    setupListeners();

    mCategoryMenuRow = (LinearLayout) findViewById(R.id.entry_edit_category_menu_row);
    mCategoryMenu = (DynamicSpinner) new DynamicSpinner(this);
    mCategoryMenuRow.addView(mCategoryMenu, new LinearLayout.LayoutParams(
        LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    mCategoryMenuRow.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
    mCategoryChanged = (TextView) findViewById(R.id.entry_edit_category_changed);
    mCategoryMenu.setOnItemSelectedListener(mCategoryMenuListener);
    setupMenu();

    mValueView = (EditText) findViewById(R.id.entry_edit_value);
    mValueChanged = (TextView) findViewById(R.id.entry_edit_value_changed);
    mValueView.setOnKeyListener(mValueViewListener);

    mTimestampView = (TextView) findViewById(R.id.entry_edit_timestamp);
    mTimestampChangedView = (TextView) findViewById(R.id.entry_edit_timestamp_changed);

    mAggregationView = (TextView) findViewById(R.id.entry_edit_n_entries);

    mPickDate = (Button) findViewById(R.id.entry_set_date);
    mPickDate.setOnClickListener(mPickDateListener);

    mPickTime = (Button) findViewById(R.id.entry_set_time);
    mPickTime.setOnClickListener(mPickTimeListener);

    mPickNow = (Button) findViewById(R.id.entry_set_now);
    mPickNow.setOnClickListener(mPickNowListener);

    mOk = (Button) findViewById(R.id.entry_edit_ok);
    mOk.setOnClickListener(mOkListener);

    mDelete = (Button) findViewById(R.id.entry_edit_delete);
    mDelete.setOnClickListener(mDeleteListener);
  }

  private void setupListeners() {
    mCategoryMenuListener = new OnItemSelectedListener() {
      public void onItemSelected(AdapterView parent, View v, int position,
          long id) {
        mCategoryId = (long) mCategoryMenu.getMappingFromPosition(position);
        updateCategoryMarker();
      }

      public void onNothingSelected(AdapterView arg0) {
        return;
      }
    };

    mPickDateListener = new View.OnClickListener() {
      public void onClick(View v) {
        showDialog(DATE_DIALOG_ID);
      }
    };

    mPickTimeListener = new View.OnClickListener() {
      public void onClick(View v) {
        showDialog(TIME_DIALOG_ID);
      }
    };

    mPickNowListener = new View.OnClickListener() {
      public void onClick(View v) {
        resetTimestamp();
      }
    };

    mValueViewListener = new View.OnKeyListener() {
      public boolean onKey(View v, int keyCode, KeyEvent event) {
        updateValueMarker();
        return false;
      }
    };

    mDateSetListener = new DatePickerDialog.OnDateSetListener() {
      public void onDateSet(DatePicker view, int year, int monthOfYear,
          int dayOfMonth) {
        mPickerTimestamp.mYear = year;
        mPickerTimestamp.mMonth = monthOfYear;
        mPickerTimestamp.mDay = dayOfMonth;
        updateDisplay();
      }
    };

    mTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
      public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        mPickerTimestamp.mHour = hourOfDay;
        mPickerTimestamp.mMinute = minute;
        mPickerTimestamp.mSecond = 0;
        updateDisplay();
      }
    };

    mOkListener = new View.OnClickListener() {
      public void onClick(View view) {
        mSave = true;
        setResult(RESULT_OK);
        finish();
      }
    };

    mDeleteListener = new View.OnClickListener() {
      public void onClick(View view) {
        getDbh().deleteEntry(mRowId);
        CategoryDbTable.Row row = getDbh().fetchCategory(mCategoryId);
        TimeSeriesCollector tsc = new TimeSeriesCollector(getDbh());
        tsc.updateTimeSeriesMeta(true);
        tsc.setHistory(mHistory);
        tsc.setSmoothing(mSmoothing);
        tsc.setSmoothing(mSensitivity);
        tsc.setInterpolators(((EvenTrendActivity) getCtx()).getInterpolators());
        tsc.updateCategoryTrend(mCategoryId);
        setResult(RESULT_OK);
        getDbh().close();
        finish();
      }
    };
  }

  public void setupMenu() {
    Cursor c = getDbh().fetchAllCategories();
    c.moveToFirst();

    for (int i = 0; i < c.getCount(); i++) {
      long catId = CategoryDbTable.getId(c);
      String label = CategoryDbTable.getCategoryName(c);
      mCategoryMenu.addSpinnerItem(label, catId);
      if (mCategoryId == catId) {
        mCategoryMenu.setSelection(i);
      }
      c.moveToNext();
    }
    c.close();
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case TIME_DIALOG_ID:
        return new TimePickerDialog(this, mTimeSetListener,
            mPickerTimestamp.mHour, mPickerTimestamp.mMinute, false);
      case DATE_DIALOG_ID:
        return new DatePickerDialog(this, mDateSetListener,
            mPickerTimestamp.mYear, mPickerTimestamp.mMonth,
            mPickerTimestamp.mDay);
    }
    return null;
  }

  @Override
  protected void onPrepareDialog(int id, Dialog dialog) {
    switch (id) {
      case TIME_DIALOG_ID:
        ((TimePickerDialog) dialog).updateTime(mPickerTimestamp.mHour,
            mPickerTimestamp.mMinute);
        break;
      case DATE_DIALOG_ID:
        ((DatePickerDialog) dialog).updateDate(mPickerTimestamp.mYear,
            mPickerTimestamp.mMonth, mPickerTimestamp.mDay);
        break;
    }
  }

  private void saveOriginalValues() {
    mOriginalTimestamp.mMillis = mPickerTimestamp.mMillis;
    mCal.setTimeInMillis(mOriginalTimestamp.mMillis);
    mOriginalTimestamp.setTo(mCal);
    mPickerTimestamp.setTo(mCal);

    mOriginalCategoryId = mCategoryId;
    mOriginalValue = mValue;
  }

  public void setTimestamp() {
    mCal.setTimeInMillis(mPickerTimestamp.mMillis);
    mPickerTimestamp.setTo(mCal);
    updateDisplay();
  }

  public void resetTimestamp() {
    mCal.setTimeInMillis(System.currentTimeMillis());
    mPickerTimestamp.setTo(mCal);
    updateDisplay();
  }

  private void updateValueMarker() {
    String s = mValueView.getText().toString();
    if (s.equals("")) {
      mValueChanged.setText("*");
      mOk.setClickable(false);
      mOk.setTextColor(Color.LTGRAY);
      return;
    }

    float value = Float.valueOf(s).floatValue();
    if (value != mOriginalValue) {
      mValueChanged.setText("*");
    } else {
      mValueChanged.setText("");
    }
    mOk.setClickable(true);
    mOk.setTextColor(Color.BLACK);
  }

  private void updateCategoryMarker() {
    if (mCategoryId != mOriginalCategoryId) {
      mCategoryChanged.setText("*");
    } else {
      mCategoryChanged.setText("");
    }
  }

  private void updateTimestampMarker() {
    if (mPickerTimestamp.isEqual(mOriginalTimestamp)) {
      mTimestampChangedView.setText("");
      mTimestampChanged = false;
    } else {
      mTimestampChangedView.setText("*");
      mTimestampChanged = true;
    }
  }

  private void updateDisplay() {
    mCal.set(Calendar.YEAR, mPickerTimestamp.mYear);
    mCal.set(Calendar.MONTH, mPickerTimestamp.mMonth);
    mCal.set(Calendar.DAY_OF_MONTH, mPickerTimestamp.mDay);
    mCal.set(Calendar.HOUR_OF_DAY, mPickerTimestamp.mHour);
    mCal.set(Calendar.MINUTE, mPickerTimestamp.mMinute);
    mCal.set(Calendar.SECOND, mPickerTimestamp.mSecond);
    mPickerTimestamp.setTo(mCal);

    mTimestampView.setText(DateUtil.toTimestamp(mCal));
    updateCategoryMarker();
    updateValueMarker();
    updateTimestampMarker();
  }

  private void populateFields(EntryDbTable.Row entry) {
    if (mRowId != null) {
      if (entry == null)
        mEntry = getDbh().fetchEntry(mRowId);
      else
        mEntry = entry;

      mCategoryId = mEntry.getCategoryId();
      mValue = mEntry.getValue();
      mPickerTimestamp.mMillis = mEntry.getTimestamp();
      mAggregation = mEntry.getNEntries();

      mValueView.setText(String.valueOf(mValue));
      mTimestampView.setText(DateUtil.toTimestamp(mPickerTimestamp.mMillis));
      mAggregationView.setText(Integer.valueOf(mAggregation).toString());
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    if (mRowId != null)
      outState.putLong(CategoryDbTable.KEY_ROWID, mRowId);
    else
      outState.putLong(CategoryDbTable.KEY_ROWID, -1);
    super.onSaveInstanceState(outState);
  }

  @Override
  protected void onPause() {
    super.onPause();
    saveState();
  }

  @Override
  protected void onResume() {
    super.onResume();
    setupPrefs();
    populateFields(null);
  }

  private void saveState() {
    if (mSave == true) {
      EntryDbTable.Row entry = new EntryDbTable.Row();

      entry.setId(mRowId);
      entry.setCategoryId(mCategoryId);
      entry.setValue(Float.valueOf(mValueView.getText().toString())
          .floatValue());
      entry.setTimestamp(mPickerTimestamp.mMillis);

      if (mRowId != null) {
        CategoryDbTable.Row cat = getDbh().fetchCategory(mCategoryId);

        if (mTimestampChanged == true && cat != null) {
          EntryDbTable.Row other = getDbh().fetchCategoryEntryInPeriod(
              cat.getId(), cat.getPeriodMs(), entry.getTimestamp());
          if (other != null) {
            other.setValue(other.getValue() + entry.getValue());
            other.setNEntries(other.getNEntries() + entry.getNEntries());
            getDbh().updateEntry(other);
            getDbh().deleteEntry(entry.getId());
            if (cat != null) {
              TimeSeriesCollector tsc = new TimeSeriesCollector(getDbh());
              tsc.updateTimeSeriesMeta(true);
              tsc.setHistory(mHistory);
              tsc.setSmoothing(mSmoothing);
              tsc.setSensitivity(mSensitivity);
              tsc.setInterpolators(((EvenTrendActivity) getCtx())
                  .getInterpolators());
              tsc.updateCategoryTrend(mCategoryId);
            }
            return;
          }
        }

        getDbh().updateEntry(entry);
        if (cat != null) {
          TimeSeriesCollector tsc = new TimeSeriesCollector(getDbh());
          tsc.updateTimeSeriesMeta(true);
          tsc.setHistory(mHistory);
          tsc.setSmoothing(mSmoothing);
          tsc.setSensitivity(mSensitivity);
          tsc.setInterpolators(((EvenTrendActivity) getCtx())
              .getInterpolators());
          tsc.updateCategoryTrend(mCategoryId);
        }
      }
    }
  }
}
