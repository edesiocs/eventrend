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

package net.redgeek.android.eventrecorder;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

public class TimeSeriesData {
  public static final String AUTHORITY = "net.redgeek.android.eventrecorder";
  public static final String DATABASE_NAME = "timeseries.db";
  
  public static final class Datapoint implements BaseColumns {
    private Datapoint() {
    }

    /**
     * The content:// style URL for this table
     */
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
        + "/datapoint");

    /**
     * The MIME type of {@link #CONTENT_URI} providing a directory of
     * datapoints.
     */
    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.redgeek.timeseries.datapoint";

    /**
     * The MIME type of a {@link #CONTENT_URI} sub-directory of a single
     * datapoint
     */
    public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.redgeek.timeseries.datapoint";

    /**
     * The name of the sql table this data resides in
     */
    public static final String TABLE_NAME = "datapoint";

    /**
     * The default sort order for this table
     */
    public static final String DEFAULT_SORT_ORDER = "ts_end ASC";

    /**
     * The _id of the datapoint
     * <p>
     * Type: LONG
     * </p>
     */
    public static final String _ID = "_id";

    /**
     * The _id of the timeseries this timestamp belongs to
     * <p>
     * Type: LONG
     * </p>
     */
    public static final String TIMESERIES_ID = "timeseries_id";

    /**
     * The value of the datapoint (duration in seconds for ranges)
     * <p>
     * Type: DOUBLE
     * </p>
     */
    public static final String VALUE = "value";

    /**
     * The number of entries that comprise the datapoint
     * <p>
     * Type: DOUBLE
     * </p>
     */
    public static final String ENTRIES = "entries";

    /**
     * The start timestamp of the datapoint
     * <p>
     * Type: INTEGER, seconds since epoch (System.currentTimeInMillis() / 1000)
     * </p>
     */
    public static final String TS_START = "ts_start";

    /**
     * The end timestamp of the datapoint (== ts_start for discrete events)
     * <p>
     * Type: INTEGER, seconds since epoch (System.currentTimeInMillis() / 1000)
     * </p>
     */
    public static final String TS_END = "ts_end";

    /**
     * The trend value of the datapoint (based on sensitivity and smoothing)
     * at that point in time.
     * <p>
     * Type: DOUBLE
     * </p>
     */
    public static final String TREND = "trend";

    /**
     * The sum of squares of the datapoint values at that time.
     * <p>
     * Type: DOUBLE
     * </p>
     */
    public static final String STDDEV = "stddev";

    public static final String SUM_ENTRIES = "sum_entries";
    public static final String SUM_VALUE = "sum_value";
    public static final String SUM_VALUE_SQR = "sum_value_sqr";
    
    public static final String[] AGGREGATE_SUFFIX = {
      "day",
      "week",
      "month",
      "quarter",
      "year",
    };
    public static final long[] AGGREGATE_TABLE_PERIOD = {
      DateMapCache.DAY_MS / DateMapCache.SECOND_MS,
      DateMapCache.WEEK_MS / DateMapCache.SECOND_MS,
      DateMapCache.MONTH_MS / DateMapCache.SECOND_MS,
      DateMapCache.QUARTER_MS / DateMapCache.SECOND_MS,
      DateMapCache.YEAR_MS / DateMapCache.SECOND_MS,
    };

    /**
     * The table creation sql
     * We create tables for each aggregation level.
     */
    
    /* for raw (un-aggregated) data:
      -- value => instantaneous value
      -- trend => trend[n-1] + (smoothing * ((value[n-1] / entries) - trend[n-1])) || value
      -- sumsqr => sumsqr[n-1] + value^2 || value^2
      -- stddev => sqrt(sumsqr / entries - (mean)^2) || 0
       for aggregated data
      -- value, entries, trend, stddev are instantaneous
  -- sum_* are monotonically increasing
  -- value => value[n-1] + value || value
  -- entries => entries[n-1] + 1 || 1
  -- entries => entries[n-1] + 1 || 1
  -- sumsqr => sumsqr[n-1] + row.value^2 || row.value^2
  -- trend => trend[n-1] + (smoothing * ((value[n-1] / entries) - trend[n-1])) || avg(trend)
  -- stddev => sqrt(period.sumsqr / count(value) - (mean)^2)
    */
    public static final String TABLE_CREATE = "create table "
        + TABLE_NAME + " ( "
        + Datapoint._ID + " integer primary key autoincrement, "
        + Datapoint.TIMESERIES_ID + " integer key not null, "
        + Datapoint.TS_START + " integer key not null, "
        + Datapoint.TS_END + " integer key not null default 0, "
        + Datapoint.VALUE + " double not null default 0.0, "
        + Datapoint.ENTRIES + " integer not null default 0, "
        + Datapoint.TREND + " double not null default 0.0, "
        + Datapoint.STDDEV + " double not null default 0.0, "
        + Datapoint.SUM_ENTRIES + " integer not null default 0, "
        + Datapoint.SUM_VALUE + " double not null default 0.0, "
        + Datapoint.SUM_VALUE_SQR + " double not null default 0.0, "

