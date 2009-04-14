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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import net.redgeek.android.eventrecorder.interpolators.CubicInterpolator;
import net.redgeek.android.eventrecorder.interpolators.LinearInterpolator;
import net.redgeek.android.eventrecorder.interpolators.StepEarlyInterpolator;
import net.redgeek.android.eventrecorder.interpolators.StepLateInterpolator;
import net.redgeek.android.eventrecorder.interpolators.StepMidInterpolator;
import net.redgeek.android.eventrecorder.interpolators.TimeSeriesInterpolator;

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

// TODO:  move RPC processing to thread / thread pool
// TODO:  have zerofill insert a datapoint even if none present
public class EventRecorder extends Service {
  private static final String TAG = "EventRecorder";

  private Handler mHandler;
  private BroadcastReceiver mIntentReceiver;
  private Calendar mCal;
  private int mLastHr;
  private Lock mLock;

  private ArrayList<TimeSeriesInterpolator> mInterpolators;

  /** Number of milliseconds in a second */
  public static final long SECOND_MS = 1000;
  /** Number of milliseconds in a minute */
  public static final long MINUTE_MS = SECOND_MS * 60;
  /** Number of milliseconds in an hour */
  public static final long HOUR_MS = MINUTE_MS * 60;
  /** Number of milliseconds in a morning or evening (1/2 day) */
  public static final long AMPM_MS = HOUR_MS * 12;
  /** Number of milliseconds in a day */
  public static final long DAY_MS = HOUR_MS * 24;
  /** Number of milliseconds in a week */
  public static final long WEEK_MS = DAY_MS * 7;
  /** Number of milliseconds in a year */
  public static final long YEAR_MS = WEEK_MS * 52;
  /** Number of milliseconds in a quarter (as defined by 1/4 of a year) */
  public static final long QUARTER_MS = WEEK_MS * 13;
  /** Number of milliseconds in a month (as defined by 1/12 of a year) */
  public static final long MONTH_MS = YEAR_MS / 12;

