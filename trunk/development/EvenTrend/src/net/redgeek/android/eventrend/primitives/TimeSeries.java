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

package net.redgeek.android.eventrend.primitives;

import android.graphics.Canvas;

import net.redgeek.android.eventrend.db.CategoryDbTable;
import net.redgeek.android.eventrend.graph.GraphView;
import net.redgeek.android.eventrend.graph.TimeSeriesPainter;
import net.redgeek.android.eventrend.graph.plugins.TimeSeriesInterpolator;
import net.redgeek.android.eventrend.synthetic.AST;
import net.redgeek.android.eventrend.util.DateUtil;
import net.redgeek.android.eventrend.util.Number;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.Map.Entry;

/** A representation of series of Datapoints plottable on screen.  
 * Specific per-category.
 * 
 * @author barclay
 *
 */
public final class TimeSeries {	
  // All of the following are ordered via x-values (time), and are references to
  // datapoints in the DataCache.  These are just the datapoints necessary
  // for graphing the time series on screen, not an exhaustive list.  Multiple
  // datapoints are required for VisiblePre (if available), as they are needed 
  // to calculate the trend line accurately and continue the graph line to the
  // left edge of the screen, and at least one datapoint is needed
  // in VisiblePost (if available) in order to connect the last on-screen point
  // to something offscreen, in order to continue drawing the line to the edge of
  // the graph.
  private ArrayList<Datapoint> mDatapoints; // a concatenation of the following:
  private int mVisiblePreFirstIdx;
  private int mVisiblePreLastIdx;
  private int mVisibleFirstIdx;
  private int mVisibleLastIdx;
  private int mVisiblePostFirstIdx;
  private int mVisiblePostLastIdx;

  // Various stats used for bounding
  private float    mVisibleMinY;
  private float    mVisibleMaxY;
  private long     mVisibleMinX;
  private long     mVisibleMaxX;

  private long     mTimestampLast;
  private int	     mNumEntries = 0;

  private boolean     mEnabled;
  CategoryDbTable.Row mDbRow;

  private ArrayList<TimeSeries> mDependents;
  private ArrayList<TimeSeries> mDependees;	

  // Drawing-related
  private float             mTouchRadius = GraphView.POINT_TOUCH_RADIUS;
  private String            mColorStr;
  private TimeSeriesPainter mPainter;

  // Various stats
  private Number.RunningStats   mValueStats;
  private Number.Trend          mValueTrend;
  private Number.RunningStats   mTimestampStats;
  private Number.WindowedStdDev mStdDevWindow;

  // Interpolator
  private TimeSeriesInterpolator mInterpolator;

  public TimeSeries(CategoryDbTable.Row row, int history, 
      float smoothing, TimeSeriesPainter painter) {
    mDbRow = row;

    mDatapoints = new ArrayList<Datapoint>();

    mPainter = painter;
    if (painter == null)
      mPainter = new TimeSeriesPainter.Default();

    mEnabled = false;
    setColor(row.getColor());
    // set thusly so we can apply min()/max() operators indiscriminately
    resetMinMax();
    resetIndices();

    mValueStats     = new Number.RunningStats();
    mTimestampStats = new Number.RunningStats();
    mValueTrend     = new Number.Trend(smoothing);
    mStdDevWindow   = new Number.WindowedStdDev(history);

    mDependents = new ArrayList<TimeSeries>();
    mDependees  = new ArrayList<TimeSeries>();	
  }

