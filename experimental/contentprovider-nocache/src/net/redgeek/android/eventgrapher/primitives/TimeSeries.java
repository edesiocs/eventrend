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
import net.redgeek.android.eventrecorder.interpolators.TimeSeriesInterpolator;
import net.redgeek.android.eventrend.category.CategoryRow;
import net.redgeek.android.eventrend.util.Number;

import java.util.ArrayList;
import java.util.List;

/**
 * A representation of series of Datapoints plottable on screen. Specific
 * per-category.
 * 
 * @author barclay
 * 
 */
public final class TimeSeries {
  // All of the following are ordered via x-values (time), and are references
  // to
  // datapoints in the DataCache. These are just the datapoints necessary
  // for graphing the time series on screen, not an exhaustive list. Multiple
  // datapoints are required for VisiblePre (if available), as they are needed
  // to calculate the trend line accurately and continue the graph line to the
  // left edge of the screen, and at least one datapoint is needed
  // in VisiblePost (if available) in order to connect the last on-screen
  // point
  // to something offscreen, in order to continue drawing the line to the edge
  // of
  // the graph.
  private ArrayList<Datapoint> mDatapoints; // a concatenation of the
  // following:
  private int mVisiblePreFirstIdx;
  private int mVisiblePreLastIdx;
  private int mVisibleFirstIdx;
  private int mVisibleLastIdx;
  private int mVisiblePostFirstIdx;
  private int mVisiblePostLastIdx;

  // Various stats used for bounding
  private float mVisibleMinY;
  private float mVisibleMaxY;
  private long mVisibleMinX;
  private long mVisibleMaxX;

  private long mTimestampLast;
  private int mNumEntries = 0;

  private boolean mEnabled;
  public CategoryRow mRow;

  // Drawing-related
  private float mTouchRadius = GraphView.POINT_TOUCH_RADIUS;
  private String mColorStr;
  private TimeSeriesPainter mPainter;

  // Various stats
  private Number.RunningStats mTimestampStats;

  // Interpolator
  private TimeSeriesInterpolator mInterpolator;

  public TimeSeries(CategoryRow row, int history, double smoothing) {
    initialize(row, history, smoothing, null);
  }

  public TimeSeries(CategoryRow row, int history, double smoothing,
      TimeSeriesPainter painter) {
    initialize(row, history, smoothing, painter);
  }

  private void initialize(CategoryRow row, int history,
      double smoothing, TimeSeriesPainter painter) {
    mRow = row;

    mPainter = painter;
    if (painter == null)
      mPainter = new TimeSeriesPainter.Default();

    mDatapoints = new ArrayList<Datapoint>();
    mEnabled = false;
    setColor(row.mColor);
    // set thusly so we can apply min()/max() operators indiscriminately
    resetMinMax();
    resetIndices();

    mTimestampStats = new Number.RunningStats();
  }

  @Override
  public String toString() {
    return mRow.mTimeSeriesName + " (id: " + mRow.mId + ")";
  }

  public void clearSeries() {
    resetIndices();
    mDatapoints.clear();
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

  public void setEnabled(boolean b) {
    mEnabled = b;
  }

  public boolean isEnabled() {
    return mEnabled;
  }

  public int getVisibleNumEntries() {
    return mNumEntries;
  }

  public float getVisibleValueMin() {
    return mVisibleMinY;
  }

  public float getVisibleValueMax() {
    return mVisibleMaxY;
  }

  public long getVisibleTimestampMin() {
    return mVisibleMinX;
  }

  public long getVisibleTimestampMax() {
    return mVisibleMaxX;
  }

  public Number.RunningStats getTimestampStats() {
    return mTimestampStats;
  }

  public int getVisiblePreFirstIdx() {
    return mVisiblePreFirstIdx;
  }

  public int getVisiblePreLastIdx() {
    return mVisiblePreLastIdx;
  }

  public int getVisibleFirstIdx() {
    return mVisibleFirstIdx;
  }

  public int getVisibleLastIdx() {
    return mVisibleLastIdx;
  }

  public int getVisiblePostFirstIdx() {
    return mVisiblePostFirstIdx;
  }

  public int getVisiblePostLastIdx() {
    return mVisiblePostLastIdx;
  }

  public String getColor() {
    return mColorStr;
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
      if (i >= mVisibleFirstIdx && i <= mVisibleLastIdx) {
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
      } else {
        d.mScreenTrend1.set(d.mScreenValue1);
        d.mScreenTrend2.set(d.mScreenValue2);
      }
    }
    interpolateBoundsToOffscreen();
  }

  public void setDatapoints(ArrayList<Datapoint> pre,
      ArrayList<Datapoint> range, ArrayList<Datapoint> post, boolean recalc) {

    clearSeries();
    if (pre != null && pre.size() > 0) {
      mVisiblePreFirstIdx = 0;
      mVisiblePreLastIdx = pre.size() - 1;
      mDatapoints.addAll(pre);
    }
    if (range != null && range.size() > 0) {
      mVisibleFirstIdx = mDatapoints.size();
      mVisibleLastIdx = mVisibleFirstIdx + range.size() - 1;
      mDatapoints.addAll(range);
    }
    if (post != null && post.size() > 0) {
      mVisiblePostFirstIdx = mDatapoints.size();
      mVisiblePostLastIdx = mVisiblePostFirstIdx + post.size() - 1;
      mDatapoints.addAll(post);
    }
    if (recalc == true)
      calcStatsAndBounds();
    return;
  }

  public Datapoint getLastPreVisible() {
    if (mVisiblePreLastIdx >= 0 && mVisiblePreLastIdx < mDatapoints.size())
      return mDatapoints.get(mVisiblePreLastIdx);
    return null;
  }