        + Datapoint.TS_START + "_" + AGGREGATE_SUFFIX[0] + " integer key not null default 0, "
        + Datapoint.TS_END + "_" + AGGREGATE_SUFFIX[0] + " integer key not null default 0, "
        + Datapoint.VALUE + "_" + AGGREGATE_SUFFIX[0] + " double not null default 0.0, "
        + Datapoint.ENTRIES + "_" + AGGREGATE_SUFFIX[0] + " integer not null default 0.0, "
        + Datapoint.TREND + "_" + AGGREGATE_SUFFIX[0] + " double not null default 0.0, "
        + Datapoint.STDDEV + "_" + AGGREGATE_SUFFIX[0] + " double not null default 0.0, "
        + Datapoint.SUM_ENTRIES + "_" + AGGREGATE_SUFFIX[0] + " integer not null default 0.0, "
        + Datapoint.SUM_VALUE + "_" + AGGREGATE_SUFFIX[0] + " double not null default 0.0, "
        + Datapoint.SUM_VALUE_SQR + "_" + AGGREGATE_SUFFIX[0] + " double not null default 0.0, "

        + Datapoint.TS_START + "_" + AGGREGATE_SUFFIX[1] + " integer key not null default 0, "
        + Datapoint.TS_END + "_" + AGGREGATE_SUFFIX[1] + " integer key not null default 0, "
        + Datapoint.VALUE + "_" + AGGREGATE_SUFFIX[1] + " double not null default 0.0, "
        + Datapoint.ENTRIES + "_" + AGGREGATE_SUFFIX[1] + " integer not null default 0.0, "
        + Datapoint.TREND + "_" + AGGREGATE_SUFFIX[1] + " double not null default 0.0, "
        + Datapoint.STDDEV + "_" + AGGREGATE_SUFFIX[1] + " double not null default 0.0, "
        + Datapoint.SUM_ENTRIES + "_" + AGGREGATE_SUFFIX[1] + " integer not null default 0.0, "
        + Datapoint.SUM_VALUE + "_" + AGGREGATE_SUFFIX[1] + " double not null default 0.0, "
        + Datapoint.SUM_VALUE_SQR + "_" + AGGREGATE_SUFFIX[1] + " double not null default 0.0, "

        + Datapoint.TS_START + "_" + AGGREGATE_SUFFIX[2] + " integer key not null default 0, "
        + Datapoint.TS_END + "_" + AGGREGATE_SUFFIX[2] + " integer key not null default 0, "
        + Datapoint.VALUE + "_" + AGGREGATE_SUFFIX[2] + " double not null default 0.0, "
        + Datapoint.ENTRIES + "_" + AGGREGATE_SUFFIX[2] + " integer not null default 0.0, "
        + Datapoint.TREND + "_" + AGGREGATE_SUFFIX[2] + " double not null default 0.0, "
        + Datapoint.STDDEV + "_" + AGGREGATE_SUFFIX[2] + " double not null default 0.0, "
        + Datapoint.SUM_ENTRIES + "_" + AGGREGATE_SUFFIX[2] + " integer not null default 0.0, "
        + Datapoint.SUM_VALUE + "_" + AGGREGATE_SUFFIX[2] + " double not null default 0.0, "
        + Datapoint.SUM_VALUE_SQR + "_" + AGGREGATE_SUFFIX[2] + " double not null default 0.0, "

