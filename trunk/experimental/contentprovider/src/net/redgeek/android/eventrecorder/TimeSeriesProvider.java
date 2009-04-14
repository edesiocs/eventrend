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

import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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

  private DatabaseHelper mDbHelper;
  private static UriMatcher sURIMatcher;
  private static HashMap<String, String> sTimeSeriesProjection;
  private static HashMap<String, String> sDatapointProjection;
  private DatabaseCache mCache;
  private DateMapCache mDateMap;
  private Lock mLock;
  
  public static final int PATH_SEGMENT_TIMERSERIES_ID = 1;
  public static final int PATH_SEGMENT_DATAPOINT_ID = 3;
  public static final int PATH_SEGMENT_DATAPOINT_RECENT_COUNT = 3;
  public static final int PATH_SEGMENT_DATAPOINT_RANGE_START = 3;
  public static final int PATH_SEGMENT_DATAPOINT_RANGE_END = 4;
  
  static {
    sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    sURIMatcher.addURI(TimeSeriesData.AUTHORITY, "timeseries", TIMESERIES);
    sURIMatcher.addURI(TimeSeriesData.AUTHORITY, "timeseries/#", TIMESERIES_ID);
    sURIMatcher.addURI(TimeSeriesData.AUTHORITY, "timeseries/#/datapoints", DATAPOINTS);
    sURIMatcher.addURI(TimeSeriesData.AUTHORITY, "timeseries/#/datapoints/#", DATAPOINTS_ID);
    sURIMatcher.addURI(TimeSeriesData.AUTHORITY, "timeseries/#/range/#", DATAPOINTS_RECENT);
    sURIMatcher.addURI(TimeSeriesData.AUTHORITY, "timeseries/#/range/#/#", DATAPOINTS_RANGE);
    
    sTimeSeriesProjection = new HashMap<String, String>();
    sTimeSeriesProjection.put(TimeSeries._ID, TimeSeries._ID);
    sTimeSeriesProjection.put(TimeSeries.TIMESERIES_NAME, TimeSeries.TIMESERIES_NAME);
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
    sDatapointProjection.put(Datapoint.UPDATES, Datapoint.UPDATES);
  }

  @Override
  public boolean onCreate() {
    mDbHelper = new DatabaseHelper(getContext());
    mCache = new DatabaseCache();
    mLock = new ReentrantLock();
    mDateMap = new DateMapCache(mDbHelper.getWritableDatabase());
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
      default:
        throw new IllegalArgumentException("getType: Unknown URI " + uri);
    }      
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
          mCache.removeTimeSeries(Long.valueOf(seriesId));

          db.setTransactionSuccessful();
        } catch (Exception e) {
          count = -1;
        } finally {
          db.endTransaction();
          LockUtil.unlock(mLock);
        }

        break;
      case DATAPOINTS_ID:
        seriesId = uri.getPathSegments().get(PATH_SEGMENT_TIMERSERIES_ID);
        datapointId = uri.getPathSegments().get(PATH_SEGMENT_DATAPOINT_ID);
        
        LockUtil.waitForLock(mLock);
        db.beginTransaction();

        try {
          count = db.delete(Datapoint.TABLE_NAME, Datapoint._ID + "=" + datapointId
              + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
              whereArgs);

          mCache.removeDatapoint(Long.valueOf(seriesId), Long.valueOf(datapointId));
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

  @Override
  public Uri insert(Uri uri, ContentValues values) {
    Uri outputUri = null;
    long id;
    SQLiteDatabase db = mDbHelper.getWritableDatabase();
    
    switch (sURIMatcher.match(uri)) {
      case TIMESERIES:
        LockUtil.waitForLock(mLock);
        db.beginTransaction();
        try {
          id = db.insert(TimeSeries.TABLE_NAME, null, values);
          if (id == -1) {
            outputUri = null;
          } else {
            outputUri = ContentUris.withAppendedId(TimeSeries.CONTENT_URI, id);
          }
          
          mCache.insertTimeSeries(Long.valueOf(id), values);           
          db.setTransactionSuccessful();
        } catch (Exception e) {
        } finally {
          db.endTransaction();
          LockUtil.unlock(mLock);
        }
        
        break;
      case DATAPOINTS_ID:
        Long timeSeriesId = values.getAsLong(Datapoint.TIMESERIES_ID);
        if (timeSeriesId < 1) {
          throw new IllegalArgumentException("insert: Invalid URI " + uri);
        }

        LockUtil.waitForLock(mLock);
        db.beginTransaction();
        try {
          id = db.insert(Datapoint.TABLE_NAME, null, values);
          if (id == -1) {
            outputUri = null;
          } else {
            outputUri = ContentUris.withAppendedId(Datapoint.CONTENT_URI, id);
          }
          
          mCache.insertDatapoint(timeSeriesId, Long.valueOf(id), values);           
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
    String orderBy = sortOrder;
    String limit = "";
    SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
    SQLiteDatabase db = mDbHelper.getReadableDatabase();
    
    switch (sURIMatcher.match(uri)) {
      case TIMESERIES:
        // TODO:  reference the cache
        qb.setTables(TimeSeries.TABLE_NAME);
        qb.setProjectionMap(sTimeSeriesProjection);
        if (TextUtils.isEmpty(sortOrder))
          orderBy = TimeSeries.DEFAULT_SORT_ORDER;
        break;
      case TIMESERIES_ID:
        // TODO:  reference the cache
        qb.setTables(TimeSeries.TABLE_NAME);
        qb.setProjectionMap(sTimeSeriesProjection);
        qb.appendWhere("_id=" + uri.getPathSegments().get(PATH_SEGMENT_TIMERSERIES_ID));
        if (TextUtils.isEmpty(sortOrder))
          orderBy = TimeSeries.DEFAULT_SORT_ORDER;
        break;
      case DATAPOINTS:
        // TODO:  reference the cache
        qb.setTables(Datapoint.TABLE_NAME);
        qb.appendWhere(TimeSeries._ID + "=" + uri.getPathSegments().get(PATH_SEGMENT_TIMERSERIES_ID));
        qb.setProjectionMap(sDatapointProjection);
        if (TextUtils.isEmpty(sortOrder))
          orderBy = Datapoint.DEFAULT_SORT_ORDER;
        break;
      case DATAPOINTS_RECENT:
        // TODO:  reference the cache
        String count = uri.getPathSegments().get(PATH_SEGMENT_DATAPOINT_RECENT_COUNT);
        qb.setTables(Datapoint.TABLE_NAME);
        qb.appendWhere(TimeSeries._ID + "=" + uri.getPathSegments().get(PATH_SEGMENT_TIMERSERIES_ID));
        orderBy = Datapoint.TS_START + " desc";
        limit = " limit " + count;
        break;
      case DATAPOINTS_RANGE:
        // TODO:  reference the cache
        String start = uri.getPathSegments().get(PATH_SEGMENT_DATAPOINT_RANGE_START);
        String end = uri.getPathSegments().get(PATH_SEGMENT_DATAPOINT_RANGE_END);
        qb.setTables(Datapoint.TABLE_NAME);
        qb.appendWhere(TimeSeries._ID + "=" + uri.getPathSegments().get(PATH_SEGMENT_TIMERSERIES_ID));
        qb.appendWhere(Datapoint.TS_START + ">=" + start);
        qb.appendWhere(Datapoint.TS_END + "<" + end);
        break;
      case DATAPOINTS_ID:
        // TODO:  reference the cache
        qb.setTables(Datapoint.TABLE_NAME);
        qb.appendWhere(TimeSeries._ID + "=" + uri.getPathSegments().get(PATH_SEGMENT_TIMERSERIES_ID));
        qb.appendWhere(Datapoint._ID + "=" + uri.getPathSegments().get(PATH_SEGMENT_DATAPOINT_ID));
        if (TextUtils.isEmpty(sortOrder))
          orderBy = Datapoint.DEFAULT_SORT_ORDER;
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
        LockUtil.waitForLock(mLock);
        db.beginTransaction();
        try {
          count = db.update(TimeSeries.TABLE_NAME, values, where, whereArgs);
          mCache.removeAllTimeSeries();
          db.setTransactionSuccessful();
        } catch (Exception e) {          
        } finally {
          db.endTransaction();
          LockUtil.unlock(mLock);
        }
        break;
      case TIMESERIES_ID:
        timeSeriesId = uri.getPathSegments().get(PATH_SEGMENT_TIMERSERIES_ID);

        LockUtil.waitForLock(mLock);
        db.beginTransaction();
        try {
          count = db.update(TimeSeries.TABLE_NAME, values, TimeSeries._ID + "="
              + timeSeriesId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
              whereArgs);
          mCache.removeTimeSeries(Long.valueOf(timeSeriesId));
          db.setTransactionSuccessful();
        } catch (Exception e) {          
        } finally {
          db.endTransaction();
          LockUtil.unlock(mLock);
        }
        break;
      case DATAPOINTS:
        LockUtil.waitForLock(mLock);
        db.beginTransaction();
        try {
          count = db.update(Datapoint.TABLE_NAME, values, where, whereArgs);
          mCache.removeAllTimeSeries();
          db.setTransactionSuccessful();
        } catch (Exception e) {
        } finally {
          db.endTransaction();
          LockUtil.unlock(mLock);
        }
        break;
      case DATAPOINTS_ID:
        timeSeriesId = uri.getPathSegments().get(PATH_SEGMENT_TIMERSERIES_ID);
        datapointId = uri.getPathSegments().get(PATH_SEGMENT_DATAPOINT_ID);
        
        LockUtil.waitForLock(mLock);
        db.beginTransaction();
        try {
          count = db.update(Datapoint.TABLE_NAME, values, Datapoint._ID + "=" + datapointId
              + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
              whereArgs);
          mCache.removeDatapoint(Long.valueOf(timeSeriesId), Long.valueOf(datapointId));
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
  
  private static class DatabaseHelper extends SQLiteOpenHelper {
    DatabaseHelper(Context context) {
      super(context, TimeSeriesData.DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
      db.execSQL(TimeSeries.TABLE_CREATE);
      db.execSQL(Datapoint.TABLE_CREATE);
      db.execSQL(DateMap.TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      db.execSQL("drop table " + TimeSeries.TABLE_CREATE);
      db.execSQL("drop table " + Datapoint.TABLE_CREATE);
      db.execSQL("drop table " + DateMap.TABLE_CREATE);
      db.execSQL(TimeSeries.TABLE_CREATE);
      db.execSQL(Datapoint.TABLE_CREATE);
      db.execSQL(DateMap.TABLE_CREATE);
    }
  }
}