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

package net.redgeek.android.eventgrapher.primitives;

import android.graphics.Canvas;

import net.redgeek.android.eventgrapher.GraphView;
import net.redgeek.android.eventgrapher.TimeSeriesPainter;
import net.redgeek.android.eventrecorder.interpolators.CubicInterpolator;
import net.redgeek.android.eventrecorder.interpolators.TimeSeriesInterpolator;
import net.redgeek.android.eventrend.category.CategoryRow;
import net.redgeek.android.eventrend.util.Number;

import java.util.ArrayList;

/**
 * A representation of series of Datapoints plottable on screen. Specific
 * per-category.
 * 
 * @author barclay
 * 
 */
public final class TimeSeries {
  private ArrayList<Datapoint> mDatapoints;

  private double mVisibleMinY;
  private double mVisibleMaxY;
  private int   mVisibleMinX;
  private int   mVisibleMaxX;

  private int mTimestampLast;
  private int mNumEntries = 0;

  private boolean mEnabled;
  public CategoryRow mRow;

  // Drawing-related
  private float mTouchRadius = GraphView.POINT_TOUCH_RADIUS;
  private TimeSeriesPainter mPainter;

  // Various stats
  private Number.RunningStats mTimestampStats;

  // Interpolator
  private TimeSeriesInterpolator mInterpolator;

  public TimeSeries(CategoryRow row) {
    initialize(row);
  }

  private void initialize(CategoryRow row) {
    mRow = row;

    mInterpolator = new CubicInterpolator();
    mPainter = new TimeSeriesPainter.Default();
    mEnabled = false;
    setColor(row.mColor);
    resetMinMax();

    mTimestampStats = new Number.RunningStats();
  }

  @Override
  public String toString() {
    return mRow.mTimeSeriesName + " (id: " + mRow.mId + ")";
  }

  public void setPointRadius(float f) {
    mPainter.setPointRadius(f);
  }

  public void setInterpolator(TimeSeriesInterpolator i) {
    mInterpolator = i;
  }

  public TimeSeriesInterpolator getInterpolator() {
    return mInterpolator;
  }

  public void setTimeSeriesPainter(TimeSeriesPainter p) {
    mPainter = p;
  }

  public TimeSeriesPainter getTimeSeriesPainter() {
    return mPainter;
  }

  public void setEnabled(boolean b) {
    mEnabled = b;
  }

  public boolean isEnabled() {
    return mEnabled;
  }

  public int getVisibleNumEntries() {
    return mNumEntries;
  }

  public double getVisibleValueMin() {
    return mVisibleMinY;
  }

  public double getVisibleValueMax() {
    return mVisibleMaxY;
  }

  public int getVisibleTimestampMin() {
    return mVisibleMinX;
  }

  public int getVisibleTimestampMax() {
    return mVisibleMaxX;
  }

  public Number.RunningStats getTimestampStats() {
    return mTimestampStats;
  }

  public void recalcStatsAndBounds(double smoothing, int history) {
    mTimestampStats = new Number.RunningStats();
    calcStatsAndBounds();
  }

  private void calcStatsAndBounds() {
    int firstNEntries = 0;

    resetMinMax();

    if (mDatapoints == null)
      return;

    for (int i = 0; i < mDatapoints.size(); i++) {
      Datapoint d = mDatapoints.get(i);
      mVisibleMinY = Math.min(mVisibleMinY, d.mValue);
      mVisibleMaxY = Math.max(mVisibleMaxY, d.mValue);
      mVisibleMinX = Math.min(mVisibleMinX, d.mTsStart);
      mVisibleMaxX = Math.max(mVisibleMaxX, d.mTsStart);

      // we run stats on the y-values themselves,
      // but use the delta of the timestamps for x-stats:
      if (i > 0) {
        long delta = (d.mTsStart - mTimestampLast);
        mTimestampStats.update(delta, d.mEntries + firstNEntries);
      }
      mTimestampLast = (d.mTsStart);
      firstNEntries = 0;
      if (i == 0)
        firstNEntries = d.mEntries;

      // d.mTrend will be used for plotting the trend line,
      // so we don't want to change the x value, since that
      // should still be an absolute time
      d.mScreenTrend1.set(d.mScreenValue1);
      d.mScreenTrend2.set(d.mScreenValue2);

      mNumEntries += d.mEntries;
    }
  }

