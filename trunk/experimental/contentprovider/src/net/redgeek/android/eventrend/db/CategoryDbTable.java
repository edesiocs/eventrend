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

package net.redgeek.android.eventrend.db;

import net.redgeek.android.eventrecorder.TimeSeriesData;
import net.redgeek.android.eventrecorder.TimeSeriesData.TimeSeriesRow;
import net.redgeek.android.eventrend.util.DateUtil;
import android.database.Cursor;
import android.graphics.Color;

/**
 * Class encapsulating the database table definition, exportable contents,
 * acceptable values, and convenience routines for interacting with the DB
 * table.
 * 
 * @author barclay
 * 
 */
public class CategoryDbTable {
  public static final String AGGREGATION_SUM = "Sum";
  public static final String AGGREGATION_AVG = "Average";

  public static final String TYPE_DISCRETE = "Discrete";
  public static final String TYPE_RANGE = "Range";
  public static final String TYPE_SYNTHETIC = "Synthetic";

  public static final String INTERP_LINEAR = "Linear";
  public static final String INTERP_STEPEARLY = "StepEarly";
  public static final String INTERP_STEPMID = "StepMid";
  public static final String INTERP_STEPLATE = "StepLate";
  public static final String INTERP_CUBIC = "Cubic";

  public static final String PERIOD_NONE = "None";
  public static final String PERIOD_HOUR = "Hour";
  public static final String PERIOD_AMPM = "AM/PM";
  public static final String PERIOD_DAY = "Day";
  public static final String PERIOD_WEEK = "Week";
  public static final String PERIOD_MONTH = "Month";
  public static final String PERIOD_QUARTER = "Quarter";
  public static final String PERIOD_YEAR = "Year";
  public static final String[] PERIODS = { PERIOD_NONE,
      PERIOD_HOUR, PERIOD_AMPM, PERIOD_DAY, PERIOD_WEEK,
      PERIOD_MONTH, PERIOD_QUARTER, PERIOD_YEAR };
  public static final long[] PERIODS_MS = { 0, DateUtil.HOUR_MS,
      DateUtil.AMPM_MS, DateUtil.DAY_MS, DateUtil.WEEK_MS, DateUtil.MONTH_MS,
      DateUtil.QUARTER_MS, DateUtil.YEAR_MS };
  // This is only used sometimes, so isn't in the regular list
  public static final long PERIOD_MS_AUTO = -1;

  public static final String TREND_DOWN_15_GOOD = "trend_down_15_good";
  public static final String TREND_DOWN_15_BAD = "trend_down_15_bad";
  public static final String TREND_DOWN_30_GOOD = "trend_down_30_good";
  public static final String TREND_DOWN_30_BAD = "trend_down_30_bad";
  public static final String TREND_DOWN_45_GOOD = "trend_down_45_good";
  public static final String TREND_DOWN_45_BAD = "trend_down_45_bad";
  public static final String TREND_UP_15_GOOD = "trend_up_15_good";
  public static final String TREND_UP_15_BAD = "trend_up_15_bad";
  public static final String TREND_UP_30_GOOD = "trend_up_30_good";
  public static final String TREND_UP_30_BAD = "trend_up_30_bad";
  public static final String TREND_UP_45_GOOD = "trend_up_45_good";
  public static final String TREND_UP_45_BAD = "trend_up_45_bad";
  public static final String TREND_FLAT = "trend_flat";
  public static final String TREND_FLAT_GOAL = "trend_flat_goal";
  public static final String TREND_DOWN_15 = "trend_down_15";
  public static final String TREND_UP_15 = "trend_up_15";
  public static final String TREND_UNKNOWN = "trend_unknown";

  public static long mapPeriodToMs(String period) {
    for (int i = 0; i < CategoryDbTable.PERIODS.length; i++) {
      if (period.equals(CategoryDbTable.PERIODS[i]))
        return CategoryDbTable.PERIODS_MS[i];
    }
    return CategoryDbTable.PERIODS_MS[0];
  }

  public static String mapMsToPeriod(long ms) {
    for (int i = 0; i < PERIODS_MS.length; i++) {
      if (ms == PERIODS_MS[i])
        return PERIODS[i];
    }
    return PERIODS[0];
  }

  public static int mapMsToIndex(long ms) {
    for (int i = 0; i < PERIODS_MS.length; i++) {
      if (ms == PERIODS_MS[i])
        return i;
    }
    return 0;
  }

  public static long getId(Cursor c) {
    return c.getLong(c.getColumnIndexOrThrow("_id"));
  }

  public static String getGroupName(Cursor c) {
    return c.getString(c.getColumnIndexOrThrow(TimeSeriesData.TimeSeriesRow.GROUP_NAME));
  }

