/*
 * Copyright (C) 2007 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package net.redgeek.android.eventrecorder;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Debug;
import android.text.TextUtils;
import android.util.Log;

import net.redgeek.android.eventrecorder.TimeSeriesData.Datapoint;
import net.redgeek.android.eventrecorder.TimeSeriesData.DateMap;
import net.redgeek.android.eventrecorder.TimeSeriesData.FormulaCache;
import net.redgeek.android.eventrecorder.TimeSeriesData.TimeSeries;
import net.redgeek.android.eventrecorder.synthetic.Formula;
import net.redgeek.android.eventrecorder.synthetic.SeriesData;
import net.redgeek.android.eventrecorder.synthetic.SeriesData.Datum;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// TODO:  support calculated series
// TODO:  flesh out interpolator plugins
public class TimeSeriesProvider extends ContentProvider {
  private static final String TAG = "TimeSeriesProvider";

  private static final int DATABASE_VERSION = 1;

  private static final int TIMESERIES = 1;
  private static final int TIMESERIES_ID = 2;
  private static final int DATAPOINTS = 3;
  private static final int DATAPOINTS_ID = 4;
  private static final int DATAPOINTS_RECENT = 5;
  private static final int DATAPOINTS_RANGE = 6;
  private static final int DATEMAP = 7;
  private static final int DATEMAP_ID = 8;
  
  private DatabaseHelper mDbHelper;
  private static UriMatcher sURIMatcher;
  private static HashMap<String, String> sTimeSeriesProjection;
  private static HashMap<String, String> sDatapointProjection;
  private static HashMap<String, String> sDatapointProjectionDay;
  private static HashMap<String, String> sDatapointProjectionWeek;
  private static HashMap<String, String> sDatapointProjectionMonth;
  private static HashMap<String, String> sDatapointProjectionQuarter;
  private static HashMap<String, String> sDatapointProjectionYear;
  private static HashMap<String, String> sDatapointProjectionAll;
  private static HashMap<String, String> sDatemapProjection;
  private DateMapCache mDateMap;
  private Lock mLock;
  
  public static final int PATH_SEGMENT_TIMERSERIES_ID = 1;
  public static final int PATH_SEGMENT_DATAPOINT_ID = 3;
  public static final int PATH_SEGMENT_DATAPOINT_RECENT_COUNT = 3;
  public static final int PATH_SEGMENT_DATAPOINT_RECENT_AGGREGATION = 4;
  public static final int PATH_SEGMENT_DATAPOINT_RANGE_START = 3;
  public static final int PATH_SEGMENT_DATAPOINT_RANGE_END = 4;
  public static final int PATH_SEGMENT_DATAPOINT_RANGE_AGGREGATION = 5;
  public static final int PATH_SEGMENT_DATEMAP_ID = 1;
  
  static {
    sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    sURIMatcher.addURI(TimeSeriesData.AUTHORITY, "timeseries", TIMESERIES);
    sURIMatcher.addURI(TimeSeriesData.AUTHORITY, "timeseries/#", TIMESERIES_ID);
    sURIMatcher.addURI(TimeSeriesData.AUTHORITY, "timeseries/#/datapoints", DATAPOINTS);
    sURIMatcher.addURI(TimeSeriesData.AUTHORITY, "timeseries/#/datapoints/#", DATAPOINTS_ID);
    sURIMatcher.addURI(TimeSeriesData.AUTHORITY, "timeseries/#/recent/#", DATAPOINTS_RECENT);
    sURIMatcher.addURI(TimeSeriesData.AUTHORITY, "timeseries/#/recent/#/*", DATAPOINTS_RECENT);
    sURIMatcher.addURI(TimeSeriesData.AUTHORITY, "timeseries/#/range/#/#", DATAPOINTS_RANGE);
    sURIMatcher.addURI(TimeSeriesData.AUTHORITY, "timeseries/#/range/#/#/*", DATAPOINTS_RANGE);
    sURIMatcher.addURI(TimeSeriesData.AUTHORITY, "datemap/", DATEMAP);
    sURIMatcher.addURI(TimeSeriesData.AUTHORITY, "datemap/#", DATEMAP_ID);
      
    sTimeSeriesProjection = new HashMap<String, String>();
    sTimeSeriesProjection.put(TimeSeries._ID, TimeSeries._ID);
    sTimeSeriesProjection.put(TimeSeries.TIMESERIES_NAME, TimeSeries.TIMESERIES_NAME);
    sTimeSeriesProjection.put(TimeSeries.RECORDING_DATAPOINT_ID, TimeSeries.RECORDING_DATAPOINT_ID);
    sTimeSeriesProjection.put(TimeSeries.GROUP_NAME, TimeSeries.GROUP_NAME);
    sTimeSeriesProjection.put(TimeSeries.DEFAULT_VALUE, TimeSeries.DEFAULT_VALUE);
    sTimeSeriesProjection.put(TimeSeries.INCREMENT, TimeSeries.INCREMENT);
    sTimeSeriesProjection.put(TimeSeries.GOAL, TimeSeries.GOAL);
    sTimeSeriesProjection.put(TimeSeries.COLOR, TimeSeries.COLOR);
    sTimeSeriesProjection.put(TimeSeries.PERIOD, TimeSeries.PERIOD);
    sTimeSeriesProjection.put(TimeSeries.RANK, TimeSeries.RANK);
    sTimeSeriesProjection.put(TimeSeries.AGGREGATION, TimeSeries.AGGREGATION);
    sTimeSeriesProjection.put(TimeSeries.TYPE, TimeSeries.TYPE);
    sTimeSeriesProjection.put(TimeSeries.ZEROFILL, TimeSeries.ZEROFILL);
    sTimeSeriesProjection.put(TimeSeries.FORMULA, TimeSeries.FORMULA);
    sTimeSeriesProjection.put(TimeSeries.UNITS, TimeSeries.UNITS);
    sTimeSeriesProjection.put(TimeSeries.INTERPOLATION, TimeSeries.INTERPOLATION);
    sTimeSeriesProjection.put(TimeSeries.SENSITIVITY, TimeSeries.SENSITIVITY);
    sTimeSeriesProjection.put(TimeSeries.SMOOTHING, TimeSeries.SMOOTHING);
    sTimeSeriesProjection.put(TimeSeries.HISTORY, TimeSeries.HISTORY);
    sTimeSeriesProjection.put(TimeSeries.DECIMALS, TimeSeries.DECIMALS);
    
    sDatapointProjection = new HashMap<String, String>();
    sDatapointProjection.put(Datapoint._ID, Datapoint._ID);
    sDatapointProjection.put(Datapoint.TIMESERIES_ID, Datapoint.TIMESERIES_ID);
    sDatapointProjection.put(Datapoint.VALUE, Datapoint.VALUE);
    sDatapointProjection.put(Datapoint.ENTRIES, Datapoint.ENTRIES);
    sDatapointProjection.put(Datapoint.TS_START, Datapoint.TS_START);
    sDatapointProjection.put(Datapoint.TS_END, Datapoint.TS_END);
    sDatapointProjection.put(Datapoint.TREND, Datapoint.TREND);
    sDatapointProjection.put(Datapoint.STDDEV, Datapoint.STDDEV);

    sDatapointProjectionDay = new HashMap<String, String>();
    sDatapointProjectionDay.put(Datapoint._ID, Datapoint._ID);
    sDatapointProjectionDay.put(Datapoint.TIMESERIES_ID, Datapoint.TIMESERIES_ID);
    sDatapointProjectionDay.put(Datapoint.VALUE, 
        Datapoint.VALUE + "_" + Datapoint.AGGREGATE_SUFFIX[0] + " as " + Datapoint.VALUE);
    sDatapointProjectionDay.put(Datapoint.ENTRIES,
        Datapoint.ENTRIES + "_" + Datapoint.AGGREGATE_SUFFIX[0] + " as " + Datapoint.ENTRIES);
    sDatapointProjectionDay.put(Datapoint.TS_START, 
        Datapoint.TS_START + "_" + Datapoint.AGGREGATE_SUFFIX[0] + " as " + Datapoint.TS_START);
    sDatapointProjectionDay.put(Datapoint.TS_END, 
        Datapoint.TS_END + "_" + Datapoint.AGGREGATE_SUFFIX[0] + " as " + Datapoint.TS_END);
    sDatapointProjectionDay.put(Datapoint.TREND, 
        Datapoint.TREND + "_" + Datapoint.AGGREGATE_SUFFIX[0] + " as " + Datapoint.TREND);
    sDatapointProjectionDay.put(Datapoint.STDDEV, 
        Datapoint.STDDEV + "_" + Datapoint.AGGREGATE_SUFFIX[0] + " as " + Datapoint.STDDEV);

    sDatapointProjectionWeek = new HashMap<String, String>();
    sDatapointProjectionWeek.put(Datapoint._ID, Datapoint._ID);
    sDatapointProjectionWeek.put(Datapoint.TIMESERIES_ID, Datapoint.TIMESERIES_ID);
    sDatapointProjectionWeek.put(Datapoint.VALUE, 
        Datapoint.VALUE + "_" + Datapoint.AGGREGATE_SUFFIX[1] + " as " + Datapoint.VALUE);
    sDatapointProjectionWeek.put(Datapoint.ENTRIES,
        Datapoint.ENTRIES + "_" + Datapoint.AGGREGATE_SUFFIX[1] + " as " + Datapoint.ENTRIES);
    sDatapointProjectionWeek.put(Datapoint.TS_START, 
        Datapoint.TS_START + "_" + Datapoint.AGGREGATE_SUFFIX[1] + " as " + Datapoint.TS_START);
    sDatapointProjectionWeek.put(Datapoint.TS_END, 
        Datapoint.TS_END + "_" + Datapoint.AGGREGATE_SUFFIX[1] + " as " + Datapoint.TS_END);
    sDatapointProjectionWeek.put(Datapoint.TREND, 
        Datapoint.TREND + "_" + Datapoint.AGGREGATE_SUFFIX[1] + " as " + Datapoint.TREND);
    sDatapointProjectionWeek.put(Datapoint.STDDEV, 
        Datapoint.STDDEV + "_" + Datapoint.AGGREGATE_SUFFIX[1] + " as " + Datapoint.STDDEV);

    sDatapointProjectionMonth = new HashMap<String, String>();
    sDatapointProjectionMonth.put(Datapoint._ID, Datapoint._ID);
    sDatapointProjectionMonth.put(Datapoint.TIMESERIES_ID, Datapoint.TIMESERIES_ID);
    sDatapointProjectionMonth.put(Datapoint.VALUE, 
        Datapoint.VALUE + "_" + Datapoint.AGGREGATE_SUFFIX[2] + " as " + Datapoint.VALUE);
    sDatapointProjectionMonth.put(Datapoint.ENTRIES,
        Datapoint.ENTRIES + "_" + Datapoint.AGGREGATE_SUFFIX[2] + " as " + Datapoint.ENTRIES);
    sDatapointProjectionMonth.put(Datapoint.TS_START, 
        Datapoint.TS_START + "_" + Datapoint.AGGREGATE_SUFFIX[2] + " as " + Datapoint.TS_START);
    sDatapointProjectionMonth.put(Datapoint.TS_END, 
        Datapoint.TS_END + "_" + Datapoint.AGGREGATE_SUFFIX[2] + " as " + Datapoint.TS_END);
    sDatapointProjectionMonth.put(Datapoint.TREND, 
        Datapoint.TREND + "_" + Datapoint.AGGREGATE_SUFFIX[2] + " as " + Datapoint.TREND);
    sDatapointProjectionMonth.put(Datapoint.STDDEV, 
        Datapoint.STDDEV + "_" + Datapoint.AGGREGATE_SUFFIX[2] + " as " + Datapoint.STDDEV);

    sDatapointProjectionQuarter = new HashMap<String, String>();
    sDatapointProjectionQuarter.put(Datapoint._ID, Datapoint._ID);
    sDatapointProjectionQuarter.put(Datapoint.TIMESERIES_ID, Datapoint.TIMESERIES_ID);
    sDatapointProjectionQuarter.put(Datapoint.VALUE, 
        Datapoint.VALUE + "_" + Datapoint.AGGREGATE_SUFFIX[3] + " as " + Datapoint.VALUE);
    sDatapointProjectionQuarter.put(Datapoint.ENTRIES,
        Datapoint.ENTRIES + "_" + Datapoint.AGGREGATE_SUFFIX[3] + " as " + Datapoint.ENTRIES);
    sDatapointProjectionQuarter.put(Datapoint.TS_START, 
        Datapoint.TS_START + "_" + Datapoint.AGGREGATE_SUFFIX[3] + " as " + Datapoint.TS_START);
    sDatapointProjectionQuarter.put(Datapoint.TS_END, 
        Datapoint.TS_END + "_" + Datapoint.AGGREGATE_SUFFIX[3] + " as " + Datapoint.TS_END);
    sDatapointProjectionQuarter.put(Datapoint.TREND, 
        Datapoint.TREND + "_" + Datapoint.AGGREGATE_SUFFIX[3] + " as " + Datapoint.TREND);
    sDatapointProjectionQuarter.put(Datapoint.STDDEV, 
        Datapoint.STDDEV + "_" + Datapoint.AGGREGATE_SUFFIX[3] + " as " + Datapoint.STDDEV);

    sDatapointProjectionYear = new HashMap<String, String>();
    sDatapointProjectionYear.put(Datapoint._ID, Datapoint._ID);
    sDatapointProjectionYear.put(Datapoint.TIMESERIES_ID, Datapoint.TIMESERIES_ID);
    sDatapointProjectionYear.put(Datapoint.VALUE, 
        Datapoint.VALUE + "_" + Datapoint.AGGREGATE_SUFFIX[4] + " as " + Datapoint.VALUE);
    sDatapointProjectionYear.put(Datapoint.ENTRIES,
        Datapoint.ENTRIES + "_" + Datapoint.AGGREGATE_SUFFIX[4] + " as " + Datapoint.ENTRIES);
    sDatapointProjectionYear.put(Datapoint.TS_START, 
        Datapoint.TS_START + "_" + Datapoint.AGGREGATE_SUFFIX[4] + " as " + Datapoint.TS_START);
    sDatapointProjectionYear.put(Datapoint.TS_END, 
        Datapoint.TS_END + "_" + Datapoint.AGGREGATE_SUFFIX[4] + " as " + Datapoint.TS_END);
    sDatapointProjectionYear.put(Datapoint.TREND, 
        Datapoint.TREND + "_" + Datapoint.AGGREGATE_SUFFIX[4] + " as " + Datapoint.TREND);
    sDatapointProjectionYear.put(Datapoint.STDDEV, 
        Datapoint.STDDEV + "_" + Datapoint.AGGREGATE_SUFFIX[4] + " as " + Datapoint.STDDEV);

    sDatapointProjectionAll = new HashMap<String, String>();
    sDatapointProjectionAll.put(Datapoint._ID, Datapoint._ID);
    sDatapointProjectionAll.put(Datapoint.TIMESERIES_ID, Datapoint.TIMESERIES_ID);
    sDatapointProjectionAll.put(Datapoint.VALUE, Datapoint.VALUE);
    sDatapointProjectionAll.put(Datapoint.ENTRIES, "1 as " + Datapoint.ENTRIES);
    sDatapointProjectionAll.put(Datapoint.TS_START, Datapoint.TS_START);
    sDatapointProjectionAll.put(Datapoint.TS_END, Datapoint.TS_END);
    sDatapointProjectionAll.put(Datapoint.TREND, Datapoint.TREND);
    sDatapointProjectionAll.put(Datapoint.STDDEV, Datapoint.STDDEV);
    sDatapointProjectionAll.put(Datapoint.SUM_VALUE, Datapoint.SUM_VALUE);
    sDatapointProjectionAll.put(Datapoint.SUM_ENTRIES, Datapoint.SUM_ENTRIES);
    sDatapointProjectionAll.put(Datapoint.SUM_VALUE_SQR, Datapoint.SUM_VALUE_SQR);
    sDatapointProjectionAll.put(
        Datapoint.TS_START + "_" + Datapoint.AGGREGATE_SUFFIX[0], 
        Datapoint.TS_START + "_" + Datapoint.AGGREGATE_SUFFIX[0]);
    sDatapointProjectionAll.put(
        Datapoint.TS_END + "_" + Datapoint.AGGREGATE_SUFFIX[0],
        Datapoint.TS_END + "_" + Datapoint.AGGREGATE_SUFFIX[0]);
    sDatapointProjectionAll.put(
        Datapoint.TREND + "_" + Datapoint.AGGREGATE_SUFFIX[0],
        Datapoint.TREND + "_" + Datapoint.AGGREGATE_SUFFIX[0]);
    sDatapointProjectionAll.put(
        Datapoint.STDDEV + "_" + Datapoint.AGGREGATE_SUFFIX[0],
        Datapoint.STDDEV + "_" + Datapoint.AGGREGATE_SUFFIX[0]);
    sDatapointProjectionAll.put(
        Datapoint.SUM_ENTRIES + "_" + Datapoint.AGGREGATE_SUFFIX[0],
        Datapoint.SUM_ENTRIES + "_" + Datapoint.AGGREGATE_SUFFIX[0]);
    sDatapointProjectionAll.put(
        Datapoint.SUM_VALUE + "_" + Datapoint.AGGREGATE_SUFFIX[0],
        Datapoint.SUM_VALUE + "_" + Datapoint.AGGREGATE_SUFFIX[0]);
    sDatapointProjectionAll.put(
        Datapoint.SUM_VALUE_SQR + "_" + Datapoint.AGGREGATE_SUFFIX[0],
        Datapoint.SUM_VALUE_SQR + "_" + Datapoint.AGGREGATE_SUFFIX[0]);
    sDatapointProjectionAll.put(
        Datapoint.TS_START + "_" + Datapoint.AGGREGATE_SUFFIX[1], 
        Datapoint.TS_START + "_" + Datapoint.AGGREGATE_SUFFIX[1]);
    sDatapointProjectionAll.put(
        Datapoint.TS_END + "_" + Datapoint.AGGREGATE_SUFFIX[1],
        Datapoint.TS_END + "_" + Datapoint.AGGREGATE_SUFFIX[1]);
    sDatapointProjectionAll.put(
        Datapoint.TREND + "_" + Datapoint.AGGREGATE_SUFFIX[1],
        Datapoint.TREND + "_" + Datapoint.AGGREGATE_SUFFIX[1]);
    sDatapointProjectionAll.put(
        Datapoint.STDDEV + "_" + Datapoint.AGGREGATE_SUFFIX[1],
        Datapoint.STDDEV + "_" + Datapoint.AGGREGATE_SUFFIX[1]);
    sDatapointProjectionAll.put(
        Datapoint.SUM_ENTRIES + "_" + Datapoint.AGGREGATE_SUFFIX[1],
        Datapoint.SUM_ENTRIES + "_" + Datapoint.AGGREGATE_SUFFIX[1]);
    sDatapointProjectionAll.put(
        Datapoint.SUM_VALUE + "_" + Datapoint.AGGREGATE_SUFFIX[1],
        Datapoint.SUM_VALUE + "_" + Datapoint.AGGREGATE_SUFFIX[1]);
    sDatapointProjectionAll.put(
        Datapoint.SUM_VALUE_SQR + "_" + Datapoint.AGGREGATE_SUFFIX[1],
        Datapoint.SUM_VALUE_SQR + "_" + Datapoint.AGGREGATE_SUFFIX[1]);
    sDatapointProjectionAll.put(
        Datapoint.TS_START + "_" + Datapoint.AGGREGATE_SUFFIX[2], 
        Datapoint.TS_START + "_" + Datapoint.AGGREGATE_SUFFIX[2]);
    sDatapointProjectionAll.put(
        Datapoint.TS_END + "_" + Datapoint.AGGREGATE_SUFFIX[2],
        Datapoint.TS_END + "_" + Datapoint.AGGREGATE_SUFFIX[2]);
    sDatapointProjectionAll.put(
        Datapoint.TREND + "_" + Datapoint.AGGREGATE_SUFFIX[2],
        Datapoint.TREND + "_" + Datapoint.AGGREGATE_SUFFIX[2]);
    sDatapointProjectionAll.put(
        Datapoint.STDDEV + "_" + Datapoint.AGGREGATE_SUFFIX[2],
        Datapoint.STDDEV + "_" + Datapoint.AGGREGATE_SUFFIX[2]);
    sDatapointProjectionAll.put(
        Datapoint.SUM_ENTRIES + "_" + Datapoint.AGGREGATE_SUFFIX[2],
        Datapoint.SUM_ENTRIES + "_" + Datapoint.AGGREGATE_SUFFIX[2]);
    sDatapointProjectionAll.put(
        Datapoint.SUM_VALUE + "_" + Datapoint.AGGREGATE_SUFFIX[2],
        Datapoint.SUM_VALUE + "_" + Datapoint.AGGREGATE_SUFFIX[2]);
    sDatapointProjectionAll.put(
        Datapoint.SUM_VALUE_SQR + "_" + Datapoint.AGGREGATE_SUFFIX[2],
        Datapoint.SUM_VALUE_SQR + "_" + Datapoint.AGGREGATE_SUFFIX[2]);
    sDatapointProjectionAll.put(
        Datapoint.TS_START + "_" + Datapoint.AGGREGATE_SUFFIX[3], 
        Datapoint.TS_START + "_" + Datapoint.AGGREGATE_SUFFIX[3]);
    sDatapointProjectionAll.put(
        Datapoint.TS_END + "_" + Datapoint.AGGREGATE_SUFFIX[3],
        Datapoint.TS_END + "_" + Datapoint.AGGREGATE_SUFFIX[3]);
    sDatapointProjectionAll.put(
        Datapoint.TREND + "_" + Datapoint.AGGREGATE_SUFFIX[3],
        Datapoint.TREND + "_" + Datapoint.AGGREGATE_SUFFIX[3]);
    sDatapointProjectionAll.put(
        Datapoint.STDDEV + "_" + Datapoint.AGGREGATE_SUFFIX[3],
        Datapoint.STDDEV + "_" + Datapoint.AGGREGATE_SUFFIX[3]);
    sDatapointProjectionAll.put(
        Datapoint.SUM_ENTRIES + "_" + Datapoint.AGGREGATE_SUFFIX[3],
        Datapoint.SUM_ENTRIES + "_" + Datapoint.AGGREGATE_SUFFIX[3]);
    sDatapointProjectionAll.put(
        Datapoint.SUM_VALUE + "_" + Datapoint.AGGREGATE_SUFFIX[3],
        Datapoint.SUM_VALUE + "_" + Datapoint.AGGREGATE_SUFFIX[3]);
    sDatapointProjectionAll.put(
        Datapoint.SUM_VALUE_SQR + "_" + Datapoint.AGGREGATE_SUFFIX[3],
        Datapoint.SUM_VALUE_SQR + "_" + Datapoint.AGGREGATE_SUFFIX[3]);
    sDatapointProjectionAll.put(
        Datapoint.TS_START + "_" + Datapoint.AGGREGATE_SUFFIX[4], 
        Datapoint.TS_START + "_" + Datapoint.AGGREGATE_SUFFIX[4]);
    sDatapointProjectionAll.put(
        Datapoint.TS_END + "_" + Datapoint.AGGREGATE_SUFFIX[4],
        Datapoint.TS_END + "_" + Datapoint.AGGREGATE_SUFFIX[4]);
    sDatapointProjectionAll.put(
        Datapoint.TREND + "_" + Datapoint.AGGREGATE_SUFFIX[4],
        Datapoint.TREND + "_" + Datapoint.AGGREGATE_SUFFIX[4]);
    sDatapointProjectionAll.put(
        Datapoint.STDDEV + "_" + Datapoint.AGGREGATE_SUFFIX[4],
        Datapoint.STDDEV + "_" + Datapoint.AGGREGATE_SUFFIX[4]);
    sDatapointProjectionAll.put(
        Datapoint.SUM_ENTRIES + "_" + Datapoint.AGGREGATE_SUFFIX[4],
        Datapoint.SUM_ENTRIES + "_" + Datapoint.AGGREGATE_SUFFIX[4]);
    sDatapointProjectionAll.put(
        Datapoint.SUM_VALUE + "_" + Datapoint.AGGREGATE_SUFFIX[4],
        Datapoint.SUM_VALUE + "_" + Datapoint.AGGREGATE_SUFFIX[4]);
    sDatapointProjectionAll.put(
        Datapoint.SUM_VALUE_SQR + "_" + Datapoint.AGGREGATE_SUFFIX[4],
        Datapoint.SUM_VALUE_SQR + "_" + Datapoint.AGGREGATE_SUFFIX[4]);

    sDatemapProjection = new HashMap<String, String>();
    sDatemapProjection.put(DateMap._ID, DateMap._ID);
    sDatemapProjection.put(DateMap.YEAR, DateMap.YEAR);
    sDatemapProjection.put(DateMap.MONTH, DateMap.MONTH);
    sDatemapProjection.put(DateMap.DOW, DateMap.DOW);
    sDatemapProjection.put(DateMap.SECONDS, DateMap.SECONDS);
  }

  @Override
  public boolean onCreate() {
    mDbHelper = new DatabaseHelper(getContext());
    mLock = new ReentrantLock();
    mDateMap = new DateMapCache();
    mDateMap.populateCacheLocalToProvider(mDbHelper.getReadableDatabase());
    return true;
  }
  
  @Override
  public String getType(Uri uri) {
    switch (sURIMatcher.match(uri)) {
      case TIMESERIES:
        return TimeSeries.CONTENT_TYPE;
      case TIMESERIES_ID:
        return TimeSeries.CONTENT_ITEM_TYPE;
      case DATAPOINTS:
      case DATAPOINTS_RECENT:
      case DATAPOINTS_RANGE:
        return Datapoint.CONTENT_TYPE;
      case DATAPOINTS_ID:
        return Datapoint.CONTENT_ITEM_TYPE;
      case DATEMAP:
        return DateMap.CONTENT_TYPE;
      case DATEMAP_ID:
        return DateMap.CONTENT_ITEM_TYPE;
      default:
        throw new IllegalArgumentException("getType: Unknown URI " + uri);
    }      
  }
  
  private Formula fetchFormula(SQLiteDatabase db, long timeSeriesId) {
    Formula formula = null;
    String[] projection = new String[] { TimeSeries.FORMULA };
    StringBuilder where = new StringBuilder();
    
    SQLiteQueryBuilder qb = new SQLiteQueryBuilder();      
    qb.setTables(TimeSeries.TABLE_NAME);
    qb.setProjectionMap(sTimeSeriesProjection);
    where.append(TimeSeries._ID).append(" = ").append(timeSeriesId);
    Cursor c = qb.query(db, projection, where.toString(), null, null, null, null);
    c.moveToFirst();
    String f = TimeSeries.getFormula(c);
    c.close();
    if (TextUtils.isEmpty(f) == false) {
      formula = new Formula(f);
    }
    return formula;
  }
  
  private ArrayList<Long> fetchSourceSeries(SQLiteDatabase db, long timeSeriesId) 
      throws Exception {
    ArrayList<Long> sourceIds = new ArrayList<Long>();
    String[] projection = new String[] { FormulaCache.SOURCE_SERIES };
    StringBuilder where = new StringBuilder();

    SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
    qb.setTables(FormulaCache.TABLE_NAME);
    where.append(FormulaCache.RESULT_SERIES).append(" = ").append(timeSeriesId);
    
    Cursor c = qb.query(db, projection, where.toString(), null, null, null, null);
    int count = c.getCount();
    c.moveToFirst();
    for (int i = 0; i < count; i++) {
      Long l = Long.valueOf(FormulaCache.getSourceSeries(c));
      if (l == timeSeriesId) {
        c.close();
        throw new Exception("A synthetic series may not depend on itself!");
      }
      sourceIds.add(l);
      c.moveToNext();
    }
    c.close();

    return sourceIds;
  }
  
  private ArrayList<Long> fetchDependentSeries(SQLiteDatabase db, long timeSeriesId)
      throws Exception {
    ArrayList<Long> resultIds = new ArrayList<Long>();
    String[] projection = new String[] { FormulaCache.RESULT_SERIES };
    StringBuilder where = new StringBuilder();

    SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
    qb.setDistinct(true);
    qb.setTables(FormulaCache.TABLE_NAME);
    where.append(FormulaCache.SOURCE_SERIES).append(" = ").append(timeSeriesId);
    
    Cursor c = qb.query(db, projection, where.toString(), null, null, null, null);
    int count = c.getCount();
    c.moveToFirst();
    for (int i = 0; i < count; i++) {
      Long l = Long.valueOf(FormulaCache.getResultSeries(c));
      if (l == timeSeriesId) {
        c.close();
        throw new Exception("A synthetic series may not depend on itself!");
      }
      resultIds.add(l);
      c.moveToNext();
    }
    c.close();

    return resultIds;
  }

  
  private ArrayList<SeriesData> fetchSourceData(SQLiteDatabase db, 
      ArrayList<Long> sourceIds, int fromTimestamp) {
    SQLiteQueryBuilder qb;
    Cursor c;
    String[] dpProjection = new String[] { Datapoint.TS_START, Datapoint.TS_END,
        Datapoint.VALUE, Datapoint.ENTRIES };
    int count;
    ArrayList<SeriesData> sources = new ArrayList<SeriesData>();
    StringBuilder where = new StringBuilder();

    int size = sourceIds.size();
    for (int i = 0; i < size; i++) {
      long id = sourceIds.get(i);
      boolean avg = false;

      String[] tsProjection = new String[] { TimeSeries.FORMULA, 
          TimeSeries.AGGREGATION, TimeSeries.TIMESERIES_NAME };
      qb = new SQLiteQueryBuilder();
      qb.setTables(TimeSeries.TABLE_NAME);
      qb.setProjectionMap(sTimeSeriesProjection);
      where.setLength(0);
      where.append(TimeSeries._ID).append(" = ").append(id);
      
      c = qb.query(db, tsProjection, where.toString(), null, null, null, null);
      c.moveToFirst();
      if (TimeSeries.getAggregation(c).equals(TimeSeries.AGGREGATION_AVG))
        avg = true;
      String name = TimeSeries.getTimeSeriesName(c);
      c.close();

      Datum d;
      SeriesData ts = new SeriesData();
      ts.mTsEarliest = Integer.MAX_VALUE;
      ts.mName = name;

      // grab the previous point in order to connect dots:
      where.setLength(0);
      where.append(Datapoint.TIMESERIES_ID).append(" = ").append(id).append(" and ");
      where.append(Datapoint.TS_START).append(" < ").append(fromTimestamp);
      String sortOrder = Datapoint.TS_START + " desc ";
      String limit = "" + size;
      c = queryDatapointInternal(where.toString(), null, null, sortOrder, limit);
      
      count = c.getCount();
      c.moveToLast();
      for (int j = 0; j < count; j++) {
        d = new Datum();
        d.mTsStart = Datapoint.getTsStartIdx(c, null);
        if (d.mTsStart < ts.mTsEarliest) {
          ts.mTsEarliest = d.mTsStart;
        }
        d.mTsEnd = Datapoint.getTsEndIdx(c, null);
        d.mValue = Datapoint.getValueIdx(c, null);
        int entries = Datapoint.getEntriesIdx(c, null);
        if (avg == true) {
          d.mValue /= entries;
        }
        ts.mData.add(d);
        c.moveToPrevious();
      }
      c.close();
        
      // now grab the rest:
      where.setLength(0);
      where.append(Datapoint.TIMESERIES_ID).append(" = ").append(id).append(" and ");
      where.append(Datapoint.TS_START).append(" >= ").append(fromTimestamp);
      sortOrder = Datapoint.TS_START + " asc ";
      c = queryDatapointInternal(where.toString(), null, null, sortOrder, null);
      
      count = c.getCount();
      c.moveToFirst();
      for (int j = 0; j < count; j++) {
        d = new Datum();
        d.mTsStart = Datapoint.getTsStartIdx(c, null);
        d.mTsEnd = Datapoint.getTsEndIdx(c, null);
        d.mValue = Datapoint.getValueIdx(c, null);
        int entries = Datapoint.getEntriesIdx(c, null);
        if (avg == true) {
          d.mValue /= entries;
        }
        ts.mData.add(d);
        c.moveToNext();
      }
      c.close();
      sources.add(ts);
    }
    
    return sources;
  }

  private void updateFormulaData(SQLiteDatabase db, long timeSeriesId, 
      int fromTimestamp, Formula formula) throws Exception {
    ArrayList<Long> sourceIds;
    ArrayList<Long> resultIds;
    ArrayList<SeriesData> sources;
    SQLiteQueryBuilder qb;         
    String[] projection;
    StringBuilder where = new StringBuilder();
    Cursor c;
    int count;
    
    // get the formula if we we're supplied with one:
    if (formula == null) {
      formula = fetchFormula(db, timeSeriesId);
    }
    
    if (formula != null) {
      // Get the source series ids:
      sourceIds = fetchSourceSeries(db, timeSeriesId);
      sources = fetchSourceData(db, sourceIds, fromTimestamp);

      SeriesData result = formula.apply(sources);

      int minTime = Integer.MAX_VALUE;
      for (int i = 0; i < sources.size(); i++) {
        int timestamp = sources.get(i).mTsEarliest;
        if (timestamp < minTime) {
          minTime = timestamp;
        }
      }
      
      // fetch the smoothing and history parameters of the resultant timeseries
      Uri timeseriesParams = ContentUris.withAppendedId(
          TimeSeries.CONTENT_URI, timeSeriesId);
      c = query(timeseriesParams, 
          new String[] { TimeSeries.SMOOTHING, TimeSeries.HISTORY,
          TimeSeries.RECORDING_DATAPOINT_ID }, 
          null, null, null);
      c.moveToFirst();
      double smoothing = TimeSeries.getSmoothing(c);
      int history = TimeSeries.getHistory(c);
      long recordingDatapointId = TimeSeries.getRecordingDatapointId(c);
      c.close();

      // Store the results
      Datum d;
      ContentValues values = new ContentValues();
      where.append(Datapoint.TIMESERIES_ID).append(" = ").append(timeSeriesId).append(" and ");
      where.append(Datapoint.TS_START).append(" >= ").append(minTime);
      count = db.delete(Datapoint.TABLE_NAME, where.toString(), null);
      int size = result.mData.size();
      for (int i = 0; i < size; i++) {
        d = result.mData.get(i);
        values.clear();

        Datapoint.setTimeSeriesId(values, timeSeriesId);
        Datapoint.setTsStart(values, d.mTsStart);
        Datapoint.setTsEnd(values, d.mTsEnd);
        Datapoint.setValue(values, d.mValue);
        Datapoint.setEntries(values, 1);

        setContentValues(db, values, timeSeriesId, recordingDatapointId,
            d.mTsStart, d.mValue, 1, smoothing, history);
        db.insert(Datapoint.TABLE_NAME, null, values);
      }
    }
    
    // Now try updating all series that may be dependent on this one:
    resultIds = fetchDependentSeries(db, timeSeriesId);
    int size = resultIds.size();
    for (int i = 0; i < size; i++) {
      updateFormulaData(db, resultIds.get(i), fromTimestamp, null);
    }
    
    return;
  }
  
  private void updateFormula(SQLiteDatabase db, long timeSeriesId, String formula) throws Exception{
    db.delete(FormulaCache.TABLE_NAME, FormulaCache.RESULT_SERIES + "="  + timeSeriesId, null);

    HashMap<String, Long> nameMap = new HashMap<String, Long>();
    String[] projection = new String[] {
      TimeSeries._ID,
      TimeSeries.TIMESERIES_NAME,
    };
    SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
    ContentValues values = new ContentValues();
    
    qb.setTables(TimeSeries.TABLE_NAME);
    qb.setProjectionMap(sTimeSeriesProjection);
    String orderBy = TimeSeries.DEFAULT_SORT_ORDER;
    Cursor c = qb.query(db, projection, null, null, null, null, orderBy);
    int count = c.getCount();
    c.moveToFirst();
    for (int i = 0; i < count; i++) {
      nameMap.put(TimeSeries.getTimeSeriesName(c), TimeSeries.getId(c));
      c.moveToNext();
    }
    c.close();
    
    Formula f = new Formula(formula);
    ArrayList<String> sources = f.getDependentNames();
    
    int size = sources.size();
    for (int i = 0; i < size; i++) {
        Long id = nameMap.get(sources.get(i));
      values.clear();
      values.put(FormulaCache.RESULT_SERIES, timeSeriesId);
      values.put(FormulaCache.SOURCE_SERIES, id.longValue());
      id = db.insert(FormulaCache.TABLE_NAME, null, values);
      if (id < 0)
        throw new Exception("Unabled to insert into formula table.");
    }
    
    updateFormulaData(db, timeSeriesId, 0, f);
    
    return;
  }

  private void updateStats(SQLiteDatabase db, long timeSeriesId, int tsStart, 
      int tsEnd) {
    ContentValues values = new ContentValues();
    int oldTsStart;
    double value;
    int entries;
    long id;
    int newPeriodStart, newPeriodEnd;
    Uri sumsUri;
    StringBuilder where = new StringBuilder();

    Datapoint.setTimeSeriesId(values, timeSeriesId);
    
    where.setLength(0);
    where.append(Datapoint.TIMESERIES_ID).append(" = ").append(timeSeriesId).append(" and ");
    where.append(Datapoint.TS_START).append(" <= ").append(tsStart);
    String sortOrder = Datapoint.TS_START + " desc ";
    String limit = "2";
    Cursor c = queryDatapointInternal(where.toString(), null, null, sortOrder, limit);

    c.moveToFirst();
    if (c.getCount() == 1) {
      // have one datapoint, which means it's the first in the series.
      // Only update it if it's at the current start time, since that's a 
      // special case.  The rest of the updates will be handled below.
      int timestamp = Datapoint.getTsStart(c);
      if (timestamp == tsStart) {      
        id = Datapoint.getId(c);
        value = Datapoint.getValue(c);

        Datapoint.setTrend(values, value);
        Datapoint.setStdDev(values, 0.0f);

        int length = Datapoint.AGGREGATE_SUFFIX.length;
        for (int i = 0; i < length; i++) {
          String suffix = Datapoint.AGGREGATE_SUFFIX[i];
          Datapoint.setTrend(values, suffix, value);
          Datapoint.setStdDev(values, suffix, 0.0f);
        }
        
        db.update(Datapoint.TABLE_NAME, values, Datapoint._ID + " = " + id, null);
      }
    }    
    c.close();

    // fetch the smoothing parameter of the timeseries
    Uri smoothingUri = ContentUris.withAppendedId(TimeSeries.CONTENT_URI, timeSeriesId);
    c = query(smoothingUri, 
        new String[] { TimeSeries.SMOOTHING, TimeSeries.HISTORY }, 
        null, null, null);
    c.moveToFirst();
    double smoothing = TimeSeries.getSmoothing(c);
    int history = TimeSeries.getHistory(c);
    c.close();

    // now fetch all the datapoints at or after the start time
    where.setLength(0);
    where.append(Datapoint.TIMESERIES_ID).append(" = ").append(timeSeriesId).append(" and ");
    where.append(Datapoint.TS_START).append(" >= ").append(tsStart).append(" and ");
    where.append(Datapoint.TS_START).append(" < ").append(tsStart);
    sortOrder = Datapoint.TS_START + " asc ";
    c = queryDatapointInternal(where.toString(), null, null, sortOrder, null);

    if (c.getCount() < 1) {
      c.close();
      return;
    }

    // loops through all datapoints updating stats
    c.moveToFirst();
    int count = c.getCount();
    for (int i = 0; i < count; i++) {
      id = Datapoint.getIdIdx(c);

      // set the aggregated stats
      int length = Datapoint.AGGREGATE_SUFFIX.length;
      for (int j = 0; j < length; j++) {
        String suffix = Datapoint.AGGREGATE_SUFFIX[j];
        newPeriodStart = Datapoint.getTsStartIdx(c, suffix);
        newPeriodEnd = Datapoint.getTsEndIdx(c, suffix);
        value = Datapoint.getValueIdx(c, suffix);
        entries = Datapoint.getEntriesIdx(c, suffix);

        // fetch data for the previous entry as aggregated for the period
        
        where.setLength(0);
        where.append(Datapoint.TIMESERIES_ID).append(" = ").append(timeSeriesId).append(" and ");
        where.append(Datapoint.TS_START).append(" < ").append(newPeriodStart).append(" ");
        sortOrder = Datapoint.TS_START + " desc ";
        String groupBy = Datapoint.TS_START + "_" + suffix;
        limit = "" + history;
        Cursor c2 = queryDatapointInternal(where.toString(), null, groupBy, sortOrder, limit);

        c2.moveToLast();
        int oldPeriodStart = Datapoint.getTsStartIdx(c2, suffix);
        double oldValue = Datapoint.getValueIdx(c2, suffix);
        double oldValueSum = Datapoint.getSumValueIdx(c2, suffix);
        double oldValueSumSqr = Datapoint.getSumValueSqrIdx(c2, suffix);
        int oldEntries = Datapoint.getEntriesIdx(c2, suffix);
        int oldEntriesSum = Datapoint.getSumEntriesIdx(c2, suffix);
        double oldTrend = Datapoint.getTrendIdx(c2, suffix);
        double trend;

        if (newPeriodStart > oldPeriodStart) {
          // restart the per-period counters
          trend = calculateTrend(value, oldTrend, smoothing);
          Datapoint.setTrend(values, suffix, trend);
        } else {
          // add to the per-period counters
          if (c2.getCount() < 2) {
            // there was a previous entry, but it was in this same period,
            // so there's no entry before this for the period
            Datapoint.setTrend(values, suffix, oldValue + value);
          } else {
            trend = calculateTrend(oldValue + value, oldTrend, smoothing);
            Datapoint.setTrend(values, suffix, trend);
          }                
        }
          
        // calculate standard deviation based on history
        double newValueSum = oldValueSum + value;
        double newValueSumSqr = oldValueSumSqr + (value * value);
        int newEntriesSum = oldEntriesSum + entries;

        double firstValueSum = 0.0f;
        double firstValueSumSqr = 0.0f;
        int firstEntriesSum = 0;
        if (c2.getCount() == history) {
          // only subtract out the initial values if we have a full history
          // window
          c2.moveToLast();
          firstValueSum = Datapoint.getSumValueIdx(c2, suffix);
          firstValueSumSqr = Datapoint.getSumValueSqrIdx(c2, suffix);
          firstEntriesSum = Datapoint.getSumEntriesIdx(c2, suffix);
        }
        double stddev = calculateStdDev(firstValueSumSqr, newValueSumSqr,
            firstValueSum, newValueSum, firstEntriesSum, newEntriesSum);
        Datapoint.setStdDev(values, suffix, stddev);
        c2.close();
      }
      
      id = db.update(Datapoint.TABLE_NAME, values, Datapoint._ID, 
          new String[] { "" + id } );      
    }
    c.close();

    return;
  }
      
  private void updateAggregations(SQLiteDatabase db, long timeSeriesId, 
      int fromTimestamp, double value, int entries) throws Exception {
    String[] aggregations = Datapoint.AGGREGATE_SUFFIX;
    String suffix;
    StringBuilder sql = new StringBuilder();
    
    double valSqr = value * value;

    sql.append("update ").append(Datapoint.TABLE_NAME).append(" set ");
    sql.append(Datapoint.SUM_VALUE).append(" = ");
    sql.append(Datapoint.SUM_VALUE).append(" + ").append(value).append(", ");
    sql.append(Datapoint.SUM_VALUE_SQR).append(" = ");
    sql.append(Datapoint.SUM_VALUE_SQR).append(" + ").append(valSqr).append(", ");
    sql.append(Datapoint.SUM_ENTRIES).append(" = ");
    sql.append(Datapoint.SUM_ENTRIES).append(" + ").append(entries).append(" ");

    for (int i = 0; i < aggregations.length; i++) {
      suffix = Datapoint.AGGREGATE_SUFFIX[i];
      sql.append(", ").append(Datapoint.VALUE).append("_").append(suffix).append(" = ");
      sql.append(Datapoint.VALUE).append("_").append(suffix).append(" + ").append(value);
      sql.append(", ").append(Datapoint.SUM_VALUE_SQR).append("_").append(suffix).append(" = ");
      sql.append(Datapoint.SUM_VALUE_SQR).append("_").append(suffix).append(" + ").append(valSqr);
      sql.append(", ").append(Datapoint.SUM_ENTRIES).append("_").append(suffix).append(" = ");
      sql.append(Datapoint.SUM_ENTRIES).append("_").append(suffix).append(" + ").append(entries);
    }
    sql.append(" where ").append(Datapoint.TIMESERIES_ID).append(" == ").append(timeSeriesId).append(" and ");
    sql.append(Datapoint.TS_START).append(" > ").append(fromTimestamp);
        
    db.rawQuery(sql.toString(), null);
    return;
  }

  private double calculateTrend(double newValue, double oldTrend, double smoothing) {
    // T(n) = T(n-1) + (smoothing * (V(n) - T(n-1)))
    return oldTrend + (smoothing * (newValue - oldTrend));
  }
  
  private double calculateStdDev(double firstSumSqr, double lastSumSqr,
      double firstValueSum, double lastValueSum, int firstEntriesSum, 
      int lastEntriesSum) {
    double sumSqr = lastSumSqr - firstSumSqr;
    double sumValue = lastValueSum - firstValueSum;
    double entriesSum = lastEntriesSum - firstEntriesSum;
    double mean = sumValue / entriesSum;
    
    double stddev = Math.sqrt((sumSqr / entriesSum) - (mean * mean));
    return stddev;
  }
    
  private void setBaseContentValues(ContentValues values, int timestamp, double value,
      int entries) {
    int period, periodStart, periodEnd;
    
    // set the non-aggregated stats
    Datapoint.setTrend(values, value);
    Datapoint.setStdDev(values, 0.0f);
    Datapoint.setSumValueSqr(values, value * value);              
    Datapoint.setSumEntries(values, entries);
    Datapoint.setSumValue(values, value);

    // set the aggregated stats, skip index 0 (no aggregation)
    int length = TimeSeries.AGGREGATION_PERIOD_TIMES.length;
    for (int i = 1; i < length; i++) {
      String suffix = Datapoint.AGGREGATE_SUFFIX[i-1];
      period = TimeSeries.AGGREGATION_PERIOD_TIMES[i];
      periodStart = mDateMap.secondsOfPeriodStart(timestamp, period);
      periodEnd = mDateMap.secondsOfPeriodEnd(periodStart, period);
      
      Datapoint.setTsStart(values, suffix, periodStart);
      Datapoint.setTsEnd(values, suffix, periodEnd);
      Datapoint.setValue(values, suffix, value);
      Datapoint.setEntries(values, suffix, entries);
      Datapoint.setTrend(values, suffix, value);
      Datapoint.setStdDev(values, suffix, 0.0f);
      Datapoint.setSumEntries(values, suffix, entries);
      Datapoint.setSumValue(values, suffix, value);
      Datapoint.setSumValueSqr(values, suffix, value * value);              
    }

    return;
  }
  
  private void setRawContentValuesStats(Cursor c, ContentValues values,
      long timeSeriesId, double value, int entries, double smoothing, int history) {
    // these are in reverse chronological order
    c.moveToFirst();

    // update the non-aggregated stats
    double oldValue = Datapoint.getValueIdx(c, null);
    double oldValueSum = Datapoint.getSumValueIdx(c, null);
    double oldValueSumSqr = Datapoint.getSumValueSqrIdx(c, null);
    int oldEntries = Datapoint.getEntriesIdx(c, null);
    int oldEntriesSum = Datapoint.getSumEntriesIdx(c, null);
    double oldTrend = Datapoint.getTrendIdx(c, null);

    double newValueSum = oldValueSum + value;
    double newValueSumSqr = oldValueSumSqr + (value * value);
    int newEntriesSum = oldEntriesSum + entries;

    double trend = calculateTrend(value, oldTrend, smoothing);
    Datapoint.setTrend(values, trend);
    Datapoint.setSumEntries(values, newEntriesSum);
    Datapoint.setSumValue(values, newValueSum);
    Datapoint.setSumValueSqr(values, newValueSumSqr);

    double firstValueSum = 0.0f;
    double firstValueSumSqr = 0.0f;
    int firstEntriesSum = 0;
    if (c.getCount() == history) {
      // only subtract out the initial values if we have a full history window
      c.moveToLast();
      firstValueSum = Datapoint.getSumValueIdx(c, null);
      firstValueSumSqr = Datapoint.getSumValueSqrIdx(c, null);
      firstEntriesSum = Datapoint.getSumEntriesIdx(c, null);
    }
    double stddev = calculateStdDev(firstValueSumSqr, newValueSumSqr,
        firstValueSum, newValueSum, firstEntriesSum, newEntriesSum);
    Datapoint.setStdDev(values, stddev);

    return;
  }

  private Cursor getPreviousDatapoints(SQLiteDatabase db, long timeSeriesId, 
      long recordingDatapointId, int timestamp, int history) {
    StringBuilder where = new StringBuilder();

    where.append(Datapoint.TIMESERIES_ID).append(" = ").append(timeSeriesId).append(" and ");
    where.append(Datapoint.TS_START).append(" < ").append(timestamp).append(" and ");
    where.append(Datapoint._ID).append(" != ").append(recordingDatapointId).append(" ");
    String sortOrder = Datapoint.TS_START + " desc ";
    String groupBy = Datapoint.TS_START;
    String limit = "" + history;
    return queryDatapointInternal(where.toString(), null, groupBy, sortOrder, limit);
  }

  private void setAggregatedContentValuesStats(ContentValues values,
      long timeSeriesId, long recordingDatapointId, int timestamp, 
      int lastTimestamp, double value, int entries, 
      double smoothing, int history) {
    double oldValue, oldValueSum, oldValueSumSqr;
    int oldEntries, oldEntriesSum;
    double newTrend, oldTrend;
    double newValueSum, newValueSumSqr;
    int newEntriesSum;
    int period, oldPeriodStart, newPeriodStart, oldPeriodEnd, newPeriodEnd;
    double firstValueSum, firstValueSumSqr;
    double stddev;
    int firstEntriesSum;
    long id;
    Uri sumsUri;
    Cursor c;
    StringBuilder where = new StringBuilder();

    // update the aggregated stats, skip index 0 (no aggregation)
    int length = TimeSeries.AGGREGATION_PERIOD_TIMES.length;
    for (int i = 1; i < length; i++) {
      String suffix = Datapoint.AGGREGATE_SUFFIX[i-1];
      period = TimeSeries.AGGREGATION_PERIOD_TIMES[i];
      newPeriodStart = mDateMap.secondsOfPeriodStart(timestamp, period);
      newPeriodEnd = mDateMap.secondsOfPeriodEnd(newPeriodStart, period);
      oldPeriodStart = mDateMap.secondsOfPeriodStart(lastTimestamp, period);

      // fetch data for the previous entry as aggregated for the period
      where.setLength(0);
      where.append(Datapoint.TIMESERIES_ID).append(" = ").append(timeSeriesId).append(" and ");
      where.append(Datapoint._ID).append(" != ").append(recordingDatapointId).append(" ");
      String groupBy = Datapoint.TS_START + "_" + suffix + " ";
      String sortOrder = Datapoint.TS_START + " desc ";
      String limit = "" + history;
      c = queryDatapointInternal(where.toString(), null, groupBy, sortOrder, limit);      
      
      c.moveToFirst();
      oldValue = Datapoint.getValueIdx(c, suffix);
      oldValueSum = Datapoint.getSumValueIdx(c, suffix);
      oldValueSumSqr = Datapoint.getSumValueSqrIdx(c, suffix);
      oldEntries = Datapoint.getEntriesIdx(c, suffix);
      oldEntriesSum = Datapoint.getSumEntriesIdx(c, suffix);
      oldTrend = Datapoint.getTrendIdx(c, suffix);

      Datapoint.setTsStart(values, suffix, newPeriodStart);
      Datapoint.setTsEnd(values, suffix, newPeriodEnd);
      if (newPeriodStart > oldPeriodStart) {
        // restart the per-period counters
        Datapoint.setValue(values, suffix, value);
        Datapoint.setEntries(values, suffix, entries);
        newTrend = calculateTrend(value, oldTrend, smoothing);
        Datapoint.setTrend(values, suffix, newTrend);
      } else {
        // add to the per-period counters
        if (c.getCount() < 2) {
          // there was a previous entry, but it was in this same period,
          // so there's no entry before this for the period
          Datapoint.setTrend(values, suffix, oldValue + value);
        } else {
          newTrend = calculateTrend(oldValue + value, oldTrend, smoothing);
          Datapoint.setTrend(values, suffix, newTrend);
        }                
        Datapoint.setValue(values, suffix, oldValue + value);
        Datapoint.setEntries(values, suffix, oldEntries + entries);
      }
        
      // calculate standard deviation based on history
      newValueSum = oldValueSum + value;
      newValueSumSqr = oldValueSumSqr + (value * value);
      newEntriesSum = oldEntriesSum + entries;

      Datapoint.setSumEntries(values, suffix, newEntriesSum);
      Datapoint.setSumValue(values, suffix, newValueSum);
      Datapoint.setSumValueSqr(values, suffix, newValueSumSqr);

      firstValueSum = 0.0f;
      firstValueSumSqr = 0.0f;
      firstEntriesSum = 0;
      if (c.getCount() == history) {
        // only subtract out the initial values if we have a full history
        // window
        c.moveToLast();
        firstValueSum = Datapoint.getSumValueIdx(c, suffix);
        firstValueSumSqr = Datapoint.getSumValueSqrIdx(c, suffix);
        firstEntriesSum = Datapoint.getSumEntriesIdx(c, suffix);
      }
      stddev = calculateStdDev(firstValueSumSqr, newValueSumSqr,
          firstValueSum, newValueSum, firstEntriesSum, newEntriesSum);
      Datapoint.setStdDev(values, suffix, stddev);
      c.close();
    }

    return;
  }

  private void setContentValues(SQLiteDatabase db, ContentValues values,
      long timeSeriesId, long recordingDatapointId, int tsStart, double value, 
      int entries, double smoothing, int history) {
    Cursor c = getPreviousDatapoints(db, timeSeriesId, recordingDatapointId, tsStart, history);
    if (c.getCount() < 1) {
      // no earlier entries for this timestamp
      c.close();
      setBaseContentValues(values, tsStart, value, entries);
    }
    else {
      // there exists an entry before this entry, chronologically
      // update the raw, un-aggregated stats:
      setRawContentValuesStats(c, values, timeSeriesId, value, 
          entries, smoothing, history);

      c.moveToFirst();
      int oldTsStart = Datapoint.getTsStart(c);
      c.close();            

      // update the aggregated stats
      setAggregatedContentValuesStats(values, timeSeriesId, recordingDatapointId,
          tsStart, oldTsStart, value, entries, smoothing, history);
    }
  }

  @Override
  public Uri insert(Uri uri, ContentValues values) {
    Uri outputUri = null;
    long id;
    int period, oldPeriodStart, newPeriodStart, oldPeriodEnd, newPeriodEnd;
    SQLiteDatabase db = mDbHelper.getWritableDatabase();
    
    switch (sURIMatcher.match(uri)) {
      case TIMESERIES:
        LockUtil.waitForLock(mLock);
        try {
          id = db.insert(TimeSeries.TABLE_NAME, null, values);
          if (id == -1) {
            outputUri = null;
          } else {
            outputUri = ContentUris.withAppendedId(TimeSeries.CONTENT_URI, id);
          }          
        } catch (Exception e) {
          Log.v(TAG, e.getMessage());
        } finally {
          LockUtil.unlock(mLock);
        }
        
        break;
      case DATAPOINTS:
        Long timeSeriesId = values.getAsLong(Datapoint.TIMESERIES_ID);
        if (timeSeriesId < 1) {
          throw new IllegalArgumentException("insert: Invalid URI " + uri);
        }

        LockUtil.waitForLock(mLock);
        db.beginTransaction();

        try {
          int oldTsStart;
          int tsStart = values.getAsInteger(Datapoint.TS_START);
          double value = values.getAsDouble(Datapoint.VALUE);
          int entries = values.getAsInteger(Datapoint.ENTRIES);

          Datapoint.setTimeSeriesId(values, timeSeriesId);
          
          // first fetch the smoothing and history parameters of the timeseries
          Uri timeseriesParams = ContentUris.withAppendedId(
              TimeSeries.CONTENT_URI, timeSeriesId);
          Cursor c = query(timeseriesParams, 
              new String[] { TimeSeries.SMOOTHING, TimeSeries.HISTORY,
              TimeSeries.RECORDING_DATAPOINT_ID }, 
              null, null, null);
          c.moveToFirst();
          double smoothing = TimeSeries.getSmoothing(c);
          int history = TimeSeries.getHistory(c);
          long recordingDatapointId = TimeSeries.getRecordingDatapointId(c);
          c.close();

          setContentValues(db, values, timeSeriesId, recordingDatapointId,
              tsStart, value, entries, smoothing, history);
          
          id = db.insert(Datapoint.TABLE_NAME, null, values);
          if (id == -1) {
            outputUri = null;
          } else {
            outputUri = ContentUris.withAppendedId(
                TimeSeriesData.TimeSeries.CONTENT_URI, timeSeriesId).buildUpon()
                .appendPath("datapoints").appendPath(""+id).build();
          }
          
          // TODO: update for new schema
          updateAggregations(db, timeSeriesId, tsStart, value, entries);
          updateFormulaData(db, timeSeriesId, tsStart, null);

          db.setTransactionSuccessful();
        } catch (Exception e) {
          Log.v(TAG, e.getMessage());
          outputUri = null;
        } finally {
          db.endTransaction();
          LockUtil.unlock(mLock);
        }

        break;
      default:
        throw new IllegalArgumentException("insert: Unknown URI " + uri);
    }      
    
    if (outputUri != null)
      getContext().getContentResolver().notifyChange(outputUri, null);
    
    return outputUri;
  }
  
  // TODO: move constant strings to defined values
  private HashMap<String, String> fetchProjectionMap(String aggregation) {
    if (aggregation == null) {
      return sDatapointProjection;
    } 
    else if (aggregation.equals("day")) {
      return sDatapointProjectionDay;
    }
    else if (aggregation.equals("week")) {
      return sDatapointProjectionWeek;
    }
    else if (aggregation.equals("month")) {
      return sDatapointProjectionDay;
    }
    else if (aggregation.equals("quarter")) {
      return sDatapointProjectionDay;
    }
    else if (aggregation.equals("year")) {
      return sDatapointProjectionDay;
    }
    return sDatapointProjection;
  }

  private String fetchGroupByString(String aggregation) {
    if (aggregation == null) {
      return Datapoint.TS_START;
    } 
    else if (aggregation.equals("day")
        || aggregation.equals("week")
        || aggregation.equals("month")
        || aggregation.equals("quarter")
        || aggregation.equals("year")) {
      return Datapoint.TS_START + "_" + aggregation;
    }
    return Datapoint.TS_START;
  }

  private Cursor queryDatapointInternal(String selection, String[] selectionArgs,
      String groupBy, String sortOrder, String limit) {
    SQLiteDatabase db = mDbHelper.getReadableDatabase();
    StringBuilder sql = new StringBuilder();
    sql.append("select ").append(Datapoint.DATAPOINT_SELECT_ALL_COLUMNS);
    sql.append(" from ").append(Datapoint.TABLE_NAME).append(" ");
    if (TextUtils.isEmpty(selection) != true) {
      sql.append(" where ").append(selection).append(" ");
    }
    if (TextUtils.isEmpty(groupBy) != true) {
      sql.append(" group by ").append(groupBy).append(" ");
    }
    if (TextUtils.isEmpty(sortOrder) != true) {
      sql.append(" order by ").append(sortOrder).append(" ");
    }
    if (TextUtils.isEmpty(limit) != true) {
      sql.append(" limit ").append(limit).append(" ");
    }
    
    return db.rawQuery(sql.toString(), selectionArgs);
  }

  private Cursor queryInternal(Uri uri, String[] projection, String selection,
      String[] selectionArgs, String sortOrder, HashMap<String, String> map) {
    String agg;
    String orderBy = sortOrder;
    String groupBy = null;
    String limit = "";
    SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
    SQLiteDatabase db = mDbHelper.getReadableDatabase();
    
    if (map != null)
      qb.setProjectionMap(map);

    switch (sURIMatcher.match(uri)) {
      case TIMESERIES:
        qb.setTables(TimeSeries.TABLE_NAME);
        if (map == null)
          qb.setProjectionMap(sTimeSeriesProjection);
        if (TextUtils.isEmpty(sortOrder))
          orderBy = TimeSeries.DEFAULT_SORT_ORDER;
        break;
      case TIMESERIES_ID:
        qb.setTables(TimeSeries.TABLE_NAME);
        if (map == null)
          qb.setProjectionMap(sTimeSeriesProjection);
        qb.appendWhere(TimeSeries._ID + " = " + uri.getPathSegments().get(PATH_SEGMENT_TIMERSERIES_ID));
        if (TextUtils.isEmpty(sortOrder))
          orderBy = TimeSeries.DEFAULT_SORT_ORDER;
        break;
      case DATAPOINTS:
        qb.setTables(Datapoint.TABLE_NAME);
        qb.appendWhere(TimeSeries._ID + " = " + uri.getPathSegments().get(PATH_SEGMENT_TIMERSERIES_ID));
        if (map == null)
          qb.setProjectionMap(sDatapointProjection);
        if (TextUtils.isEmpty(sortOrder))
          orderBy = Datapoint.DEFAULT_SORT_ORDER;
        break;
      case DATAPOINTS_RECENT:
        String count = uri.getPathSegments().get(PATH_SEGMENT_DATAPOINT_RECENT_COUNT);
        qb.setTables(Datapoint.TABLE_NAME);

        orderBy = Datapoint.TS_START + " desc";
        if (map == null)
          qb.setProjectionMap(sDatapointProjection);
        try {
          agg = uri.getPathSegments().get(PATH_SEGMENT_DATAPOINT_RECENT_AGGREGATION);
          if (map == null)
            qb.setProjectionMap(fetchProjectionMap(agg));
          groupBy = fetchGroupByString(agg);
        } catch (Exception e) { } // nothing
        qb.appendWhere(Datapoint.TIMESERIES_ID + " = " + uri.getPathSegments().get(PATH_SEGMENT_TIMERSERIES_ID));
        limit = count;
        break;
      case DATAPOINTS_RANGE:
        String start = uri.getPathSegments().get(PATH_SEGMENT_DATAPOINT_RANGE_START);
        String end = uri.getPathSegments().get(PATH_SEGMENT_DATAPOINT_RANGE_END);
        qb.setTables(Datapoint.TABLE_NAME);

        orderBy = Datapoint.TS_START + " desc";
        if (map == null)
          qb.setProjectionMap(sDatapointProjection);
        try {
          agg = uri.getPathSegments().get(PATH_SEGMENT_DATAPOINT_RECENT_AGGREGATION);
          if (map != null)
            qb.setProjectionMap(fetchProjectionMap(agg));
          groupBy = fetchGroupByString(agg);
          if (sortOrder == null || TextUtils.isEmpty(sortOrder)) {
            orderBy = Datapoint.TS_START + " desc ";            
          }
        } catch (Exception e) { } // nothing
        
        qb.appendWhere(Datapoint.TIMESERIES_ID + " = " 
            + uri.getPathSegments().get(PATH_SEGMENT_TIMERSERIES_ID) + " AND ");
        qb.appendWhere(Datapoint.TS_START + " >= " + start + " AND ");
        qb.appendWhere(Datapoint.TS_START + " < " + end + " ");
        break;
      case DATAPOINTS_ID:
        qb.setTables(Datapoint.TABLE_NAME);
        if (map == null)
          qb.setProjectionMap(sDatapointProjection);
        qb.appendWhere(Datapoint.TIMESERIES_ID + "=" + uri.getPathSegments().get(PATH_SEGMENT_TIMERSERIES_ID) + " AND ");
        qb.appendWhere(Datapoint._ID + "=" + uri.getPathSegments().get(PATH_SEGMENT_DATAPOINT_ID));
        if (TextUtils.isEmpty(sortOrder))
          orderBy = Datapoint.DEFAULT_SORT_ORDER;
        break;
      case DATEMAP:
        qb.setTables(DateMap.TABLE_NAME);
        if (map == null)
          qb.setProjectionMap(sDatemapProjection);
        if (TextUtils.isEmpty(sortOrder))
          orderBy = DateMap.DEFAULT_SORT_ORDER;
        break;
      case DATEMAP_ID:
        qb.setTables(DateMap.TABLE_NAME);
        if (map == null)
          qb.setProjectionMap(sDatemapProjection);
        qb.appendWhere(DateMap._ID + " = " + uri.getPathSegments().get(PATH_SEGMENT_DATEMAP_ID));
        if (TextUtils.isEmpty(sortOrder))
          orderBy = DateMap.DEFAULT_SORT_ORDER;
        break;
      default:
        throw new IllegalArgumentException("query: Unknown URI " + uri);
    } 
    
    Cursor c = qb.query(db, projection, selection, selectionArgs, groupBy, null, orderBy, limit);
    return c;
  }
  
  @Override
  public Cursor query(Uri uri, String[] projection, String selection,
      String[] selectionArgs, String sortOrder) {
    Cursor c = queryInternal(uri, projection, selection, selectionArgs, 
        sortOrder, null);
    c.setNotificationUri(getContext().getContentResolver(), uri);
    return c;
  }

  // TODO: update for new schema
  @Override
  public int update(Uri uri, ContentValues values, String where,
      String[] whereArgs) {
    SQLiteDatabase db = mDbHelper.getWritableDatabase();
    String timeSeriesId;
    String datapointId;
    int count = 0;
    Cursor c;
    
    switch (sURIMatcher.match(uri)) {
      case TIMESERIES_ID:
        timeSeriesId = uri.getPathSegments().get(PATH_SEGMENT_TIMERSERIES_ID);

        LockUtil.waitForLock(mLock);
        db.beginTransaction();

        // fetch the some of the existing params of the timeseries to prevent
        // having to updates stats if they don't change.
        Uri current = ContentUris.withAppendedId(TimeSeries.CONTENT_URI, 
            Long.valueOf(timeSeriesId));
        c = query(uri, new String[] { TimeSeries.SMOOTHING, 
            TimeSeries.HISTORY, TimeSeries.FORMULA }, 
            null, null, null);
        c.moveToFirst();
        double oldSmoothing = TimeSeries.getSmoothing(c);
        int oldHistory = TimeSeries.getHistory(c);
        String oldFormula = TimeSeries.getFormula(c);
        c.close();

        try {
          double newSmoothing = oldSmoothing;
          int newHistory = oldHistory;
          String newFormula = oldFormula;
          
          if (values.containsKey(TimeSeries.SMOOTHING))
            newSmoothing = values.getAsDouble(TimeSeries.SMOOTHING);
          if (values.containsKey(TimeSeries.HISTORY))
            newHistory = values.getAsInteger(TimeSeries.HISTORY);
          if (values.containsKey(TimeSeries.FORMULA))
            newFormula = values.getAsString(TimeSeries.FORMULA);
                    
          count = db.update(TimeSeries.TABLE_NAME, values, TimeSeries._ID + "="
              + timeSeriesId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
              whereArgs);
          if (newSmoothing != oldSmoothing || newHistory != oldHistory) {
            updateStats(db, Long.valueOf(timeSeriesId), 0, Integer.MAX_VALUE);
          }
          
          if (TextUtils.isEmpty(newFormula) != true && newFormula != oldFormula) {
            updateFormula(db, Long.valueOf(timeSeriesId), values.getAsString(TimeSeries.FORMULA));
            updateFormulaData(db, Long.valueOf(timeSeriesId), 0, null);
          }
          
          db.setTransactionSuccessful();
        } catch (Exception e) {
          Log.v(TAG, e.getMessage());
          count = -1;
        } finally {
          db.endTransaction();
          LockUtil.unlock(mLock);
        }
        break;
      case DATAPOINTS_ID:
        timeSeriesId = uri.getPathSegments().get(PATH_SEGMENT_TIMERSERIES_ID);
        datapointId = uri.getPathSegments().get(PATH_SEGMENT_DATAPOINT_ID);
        long tsId = Long.valueOf(timeSeriesId);
        
        LockUtil.waitForLock(mLock);
        db.beginTransaction();
        
        try {
          SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
          qb.setProjectionMap(sDatapointProjection);
          qb.setTables(Datapoint.TABLE_NAME);
          qb.appendWhere(Datapoint._ID + " = " + datapointId + " ");

          c = qb.query(db, null, null, null, null, null, null, null);
          if (c == null || c.getCount() < 1) {
            if (c != null)
              c.close();
            throw new Exception("update: couldn't find source datapoint");
          }
          
          c.moveToFirst();
          int oldStart = Datapoint.getTsStart(c);
          double oldValue = Datapoint.getValue(c);
          int oldEntries = Datapoint.getEntries(c);

          int newStart = oldStart;
          double newValue = oldValue;
          int newEntries = oldEntries;
          if (values.containsKey(Datapoint.TS_START))
            newStart = values.getAsInteger(Datapoint.TS_START);
          if (values.containsKey(Datapoint.VALUE))
            newValue = values.getAsDouble(Datapoint.VALUE);
          if (values.containsKey(Datapoint.ENTRIES))
            newEntries = values.getAsInteger(Datapoint.ENTRIES);
          
          // fetch the smoothing and history parameters of the timeseries
          Uri timeseriesParams = ContentUris.withAppendedId(
              TimeSeries.CONTENT_URI, tsId);
          c = query(timeseriesParams, 
              new String[] { TimeSeries.SMOOTHING, TimeSeries.HISTORY,
              TimeSeries.RECORDING_DATAPOINT_ID }, 
              null, null, null);
          c.moveToFirst();
          double smoothing = TimeSeries.getSmoothing(c);
          int history = TimeSeries.getHistory(c);
          long recordingDatapointId = TimeSeries.getRecordingDatapointId(c);
          c.close();

          setContentValues(db, values, tsId, recordingDatapointId, newStart, 
              newValue, newEntries, smoothing, history);

          count = db.update(Datapoint.TABLE_NAME, values, Datapoint._ID + "=" + datapointId
              + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
              whereArgs);
                    
          updateAggregations(db, tsId, oldStart, -oldValue, -oldEntries);
          updateAggregations(db, tsId, newStart, newValue, newEntries);      
          updateStats(db, tsId, oldStart < newStart ? oldStart : newStart, 
              Integer.MAX_VALUE);
          updateFormulaData(db, tsId, oldStart < newStart ? oldStart : newStart, null);

          db.setTransactionSuccessful();
        } catch (Exception e) {
          Log.v(TAG, e.getMessage());
          count = -1;
        } finally {
          db.endTransaction();
          LockUtil.unlock(mLock);
        }
        break;
      default:
        throw new IllegalArgumentException("update: Unknown URI " + uri);
    }      

    if (count > 0)
      getContext().getContentResolver().notifyChange(uri, null);
    return count;
  }
  
  // TODO: update for new schema
  @Override
  public int delete(Uri uri, String where, String[] whereArgs) {
    SQLiteDatabase db = mDbHelper.getWritableDatabase();
    String seriesId, datapointId;
    int count;
    switch (sURIMatcher.match(uri)) {
      case TIMESERIES_ID:
        seriesId = uri.getPathSegments().get(PATH_SEGMENT_TIMERSERIES_ID);

        LockUtil.waitForLock(mLock);        
        db.beginTransaction();

        try {
          // TODO:  check to see if we're deleting a series that is the source
          // of a synthetic series
          
          // Delete datapoints associated with the timeseries
          count = db.delete(Datapoint.TABLE_NAME, Datapoint.TIMESERIES_ID + "=" 
              + seriesId, null);
          // and the timeseries meta-data
          db.delete(TimeSeries.TABLE_NAME, TimeSeries._ID + "=" + seriesId
              + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
              whereArgs);
          db.delete(FormulaCache.TABLE_NAME, FormulaCache.RESULT_SERIES + "=" + seriesId
              + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
              whereArgs);
          db.setTransactionSuccessful();
        } catch (Exception e) {
          Log.v(TAG, e.getMessage());
          count = -1;
        } finally {
          db.endTransaction();
          LockUtil.unlock(mLock);
        }

        break;
      case DATAPOINTS_ID:
        seriesId = uri.getPathSegments().get(PATH_SEGMENT_TIMERSERIES_ID);
        datapointId = uri.getPathSegments().get(PATH_SEGMENT_DATAPOINT_ID);
        long tsId = Long.valueOf(seriesId);
        
        LockUtil.waitForLock(mLock);
        db.beginTransaction();

        try {
          SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
          qb.setProjectionMap(sDatapointProjection);
          qb.setTables(Datapoint.TABLE_NAME);
          qb.appendWhere(Datapoint._ID + " = " + datapointId + " ");

          Cursor c = qb.query(db, null, null, null, null, null, null, null);
          if (c == null || c.getCount() < 1) {
            if (c != null)
              c.close();
            throw new Exception("update: couldn't find source datapoint");
          }
          
          c.moveToFirst();
          int oldStart = Datapoint.getTsStart(c);
          double oldValue = Datapoint.getValue(c);
          int oldEntries = Datapoint.getEntries(c);
          
          count = db.delete(Datapoint.TABLE_NAME, Datapoint._ID + "=" + datapointId
              + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
              whereArgs);
                    
          updateAggregations(db, tsId, oldStart, -oldValue, -oldEntries);
          updateStats(db, tsId, oldStart, Integer.MAX_VALUE);
          updateFormulaData(db, tsId, oldStart, null);

          db.setTransactionSuccessful();
        } catch (Exception e) {
          Log.v(TAG, e.getMessage());
          count = -1;
        } finally {
          db.endTransaction();
          LockUtil.unlock(mLock);
        }

        break;
      default:
        throw new IllegalArgumentException("delete: Unknown URI " + uri);
    }

    if (count > 0)
      getContext().getContentResolver().notifyChange(uri, null);
    return count;
  }

  private static class DatabaseHelper extends SQLiteOpenHelper {
    DatabaseHelper(Context context) {
      super(context, TimeSeriesData.DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
      db.execSQL(TimeSeries.TABLE_CREATE);
      db.execSQL(Datapoint.TABLE_CREATE);
      db.execSQL(DateMap.TABLE_CREATE);
      db.execSQL(FormulaCache.TABLE_CREATE);
    
      generateDateMapCacheData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      db.execSQL("drop table " + TimeSeries.TABLE_CREATE);
      db.execSQL("drop table " + Datapoint.TABLE_CREATE);
      db.execSQL("drop table " + DateMap.TABLE_CREATE);
      db.execSQL(TimeSeries.TABLE_CREATE);
      db.execSQL(Datapoint.TABLE_CREATE);
      db.execSQL(DateMap.TABLE_CREATE);
      db.execSQL(FormulaCache.TABLE_CREATE);
    }
    
    private void generateDateMapCacheData(SQLiteDatabase db) {
      Calendar cal = Calendar.getInstance();
      
      int dow;
      int secs;
      
      ContentValues values = new ContentValues();

      for (int yyyy = 2000; yyyy < 2020; yyyy++) {
        cal.set(Calendar.YEAR, yyyy);
        for (int mm = 0; mm < 12; mm++) {
          cal.set(Calendar.MONTH, mm);
          cal.set(Calendar.DAY_OF_MONTH, 1);
          cal.set(Calendar.HOUR_OF_DAY, 0);
          cal.set(Calendar.MINUTE, 0);
          cal.set(Calendar.SECOND, 0);
          cal.set(Calendar.MILLISECOND, 0);

          secs = (int) (cal.getTimeInMillis() / DateMap.SECOND_MS);
          dow = cal.get(Calendar.DAY_OF_WEEK);
 
          values.clear();
          values.put(TimeSeriesData.DateMap.YEAR, yyyy);
          values.put(TimeSeriesData.DateMap.MONTH, mm);
          values.put(TimeSeriesData.DateMap.DOW, dow);
          values.put(TimeSeriesData.DateMap.SECONDS, secs);

          db.insert(TimeSeriesData.DateMap.TABLE_NAME, null, values);
        }
      }
    }
  }
}