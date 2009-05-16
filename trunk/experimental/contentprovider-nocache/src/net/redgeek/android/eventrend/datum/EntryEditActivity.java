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
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.AdapterView.OnItemSelectedListener;

import net.redgeek.android.eventgrapher.primitives.TimeSeriesCollector;
import net.redgeek.android.eventrecorder.TimeSeriesData;
import net.redgeek.android.eventrecorder.TimeSeriesProvider;
import net.redgeek.android.eventrecorder.TimeSeriesData.Datapoint;
import net.redgeek.android.eventrecorder.TimeSeriesData.TimeSeries;
import net.redgeek.android.eventrend.EvenTrendActivity;
import net.redgeek.android.eventrend.R;
import net.redgeek.android.eventrend.util.DateUtil;

import java.util.Calendar;

public class EntryEditActivity extends EvenTrendActivity {
  // Dialog IDs
  static final int START_TIME_DIALOG_ID = 0;
  static final int START_DATE_DIALOG_ID = 1;
  static final int END_TIME_DIALOG_ID = 2;
  static final int END_DATE_DIALOG_ID = 3;

  // UI elements
  private TextView mCategoryName;
  private TextView mValueLabel;
  private EditText mValueView;
  private TextView mEntriesLabel;
  private EditText mEntriesView;
  private Button mOk;
  private Button mDelete;
  private TextView mTsStartLabel;
  private Button mPickStartDate;
  private Button mPickStartTime;
  private Button mPickStartNow;
  private TextView mTsEndLabel;
  private Button mPickEndDate;
  private Button mPickEndTime;
  private Button mPickEndNow;
  private TableRow mTsEndRow;
  private TableRow mEditTsEndRow;

  // Listeners
  private OnItemSelectedListener mCategoryMenuListener;
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
  private double mValue = 0.0;
  private boolean mTimestampChanged = false;
  private boolean mSave = false;
  private int mAggregation;

  // original values
  private long mOriginalCategoryId;
  private double mOriginalValue;
  private DateUtil.DateItem mOriginalTimestamp;

