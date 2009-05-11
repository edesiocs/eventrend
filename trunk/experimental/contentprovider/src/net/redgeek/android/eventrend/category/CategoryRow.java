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

package net.redgeek.android.eventrend.category;

import android.database.Cursor;

import net.redgeek.android.eventrecorder.TimeSeriesData;

public class CategoryRow implements Comparable<CategoryRow> {
  public long   mId;
  public String mTimeSeriesName;
  public long   mRecordingDatapointId;
  public String mGroup;
  public double mDefaultValue;
  public double mIncrement;
  public double mGoal;
  public String mColor;
  public int    mPeriod;
  public String mUnits;
  public int    mRank;
  public String mAggregation;
  public String mType;
  public int    mZerofill;
  public String mFormula;
  public String mInterpolation;
  public double mSensitivity;
  public double mSmoothing;
  public int    mHistory;
  public int    mDecimals;

  public long   mTimestamp;
  
  private boolean mSelectable = true;

  public CategoryRow() {
    mTimestamp = 0;
  }

  public CategoryRow(Cursor c) {
    mTimestamp = 0;
    mId = TimeSeriesData.TimeSeries.getId(c);
    mTimeSeriesName = TimeSeriesData.TimeSeries.getTimeSeriesName(c);
    mGroup = TimeSeriesData.TimeSeries.getGroupName(c);
    mRecordingDatapointId = TimeSeriesData.TimeSeries.getRecordingDatapointId(c);
    mDefaultValue = TimeSeriesData.TimeSeries.getDefaultValue(c);
    mIncrement = TimeSeriesData.TimeSeries.getIncrement(c);
    mGoal = TimeSeriesData.TimeSeries.getGoal(c);
    mColor = TimeSeriesData.TimeSeries.getColor(c);
    mPeriod = TimeSeriesData.TimeSeries.getPeriod(c);
    mUnits = TimeSeriesData.TimeSeries.getUnits(c);
    mRank = TimeSeriesData.TimeSeries.getRank(c);
    mAggregation = TimeSeriesData.TimeSeries.getAggregation(c);
    mType = TimeSeriesData.TimeSeries.getType(c);
    mZerofill = TimeSeriesData.TimeSeries.getZerofill(c);
    mFormula = TimeSeriesData.TimeSeries.getFormula(c);
    mInterpolation = TimeSeriesData.TimeSeries.getInterpolation(c);
    mSensitivity = TimeSeriesData.TimeSeries.getSensitivity(c);
    mSmoothing = TimeSeriesData.TimeSeries.getSmoothing(c);
    mHistory = TimeSeriesData.TimeSeries.getHistory(c);
    mDecimals = TimeSeriesData.TimeSeries.getDecimals(c);
  }

  public CategoryRow(CategoryRow row) {
    mTimestamp = row.mTimestamp;
    mId = row.mId;
    mTimeSeriesName = row.mTimeSeriesName;
    mGroup = row.mGroup;
    mRecordingDatapointId = row.mRecordingDatapointId;
    mDefaultValue = row.mDefaultValue;
    mIncrement = row.mIncrement;
    mGoal = row.mGoal;
    mColor = row.mColor;
    mPeriod = row.mPeriod;
    mUnits = row.mUnits;
    mRank = row.mRank;
    mAggregation = row.mAggregation;
    mType = row.mType;
    mZerofill = row.mZerofill;
    mFormula = row.mFormula;
    mInterpolation = row.mInterpolation;
    mSensitivity = row.mSensitivity;
    mSmoothing = row.mSmoothing;
    mHistory = row.mHistory;
    mDecimals = row.mDecimals;
  }

  public boolean isSelectable() {
    return mSelectable;
  }

  public void setSelectable(boolean selectable) {
    mSelectable = selectable;
  }
  
  public int compareTo(CategoryRow other) {
    if (this.mRank < other.mRank)
      return -1;
    else if (this.mRank > other.mRank)
      return 1;
    return 0;
  }
}