        + Datapoint.TS_START + "_" + AGGREGATE_SUFFIX[3] + " integer key not null default 0, "
        + Datapoint.TS_END + "_" + AGGREGATE_SUFFIX[3] + " integer key not null default 0, "
        + Datapoint.VALUE + "_" + AGGREGATE_SUFFIX[3] + " double not null default 0.0, "
        + Datapoint.ENTRIES + "_" + AGGREGATE_SUFFIX[3] + " integer not null default 0.0, "
        + Datapoint.TREND + "_" + AGGREGATE_SUFFIX[3] + " double not null default 0.0, "
        + Datapoint.STDDEV + "_" + AGGREGATE_SUFFIX[3] + " double not null default 0.0, "
        + Datapoint.SUM_ENTRIES + "_" + AGGREGATE_SUFFIX[3] + " integer not null default 0.0, "
        + Datapoint.SUM_VALUE + "_" + AGGREGATE_SUFFIX[3] + " double not null default 0.0, "
        + Datapoint.SUM_VALUE_SQR + "_" + AGGREGATE_SUFFIX[3] + " double not null default 0.0, "

        + Datapoint.TS_START + "_" + AGGREGATE_SUFFIX[4] + " integer key not null default 0, "
        + Datapoint.TS_END + "_" + AGGREGATE_SUFFIX[4] + " integer key not null default 0, "
        + Datapoint.VALUE + "_" + AGGREGATE_SUFFIX[4] + " double not null default 0.0, "
        + Datapoint.ENTRIES + "_" + AGGREGATE_SUFFIX[4] + " integer not null default 0.0, "
        + Datapoint.TREND + "_" + AGGREGATE_SUFFIX[4] + " double not null default 0.0, "
        + Datapoint.STDDEV + "_" + AGGREGATE_SUFFIX[4] + " double not null default 0.0, "
        + Datapoint.SUM_ENTRIES + "_" + AGGREGATE_SUFFIX[4] + " integer not null default 0.0, "
        + Datapoint.SUM_VALUE + "_" + AGGREGATE_SUFFIX[4] + " double not null default 0.0, "
        + Datapoint.SUM_VALUE_SQR + "_" + AGGREGATE_SUFFIX[4] + " double not null default 0.0 "
        + ");";
        
    public static long getId(Cursor c) {
      return c.getLong(c.getColumnIndexOrThrow(_ID));
    }

    public static long getTimeSeriesId(Cursor c) {
      return c.getLong(c.getColumnIndexOrThrow(TIMESERIES_ID));
    }

    public static double getValue(Cursor c) {
      return c.getDouble(c.getColumnIndexOrThrow(VALUE));
    }

    public static double getValue(Cursor c, String suffix) {
      return c.getDouble(c.getColumnIndexOrThrow(VALUE + "_" + suffix));
    }

    public static int getEntries(Cursor c) {
      return c.getInt(c.getColumnIndexOrThrow(ENTRIES));
    }

    public static int getEntries(Cursor c, String suffix) {
      return c.getInt(c.getColumnIndexOrThrow(ENTRIES + "_" + suffix));
    }

    public static int getTsStart(Cursor c) {
      return c.getInt(c.getColumnIndexOrThrow(TS_START));
    }

    public static int getTsStart(Cursor c, String suffix) {
      return c.getInt(c.getColumnIndexOrThrow(TS_START + "_" + suffix));
    }

    public static int getTsEnd(Cursor c) {
      return c.getInt(c.getColumnIndexOrThrow(TS_END));
    }

    public static int getTsEnd(Cursor c, String suffix) {
      return c.getInt(c.getColumnIndexOrThrow(TS_END + "_" + suffix));
    }

    public static double getTrend(Cursor c) {
      return c.getDouble(c.getColumnIndexOrThrow(TREND));
    }

    public static double getTrend(Cursor c, String suffix) {
      return c.getDouble(c.getColumnIndexOrThrow(TREND + "_" + suffix));
    }

    public static double getStdDev(Cursor c) {
      return c.getDouble(c.getColumnIndexOrThrow(STDDEV));
    }

    public static double getStdDev(Cursor c, String suffix) {
      return c.getDouble(c.getColumnIndexOrThrow(STDDEV + "_" + suffix));
    }

    public static int getSumEntries(Cursor c) {
      return c.getInt(c.getColumnIndexOrThrow(SUM_ENTRIES));
    }

    public static int getSumEntries(Cursor c, String suffix) {
      return c.getInt(c.getColumnIndexOrThrow(SUM_ENTRIES + "_" + suffix));
    }

