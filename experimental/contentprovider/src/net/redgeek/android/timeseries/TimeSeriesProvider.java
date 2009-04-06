package net.redgeek.android.timeseries;

import java.util.HashMap;

import net.redgeek.android.timeseries.TimeSeriesData.Category;
import net.redgeek.android.timeseries.TimeSeriesData.Datapoint;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;

public class TimeSeriesProvider extends ContentProvider {
  private static final String TAG = "TimeSeriesProvider";

  private static final int DATABASE_VERSION = 4;

  private static final int TIMESERIES = 1;
  private static final int TIMESERIES_ID = 2;
  private static final int DATAPOINTS = 3;
  private static final int DATAPOINTS_ID = 4;

  private DatabaseHelper mDbHelper;
  private static final UriMatcher sURIMatcher;
  private static HashMap<String, String> sTimeSeriesProjection;
  private static HashMap<String, String> sDatapointProjection;

  static {
    sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    sURIMatcher.addURI(TimeSeriesData.AUTHORITY, "/timeseries", TIMESERIES);
    sURIMatcher.addURI(TimeSeriesData.AUTHORITY, "/timeseries/#", TIMESERIES_ID);
    sURIMatcher.addURI(TimeSeriesData.AUTHORITY, "/timeseries/#/datapoints", DATAPOINTS);
    sURIMatcher.addURI(TimeSeriesData.AUTHORITY, "/timeseries/#/datapoints/#", DATAPOINTS_ID);
    
    sTimeSeriesProjection = new HashMap<String, String>();
    sTimeSeriesProjection.put(Category._ID, Category._ID);
    sTimeSeriesProjection.put(Category.CATEGORY_NAME, Category.CATEGORY_NAME);
    sTimeSeriesProjection.put(Category.GROUP_NAME, Category.GROUP_NAME);
    sTimeSeriesProjection.put(Category.GROUP_NAME, Category.GROUP_NAME);
    sTimeSeriesProjection.put(Category.DEFAULT_VALUE, Category.DEFAULT_VALUE);
    sTimeSeriesProjection.put(Category.INCREMENT, Category.INCREMENT);
    sTimeSeriesProjection.put(Category.GOAL, Category.GOAL);
    sTimeSeriesProjection.put(Category.COLOR, Category.COLOR);
    sTimeSeriesProjection.put(Category.PERIOD, Category.PERIOD);
    sTimeSeriesProjection.put(Category.RANK, Category.RANK);
    sTimeSeriesProjection.put(Category.AGGREGATION, Category.AGGREGATION);
    sTimeSeriesProjection.put(Category.TYPE, Category.TYPE);
    sTimeSeriesProjection.put(Category.ZEROFILL, Category.ZEROFILL);
    sTimeSeriesProjection.put(Category.FORMULA, Category.FORMULA);
    sTimeSeriesProjection.put(Category.INTERPOLATION, Category.INTERPOLATION);
    sTimeSeriesProjection.put(Category.RECENT_TIMESTAMP, Category.RECENT_TIMESTAMP);
    sTimeSeriesProjection.put(Category.RECENT_VALUE, Category.RECENT_VALUE);
    sTimeSeriesProjection.put(Category.RECENT_TREND, Category.RECENT_TREND);
    sTimeSeriesProjection.put(Category.TREND_STATE, Category.TREND_STATE);
    
    sDatapointProjection = new HashMap<String, String>();
    sDatapointProjection.put(Datapoint._ID, Datapoint._ID);
    sDatapointProjection.put(Datapoint.CATEGORY_ID, Datapoint.CATEGORY_ID);
    sDatapointProjection.put(Datapoint.TS_START, Datapoint.TS_START);
    sDatapointProjection.put(Datapoint.TS_END, Datapoint.TS_END);
    sDatapointProjection.put(Datapoint.VALUE, Datapoint.VALUE);
    sDatapointProjection.put(Datapoint.UPDATES, Datapoint.UPDATES);
  }

  @Override
  public boolean onCreate() {
    mDbHelper = new DatabaseHelper(getContext());
    return true;
  }

