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
import net.redgeek.android.eventrend.util.Number;

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
    sDatapointProjection.put(Datapoint.TS_START, Datapoint.TS_START);
    sDatapointProjection.put(Datapoint.TS_END, Datapoint.TS_END);
    sDatapointProjection.put(Datapoint.VALUE, Datapoint.VALUE);
    sDatapointProjection.put(Datapoint.ENTRIES, Datapoint.ENTRIES);
    sDatapointProjection.put(Datapoint.TREND, Datapoint.TREND);
    sDatapointProjection.put(Datapoint.STDDEV, Datapoint.STDDEV);

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
    ArrayList<Long> sourceIds = new ArrayList<Long>();
    ArrayList<SeriesData> sources = new ArrayList<SeriesData>();
    SQLiteQueryBuilder qb;         
    String[] projection;
    Cursor c;
    int count;

    if (formula == null) {
      projection = new String[] { TimeSeries.FORMULA };
      qb = new SQLiteQueryBuilder();      
      qb.setTables(TimeSeries.TABLE_NAME);
      qb.setProjectionMap(sTimeSeriesProjection);
      c = qb.query(db, projection, TimeSeries._ID + " = ? ", 
          new String[] { "" + timeSeriesId }, null, null, null);
      c.moveToFirst();
      String f = TimeSeries.getFormula(c);
      c.close();
      if (f != null && f.equals("") == false) {
        formula = new Formula(f);
      }
    }
    
    if (formula != null) {
      // Get the source series ids:
      projection = new String[] { FormulaCache.SOURCE_SERIES };
      qb = new SQLiteQueryBuilder();
      qb.setTables(FormulaCache.TABLE_NAME);
      c = qb.query(db, projection, FormulaCache.RESULT_SERIES + " = ? ",
          new String[] { "" + timeSeriesId }, null, null, null);
      count = c.getCount();
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

      // Gather the data for each:
      projection = new String[] { Datapoint.TS_START, Datapoint.TS_END,
          Datapoint.VALUE, Datapoint.ENTRIES, };
      int size = sourceIds.size();
      for (int i = 0; i < size; i++) {
        long id = sourceIds.get(i);
        boolean avg = false;

        projection = new String[] { TimeSeries.FORMULA, TimeSeries.AGGREGATION };
        qb = new SQLiteQueryBuilder();
        qb.setTables(TimeSeries.TABLE_NAME);
        qb.setProjectionMap(sTimeSeriesProjection);
        c = qb.query(db, projection, TimeSeries._ID + " = ? ",
            new String[] { "" + id }, null, null, null);
        c.moveToFirst();
        if (TimeSeries.getAggregation(c).equals(TimeSeries.AGGREGATION_AVG))
          avg = true;
        c.close();

        qb = new SQLiteQueryBuilder();
        qb.setTables(Datapoint.TABLE_NAME);
        qb.setProjectionMap(sDatapointProjection);
        qb.appendWhere(Datapoint.TIMESERIES_ID + " = " + id);
        String orderBy = Datapoint.DEFAULT_SORT_ORDER;

        Datum d;
        SeriesData ts = new SeriesData();
        c = qb.query(db, projection, null, null, null, null, orderBy);
        count = c.getCount();
        c.moveToFirst();
        for (int j = 0; j < count; j++) {
          d = new Datum();
          d.mTsStart = Datapoint.getTsStart(c);
          d.mTsEnd = Datapoint.getTsEnd(c);
          d.mValue = Datapoint.getValue(c);
          int entries = Datapoint.getEntries(c);
          if (avg == true) {
            d.mValue /= entries;
          }
          ts.mData.add(d);
          c.moveToNext();
        }
        c.close();
        sources.add(ts);
      }

      SeriesData result = formula.apply(sources);

      // Store the results
      Datum d;
      ContentValues values = new ContentValues();
      count = db.delete(Datapoint.TABLE_NAME, Datapoint.TIMESERIES_ID + "="
          + timeSeriesId, null);
      size = result.mData.size();
      for (int i = 0; i < size; i++) {
        d = result.mData.get(i);
        values.clear();
        values.put(Datapoint.TS_START, d.mTsStart);
        values.put(Datapoint.TS_END, d.mTsEnd);
        values.put(Datapoint.VALUE, d.mValue);
        values.put(Datapoint.ENTRIES, 1);
        values.put(Datapoint.STDDEV, 0.0f);
        values.put(Datapoint.TREND, 0.0f);
        db.insert(Datapoint.TABLE_NAME, null, values);
        insertAggregations(db, timeSeriesId, d.mTsStart, d.mValue);
      }
      updateStats(db, timeSeriesId, 0);
    }
    // Get any series dependent on this one and recalc
    projection = new String[] { FormulaCache.RESULT_SERIES };
    qb = new SQLiteQueryBuilder();
    qb.setTables(FormulaCache.TABLE_NAME);
    c = qb.query(db, projection, FormulaCache.SOURCE_SERIES + " = ? ", 
        new String[] { "" + timeSeriesId}, null, null, null);
    count = c.getCount();
    c.moveToFirst();
    for (int i = 0; i < count; i++) {
      // TODO:  prevent recursing to ourself
      long id = FormulaCache.getResultSeries(c);
      updateFormulaData(db, id, null);
      c.moveToNext();
    }
    c.close();

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

  private void updateStats(SQLiteDatabase db, long timeSeriesId, int fromSeconds, 
      String table) {
    SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
    String[] projection = new String[] { 
        TimeSeries.SMOOTHING,
        TimeSeries.HISTORY,
    };
    
    qb.setTables(TimeSeries.TABLE_NAME);
    qb.setProjectionMap(sTimeSeriesProjection);
    qb.appendWhere(TimeSeries._ID + " = " + timeSeriesId);

    Cursor c = qb.query(db, projection, null, null, null, null, null);
    if (c == null)
      return;
    if (c.getCount() < 1) {
      c.close();
      return;
    }
      
    c.moveToFirst();
    int history = TimeSeries.getHistory(c);
    float smoothing = TimeSeries.getSmoothing(c);
    c.close();
    
    qb = new SQLiteQueryBuilder();
    qb.setProjectionMap(sDatapointProjection);
    qb.setTables(table);
    qb.appendWhere(Datapoint.TIMESERIES_ID + " = " + timeSeriesId + " AND ");
    qb.appendWhere(Datapoint.TS_START + " <= " + fromSeconds + " ");
    
    int startTimestamp = 0;
    c = qb.query(db, null, null, null, null, null, 
        Datapoint.TS_START + " desc ", "" + history);
    if (c == null)
      return;
    if (c.getCount() > 0) {
      c.moveToLast();
      startTimestamp = Datapoint.getTsStart(c);
    }
    c.close();
    
    qb = new SQLiteQueryBuilder();
    qb.setProjectionMap(sDatapointProjection);
    qb.setTables(table);
    qb.appendWhere(Datapoint.TIMESERIES_ID + " = " + timeSeriesId + " AND ");
    qb.appendWhere(Datapoint.TS_START + " >= " + startTimestamp + " ");

    c = qb.query(db, null, null, null, null, null, 
        Datapoint.TS_START + " asc ", null);
    if (c == null)
      return;
    if (c.getCount() < 1) {
      c.close();
      return;
    }

    ContentValues values = new ContentValues();
    Number.SmoothedTrend trend = new Number.SmoothedTrend(smoothing);
    Number.WindowedStdDev stats = new Number.WindowedStdDev(history);

    float value = 0.0f;
    long id = 0;
    int entries = 0;
    int count = c.getCount();
    c.moveToFirst();
    for (int i = 0; i < count && i < history * 2; i++) {
      id = Datapoint.getId(c);
      value = Datapoint.getValue(c);
      trend.update(value);
      stats.update(value);
      
      values.clear();
      values.put(Datapoint.TREND, trend.mTrend);
      values.put(Datapoint.STDDEV, stats.getStandardDev());
      db.update(table, values, Datapoint._ID + " = " + id, null);
      
      c.moveToNext();
    }
    c.close();

    return;
  }
    
  
  private void updateStats(SQLiteDatabase db, long timeSeriesId, int fromSeconds) {
    Cursor c;
    ContentValues values = new ContentValues();
    String table;
    long id;
    int period, oldPeriodStart, newPeriodStart;
    String[] tables = TimeSeriesData.Datapoint.AGGREGATE_TABLE_SUFFIX;

    updateStats(db, timeSeriesId, fromSeconds, TimeSeriesData.Datapoint.TABLE_NAME);
    for (int i = 0; i < tables.length; i++) {
      table = TimeSeriesData.Datapoint.TABLE_NAME + "_" + tables[i];
      updateStats(db, timeSeriesId, fromSeconds, table);
    }

    return;
  }
  
  private void insertAggregations(SQLiteDatabase db, long timeSeriesId, 
      int tsStart, float value) throws Exception {
    Cursor c;
    ContentValues values = new ContentValues();
    String table;
    long id;
    int period, periodStart, periodEnd;
    String[] tables = TimeSeriesData.Datapoint.AGGREGATE_TABLE_SUFFIX;
    SQLiteQueryBuilder qb;

    for (int i = 0; i < tables.length; i++) {
      period = (int) TimeSeriesData.Datapoint.AGGREGATE_TABLE_PERIOD[i];
      periodStart = mDateMap.secondsOfPeriodStart(tsStart, period);
      periodEnd = mDateMap.secondsOfPeriodEnd(periodStart, period);

      qb = new SQLiteQueryBuilder();
      qb.setProjectionMap(sDatapointProjection);

      table = TimeSeriesData.Datapoint.TABLE_NAME + "_" + tables[i];
      qb.setTables(table);

      // insert, or update within the same period
      qb.appendWhere(Datapoint.TIMESERIES_ID + " = " + timeSeriesId + " AND ");
      qb.appendWhere(Datapoint.TS_START + " == " + periodStart + " ");

      c = qb.query(db, null, null, null, null, null, null, null);
      if (c == null || c.getCount() < 1) {
        // insert
        c.moveToFirst();
        values.clear();
        values.put(TimeSeriesData.Datapoint.TIMESERIES_ID, timeSeriesId);
        values.put(TimeSeriesData.Datapoint.TS_START, periodStart);
        values.put(TimeSeriesData.Datapoint.TS_END, periodEnd);
        values.put(TimeSeriesData.Datapoint.VALUE, value);
        values.put(TimeSeriesData.Datapoint.ENTRIES, 1);
        values.put(TimeSeriesData.Datapoint.TREND, 0);
        values.put(TimeSeriesData.Datapoint.STDDEV, 0);
        id = db.insert(table, null, values);
        if (c != null)
          c.close();
        if (id == -1)
          throw new Exception("insert: couldn't insert new aggregate");
      } else {
        // update
        c.moveToFirst();
        id = TimeSeriesData.Datapoint.getId(c);
        int entries = TimeSeriesData.Datapoint.getEntries(c);
        float oldValue = TimeSeriesData.Datapoint.getValue(c);

        values.clear();
        values.put(TimeSeriesData.Datapoint.VALUE, oldValue + value);
        values.put(TimeSeriesData.Datapoint.ENTRIES, entries + 1);
        id = db.update(table, values, TimeSeriesData.Datapoint._ID + " = ? ",
            new String[] { "" + id });
        c.close();
        if (id == -1)
          throw new Exception("insert: couldn't update old aggregate");
      }
    }

    return;
  }
  
  private void recalcAggregation(SQLiteDatabase db, String table,
      int period, long timeSeriesId, int periodStart) throws Exception {
    ContentValues values = new ContentValues();    
    Cursor c;
    int id, periodEnd;
    String sql;
    
    if (values == null)
      throw new Exception("recalcAggregation: couldn't allocation memory");
    
    periodEnd = mDateMap.secondsOfPeriodEnd(periodStart, period);
    sql = "select sum(" + Datapoint.VALUE + "), sum(" + Datapoint.ENTRIES
        + ") from " + Datapoint.TABLE_NAME + " where "
        + Datapoint.TIMESERIES_ID + " = ? and " + Datapoint.TS_START
        + " >= ? and " + Datapoint.TS_START + " <= ?;";

    c = db.rawQuery(sql, new String[] { "" + timeSeriesId, "" + periodStart,
        "" + periodEnd });
    if (c == null || c.getCount() < 1) {
      if (c != null)
        c.close();
      throw new Exception("update: could not find source data to aggregate");
    }

    c.moveToFirst();
    float sum = c.getFloat(c.getColumnIndexOrThrow("sum(" + Datapoint.VALUE + ")"));
    int count = c.getInt(c.getColumnIndexOrThrow("sum(" + Datapoint.ENTRIES + ")"));
    c.close();

    values.clear();
    values.put(TimeSeriesData.Datapoint.VALUE, sum);
    values.put(TimeSeriesData.Datapoint.ENTRIES, count);
    id = db.update(table, values, TimeSeriesData.Datapoint.TS_START + " = ? ",
        new String[] { "" + periodStart });
    if (id == -1)
      throw new Exception("insert: couldn't update old aggregate");

    return;
  }

  
  private void updateAggregations(SQLiteDatabase db, long timeSeriesId, 
      int oldStart, int newStart) throws Exception {
    long id;
    int period, oldPeriodStart, newPeriodStart;
    String[] tables = TimeSeriesData.Datapoint.AGGREGATE_TABLE_SUFFIX;
    String table, sql;
    
    for (int i = 0; i < tables.length; i++) {
      period = (int) TimeSeriesData.Datapoint.AGGREGATE_TABLE_PERIOD[i];
      table = TimeSeriesData.Datapoint.TABLE_NAME + "_" + tables[i];

      oldPeriodStart = mDateMap.secondsOfPeriodStart(oldStart, period);
      newPeriodStart = mDateMap.secondsOfPeriodStart(newStart, period);
      
      recalcAggregation(db, table, period, timeSeriesId, oldPeriodStart);

      if (oldPeriodStart != newPeriodStart) {
        recalcAggregation(db, table, period, timeSeriesId, newPeriodStart);
      }
    }

    return;
  }

  private void removeFromAggregations(SQLiteDatabase db, long timeSeriesId, 
      int tsStart) throws Exception {
    long id;
    int period, periodStart;
    String[] tables = TimeSeriesData.Datapoint.AGGREGATE_TABLE_SUFFIX;
    String table, sql;
    
    for (int i = 0; i < tables.length; i++) {
      period = (int) TimeSeriesData.Datapoint.AGGREGATE_TABLE_PERIOD[i];
      table = TimeSeriesData.Datapoint.TABLE_NAME + "_" + tables[i];

      periodStart = mDateMap.secondsOfPeriodStart(tsStart, period);
      recalcAggregation(db, table, period, timeSeriesId, periodStart);
    }

    return;
  }
  
  @Override
  public Uri insert(Uri uri, ContentValues values) {
    Uri outputUri = null;
    long id;
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
          // insert into the base table
          id = db.insert(Datapoint.TABLE_NAME, null, values);
          if (id == -1) {
            outputUri = null;
          } else {
            outputUri = ContentUris.withAppendedId(
                TimeSeriesData.TimeSeries.CONTENT_URI, timeSeriesId).buildUpon()
                .appendPath("datapoints").appendPath(""+id).build();
          }
          
          int tsStart = 0;
          int tsEnd = 0;
          float newValue = 0.0f;
          
          if (values.containsKey(Datapoint.TS_START))
            tsStart = values.getAsInteger(Datapoint.TS_START);
          if (values.containsKey(Datapoint.VALUE))
            newValue = values.getAsFloat(TimeSeriesData.Datapoint.VALUE);
          if (values.containsKey(Datapoint.TS_END))
            tsEnd = values.getAsInteger(Datapoint.TS_END);

          insertAggregations(db, timeSeriesId, tsStart, newValue);
          updateStats(db, timeSeriesId, tsStart);
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

  @Override
  public Cursor query(Uri uri, String[] projection, String selection,
      String[] selectionArgs, String sortOrder) {
    String table, agg;
    String orderBy = sortOrder;
    String limit = "";
    SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
    SQLiteDatabase db = mDbHelper.getReadableDatabase();
    
    switch (sURIMatcher.match(uri)) {
      case TIMESERIES:
        qb.setTables(TimeSeries.TABLE_NAME);
        qb.setProjectionMap(sTimeSeriesProjection);
        if (TextUtils.isEmpty(sortOrder))
          orderBy = TimeSeries.DEFAULT_SORT_ORDER;
        break;
      case TIMESERIES_ID:
        qb.setTables(TimeSeries.TABLE_NAME);
        qb.setProjectionMap(sTimeSeriesProjection);
        qb.appendWhere(TimeSeries._ID + " = " + uri.getPathSegments().get(PATH_SEGMENT_TIMERSERIES_ID));
        if (TextUtils.isEmpty(sortOrder))
          orderBy = TimeSeries.DEFAULT_SORT_ORDER;
        break;
      case DATAPOINTS:
        qb.setTables(Datapoint.TABLE_NAME);
        qb.appendWhere(TimeSeries._ID + " = " + uri.getPathSegments().get(PATH_SEGMENT_TIMERSERIES_ID));
        qb.setProjectionMap(sDatapointProjection);
        if (TextUtils.isEmpty(sortOrder))
          orderBy = Datapoint.DEFAULT_SORT_ORDER;
        break;
      case DATAPOINTS_RECENT:
        String count = uri.getPathSegments().get(PATH_SEGMENT_DATAPOINT_RECENT_COUNT);
        table = Datapoint.TABLE_NAME;
        try {
          agg = uri.getPathSegments().get(PATH_SEGMENT_DATAPOINT_RECENT_AGGREGATION);
          if (agg != null && TextUtils.isEmpty(agg) == false) {
            table += "_" + agg;
          }
        } catch (Exception e) { } // nothing
        qb.setTables(table);
        qb.appendWhere(Datapoint.TIMESERIES_ID + " = " + uri.getPathSegments().get(PATH_SEGMENT_TIMERSERIES_ID));
        orderBy = Datapoint.TS_START + " desc";
        limit = count;
        break;
      case DATAPOINTS_RANGE:
        String start = uri.getPathSegments().get(PATH_SEGMENT_DATAPOINT_RANGE_START);
        String end = uri.getPathSegments().get(PATH_SEGMENT_DATAPOINT_RANGE_END);
        table = Datapoint.TABLE_NAME;
        try {
          agg = uri.getPathSegments().get(PATH_SEGMENT_DATAPOINT_RANGE_AGGREGATION);
          if (agg != null && TextUtils.isEmpty(agg) == false) {
            table += "_" + table;
          }
        } catch (Exception e) { } // nothing
        qb.setTables(table);
        qb.appendWhere(TimeSeries._ID + " = " 
            + uri.getPathSegments().get(PATH_SEGMENT_TIMERSERIES_ID) + " AND ");
        qb.appendWhere(Datapoint.TS_START + " >= " + start + " AND ");
        qb.appendWhere(Datapoint.TS_START + " < " + end + " ");
        break;
      case DATAPOINTS_ID:
        qb.setTables(Datapoint.TABLE_NAME);
        qb.appendWhere(Datapoint.TIMESERIES_ID + "=" + uri.getPathSegments().get(PATH_SEGMENT_TIMERSERIES_ID) + " AND ");
        qb.appendWhere(Datapoint._ID + "=" + uri.getPathSegments().get(PATH_SEGMENT_DATAPOINT_ID));
        if (TextUtils.isEmpty(sortOrder))
          orderBy = Datapoint.DEFAULT_SORT_ORDER;
        break;
      case DATEMAP:
        qb.setTables(DateMap.TABLE_NAME);
        qb.setProjectionMap(sDatemapProjection);
        if (TextUtils.isEmpty(sortOrder))
          orderBy = DateMap.DEFAULT_SORT_ORDER;
        break;
      case DATEMAP_ID:
        qb.setTables(DateMap.TABLE_NAME);
        qb.setProjectionMap(sDatemapProjection);
        qb.appendWhere(DateMap._ID + " = " + uri.getPathSegments().get(PATH_SEGMENT_DATEMAP_ID));
        if (TextUtils.isEmpty(sortOrder))
          orderBy = DateMap.DEFAULT_SORT_ORDER;
        break;
      default:
        throw new IllegalArgumentException("query: Unknown URI " + uri);
    } 
    
    Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy, limit);
    c.setNotificationUri(getContext().getContentResolver(), uri);
    
    return c;
  }

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
            updateStats(db, Long.valueOf(timeSeriesId), 0);
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
          float oldValue = Datapoint.getTsStart(c);

          count = db.update(Datapoint.TABLE_NAME, values, Datapoint._ID + "=" + datapointId
              + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
              whereArgs);
          
          int newStart = oldStart;
          float newValue = oldValue;
          if (values.containsKey(Datapoint.TS_START))
            newStart = values.getAsInteger(Datapoint.TS_START);
          if (values.containsKey(Datapoint.VALUE))
            newValue = values.getAsFloat(TimeSeriesData.Datapoint.VALUE);
          
          updateAggregations(db, tsId, oldStart, newStart);
          updateStats(db, tsId, oldStart);
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
          float oldValue = Datapoint.getTsStart(c);
          
          count = db.delete(Datapoint.TABLE_NAME, Datapoint._ID + "=" + datapointId
              + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
              whereArgs);
          
          removeFromAggregations(db, tsId, oldStart);
          updateStats(db, tsId, oldStart);
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
      db.execSQL(Datapoint.TABLE_CREATE_DAY);
      db.execSQL(Datapoint.TABLE_CREATE_WEEK);
      db.execSQL(Datapoint.TABLE_CREATE_MONTH);
      db.execSQL(Datapoint.TABLE_CREATE_QUARTER);
      db.execSQL(Datapoint.TABLE_CREATE_YEAR);
      db.execSQL(DateMap.TABLE_CREATE);
      db.execSQL(FormulaCache.TABLE_CREATE);
    
      generateDateMapCacheData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      db.execSQL("drop table " + TimeSeries.TABLE_CREATE);
      db.execSQL("drop table " + Datapoint.TABLE_CREATE);
      db.execSQL("drop table " + Datapoint.TABLE_CREATE_DAY);
      db.execSQL("drop table " + Datapoint.TABLE_CREATE_WEEK);
      db.execSQL("drop table " + Datapoint.TABLE_CREATE_MONTH);
      db.execSQL("drop table " + Datapoint.TABLE_CREATE_QUARTER);
      db.execSQL("drop table " + Datapoint.TABLE_CREATE_YEAR);
      db.execSQL("drop table " + DateMap.TABLE_CREATE);
      db.execSQL(TimeSeries.TABLE_CREATE);
      db.execSQL(Datapoint.TABLE_CREATE);
      db.execSQL(Datapoint.TABLE_CREATE_DAY);
      db.execSQL(Datapoint.TABLE_CREATE_WEEK);
      db.execSQL(Datapoint.TABLE_CREATE_MONTH);
      db.execSQL(Datapoint.TABLE_CREATE_QUARTER);
      db.execSQL(Datapoint.TABLE_CREATE_YEAR);
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