    public static double getSumValue(Cursor c) {
      return c.getDouble(c.getColumnIndexOrThrow(SUM_VALUE));
    }

    public static double getSumValue(Cursor c, String suffix) {
      return c.getDouble(c.getColumnIndexOrThrow(SUM_VALUE + "_" + suffix));
    }

    public static double getSumValueSqr(Cursor c) {
      return c.getDouble(c.getColumnIndexOrThrow(SUM_VALUE_SQR));
    }

    public static double getSumValueSqr(Cursor c, String suffix) {
      return c.getDouble(c.getColumnIndexOrThrow(SUM_VALUE_SQR + "_" + suffix));
    }

    public static void setId(ContentValues cv, long id) {
      cv.put(_ID, id);
    }

    public static void setTimeSeriesId(ContentValues cv, long timeSeriesId) {
      cv.put(TIMESERIES_ID, timeSeriesId);
    }

    public static void setValue(ContentValues cv, double value) {
      cv.put(VALUE, value);
    }

    public static void setValue(ContentValues cv, String suffix, double value) {
      cv.put(VALUE + "_" + suffix, value);
    }

    public static void setEntries(ContentValues cv, int entries) {
      cv.put(ENTRIES, entries);
    }

    public static void setEntries(ContentValues cv, String suffix, int entries) {
      cv.put(ENTRIES + "_" + suffix, entries);
    }

    public static void setTsStart(ContentValues cv, int tsStart) {
      cv.put(TS_START, tsStart);
    }

    public static void setTsStart(ContentValues cv, String suffix, int tsStart) {
      cv.put(TS_START + "_" + suffix, tsStart);
    }

    public static void setTsEnd(ContentValues cv, int tsEnd) {
      cv.put(TS_END, tsEnd);
    }

    public static void setTsEnd(ContentValues cv, String suffix, int tsEnd) {
      cv.put(TS_END + "_" + suffix, tsEnd);
    }

    public static void setTrend(ContentValues cv, double trend) {
      cv.put(TREND, trend);
    }

    public static void setTrend(ContentValues cv, String suffix, double trend) {
      cv.put(TREND + "_" + suffix, trend);
    }

    public static void setStdDev(ContentValues cv, double sumSqr) {
      cv.put(STDDEV, sumSqr);
    }

    public static void setStdDev(ContentValues cv, String suffix, double sumSqr) {
      cv.put(STDDEV + "_" + suffix, sumSqr);
    }

    public static void setSumEntries(ContentValues cv, int entries) {
      cv.put(SUM_ENTRIES, entries);
    }

    public static void setSumEntries(ContentValues cv, String suffix, int entries) {
      cv.put(SUM_ENTRIES + "_" + suffix, entries);
    }
    
    public static void setSumValue(ContentValues cv, double sumValue) {
      cv.put(SUM_VALUE, sumValue);
    }

    public static void setSumValue(ContentValues cv, String suffix, double sumValue) {
      cv.put(SUM_VALUE + "_" + suffix, sumValue);
    }
    
    public static void setSumValueSqr(ContentValues cv, double sumValueSqr) {
      cv.put(SUM_VALUE_SQR, sumValueSqr);
    }

