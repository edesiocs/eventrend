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
  public static final String TABLE_NAME = "categories";

  public static final String KEY_ROWID = "_id";
  public static final String KEY_GROUP_NAME = "group_name";
  public static final String KEY_CATEGORY_NAME = "category_name";
  public static final String KEY_DEFAULT_VALUE = "default_value";
  public static final String KEY_LAST_VALUE = "last_value";
  public static final String KEY_LAST_TREND = "last_trend";
  public static final String KEY_INCREMENT = "increment";
  public static final String KEY_GOAL = "goal";
  public static final String KEY_COLOR = "color";
  public static final String KEY_TYPE = "type";
  public static final String KEY_PERIOD_MS = "period_in_ms";
  public static final String KEY_RANK = "rank";
  public static final String KEY_PERIOD_ENTRIES = "period_entries";
  public static final String KEY_TREND_STATE = "trend_state";
  public static final String KEY_INTERPOLATION = "interpolation";
  public static final String KEY_ZEROFILL = "zerofill";
  public static final String KEY_SYNTHETIC = "synthetic";
  public static final String KEY_FORMULA = "formula";

  public static final String KEY_TYPE_SUM = "Sum";
  public static final String KEY_TYPE_AVERAGE = "Average";

  public static final String KEY_INTERP_LINEAR = "Linear";
  public static final String KEY_INTERP_STEPEARLY = "StepEarly";
  public static final String KEY_INTERP_STEPMID = "StepMid";
  public static final String KEY_INTERP_STEPLATE = "StepLate";
  public static final String KEY_INTERP_CUBIC = "Cubic";

  // // public static final int KEY_INTERP_LINEAR_IDX = 0;
  // // public static final int KEY_INTERP_STEPEARLY_IDX = 1;
  // // public static final int KEY_INTERP_STEPMID_IDX = 2;
  // // public static final int KEY_INTERP_STEPLATE_IDX = 3;
  // // public static final int KEY_INTERP_CUBIC_IDX = 4;
  // // public static final String[] KEY_INTERPOLATION_STRINGS = {
  // // KEY_INTERP_LINEAR,
  // // KEY_INTERP_STEPEARLY,
  // // KEY_INTERP_STEPMID,
  // // KEY_INTERP_STEPLATE,
  // // KEY_INTERP_CUBIC,
  // // };
  // public static int mapInterpolationToIndex(String interp) {
  // for (int i = 0; i < KEY_INTERPOLATION_STRINGS.length; i++) {
  // if (interp.equals(KEY_INTERPOLATION_STRINGS[i]))
  // return i;
  // }
  // return 0;
  // }

  public static final String KEY_PERIOD_NONE = "None";
  public static final String KEY_PERIOD_HOUR = "Hour";
  public static final String KEY_PERIOD_AMPM = "AM/PM";
  public static final String KEY_PERIOD_DAY = "Day";
  public static final String KEY_PERIOD_WEEK = "Week";
  public static final String KEY_PERIOD_MONTH = "Month";
  public static final String KEY_PERIOD_QUARTER = "Quarter";
  public static final String KEY_PERIOD_YEAR = "Year";
  public static final String[] KEY_PERIODS = { KEY_PERIOD_NONE,
      KEY_PERIOD_HOUR, KEY_PERIOD_AMPM, KEY_PERIOD_DAY, KEY_PERIOD_WEEK,
      KEY_PERIOD_MONTH, KEY_PERIOD_QUARTER, KEY_PERIOD_YEAR };
  public static final long[] KEY_PERIODS_MS = { 0, DateUtil.HOUR_MS,
      DateUtil.AMPM_MS, DateUtil.DAY_MS, DateUtil.WEEK_MS, DateUtil.MONTH_MS,
      DateUtil.QUARTER_MS, DateUtil.YEAR_MS };
  // This is only used sometimes, so isn't in the regular list
  public static final long KEY_PERIOD_MS_AUTO = -1;

  public static final String KEY_TREND_DOWN_BAD = "trend_down_bad";
  public static final String KEY_TREND_DOWN_GOOD = "trend_down_good";
  public static final String KEY_TREND_DOWN_SLIGHT = "trend_down_slight";
  public static final String KEY_TREND_DOWN_SLIGHT_BAD = "trend_down_slight_bad";
  public static final String KEY_TREND_DOWN_SLIGHT_GOOD = "trend_down_slight_good";
  public static final String KEY_TREND_FLAT = "trend_flat";
  public static final String KEY_TREND_FLAT_GOAL = "trend_flat_goal";
  public static final String KEY_TREND_UNKNOWN = "trend_unknown";
  public static final String KEY_TREND_UP_BAD = "trend_up_bad";
  public static final String KEY_TREND_UP_GOOD = "trend_up_good";
  public static final String KEY_TREND_UP_SLIGHT = "trend_up_slight";
  public static final String KEY_TREND_UP_SLIGHT_BAD = "trend_up_slight_bad";
  public static final String KEY_TREND_UP_SLIGHT_GOOD = "trend_up_slight_good";

  public static final String KEY_STAR = TABLE_NAME + ".*";
  public static final String[] KEY_ALL = { KEY_ROWID, KEY_GROUP_NAME,
      KEY_CATEGORY_NAME, KEY_DEFAULT_VALUE, KEY_LAST_VALUE, KEY_LAST_TREND,
      KEY_INCREMENT, KEY_GOAL, KEY_COLOR, KEY_TYPE, KEY_PERIOD_MS, KEY_RANK,
      KEY_PERIOD_ENTRIES, KEY_TREND_STATE, KEY_INTERPOLATION, KEY_ZEROFILL,
      KEY_SYNTHETIC, KEY_FORMULA };

  public static final String[] EXPORTABLE = { KEY_GROUP_NAME,
      KEY_CATEGORY_NAME, KEY_DEFAULT_VALUE, KEY_LAST_TREND, KEY_INCREMENT,
      KEY_GOAL, KEY_COLOR, KEY_TYPE, KEY_PERIOD_MS, KEY_RANK,
      KEY_INTERPOLATION, KEY_ZEROFILL, KEY_SYNTHETIC, KEY_FORMULA };

  public static final String TABLE_CREATE = "create table " + TABLE_NAME + " ("
      + KEY_ROWID + " integer primary key autoincrement, " + KEY_GROUP_NAME
      + " text, " + KEY_CATEGORY_NAME + " text not null, " + KEY_DEFAULT_VALUE
      + " float not null, " + KEY_LAST_VALUE + " float not null, "
      + KEY_LAST_TREND + " float not null, " + KEY_INCREMENT
      + " float not null, " + KEY_GOAL + " float not null, " + KEY_COLOR
      + " text not null, " + KEY_TYPE + " text not null, " + KEY_PERIOD_MS
      + " int not null, " + KEY_RANK + " integer not null, "
      + KEY_PERIOD_ENTRIES + " int not null, " + KEY_TREND_STATE
      + " text not null, " + KEY_INTERPOLATION + " text not null, "
      + KEY_ZEROFILL + " byte not null, " + KEY_SYNTHETIC + " byte not null, "
      + KEY_FORMULA + " text not null);";

  public static long mapPeriodToMs(String period) {
    for (int i = 0; i < KEY_PERIODS.length; i++) {
      if (period.equals(KEY_PERIODS[i]))
        return KEY_PERIODS_MS[i];
    }
    return KEY_PERIODS_MS[0];
  }

  public static String mapMsToPeriod(long ms) {
    for (int i = 0; i < KEY_PERIODS_MS.length; i++) {
      if (ms == KEY_PERIODS_MS[i])
        return KEY_PERIODS[i];
    }
    return KEY_PERIODS[0];
  }

  public static int mapMsToIndex(long ms) {
    for (int i = 0; i < KEY_PERIODS_MS.length; i++) {
      if (ms == KEY_PERIODS_MS[i])
        return i;
    }
    return 0;
  }

  public static long getId(Cursor c) {
    return c.getLong(c.getColumnIndexOrThrow(KEY_ROWID));
  }

  public static String getGroupName(Cursor c) {
    return c.getString(c.getColumnIndexOrThrow(KEY_GROUP_NAME));
  }

  public static String getCategoryName(Cursor c) {
    return c.getString(c.getColumnIndexOrThrow(KEY_CATEGORY_NAME));
  }

  public static float getDefaultValue(Cursor c) {
    return c.getFloat(c.getColumnIndexOrThrow(KEY_DEFAULT_VALUE));
  }

  public static float getLastValue(Cursor c) {
    return c.getFloat(c.getColumnIndexOrThrow(KEY_LAST_VALUE));
  }

  public static float getLastTrend(Cursor c) {
    return c.getFloat(c.getColumnIndexOrThrow(KEY_LAST_TREND));
  }

  public static float getIncrement(Cursor c) {
    return c.getFloat(c.getColumnIndexOrThrow(KEY_INCREMENT));
  }

  public static float getGoal(Cursor c) {
    return c.getFloat(c.getColumnIndexOrThrow(KEY_GOAL));
  }

  public static String getType(Cursor c) {
    return c.getString(c.getColumnIndexOrThrow(KEY_TYPE));
  }

  public static String getColor(Cursor c) {
    return c.getString(c.getColumnIndexOrThrow(KEY_COLOR));
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

  public static long getPeriodMs(Cursor c) {
    return c.getLong(c.getColumnIndexOrThrow(KEY_PERIOD_MS));
  }

  public static int getRank(Cursor c) {
    return c.getInt(c.getColumnIndexOrThrow(KEY_RANK));
  }

  public static int getPeriodEntries(Cursor c) {
    return c.getInt(c.getColumnIndexOrThrow(KEY_PERIOD_ENTRIES));
  }

  public static String getTrendState(Cursor c) {
    return c.getString(c.getColumnIndexOrThrow(KEY_TREND_STATE));
  }

  public static String getInterpolation(Cursor c) {
    return c.getString(c.getColumnIndexOrThrow(KEY_INTERPOLATION));
  }

  public static boolean getZeroFill(Cursor c) {
    return c.getInt(c.getColumnIndexOrThrow(KEY_ZEROFILL)) == 0 ? false : true;
  }

  public static boolean getSynthetic(Cursor c) {
    return c.getInt(c.getColumnIndexOrThrow(KEY_SYNTHETIC)) == 0 ? false : true;
  }

  public static String getFormula(Cursor c) {
    return c.getString(c.getColumnIndexOrThrow(KEY_FORMULA));
  }

  public static class Row {
    private long mId = 0;
    private String mGroupName = "";
    private String mCategoryName = "";
    private float mDefaultValue = 0.0f;
    private float mLastValue = 0.0f;
    private float mLastTrend = 0.0f;
    private float mIncrement = 0.0f;
    private float mGoal = 0.0f;
    private String mType = KEY_TYPE_SUM;
    private String mColor = "#000000";
    private long mPeriodMs = 0;
    private int mRank = 0;
    private int mPeriodEntries = 0;
    private String mTrendState = KEY_TREND_UNKNOWN;
    private String mInterpolation = KEY_INTERP_LINEAR;
    private boolean mZeroFill = false;
    private boolean mSynthetic = false;
    private String mFormula = "";

    public Row() {
    }

    public Row(Row r) {
      mId = r.mId;
      mGroupName = new String(r.mGroupName);
      mCategoryName = new String(r.mCategoryName);
      mDefaultValue = r.mDefaultValue;
      mLastValue = r.mLastValue;
      mLastTrend = r.mLastTrend;
      mIncrement = r.mIncrement;
      mGoal = r.mGoal;
      mType = new String(r.mType);
      mColor = new String(r.mColor);
      mPeriodMs = r.mPeriodMs;
      mRank = r.mRank;
      mPeriodEntries = r.mPeriodEntries;
      mTrendState = new String(r.mTrendState);
      mInterpolation = new String(r.mInterpolation);
      mZeroFill = r.mZeroFill;
      mSynthetic = r.mSynthetic;
      mFormula = new String(r.mFormula);
    }

    public Row(Cursor c) {
      populateFromCursor(c);
    }

    public void populateFromCursor(Cursor c) {
      if (c == null)
        return;

      mId = c.getLong(c.getColumnIndexOrThrow(KEY_ROWID));
      mGroupName = c.getString(c.getColumnIndexOrThrow(KEY_GROUP_NAME));
      mCategoryName = c.getString(c.getColumnIndexOrThrow(KEY_CATEGORY_NAME));
      mDefaultValue = c.getFloat(c.getColumnIndexOrThrow(KEY_DEFAULT_VALUE));
      mLastValue = c.getFloat(c.getColumnIndexOrThrow(KEY_LAST_VALUE));
      mLastTrend = c.getFloat(c.getColumnIndexOrThrow(KEY_LAST_TREND));
      mIncrement = c.getFloat(c.getColumnIndexOrThrow(KEY_INCREMENT));
      mGoal = c.getFloat(c.getColumnIndexOrThrow(KEY_GOAL));
      mType = c.getString(c.getColumnIndexOrThrow(KEY_TYPE));
      mColor = c.getString(c.getColumnIndexOrThrow(KEY_COLOR));
      mPeriodMs = c.getLong(c.getColumnIndexOrThrow(KEY_PERIOD_MS));
      mRank = c.getInt(c.getColumnIndexOrThrow(KEY_RANK));
      mPeriodEntries = c.getInt(c.getColumnIndexOrThrow(KEY_PERIOD_ENTRIES));
      mTrendState = c.getString(c.getColumnIndexOrThrow(KEY_TREND_STATE));
      mInterpolation = c.getString(c.getColumnIndexOrThrow(KEY_INTERPOLATION));
      setZeroFill(c.getInt(c.getColumnIndexOrThrow(KEY_ZEROFILL)));
      setSynthetic(c.getInt(c.getColumnIndexOrThrow(KEY_SYNTHETIC)));
      mFormula = c.getString(c.getColumnIndexOrThrow(KEY_FORMULA));
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

    public float getLastValue() {
      return mLastValue;
    }

    public void setLastValue(float lastValue) {
      mLastValue = lastValue;
    }

    public float getLastTrend() {
      return mLastTrend;
    }

    public void setLastTrend(float lastTrend) {
      mLastTrend = lastTrend;
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

    public String getType() {
      return mType;
    }

    public void setType(String type) {
      mType = type;
    }

    public String getColor() {
      return mColor;
    }

    public void setColor(String color) {
      mColor = color;
    }

    public long getPeriodMs() {
      return mPeriodMs;
    }

    public void setPeriodMs(long periodMs) {
      mPeriodMs = periodMs;
    }

    public int getRank() {
      return mRank;
    }

    public void setRank(int rank) {
      mRank = rank;
    }

    public int getPeriodEntries() {
      return mPeriodEntries;
    }

    public void setPeriodEntries(int periodEntries) {
      mPeriodEntries = periodEntries;
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

    public boolean getSynthetic() {
      return mSynthetic;
    }

    public void setSynthetic(boolean synthetic) {
      mSynthetic = synthetic;
    }

    public void setSynthetic(int synthetic) {
      if (synthetic == 0)
        mSynthetic = false;
      else
        mSynthetic = true;
    }

    public String getFormula() {
      return mFormula;
    }

    public void setFormula(String formula) {
      mFormula = formula;
    }
  }
}
