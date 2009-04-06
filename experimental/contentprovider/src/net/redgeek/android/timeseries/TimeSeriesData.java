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

package net.redgeek.android.timeseries;

import android.net.Uri;
import android.provider.BaseColumns;

public class TimeSeriesData {
  public static final String AUTHORITY = "net.redgeek.android.eventrend.timerseriesprovider";
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
    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.timeseries.datapoint";

    /**
     * The MIME type of a {@link #CONTENT_URI} sub-directory of a single
     * datapoint
     */
    public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.timeseries.datapoint";

    /**
     * The name of the sql table this data resides in
     */
    public static final String TABLE_NAME = "datapoint";
        
    /**
     * The default sort order for this table
     */
    public static final String DEFAULT_SORT_ORDER = "ts_end ASC";

    /**
     * The id of the datapoint
     * <p>Type: INTEGER</p>
     */
    public static final String _ID = "_id";

    /**
     * The _id of the category this timestamp belongs to
     * <p>Type: INTEGER</p>
     */
    public static final String CATEGORY_ID = "category_id";

    /**
     * The start timestamp of the datapoint
     * <p>Type: LONG, milliseconds since epoch from System.currentTimeInMillis()</p>
     */
    public static final String TS_START = "ts_start";

    /**
     * The end timestamp of the datapoint
     * <p>Type: LONG, milliseconds since epoch from System.currentTimeInMillis()</p>
     */
    public static final String TS_END = "ts_end";

    /**
     * The value of the timestamp
     * <p>Type: FLOAT</p>
     */
    public static final String VALUE = "value";

    /**
     * The number of updates to the datapoint
     * <p>Type: INTEGER</p>
     */
    public static final String UPDATES = "updates";
    
