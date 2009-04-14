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

import java.util.ArrayList;

import net.redgeek.android.eventrecorder.interpolators.CubicInterpolator;
import net.redgeek.android.eventrecorder.interpolators.LinearInterpolator;
import net.redgeek.android.eventrecorder.interpolators.StepEarlyInterpolator;
import net.redgeek.android.eventrecorder.interpolators.StepLateInterpolator;
import net.redgeek.android.eventrecorder.interpolators.StepMidInterpolator;
import net.redgeek.android.eventrecorder.interpolators.TimeSeriesInterpolator;
import net.redgeek.android.eventrend.util.DialogUtil;
import net.redgeek.android.eventrend.util.GUITask;
import net.redgeek.android.eventrend.util.GUITaskQueue;
import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;

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
  
  private Context mCtx;
  private ContentResolver mContent;
  private DialogUtil mDialogUtil;

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

  public Context getCtx() {
    return mCtx;
  }

  public ContentResolver getContent() {
    return mContent;
  }

  public DialogUtil getDialogUtil() {
    return mDialogUtil;
  }
}