    public static void setSumValueSqr(ContentValues cv, String suffix, double sumValueSqr) {
      cv.put(SUM_VALUE_SQR + "_" + suffix, sumValueSqr);
    }
  }

  public static final class TimeSeries implements BaseColumns {
    private TimeSeries() {
    }

    /**
     * The content:// style URL for this table
     */
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
        + "/timeseries");

    /**
     * The MIME type of {@link #CONTENT_URI} providing a directory of
     * categories.
     */
    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.redgeek.timeseries";

    /**
     * The MIME type of a {@link #CONTENT_URI} sub-directory of a single
     * category
     */
    public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.redgeek.timeseries";

    /**
     * The name of the sql table this data resides in
     */
    public static final String TABLE_NAME = "timeseries";

    /**
     * The default sort order for this table
     */
    public static final String DEFAULT_SORT_ORDER = "rank ASC";

    /**
     * The _id of the timeseries
     * <p>
     * Type: LONG
     * </p>
     */
    public static final String _ID = "_id";

    /**
     * The name of the category.
     * <p>
     * Type: STRING
     * </p>
     */
    public static final String TIMESERIES_NAME = "timeseries_name";

    /**
     * The _id of a datapoint in the process of recording, else 0
     * <p>
     * Type: LONG
     * </p>
     */
    public static final String RECORDING_DATAPOINT_ID = "recording_datapoint_id";

    /**
     * The name of the group it belongs to
     * <p>
     * Type: STRING
     * </p>
     */
    public static final String GROUP_NAME = "group_name";

    /**
     * The default value for data input
     * <p>
     * Type: DOUBLE
     * </p>
     */
    public static final String DEFAULT_VALUE = "default_value";

    /**
     * The increment/decrement for input
     * <p>
     * Type: DOUBLE
     * </p>
     */
    public static final String INCREMENT = "increment";

    /**
     * The goal value for data input
     * <p>
     * Type: DOUBLE
     * </p>
     */
    public static final String GOAL = "goal";

    /**
     * The color associated with the series
     * <p>
     * Type: STRING, in the form "#rrggbb" (no quotes)
     * </p>
     */
    public static final String COLOR = "color";

    /**
     * The aggregation period of the series in seconds
     * <p>
     * Type: INTEGER
     * </p>
     */
    public static final String PERIOD = "period";

    /**
     * The units being measure
     * <p>
     * Type: STRING
     * </p>
     */
    public static final String UNITS = "units";

    /**
     * The rank of the series (for ordering)
     * <p>
     * Type: INTEGER
     * </p>
     */
    public static final String RANK = "rank";

    /**
     * How to aggregate data within a given period ('sum' or 'average')
     * <p>
     * Type: STRING, 'sum' or 'average'
     * </p>
     */
    public static final String AGGREGATION = "aggregation";
    public static final String AGGREGATION_SUM = "sum";
    public static final String AGGREGATION_AVG = "average";
    public static final String[] AGGREGATIONS = {
      AGGREGATION_SUM, AGGREGATION_SUM
    };

    /**
     * The type of the series: discrete: represents a single point in time,
     * ts_start == ts_end range: represents a range of time, ts_end >= ts_start
     * calculated: based on the input of other timeseries
     * <p>
     * Type: STRING, 'discrete', 'range', 'synthetic'
     * </p>
     */
    public static final String TYPE = "type";
    public static final String TYPE_DISCRETE = "discrete";
    public static final String TYPE_RANGE = "range";
    public static final String TYPE_SYNTHETIC = "synthetic";
    public static final String[] TYPES = {
      TYPE_DISCRETE, TYPE_RANGE, TYPE_SYNTHETIC
    };

    /**
     * If aggregation periods without entries should have an entry automatically
     * inserted.
     * <p>
     * Type: INTEGER, 0 or 1
     * </p>
     */
    public static final String ZEROFILL = "zerofill";

    /**
     * If type == 'synthetic', this is the formula defining the calculation to
     * perform.
     * <p>
     * Type: STRING
     * </p>
     */
    public static final String FORMULA = "formula";

    /**
     * How to interpolate data between points.
     * <p>
     * Type: STRING, full class name of interpolator class
     * </p>
     */
    public static final String INTERPOLATION = "interpolation";

    /**
     * The sensitivity of the timeseries.
     * <p>
     * Type: DOUBLE
     * </p>
     */
    public static final String SENSITIVITY = "sensitivity";

    /**
     * The smoothing constant of the timeseries.
     * <p>
     * Type: DOUBLE
     * </p>
     */
    public static final String SMOOTHING = "smoothing";

    /**
     * The number of datapoints to take into account when calculating trend.
     * <p>
     * Type: INTEGER
     * </p>
     */
    public static final String HISTORY = "history";

    /**
     * The number of decimals points to round to.
     * <p>
     * Type: INTEGER
     * </p>
     */
    public static final String DECIMALS = "decimals";

    /**
     * The table creation sql
     */
    public static final String TABLE_CREATE = "create table " 
        + TABLE_NAME + " (" 
        + _ID + " integer primary key autoincrement, " 
        + TIMESERIES_NAME + " text not null default '', " 
        + RECORDING_DATAPOINT_ID + " integer not null default 0, "
        + GROUP_NAME + " text default '', " 
        + DEFAULT_VALUE + " double not null default 1.0, "
        + INCREMENT + " double not null default 1.0, " 
        + GOAL + " double not null default 0.0, " 
        + COLOR + " text not null default '#cccccc', " 
        + PERIOD + " integer not null default 0, " 
        + RANK + " integer not null, " 
        + AGGREGATION + " text not null default 'sum', " 
        + TYPE + " string not null default 'discrete', " 
        + ZEROFILL + " integer not null default 0, " 
        + FORMULA + " text not null default '', " 
        + INTERPOLATION + " text not null default '', " 
        + UNITS + " text not null default '', "
        + SENSITIVITY + " double not null default 0.5, " 
        + SMOOTHING + " double not null default 0.1, "
        + HISTORY + " integer not null default 20, " 
        + DECIMALS + " integer not null default 2" 
        + ");";
    
    public static long getId(Cursor c) {
      return c.getLong(c.getColumnIndexOrThrow(_ID));
    }
    
    public static String getTimeSeriesName(Cursor c) {
      return c.getString(c.getColumnIndexOrThrow(TIMESERIES_NAME));
    }

    public static long getRecordingDatapointId(Cursor c) {
      return c.getLong(c.getColumnIndexOrThrow(RECORDING_DATAPOINT_ID));
    }

    public static String getGroupName(Cursor c) {
      return c.getString(c.getColumnIndexOrThrow(GROUP_NAME));
    }

    public static double getDefaultValue(Cursor c) {
      return c.getDouble(c.getColumnIndexOrThrow(DEFAULT_VALUE));
    }
    
    public static double getIncrement(Cursor c) {
      return c.getDouble(c.getColumnIndexOrThrow(INCREMENT));
    }
    
    public static double getGoal(Cursor c) {
      return c.getDouble(c.getColumnIndexOrThrow(GOAL));
    }
    
    public static String getColor(Cursor c) {
      return c.getString(c.getColumnIndexOrThrow(COLOR));
    }
    
    public static int getPeriod(Cursor c) {
      return c.getInt(c.getColumnIndexOrThrow(PERIOD));
    }
    
    public static String getUnits(Cursor c) {
      return c.getString(c.getColumnIndexOrThrow(UNITS));
    }
    
    public static int getRank(Cursor c) {
      return c.getInt(c.getColumnIndexOrThrow(RANK));
    }
    
    public static String getAggregation(Cursor c) {
      return c.getString(c.getColumnIndexOrThrow(AGGREGATION));
    }
    
    public static String getType(Cursor c) {
      return c.getString(c.getColumnIndexOrThrow(TYPE));
    }
    
    public static int getZerofill(Cursor c) {
      return c.getInt(c.getColumnIndexOrThrow(ZEROFILL));
    }
    
    public static String getFormula(Cursor c) {
      return c.getString(c.getColumnIndexOrThrow(FORMULA));
    }
    
    public static String getInterpolation(Cursor c) {
      return c.getString(c.getColumnIndexOrThrow(INTERPOLATION));
    }

    public static double getSensitivity(Cursor c) {
      return c.getDouble(c.getColumnIndexOrThrow(SENSITIVITY));
    }

    public static double getSmoothing(Cursor c) {
      return c.getDouble(c.getColumnIndexOrThrow(SMOOTHING));
    }

    public static int getHistory(Cursor c) {
      return c.getInt(c.getColumnIndexOrThrow(HISTORY));
    }

    public static int getDecimals(Cursor c) {
      return c.getInt(c.getColumnIndexOrThrow(DECIMALS));
    }
    
    public static final int[] AGGREGATION_PERIOD_TIMES = {
      0,    
      (int) (DateMapCache.DAY_MS / DateMapCache.SECOND_MS),    
      (int) (DateMapCache.WEEK_MS / DateMapCache.SECOND_MS),    
      (int) (DateMapCache.MONTH_MS / DateMapCache.SECOND_MS),    
      (int) (DateMapCache.QUARTER_MS / DateMapCache.SECOND_MS),    
      (int) (DateMapCache.YEAR_MS / DateMapCache.SECOND_MS),    
    };

    public static final String[] AGGREGATION_PERIOD_NAMES = {
      "None",
      "Day",
      "Week",
      "Month",
      "Quarter",
      "Year",
    };  
    
    public static int periodToIndex(int period) {
      for (int i = 0; i < AGGREGATION_PERIOD_TIMES.length; i++) {
        if (AGGREGATION_PERIOD_TIMES[i] == period)
          return i;
      }
      return -1;
    }

    public static String periodToUriAggregation(int period) {
      if (period == 0)
        return null;
      for (int i = 1; i < AGGREGATION_PERIOD_TIMES.length; i++) {
        if (AGGREGATION_PERIOD_TIMES[i] == period) {
          return Datapoint.AGGREGATE_SUFFIX[i-1];
        }
      }
      return null;
    }
  }
  
  public static final class DateMap implements BaseColumns {
    private DateMap() {
    }

    /**
     * The content:// style URL for this table
     */
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
        + "/datemap");

    /**
     * The MIME type of {@link #CONTENT_URI} providing a directory of
     * datapoints.
     */
    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.redgeek.datemap";

    /**
     * The MIME type of a {@link #CONTENT_URI} sub-directory of a single
     * datapoint
     */
    public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.redgeek.datemap";

    /**
     * The name of the sql table this data resides in
     */
    public static final String TABLE_NAME = "datemap";

    /**
     * The default sort order for this table
     */
    public static final String DEFAULT_SORT_ORDER = "seconds ASC";

    /**
     * The _id of the datapoint
     * <p>
     * Type: LONG
     * </p>
     */
    public static final String _ID = "_id";

    /**
     * The year portion of this milliseconds-since-epoch mapping
     * <p>
     * Type: INTEGER
     * </p>
     */
    public static final String YEAR = "year";

    /**
     * The month portion of this milliseconds-since-epoch mapping
     * <p>
     * Type: BYTE
     * </p>
     */
    public static final String MONTH = "month";

    /**
     * The dow of week of this milliseconds-since-epoch mapping
     * <p>
     * Type: BYTE
     * </p>
     */
    public static final String DOW = "dow";

    /**
     * The time in seconds since epoch for the start of year and month
     * specified.
     * <p>
     * Type: INTEGER, seconds since epoch (System.currentTimeInMillis() / 1000)
     * </p>
     */
    public static final String SECONDS = "seconds";

    /**
     * The table creation sql
     */
    public static final String TABLE_CREATE = "create table " + TABLE_NAME
        + " (" + _ID + " integer primary key autoincrement, " 
        + YEAR + " integer key not null, " 
        + MONTH + " byte key not null, "
        + DOW + " byte key not null, " 
        + SECONDS + " integer key not null);";
    
    public static long getId(Cursor c) {
      return c.getLong(c.getColumnIndexOrThrow(_ID));
    }

    public static int getYear(Cursor c) {
      return c.getInt(c.getColumnIndexOrThrow(YEAR));
    }

    public static int getMonth(Cursor c) {
      return c.getInt(c.getColumnIndexOrThrow(MONTH));
    }

    public static int getDOW(Cursor c) {
      return c.getInt(c.getColumnIndexOrThrow(DOW));
    }

    public static int getSeconds(Cursor c) {
      return c.getInt(c.getColumnIndexOrThrow(SECONDS));
    }
  }
  
  public static final class FormulaCache implements BaseColumns {
    private FormulaCache() {
    }

    /**
     * The name of the sql table this data resides in
     */
    public static final String TABLE_NAME = "formula";

    /**
     * The default sort order for this table
     */
    public static final String DEFAULT_SORT_ORDER = "result_series ASC";

    /**
     * The _id of the formula cache entry
     * <p>
     * Type: LONG
     * </p>
     */
    public static final String _ID = "_id";

    /**
     * The year portion of this milliseconds-since-epoch mapping
     * <p>
     * Type: INTEGER
     * </p>
     */
    public static final String RESULT_SERIES = "result_series";

    /**
     * The month portion of this milliseconds-since-epoch mapping
     * <p>
     * Type: INTEGER
     * </p>
     */
    public static final String SOURCE_SERIES = "source_series";

    /**
     * The table creation sql
     */
    public static final String TABLE_CREATE = "create table " + TABLE_NAME
        + " (" + _ID + " integer primary key autoincrement, " 
        + RESULT_SERIES + " integer key not null, " 
        + SOURCE_SERIES + " integer key not null);";
    
    public static long getId(Cursor c) {
      return c.getLong(c.getColumnIndexOrThrow(_ID));
    }

    public static int getResultSeries(Cursor c) {
      return c.getInt(c.getColumnIndexOrThrow(RESULT_SERIES));
    }

    public static int getSourceSeries(Cursor c) {
      return c.getInt(c.getColumnIndexOrThrow(SOURCE_SERIES));
    }
  }
}
