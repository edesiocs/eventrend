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

package net.redgeek.android.eventgrapher.primitives;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.net.Uri.Builder;
import android.text.TextUtils;

import net.redgeek.android.eventgrapher.TimeSeriesPainter;
import net.redgeek.android.eventrecorder.TimeSeriesData;
import net.redgeek.android.eventrecorder.TimeSeriesData.FormulaCache;
import net.redgeek.android.eventrecorder.interpolators.TimeSeriesInterpolator;
import net.redgeek.android.eventrend.category.CategoryRow;
import net.redgeek.android.eventrend.util.DateUtil;
import net.redgeek.android.eventrend.util.DateUtil.Period;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TimeSeriesCollector {
  private ArrayList<TimeSeries> mSeries;

  private long mAggregationMs;
  private int mHistory = 20;
  private float mSmoothing = 0.1f;
  private float mSensitivity = 1.0f;
  private Lock mLock;
  private boolean mAutoAggregation;
  private int mAutoAggregationOffset;

  private DatapointCache mDatapointCache;
  private FormulaCache mFormulaCache;
  private ArrayList<TimeSeriesInterpolator> mInterpolators;

  private ContentProvider mProvider;
  private DateUtil mAutoAggSpan;
  private Calendar mCal1;
  private Calendar mCal2;

  private long mCollectionStart;
  private long mCollectionEnd;
  private long mQueryStart;
  private long mQueryEnd;

  private TimeSeriesPainter mDefaultPainter;

  public TimeSeriesCollector(ContentProvider provider) {
    initialize(provider, null);
  }

  public TimeSeriesCollector(ContentProvider provider, TimeSeriesPainter painter) {
    initialize(provider, painter);
  }

  public void initialize(ContentProvider provider, TimeSeriesPainter painter) {
    mProvider = provider;
    mSeries = new ArrayList<TimeSeries>();

    mAutoAggSpan = new DateUtil();
    mDatapointCache = new DatapointCache(mProvider);
    mAutoAggregation = false;
    mAutoAggregationOffset = 0;
    mCal1 = Calendar.getInstance();
    mCal2 = Calendar.getInstance();

    mDefaultPainter = painter;

    mLock = new ReentrantLock();
  }

  @Override
  public String toString() {
    return mSeries.toString();
  }

  public void updateTimeSeriesMetaLocking(boolean disableByDefault) {
    waitForLock();
    
    Uri uri = TimeSeriesData.TimeSeries.CONTENT_URI;
    Cursor c = mProvider.query(uri, null, null, null, null);
    int count = c.getCount();
    if (count < 1) {
      c.close();
      unlock();
      return;
    }

    c.moveToFirst();
    for (int i = 0; i < c.getCount(); i++) {
      CategoryRow row = new CategoryRow(c);
      updateTimeSeriesMeta(row, disableByDefault);
      c.moveToNext();
    }
    c.close();

    unlock();
  }

  private void updateTimeSeriesMeta(CategoryRow row, boolean disable) {
    TimeSeries ts = getSeriesByIdNonlocking(row.mId);

    if (ts == null) {
      if (mDefaultPainter == null) {
        TimeSeriesPainter p = new TimeSeriesPainter.Default();
        ts = new TimeSeries(row, mHistory, mSmoothing, p);
      } else {
        ts = new TimeSeries(row, mHistory, mSmoothing, mDefaultPainter);
      }
      mSeries.add(ts);
      mDatapointCache.addCacheableCategory(row.mId, mHistory);
    }

    ts.mRow = row;

    if (disable)
      ts.setEnabled(false);

  }

  public void updateTimeSeriesData(String aggregation, boolean flushCache) {
    updateTimeSeriesData(mQueryStart, mQueryEnd, aggregation, flushCache);
  }

  public void updateTimeSeriesData(long start, long end, String aggregation, boolean flushCache) {
    waitForLock();
    for (int i = 0; i < mSeries.size(); i++) {
      TimeSeries ts = mSeries.get(i);
      if (ts != null && ts.isEnabled() == true) {
        long catId = ts.mRow.mId;
        updateTimeSeriesData(catId, start, end, aggregation, flushCache);
      }
    }
    unlock();
  }

  public void updateTimeSeriesData(long catId, String aggregation, boolean flushCache) {
    waitForLock();
    updateTimeSeriesData(catId, mQueryStart, mQueryEnd, aggregation, flushCache);
    unlock();
  }

  private void updateTimeSeriesData(long catId, long start, long end,
      String aggregation, boolean flushCache) {
    if (flushCache == true)
      mDatapointCache.refresh(catId, aggregation);

    gatherSeries(start, end, aggregation);
  }

  public void setSmoothing(float smoothing) {
    mSmoothing = smoothing;
    waitForLock();
    for (int i = 0; i < mSeries.size(); i++) {
      mSeries.get(i).recalcStatsAndBounds(mSmoothing, mHistory);
    }
    unlock();
  }

  public void setHistory(int history) {
    mHistory = history;
    waitForLock();
    for (int i = 0; i < mSeries.size(); i++) {
      mSeries.get(i).recalcStatsAndBounds(mSmoothing, mHistory);
    }
    unlock();
  }

  public void setSensitivity(float sensitivity) {
    mSensitivity = sensitivity;
  }

  public void setInterpolators(ArrayList<TimeSeriesInterpolator> list) {
    mInterpolators = list;
  }

  private void waitForLock() {
    while (lock() == false) {
    }
  }

  public boolean lock() {
    try {
      return mLock.tryLock(1000L, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      return false;
    }
  }

  public void unlock() {
    mLock.unlock();
  }

  public void setAutoAggregation(boolean b) {
    mAutoAggregation = b;
  }

  public void setAutoAggregationOffset(int offset) {
    mAutoAggregationOffset = offset;
  }

  public boolean getAutoAggregation() {
    return mAutoAggregation;
  }

  public void setAggregationMs(long millis) {
    mAggregationMs = millis;
  }

  public void setSeriesInterpolator(TimeSeries ts, String type) {
    TimeSeriesInterpolator tsi = null;
    if (mInterpolators == null)
      return;
    
    for (int i = 0; i < mInterpolators.size(); i++) {
      tsi = mInterpolators.get(i);
      if (type.equals(tsi.getName()))
        break;
    }

    if (tsi != null) {
      waitForLock();
      ts.setInterpolator(tsi);
      unlock();
    }

    return;
  }

  public void clearSeriesLocking() {
    TimeSeries ts;

    waitForLock();
    for (int i = 0; i < mSeries.size(); i++) {
      ts = mSeries.get(i);
      if (ts != null) {
        ts.clearSeries();
      }
    }
    mSeries.clear();
    unlock();
  }

  public boolean isSeriesEnabled(long catId) {
    boolean b;
    waitForLock();
    TimeSeries ts = getSeriesByIdNonlocking(catId);
    if (ts == null)
      b = false;
    else
      b = ts.isEnabled();
    unlock();
    return b;
  }

  public void setSeriesEnabled(long catId, boolean b) {
    waitForLock();    
    TimeSeries ts = getSeriesByIdNonlocking(catId);
    if (ts != null)
      ts.setEnabled(b);
    unlock();
    return;
  }

  public void toggleSeriesEnabled(long catId) {
    waitForLock();
    TimeSeries ts = getSeriesByIdNonlocking(catId);
    
    if (ts.isEnabled())
      ts.setEnabled(false);
    else
      ts.setEnabled(true);
    unlock();
    return;
  }

  public int numSeries() {
    int i;
    waitForLock();
    i = mSeries.size();
    unlock();
    return i;
  }

  public TimeSeries getSeries(int i) {
    TimeSeries ts = null;
    try {
      ts = mSeries.get(i);
    } catch(IndexOutOfBoundsException e) {
      ts = null;
    }
    return ts;
  }

  public CategoryRow getSeriesMetaLocking(int i) {
    CategoryRow row = null;
    waitForLock();
    TimeSeries ts = getSeries(i);
    row = new CategoryRow(ts.mRow);
    unlock();
    return row;
  }

  public long getSeriesIdLocking(int i) {
    long id = -1;
    waitForLock();
    TimeSeries ts = getSeries(i);
    if (ts != null)
      id = ts.mRow.mId;
    unlock();
    return id;
  }

  private TimeSeries getSeriesByIdNonlocking(long catId) {
    for (int i = 0; i < mSeries.size(); i++) {
      TimeSeries ts = mSeries.get(i);
      if (ts != null && ts.mRow.mId == catId)
        return ts;
    }
    return null;
  }

  public TimeSeries getSeriesByIdLocking(long catId) {
    waitForLock();
    TimeSeries ts = getSeriesByIdNonlocking(catId);
    unlock();
    return ts;
  }

  public TimeSeries getSeriesByNameNonlocking(String name) {
    for (int i = 0; i < mSeries.size(); i++) {
      TimeSeries ts = mSeries.get(i);
      if (ts != null && ts.mRow.mTimeSeriesName.equals(name))
        return ts;
    }
    return null;
  }

  public TimeSeries getSeriesByNameLocking(String name) {
    waitForLock();
    TimeSeries ts = getSeriesByNameNonlocking(name);
    unlock();
    return ts;
  }

  public ArrayList<TimeSeries> getAllSeries() {
    return mSeries;
  }

  public ArrayList<TimeSeries> getAllEnabledSeries() {
    ArrayList<TimeSeries> list = new ArrayList<TimeSeries>();
    for (int i = 0; i < mSeries.size(); i++) {
      TimeSeries ts = mSeries.get(i);
      if (ts != null && ts.isEnabled())
        list.add(ts);
    }
    return list;
  }

  public Datapoint getVisibleFirstDatapointLocking() {
    Datapoint first = null;
    waitForLock();
    for (int i = 0; i < mSeries.size(); i++) {
      TimeSeries ts = mSeries.get(i);
      if (ts != null && ts.isEnabled() == true) {
        Datapoint d = ts.getFirstVisible();
        if (first == null)
          first = d;
        else if (d.mTsStart < first.mTsStart)
          first = d;
      }
    }
    unlock();
    return first;
  }

  public Datapoint getVisibleLastDatapointLocking() {
    Datapoint last = null;
    waitForLock();
    for (int i = 0; i < mSeries.size(); i++) {
      TimeSeries ts = mSeries.get(i);
      if (ts != null && ts.isEnabled() == true) {
        Datapoint d = ts.getLastVisible();
        if (last == null)
          last = d;
        else if (d.mTsStart > last.mTsStart)
          last = d;
      }
    }
    unlock();
    return last;
  }

  public void clearCache() {
    mDatapointCache.clearCache();
    clearSeriesLocking();
  }

  public synchronized void gatherLatestDatapointsLocking(long catId, int history,
      String aggregation) {
    waitForLock();
    mDatapointCache.populateLatest(catId, history, aggregation);
    TimeSeries ts = getSeriesByIdNonlocking(catId);
    if (ts == null) {
      unlock();
      return;
    }

    ts.clearSeries();

    Builder builder = ContentUris.withAppendedId(
        TimeSeriesData.TimeSeries.CONTENT_URI, catId).buildUpon()
        .appendPath("recent").appendPath("1");
    if (TextUtils.isEmpty(aggregation))
      builder.appendPath(aggregation);
    
    Uri uri = builder.build();
    Cursor c = mProvider.query(uri, null, null, null, null);
    int count = c.getCount();
    if (count < 1) {
      c.close();
      unlock();
      return;
    }
    c.moveToFirst();
    double lastValue = TimeSeriesData.Datapoint.getValue(c);
    long lastTsStart = TimeSeriesData.Datapoint.getTsStart(c);
    c.close();

    ArrayList<Datapoint> l = mDatapointCache.getLast(catId, history);
    if (l == null || l.size() < 1 || lastTsStart > l.get(0).mTsStart
        || lastValue != l.get(0).mValue) {
      mDatapointCache.clearCache(catId);
      mDatapointCache.populateLatest(catId, history, aggregation);
      l = mDatapointCache.getLast(catId, history);
    }

    ts.setDatapoints(null, l, null, true);

    unlock();
  }

  public synchronized void gatherSeriesLocking(long milliStart, long milliEnd,
      String aggregation) {
    waitForLock();
    gatherSeries(milliStart, milliEnd, aggregation);
    unlock();
  }
  
  public synchronized void gatherSeries(long milliStart, long milliEnd, String
      aggregation) {
    ArrayList<Datapoint> pre, range, post;
    boolean has_data;
    long oldAggregationMs = mAggregationMs;

    mQueryStart = milliStart;
    mQueryEnd = milliEnd;

    setCollectionTimes(milliStart, milliEnd);
    for (int i = 0; i < mSeries.size(); i++) {
      has_data = false;

      TimeSeries ts = mSeries.get(i);
      if (ts.isEnabled() == false)
        continue;

      mDatapointCache.populateRange(ts.mRow.mId, mCollectionStart,
          mCollectionEnd, mAggregationMs, aggregation);

      pre = mDatapointCache.getDataBefore(ts.mRow.mId, mHistory,
          mCollectionStart);
      if (pre != null && pre.size() > 0)
        has_data = true;

      range = mDatapointCache.getDataInRange(ts.mRow.mId, mCollectionStart, mCollectionEnd);
      if (range != null && range.size() > 0)
        has_data = true;

      post = mDatapointCache.getDataAfter(ts.mRow.mId, 1, mCollectionEnd);
      if (post != null && range.size() > 0)
        has_data = true;

      if (has_data == true)
        ts.setDatapoints(pre, range, post, true);
    }

    mAggregationMs = oldAggregationMs;

    return;
  }

  public Datapoint getLastDatapoint(long catId) {
    ArrayList<Datapoint> list = mDatapointCache.getLast(catId, 1);
    if (list == null || list.size() < 1)
      return null;
    return list.get(0);
  }

  private void setCollectionTimes(long milliStart, long milliEnd) {
    if (mAutoAggregation == true) {
      mAutoAggSpan.setSpanOffset(mAutoAggregationOffset);
      mAutoAggSpan.setSpan(milliStart, milliEnd);
      mAggregationMs = DateUtil.mapPeriodToLong(mAutoAggSpan.getSpan());
    }

    // this adjustment is to make sure that the edges of the visible range
    // doesn't
    // span periods (for the purposes of calculations, not display --
    // anything
    // that's
    // in the visible range array, but not actually on-screen due to
    // aggregation
    // back
    // to the first datapoint in the period, will just be drawn off screen.
    // Note
    // that
    // this will also cause the scaling to take into account these
    // off-screen
    // points,
    // but that's okay, and perhaps desireable, since it's more likely to
    // have
    // no
    // datapoints visible within the range, having only aggregated points on
    // either
    // side, when aggregating.
    if (mAggregationMs != 0) {
      Period p = DateUtil.mapLongToPeriod(mAggregationMs);

      mCal1.setTimeInMillis(milliStart);
      DateUtil.setToPeriodStart(mCal1, p);
      milliStart = mCal1.getTimeInMillis();

      mCal2.setTimeInMillis(milliEnd);
      DateUtil.setToPeriodStart(mCal2, p);

      int step = 1;
      if (p == Period.QUARTER)
        step = 3;
      mCal2.add(DateUtil.mapLongToCal(mAggregationMs), step);

      milliEnd = mCal2.getTimeInMillis();
    }

    mCollectionStart = milliStart;
    mCollectionEnd = milliEnd;

    return;
  }
}