  public static String getCategoryName(Cursor c) {
    return c.getString(c.getColumnIndexOrThrow(TimeSeriesData.TimeSeriesRow.TIMESERIES_NAME));
  }

  public static float getDefaultValue(Cursor c) {
    return c.getFloat(c.getColumnIndexOrThrow(TimeSeriesData.TimeSeriesRow.DEFAULT_VALUE));
  }

  public static float getLastValue(Cursor c) {
    return c.getFloat(c.getColumnIndexOrThrow(TimeSeriesData.TimeSeriesRow.RECENT_VALUE));
  }

  public static long getLastTimestamp(Cursor c) {
    return c.getLong(c.getColumnIndexOrThrow(TimeSeriesData.TimeSeriesRow.RECENT_TIMESTAMP));
  }

  public static float getLastTrend(Cursor c) {
    return c.getFloat(c.getColumnIndexOrThrow(TimeSeriesData.TimeSeriesRow.RECENT_TREND));
  }

  public static float getIncrement(Cursor c) {
    return c.getFloat(c.getColumnIndexOrThrow(TimeSeriesData.TimeSeriesRow.INCREMENT));
  }

  public static float getGoal(Cursor c) {
    return c.getFloat(c.getColumnIndexOrThrow(TimeSeriesData.TimeSeriesRow.GOAL));
  }

  public static String getAggregation(Cursor c) {
    return c.getString(c.getColumnIndexOrThrow(TimeSeriesData.TimeSeriesRow.AGGREGATION));
  }

  public static String getColor(Cursor c) {
    return c.getString(c.getColumnIndexOrThrow(TimeSeriesData.TimeSeriesRow.COLOR));
  }

  public static int getColorInt(Cursor c) {
    int colorInt;
    try {
      colorInt = Color.parseColor(getColor(c));
    } catch (IllegalArgumentException e) {
      colorInt = Color.BLACK;
    }
    return colorInt;
  }

  public static long getPeriodM(Cursor c) {
    return c.getLong(c.getColumnIndexOrThrow(TimeSeriesData.TimeSeriesRow.PERIOD));
  }

  public static int getRank(Cursor c) {
    return c.getInt(c.getColumnIndexOrThrow(TimeSeriesData.TimeSeriesRow.RANK));
  }

  public static String getTrendState(Cursor c) {
    return c.getString(c.getColumnIndexOrThrow(TimeSeriesData.TimeSeriesRow.TREND_STATE));
  }

  public static String getInterpolation(Cursor c) {
    return c.getString(c.getColumnIndexOrThrow(TimeSeriesData.TimeSeriesRow.INTERPOLATION));
  }

  public static boolean getZeroFill(Cursor c) {
    return c.getInt(c.getColumnIndexOrThrow(TimeSeriesData.TimeSeriesRow.ZEROFILL)) == 0 ? false : true;
  }

  public static String getType(Cursor c) {
    return c.getString(c.getColumnIndexOrThrow(TimeSeriesData.TimeSeriesRow.TYPE));
  }

  public static String getFormula(Cursor c) {
    return c.getString(c.getColumnIndexOrThrow(TimeSeriesData.TimeSeriesRow.FORMULA));
  }

  public static class Row {
    private long mId = 0;
    private String mGroupName = "";
    private String mCategoryName = "";
    private float mDefaultValue = 0.0f;
    private float mRecentValue = 0.0f;
    private long mRecentTimestamp = 0;
    private float mRecentTrend = 0.0f;
    private float mIncrement = 0.0f;
    private float mGoal = 0.0f;
    private String mAggregation = AGGREGATION_SUM;
    private String mType = TYPE_DISCRETE;
    private String mColor = "#000000";
    private long mPeriod = 0;
    private int mRank = 0;
    private String mTrendState = TREND_UNKNOWN;
    private String mInterpolation = INTERP_CUBIC;
    private boolean mZeroFill = false;
    private String mFormula = "";

    public Row() {
    }

    public Row(Row r) {
      set(r);
    }

    public Row(Cursor c) {
      populateFromCursor(c);
    }
    
    public void set(Row r) {
      mId = r.mId;
      mGroupName = new String(r.mGroupName);
      mCategoryName = new String(r.mCategoryName);
      mDefaultValue = r.mDefaultValue;
      mRecentValue = r.mRecentValue;
      mRecentTimestamp = r.mRecentTimestamp;
      mRecentTrend = r.mRecentTrend;
      mIncrement = r.mIncrement;
      mGoal = r.mGoal;
      mAggregation = new String(r.mAggregation);
      mType = new String(r.mType);
      mColor = new String(r.mColor);
      mPeriod = r.mPeriod;
      mRank = r.mRank;
      mTrendState = new String(r.mTrendState);
      mInterpolation = new String(r.mInterpolation);
      mZeroFill = r.mZeroFill;
      mFormula = new String(r.mFormula);
    }

