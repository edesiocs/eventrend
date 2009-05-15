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

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import net.redgeek.android.eventrecorder.DateMapCache;
import net.redgeek.android.eventrecorder.IEventRecorderService;
import net.redgeek.android.eventrend.util.DialogUtil;
import net.redgeek.android.eventrend.util.GUITask;
import net.redgeek.android.eventrend.util.GUITaskQueue;

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

  // Common activity request codes
  public static final int ARC_CATEGORY_LIST = 1;
  public static final int ARC_CATEGORY_CREATE = 2;
  public static final int ARC_CATEGORY_EDIT = 3;
  public static final int ARC_ENTRY_LIST = 10;
  public static final int ARC_ENTRY_EDIT = 11;
  public static final int ARC_VISUALIZE_VIEW = 20;
  public static final int ARC_IMPORT_REPLACE = 40;
  public static final int ARC_IMPORT_MERGE = 41;
  public static final int ARC_PREFS_EDIT = 50;
  public static final int ARC_FORMULA_EDIT = 60;

  // Common activity result codes
  public static final int CATEGORY_CANCELED = RESULT_FIRST_USER + 1;
  public static final int CATEGORY_CREATED  = RESULT_FIRST_USER + 2;
  public static final int CATEGORY_MODIFIED = RESULT_FIRST_USER + 3;
  public static final int CATEGORY_OP_ERR   = RESULT_FIRST_USER + 10;

  protected Context mCtx;
  protected ContentResolver mContent;
  protected DialogUtil mDialogUtil;
  protected IEventRecorderService mRecorderService;
  protected EventRecorderConnection mRecorderConnection = null;
  protected DateMapCache mDateMapCache;

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);

    mCtx = this;
    mDialogUtil = new DialogUtil(mCtx);
    mContent = getContentResolver();
    mDateMapCache = new DateMapCache();
    mDateMapCache.populateCache(mCtx);
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
      startService(new Intent(IEventRecorderService.class.getName()));
      bindService(new Intent(new Intent(IEventRecorderService.class
          .getName())), mRecorderConnection, Context.BIND_AUTO_CREATE);
    } else {
      Toast.makeText(this, "Cannot bind, service already bound.",
          Toast.LENGTH_SHORT).show();
    }
  }

  public void unbindFromService() {
    if (mRecorderConnection != null) {
      unbindService(mRecorderConnection);
      mRecorderConnection = null;
    }
  }

  class EventRecorderConnection implements ServiceConnection {
    public void onServiceConnected(ComponentName className, IBinder service) {
      mRecorderService = IEventRecorderService.Stub.asInterface(service);
    }

    public void onServiceDisconnected(ComponentName className) {
      mRecorderService = null;
    }
  };  
}
