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
  
  public static final class DateMap implements BaseColumns {
    private DateMap() {
    }

    /**
     * The content:// style URL for this table
     */
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
        + "/datamap");

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
    public static final String DEFAULT_SORT_ORDER = "ts_end ASC";

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
     * The time in milliseconds since epoch for the start of year and month
     * specified.
     * <p>
     * Type: LONG, milliseconds since epoch from System.currentTimeInMillis()
     * </p>
     */
    public static final String MILLISECONDS = "milliseconds";

    /**
     * The table creation sql
     */
    public static final String TABLE_CREATE = "create table " + TABLE_NAME
        + " (" + _ID + " integer primary key autoincrement, " 
        + YEAR + " integer key not null, " 
        + MONTH + " byte key not null, "
        + DOW + " byte key not null, " 
        + MILLISECONDS + " long key not null);";
    
    public static long getId(Cursor c) {
      return c.getLong(c.getColumnIndexOrThrow(TimeSeriesData.DateMap._ID));
    }

    public static int getYear(Cursor c) {
      return c.getInt(c.getColumnIndexOrThrow(TimeSeriesData.DateMap.YEAR));
    }

    public static int getMonth(Cursor c) {
      return c.getInt(c.getColumnIndexOrThrow(TimeSeriesData.DateMap.MONTH));
    }

    public static int getDOW(Cursor c) {
      return c.getInt(c.getColumnIndexOrThrow(TimeSeriesData.DateMap.DOW));
    }

    public static long getMilliseconds(Cursor c) {
      return c.getLong(c.getColumnIndexOrThrow(TimeSeriesData.DateMap.MILLISECONDS));
    }
  }
  
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
     * Type: LONG, milliseconds since epoch from System.currentTimeInMillis()
     * </p>
     */
    public static final String TS_START = "ts_start";

    /**
     * The end timestamp of the datapoint
     * <p>
     * Type: LONG, milliseconds since epoch from System.currentTimeInMillis()
     * </p>
     */
    public static final String TS_END = "ts_end";

    /**
     * The value of the datapoint
     * <p>
     * Type: FLOAT
     * </p>
     */
    public static final String VALUE = "value";

    /**
     * The number of updates to the datapoint
     * <p>
     * Type: INTEGER
     * </p>
     */
    public static final String UPDATES = "updates";

    /**
     * The table creation sql
     */
    public static final String TABLE_CREATE = "create table " + TABLE_NAME
        + " (" + _ID + " integer primary key autoincrement, " + TIMESERIES_ID
        + " integer key not null, " + TS_START + " long key not null, "
        + TS_END + " long key not null, " + VALUE + " float not null, "
        + UPDATES + " integer not null);";
    
    public static long getId(Cursor c) {
      return c.getLong(c.getColumnIndexOrThrow(TimeSeriesData.Datapoint._ID));
    }

    public static long getTimeSeriesId(Cursor c) {
      return c.getLong(c.getColumnIndexOrThrow(TimeSeriesData.Datapoint.TIMESERIES_ID));
    }

    public static long getTsStart(Cursor c) {
      return c.getLong(c.getColumnIndexOrThrow(TimeSeriesData.Datapoint.TS_START));
    }

    public static long getTsEnd(Cursor c) {
      return c.getLong(c.getColumnIndexOrThrow(TimeSeriesData.Datapoint.TS_END));
    }

    public static float getValue(Cursor c) {
      return c.getFloat(c.getColumnIndexOrThrow(TimeSeriesData.Datapoint.VALUE));
    }

    public static int getUpdates(Cursor c) {
      return c.getInt(c.getColumnIndexOrThrow(TimeSeriesData.Datapoint.UPDATES));
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
     * The aggregation period of the series in milliseconds
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

    /**
     * The type of the series: discrete: represents a single point in time,
     * ts_start == ts_end range: represents a range of time, ts_end >= ts_start
     * calculated: based on the input of other timeseries
     * <p>
     * Type: STRING, 'discrete', 'range', 'synthetic'
     * </p>
     */
    public static final String TYPE = "type";

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
        + " text, " + INTERPOLATION + " text not null, " + UNITS + " text);";
    
    public static long getId(Cursor c) {
      return c.getLong(c.getColumnIndexOrThrow(TimeSeriesData.TimeSeries._ID));
    }
    
    public static String getTimeSeriesName(Cursor c) {
      return c.getString(c.getColumnIndexOrThrow(TimeSeriesData.TimeSeries.TIMESERIES_NAME));
    }

    public static long getRecordingDatapointId(Cursor c) {
      return c.getLong(c.getColumnIndexOrThrow(TimeSeriesData.TimeSeries.RECORDING_DATAPOINT_ID));
    }

    public static String getGroupName(Cursor c) {
      return c.getString(c.getColumnIndexOrThrow(TimeSeriesData.TimeSeries.GROUP_NAME));
    }

    public static float getDefaultValue(Cursor c) {
      return c.getFloat(c.getColumnIndexOrThrow(TimeSeriesData.TimeSeries.DEFAULT_VALUE));
    }
    
    public static float getIncrement(Cursor c) {
      return c.getFloat(c.getColumnIndexOrThrow(TimeSeriesData.TimeSeries.INCREMENT));
    }
    
    public static float getGoal(Cursor c) {
      return c.getFloat(c.getColumnIndexOrThrow(TimeSeriesData.TimeSeries.GOAL));
    }
    
    public static String getColor(Cursor c) {
      return c.getString(c.getColumnIndexOrThrow(TimeSeriesData.TimeSeries.COLOR));
    }
    
    public static int getPeriod(Cursor c) {
      return c.getInt(c.getColumnIndexOrThrow(TimeSeriesData.TimeSeries.PERIOD));
    }
    
    public static String getUnits(Cursor c) {
      return c.getString(c.getColumnIndexOrThrow(TimeSeriesData.TimeSeries.UNITS));
    }
    
    public static int getRank(Cursor c) {
      return c.getInt(c.getColumnIndexOrThrow(TimeSeriesData.TimeSeries.RANK));
    }
    
    public static String getAggregation(Cursor c) {
      return c.getString(c.getColumnIndexOrThrow(TimeSeriesData.TimeSeries.AGGREGATION));
    }
    
    public static String getType(Cursor c) {
      return c.getString(c.getColumnIndexOrThrow(TimeSeriesData.TimeSeries.TYPE));
    }
    
    public static int getZerofill(Cursor c) {
      return c.getInt(c.getColumnIndexOrThrow(TimeSeriesData.TimeSeries.ZEROFILL));
    }
    
    public static String getFormula(Cursor c) {
      return c.getString(c.getColumnIndexOrThrow(TimeSeriesData.TimeSeries.FORMULA));
    }
    
    public static String getInterpolation(Cursor c) {
      return c.getString(c.getColumnIndexOrThrow(TimeSeriesData.TimeSeries.INTERPOLATION));
    }
  }
}
