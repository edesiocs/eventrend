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

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TimePicker;

import net.redgeek.android.eventrecorder.DateMapCache;
import net.redgeek.android.eventrecorder.TimeSeriesData;
import net.redgeek.android.eventrecorder.TimeSeriesProvider;
import net.redgeek.android.eventrecorder.DateMapCache.DateItem;
import net.redgeek.android.eventrecorder.TimeSeriesData.Datapoint;
import net.redgeek.android.eventrecorder.TimeSeriesData.DateMap;
import net.redgeek.android.eventrecorder.TimeSeriesData.TimeSeries;
import net.redgeek.android.eventrend.EvenTrendActivity;
import net.redgeek.android.eventrend.R;

public class EntryEditActivity extends EvenTrendActivity {
  // Dialog IDs
  static final int START_TIME_DIALOG_ID = 0;
  static final int START_DATE_DIALOG_ID = 1;
  static final int END_TIME_DIALOG_ID = 2;
  static final int END_DATE_DIALOG_ID = 3;

  // UI elements
  private TextView mCategoryNameView;
  private TextView mValueLabel;
  private EditText mValueView;
  private TextView mEntriesLabel;
  private EditText mEntriesView;
  private Button mOk;
  private Button mDelete;
  private TextView mTsStartLabel;
  private TextView mTsStartValue;
  private Button mPickStartDate;
  private Button mPickStartTime;
  private Button mPickStartNow;
  private TextView mTsEndLabel;
  private TextView mTsEndValue;
  private Button mPickEndDate;
  private Button mPickEndTime;
  private Button mPickEndNow;
  private TableRow mTsEndRow;
  private TableRow mEditTsEndLabelRow;

  // Listeners
  private View.OnClickListener mPickStartDateListener;
  private View.OnClickListener mPickStartTimeListener;
  private View.OnClickListener mPickStartNowListener;
  private View.OnClickListener mPickEndDateListener;
  private View.OnClickListener mPickEndTimeListener;
  private View.OnClickListener mPickEndNowListener;
  private View.OnKeyListener mValueViewListener;
  private View.OnKeyListener mEntriesViewListener;
  private View.OnClickListener mOkListener;
  private View.OnClickListener mDeleteListener;
  private DatePickerDialog.OnDateSetListener mStartDateSetListener;
  private TimePickerDialog.OnTimeSetListener mStartTimeSetListener;
  private DatePickerDialog.OnDateSetListener mEndDateSetListener;
  private TimePickerDialog.OnTimeSetListener mEndTimeSetListener;

  // Private data
  private EntryRow mRow;
  private String mCategoryType;
  private String mCategoryName;
  private DateMapCache mDateMap;

  // original values
  private double mOriginalValue;
  private int mOriginalEntries;
  private DateMapCache.DateItem mOriginalStartTs;
  private DateMapCache.DateItem mOriginalEndTs;

