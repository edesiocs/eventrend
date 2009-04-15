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

package net.redgeek.android.eventrend;

import net.redgeek.android.eventrecorder.IEventRecorderService;
import net.redgeek.android.eventrend.util.DialogUtil;
import net.redgeek.android.eventrend.util.GUITask;
import net.redgeek.android.eventrend.util.GUITaskQueue;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

/**
 * Base class for all activities in the application. Note that not all activity
 * need be ListActivities, but most are, and it's convenient to wrap a bunch of
 * common setup, teardown, and sundry tasks in this one. The only penalty is a
 * little extra overhead in activities that don't actually use a list, and the
 * requirement for (an invisible) ListView be present in the root xml layout
 * file for the activity.
 * 
 * <p>
 * The common elements are creating constants for Intent indices, common Bundle
 * keys, storing the Context of the activity, creating a DialogUtil instance,
 * creating and opening a DatabaseAdapter, and starting and stopping the
 * GUITaskQueue on pause and resume.
 * 
 * @author barclay
 * 
 */
public class EvenTrendActivity extends ListActivity implements GUITask {
  public static final String TAG = "EvenTrend";

  protected Context mCtx;
  protected ContentResolver mContent;
  protected DialogUtil mDialogUtil;
  protected IEventRecorderService mRecorderService;
  protected EventRecorderConnection mRecorderConnection = null;

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);

    mCtx = this;
    mDialogUtil = new DialogUtil(mCtx);
    mContent = getContentResolver();
  }

  @Override
  protected void onPause() {
    GUITaskQueue.getInstance().stop();
    super.onPause();
  }

  @Override
  protected void onResume() {
    GUITaskQueue.getInstance().start();
    super.onResume();
  }

  @Override
  protected void onDestroy() {
    super.onResume();
  }

  public void afterExecute() {
  }

  public void executeNonGuiTask() throws Exception {
  }

  public void onFailure(Throwable t) {
  }

  public void startAndBind() {
    if (mRecorderConnection == null) {
      mRecorderConnection = new EventRecorderConnection();
      ComponentName name = startService(new Intent(IEventRecorderService.class
          .getName()));
      if (name != null) {
        Toast.makeText(this, "Started service: " + name, Toast.LENGTH_SHORT)
            .show();
      } else {
        Toast.makeText(this, "Unable to start service.", Toast.LENGTH_SHORT)
            .show();
      }

      if (bindService(new Intent(new Intent(IEventRecorderService.class
          .getName())), mRecorderConnection, Context.BIND_AUTO_CREATE)) {
        Toast.makeText(this, "Bound to service.", Toast.LENGTH_SHORT).show();
      } else {
        Toast.makeText(this, "Unable to bind to service.", Toast.LENGTH_SHORT)
            .show();
      }
    } else {
      Toast.makeText(this, "Cannot bind, service already bound.",
          Toast.LENGTH_SHORT).show();
    }
  }

  public void unbindFromService() {
    if (mRecorderConnection != null) {
      unbindService(mRecorderConnection);
      Toast.makeText(this, "Unbound from service.", Toast.LENGTH_SHORT).show();
      mRecorderConnection = null;
    } else {
      Toast.makeText(this, "Cannot unbind, service not bound.",
          Toast.LENGTH_SHORT).show();
    }
  }

  class EventRecorderConnection implements ServiceConnection {
    public void onServiceConnected(ComponentName className, IBinder service) {
      mRecorderService = IEventRecorderService.Stub.asInterface(service);
      Toast.makeText(mCtx, "onServiceConnected.", Toast.LENGTH_SHORT).show();
    }

    public void onServiceDisconnected(ComponentName className) {
      mRecorderService = null;
      Toast.makeText(mCtx, "onServiceDisconnected.", Toast.LENGTH_SHORT).show();
    }
  };
}
