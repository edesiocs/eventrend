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

import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import net.redgeek.android.eventrecorder.DateMapCache.DateMapCacheEntry;
import net.redgeek.android.eventrecorder.TimeSeriesData.Datapoint;
import net.redgeek.android.eventrecorder.TimeSeriesData.DateMap;
import net.redgeek.android.eventrecorder.TimeSeriesData.TimeSeries;
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
import android.text.TextUtils;

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
    sTimeSeriesProjection.put(TimeSeries.INTERPOLATION, TimeSeries.INTERPOLATION);
    sTimeSeriesProjection.put(TimeSeries.UNITS, TimeSeries.UNITS);
    
    sDatapointProjection = new HashMap<String, String>();
    sDatapointProjection.put(Datapoint._ID, Datapoint._ID);
    sDatapointProjection.put(Datapoint.TIMESERIES_ID, Datapoint.TIMESERIES_ID);
    sDatapointProjection.put(Datapoint.TS_START, Datapoint.TS_START);
    sDatapointProjection.put(Datapoint.TS_END, Datapoint.TS_END);
    sDatapointProjection.put(Datapoint.VALUE, Datapoint.VALUE);

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

  private void updateAggregations(SQLiteDatabase db, long timeSeriesId, 
      int oldStart, int newStart, float oldValue, float newValue, 
      boolean update, boolean delete) throws Exception {
    Cursor c;
    ContentValues values = new ContentValues();
    String table;
    long id;
    int period, oldPeriodStart, newPeriodStart;
    String[] tables = TimeSeriesData.Datapoint.AGGREGATE_TABLE_SUFFIX;

    for (int i = 0; i < tables.length; i++) {
      SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
      qb.setProjectionMap(sDatapointProjection);

      table = TimeSeriesData.Datapoint.TABLE_NAME + "_" + tables[i];
      qb.setTables(table);

      period = (int) TimeSeriesData.Datapoint.AGGREGATE_TABLE_PERIOD[i];
      newPeriodStart = mDateMap.secondsOfPeriodStart(newStart, period);

      if (update == true || delete == true) {
        // updating or deleting, not inserting
        oldPeriodStart = mDateMap.secondsOfPeriodStart(oldStart, period);
 
        if (delete == false && oldPeriodStart == newPeriodStart) {
          qb.appendWhere(TimeSeries._ID + " = " + timeSeriesId + " AND ");
          qb.appendWhere(Datapoint.TS_START + " == " + oldPeriodStart + " ");
          c = qb.query(db, null, null, null, null, null, null, null);
          if (c == null || c.getCount() < 1) {
            if (c != null)
              c.close();
            throw new Exception("update: could not find old aggregate");
          }

          id = TimeSeriesData.Datapoint.getId(c);
          oldValue = TimeSeriesData.Datapoint.getValue(c);
          c.close();

          values.clear();
          values.put(TimeSeriesData.Datapoint.TIMESERIES_ID, timeSeriesId);
          values.put(TimeSeriesData.Datapoint.VALUE, oldValue + (oldValue - newValue));
          id = db.update(table, values, TimeSeriesData.Datapoint._ID + " = ? ",
              new String[] { "" + id });
          if (id == -1)
              throw new Exception("insert: couldn't update old aggregate");
        }
        else {
          // period has changed, have to subtract from old period and add to new
          // period (or we're deleting)
          SQLiteQueryBuilder qb2 = new SQLiteQueryBuilder();
          qb2.appendWhere(TimeSeries._ID + " = " + timeSeriesId + " AND ");
          qb2.appendWhere(Datapoint.TS_START + " == " + oldPeriodStart + " ");
          c = qb2.query(db, null, null, null, null, null, null, null);
          if (c == null || c.getCount() < 1) {
            if (c != null)
              c.close();
            throw new Exception("update: could not find old aggregate");
          }

          id = TimeSeriesData.Datapoint.getId(c);
          oldValue = TimeSeriesData.Datapoint.getValue(c);
          int entries = TimeSeriesData.Datapoint.getEntries(c);
          c.close();

          values.clear();
          values.put(TimeSeriesData.Datapoint.TIMESERIES_ID, timeSeriesId);
          values.put(TimeSeriesData.Datapoint.TS_START, oldPeriodStart);
          values.put(TimeSeriesData.Datapoint.TS_END, oldPeriodStart + period - 1);
          values.put(TimeSeriesData.Datapoint.VALUE, oldValue - newValue);
          values.put(TimeSeriesData.Datapoint.ENTRIES, entries - 1);
          id = db.update(table, values, TimeSeriesData.Datapoint._ID + " = ? ",
              new String[] { "" + id });
          if (id == -1)
              throw new Exception("insert: couldn't update old aggregate");
        }
      }
          
      if (delete != true) {
      // insert, or update within the same period
        qb.appendWhere(TimeSeries._ID + " = " + timeSeriesId + " AND ");
        qb.appendWhere(Datapoint.TS_START + " == " + newPeriodStart + " ");

        c = qb.query(db, null, null, null, null, null, null, null);
        if (c == null || c.getCount() < 1) {
          // insert
          values.clear();
          values.put(TimeSeriesData.Datapoint.TIMESERIES_ID, timeSeriesId);
          values.put(TimeSeriesData.Datapoint.TS_START, newPeriodStart);
          values.put(TimeSeriesData.Datapoint.TS_END, newPeriodStart + period
              - 1);
          values.put(TimeSeriesData.Datapoint.VALUE, newValue);
          values.put(TimeSeriesData.Datapoint.ENTRIES, 1);
          id = db.insert(table, null, values);
          if (c != null)
            c.close();
          if (id == -1)
            throw new Exception("insert: couldn't insert new aggregate");
        } else {
          // update
          id = TimeSeriesData.Datapoint.getId(c);
          int entries = TimeSeriesData.Datapoint.getEntries(c);
          oldValue = TimeSeriesData.Datapoint.getValue(c);

          values.clear();
          values.put(TimeSeriesData.Datapoint.TS_START, newPeriodStart);
          values.put(TimeSeriesData.Datapoint.TS_END, newPeriodStart + period
              - 1);
          values.put(TimeSeriesData.Datapoint.VALUE, oldValue + newValue);
          values.put(TimeSeriesData.Datapoint.ENTRIES, entries + 1);
          id = db.update(table, values, TimeSeriesData.Datapoint._ID + " = ? ",
              new String[] { "" + id });
          c.close();
          if (id == -1)
            throw new Exception("insert: couldn't update old aggregate");
        }
      }
    }

    return;
  }
  
  @Override
  public Uri insert(Uri uri, ContentValues values) {
    Uri outputUri = null;
    long id;
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

          // Only aggregate if we're adding a discrete event or a range event
          // that has an endpoint set.  We'll take care of updating aggregations
          // in 'update' when the end of the range event is set.
          if (tsEnd >= tsStart && tsEnd != 0) {     
            updateAggregations(db, timeSeriesId, 0, tsStart, 0, newValue, false, false);
          }
          
          db.setTransactionSuccessful();
        } catch (Exception e) {
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
        qb.appendWhere("_id=" + uri.getPathSegments().get(PATH_SEGMENT_TIMERSERIES_ID));
        if (TextUtils.isEmpty(sortOrder))
          orderBy = TimeSeries.DEFAULT_SORT_ORDER;
        break;
      case DATAPOINTS:
        qb.setTables(Datapoint.TABLE_NAME);
        qb.appendWhere(TimeSeries._ID + "=" + uri.getPathSegments().get(PATH_SEGMENT_TIMERSERIES_ID));
        qb.setProjectionMap(sDatapointProjection);
        if (TextUtils.isEmpty(sortOrder))
          orderBy = Datapoint.DEFAULT_SORT_ORDER;
        break;
      case DATAPOINTS_RECENT:
        String count = uri.getPathSegments().get(PATH_SEGMENT_DATAPOINT_RECENT_COUNT);
        table = Datapoint.TABLE_NAME;
        agg = uri.getPathSegments().get(PATH_SEGMENT_DATAPOINT_RECENT_AGGREGATION);
        if (agg != null && TextUtils.isEmpty(agg) == false) {
          table += "_" + table;
        }
        qb.setTables(table);
        qb.appendWhere(TimeSeries._ID + "=" + uri.getPathSegments().get(PATH_SEGMENT_TIMERSERIES_ID));
        orderBy = Datapoint.TS_START + " desc";
        limit = count;
        break;
      case DATAPOINTS_RANGE:
        String start = uri.getPathSegments().get(PATH_SEGMENT_DATAPOINT_RANGE_START);
        String end = uri.getPathSegments().get(PATH_SEGMENT_DATAPOINT_RANGE_END);
        table = Datapoint.TABLE_NAME;
        agg = uri.getPathSegments().get(PATH_SEGMENT_DATAPOINT_RANGE_AGGREGATION);
        if (agg != null && TextUtils.isEmpty(agg) == false) {
          table += "_" + table;
        }
        qb.setTables(table);
        qb.appendWhere(TimeSeries._ID + " = " 
            + uri.getPathSegments().get(PATH_SEGMENT_TIMERSERIES_ID) + " AND ");
        qb.appendWhere(Datapoint.TS_START + " >= " + start + " AND ");
        qb.appendWhere(Datapoint.TS_START + " < " + end + " ");
        break;
      case DATAPOINTS_ID:
        qb.setTables(Datapoint.TABLE_NAME);
        qb.appendWhere(TimeSeries._ID + "=" + uri.getPathSegments().get(PATH_SEGMENT_TIMERSERIES_ID));
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
        qb.appendWhere("_id=" + uri.getPathSegments().get(PATH_SEGMENT_DATEMAP_ID));
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
      case TIMESERIES:
        // TODO:  if smoothing or history change, recalc trend for whole series
        LockUtil.waitForLock(mLock);
        try {
          count = db.update(TimeSeries.TABLE_NAME, values, where, whereArgs);
        } catch (Exception e) {          
        } finally {
          LockUtil.unlock(mLock);
        }
        break;
      case TIMESERIES_ID:
        // TODO:  if smoothing or history change, recalc trend for whole series
        timeSeriesId = uri.getPathSegments().get(PATH_SEGMENT_TIMERSERIES_ID);

        LockUtil.waitForLock(mLock);
        try {
          count = db.update(TimeSeries.TABLE_NAME, values, TimeSeries._ID + "="
              + timeSeriesId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
              whereArgs);
        } catch (Exception e) {          
        } finally {
          LockUtil.unlock(mLock);
        }
        break;
//      case DATAPOINTS:
//      // TODO:  recalc trend, stddev for all affected series
//      // TODO:  fix aggregations on update
//        LockUtil.waitForLock(mLock);        
//        try {
//          count = db.update(Datapoint.TABLE_NAME, values, where, whereArgs);
//        } catch (Exception e) {
//        } finally {
//          LockUtil.unlock(mLock);
//        }
//        break;
      case DATAPOINTS_ID:
        // TODO:  recalc trend, stddev for all affected series
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
          
          updateAggregations(db, tsId, oldStart, newStart, oldValue, newValue, true, false);   
          
          db.setTransactionSuccessful();
        } catch (Exception e) {
        } finally {
          db.endTransaction();
          LockUtil.unlock(mLock);
        }
        break;
      default:
        throw new IllegalArgumentException("update: Unknown URI " + uri);
    }      

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
          // Delete datapoints associated with the timeseries
          count = db.delete(Datapoint.TABLE_NAME, Datapoint.TIMESERIES_ID + "=" 
              + seriesId, null);
          // and the timeseries meta-data
          count = db.delete(TimeSeries.TABLE_NAME, TimeSeries._ID + "=" + seriesId
              + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
              whereArgs);

          db.setTransactionSuccessful();
        } catch (Exception e) {
          count = -1;
        } finally {
          db.endTransaction();
          LockUtil.unlock(mLock);
        }

        break;
      case DATAPOINTS_ID:
        // TODO:  recalc trend, stddev for all affected series
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
          
          int oldStart = Datapoint.getTsStart(c);
          float oldValue = Datapoint.getTsStart(c);
          
          count = db.delete(Datapoint.TABLE_NAME, Datapoint._ID + "=" + datapointId
              + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
              whereArgs);
          
          updateAggregations(db, tsId, oldStart, 0, oldValue, 0, false, true);   

          db.setTransactionSuccessful();
        } catch (Exception e) {
          count = -1;
        } finally {
          db.endTransaction();
          LockUtil.unlock(mLock);
        }

        break;
      default:
        throw new IllegalArgumentException("delete: Unknown URI " + uri);
    }

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
      db.execSQL(Datapoint.TABLE_CREATE_HOUR);
      db.execSQL(Datapoint.TABLE_CREATE_AMPM);
      db.execSQL(Datapoint.TABLE_CREATE_DAY);
      db.execSQL(Datapoint.TABLE_CREATE_WEEK);
      db.execSQL(Datapoint.TABLE_CREATE_MONTH);
      db.execSQL(Datapoint.TABLE_CREATE_QUARTER);
      db.execSQL(Datapoint.TABLE_CREATE_YEAR);
      db.execSQL(Datapoint.TABLE_CREATE);
      db.execSQL(DateMap.TABLE_CREATE);
    
      generateDateMapCacheData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      db.execSQL("drop table " + TimeSeries.TABLE_CREATE);
      db.execSQL("drop table " + Datapoint.TABLE_CREATE);
      db.execSQL("drop table " + Datapoint.TABLE_CREATE_HOUR);
      db.execSQL("drop table " + Datapoint.TABLE_CREATE_AMPM);
      db.execSQL("drop table " + Datapoint.TABLE_CREATE_DAY);
      db.execSQL("drop table " + Datapoint.TABLE_CREATE_WEEK);
      db.execSQL("drop table " + Datapoint.TABLE_CREATE_MONTH);
      db.execSQL("drop table " + Datapoint.TABLE_CREATE_QUARTER);
      db.execSQL("drop table " + Datapoint.TABLE_CREATE_YEAR);
      db.execSQL("drop table " + DateMap.TABLE_CREATE);
      db.execSQL(TimeSeries.TABLE_CREATE);
      db.execSQL(Datapoint.TABLE_CREATE);
      db.execSQL(Datapoint.TABLE_CREATE_HOUR);
      db.execSQL(Datapoint.TABLE_CREATE_AMPM);
      db.execSQL(Datapoint.TABLE_CREATE_DAY);
      db.execSQL(Datapoint.TABLE_CREATE_WEEK);
      db.execSQL(Datapoint.TABLE_CREATE_MONTH);
      db.execSQL(Datapoint.TABLE_CREATE_QUARTER);
      db.execSQL(Datapoint.TABLE_CREATE_YEAR);
      db.execSQL(DateMap.TABLE_CREATE);
    }
    
    private void generateDateMapCacheData(SQLiteDatabase db) {
      Calendar cal = Calendar.getInstance();
      
      int dow;
      int secs;
      
      cal.set(Calendar.MONTH, 0);
      cal.set(Calendar.DAY_OF_MONTH, 1);
      cal.set(Calendar.HOUR_OF_DAY, 0);
      cal.set(Calendar.MINUTE, 0);
      cal.set(Calendar.SECOND, 0);

      ContentValues values = new ContentValues();

      for (int yyyy = 2000; yyyy < 2020; yyyy++) {
        cal.set(Calendar.YEAR, yyyy);
        for (int mm = 0; mm < 12; mm++) {
          cal.set(Calendar.MONTH, mm);
          dow = cal.get(Calendar.DAY_OF_WEEK);
          secs = (int) (cal.getTimeInMillis() / DateMapCache.SECOND_MS);
          
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