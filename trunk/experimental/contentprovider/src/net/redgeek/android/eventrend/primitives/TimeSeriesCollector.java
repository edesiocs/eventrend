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

package net.redgeek.android.eventrend.primitives;

import android.database.Cursor;
import android.util.Log;

import net.redgeek.android.eventrecorder.synthetic.Formula;
import net.redgeek.android.eventrecorder.synthetic.FormulaCache;
import net.redgeek.android.eventrend.db.EvenTrendDbAdapter;
import net.redgeek.android.eventrend.graph.TimeSeriesPainter;
import net.redgeek.android.eventrend.graph.plugins.TimeSeriesInterpolator;
import net.redgeek.android.eventrend.util.DateUtil;
import net.redgeek.android.eventrend.util.Number;
import net.redgeek.android.eventrend.util.DateUtil.Period;
import net.redgeek.android.eventrend.util.Number.TrendState;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
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

  private EvenTrendDbAdapter mDbh;
  private DateUtil mAutoAggSpan;
  private Calendar mCal1;
  private Calendar mCal2;

  private long mCollectionStart;
  private long mCollectionEnd;
  private long mQueryStart;
  private long mQueryEnd;

  private TimeSeriesPainter mDefaultPainter;

  public TimeSeriesCollector(EvenTrendDbAdapter dbh) {
    initialize(dbh, null);
  }

  public TimeSeriesCollector(EvenTrendDbAdapter dbh, TimeSeriesPainter painter) {
    initialize(dbh, painter);
  }

  public void initialize(EvenTrendDbAdapter dbh, TimeSeriesPainter painter) {
    mDbh = dbh;
    mSeries = new ArrayList<TimeSeries>();

    mAutoAggSpan = new DateUtil();
    mDatapointCache = new DatapointCache(mDbh);
    mFormulaCache = new FormulaCache();
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

  public EvenTrendDbAdapter getDbh() {
    return mDbh;
  }

  public void updateTimeSeriesMetaLocking(boolean disableByDefault) {
    waitForLock();
    Cursor c = mDbh.fetchAllCategories();
    c.moveToFirst();
    for (int i = 0; i < c.getCount(); i++) {
      CategoryDbTable.Row row = new CategoryDbTable.Row(c);
      updateTimeSeriesMeta(row, disableByDefault);
      c.moveToNext();
    }
    c.close();

    // cycle through again, we may have had a series that was dependent on
    // another series that was created later
    for (int i = 0; i < mSeries.size(); i++) {
      setDependents(mSeries.get(i));
      setDependees(mSeries.get(i));
    }

    unlock();
  }

  private void updateTimeSeriesMeta(CategoryDbTable.Row row, boolean disable) {
    TimeSeries ts = getSeriesByIdNonlocking(row.getId());

    if (ts == null) {
      if (mDefaultPainter == null) {
        TimeSeriesPainter p = new TimeSeriesPainter.Default();
        ts = new TimeSeries(row, mHistory, mSmoothing, p);
      } else {
        ts = new TimeSeries(row, mHistory, mSmoothing, mDefaultPainter);
      }
      mSeries.add(ts);
      mDatapointCache.addCacheableCategory(row.getId(), mHistory);
    }

    ts.setDbRow(row);
    setSeriesInterpolator(ts, row.getInterpolation());

    if (row.getSynthetic() == true) {
      Formula formula = mFormulaCache.getFormula(Long.valueOf(row.getId()));
      if (formula == null)
        formula = new Formula();
      formula.setFormula(row.getFormula());
      mFormulaCache.setFormula(row.getId(), formula);
    }

    if (disable)
      ts.setEnabled(false);

    setDependents(ts);
    setDependees(ts);
  }

  public void updateTimeSeriesData(boolean flushCache) {
    updateTimeSeriesData(mQueryStart, mQueryEnd, flushCache);
  }

  public void updateTimeSeriesData(long start, long end, boolean flushCache) {
    waitForLock();
    for (int i = 0; i < mSeries.size(); i++) {
      TimeSeries ts = mSeries.get(i);
      if (ts != null && ts.isEnabled() == true) {
        long catId = ts.getDbRow().getId();
        updateTimeSeriesData(catId, start, end, flushCache);
      }
    }
    unlock();
  }

  public void updateTimeSeriesData(long catId, boolean flushCache) {
    waitForLock();
    updateTimeSeriesData(catId, mQueryStart, mQueryEnd, flushCache);
    unlock();
  }

  private void updateTimeSeriesData(long catId, long start, long end,
      boolean flushCache) {
    if (flushCache == true)
      mDatapointCache.refresh(catId);

    gatherSeries(start, end);
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

  public CategoryDbTable.Row getSeriesMetaLocking(int i) {
    CategoryDbTable.Row row = null;
    waitForLock();
    TimeSeries ts = getSeries(i);
    row = new CategoryDbTable.Row(ts.getDbRow());
    unlock();
    return row;
  }

  public long getSeriesIdLocking(int i) {
    long id = -1;
    waitForLock();
    TimeSeries ts = getSeries(i);
    if (ts != null)
      id = ts.getDbRow().getId();
    unlock();
    return id;
  }

  private TimeSeries getSeriesByIdNonlocking(long catId) {
    for (int i = 0; i < mSeries.size(); i++) {
      TimeSeries ts = mSeries.get(i);
      if (ts != null && ts.getDbRow().getId() == catId)
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
      if (ts != null && ts.getDbRow().getCategoryName().equals(name))
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
        else if (d.mMillis < first.mMillis)
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
        else if (d.mMillis > last.mMillis)
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

  public synchronized void gatherLatestDatapointsLocking(long catId, int history) {
    waitForLock();
    mDatapointCache.populateLatest(catId, history);
    TimeSeries ts = getSeriesByIdNonlocking(catId);
    if (ts == null) {
      unlock();
      return;
    }

    if (ts.getDbRow().getSynthetic() == false) {
      ts.clearSeries();

      EntryDbTable.Row entry = mDbh.fetchLastCategoryEntry(catId);
      if (entry != null) {
        ArrayList<Datapoint> l = mDatapointCache.getLast(catId, history);
        if (l == null || l.size() < 1
            || entry.getTimestamp() > l.get(0).mMillis
            || entry.getValue() != l.get(0).mValue.y) {
          mDatapointCache.clearCache(catId);
          mDatapointCache.populateLatest(catId, history);
          l = mDatapointCache.getLast(catId, history);
        }

        l = aggregateDatapoints(l, ts.getDbRow().getType());
        ts.setDatapoints(null, l, null, true);
      }
    }

    unlock();
  }

  public synchronized void gatherSeriesLocking(long milliStart, long milliEnd) {
    waitForLock();
    gatherSeries(milliStart, milliEnd);
    unlock();
  }
  
  public synchronized void gatherSeries(long milliStart, long milliEnd) {
    ArrayList<Datapoint> pre, range, post;
    boolean has_data;
    long oldAggregationMs = mAggregationMs;

    mQueryStart = milliStart;
    mQueryEnd = milliEnd;

    setCollectionTimes(milliStart, milliEnd);
    for (int i = 0; i < mSeries.size(); i++) {
      has_data = false;

      TimeSeries ts = mSeries.get(i);
      if (ts == null || ts.getDbRow().getSynthetic())
        continue;

      if (ts.isEnabled() == false) {
        boolean skip = true;
        for (int j = 0; j < ts.getDependees().size(); j++) {
          if (ts.getDependees().get(j).isEnabled() == true) {
            skip = false;
            break;
          }
        }
        if (skip == true)
          continue;
      }

      mDatapointCache.populateRange(ts.getDbRow().getId(), mCollectionStart,
          mCollectionEnd, mAggregationMs);

      pre = mDatapointCache.getDataBefore(ts.getDbRow().getId(), mHistory,
          mCollectionStart);
      if (pre != null && pre.size() > 0)
        has_data = true;

      range = mDatapointCache.getDataInRange(ts.getDbRow().getId(),
          mCollectionStart, mCollectionEnd);
      if (range != null && range.size() > 0)
        has_data = true;

      post = mDatapointCache.getDataAfter(ts.getDbRow().getId(), 1,
          mCollectionEnd);
      if (post != null && range.size() > 0)
        has_data = true;

      if (has_data == true)
        ts.setDatapoints(pre, range, post, true);
    }

    generateSynthetics();
    
    ArrayList<TimeSeries> enabledSeries = getAllEnabledSeries();
    for (int i = 0; i < enabledSeries.size(); i++) {
      aggregateDatapoints(enabledSeries.get(i));
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

  public synchronized void updateCategoryTrend(long catId) {
    String trendStr = "trend_unknown";
    float stdDev = 0.0f;
    float lastTrend = 0.0f;
    float newTrend = 0.0f;

    gatherLatestDatapointsLocking(catId, mHistory);
    TimeSeries ts = getSeriesByIdLocking(catId);
    if (ts == null)
      return;

    if (ts.getDbRow().getSynthetic() == true)
      return;

    lastTrend = ts.getTrendStats().mTrendPrev;
    newTrend = ts.getTrendStats().mTrend;
    stdDev = ts.getValueStats().mStdDev;

    TrendState state = Number.getTrendState(lastTrend, newTrend, ts.getDbRow()
        .getGoal(), mSensitivity, stdDev);
    trendStr = Number.mapTrendStateToString(state);

    mDbh.updateCategoryTrend(catId, trendStr, newTrend);

    if (ts.getDependees() != null && ts.getDependees().size() > 0) {
      for (int i = 0; i < ts.getDependees().size(); i++) {
        TimeSeries dependee = ts.getDependees().get(i);

        for (int j = 0; j < dependee.getDependents().size(); j++) {
          TimeSeries tmp = dependee.getDependents().get(j);
          if (tmp != null)
            gatherLatestDatapointsLocking(tmp.getDbRow().getId(), mHistory);
        }

        Formula formula = mFormulaCache.getFormula(dependee.getDbRow().getId());
        ArrayList<Datapoint> calculated = formula.apply(dependee
            .getDependents());
        dependee.setDatapoints(null, calculated, null, true);

        lastTrend = dependee.getTrendStats().mTrendPrev;
        newTrend = dependee.getTrendStats().mTrend;
        stdDev = dependee.getValueStats().mStdDev;

        state = Number.getTrendState(lastTrend, newTrend, dependee.getDbRow()
            .getGoal(), mSensitivity, stdDev);
        trendStr = Number.mapTrendStateToString(state);

        mDbh.updateCategoryTrend(dependee.getDbRow().getId(), trendStr,
            newTrend);
      }
    }
  }

  private void generateSynthetics() {
    for (int i = 0; i < mSeries.size(); i++) {
      TimeSeries synth = mSeries.get(i);
      if (synth == null || synth.getDbRow().getSynthetic() == false
          || synth.isEnabled() == false)
        continue;

      generateSynthetic(synth);
    }
  }

  private void generateSynthetic(TimeSeries synth) {
    Formula formula = mFormulaCache.getFormula(synth.getDbRow().getId());

    long ms;
    long firstVisibleMs = Long.MAX_VALUE;
    long lastVisibleMs = Long.MIN_VALUE;

    for (int j = 0; j < synth.getDependents().size(); j++) {
      TimeSeries ts = synth.getDependents().get(j);
      List<Datapoint> range = ts.getVisible();

      if (range != null) {
        ms = range.get(0).mMillis;
        if (ms < firstVisibleMs)
          firstVisibleMs = ms;
        ms = range.get(range.size() - 1).mMillis;
        if (ms > lastVisibleMs)
          lastVisibleMs = ms;
      }
    }

    ArrayList<Datapoint> calculated = formula.apply(synth.getDependents());
    ArrayList<Datapoint> pre = new ArrayList<Datapoint>();
    ArrayList<Datapoint> visible = new ArrayList<Datapoint>();
    ArrayList<Datapoint> post = new ArrayList<Datapoint>();

    for (int j = 0; j < calculated.size(); j++) {
      Datapoint d = calculated.get(j);
      d.mCatId = synth.getDbRow().getId();
      d.mSynthetic = true;
      if (d.mMillis < firstVisibleMs)
        pre.add(d);
      else if (d.mMillis <= lastVisibleMs)
        visible.add(d);
      else
        post.add(d);
    }

    pre = aggregateDatapoints(pre, synth.getDbRow().getType());
    visible = aggregateDatapoints(visible, synth.getDbRow().getType());
    post = aggregateDatapoints(post, synth.getDbRow().getType());

    synth.setDatapoints(pre, visible, post, true);
  }

  private void aggregateDatapoints(TimeSeries ts) {
    ArrayList<Datapoint> pre;
    ArrayList<Datapoint> range;
    ArrayList<Datapoint> post;

    pre = aggregateDatapoints(ts.getVisiblePre(), ts.getDbRow().getType());
    range = aggregateDatapoints(ts.getVisible(), ts.getDbRow().getType());
    post = aggregateDatapoints(ts.getVisiblePost(), ts.getDbRow().getType());
    ts.setDatapoints(pre, range, post, true);
    
    return;
  }

  private ArrayList<Datapoint> aggregateDatapoints(List<Datapoint> list,
      String type) {
    Datapoint accumulator = null;
    Datapoint d = null;

    ArrayList<Datapoint> newList = new ArrayList<Datapoint>();

    if (list == null)
      return newList;
    
    if (mAggregationMs == 0) {
      newList.addAll(list);
      return newList;
    }

    for (int i = 0; i < list.size(); i++) {
      d = list.get(i);

      if (i == 0) {
        accumulator = new Datapoint(d);
        continue;
      }

      if (inSameAggregationPeriod(accumulator, d) == false) {
        newList.add(accumulator);
        accumulator = new Datapoint(d);
        accumulator.mNEntries = 1;
      } else {
        if (type.equals(CategoryDbTable.KEY_TYPE_SUM)) {
          accumulator.mValue.y += d.mValue.y;
          accumulator.mNEntries++;
        } else if (type.equals(CategoryDbTable.KEY_TYPE_AVERAGE)) {
          if (accumulator.mNEntries + d.mNEntries != 0) {
            accumulator.mNEntries++;
            float oldMean = accumulator.mValue.y;
            accumulator.mValue.y += ((d.mValue.y - oldMean) / accumulator.mNEntries);
          }
        }
      }
    }

    if (accumulator != null)
      newList.add(accumulator);

    return newList;
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

  private boolean inSameAggregationPeriod(Datapoint d1, Datapoint d2) {
    if (d1 == null || d2 == null)
      return false;

    mCal1.setTimeInMillis(d1.mMillis);
    mCal2.setTimeInMillis(d2.mMillis);
    return DateUtil.inSamePeriod(mCal1, mCal2, mAggregationMs);
  }

  private void setDependents(TimeSeries synth) {
    if (synth.getDbRow().getSynthetic() == false)
      return;

    Formula formula = mFormulaCache.getFormula(synth.getDbRow().getId());
    ArrayList<String> names = formula.getDependentNames();
    if (names == null)
      return;

    synth.getDependents().clear();
    for (int i = 0; i < mSeries.size(); i++) {
      TimeSeries ts = mSeries.get(i);
      if (ts == null || ts == synth)
        continue;

      if (names.contains(ts.getDbRow().getCategoryName()))
        synth.addDependent(ts);
    }

    return;
  }

  private void setDependees(TimeSeries ts) {
    ts.getDependees().clear();
    for (int i = 0; i < mSeries.size(); i++) {
      TimeSeries dependee = getSeries(i);
      if (ts == null || dependee == null || ts == dependee)
        continue;

      if (dependee.getDbRow().getSynthetic() == true) {
        Formula formula = mFormulaCache.getFormula(dependee.getDbRow().getId());
        ArrayList<String> names = formula.getDependentNames();

        if (names != null && names.contains(ts.getDbRow().getCategoryName()))
          ts.addDependee(dependee);
      }
    }

    return;
  }
}