  public TimeSeries(TimeSeries series) {
    mDbRow = new CategoryDbTable.Row(series.mDbRow);

    mDatapoints = new ArrayList<Datapoint>();
    mVisiblePreFirstIdx  = series.mVisiblePreFirstIdx;
    mVisiblePreLastIdx   = series.mVisiblePreLastIdx;
    mVisibleFirstIdx     = series.mVisibleFirstIdx;
    mVisibleLastIdx      = series.mVisibleLastIdx;
    mVisiblePostFirstIdx = series.mVisiblePostFirstIdx;
    mVisiblePostLastIdx  = series.mVisiblePostLastIdx;

    copyDatapoints(series);
    mEnabled     = series.mEnabled;
    mVisibleMinY = series.mVisibleMinY;
    mVisibleMaxY = series.mVisibleMaxY;
    mVisibleMinX = series.mVisibleMinX;
    mVisibleMaxX = series.mVisibleMaxX;

    mTimestampLast     = series.mTimestampLast;
    mNumEntries        = series.mNumEntries;
    mEnabled           = series.mEnabled;

    mValueStats     = new Number.RunningStats(series.mValueStats);
    mTimestampStats = new Number.RunningStats(series.mTimestampStats);
    mValueTrend     = new Number.Trend(series.mValueTrend);
    mStdDevWindow   = new Number.WindowedStdDev(series.mStdDevWindow);

    mDependents = new ArrayList<TimeSeries>();
    for (int i = 0; i < mDependents.size(); i++) {
      mDependents.add(series.mDependents.get(i));
    }
    mDependees  = new ArrayList<TimeSeries>();	
    for (int i = 0; i < mDependees.size(); i++) {
      mDependees.add(series.mDependees.get(i));
    }

    mPainter = series.mPainter;

    // this should be fine to share; it should be stateless
    mInterpolator = series.mInterpolator;
  }

  @Override
  public String toString() {
    return mDbRow.getCategoryName() + " (id: " + mDbRow.getId() + ")";
  }

  public void clearSeries() {
    resetIndices();
    mDatapoints.clear();
  }

  public void setPointRadius(float f) {
    mPainter.setPointRadius(f);
  }

  public CategoryDbTable.Row getDbRow() {
    return mDbRow;
  }