  // picker values
  private DateUtil.DateItem mStartPickerTimestamp;
  private DateUtil.DateItem mEndPickerTimestamp;
  private Calendar mCal;

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);

    setupData(icicle);
    setupUI();
    populateFields(mRow);
    saveOriginalValues();

    setTimestamp();
  }

  private void setupData(Bundle icicle) {
    Long rowId = null;
    Long catId = null;
    mRow = new EntryRow("");
    
    Intent it = getIntent();
    // prefer uris ...
    if (it != null) {
      Uri uri = it.getData();
      if (uri != null) {
        try {
          String rowIdStr = uri.getPathSegments().get(TimeSeriesProvider.PATH_SEGMENT_DATAPOINT_ID);
          rowId = Long.valueOf(rowIdStr);
          String catIdStr = uri.getPathSegments().get(TimeSeriesProvider.PATH_SEGMENT_TIMERSERIES_ID);
          catId = Long.valueOf(catIdStr);
        } catch (Exception e) { } // nothing
      }
    }

    // try the icicle next ...
    if (rowId == null || rowId < 1) {
      if (icicle != null) {
        rowId = icicle.getLong(TimeSeriesData.Datapoint._ID);
        if (rowId < 0)
          rowId = null;
        catId = icicle.getLong(TimeSeriesData.Datapoint.TIMESERIES_ID);
        if (catId < 0)
          catId = null;
      }
      // lastly, fall back on the bundle ...
      if (rowId == null) {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
          rowId = extras.getLong(TimeSeriesData.Datapoint._ID);
          catId = extras.getLong(TimeSeriesData.Datapoint.TIMESERIES_ID);
        }
      }
    }
    
    mRow.mId = rowId;
    mRow.mTimeSeriesId = catId;
    
    Uri uri = ContentUris.withAppendedId(
        TimeSeriesData.TimeSeries.CONTENT_URI, mRow.mTimeSeriesId).buildUpon()
        .appendPath("datapoints").appendPath(""+mRow.mId).build();
    Cursor c = mCtx.getContentResolver().query(uri, null, null, null, null);
    if (c != null && c.getCount() > 0) {      
      c.moveToFirst();
      // TODO:  fetch original data
    }
    c.close();

    uri = ContentUris.withAppendedId(
        TimeSeriesData.TimeSeries.CONTENT_URI, mRow.mTimeSeriesId);
    c = mCtx.getContentResolver().query(uri, null, null, null, null);
    if (c != null && c.getCount() > 0) {      
      c.moveToFirst();
      mCategoryType = TimeSeries.getType(c);
      mRow.mName = TimeSeries.getTimeSeriesName(c);
    }
    c.close();

    mCal = Calendar.getInstance();
    mOriginalTimestamp = new DateUtil.DateItem();
    mStartPickerTimestamp = new DateUtil.DateItem();
    mEndPickerTimestamp = new DateUtil.DateItem();
  }

  private void setupUI() {
    setContentView(R.layout.entry_edit);

    setupListeners();

    mCategoryName = (TextView) findViewById(R.id.entry_edit_name);

    mValueView = (EditText) findViewById(R.id.entry_edit_value);
    mValueView.setOnKeyListener(mValueViewListener);
    mValueLabel = (TextView) findViewById(R.id.entry_value_label);

    mEntriesView = (EditText) findViewById(R.id.entry_edit_entries);
    mEntriesView.setOnKeyListener(mValueViewListener);
    mEntriesLabel = (TextView) findViewById(R.id.entry_entries_label);

    mTsStartLabel = (TextView) findViewById(R.id.entry_ts_start_label);
    mPickStartDate = (Button) findViewById(R.id.entry_set_ts_start_date);
    mPickStartDate.setOnClickListener(mPickStartDateListener);
    mPickStartTime = (Button) findViewById(R.id.entry_set_ts_start_time);
    mPickStartTime.setOnClickListener(mPickStartTimeListener);
    mPickStartNow = (Button) findViewById(R.id.entry_set_ts_start_now);
    mPickStartNow.setOnClickListener(mPickStartNowListener);

    mTsEndLabel = (TextView) findViewById(R.id.entry_ts_end_label);
    mPickEndDate = (Button) findViewById(R.id.entry_set_ts_end_date);
    mPickEndDate.setOnClickListener(mPickEndDateListener);
    mPickEndTime = (Button) findViewById(R.id.entry_set_ts_end_time);
    mPickEndTime.setOnClickListener(mPickEndTimeListener);
    mPickEndNow = (Button) findViewById(R.id.entry_set_ts_end_now);
    mPickEndNow.setOnClickListener(mPickEndNowListener);

    mTsEndRow = (TableRow) findViewById(R.id.entry_ts_end_row);
    mEditTsEndRow = (TableRow) findViewById(R.id.entry_edit_ts_end_row);

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

    mPickStartDateListener = new View.OnClickListener() {
      public void onClick(View v) {
        showDialog(END_DATE_DIALOG_ID);
      }
    };

    mPickStartTimeListener = new View.OnClickListener() {
      public void onClick(View v) {
        showDialog(END_TIME_DIALOG_ID);
      }
    };

    mPickStartNowListener = new View.OnClickListener() {
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
        mSave = true;
        setResult(RESULT_OK);
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

    double value = Double.valueOf(s).doubleValue();
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

  private void populateFields(EntryRow entry) {
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
    if (mRowId != null) {
      outState.putLong(Datapoint._ID, mRowId);
      outState.putLong(Datapoint.TIMESERIES_ID, mTimeSeriesId);
    } else {
      outState.putLong(CategoryDbTable.KEY_ROWID, -1);
    }
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
      entry.setValue(Double.valueOf(mValueView.getText().toString())
          .doubleValue());
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
              tsc.setHistory(mHistory);
              tsc.setSmoothing(mSmoothing);
              tsc.setSensitivity(mSensitivity);
              tsc.setInterpolators(((EvenTrendActivity) getCtx())
                  .getInterpolators());
              tsc.updateTimeSeriesMetaLocking(true);
              tsc.updateCategoryTrend(mCategoryId);
            }
            return;
          }
        }

        getDbh().updateEntry(entry);
        if (cat != null) {
          TimeSeriesCollector tsc = new TimeSeriesCollector(getDbh());
          tsc.setHistory(mHistory);
          tsc.setSmoothing(mSmoothing);
          tsc.setSensitivity(mSensitivity);
          tsc.setInterpolators(((EvenTrendActivity) getCtx())
              .getInterpolators());
          tsc.updateTimeSeriesMetaLocking(true);
          tsc.updateCategoryTrend(mCategoryId);
        }
      }
    }
  }
}
