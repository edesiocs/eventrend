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

import net.redgeek.android.eventrend.R;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class SampleClient extends Activity {
  private static final String TAG = "SampleClient";
  private IEventRecorderService mEventRecorder;
  private EventRecorderConnection mConn = null;
  private ContentResolver mContent;
  private Context mCtx;

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    mCtx = this;
    mContent = getContentResolver();

    setContentView(R.layout.sample_client);

    startAndBind();
    populateData();

    Button seriesButton = (Button) findViewById(R.id.series_view);
    seriesButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        showSeries();
      }
    });
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    unbindFromService();
  }

  private void startAndBind() {
    if (mConn == null) {
      mConn = new EventRecorderConnection();
      // ComponentName name = startService(new Intent("net.redgeek.android.eventrecorder.IEventRecorderService"));      
      // or:
      ComponentName name = startService(new Intent(IEventRecorderService.class.getName()));      
      if (name != null) {
        Toast.makeText(this, "Started service: " + name, Toast.LENGTH_SHORT).show();
      } else {
        Toast.makeText(this, "Unable to start service.", Toast.LENGTH_SHORT).show();
      }
      
      if (bindService(new Intent(new Intent(IEventRecorderService.class.getName())), 
          mConn, Context.BIND_AUTO_CREATE)) {
        Toast.makeText(this, "Bound to service.", Toast.LENGTH_SHORT).show();
      } else {
        Toast.makeText(this, "Unable to bind to service.", Toast.LENGTH_SHORT).show();
      }
    } else {
      Toast.makeText(this, "Cannot bind, service already bound.",
          Toast.LENGTH_SHORT).show();
    }
  }

  private void unbindFromService() {
    if (mConn != null) {
      unbindService(mConn);

      stopService(new Intent(IEventRecorderService.class.getName()));
      Toast.makeText(this, "Unbound from service.", Toast.LENGTH_SHORT).show();
      mConn = null;
    } else {
      Toast.makeText(this, "Cannot unbind, service not bound.",
          Toast.LENGTH_SHORT).show();
    }
  }
  
  private void populateData() {
    Cursor c = getContentResolver().query(TimeSeriesData.TimeSeries.CONTENT_URI, 
        null, null, null, null);
    if (c.getCount() > 0) {
      c.close();
      return;
    }
    
    ContentValues values = new ContentValues();

    values.put(TimeSeriesData.TimeSeries.TIMESERIES_NAME, "Series One");
    values.put(TimeSeriesData.TimeSeries.TIMESERIES_NAME, "Group Name");
    values.put(TimeSeriesData.TimeSeries.RECORDING_DATAPOINT_ID, 0);
    values.put(TimeSeriesData.TimeSeries.DEFAULT_VALUE, 0.0f);
    values.put(TimeSeriesData.TimeSeries.INCREMENT, 1.0f);
    values.put(TimeSeriesData.TimeSeries.GOAL, 0.0f);
    values.put(TimeSeriesData.TimeSeries.COLOR, "#cccccc");
    values.put(TimeSeriesData.TimeSeries.PERIOD, 0);
    values.put(TimeSeriesData.TimeSeries.RANK, 1);
    values.put(TimeSeriesData.TimeSeries.AGGREGATION, "sum");
    values.put(TimeSeriesData.TimeSeries.TYPE, "discrete");
    values.put(TimeSeriesData.TimeSeries.ZEROFILL, 0);
    values.put(TimeSeriesData.TimeSeries.FORMULA, "");
    values.put(TimeSeriesData.TimeSeries.UNITS, "");
    values.put(TimeSeriesData.TimeSeries.INTERPOLATION, "cubic");
    
    values.put(TimeSeriesData.TimeSeries.TIMESERIES_NAME, "Series One");
    getContentResolver().insert(TimeSeriesData.TimeSeries.CONTENT_URI, values);

    values.put(TimeSeriesData.TimeSeries.TIMESERIES_NAME, "Series Two");
    getContentResolver().insert(TimeSeriesData.TimeSeries.CONTENT_URI, values);
  }

  private void showSeries() {
    try {
      String data = "";
      String projection[] = new String[] { TimeSeriesData.TimeSeries.TIMESERIES_NAME };
      Uri mTimeSeries = TimeSeriesData.TimeSeries.CONTENT_URI;

      // Query the content provider
      Cursor c = managedQuery(mTimeSeries, projection, null, null, null);
      if (c.moveToFirst()) {
        String name = null;
        do {
          name = c.getString(c.getColumnIndex(projection[0]));

          // Query the service
          if (mEventRecorder != null) {
            long id = mEventRecorder.getTimeSeriesId(name);
            data += id + ": " + name + "\n";
          } else {
            data += "(service not avail): " + name + "\n";            
          }
        } while (c.moveToNext());
      }

      TextView t = (TextView) findViewById(R.id.data_view);
      t.setText(data);
    } catch (Exception ex) {
      Toast.makeText(this, "Caught exception.", Toast.LENGTH_SHORT).show();
    }
    return;
  }

  class EventRecorderConnection implements ServiceConnection {
    public void onServiceConnected(ComponentName className, IBinder service) {
      mEventRecorder = IEventRecorderService.Stub.asInterface(service);
      Toast.makeText(mCtx, "onServiceConnected.", Toast.LENGTH_SHORT).show();
    }

    public void onServiceDisconnected(ComponentName className) {
      mEventRecorder = null;
      Toast.makeText(mCtx, "onServiceDisconnected.", Toast.LENGTH_SHORT).show();
    }
  };
}