    public void populateFromCursor(Cursor c) {
      if (c == null)
        return;

      mId = c.getLong(c.getColumnIndexOrThrow("_id"));
      mGroupName = c.getString(c.getColumnIndexOrThrow(TimeSeriesRow.GROUP_NAME));
      mCategoryName = c.getString(c.getColumnIndexOrThrow(TimeSeriesRow.TIMESERIES_NAME));
      mDefaultValue = c.getFloat(c.getColumnIndexOrThrow(TimeSeriesRow.DEFAULT_VALUE));
      mRecentValue = c.getFloat(c.getColumnIndexOrThrow(TimeSeriesRow.RECENT_VALUE));
      mRecentTimestamp = c.getLong(c.getColumnIndexOrThrow(TimeSeriesRow.RECENT_TIMESTAMP));
      mRecentTrend = c.getFloat(c.getColumnIndexOrThrow(TimeSeriesRow.RECENT_TREND));
      mIncrement = c.getFloat(c.getColumnIndexOrThrow(TimeSeriesRow.INCREMENT));
      mGoal = c.getFloat(c.getColumnIndexOrThrow(TimeSeriesRow.GOAL));
      mAggregation = c.getString(c.getColumnIndexOrThrow(TimeSeriesRow.AGGREGATION));
      mType = c.getString(c.getColumnIndexOrThrow(TimeSeriesRow.TYPE));
      mColor = c.getString(c.getColumnIndexOrThrow(TimeSeriesRow.COLOR));
      mPeriod = c.getLong(c.getColumnIndexOrThrow(TimeSeriesRow.PERIOD));
      mRank = c.getInt(c.getColumnIndexOrThrow(TimeSeriesRow.RANK));
      mTrendState = c.getString(c.getColumnIndexOrThrow(TimeSeriesRow.TREND_STATE));
      mInterpolation = c.getString(c.getColumnIndexOrThrow(TimeSeriesRow.INTERPOLATION));
      setZeroFill(c.getInt(c.getColumnIndexOrThrow(TimeSeriesRow.ZEROFILL)));
      mFormula = c.getString(c.getColumnIndexOrThrow(TimeSeriesRow.FORMULA));
      return;
    }

    public long getId() {
      return mId;
    }

    public void setId(long id) {
      mId = id;
    }

    public String getCategoryName() {
      return mCategoryName;
    }

    public void setCategoryName(String categoryName) {
      mCategoryName = categoryName;
    }

    public String getGroupName() {
      return mGroupName;
    }

    public void setGroupName(String groupName) {
      mGroupName = groupName;
    }

    public float getDefaultValue() {
      return mDefaultValue;
    }

    public void setDefaultValue(float defaultValue) {
      mDefaultValue = defaultValue;
    }

    public float getRecentValue() {
      return mRecentValue;
    }

    public void setRecentValue(float recentValue) {
      mRecentValue = recentValue;
    }

    public long getRecentTimestamp() {
      return mRecentTimestamp;
    }

    public void setRecentTimestamp(long recentTimestamp) {
      mRecentTimestamp = recentTimestamp;
    }

    public float getRecentTrend() {
      return mRecentTrend;
    }

    public void setRecentTrend(float recentTrend) {
      mRecentTrend = recentTrend;
    }

    public float getIncrement() {
      return mIncrement;
    }

    public void setIncrement(float increment) {
      mIncrement = increment;
    }

    public float getGoal() {
      return mGoal;
    }

    public void setGoal(float goal) {
      mGoal = goal;
    }

    public String getAggregation() {
      return mAggregation;
    }

    public void setAggregation(String aggregation) {
      mAggregation = aggregation;
    }

    public String getColor() {
      return mColor;
    }

    public void setColor(String color) {
      mColor = color;
    }

    public long getPeriod() {
      return mPeriod;
    }

    public void setPeriod(long period) {
      mPeriod = period;
    }

    public int getRank() {
      return mRank;
    }

    public void setRank(int rank) {
      mRank = rank;
    }

    public String getTrendState() {
      return mTrendState;
    }

    public void setTrendState(String trendState) {
      mTrendState = trendState;
    }

    public String getInterpolation() {
      return mInterpolation;
    }

    public void setInterpolation(String interp) {
      mInterpolation = interp;
    }

    public boolean getZeroFill() {
      return mZeroFill;
    }

    public void setZeroFill(boolean fill) {
      mZeroFill = fill;
    }

    public void setZeroFill(int fill) {
      if (fill == 0)
        mZeroFill = false;
      else
        mZeroFill = true;
    }

    public String getType() {
      return mType;
    }

    public void setType(String type) {
      mType = type;
    }

    public String getFormula() {
      return mFormula;
    }

    public void setFormula(String formula) {
      mFormula = formula;
    }
  }
}
