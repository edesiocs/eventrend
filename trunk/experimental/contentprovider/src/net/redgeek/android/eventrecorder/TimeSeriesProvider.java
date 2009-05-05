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
import android.net.Uri.Builder;
import android.os.Debug;
import android.text.TextUtils;
import android.util.Log;

import net.redgeek.android.eventrecorder.TimeSeriesData.Datapoint;
import net.redgeek.android.eventrecorder.TimeSeriesData.DateMap;
import net.redgeek.android.eventrecorder.TimeSeriesData.FormulaCache;
import net.redgeek.android.eventrecorder.TimeSeriesData.TimeSeries;
import net.redgeek.android.eventrecorder.synthetic.Formula;

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

  private void updateFormulaData(SQLiteDatabase db, long timeSeriesId, 
      Formula formula) throws Exception {
//    ArrayList<Long> sourceIds = new ArrayList<Long>();
//    ArrayList<SeriesData> sources = new ArrayList<SeriesData>();
//    SQLiteQueryBuilder qb;         
//    String[] projection;
//    Cursor c;
//    int count;
//
//    if (formula == null) {
//      projection = new String[] { TimeSeries.FORMULA };
//      qb = new SQLiteQueryBuilder();      
//      qb.setTables(TimeSeries.TABLE_NAME);
//      qb.setProjectionMap(sTimeSeriesProjection);
//      c = qb.query(db, projection, TimeSeries._ID + " = ? ", 
//          new String[] { "" + timeSeriesId }, null, null, null);
//      c.moveToFirst();
//      String f = TimeSeries.getFormula(c);
//      c.close();
//      if (f != null && f.equals("") == false) {
//        formula = new Formula(f);
//      }
//    }
//    
//    if (formula != null) {
//      // Get the source series ids:
//      projection = new String[] { FormulaCache.SOURCE_SERIES };
//      qb = new SQLiteQueryBuilder();
//      qb.setTables(FormulaCache.TABLE_NAME);
//      c = qb.query(db, projection, FormulaCache.RESULT_SERIES + " = ? ",
//          new String[] { "" + timeSeriesId }, null, null, null);
//      count = c.getCount();
//      c.moveToFirst();
//      for (int i = 0; i < count; i++) {
//        Long l = Long.valueOf(FormulaCache.getSourceSeries(c));
//        if (l == timeSeriesId) {
//          c.close();
//          throw new Exception("A synthetic series may not depend on itself!");
//        }
//        sourceIds.add(l);
//        c.moveToNext();
//      }
//      c.close();
//
//      // Gather the data for each:
//      projection = new String[] { Datapoint.TS_START, Datapoint.TS_END,
//          Datapoint.SUM, Datapoint.ENTRIES, };
//      int size = sourceIds.size();
//      for (int i = 0; i < size; i++) {
//        long id = sourceIds.get(i);
//        boolean avg = false;
//
//        projection = new String[] { TimeSeries.FORMULA, TimeSeries.AGGREGATION };
//        qb = new SQLiteQueryBuilder();
//        qb.setTables(TimeSeries.TABLE_NAME);
//        qb.setProjectionMap(sTimeSeriesProjection);
//        c = qb.query(db, projection, TimeSeries._ID + " = ? ",
//            new String[] { "" + id }, null, null, null);
//        c.moveToFirst();
//        if (TimeSeries.getAggregation(c).equals(TimeSeries.AGGREGATION_AVG))
//          avg = true;
//        c.close();
//
//        qb = new SQLiteQueryBuilder();
//        qb.setTables(Datapoint.TABLE_NAME);
//        qb.setProjectionMap(sDatapointProjection);
//        qb.appendWhere(Datapoint.TIMESERIES_ID + " = " + id);
//        String orderBy = Datapoint.DEFAULT_SORT_ORDER;
//
//        Datum d;
//        SeriesData ts = new SeriesData();
//        c = qb.query(db, projection, null, null, null, null, orderBy);
//        count = c.getCount();
//        c.moveToFirst();
//        for (int j = 0; j < count; j++) {
//          d = new Datum();
//          d.mTsStart = Datapoint.getTsStart(c);
//          d.mTsEnd = Datapoint.getTsEnd(c);
//          d.mValue = Datapoint.getSum(c);
//          int entries = Datapoint.getEntries(c);
//          if (avg == true) {
//            d.mValue /= entries;
//          }
//          ts.mData.add(d);
//          c.moveToNext();
//        }
//        c.close();
//        sources.add(ts);
//      }
//
//      SeriesData result = formula.apply(sources);
//
//      // Store the results
//      Datum d;
//      ContentValues values = new ContentValues();
//      count = db.delete(Datapoint.TABLE_NAME, Datapoint.TIMESERIES_ID + "="
//          + timeSeriesId, null);
//      size = result.mData.size();
//      for (int i = 0; i < size; i++) {
//        d = result.mData.get(i);
//        values.clear();
//        values.put(Datapoint.TS_START, d.mTsStart);
//        values.put(Datapoint.TS_END, d.mTsEnd);
//        values.put(Datapoint.SUM, d.mValue);
//        values.put(Datapoint.ENTRIES, 1);
//        values.put(Datapoint.SUMSQR, 0.0f);
//        values.put(Datapoint.TREND, 0.0f);
//        db.insert(Datapoint.TABLE_NAME, null, values);
//        insertAggregations(db, timeSeriesId, d.mTsStart, d.mValue);
//      }
//      updateStats(db, timeSeriesId, 0);
//    }
//    // Get any series dependent on this one and recalc
//    projection = new String[] { FormulaCache.RESULT_SERIES };
//    qb = new SQLiteQueryBuilder();
//    qb.setTables(FormulaCache.TABLE_NAME);
//    c = qb.query(db, projection, FormulaCache.SOURCE_SERIES + " = ? ", 
//        new String[] { "" + timeSeriesId}, null, null, null);
//    count = c.getCount();
//    c.moveToFirst();
//    for (int i = 0; i < count; i++) {
//      // TODO:  prevent recursing to ourself
//      long id = FormulaCache.getResultSeries(c);
//      updateFormulaData(db, id, null);
//      c.moveToNext();
//    }
//    c.close();

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
    
    updateFormulaData(db, timeSeriesId, f);
    
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

    Datapoint.setTimeSeriesId(values, timeSeriesId);
    
    SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
    qb.setTables(Datapoint.TABLE_NAME);
    qb.setProjectionMap(sDatapointProjectionAll);
    qb.appendWhere(Datapoint.TIMESERIES_ID + " = " + timeSeriesId + " and ");
    qb.appendWhere(Datapoint.TS_START + " <= " + tsStart);
    String orderBy = Datapoint.TS_START + " desc ";
    Cursor c = qb.query(db, null, null, null, null, null, orderBy, "2");
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
    qb = new SQLiteQueryBuilder();
    qb.setTables(Datapoint.TABLE_NAME);
    qb.setProjectionMap(sDatapointProjectionAll);
    qb.appendWhere(Datapoint.TIMESERIES_ID + " = " + timeSeriesId + " and ");
    qb.appendWhere(Datapoint.TS_START + " >= " + tsStart + " and ");
    qb.appendWhere(Datapoint.TS_START + " < " + tsStart);
    orderBy = Datapoint.TS_START + " asc ";
    c = qb.query(db, null, null, null, null, null, orderBy);
    if (c.getCount() < 1) {
      c.close();
      return;
    }

    // loops through all datapoints updating stats
    c.moveToFirst();
    int count = c.getCount();
    for (int i = 0; i < count; i++) {
      id = Datapoint.getId(c);

      // set the aggregated stats
      int length = Datapoint.AGGREGATE_SUFFIX.length;
      for (int j = 0; j < length; j++) {
        String suffix = Datapoint.AGGREGATE_SUFFIX[j];
        newPeriodStart = Datapoint.getTsStart(c, suffix);
        newPeriodEnd = Datapoint.getTsEnd(c, suffix);
        value = Datapoint.getValue(c, suffix);
        entries = Datapoint.getEntries(c, suffix);

        // fetch data for the previous entry as aggregated for the period
        qb = new SQLiteQueryBuilder();
        qb.setTables(Datapoint.TABLE_NAME);
        orderBy = Datapoint.TS_START + " desc ";
        qb.setProjectionMap(fetchProjectionMap(suffix));
        String groupBy = Datapoint.TS_START + "_" + suffix;
        qb.appendWhere(TimeSeries._ID + " = " + timeSeriesId + " AND ");
        qb.appendWhere(Datapoint.TS_START + " < " + newPeriodStart + " ");
        String limit = "" + history;
        
        Cursor c2 = qb.query(db, null, null, null, groupBy, null, orderBy, limit);
        c2.moveToLast();
        int oldPeriodStart = Datapoint.getTsStart(c2);
        double oldValue = Datapoint.getValue(c2);
        double oldValueSum = Datapoint.getSumValue(c2);
        double oldValueSumSqr = Datapoint.getSumValueSqr(c2);
        int oldEntries = Datapoint.getEntries(c2);
        int oldEntriesSum = Datapoint.getSumEntries(c2);
        double oldTrend = Datapoint.getTrend(c2);
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
          firstValueSum = Datapoint.getSumValue(c2, suffix);
          firstValueSumSqr = Datapoint.getSumValueSqr(c2, suffix);
          firstEntriesSum = Datapoint.getSumEntries(c2, suffix);
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
    String suffix, sql;

    double valSqr = value * value;
    sql = "update " + Datapoint.TABLE_NAME + " set "
      + Datapoint.SUM_VALUE + " = " + Datapoint.SUM_VALUE + " + " + value + ", "
      + Datapoint.SUM_VALUE_SQR + " = " + Datapoint.SUM_VALUE_SQR + " + " + valSqr + ", "
      + Datapoint.SUM_ENTRIES + " = " + Datapoint.SUM_ENTRIES + " + " + entries + " ";
    for (int i = 0; i < aggregations.length; i++) {
      suffix = Datapoint.AGGREGATE_SUFFIX[i];
      sql += ", " + Datapoint.VALUE + "_" + suffix + " = " 
                  + Datapoint.VALUE + "_" + suffix + " + " + value
          +  ", " + Datapoint.SUM_VALUE_SQR + "_" + suffix + " = " 
                  + Datapoint.SUM_VALUE_SQR + "_" + suffix + " + " + valSqr
          +  ", " + Datapoint.SUM_ENTRIES + "_" + suffix + " = " 
                  + Datapoint.SUM_ENTRIES + "_" + suffix + " + " + entries;
    }
    sql += " where " + Datapoint.TIMESERIES_ID + " == ? and "
      + Datapoint.TS_START + " > ?; ";

    
    db.rawQuery(sql, new String[] { "" + timeSeriesId, "" + fromTimestamp });
    return;
  }

  private void removeFromAggregations(SQLiteDatabase db, long timeSeriesId, 
      int tsStart) throws Exception {
//    long id;
//    int period, periodStart;
//    String[] tables = TimeSeriesData.Datapoint.AGGREGATE_SUFFIX;
//    String table, sql;
//    
//    for (int i = 0; i < tables.length; i++) {
//      period = (int) TimeSeriesData.Datapoint.AGGREGATE_TABLE_PERIOD[i];
//      table = TimeSeriesData.Datapoint.TABLE_NAME + "_" + tables[i];
//
//      periodStart = mDateMap.secondsOfPeriodStart(tsStart, period);
//      recalcAggregation(db, table, period, timeSeriesId, periodStart);
//    }
//
//    return;
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
    
  @Override
  public Uri insert(Uri uri, ContentValues values) {
    Uri outputUri = null;
    long id;
    int period, oldPeriodStart, newPeriodStart, oldPeriodEnd, newPeriodEnd;
    SQLiteDatabase db = mDbHelper.getWritableDatabase();
    
    Debug.startMethodTracing("cpInsert");
    
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
          
          SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
          qb.setTables(Datapoint.TABLE_NAME);
          qb.setProjectionMap(sDatapointProjectionAll);
          qb.appendWhere(Datapoint.TIMESERIES_ID + " = " + timeSeriesId + " and ");
          qb.appendWhere(Datapoint.TS_START + " < " + tsStart);
          String orderBy = Datapoint.TS_START + " desc";
          String limit = "1";        
          Cursor c = qb.query(db, null, null, null, null, null, orderBy, limit);
          if (c.getCount() < 1) {
            // no earlier entries for this timestamp
            c.close();
            
            // set the non-aggregated stats
            Datapoint.setTrend(values, value);
            Datapoint.setStdDev(values, 0.0f);
            Datapoint.setSumValueSqr(values, value * value);              
            Datapoint.setSumEntries(values, entries);
            Datapoint.setSumValue(values, value);

            // set the aggregated stats
            int length = Datapoint.AGGREGATE_SUFFIX.length;
            for (int i = 0; i < length; i++) {
              String suffix = Datapoint.AGGREGATE_SUFFIX[i];
              period = (int) Datapoint.AGGREGATE_TABLE_PERIOD[i];
              newPeriodStart = mDateMap.secondsOfPeriodStart(tsStart, period);
              newPeriodEnd = mDateMap.secondsOfPeriodEnd(newPeriodStart, period);
              
              Datapoint.setTsStart(values, suffix, newPeriodStart);
              Datapoint.setTsEnd(values, suffix, newPeriodEnd);
              Datapoint.setValue(values, suffix, value);
              Datapoint.setEntries(values, suffix, entries);
              Datapoint.setTrend(values, suffix, value);
              Datapoint.setStdDev(values, suffix, 0.0f);
              Datapoint.setSumEntries(values, suffix, 1);
              Datapoint.setSumValue(values, suffix, value);
              Datapoint.setSumValueSqr(values, suffix, value * value);              
            }
          }
          else {
            // there exists an entry before this entry, chronologically
            c.moveToFirst();
            oldTsStart = Datapoint.getTsStart(c);
            c.close();
            
            // fetch the smoothing parameter of the timeseries
            Uri smoothingUri = ContentUris.withAppendedId(TimeSeries.CONTENT_URI, timeSeriesId);
            Cursor c2 = query(smoothingUri, 
                new String[] { TimeSeries.SMOOTHING, TimeSeries.HISTORY }, 
                null, null, null);
            c2.moveToFirst();
            double smoothing = TimeSeries.getSmoothing(c2);
            int history = TimeSeries.getHistory(c2);
            c2.close();

            double oldValue, oldValueSum, oldValueSumSqr;
            int oldEntries, oldEntriesSum;
            double oldTrend;
            double newValueSum, newValueSumSqr;
            int newEntriesSum;

            // update the non-aggregated stats
            Uri sumsUri = ContentUris.withAppendedId(
                TimeSeries.CONTENT_URI, timeSeriesId).buildUpon()
                .appendPath("recent").appendPath("" + history).build();
            c2 = queryInternal(sumsUri, null, null, null, null, 
                sDatapointProjectionAll);
            c2.moveToFirst();
            oldValue = Datapoint.getValue(c2);
            oldValueSum = Datapoint.getSumValue(c2);
            oldValueSumSqr = Datapoint.getSumValueSqr(c2);
            oldEntries = Datapoint.getEntries(c2);
            oldEntriesSum = Datapoint.getSumEntries(c2);
            oldTrend = Datapoint.getTrend(c2);

            newValueSum = oldValueSum + value;
            newValueSumSqr = oldValueSumSqr + (value * value);
            newEntriesSum = oldEntriesSum + entries;

            double trend = calculateTrend(value, oldTrend, smoothing);
            Datapoint.setTrend(values, trend);
            Datapoint.setSumEntries(values, newEntriesSum);
            Datapoint.setSumValue(values, newValueSum);
            Datapoint.setSumValueSqr(values, newValueSumSqr);

            double firstValueSum = 0.0f;
            double firstValueSumSqr = 0.0f;
            int firstEntriesSum = 0;
            if (c2.getCount() == history) {
              // only subtract out the initial values if we have a full history
              // window
              c2.moveToLast();
              firstValueSum = Datapoint.getSumValue(c2);
              firstValueSumSqr = Datapoint.getSumValueSqr(c2);
              firstEntriesSum = Datapoint.getSumEntries(c2);
            }
            double stddev = calculateStdDev(firstValueSumSqr, newValueSumSqr,
                firstValueSum, newValueSum, firstEntriesSum, newEntriesSum);
            Datapoint.setStdDev(values, stddev);
            c2.close();

            // update the aggregated stats
            int length = Datapoint.AGGREGATE_SUFFIX.length;
            for (int i = 0; i < length; i++) {
              String suffix = Datapoint.AGGREGATE_SUFFIX[i];
              period = (int) TimeSeriesData.Datapoint.AGGREGATE_TABLE_PERIOD[i];
              newPeriodStart = mDateMap.secondsOfPeriodStart(tsStart, period);
              newPeriodEnd = mDateMap.secondsOfPeriodEnd(newPeriodStart, period);
              oldPeriodStart = mDateMap.secondsOfPeriodStart(oldTsStart, period);

              // fetch data for the previous entry as aggregated for the period
              sumsUri = ContentUris.withAppendedId(
                  TimeSeries.CONTENT_URI, timeSeriesId).buildUpon()
                  .appendPath("recent").appendPath("" + history).
                  appendPath(suffix).build();
              c2 = queryInternal(sumsUri, null, null, null, null, 
                  sDatapointProjectionAll);
              c2.moveToFirst();
              oldValue = Datapoint.getValue(c2);
              oldValueSum = Datapoint.getSumValue(c2);
              oldValueSumSqr = Datapoint.getSumValueSqr(c2);
              oldEntries = Datapoint.getEntries(c2);
              oldEntriesSum = Datapoint.getSumEntries(c2);
              oldTrend = Datapoint.getTrend(c2);

              Datapoint.setTsStart(values, suffix, newPeriodStart);
              Datapoint.setTsEnd(values, suffix, newPeriodEnd);
              if (newPeriodStart > oldPeriodStart) {
                // restart the per-period counters
                Datapoint.setValue(values, suffix, value);
                Datapoint.setEntries(values, suffix, entries);
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
              if (c2.getCount() == history) {
                // only subtract out the initial values if we have a full history
                // window
                c2.moveToLast();
                firstValueSum = Datapoint.getSumValue(c2, suffix);
                firstValueSumSqr = Datapoint.getSumValueSqr(c2, suffix);
                firstEntriesSum = Datapoint.getSumEntries(c2, suffix);
              }
              stddev = calculateStdDev(firstValueSumSqr, newValueSumSqr,
                  firstValueSum, newValueSum, firstEntriesSum, newEntriesSum);
              Datapoint.setStdDev(values, suffix, stddev);
              c2.close();
            }
          }
          
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
          updateFormulaData(db, timeSeriesId, null);

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
    
    Debug.stopMethodTracing();

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
          groupBy = Datapoint.TS_START + "_" + agg;
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
          groupBy = Datapoint.TS_START + "_" + agg;
          if (sortOrder == null || TextUtils.isEmpty(sortOrder)) {
            orderBy = Datapoint.TS_START + " desc ";            
          }
        } catch (Exception e) { } // nothing
        
        qb.appendWhere(TimeSeries._ID + " = " 
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
    
    switch (sURIMatcher.match(uri)) {
      case TIMESERIES_ID:
        timeSeriesId = uri.getPathSegments().get(PATH_SEGMENT_TIMERSERIES_ID);

        LockUtil.waitForLock(mLock);
        db.beginTransaction();

        try {
          count = db.update(TimeSeries.TABLE_NAME, values, TimeSeries._ID + "="
              + timeSeriesId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
              whereArgs);
          if (values.containsKey(TimeSeries.SMOOTHING) ||
              values.containsKey(TimeSeries.HISTORY)) {
            updateStats(db, Long.valueOf(timeSeriesId), 0, Integer.MAX_VALUE);
          }
          
          if (values.containsKey(TimeSeries.FORMULA)) {
            updateFormula(db, Long.valueOf(timeSeriesId), 
                values.getAsString(TimeSeries.FORMULA));
            updateFormulaData(db, Long.valueOf(timeSeriesId), null);
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

          count = db.update(Datapoint.TABLE_NAME, values, Datapoint._ID + "=" + datapointId
              + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
              whereArgs);
          
          int newStart = oldStart;
          double newValue = oldValue;
          int newEntries = oldEntries;
          if (values.containsKey(Datapoint.TS_START))
            newStart = values.getAsInteger(Datapoint.TS_START);
          if (values.containsKey(Datapoint.VALUE))
            newValue = values.getAsDouble(Datapoint.VALUE);
          if (values.containsKey(Datapoint.ENTRIES))
            newEntries = values.getAsInteger(Datapoint.ENTRIES);
          
          updateAggregations(db, tsId, oldStart, -oldValue, -oldEntries);
          updateAggregations(db, tsId, newStart, newValue, newEntries);      
          updateStats(db, tsId, oldStart < newStart ? oldStart : newStart, 
              Integer.MAX_VALUE);
          updateFormulaData(db, tsId, null);

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
          updateFormulaData(db, tsId, null);

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

          secs = (int) (cal.getTimeInMillis() / DateMapCache.SECOND_MS);
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