    /**
     * The table creation sql
     */
    public static final String TABLE_CREATE = "create table " + TABLE_NAME + " ("
      + _ID + " integer primary key autoincrement, " 
      + CATEGORY_ID + " integer key not null, " 
      + TS_START + " long key not null, "
      + TS_END + " long key not null, "
      + VALUE + " float not null, " 
      + UPDATES + " integer not null);";
  }
  
  public static final class Category implements BaseColumns {
    private Category() {
    }

    /**
     * The content:// style URL for this table
     */
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
        + "/category");

    /**
     * The MIME type of {@link #CONTENT_URI} providing a directory of
     * categories.
     */
    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.timeseries.timeseries";

    /**
     * The MIME type of a {@link #CONTENT_URI} sub-directory of a single
     * category
     */
    public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.timeseries.timeseries";

    /**
     * The name of the sql table this data resides in
     */
    public static final String TABLE_NAME = "category";

    /**
     * The default sort order for this table
     */
    public static final String DEFAULT_SORT_ORDER = "rank ASC";

    /**
     * The id of the category
     * <p>Type: INTEGER</p>
     */
    public static final String _ID = "_id";

    /**
     * The name of the category.
     * <p>Type: STRING</p>
     */
    public static final String CATEGORY_NAME = "category_name";

    /**
     * The name of the group it belongs to
     * <p>Type: STRING</p>
     */
    public static final String GROUP_NAME = "group_name";

    /**
     * The default value for data input
     * <p>Type: FLOAT</p>
     */
    public static final String DEFAULT_VALUE = "default_value";
    
    /**
     * The increment/decrement for input
     * <p>Type: FLOAT</p>
     */
    public static final String INCREMENT = "increment";

    /**
     * The goal value for data input
     * <p>Type: FLOAT</p>
     */
    public static final String GOAL = "goal";

    /**
     * The color associated with the series
     * <p>Type: STRING, in the form "#rrggbb" (no quotes)</p>
     */
    public static final String COLOR = "color";

    /**
     * The aggregation period of the series in milliseconds
     * <p>Type: INTEGER</p>
     */
    public static final String PERIOD = "period";
    
    /**
     * The rank of the series (for ordering)
     * <p>Type: INTEGER</p>
     */
    public static final String RANK = "rank";

    /**
     * How to aggregate data within a given period ('sum' or 'average')
     * <p>Type: STRING, 'sum' or 'average'</p>
     */
    public static final String AGGREGATION = "aggregation";

    /**
     * The type of the series:
     *   discrete: represents a single point in time, ts_start == ts_end
     *   range: represents a range of time, ts_end >= ts_start
     *   calculated: based on the input of other timeseries
     * <p>Type: STRING, 'discrete', 'range', 'synthetic'</p>
     */
    public static final String TYPE = "type";

    /**
     * If aggregation periods without entries should have an entry automatically
     * inserted.
     * <p>Type: INTEGER, 0 or 1</p>
     */
    public static final String ZEROFILL = "zerofill";

    /**
     * If type == 'synthetic', this is the formula defining the calculation to
     * perform.
     * <p>Type: STRING</p>
     */
    public static final String FORMULA = "formula";

    /**
     * How to interpolate data between points.
     * <p>Type: STRING, base class name of interpolator class</p>
     */
    public static final String INTERPOLATION = "interpolation";

    // The following are just cached data:
    
    /**
     * Most recent datapoint timestamp
     * <p>Type: LONG, milliseconds since epoch from System.currentTimeInMillis()</p>
     */
    public static final String RECENT_TIMESTAMP = "recent_timestamp";
    
    /**
     * Most recent datapoint value
     * <p>Type: FLOAT</p>
     */
    public static final String RECENT_VALUE = "recent_value";

    /**
     * Most recent datapoint trend value
     * <p>Type: FLOAT</p>
     */
    public static final String RECENT_TREND = "recent_trend";
    
    /**
     * Current state of the trend:
     *   TREND_STATE_DOWN_15_GOOD = "trend_down_15_good";
     *   TREND_STATE_DOWN_15_BAD = "trend_down_15_bad";
     *   TREND_STATE_DOWN_30_GOOD = "trend_down_30_good";
     *   TREND_STATE_DOWN_30_BAD = "trend_down_30_bad";
     *   TREND_STATE_DOWN_45_GOOD = "trend_down_45_good";
     *   TREND_STATE_DOWN_45_BAD = "trend_down_45_bad";
     *   TREND_STATE_UP_15_GOOD = "trend_up_15_good";
     *   TREND_STATE_UP_15_BAD = "trend_up_15_bad";
     *   TREND_STATE_UP_30_GOOD = "trend_up_30_good";
     *   TREND_STATE_UP_30_BAD = "trend_up_30_bad";
     *   TREND_STATE_UP_45_GOOD = "trend_up_45_good";
     *   TREND_STATE_UP_45_BAD = "trend_up_45_bad";
     *   TREND_STATE_FLAT = "trend_flat";
     *   TREND_STATE_FLAT_GOAL = "trend_flat_goal";
     *   TREND_STATE_DOWN_15 = "trend_down_15";
     *   TREND_STATE_UP_15 = "trend_up_15";
     *   TREND_STATE_UNKNOWN = "trend_unknown";
     * <p>Type: STRING</p>
     */
    public static final String TREND_STATE = "trend_state";    
    
    /**
     * The table creation sql
     */
    public static final String TABLE_CREATE = "create table " + TABLE_NAME + " ("
      + _ID + " integer primary key autoincrement, " 
      + CATEGORY_NAME + " text not null, "
      + GROUP_NAME + " text, "
      + DEFAULT_VALUE + " float not null, " 
      + INCREMENT + " float not null, "
      + GOAL + " float not null, "
      + COLOR + " text not null, "
      + PERIOD + " integer not null, "
      + RANK + " integer not null, "
      + AGGREGATION + " text not null, "
      + TYPE + " string not null, "
      + ZEROFILL + " integer not null, "
      + FORMULA + " text, "
      + INTERPOLATION + " text not null, "
      + ZEROFILL + " integer not null, "
      + RECENT_TIMESTAMP + " long not null, "
      + RECENT_VALUE + " float not null, "
      + RECENT_TREND + " float not null, "
      + TREND_STATE + " string not null);";
  }
}