  @Override
  public void onCreate() {
    super.onCreate();

    mCal = Calendar.getInstance();
    mLock = new ReentrantLock();

    mHandler = new Handler();
    mIntentReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_TIMEZONE_CHANGED)
            || action.equals(Intent.ACTION_TIME_TICK)
            || action.equals(Intent.ACTION_TIME_CHANGED)) {
          boolean updateZerofills = true;
          mCal.setTimeInMillis(System.currentTimeMillis());

          // if it's just a time tick (minute), don't try to update unless the
          // hour has changed
          if (action.equals(Intent.ACTION_TIME_TICK) && 
              mCal.get(Calendar.HOUR_OF_DAY) == mLastHr) {
            updateZerofills = false;
          }

          if (updateZerofills == true) {
            mLastHr = mCal.get(Calendar.HOUR_OF_DAY);
            zerofill();
          }
        }
      }
    };
    
    registerInterpolators(mInterpolators);
        
    IntentFilter filter = new IntentFilter();
    filter.addAction(Intent.ACTION_TIME_TICK);
    filter.addAction(Intent.ACTION_TIME_CHANGED);
    filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
    this.registerReceiver(mIntentReceiver, filter, null, mHandler);

    mCal.setTimeInMillis(System.currentTimeMillis());
    mLastHr = mCal.get(Calendar.HOUR_OF_DAY);
    zerofill();
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
    // Returns the _id of the timeseries, 0 for not found, < 0 for error.
    // See TimeSeriesProvider
    public long getTimeSeriesId(String name) {
      if (name == null)
        return -1;
      
      String[] projection = new String[] { TimeSeriesData.TimeSeries._ID };
      if (projection == null)
        return -1;

      Uri timeseries = TimeSeriesData.TimeSeries.CONTENT_URI;
      if (timeseries == null)
        return -1;
      
      Cursor c = getContentResolver().query(timeseries, 
          projection, TimeSeriesData.TimeSeries.TIMESERIES_NAME + " = ? ",
          new String[] { name }, null);
      if (c == null)
        return -1;
      if (c.getCount() < 1) {
        c.close();
        return 0;
      }

      c.moveToFirst();
      long id = c.getLong(c.getColumnIndexOrThrow(TimeSeriesData.TimeSeries._ID));
      c.close();

      return id;
    }

    // Create a new event for the timeseries and set a start marker.
    // Returns the _id of the datapoint, 0 for not found, < 0 for error.
    // See TimeSeriesProvider
    public long recordEventStart(long timeSeriesId) {
      if (timeSeriesId < 1)
        return 0;

      ContentValues values = new ContentValues();
      if (values == null)
        return -1;

      LockUtil.waitForLock(mLock);
      long id = currentlyRecordingId(timeSeriesId);
      if (id < 0) { // error
        LockUtil.unlock(mLock);
        return id;
      }
      if (id > 1) { // datapoint already recording
        LockUtil.unlock(mLock);
        return -2;
      }

      long now = System.currentTimeMillis();
      values.put(TimeSeriesData.Datapoint.TIMESERIES_ID, timeSeriesId);
      values.put(TimeSeriesData.Datapoint.TS_START, now);
      values.put(TimeSeriesData.Datapoint.TS_END, 0);
      values.put(TimeSeriesData.Datapoint.VALUE, 0);
      values.put(TimeSeriesData.Datapoint.UPDATES, 1);

      Uri uri = getContentResolver().insert(
          TimeSeriesData.Datapoint.CONTENT_URI, values);
      if (uri == null) {
        LockUtil.unlock(mLock);
        return -1;
      }

      int datapointId;
      try {
        datapointId = Integer.parseInt(uri.getPathSegments().get(
            TimeSeriesProvider.PATH_SEGMENT_DATAPOINT_ID));
      } catch (Exception e) {
        LockUtil.unlock(mLock);
        return -1;
      }
      
      values.clear();
      values.put(TimeSeriesData.TimeSeries.RECORDING_DATAPOINT_ID, datapointId);
      Uri timeseries = ContentUris.withAppendedId(
          TimeSeriesData.TimeSeries.CONTENT_URI, timeSeriesId);
      if (timeseries == null) {
        LockUtil.unlock(mLock);
        return -1;
      }

      int count = getContentResolver().update(
          TimeSeriesData.Datapoint.CONTENT_URI, values,
          TimeSeriesData.TimeSeries._ID + " = ? ",
          new String[] { "" + timeSeriesId } );
      if (count != 1) {
        LockUtil.unlock(mLock);
        return -1;
      }
      
      LockUtil.unlock(mLock);
      return datapointId;
    }

    // Stops the currently running event for the time series.
    // Returns the _id of the datapoint, 0 if no datapoint is recording, < 0 for
    // error.
    // See TimeSeriesProvider
    public long recordEventStop(long timeSeriesId, float value) {
      if (timeSeriesId < 1)
        return 0;

      LockUtil.waitForLock(mLock);
      long datapointId = currentlyRecordingId(timeSeriesId);
      if (datapointId < 0) { // error
        LockUtil.unlock(mLock);
        return datapointId;
      }
      if (datapointId == 0) { // no datapoint currently recording
        LockUtil.unlock(mLock);
        return -2;
      }

      ContentValues values = new ContentValues();
      if (values == null) {
        LockUtil.unlock(mLock);
        return -1;
      }

      long now = System.currentTimeMillis();
      values.put(TimeSeriesData.Datapoint.TS_END, now);
      values.put(TimeSeriesData.Datapoint.VALUE, value);
      int count = getContentResolver().update(
          TimeSeriesData.Datapoint.CONTENT_URI, values,
          TimeSeriesData.Datapoint._ID + " = ? ", 
          new String[] { "" + datapointId });
      if (count != 1) {
        LockUtil.unlock(mLock);
        return -1;
      }

      values.clear();
      values.put(TimeSeriesData.TimeSeries.RECORDING_DATAPOINT_ID, 0);
      count = getContentResolver().update(
          TimeSeriesData.TimeSeries.CONTENT_URI, values,
          TimeSeriesData.TimeSeries._ID + " = ? ",
          new String[] { "" + timeSeriesId });
      if (count != 1) {
        LockUtil.unlock(mLock);
        return -1;
      }
      
      LockUtil.unlock(mLock);
      return datapointId;
    }

    // Records a discrete event.
    // Returns the _id of the datapoint, < 0 for error.
    // See TimeSeriesProvider
    public long recordEvent(long timeSeriesId, float value) {
      LockUtil.waitForLock(mLock);
      long datapointId = currentlyRecordingId(timeSeriesId);
      if (datapointId < 0) { // error
        LockUtil.unlock(mLock);
        return datapointId;
      }
      if (datapointId != 0) { // datapoint currently recording
        LockUtil.unlock(mLock);
        return -2;
      }

      ContentValues values = new ContentValues();
      if (values == null) {
        LockUtil.unlock(mLock);
        return -1;
      }

      long now = System.currentTimeMillis();
      values.put(TimeSeriesData.Datapoint.TIMESERIES_ID, timeSeriesId);
      values.put(TimeSeriesData.Datapoint.TS_START, now);
      values.put(TimeSeriesData.Datapoint.TS_END, now);
      values.put(TimeSeriesData.Datapoint.VALUE, value);
      values.put(TimeSeriesData.Datapoint.UPDATES, 1);

      Uri uri = getContentResolver().insert(
          TimeSeriesData.Datapoint.CONTENT_URI, values);
      if (uri == null) {
        LockUtil.unlock(mLock);
        return -1;
      }

      try {
        datapointId = Integer.parseInt(uri.getPathSegments().get(
            TimeSeriesProvider.PATH_SEGMENT_DATAPOINT_ID));
      } catch (Exception e) {
        LockUtil.unlock(mLock);
        return -1;
      }

      LockUtil.unlock(mLock);
      return datapointId;
    }
  };

  private long currentlyRecordingId(long timeSeriesId) {
    String[] projection = new String[] { TimeSeriesData.TimeSeries.RECORDING_DATAPOINT_ID };
    if (projection == null)
      return -1;

    // don't add an event if there's one currently record
    Uri timeseries = ContentUris.withAppendedId(
        TimeSeriesData.TimeSeries.CONTENT_URI, timeSeriesId);
    if (timeseries == null)
      return -1;

    Cursor c = getContentResolver().query(timeseries, projection, null, null, null);
    if (c == null)
      return -1;
    if (c.getCount() < 1)
      return -1;

    c.moveToFirst();
    long id = c.getLong(c.getColumnIndexOrThrow(TimeSeriesData.TimeSeries.RECORDING_DATAPOINT_ID));
    c.close();
    return id;
  }

  private void zerofill() {
    Cursor tsCur;
    Cursor dpCur;
    String[] timeSeriesProjection = new String[] {
        TimeSeriesData.TimeSeries._ID,
        TimeSeriesData.TimeSeries.PERIOD, };
    String[] datapointProjection = new String[] { TimeSeriesData.Datapoint.TS_END, };

    if (timeSeriesProjection == null || datapointProjection == null)
      return;

    ContentValues values = new ContentValues();
    if (values == null)
      return;

    Uri timeseries = TimeSeriesData.TimeSeries.CONTENT_URI;
    if (timeseries == null)
      return;

    LockUtil.waitForLock(mLock);
    tsCur = getContentResolver().query(timeseries, timeSeriesProjection, 
        TimeSeriesData.TimeSeries.ZEROFILL + " != 0 and " +
        TimeSeriesData.TimeSeries.RECORDING_DATAPOINT_ID + " > 0 and " +
        TimeSeriesData.TimeSeries.PERIOD + " != 0 ", null, null);
    if (tsCur == null || tsCur.getCount() < 1) {
      LockUtil.unlock(mLock);
      return;
    }

    long now = System.currentTimeMillis();
    int count = tsCur.getCount();
    tsCur.moveToFirst();
    for (int i = 0; i < count; i++) {
      int period = tsCur.getInt(tsCur.getColumnIndexOrThrow(TimeSeriesData.TimeSeries.PERIOD));
      if (period < 1) {
        tsCur.moveToNext();
        continue;
      }

      long timeSeriesId = tsCur.getLong(tsCur.getColumnIndexOrThrow(TimeSeriesData.TimeSeries._ID));

      Uri lastDatapoint = ContentUris.withAppendedId(
          TimeSeriesData.Datapoint.CONTENT_URI, timeSeriesId).buildUpon()
          .appendPath("range").appendPath("1").build();
      if (lastDatapoint == null)
        continue;

      dpCur = getContentResolver().query(lastDatapoint, datapointProjection, 
          null, null, null);
      if (dpCur == null)
        continue;

      if (dpCur.getCount() < 1) {
        values.clear();
        values.put(TimeSeriesData.Datapoint.TIMESERIES_ID, timeSeriesId);
        values.put(TimeSeriesData.Datapoint.TS_START, now);
        values.put(TimeSeriesData.Datapoint.TS_END, now);
        values.put(TimeSeriesData.Datapoint.VALUE, 0);
        values.put(TimeSeriesData.Datapoint.UPDATES, 1);

        getContentResolver().insert(TimeSeriesData.Datapoint.CONTENT_URI,
            values);
      } else {
        long tsEnd = dpCur.getLong(dpCur.getColumnIndexOrThrow(TimeSeriesData.Datapoint.TS_END));

        mCal.setTimeInMillis(tsEnd);
        mCal.add(Calendar.MILLISECOND, period);
        setToPeriodStart(mCal, period);

        while (true) {
          long ms = mCal.getTimeInMillis();
          if (ms + period >= now)
            break;

          values.clear();
          values.put(TimeSeriesData.Datapoint.TIMESERIES_ID, timeSeriesId);
          values.put(TimeSeriesData.Datapoint.TS_START, ms);
          values.put(TimeSeriesData.Datapoint.TS_END, ms);
          values.put(TimeSeriesData.Datapoint.VALUE, 0);
          values.put(TimeSeriesData.Datapoint.UPDATES, 1);

          getContentResolver().insert(TimeSeriesData.Datapoint.CONTENT_URI,
              values);

          mCal.add(Calendar.MILLISECOND, period);
          setToPeriodStart(mCal, period);
        }
        dpCur.close();
      }
      tsCur.close();
      LockUtil.unlock(mLock);

      return;
    }
  }
  
  private void setToPeriodStart(Calendar c, int period) {
    if (period > YEAR_MS) {
      c.set(Calendar.MONTH, 0);
      c.set(Calendar.DAY_OF_MONTH, 1);
      c.set(Calendar.HOUR_OF_DAY, 0);
      c.set(Calendar.MINUTE, 0);
      c.set(Calendar.SECOND, 0);
      c.set(Calendar.MILLISECOND, 0);
    } else if (period > QUARTER_MS) {
      int month = c.get(Calendar.MONTH);
      if (month >= 9)
        c.set(Calendar.MONTH, 9);
      else if (month >= 9)
        c.set(Calendar.MONTH, 6);
      else if (month >= 9)
        c.set(Calendar.MONTH, 3);
      else
        c.set(Calendar.MONTH, 0);
      c.set(Calendar.DAY_OF_MONTH, 0);
      c.set(Calendar.HOUR_OF_DAY, 0);
      c.set(Calendar.MINUTE, 0);
      c.set(Calendar.SECOND, 0);
      c.set(Calendar.MILLISECOND, 0);
    } else if (period > MONTH_MS) {
      c.set(Calendar.DAY_OF_MONTH, 1);
      c.set(Calendar.HOUR_OF_DAY, 0);
      c.set(Calendar.MINUTE, 0);
      c.set(Calendar.SECOND, 0);
      c.set(Calendar.MILLISECOND, 0);
    } else if (period > WEEK_MS) {
      c.set(Calendar.DAY_OF_WEEK, c.getFirstDayOfWeek());
      c.set(Calendar.HOUR_OF_DAY, 0);
      c.set(Calendar.MINUTE, 0);
      c.set(Calendar.SECOND, 0);
      c.set(Calendar.MILLISECOND, 0);
    } else if (period > DAY_MS) {
      c.set(Calendar.HOUR_OF_DAY, 0);
      c.set(Calendar.MINUTE, 0);
      c.set(Calendar.SECOND, 0);
      c.set(Calendar.MILLISECOND, 0);
    } else if (period > AMPM_MS) {
      if (c.get(Calendar.AM_PM) == Calendar.AM)
        c.set(Calendar.HOUR_OF_DAY, 0);
      else
        c.set(Calendar.HOUR_OF_DAY, 12);
      c.set(Calendar.MINUTE, 0);
      c.set(Calendar.SECOND, 0);
      c.set(Calendar.MILLISECOND, 0);
    } else if (period > HOUR_MS) {
      c.set(Calendar.MINUTE, 0);
      c.set(Calendar.SECOND, 0);
      c.set(Calendar.MILLISECOND, 0);
    } else if (period > MINUTE_MS) {
      c.set(Calendar.SECOND, 0);
      c.set(Calendar.MILLISECOND, 0);
    }

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