  public Datapoint getFirstVisible() {
    if (mVisibleFirstIdx >= 0 && mVisibleFirstIdx < mDatapoints.size())
      return mDatapoints.get(mVisibleFirstIdx);
    return null;
  }

  public Datapoint getLastVisible() {
    if (mVisibleLastIdx >= 0 && mVisibleLastIdx < mDatapoints.size())
      return mDatapoints.get(mVisibleLastIdx);
    return null;
  }

  public Datapoint getFirstPostVisible() {
    if (mVisiblePostFirstIdx >= 0 && mVisiblePostFirstIdx < mDatapoints.size())
      return mDatapoints.get(mVisiblePostFirstIdx);
    return null;
  }

  public Datapoint lookupVisibleDatapoint(FloatTuple press) {
    Datapoint d;

    if (isEnabled() == false)
      return null;

    if (mVisibleFirstIdx != Integer.MIN_VALUE
        && mVisibleLastIdx != Integer.MAX_VALUE) {
      for (int i = mVisibleFirstIdx; i <= mVisibleLastIdx; i++) {
        d = mDatapoints.get(i);

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

  public List<Datapoint> getVisiblePre() {
    List<Datapoint> view = null;
    if (mVisiblePreFirstIdx != Integer.MIN_VALUE
        && mVisiblePreLastIdx != Integer.MAX_VALUE) {
      try {
        view = mDatapoints.subList(mVisiblePreFirstIdx, mVisiblePreLastIdx + 1);
      } catch(IndexOutOfBoundsException e) {
        // nothing
      } catch(IllegalArgumentException e) {
        // nothing
      }
    }
    return view;
  }

  public List<Datapoint> getVisible() {
    List<Datapoint> view = null;
    if (mVisibleFirstIdx != Integer.MIN_VALUE
        && mVisibleLastIdx != Integer.MAX_VALUE) {
      try {
        view = mDatapoints.subList(mVisibleFirstIdx, mVisibleLastIdx + 1);
      } catch(IndexOutOfBoundsException e) {        
        // nothing
      } catch(IllegalArgumentException e) {
        // nothing
      }
    }
    return view;
  }

  public List<Datapoint> getVisiblePost() {
    List<Datapoint> view = null;
    if (mVisiblePostFirstIdx != Integer.MIN_VALUE
        && mVisiblePostLastIdx != Integer.MAX_VALUE) {
      try {
        view =mDatapoints.subList(mVisiblePostFirstIdx, mVisiblePostLastIdx + 1);
      } catch(IndexOutOfBoundsException e) {
        // nothing
      } catch(IllegalArgumentException e) {
        // nothing
      }
    }
    return view;
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
  
  private void interpolateBoundsToOffscreen() {
    int nDatapoints;
    Datapoint d1 = null;
    Datapoint d2 = null;
    float iy = 0.0f;

    List<Datapoint> preVisible = getVisiblePre();
    List<Datapoint> visible = getVisible();
    List<Datapoint> postVisible = getVisiblePost();

    nDatapoints = mDatapoints.size();
    if (nDatapoints < 2)
      return;

    if (postVisible != null) {
      // datapoints after visible range to interpolate to
      d2 = postVisible.get(0);
      if (visible != null) {
        // also datapoints in visible range
        d1 = visible.get(visible.size() - 1);
      } else if (preVisible != null) {
        // no datapoints in visible range, grab last from before the
        // visible range
        d1 = preVisible.get(preVisible.size() - 1);
      } else {
        // no datapoints to connect it to
        return;
      }

      if (d2.mTsStart > d1.mTsStart) {
        iy = (d2.mValue - d1.mValue) / (d2.mTsStart - d1.mTsStart);
        if (iy > 0 && d2.mValue > mVisibleMaxY) {
          mVisibleMaxY = d2.mValue;
        }
        if (iy < 0 && d2.mValue < mVisibleMinY) {
          mVisibleMinY = d2.mValue;
        }
      }
    }
    if (preVisible != null) {
      // we have a datapoint before the beginning to interpolate
      d1 = preVisible.get(preVisible.size() - 1);
      if (visible != null) {
        // also datapoints in visible range
        d2 = visible.get(0);
      } else if (postVisible != null) {
        // no datapoints in visible range, grab first from beyond the
        // visible range
        d2 = postVisible.get(0);
      } else {
        // no datapoints to connect it to
        return;
      }

      if (d1.mTsStart < d2.mTsStart) {
        iy = (d2.mValue - d1.mValue) / (d2.mTsStart - d1.mTsStart);
        if (iy < 0 && d1.mValue > mVisibleMaxY) {
          mVisibleMaxY = d1.mValue;
        }
        if (iy > 0 && d1.mValue < mVisibleMinY) {
          mVisibleMinY = d1.mValue;
        }
      }
    }
  }

  private void setColor(String color) {
    mColorStr = color;
    mPainter.setColor(color);
  }

  private void resetIndices() {
    mVisiblePreFirstIdx = Integer.MIN_VALUE;
    mVisiblePreLastIdx = Integer.MIN_VALUE;
    mVisibleFirstIdx = Integer.MIN_VALUE;
    mVisibleLastIdx = Integer.MIN_VALUE;
    mVisiblePostFirstIdx = Integer.MIN_VALUE;
    mVisiblePostLastIdx = Integer.MIN_VALUE;
  }

  private void resetMinMax() {
    mVisibleMinY = Float.MAX_VALUE;
    mVisibleMaxY = Float.MIN_VALUE;
    mVisibleMinX = Long.MAX_VALUE;
    mVisibleMaxX = Long.MIN_VALUE;
  }
}