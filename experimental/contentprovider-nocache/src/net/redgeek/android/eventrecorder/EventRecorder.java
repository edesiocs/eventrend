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

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;

import net.redgeek.android.eventrecorder.TimeSeriesData.Datapoint;
import net.redgeek.android.eventrecorder.TimeSeriesData.DateMap;
import net.redgeek.android.eventrecorder.TimeSeriesData.TimeSeries;
import net.redgeek.android.eventrecorder.interpolators.CubicInterpolator;
import net.redgeek.android.eventrecorder.interpolators.LinearInterpolator;
import net.redgeek.android.eventrecorder.interpolators.StepEarlyInterpolator;
import net.redgeek.android.eventrecorder.interpolators.StepLateInterpolator;
import net.redgeek.android.eventrecorder.interpolators.StepMidInterpolator;
import net.redgeek.android.eventrecorder.interpolators.TimeSeriesInterpolator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// TODO:  move RPC processing to thread / thread pool
// TODO:  have zerofill insert a datapoint even if none present
public class EventRecorder extends Service {
  public static final int ERSERVICE_SUCCESS       =  0;
  public static final int ERSERVICE_OK            =  ERSERVICE_SUCCESS;
  public static final int ERSERVICE_BUSY          = -1;
  public static final int ERSERVICE_ERR_NOT_FOUND = -2;
  public static final int ERSERVICE_ERR_PARAM     = -3;
  public static final int ERSERVICE_ERR_OOM       = -4;
  public static final int ERSERVICE_ERR_SQL       = -5;
  
  private static final String TAG = "EventRecorder";

  private BroadcastReceiver mIntentReceiver;
  private Calendar mCal;
  private int mLastHr;
  private Lock mLock;
  private DateMapCache mDateMap;

  private ArrayList<TimeSeriesInterpolator> mInterpolators;

  private Thread mZerofillThread;
  private Runnable mZerofiller;
  private Handler mZerofillHandler;
  private Handler mTimeChangeHandler;
  
  private boolean mFilling = false;
  private int mFillsToGo = 0;
  private int mFillsDone = 0;