  // picker values
  private DateMapCache.DateItem mStartPickerTimestamp;
  private DateMapCache.DateItem mEndPickerTimestamp;

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);

    setupData(icicle);
    setupUI();
    populateFields(mRow);

    updateDisplay();
  }

  private void setupData(Bundle icicle) {
    Long rowId = null;
    mRow = new EntryRow();
    
    Intent it = getIntent();
    // prefer uris ...
    if (it != null) {
      Uri uri = it.getData();
      if (uri != null) {
        try {
          String rowIdStr = uri.getPathSegments().get(TimeSeriesProvider.PATH_SEGMENT_DATAPOINT_ID);
          rowId = Long.valueOf(rowIdStr);
        } catch (Exception e) { } // nothing
      }
    }

    // try the icicle next ...
    if (rowId == null || rowId < 1) {
      if (icicle != null) {
        rowId = icicle.getLong(TimeSeriesData.Datapoint._ID);
        if (rowId < 0)
          rowId = null;
      }
      // lastly, fall back on the bundle ...
      if (rowId == null) {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
          rowId = extras.getLong(TimeSeriesData.Datapoint._ID);
        }
      }
    }
    
    mRow.mId = rowId;
    
    mOriginalStartTs = new DateMapCache.DateItem();
    mOriginalEndTs = new DateMapCache.DateItem();
    mStartPickerTimestamp = new DateMapCache.DateItem();
    mEndPickerTimestamp = new DateMapCache.DateItem();

    mDateMap = new DateMapCache();
    mDateMap.populateCache(mCtx);

    Uri uri = ContentUris.withAppendedId(
        TimeSeriesData.Datapoint.CONTENT_URI, mRow.mId);
    Cursor c = mCtx.getContentResolver().query(uri, null, null, null, null);
    if (c != null && c.getCount() > 0) {      
      c.moveToFirst();
      mRow.mTimeSeriesId = Datapoint.getTimeSeriesId(c);
      mRow.mValue = Datapoint.getValue(c);
      mRow.mEntries = Datapoint.getEntries(c);
      mRow.mTsStart = Datapoint.getTsStart(c);
      mRow.mTsEnd = Datapoint.getTsEnd(c);
      
      saveOriginalValues(mRow.mValue, mRow.mEntries, mRow.mTsStart, mRow.mTsEnd);
    }
    c.close();

    uri = ContentUris.withAppendedId(
        TimeSeriesData.TimeSeries.CONTENT_URI, mRow.mTimeSeriesId);
    c = mCtx.getContentResolver().query(uri, null, null, null, null);
    if (c != null && c.getCount() > 0) {      
      c.moveToFirst();
      mCategoryType = TimeSeries.getType(c);
      mCategoryName = TimeSeries.getTimeSeriesName(c);
    }
    c.close();
  }

  private void setupUI() {
    setContentView(R.layout.entry_edit);

    setupListeners();

    mCategoryNameView = (TextView) findViewById(R.id.entry_edit_name);
    mValueView = (EditText) findViewById(R.id.entry_edit_value);
    mValueLabel = (TextView) findViewById(R.id.entry_value_label);
    mEntriesView = (EditText) findViewById(R.id.entry_edit_entries);
    mEntriesLabel = (TextView) findViewById(R.id.entry_entries_label);

    if (mCategoryType.toLowerCase().equals(TimeSeries.TYPE_DISCRETE.toLowerCase())) {      
      mValueView.setOnKeyListener(mValueViewListener);
      mEntriesView.setOnKeyListener(mEntriesViewListener);
    } else {
      mValueView.setFocusable(false);
      mEntriesView.setFocusable(false);
    }

    mTsStartLabel = (TextView) findViewById(R.id.entry_ts_start_label);
    mTsStartValue = (TextView) findViewById(R.id.entry_edit_ts_start);
    mPickStartDate = (Button) findViewById(R.id.entry_set_ts_start_date);
    mPickStartTime = (Button) findViewById(R.id.entry_set_ts_start_time);
    mPickStartNow = (Button) findViewById(R.id.entry_set_ts_start_now);

    if (mCategoryType.toLowerCase().equals(TimeSeries.TYPE_DISCRETE.toLowerCase()) ||
        mCategoryType.toLowerCase().equals(TimeSeries.TYPE_RANGE.toLowerCase())) {      
      if (mCategoryType.toLowerCase().equals(TimeSeries.TYPE_RANGE.toLowerCase())) {  
        mTsStartLabel.setText("Start Timestamp");
      }
      mPickStartDate.setOnClickListener(mPickStartDateListener);
      mPickStartTime.setOnClickListener(mPickStartTimeListener);
      mPickStartNow.setOnClickListener(mPickStartNowListener);
    } else {
      mPickStartDate.setVisibility(View.GONE);
      mPickStartTime.setVisibility(View.GONE);
      mPickStartNow.setVisibility(View.GONE);
    }

    mTsEndLabel = (TextView) findViewById(R.id.entry_ts_end_label);
    mTsEndValue = (TextView) findViewById(R.id.entry_edit_ts_end);
    mPickEndDate = (Button) findViewById(R.id.entry_set_ts_end_date);
    mPickEndTime = (Button) findViewById(R.id.entry_set_ts_end_time);
    mPickEndNow = (Button) findViewById(R.id.entry_set_ts_end_now);
    mTsEndRow = (TableRow) findViewById(R.id.entry_ts_end_row);
    mEditTsEndLabelRow = (TableRow) findViewById(R.id.entry_ts_end_label_row);

    if (mCategoryType.toLowerCase().equals(TimeSeries.TYPE_RANGE.toLowerCase())) {
      mPickEndDate.setOnClickListener(mPickEndDateListener);
      mPickEndTime.setOnClickListener(mPickEndTimeListener);
      mPickEndNow.setOnClickListener(mPickEndNowListener);
    } else {
      mPickEndDate.setVisibility(View.GONE);
      mPickEndTime.setVisibility(View.GONE);
      mPickEndNow.setVisibility(View.GONE);
      mTsEndRow.setVisibility(View.GONE);
      mEditTsEndLabelRow.setVisibility(View.GONE);
    }

    mOk = (Button) findViewById(R.id.entry_edit_ok);
    mOk.setOnClickListener(mOkListener);

    mDelete = (Button) findViewById(R.id.entry_edit_delete);
    mDelete.setOnClickListener(mDeleteListener);
  }

  private void setupListeners() {
    mPickStartDateListener = new View.OnClickListener() {
      public void onClick(View v) {
        showDialog(START_DATE_DIALOG_ID);
      }
    };

    mPickStartTimeListener = new View.OnClickListener() {
      public void onClick(View v) {
        showDialog(START_TIME_DIALOG_ID);
      }
    };

    mPickStartNowListener = new View.OnClickListener() {
      public void onClick(View v) {
        resetTimestamp(mStartPickerTimestamp);
      }
    };

    mPickEndDateListener = new View.OnClickListener() {
      public void onClick(View v) {
        showDialog(END_DATE_DIALOG_ID);
      }
    };

    mPickEndTimeListener = new View.OnClickListener() {
      public void onClick(View v) {
        showDialog(END_TIME_DIALOG_ID);
      }
    };

    mPickEndNowListener = new View.OnClickListener() {
      public void onClick(View v) {
        resetTimestamp(mEndPickerTimestamp);
      }
    };

    mValueViewListener = new View.OnKeyListener() {
      public boolean onKey(View v, int keyCode, KeyEvent event) {
        updateValueMarker();
        return false;
      }
    };

    mEntriesViewListener = new View.OnKeyListener() {
      public boolean onKey(View v, int keyCode, KeyEvent event) {
        updateEntriesMarker();
        return false;
      }
    };

    mStartDateSetListener = new DatePickerDialog.OnDateSetListener() {
      public void onDateSet(DatePicker view, int year, int monthOfYear,
          int dayOfMonth) {
        mStartPickerTimestamp.mYear = year;
        mStartPickerTimestamp.mMonth = monthOfYear;
        mStartPickerTimestamp.mDay = dayOfMonth;
        updateDisplay();
      }
    };

    mStartTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
      public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        mStartPickerTimestamp.mHour = hourOfDay;
        mStartPickerTimestamp.mMinute = minute;
        mStartPickerTimestamp.mSecond = 0;
        updateDisplay();
      }
    };

    mEndDateSetListener = new DatePickerDialog.OnDateSetListener() {
      public void onDateSet(DatePicker view, int year, int monthOfYear,
          int dayOfMonth) {
        mEndPickerTimestamp.mYear = year;
        mEndPickerTimestamp.mMonth = monthOfYear;
        mEndPickerTimestamp.mDay = dayOfMonth;
        updateDisplay();
      }
    };

    mEndTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
      public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        mEndPickerTimestamp.mHour = hourOfDay;
        mEndPickerTimestamp.mMinute = minute;
        mEndPickerTimestamp.mSecond = 0;
        updateDisplay();
      }
    };

    mOkListener = new View.OnClickListener() {
      public void onClick(View view) {
        setResult(saveState());
        finish();
      }
    };

    mDeleteListener = new View.OnClickListener() {
      public void onClick(View view) {
        Uri uri = ContentUris.withAppendedId(
            TimeSeriesData.TimeSeries.CONTENT_URI, mRow.mTimeSeriesId).buildUpon()
            .appendPath("datapoints").appendPath(""+mRow.mId).build();
        mCtx.getContentResolver().delete(uri, null, null);
        setResult(ENTRY_DELETED);
        finish();
      }
    };
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case START_TIME_DIALOG_ID:
        return new TimePickerDialog(this, mStartTimeSetListener,
            mStartPickerTimestamp.mHour, mStartPickerTimestamp.mMinute, false);
      case START_DATE_DIALOG_ID:
        return new DatePickerDialog(this, mStartDateSetListener,
            mStartPickerTimestamp.mYear, mStartPickerTimestamp.mMonth,
            mStartPickerTimestamp.mDay);
      case END_TIME_DIALOG_ID:
        return new TimePickerDialog(this, mEndTimeSetListener,
            mEndPickerTimestamp.mHour, mEndPickerTimestamp.mMinute, false);
      case END_DATE_DIALOG_ID:
        return new DatePickerDialog(this, mEndDateSetListener,
            mEndPickerTimestamp.mYear, mEndPickerTimestamp.mMonth,
            mEndPickerTimestamp.mDay);
    }
    return null;
  }

  @Override
  protected void onPrepareDialog(int id, Dialog dialog) {
    switch (id) {
      case START_TIME_DIALOG_ID:
        ((TimePickerDialog) dialog).updateTime(mStartPickerTimestamp.mHour,
            mStartPickerTimestamp.mMinute);
        break;
      case START_DATE_DIALOG_ID:
        ((DatePickerDialog) dialog).updateDate(mStartPickerTimestamp.mYear,
            mStartPickerTimestamp.mMonth, mStartPickerTimestamp.mDay);
        break;
      case END_TIME_DIALOG_ID:
        ((TimePickerDialog) dialog).updateTime(mEndPickerTimestamp.mHour,
            mEndPickerTimestamp.mMinute);
        break;
      case END_DATE_DIALOG_ID:
        ((DatePickerDialog) dialog).updateDate(mEndPickerTimestamp.mYear,
            mEndPickerTimestamp.mMonth, mEndPickerTimestamp.mDay);
        break;
    }
  }

  private void saveOriginalValues(double value, int entries, int tsStart, int tsEnd) {
    mDateMap.getDateItem(tsStart, mOriginalStartTs);
    mStartPickerTimestamp.set(mOriginalStartTs);

    mDateMap.getDateItem(tsEnd, mOriginalEndTs);
    mEndPickerTimestamp.set(mOriginalEndTs);

    mOriginalValue = value;
    mOriginalEntries = entries;
  }

  public void resetTimestamp(DateItem timestamp) {
    timestamp.mEpochSeconds = (int) (System.currentTimeMillis() / DateMap.SECOND_MS);
    mDateMap.getDateItem(timestamp.mEpochSeconds, timestamp);
    updateDisplay();
  }

  private void updateValueMarker() {
    String s = mValueView.getText().toString();
    if (s.equals("")) {
      mValueLabel.setTextColor(Color.RED);
    }

    double value = Double.valueOf(s).doubleValue();
    if (value != mOriginalValue) {
      mValueLabel.setTextColor(Color.RED);
    } else {
      mValueLabel.setTextColor(Color.LTGRAY);
    }
  }

  private void updateEntriesMarker() {
    String s = mEntriesView.getText().toString();
    if (s.equals("")) {
      mEntriesLabel.setTextColor(Color.RED);
    }

    int entries = Integer.valueOf(s).intValue();
    if (entries != mOriginalEntries) {
      mEntriesLabel.setTextColor(Color.RED);
    } else {
      mEntriesLabel.setTextColor(Color.LTGRAY);
    }
  }

  private void updateTimestampMarker() {
    if (mStartPickerTimestamp.isEqual(mOriginalStartTs)) {
      mTsStartLabel.setTextColor(Color.LTGRAY);
    } else {
      mTsStartLabel.setTextColor(Color.RED);
    }

    if (mEndPickerTimestamp.isEqual(mOriginalEndTs)) {
      mTsEndLabel.setTextColor(Color.LTGRAY);
    } else {
      mTsEndLabel.setTextColor(Color.RED);
    }
  }

  private void updateDisplay() {
    String str;
    int seconds;
    
    seconds = mDateMap.getEpochSeconds(mStartPickerTimestamp);
    str = mDateMap.toDisplayTime(seconds, 0);
    mTsStartValue.setText(str);
    
    seconds = mDateMap.getEpochSeconds(mEndPickerTimestamp);
    str = mDateMap.toDisplayTime(seconds, 0);
    mTsEndValue.setText(str);
    
    updateValueMarker();
    updateEntriesMarker();
    updateTimestampMarker();
  }

  private void populateFields(EntryRow entry) {
    if (mRow.mId > 0) {
      mCategoryNameView.setText(mCategoryName);
      mValueView.setText(String.valueOf(mRow.mValue));
      mEntriesView.setText(String.valueOf(mRow.mEntries));
      mTsStartValue.setText(mDateMap.toDisplayTime(mStartPickerTimestamp.mEpochSeconds, 0));
      mTsEndValue.setText(mDateMap.toDisplayTime(mEndPickerTimestamp.mEpochSeconds, 0));
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    if (mRow.mId > 0) {
      outState.putLong(Datapoint._ID, mRow.mId);
      outState.putLong(Datapoint.TIMESERIES_ID, mRow.mTimeSeriesId);
    }
    super.onSaveInstanceState(outState);
  }

  @Override
  protected void onPause() {
    super.onPause();
  }

  @Override
  protected void onResume() {
    super.onResume();
    populateFields(null);
  }

  private int saveState() {
    ContentValues values = new ContentValues();
    Datapoint.setEntries(values, Integer.parseInt(mEntriesView.getText().toString()));
    Datapoint.setTsStart(values, mStartPickerTimestamp.mEpochSeconds);
    
    if (mCategoryType.toLowerCase().equals(TimeSeries.TYPE_RANGE.toLowerCase())) {
      Datapoint.setTsEnd(values, mEndPickerTimestamp.mEpochSeconds);
      Datapoint.setValue(values, mEndPickerTimestamp.mEpochSeconds - mStartPickerTimestamp.mEpochSeconds);
    } else {
      Datapoint.setValue(values, Double.parseDouble(mValueView.getText().toString()));
      Datapoint.setTsEnd(values, mStartPickerTimestamp.mEpochSeconds);
    }
      
    if (mRow.mId > 0 && mRow.mTimeSeriesId > 0) {
      Uri uri = ContentUris.withAppendedId(
          TimeSeriesData.TimeSeries.CONTENT_URI, mRow.mTimeSeriesId).buildUpon()
          .appendPath("datapoints").appendPath(""+mRow.mId).build();
      getContentResolver().update(uri, values, null, null);
      return ENTRY_MODIFIED;
    }
    return ENTRY_OP_ERR;
  }
}