  public void setDatapoints(ArrayList<Datapoint> datapoints) {
    mDatapoints = datapoints;
    calcStatsAndBounds();
    return;
  }

  public Datapoint lookupDatapoint(FloatTuple press) {
    Datapoint d;

    if (isEnabled() == false)
      return null;

    int size = mDatapoints.size();
    for (int i = 0; i < size; i++) {
      d = mDatapoints.get(i);

      // TODO: check to make sure we only check in-range values
      if (press.x >= d.mScreenValue1.x - mTouchRadius
          && press.x <= d.mScreenValue1.x + mTouchRadius
          && press.y >= d.mScreenValue1.y - mTouchRadius
          && press.y <= d.mScreenValue1.y + mTouchRadius) {
        return d;
      }
      if (press.x >= d.mScreenValue2.x - mTouchRadius
          && press.x <= d.mScreenValue2.x + mTouchRadius
          && press.y >= d.mScreenValue2.y - mTouchRadius
          && press.y <= d.mScreenValue2.y + mTouchRadius) {
        return d;
      }
    }

    return null;
  }

  public Datapoint findNeighbor(long timestamp, boolean pre) {
    Datapoint d = null;

    if (mDatapoints == null || mDatapoints.size() < 1)
      return d;

    int min = 0;
    int max = mDatapoints.size() - 1;
    int mid = max / 2;

    d = mDatapoints.get(mid);
    while (d != null) {
      if (d.mTsStart == timestamp) {
        return d;
      } else if (max < min) {
        if (pre == true) {
          if (d.mTsStart > timestamp) {
            if (mid - 1 >= 0)
              d = mDatapoints.get(mid - 1);
            else
              d = null;
          }
        } else {
          if (d.mTsStart < timestamp) {
            if (mid + 1 < mDatapoints.size())
              d = mDatapoints.get(mid + 1);
            else
              d = null;
          }
        }
        return d;
      } else if (d.mTsStart < timestamp) {
        min = mid + 1;
      } else if (d.mTsStart > timestamp) {
        max = mid - 1;
      }
      mid = min + ((max - min) / 2);

      // Check to see if we were trying to run off the end, if so, just
      // return the first or last entry.
      if (mid >= mDatapoints.size() && pre == true)
        return d;
      if (mid < 0 && pre == false)
        return d;

      if (mid < 0 || mid > mDatapoints.size() - 1)
        break;
      d = mDatapoints.get(mid);
    }

    return null;
  }

  public ArrayList<Datapoint> getDatapoints() {
    return mDatapoints;
  }
  
  public Datapoint findPreNeighbor(long timestamp) {
    return findNeighbor(timestamp, true);
  }

  public Datapoint findPostNeighbor(long timestamp) {
    return findNeighbor(timestamp, false);
  }

  public void drawPath(Canvas canvas) {
    mPainter.drawPath(canvas, this);
  }

  public void drawTrend(Canvas canvas) {
    mPainter.drawTrend(canvas, this);
  }

  public void drawText(Canvas canvas, String s, float x, float y) {
    mPainter.drawText(canvas, s, x, y);
  }

  public void drawGoal(Canvas canvas, FloatTuple start, FloatTuple end) {
    mPainter.drawGoal(canvas, start, end);
  }

  public void drawMarker(Canvas canvas, FloatTuple start, FloatTuple end) {
    mPainter.drawMarker(canvas, start, end);
  }
  
  private void setColor(String color) {
    mRow.mColor = color;
    mPainter.setColor(color);
  }

  private void resetMinMax() {
    mVisibleMinY = Float.MAX_VALUE;
    mVisibleMaxY = Float.MIN_VALUE;
    mVisibleMinX = Integer.MAX_VALUE;
    mVisibleMaxX = Integer.MIN_VALUE;
  }
}