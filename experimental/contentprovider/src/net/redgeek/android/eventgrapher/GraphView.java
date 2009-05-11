/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.redgeek.android.eventgrapher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.TextView;
import android.widget.ZoomControls;

import net.redgeek.android.eventgrapher.primitives.FloatTuple;
import net.redgeek.android.eventrend.Preferences;
import net.redgeek.android.eventrend.util.DateUtil;
import net.redgeek.android.eventrend.util.GUITask;
import net.redgeek.android.eventrend.util.GUITaskQueue;
import net.redgeek.android.eventrend.util.ProgressIndicator;
import net.redgeek.android.eventrend.util.DateUtil.Period;

import java.util.Calendar;

/**
 * By far the most convoluted and dis-organized of classes, Currently undergoing
 * refactoring to minimize the noodles.
 * 
 * @author barclay
 * 
 */
public class GraphView extends View implements OnLongClickListener, GUITask {
  // Various UI parameters
  public static final int TOP_MARGIN = 5;
  public static final int LEFT_MARGIN = 40;
  public static final int RIGHT_MARGIN = 10;
  public static final int BOTTOM_MARGIN = 40;
  public static final int X_TICK = 3;
  public static final int Y_TICK = 3;
  public static final float POINT_RADIUS = 2.0f;
  public static final int PATH_WIDTH = 2;
  public static final int TREND_WIDTH = 1;
  public static final int GOAL_WIDTH = 1;
  public static final int AXIS_WIDTH = 1;
  public static final int LABEL_WIDTH = 1;
  public static final int TREND_DASH_WIDTH = 4;
  public static final int GOAL_DASH_WIDTH = 2;
  public static final int TICK_LENGTH = 5;
  public static final float TICK_MIN_DISTANCE = 25.0f;
  public static final float ZOOM_FACTOR = 1.1f;
  public static final int TEXT_HEIGHT = 15;
  public static final int PLOT_TOP_PAD = 10;
  public static final int PLOT_BOTTOM_PAD = 15;
  public static final float POINT_TOUCH_RADIUS = 20.0f;

  public static final int ZOOM_CTRL_HIDE_MS = 3000;
  public static final int ZOOM_CTRL_LINES_MS = 200;

  public static final float MINIMUM_DELTA = 0.00001f;
  
  // UI elements
  private ZoomControls mZoomControls;
  private Graph mGraph;
  private ProgressIndicator.Titlebar mProgress;
  private TextView mStatus;
  private Canvas mCanvas;

  // Listeners
  private View.OnClickListener mZoomInListener;
  private View.OnClickListener mZoomOutListener;

  // Private data
  private Context mCtx;
  private TimeSeriesCollector mTSC;
  private Calendar mCal;
  private FloatTuple mLastEvent;

  // Tasks and handlers
  private Handler mZoomHandler;
  private Runnable mHideZoomControls;
  private DataCollectionTask mCollector;

  public GraphView(Context context, ZoomControls zoomControls,
      TimeSeriesCollector tsc) {
    super(context);
    mCtx = context;
    mTSC = tsc;

    setupData();
    setupUI(zoomControls);
  }

  private void setupData() {
    mCal = Calendar.getInstance();

    mCollector = new DataCollectionTask(mTSC);
    mGraph = new Graph(mCtx, mTSC, getWidth(), getHeight());
    mZoomHandler = new Handler();
  }

  private void setupUI(ZoomControls zoomControls) {
    setupListeners();

    mStatus = ((GraphActivity) mCtx).getGraphStatusTextView();
    setColorScheme();
    setFocusableInTouchMode(true);
    setOnLongClickListener(this);
    mZoomControls = zoomControls;
    mZoomControls.setOnZoomInClickListener(mZoomInListener);
    mZoomControls.setOnZoomOutClickListener(mZoomOutListener);
    mProgress = new ProgressIndicator.Titlebar(mCtx);

    mHideZoomControls = new Runnable() {
      public void run() {
        mZoomControls.setVisibility(View.INVISIBLE);
      }
    };
  }

  private void setupListeners() {
    mZoomInListener = new View.OnClickListener() {
      public void onClick(View v) {
        scheduleHideZoomControls();
        long start = mGraph.getGraphStart();
        long end = mGraph.getGraphEnd();

        long delta = (end - start) / 4;
        start += delta;
        end -= delta;

        if (start + DateUtil.MINUTE_MS * 10 >= end) {
          end += ((DateUtil.MINUTE_MS * 10) / 2);
          start = end - ((DateUtil.MINUTE_MS * 10) / 2);
        }

        mGraph.setGraphRange(start, end);
        updateData();
      }
    };

    mZoomOutListener = new View.OnClickListener() {
      public void onClick(View v) {
        scheduleHideZoomControls();

        long start = mGraph.getGraphStart();
        long end = mGraph.getGraphEnd();

        long delta = (end - start) / 2;
        start -= delta;
        end += delta;
        if (start < 0)
          start = 0;

        mGraph.setGraphRange(start, end);
        updateData();
      }
    };
  }

