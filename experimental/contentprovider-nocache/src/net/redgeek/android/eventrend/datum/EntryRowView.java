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

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import net.redgeek.android.eventrecorder.DateMapCache;
import net.redgeek.android.eventrecorder.TimeSeriesData.TimeSeries;
import net.redgeek.android.eventrend.R;
import net.redgeek.android.eventrend.util.DateUtil;

public class EntryRowView extends TableLayout {
  private TextView mValueText;
  private TextView mTimestampText;

  private long mRowId;
  private String mRowType;
  private Context mCtx;
  private DateMapCache mDateMap;
  
  private LinearLayout mRow;
  private int mPad = 2;

  public EntryRowView(Context context, EntryRow anEntry, DateMapCache dateMap, String rowType) {
    super(context);
    mCtx = context;
    mDateMap = dateMap;
    mRowType = rowType;
    setupUI(context);
    populateFields(anEntry);
  }

  private void setupUI(Context context) {
    setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);

    LayoutInflater inflater = (LayoutInflater) mCtx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    inflater.inflate(R.layout.entry_row, this);

    mRow = (LinearLayout) findViewById(R.id.entry_row);
    mTimestampText = (TextView) findViewById(R.id.entry_timestamp);
    mValueText = (TextView) findViewById(R.id.entry_value);
  }

  private void populateFields(EntryRow entry) {
    mTimestampText.setText(mDateMap.toDisplayTime(entry.mTsStart, 0));
    if (mRowType.equals(TimeSeries.TYPE_RANGE)) {
      mValueText.setText(DateMapCache.toScaledTime(entry.mValue));
    } else {
      mValueText.setText(Double.valueOf(entry.mValue).toString());
    }
    mRowId = entry.mId;
  }

  public void setValue(double value) {
    if (mRowType.equals(TimeSeries.TYPE_RANGE)) {
      mValueText.setText(DateMapCache.toScaledTime(value));
    } else {
      mValueText.setText(Double.valueOf(value).toString());
    }
  }

  public void setTimestamp(int timestamp) {
    mTimestampText.setText(mDateMap.toDisplayTime(timestamp, 0));
  }

  public long getRowId() {
    return mRowId;
  }
}