  @Override
  public int delete(Uri uri, String where, String[] whereArgs) {
    SQLiteDatabase db = mDbHelper.getWritableDatabase();
    String id;
    int count;
    switch (sURIMatcher.match(uri)) {
      case TIMESERIES_ID:
        id = uri.getPathSegments().get(1);
        // Delete datapoints associated with the timeseries
        count = db.delete(Datapoint.TABLE_NAME, Datapoint.CATEGORY_ID + "=" + id,
            null);
        // and the timeseries meta-data
        count = db.delete(Category.TABLE_NAME, Category._ID + "=" + id
            + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
            whereArgs);
        break;
      case DATAPOINTS:
        count = db.delete(Datapoint.TABLE_NAME, where, whereArgs);
        break;
      case DATAPOINTS_ID:
        id = uri.getPathSegments().get(2);
        count = db.delete(Datapoint.TABLE_NAME, Datapoint._ID + "=" + id
            + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
            whereArgs);
        break;
      default:
        throw new IllegalArgumentException("Unknown URI " + uri);
    }

    getContext().getContentResolver().notifyChange(uri, null);
    return count;
  }

  @Override
  public String getType(Uri uri) {
      int match = sURIMatcher.match(uri);
      switch (match) {
        case TIMESERIES:
          return Category.CONTENT_TYPE;
        case TIMESERIES_ID:
          return Category.CONTENT_ITEM_TYPE;
        case DATAPOINTS:
          return Datapoint.CONTENT_TYPE;
        case DATAPOINTS_ID:
          return Datapoint.CONTENT_ITEM_TYPE;
        default:
          throw new IllegalArgumentException("Unknown URI " + uri);
      }      
  }

  @Override
  public Uri insert(Uri uri, ContentValues values) {
    SQLiteDatabase db;
    long id;
    
    switch (sURIMatcher.match(uri)) {
      case DATAPOINTS_ID:
        if (values.getAsLong(Datapoint.CATEGORY_ID) < 1)
          throw new IllegalArgumentException("Invalid URI " + uri);

        db = mDbHelper.getWritableDatabase();
        id = db.insert(Datapoint.TABLE_NAME, null, values);
        if (id == -1) {
          return null;
        } else {
          return ContentUris.withAppendedId(Datapoint.CONTENT_URI, id);
        }
      case TIMESERIES_ID:
        db = mDbHelper.getWritableDatabase();
        id = db.insert(Category.TABLE_NAME, null, values);
        if (id == -1) {
          return null;
        } else {
          return ContentUris.withAppendedId(Category.CONTENT_URI, id);
        }
      default:
        throw new IllegalArgumentException("Unknown URI " + uri);
    }      
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection,
      String[] selectionArgs, String sortOrder) {
    SQLiteDatabase db = mDbHelper.getWritableDatabase();

      switch (sURIMatcher.match(uri)) {
      case TIMESERIES:
      case TIMESERIES_ID:
        return db.query(Category.TABLE_NAME, projection, selection,
            selectionArgs, null, null, sortOrder);
      case DATAPOINTS:
      case DATAPOINTS_ID:
        return db.query(Datapoint.TABLE_NAME, projection, selection,
            selectionArgs, null, null, sortOrder);
      default:
        throw new IllegalArgumentException("Unknown URI " + uri);
    }
  }

  @Override
  public int update(Uri uri, ContentValues values, String where,
      String[] whereArgs) {
    SQLiteDatabase db = mDbHelper.getWritableDatabase();
    String id;
    int count;
    
    switch (sURIMatcher.match(uri)) {
      case TIMESERIES:
        count = db.update(Category.TABLE_NAME, values, where, whereArgs);
        break;
      case TIMESERIES_ID:
        id = uri.getPathSegments().get(1);
        count = db.update(Datapoint.TABLE_NAME, values, Datapoint._ID + "=" + id
            + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
            whereArgs);
        break;
      case DATAPOINTS:
        count = db.update(Datapoint.TABLE_NAME, values, where, whereArgs);
        break;
      case DATAPOINTS_ID:
        id = uri.getPathSegments().get(2);
        count = db.update(Datapoint.TABLE_NAME, values, Datapoint._ID + "=" + id
            + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
            whereArgs);
        break;
      default:
        throw new IllegalArgumentException("Unknown URI " + uri);
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
      db.execSQL(Category.TABLE_CREATE);
      db.execSQL(Datapoint.TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      // TODO: perform alters to save data
    }
  }
}