  public void setColorScheme() {
    if (Preferences.getDefaultGraphIsBlack(mCtx) == true)
      setBackgroundColor(Color.BLACK);
    else
      setBackgroundColor(Color.WHITE);
    mGraph.setColorScheme();
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    mGraph.setGraphSize(w, h);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    scheduleHideZoomControls();
    if (event.getAction() == MotionEvent.ACTION_DOWN) {
      requestFocus();
      mLastEvent = new FloatTuple(event.getX(), event.getY());
      FloatTuple plotCoords = new FloatTuple(mLastEvent);
      plotCoords.minus(mGraph.getPlotOffset());
      lookupDatapoint(plotCoords);
      return super.onTouchEvent(event);
    }
    if (event.getAction() != MotionEvent.ACTION_MOVE) {
      mLastEvent = null;
      return super.onTouchEvent(event);
    }

    FloatTuple movement = new FloatTuple(event.getX(), event.getY());
    if (mLastEvent != null) {
      movement = movement.minus(mLastEvent);
    } else if (event.getHistorySize() > 0) {
      movement = movement.minus(new FloatTuple(event.getHistoricalX(0), event
          .getHistoricalY(0)));
    } else {
      mLastEvent = movement;
      return super.onTouchEvent(event);
    }
    mLastEvent = null;

    long start = mGraph.getGraphStart();
    long end = mGraph.getGraphEnd();

    movement.x *= ((end - start) / getWidth());
    movement.x *= -1;
    movement.y = 0;

    end += movement.x;
    if (end < 0)
      end = 0;

    start += movement.x;
    if (start < 0)
      start = 0;

    mGraph.setGraphRange(start, end);
    updateData();
    return super.onTouchEvent(event);
  }

  private void scheduleHideZoomControls() {
    mZoomHandler.removeCallbacks(mHideZoomControls);
    mZoomHandler.postDelayed(mHideZoomControls, ZOOM_CTRL_HIDE_MS);
  }

  public boolean onLongClick(View v) {
    scheduleHideZoomControls();
    mZoomControls.setVisibility(View.VISIBLE);
    return true;
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    if (getWidth() > 0 && getHeight() > 0) {
      mCanvas = canvas;
      mGraph.plot(mCanvas);
    }
  }

  public void executeNonGuiTask() throws Exception {
    long start = mGraph.getGraphStart();
    long end = mGraph.getGraphEnd();

    mCollector.setSpan(start, end);
    mCollector.doCollection();
  }

  public void afterExecute() {
    updateStatus();
    setFocusable(true);
    invalidate();
  }

  public void onFailure(Throwable t) {
  }

  public void updateData() {
    long start = mGraph.getGraphStart();
    long end = mGraph.getGraphEnd();

    mGraph.setGraphRange(start, end);

    // Useful debugging: uncomment the following, and comment out the
    // addTask() below -- this makes the data collection run synchronously.
    // mCollector.setSpan(start, end);
    // mCollector.doCollection();
    // updateStatus();
    // invalidate();

    GUITaskQueue.getInstance().addTask(mProgress, this);
  }

  private void updateStatus() {
    long start = mGraph.getGraphStart();
    long end = mGraph.getGraphEnd();
    String str = new String();

    if (mTSC.getAutoAggregation() == true) {
      Period span = mGraph.getSpan();
      str = "Aggregation: " + DateUtil.mapPeriodToString(span) + " ";
    }

    if (mGraph.mSelectedDatapoint != null) {
      str += mGraph.mSelectedDatapoint.toLabelString() + ": "
          + mGraph.mSelectedDatapoint.mY.y;
      mStatus.setText(str);
      mStatus.setTextColor(mGraph.mSelectedColor);
      return;
    }

    if (mTSC.getAutoAggregation() == false) {
      str += DateUtil.toTimestamp(start) + " - " + DateUtil.toTimestamp(end);
    }
    mStatus.setText(str);
    mStatus.setTextColor(Color.GRAY);
  }

  public void snapToSpan() {
    long start = mGraph.getGraphStart();
    long end = mGraph.getGraphEnd();

    long delta = end - start;
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(start);
    DateUtil.setToPeriodStart(cal, mGraph.getSpan());
    start = cal.getTimeInMillis();
    end = start + delta;

    mGraph.setGraphRange(start, end);

    Log.v("graphview", "snapToSpan() -> updateData");

    updateData();
  }

  public void resetZoom() {
    long start = mGraph.getGraphStart();
    long end = mGraph.getGraphEnd();

    end = mCal.getTimeInMillis();
    start = end - DateUtil.DAY_MS * 7;

    mGraph.setGraphRange(start, end);
  }

  public void setTrendView(boolean b) {
    mGraph.viewTrends(b);
  }

  public void setGoalView(boolean b) {
    mGraph.viewGoals(b);
  }

  public void setMarkersView(boolean b) {
    mGraph.viewMarkers(b);
  }

  public void lookupDatapoint(FloatTuple t) {
    mGraph.lookupDatapoint(t);
    updateStatus();
  }

  public Graph getGraph() {
    return mGraph;
  }
}