  public void setDbRow(CategoryDbTable.Row row) {
    mDbRow = row;
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

  public void addDependent(TimeSeries ts) {
    if (mDependents.contains(ts) != true)
      mDependents.add(ts);
  } 

  public ArrayList<TimeSeries> getDependents() {
    return mDependents;
  } 

  public void addDependee(TimeSeries ts) {
    if (mDependees.contains(ts) != true)
      mDependees.add(ts);
  } 

  public ArrayList<TimeSeries> getDependees() {
    return mDependees;
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
  
  public Number.RunningStats getValueStats() {
    return mValueStats;
  }

  public Number.RunningStats getTimestampStats() {
    return mTimestampStats;
  }

  public Number.Trend getTrendStats() {
    return mValueTrend;
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

  public void recalcStatsAndBounds(float smoothing, int history) {
    mValueStats     = new Number.RunningStats();
    mTimestampStats = new Number.RunningStats();
    mValueTrend     = new Number.Trend(smoothing);
    mStdDevWindow   = new Number.WindowedStdDev(history);
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
        mVisibleMinY = Math.min(mVisibleMinY, d.mValue.y);
        mVisibleMaxY = Math.max(mVisibleMaxY, d.mValue.y);
        mVisibleMinX = Math.min(mVisibleMinX, d.mMillis);
        mVisibleMaxX = Math.max(mVisibleMaxX, d.mMillis);
        
        // we run stats on the y-values themselves ...
        mValueStats.update(d.mValue.y, d.mNEntries);
        mValueTrend.update(d.mValue.y);
        mStdDevWindow.update(d.mValue.y);
        d.mStdDev = mStdDevWindow.getStandardDev();
        // but use the delta of the timestamps for x-stats:
        if (i > 0) {
          long delta = (d.mMillis - mTimestampLast);
          mTimestampStats.update(delta, d.mNEntries + firstNEntries);
        }
        mTimestampLast = (d.mMillis);
        firstNEntries = 0;
        if (i == 0)
          firstNEntries = d.mNEntries;

        // d.mTrend will be used for plotting the trend line,
        // so we don't want to change the x value, since that
        // should still be an absolute time
        d.mTrend.x = d.mValue.x;
        d.mTrend.y = mValueTrend.mTrend;

        mNumEntries += d.mNEntries;       
      } else {
        mValueTrend.update(d.mValue.y);
        d.mTrend.x = d.mValue.x;
        d.mTrend.y = mValueTrend.mTrend;

        mStdDevWindow.update(d.mValue.y);
        d.mStdDev = mStdDevWindow.getStandardDev();
      }
    }
    interpolateBoundsToOffscreen();
  }

  public void setDatapoints(ArrayList<Datapoint> pre, ArrayList<Datapoint> range,
      ArrayList<Datapoint> post) {

    clearSeries();
    if (pre != null && pre.size() > 0) {
      mVisiblePreFirstIdx = 0;
      mVisiblePreLastIdx = pre.size() - 1;
      mDatapoints.addAll(pre);
    }
    if (range != null && range.size() > 0) {
      mVisibleFirstIdx = mDatapoints.size();
      mVisibleLastIdx  = mVisibleFirstIdx + range.size() - 1;
      mDatapoints.addAll(range);		
    }
    if (post != null && post.size() > 0) {
      mVisiblePostFirstIdx = mDatapoints.size();
      mVisiblePostLastIdx  = mVisiblePostFirstIdx + post.size() - 1;
      mDatapoints.addAll(post);		
    }
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

  public Datapoint lookupVisibleDatapoint(Tuple press) {
    Datapoint d;

    if (isEnabled() == false)
      return null;

    if (mVisibleFirstIdx != Integer.MIN_VALUE && mVisibleLastIdx != Integer.MAX_VALUE) {		
      for (int i = mVisibleFirstIdx; i <= mVisibleLastIdx; i++ ) {
        d = mDatapoints.get(i);

        if (press.x >= d.mValueScreen.x - mTouchRadius &&
            press.x <= d.mValueScreen.x + mTouchRadius && 
            press.y >= d.mValueScreen.y - mTouchRadius && 
            press.y <= d.mValueScreen.y + mTouchRadius) {
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
      if (d.mMillis == timestamp) {
        return d;
      }
      else if (max < min) {
        if (pre == true) {
          if (d.mMillis > timestamp) {
            if (mid - 1 >= 0)
              d = mDatapoints.get(mid - 1);
            else
              d = null;
          }
        } else {
          if (d.mMillis < timestamp) {
            if (mid + 1 < mDatapoints.size())
              d = mDatapoints.get(mid + 1);					
            else
              d = null;
          }
        }
        return d;
      }			
      else if (d.mMillis < timestamp) {
        min = mid + 1;
      }
      else if (d.mMillis > timestamp) {
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
    if (mVisiblePreFirstIdx != Integer.MIN_VALUE && mVisiblePreLastIdx != Integer.MAX_VALUE)
      return mDatapoints.subList(mVisiblePreFirstIdx, mVisiblePreLastIdx + 1);
    return null;
  }

  public List<Datapoint> getVisible() {
    if (mVisibleFirstIdx != Integer.MIN_VALUE && mVisibleLastIdx != Integer.MAX_VALUE)
      return mDatapoints.subList(mVisibleFirstIdx, mVisibleLastIdx + 1);
    return null;
  }

  public List<Datapoint> getVisiblePost() {
    if (mVisiblePostFirstIdx != Integer.MIN_VALUE && mVisiblePostLastIdx != Integer.MAX_VALUE)
      return mDatapoints.subList(mVisiblePostFirstIdx, mVisiblePostLastIdx + 1);
    return null;
  }

  public Datapoint findPreNeighbor(long timestamp) {
    return findNeighbor(timestamp, true);
  }

  public Datapoint findPostNeighbor(long timestamp) {
    return findNeighbor(timestamp, false);
  }

  public Float interpolateScreenCoord(long timestamp) {
    Datapoint d1 = findPreNeighbor(timestamp - 1);
    Datapoint d2 = findPostNeighbor(timestamp);
    if (d1 == null || d2 == null)
      return null;
    return mInterpolator.interpolateY(d1.mValueScreen, d2.mValueScreen, timestamp);
  }

  public Float interpolateValue(long timestamp) {
    Datapoint d2 = findPostNeighbor(timestamp); // inclusive
    if (d2 == null)
      return null;
    
    if (d2.mMillis == timestamp)
      return new Float(d2.mValue.y);
    
    Datapoint d1 = findPreNeighbor(timestamp); // inclusive
    if (d1 == null)
      return null;
      
    return mInterpolator.interpolateY(d1.mValue, d2.mValue, timestamp);
  }

  public void floatOp(Float f, AST.Opcode op, boolean pre) {
    if (f == null || f.isNaN() || f.isInfinite())
      return;

    Datapoint d;
    for (int i = 0; i < mDatapoints.size(); i++) {
      d = mDatapoints.get(i);
      if (pre == false) {
        if (op == AST.Opcode.PLUS)
          d.mValue.y += f;
        else if (op == AST.Opcode.MINUS)
          d.mValue.y -= f;
        else if (op == AST.Opcode.MULTIPLY)
          d.mValue.y *= f;
        else if (op == AST.Opcode.DIVIDE) {
          if (f != 0)
            d.mValue.y /= f;
        }
      } else {
        if (op == AST.Opcode.PLUS)
          d.mValue.y = f + d.mValue.y;
        else if (op == AST.Opcode.MINUS)
          d.mValue.y = f - d.mValue.y;
        else if (op == AST.Opcode.MULTIPLY)
          d.mValue.y = f * d.mValue.y;
        else if (op == AST.Opcode.DIVIDE) {
          if (d.mValue.y != 0)
            d.mValue.y = f / d.mValue.y;
        }
      }
    }
  }

  public void plusPre(Float f) { 
    floatOp(f, AST.Opcode.PLUS, true); 
  }

  public void minusPre(Float f) { 
    floatOp(f, AST.Opcode.MINUS, true); 
  }

  public void multiplyPre(Float f) { 
    floatOp(f, AST.Opcode.MULTIPLY, true); 
  }

  public void dividePre(Float f) { 
    floatOp(f, AST.Opcode.DIVIDE, true); 
  }

  public void plusPost(Float f) { 
    floatOp(f, AST.Opcode.PLUS, false); 
  }

  public void minusPost(Float f) { 
    floatOp(f, AST.Opcode.MINUS, false); 
  }

  public void multiplyPost(Float f) { 
    floatOp(f, AST.Opcode.MULTIPLY, false); 
  }

  public void dividePost(Float f) { 
    floatOp(f, AST.Opcode.DIVIDE, false); 
  }

  public void longOp(Long l, AST.Opcode op, boolean pre) {
    if (l == null)
      return;

    Datapoint d;
    for (int i = 0; i < mDatapoints.size(); i++) {
      d = mDatapoints.get(i);
      if (pre == false) {
        if (op == AST.Opcode.PLUS)
          d.mValue.y += l;
        else if (op == AST.Opcode.MINUS)
          d.mValue.y -= l;
        else if (op == AST.Opcode.MULTIPLY)
          d.mValue.y *= l;
        else if (op == AST.Opcode.DIVIDE) {
          if (l != 0)
            d.mValue.y /= l;
        }
      } else {
        if (op == AST.Opcode.PLUS)
          d.mValue.y = l + d.mValue.y;
        else if (op == AST.Opcode.MINUS)
          d.mValue.y = l - d.mValue.y;
        else if (op == AST.Opcode.MULTIPLY)
          d.mValue.y = l * d.mValue.y;
        else if (op == AST.Opcode.DIVIDE) {
          if (d.mValue.y != 0)
            d.mValue.y = l / d.mValue.y;
        }
      }
    }
  }

  public void plusPre(Long l) { 
    longOp(l, AST.Opcode.PLUS, true); 
  }

  public void minusPre(Long l) { 
    longOp(l, AST.Opcode.MINUS, true); 
  }

  public void multiplyPre(Long l) { 
    longOp(l, AST.Opcode.MULTIPLY, true); 
  }

  public void dividePre(Long l) { 
    longOp(l, AST.Opcode.DIVIDE, true); 
  }

  public void plusPost(Long l) { 
    longOp(l, AST.Opcode.PLUS, false); 
  }

  public void minusPost(Long l) { 
    longOp(l, AST.Opcode.MINUS, false); 
  }

  public void multiplyPost(Long l) { 
    longOp(l, AST.Opcode.MULTIPLY, false); 
  }

  public void dividePost(Long l) { 
    longOp(l, AST.Opcode.DIVIDE, false); 
  }

  public void previousValue() {
    Datapoint d1, d2;
    for (int i = mDatapoints.size() - 1; i > 0; i--) {
      d1 = mDatapoints.get(i-1);
      d2 = mDatapoints.get(i);
      d2.mValue.y = d2.mValue.y - d1.mValue.y;
    }
  }

  public void previousTimestamp() {
    Datapoint d1, d2;
    for (int i = mDatapoints.size() - 1; i > 0; i--) {
      d1 = mDatapoints.get(i-1);
      d2 = mDatapoints.get(i);
      d2.mValue.y = d2.mMillis - d1.mMillis;
    }
    if (mDatapoints.size() > 0) {
      d1 = mDatapoints.get(0);
      d1.mValue.y = 0;
    }
  }

  public void inPeriod(DateUtil.Period p) {
    Datapoint d;
    long ms = DateUtil.mapPeriodToLong(p);
    for (int i = 0; i < mDatapoints.size(); i++) {
      d = mDatapoints.get(i);
      if (ms != 0)
        d.mValue.y /= ms;
    }
  }

  public void asPeriod(DateUtil.Period p) {
    Datapoint d;
    long ms = DateUtil.mapPeriodToLong(p);
    for (int i = 0; i < mDatapoints.size(); i++) {
      d = mDatapoints.get(i);
      d.mValue.y *= ms;
    }
  }

  // We need to gather a list of all timestamps so we can interpolate from
  // ones series to the other, and vice versa, in order to make the operations
  // commutative
  public void timeseriesOp(TimeSeries ts, AST.Opcode op) {
    Float f1, f2;
    Datapoint d1, d2;
    ArrayList<Datapoint> pre   = new ArrayList<Datapoint>();
    ArrayList<Datapoint> range = new ArrayList<Datapoint>();
    ArrayList<Datapoint> post  = new ArrayList<Datapoint>();
    TreeMap<Long, Boolean> timestamps = new TreeMap<Long, Boolean>();

    for (int i = 0; i < mDatapoints.size(); i++) {
      timestamps.put(new Long(mDatapoints.get(i).mMillis), true);
    }    
    for (int i = 0; i < ts.mDatapoints.size(); i++) {
      timestamps.put(new Long(ts.mDatapoints.get(i).mMillis), true);
    }    
    
    Iterator<Long> iterator = timestamps.keySet().iterator();
    while (iterator.hasNext()) {
      Long ms = iterator.next();

      f1 = interpolateValue(ms);
      f2 = ts.interpolateValue(ms);
      
      // We handle invalid interpolations slightly differing depending on
      // opcode.  For example, for + and -, it could be justified that adding
      // or subtracting to/from a datapoint that doesn't exist means that the
      // missing value should be 0 (this assumption can certainly be challenged,
      // but for most of the use cases for the application, I believe this is 
      // correct.)  However, for * and /, should we attempt to return the 
      // identity, or 0? It's unclear, so we bail on the calculation.
      if (op == AST.Opcode.PLUS || op == AST.Opcode.MINUS) {
        if (f1 == null)        
          f1 = new Float(0.0f);
        if (f2 == null)
          f2 = new Float(0.0f);
      }
      if (f1 == null || f1.isNaN() || f1.isInfinite())        
        continue;
      if (f2 == null || f2.isNaN() || f2.isInfinite())
        continue;      
      
      d1 = new Datapoint(ms, f1, getDbRow().getId(), -1, 1);

      if (op == AST.Opcode.PLUS)
        d1.mValue.y += f2;             
      else if (op == AST.Opcode.MINUS)
        d1.mValue.y -= f2;             
      else if (op == AST.Opcode.MULTIPLY)
        d1.mValue.y *= f2;             
      else if (op == AST.Opcode.DIVIDE) {
        if (f2 == 0)
          continue;
        else
          d1.mValue.y /= f2;               
      }

      if (ms < getVisibleTimestampMin() && ms < ts.getVisibleTimestampMin()) {
        pre.add(d1);
      }
      else if (ms > getVisibleTimestampMax() && ms > ts.getVisibleTimestampMax()) {
        post.add(d1);
      }
      else {
        range.add(d1);
      }
    }       

    setDatapoints(pre, range, post);
  }

  public void timeseriesPlus(TimeSeries ts) { 
    timeseriesOp(ts, AST.Opcode.PLUS); 
  }

  public void timeseriesMinus(TimeSeries ts) { 
    timeseriesOp(ts, AST.Opcode.MINUS); 
  }

  public void timeseriesMultiply(TimeSeries ts) { 
    timeseriesOp(ts, AST.Opcode.MULTIPLY); 
  }

  public void timeseriesDivide(TimeSeries ts) { 
    timeseriesOp(ts, AST.Opcode.DIVIDE); 
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

  public void drawGoal(Canvas canvas, Tuple start, Tuple end) {
    mPainter.drawGoal(canvas, start, end);
  }

  public void drawMarker(Canvas canvas, Tuple start, Tuple end) {
    mPainter.drawMarker(canvas, start, end);
  }

  private void interpolateBoundsToOffscreen() {
    int nDatapoints;
    Datapoint d1 = null;
    Datapoint d2 = null;
    float iy = 0.0f;

    List<Datapoint> preVisible  = getVisiblePre();
    List<Datapoint> visible     = getVisible();
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
        // no datapoints in visible range, grab last from before the visible range
        d1 = preVisible.get(preVisible.size() - 1);
      } else {
        // no datapoints to connect it to
        return;
      }

      if (d2.mMillis > d1.mMillis) {
        iy = (d2.mValue.y - d1.mValue.y) / (d2.mMillis - d1.mMillis);
        if (iy > 0 && d2.mValue.y > mVisibleMaxY) {
          mVisibleMaxY = d2.mValue.y;
        }
        if (iy < 0 && d2.mValue.y < mVisibleMinY) {
          mVisibleMinY = d2.mValue.y;
        }
      }
    }
    if (preVisible != null) {
      // we have a datapoint before the beginning to interpolate
      d1 = preVisible.get(preVisible.size()-1);
      if (visible != null) {
        // also datapoints in visible range
        d2 = visible.get(0);
      } else if (postVisible != null) {
        // no datapoints in visible range, grab first from beyond the visible range
        d2 = postVisible.get(0);
      } else {
        // no datapoints to connect it to
        return;
      }
      
      if (d1.mMillis < d2.mMillis) {
        iy = (d2.mValue.y - d1.mValue.y) / (d2.mMillis - d1.mMillis);
        if (iy < 0 && d1.mValue.y > mVisibleMaxY) {
          mVisibleMaxY = d1.mValue.y;
        }
        if (iy > 0 && d1.mValue.y < mVisibleMinY) {
          mVisibleMinY = d1.mValue.y;
        }
      }
    }
  }

  private void copyDatapoints(TimeSeries source) {
    mDatapoints.clear();
    if (source.mDatapoints == null)
      return;

    for (int i = 0; i < source.mDatapoints.size(); i++) {
      Datapoint d = new Datapoint(source.mDatapoints.get(i));
      mDatapoints.add(d);
    }

    return;
  }

  private void setColor(String color) {
    mColorStr = color;
    mPainter.setColor(color);
  }  
  
  private void resetIndices() {
    mVisiblePreFirstIdx  = Integer.MIN_VALUE;
    mVisiblePreLastIdx   = Integer.MIN_VALUE;
    mVisibleFirstIdx     = Integer.MIN_VALUE;
    mVisibleLastIdx      = Integer.MIN_VALUE;
    mVisiblePostFirstIdx = Integer.MIN_VALUE;
    mVisiblePostLastIdx  = Integer.MIN_VALUE;
  }  
  
  private void resetMinMax() {
    mVisibleMinY = Float.MAX_VALUE;
    mVisibleMaxY = Float.MIN_VALUE;
    mVisibleMinX = Long.MAX_VALUE;
    mVisibleMaxX = Long.MIN_VALUE;
  }
}