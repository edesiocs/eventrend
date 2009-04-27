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
     * The value of the datapoint (duration in seconds for ranges)
     * <p>
     * Type: FLOAT
     * </p>
     */
    public static final String VALUE = "value";

    /**
     * The number of entries that comprise the datapoint
     * <p>
     * Type: FLOAT
     * </p>
     */
    public static final String ENTRIES = "entries";

    /**
     * The trend value of the datapoint (based on sensitivity and smoothing)
     * at that point in time.
     * <p>
     * Type: FLOAT
     * </p>
     */
    public static final String TREND = "trend";

    /**
     * The standard deviation of the datapoint values at that time.
     * <p>
     * Type: FLOAT
     * </p>
     */
    public static final String STDDEV = "stddev";

    /**
     * The table creation sql
     * We create tables for each aggregation level.
     */
    public static final String TABLE_CONTENTS = " (" + _ID
        + " integer primary key autoincrement, " + TIMESERIES_ID
        + " integer key not null, " + TS_START + " integer key not null, "
        + TS_END + " integer key not null, " + VALUE + " float not null, "
        + ENTRIES + " integer not null, " + STDDEV + " float not null, "
        + TREND + " float not null" + ");";
    
    public static final String TABLE_CREATE = "create table " 
        + TABLE_NAME + TABLE_CONTENTS;
    
    public static final String[] TRIGGERS = {
      "create trigger trend_insert after insert on datapoint begin " +
      "  delete from stats where timeseries_id = NEW.timeseries_id; " +
      "  insert into stats " +
      "    select * from datapoint where timeseries_id = NEW.timeseries_id and ts_start <= NEW.ts_start " +
      "    order by ts_start desc limit " +
      "      (select history from timeseries where _id == NEW.timeseries_id); " +
      "  update datapoint set variance = ( " +
      "      select ((sum(value * value) - (sum(value) * avg(value))) / count(value)) from stats " +
      "      where timeseries_id = NEW.timeseries_id and ts_start <= NEW.ts_start) " +
      "    where datapoint._id == NEW._id; " +
      "  delete from stats where timeseries_id = NEW.timeseries_id; " +
      "  insert into stats " +
      "    select * from datapoint where ts_start <= NEW.ts_start order by ts_start desc limit 2; " +
      "  update datapoint set trend = " +
      "       (select case count(trend) " +
      "         when 1 then (select value from stats where timeseries_id = NEW.timeseries_id order by ts_start desc limit 1) " +
      "         else (select trend from stats where timeseries_id = NEW.timeseries_id order by ts_start asc limit 1) " +
      "               + " +
      "               ((select smoothing from timeseries where _id == NEW.timeseries_id) " +
      "                 * " +
      "               (NEW.value - (select trend from stats where timeseries_id = NEW.timeseries_id order by ts_start asc limit 1))) " +
      "         end " +
      "      ) where datapoint._id == NEW._id; " +
      "  end;",
      
      "create trigger trend_insert after insert on datapoint begin " +
      "  -- touch the variance field to trigger and update " +
      "  update datapoint set variance = 0 where datapoint._id = NEW._id; " +
      "end;",

      "create trigger trend_update after update on datapoint begin " +
      "  delete from stats where timeseries_id = NEW.timeseries_id; " +
      "  insert into stats " +
      "    select * from datapoint where timeseries_id = NEW.timeseries_id and ts_start <= NEW.ts_start " +
      "    order by ts_start desc limit " +
      "      (select history from timeseries where _id == NEW.timeseries_id); " +
      "  update datapoint set variance = ( " +
      "      select ((sum(value * value) - (sum(value) * avg(value))) / count(value)) from stats " +
      "      where timeseries_id = NEW.timeseries_id and ts_start <= NEW.ts_start) " +
      "    where datapoint._id == NEW._id; " +
      "  delete from stats where timeseries_id = NEW.timeseries_id; " +
      "  insert into stats " +
      "    select * from datapoint where ts_start <= NEW.ts_start order by ts_start desc limit 2; " +
      "  update datapoint set trend = " +
      "       (select case count(trend) " +
      "         when 1 then (select value from stats where timeseries_id = 1 order by ts_start desc limit 1) " +
      "         else (select trend from stats where timeseries_id = 1 order by ts_start asc limit 1) " +
      "               + " +
      "               ((select smoothing from timeseries where _id == NEW.timeseries_id) " +
      "                 * " +
      "               (NEW.value - (select trend from stats where timeseries_id = NEW.timeseries_id order by ts_start asc limit 1))) " +
      "         end " +
      "         from stats " +
      "      ) where datapoint._id == NEW._id; " +
      "  update datapoint set variance = 0 " +
      "    where timeseries_id = NEW.timeseries_id and ts_start > NEW.ts_start; " +
      "  end;",

      "create trigger trend_update_deps after update on datapoint begin " + 
      "  delete from stats where timeseries_id = NEW.timeseries_id; " + 
      "  insert into stats " + 
      "    select * from datapoint where timeseries_id = NEW.timeseries_id and ts_start <= NEW.ts_start " + 
      "    order by ts_start desc limit " + 
      "      (select history from timeseries where _id == NEW.timeseries_id); " + 
      "  update datapoint set variance = ( " + 
      "      select ((sum(value * value) - (sum(value) * avg(value))) / count(value)) from stats " + 
      "      where timeseries_id = NEW.timeseries_id and ts_start <= NEW.ts_start) " + 
      "    where datapoint._id == NEW._id; " + 
      "  delete from stats where timeseries_id = NEW.timeseries_id; " + 
      "  insert into stats " + 
      "    select * from datapoint where ts_start <= NEW.ts_start order by ts_start desc limit 2; " + 
      "  update datapoint set trend = " + 
      "       (select case count(trend) " + 
      "         when 1 then (select value from stats where timeseries_id = 1 order by ts_start desc limit 1) " + 
      "         else (select trend from stats where timeseries_id = 1 order by ts_start asc limit 1) " + 
      "               + " + 
      "               ((select smoothing from timeseries where _id == NEW.timeseries_id) " + 
      "                 * " + 
      "               (NEW.value - (select trend from stats where timeseries_id = NEW.timeseries_id order by ts_start asc limit 1))) " + 
      "         end " + 
      "         from stats " + 
      "      ) where datapoint._id == NEW._id; " + 
      "  end;",
    };

    public static final String[] AGGREGATE_TABLE_SUFFIX = {
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
    
//    public static final String TABLE_CREATE_STATS = "create table stats " + TABLE_CONTENTS;
    public static final String TABLE_CREATE_DAY = "create table " + TABLE_NAME
        + "_day" + TABLE_CONTENTS;
    public static final String TABLE_CREATE_WEEK = "create table " + TABLE_NAME
        + "_week" + TABLE_CONTENTS;
    public static final String TABLE_CREATE_MONTH = "create table "
        + TABLE_NAME + "_month" + TABLE_CONTENTS;
    public static final String TABLE_CREATE_QUARTER = "create table "
        + TABLE_NAME + "_quarter" + TABLE_CONTENTS;
    public static final String TABLE_CREATE_YEAR = "create table " + TABLE_NAME
        + "_year" + TABLE_CONTENTS;
    
    public static long getId(Cursor c) {
      return c.getLong(c.getColumnIndexOrThrow(_ID));
    }

    public static long getTimeSeriesId(Cursor c) {
      return c.getLong(c.getColumnIndexOrThrow(TIMESERIES_ID));
    }

    public static int getTsStart(Cursor c) {
      return c.getInt(c.getColumnIndexOrThrow(TS_START));
    }

    public static int getTsEnd(Cursor c) {
      return c.getInt(c.getColumnIndexOrThrow(TS_END));
    }

    public static float getValue(Cursor c) {
      return c.getFloat(c.getColumnIndexOrThrow(VALUE));
    }

    public static int getEntries(Cursor c) {
      return c.getInt(c.getColumnIndexOrThrow(ENTRIES));
    }

    public static float getTrend(Cursor c) {
      return c.getFloat(c.getColumnIndexOrThrow(TREND));
    }

    public static float getStdDev(Cursor c) {
      return c.getFloat(c.getColumnIndexOrThrow(STDDEV));
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
     * Type: FLOAT
     * </p>
     */
    public static final String DEFAULT_VALUE = "default_value";

    /**
     * The increment/decrement for input
     * <p>
     * Type: FLOAT
     * </p>
     */
    public static final String INCREMENT = "increment";

    /**
     * The goal value for data input
     * <p>
     * Type: FLOAT
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
     * Type: FLOAT
     * </p>
     */
    public static final String SENSITIVITY = "sensitivity";

    /**
     * The smoothing constant of the timeseries.
     * <p>
     * Type: FLOAT
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
    public static final String TABLE_CREATE = "create table " + TABLE_NAME
        + " (" + _ID + " integer primary key autoincrement, " + TIMESERIES_NAME
        + " text not null, " + RECORDING_DATAPOINT_ID + " integer not null, "
        + GROUP_NAME + " text, " + DEFAULT_VALUE + " float not null, "
        + INCREMENT + " float not null, " + GOAL + " float not null, " + COLOR
        + " text not null, " + PERIOD + " integer not null, " + RANK
        + " integer not null, " + AGGREGATION + " text not null, " + TYPE
        + " string not null, " + ZEROFILL + " integer not null, " + FORMULA
        + " text, " + INTERPOLATION + " text not null, " + UNITS + " text, "
        + SENSITIVITY + " float not null, " + SMOOTHING + " float not null, "
        + HISTORY + " integer not null, " + DECIMALS + " integer not null" + ");";
    
    public static final String[] TRIGGERS = {
      " create trigger series_delete after delete on timeseries begin " +
      "   delete from datapoint where datapoint.timeseries_id == OLD._id; " +
      " end;"
    };
    
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

    public static float getDefaultValue(Cursor c) {
      return c.getFloat(c.getColumnIndexOrThrow(DEFAULT_VALUE));
    }
    
    public static float getIncrement(Cursor c) {
      return c.getFloat(c.getColumnIndexOrThrow(INCREMENT));
    }
    
    public static float getGoal(Cursor c) {
      return c.getFloat(c.getColumnIndexOrThrow(GOAL));
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

    public static float getSensitivity(Cursor c) {
      return c.getFloat(c.getColumnIndexOrThrow(SENSITIVITY));
    }

    public static float getSmoothing(Cursor c) {
      return c.getFloat(c.getColumnIndexOrThrow(SMOOTHING));
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
          return Datapoint.AGGREGATE_TABLE_SUFFIX[i-1];
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