  @Override
  public void onCreate() {
    super.onCreate();

    mCal = Calendar.getInstance();
    mLock = new ReentrantLock();

    mZerofiller = new Runnable() {
      public void run() {
        zerofill();
      }
    };
    mZerofillHandler = new Handler();

    mTimeChangeHandler = new Handler();
    mIntentReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_TIMEZONE_CHANGED)
            || action.equals(Intent.ACTION_TIME_TICK)
            || action.equals(Intent.ACTION_TIME_CHANGED)) {
          boolean hourChanged = true;
          mCal.setTimeInMillis(System.currentTimeMillis() / DateMap.SECOND_MS);

          // if it's just a time tick (minute), don't try to update unless the
          // hour has changed
          if (action.equals(Intent.ACTION_TIME_TICK) && 
              mCal.get(Calendar.HOUR_OF_DAY) == mLastHr) {
            hourChanged = false;
          }

          if (hourChanged == true) {
            mLastHr = mCal.get(Calendar.HOUR_OF_DAY);
            mZerofillThread.start();
          }
        }
      }
    };
    
    mInterpolators = new ArrayList<TimeSeriesInterpolator>();
    registerInterpolators(mInterpolators);
        
    IntentFilter filter = new IntentFilter();
    filter.addAction(Intent.ACTION_TIME_TICK);
    filter.addAction(Intent.ACTION_TIME_CHANGED);
    filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
    this.registerReceiver(mIntentReceiver, filter, null, mTimeChangeHandler);

    mCal.setTimeInMillis(System.currentTimeMillis() / DateMap.SECOND_MS);    
    mDateMap = new DateMapCache();

    mLastHr = mCal.get(Calendar.HOUR_OF_DAY);

    mDateMap.populateCache(this);    
    mZerofillThread = new Thread(mZerofiller);
    mZerofillThread.start();
    
    return;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
  }

  @Override
  public IBinder onBind(Intent intent) {
    String serviceName = IEventRecorderService.class.getName();
    if (IEventRecorderService.class.getName().equals(intent.getAction())) {
      return mBinder;
    }
    return null;
  }

  private final IEventRecorderService.Stub mBinder = new IEventRecorderService.Stub() {
    public int getServiceStatus() {
      if (mFilling == true)
        return ERSERVICE_BUSY;
      else
        return ERSERVICE_OK;
    }

    public int getServiceFillsTotal() {
      return mFillsToGo;
    }
    
    public int getServiceFillsPerformed() {
      return mFillsDone;
    }
    
    // Returns the _id of the timeseries, 0 for not found, < 0 for error.
    // See TimeSeriesProvider
    public long getTimeSeriesId(String name) {
      if (name == null)
        return ERSERVICE_ERR_PARAM;
      
      String[] projection = new String[] { TimeSeries._ID };
      if (projection == null)
        return ERSERVICE_ERR_OOM;

      Uri timeseries = TimeSeries.CONTENT_URI;
      if (timeseries == null)
        return ERSERVICE_ERR_OOM;
      
      Cursor c = getContentResolver().query(timeseries, 
          projection, TimeSeries.TIMESERIES_NAME + " = ? ",
          new String[] { name }, null);
      if (c == null)
        return ERSERVICE_ERR_SQL;
      if (c.getCount() < 1) {
        c.close();
        return ERSERVICE_ERR_NOT_FOUND;
      }

      c.moveToFirst();
      long id = TimeSeries.getId(c);
      c.close();

      return id;
    }

    // Create a new event for the timeseries and set a start marker.
    // Returns the _id of the datapoint, 0 for not found, < 0 for error.
    // See TimeSeriesProvider
    public long recordEventStart(long timeSeriesId) {
      if (timeSeriesId < 1)
        return ERSERVICE_ERR_PARAM;

      ContentValues values = new ContentValues();
      if (values == null)
        return ERSERVICE_ERR_OOM;

      LockUtil.waitForLock(mLock);
      long id = currentlyRecordingId(timeSeriesId);
      if (id < 0) { // error
        LockUtil.unlock(mLock);
        return id;
      }
      if (id > 1) { // datapoint already recording
        LockUtil.unlock(mLock);
        return ERSERVICE_ERR_PARAM;
      }

      long now = System.currentTimeMillis() / DateMap.SECOND_MS;
      values.put(Datapoint.TIMESERIES_ID, timeSeriesId);
      values.put(Datapoint.TS_START, now);
      values.put(Datapoint.TS_END, 0);
      values.put(Datapoint.VALUE, 0);
      values.put(Datapoint.ENTRIES, 1);

      Uri uri = ContentUris.withAppendedId(
          TimeSeries.CONTENT_URI, timeSeriesId).buildUpon()
          .appendPath("datapoints").build();
      uri = getContentResolver().insert(uri, values);
      if (uri == null) {
        LockUtil.unlock(mLock);
        return ERSERVICE_ERR_OOM;
      }

      int datapointId;
      try {
        datapointId = Integer.parseInt(uri.getPathSegments().get(
            TimeSeriesProvider.PATH_SEGMENT_TIMESERIES_DATAPOINT_ID));
      } catch (Exception e) {
        LockUtil.unlock(mLock);
        return ERSERVICE_ERR_SQL;
      }
      
      values.clear();
      values.put(TimeSeries.RECORDING_DATAPOINT_ID, datapointId);
      Uri timeseries = ContentUris.withAppendedId(
          TimeSeries.CONTENT_URI, timeSeriesId);
      if (timeseries == null) {
        LockUtil.unlock(mLock);
        return ERSERVICE_ERR_SQL;
      }

      int count = getContentResolver().update(timeseries, values, null, null);
      if (count != 1) {
        LockUtil.unlock(mLock);
        return ERSERVICE_ERR_SQL;
      }
      
      LockUtil.unlock(mLock);
      return datapointId;
    }

    // Stops the currently running event for the time series.
    // Returns the _id of the datapoint, 0 if no datapoint is recording, < 0 for
    // error.
    // See TimeSeriesProvider
    public long recordEventStop(long timeSeriesId) {
      if (timeSeriesId < 1)
        return ERSERVICE_ERR_PARAM;

      LockUtil.waitForLock(mLock);
      long datapointId = currentlyRecordingId(timeSeriesId);
      if (datapointId < 0) { // error
        LockUtil.unlock(mLock);
        return datapointId;
      }
      if (datapointId == 0) { // no datapoint currently recording
        LockUtil.unlock(mLock);
        return ERSERVICE_ERR_PARAM;
      }

      ContentValues values = new ContentValues();
      if (values == null) {
        LockUtil.unlock(mLock);
        return ERSERVICE_ERR_OOM;
      }

      Uri uri = ContentUris.withAppendedId(
          TimeSeries.CONTENT_URI, timeSeriesId).buildUpon()
          .appendPath("datapoints").appendPath("" + datapointId).build();
      if (uri == null) {
        LockUtil.unlock(mLock);
        return ERSERVICE_ERR_OOM;
      }
      
      Cursor c = getContentResolver().query(uri, null, null, null, null);
      if (c == null)
        return -1;
      if (c.getCount() < 1) {
        c.close();
        return ERSERVICE_ERR_NOT_FOUND;
      }

      c.moveToFirst();
      long tsStart = Datapoint.getTsStart(c);
      c.close();

      long now = System.currentTimeMillis() / DateMap.SECOND_MS;
      values.put(Datapoint.TS_END, now);
      values.put(Datapoint.VALUE, now - tsStart);
      values.put(Datapoint.ENTRIES, 1);
      int count = getContentResolver().update(uri, values, null, null);
      if (count != 1) {
        LockUtil.unlock(mLock);
        return ERSERVICE_ERR_SQL;
      }

      values.clear();
      values.put(TimeSeries.RECORDING_DATAPOINT_ID, 0);
      Uri timeseries = ContentUris.withAppendedId(
          TimeSeries.CONTENT_URI, timeSeriesId);
      if (timeseries == null) {
        LockUtil.unlock(mLock);
        return ERSERVICE_ERR_OOM;
      }

      count = getContentResolver().update(timeseries, values, null, null);
      if (count != 1) {
        LockUtil.unlock(mLock);
        return ERSERVICE_ERR_SQL;
      }
      
      LockUtil.unlock(mLock);
      return datapointId;
    }

    // Records a discrete event with the timestamp specified.
    // Returns the _id of the datapoint, < 0 for error.
    // See TimeSeriesProvider
    public long recordEvent(long timeSeriesId, long timestamp, double value) {
      LockUtil.waitForLock(mLock);
      long datapointId = currentlyRecordingId(timeSeriesId);
      if (datapointId < 0) { // error
        LockUtil.unlock(mLock);
        return datapointId;
      }
      if (datapointId != 0) { // datapoint currently recording
        LockUtil.unlock(mLock);
        return ERSERVICE_ERR_PARAM;
      }
       
      ContentValues values = new ContentValues();
      if (values == null) {
        LockUtil.unlock(mLock);
        return ERSERVICE_ERR_OOM;
      }

      values.put(Datapoint.TIMESERIES_ID, timeSeriesId);
      values.put(Datapoint.TS_START, timestamp);
      values.put(Datapoint.TS_END, timestamp);
      values.put(Datapoint.VALUE, value);
      values.put(Datapoint.ENTRIES, 1);

      Uri uri = ContentUris.withAppendedId(
          TimeSeries.CONTENT_URI, timeSeriesId).buildUpon()
          .appendPath("datapoints").build();
      uri = getContentResolver().insert(uri, values);
      if (uri == null) {
        LockUtil.unlock(mLock);
        return ERSERVICE_ERR_SQL;
      }

      try {
        datapointId = Integer.parseInt(uri.getPathSegments().get(
            TimeSeriesProvider.PATH_SEGMENT_TIMESERIES_DATAPOINT_ID));
      } catch (Exception e) {
        LockUtil.unlock(mLock);
        return ERSERVICE_ERR_SQL;
      }

      LockUtil.unlock(mLock);
      return datapointId;
    }
    
    // Records a discrete event at the current time.
    // Returns the _id of the datapoint, < 0 for error.
    // See TimeSeriesProvider
    public long recordEventNow(long timeSeriesId, double value) {
      long now = System.currentTimeMillis() / DateMap.SECOND_MS;
      return recordEvent(timeSeriesId, now, value);
    }
  };

  private long currentlyRecordingId(long timeSeriesId) {
    String[] projection = new String[] { TimeSeries.RECORDING_DATAPOINT_ID };
    if (projection == null)
      return ERSERVICE_ERR_OOM;

    // don't add an event if there's one currently record
    Uri timeseries = ContentUris.withAppendedId(
        TimeSeries.CONTENT_URI, timeSeriesId);
    if (timeseries == null)
      return ERSERVICE_ERR_OOM;

    Cursor c = getContentResolver().query(timeseries, projection, null, null, null);
    if (c == null)
      return ERSERVICE_ERR_SQL;
    if (c.getCount() < 1) {
      c.close();
      return ERSERVICE_ERR_NOT_FOUND;
    }

    c.moveToFirst();
    long id = TimeSeries.getRecordingDatapointId(c);
    c.close();
    
    return id;
  }

  private void zerofill() {
    mFilling = true;

    Cursor tsCur;
    Cursor dpCur;
    String[] timeSeriesProjection = new String[] {
        TimeSeries._ID,
        TimeSeries.TYPE,
        TimeSeries.PERIOD, };
    String[] datapointProjection = new String[] { Datapoint.TS_END };

    if (timeSeriesProjection == null || datapointProjection == null) {
      mFilling = false;
      return;
    }

    ContentValues values = new ContentValues();
    if (values == null) {
      mFilling = false;
      return;
    }

    Uri timeseries = TimeSeries.CONTENT_URI;
    if (timeseries == null) {
      mFilling = false;
      return;
    }
 
    LockUtil.waitForLock(mLock);
    tsCur = getContentResolver().query(timeseries, timeSeriesProjection, 
        TimeSeries.ZEROFILL + " != 0 and " +
        TimeSeries.RECORDING_DATAPOINT_ID + " == 0 and " +
        TimeSeries.TYPE + " != \"" + TimeSeries.TYPE_SYNTHETIC + "\" and " +
        TimeSeries.PERIOD + " != 0 ", null, null);
    if (tsCur == null) {
      mFilling = false;
      LockUtil.unlock(mLock);
      return;
    }
    if (tsCur.getCount() < 1) {
      tsCur.close();
      mFilling = false;
      LockUtil.unlock(mLock);
      return;
    }

    int now = (int) (System.currentTimeMillis() / DateMap.SECOND_MS);
    mFillsToGo = 0;
    mFillsDone = 0;

    int count = tsCur.getCount();
    tsCur.moveToFirst();
    for (int i = 0; i < count; i++) {
      int period = TimeSeries.getPeriod(tsCur);
      int periodStart = mDateMap.secondsOfPeriodStart(now, period);

      if (period < 1) {
        tsCur.moveToNext();
        continue;
      }

      long timeSeriesId = TimeSeries.getId(tsCur);
      Uri lastDatapoint = ContentUris.withAppendedId(
          TimeSeries.CONTENT_URI, timeSeriesId).buildUpon()
          .appendPath("recent").appendPath("1").build();
      if (lastDatapoint == null)
        continue;

      dpCur = getContentResolver().query(lastDatapoint, datapointProjection, 
          null, null, null);
      if (dpCur == null)
        continue;

      String type = TimeSeries.getType(tsCur);
      Uri uri = ContentUris.withAppendedId(
          TimeSeries.CONTENT_URI, timeSeriesId).buildUpon()
          .appendPath("datapoints").build();

      dpCur.moveToFirst();
      if (dpCur.getCount() < 1) {
        mFillsToGo++;

        values.clear();
        values.put(Datapoint.TIMESERIES_ID, timeSeriesId);
        values.put(Datapoint.TS_START, periodStart);
        if (type.equals(TimeSeries.TYPE_RANGE))
          values.put(Datapoint.TS_END, periodStart + period - 1);
        else
          values.put(Datapoint.TS_END, periodStart);
        values.put(Datapoint.VALUE, 0);
        values.put(Datapoint.ENTRIES, 1);
        
        getContentResolver().insert(uri, values);
        mFillsDone++;
      } else {
        int tsEnd = Datapoint.getTsEnd(dpCur);        
        int secs = mDateMap.secondsOfPeriodStart(tsEnd + period, period);
        mFillsToGo += ((now - secs) / period);

        while (true) {
          if (secs + period >= now)
            break;

          values.clear();
          values.put(Datapoint.TIMESERIES_ID, timeSeriesId);
          values.put(Datapoint.TS_START, secs);
          if (type.equals(TimeSeries.TYPE_RANGE))         
            values.put(Datapoint.TS_END, secs + period - 1);
          else
            values.put(Datapoint.TS_END, secs);
          values.put(Datapoint.VALUE, 0);
          values.put(Datapoint.ENTRIES, 1);

          getContentResolver().insert(uri, values);
          mFillsDone++;

          secs = mDateMap.secondsOfPeriodStart(secs + period, period);
        }
      }
      dpCur.close();
      tsCur.moveToNext();
    }
    
    tsCur.close();    
    LockUtil.unlock(mLock);
    
    mFillsToGo = 0;
    mFillsDone = 0;
    mFilling = false;

    return;
  }
    
  public ArrayList<TimeSeriesInterpolator> getInterpolators() {
    return mInterpolators;
  }

  public TimeSeriesInterpolator getInterpolator(String name) {
    TimeSeriesInterpolator tsi = null;

    if (name == null)
      return null;

    for (int i = 0; i < mInterpolators.size(); i++) {
      tsi = mInterpolators.get(i);
      if (name.equals(tsi.getName()))
        return tsi;
    }

    return null;
  }
  
  private static void registerInterpolators(ArrayList<TimeSeriesInterpolator> list) {
    registerInterpolator(list, new LinearInterpolator());
    registerInterpolator(list, new CubicInterpolator());
    registerInterpolator(list, new StepEarlyInterpolator());
    registerInterpolator(list, new StepMidInterpolator());
    registerInterpolator(list, new StepLateInterpolator());
  }

  private static void registerInterpolator(ArrayList<TimeSeriesInterpolator> list,
      TimeSeriesInterpolator tsi) {
    list.add(tsi);
  }
}