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

package net.redgeek.android.eventrend.backgroundtasks;

import java.util.Calendar;

import net.redgeek.android.eventrend.EvenTrendActivity;
import net.redgeek.android.eventrend.util.DateUtil;
import net.redgeek.android.eventrend.util.DateUtil.Period;
import net.redgeek.android.timeseries.Datapoint;
import net.redgeek.android.timeseries.EntryDbTable;
import net.redgeek.android.timeseries.TimeSeries;
import net.redgeek.android.timeseries.TimeSeriesCollector;
import android.util.Log;

/**
 * Task to perform one or both of the two actions:
 * <ul>
 * <li>Zero-fill any periods for a category, or all categories, that have no
 * entries since the last entry.
 * <li>Fetch the recent history for a category, or all categories, calculate the
 * new trend, and store the results.
 * </ul>
 * 
 * <p>
 * If both actions are to be performed, the actions are performed in the order
 * listed above, so that the zeros can be taken into account when calculating
 * the new trend.
 * 
 * <p>
 * Note that since this is "backgroundtask", no UI operations may be performed.
 * 
 * @author barclay
 * 
 */
public class UpdateRecentDataTask {
  private TimeSeriesCollector mTSC;
  private Calendar mCal;
  private int     mHistory;

  private boolean mZerofill;
  private boolean mUpdateTrend;

  public UpdateRecentDataTask(TimeSeriesCollector tsc, int history) {
    mTSC = tsc;
    mCal = Calendar.getInstance();
    mHistory = history;
    mZerofill = false;
    mUpdateTrend = false;
  }

  public void setZerofill(boolean b) {
    mZerofill = b;
  }

  public void setUpdateTrend(boolean b) {
    mUpdateTrend = b;
  }

  public void fillAllCategories() {
    for (int i = 0; i < mTSC.numSeries(); i++) {
      long id = mTSC.getSeriesIdLocking(i);
      if (id > 0) {
        fillCategory(id, mZerofill, mUpdateTrend);
      }
    }
    return;
  }

  public void fillCategory(long catId) {
    fillCategory(catId, mZerofill, mUpdateTrend);
    return;
  }

  private synchronized void fillCategory(long catId, boolean zerofill, 
      boolean updateTrend) {
    EntryDbTable.Row entry = new EntryDbTable.Row();
    
    if (zerofill == false && updateTrend == true) {
      // quick check to see if we need to update trends only...
      mTSC.updateCategoryTrend(catId);
      return;
    }

    TimeSeries ts = mTSC.getSeriesByIdLocking(catId);
    if (ts == null || ts.getDbRow().getZeroFill() == false) {
      return;
    }

    mTSC.gatherLatestDatapointsLocking(catId, mHistory);
    Datapoint d = mTSC.getLastDatapoint(catId);
    if (d == null) {
      return;
    }

    long periodMs = ts.getDbRow().getPeriodMs();
    long now = System.currentTimeMillis();

    mCal.setTimeInMillis(d.mMillis);
    while (true) {
      if (periodMs == DateUtil.HOUR_MS) {
        DateUtil.setToPeriodStart(mCal, Period.HOUR);        
        mCal.add(Calendar.HOUR, 1);
      } else if (periodMs == DateUtil.AMPM_MS) {
        DateUtil.setToPeriodStart(mCal, Period.AMPM);
        mCal.add(Calendar.HOUR, 12);
      } else if (periodMs == DateUtil.DAY_MS) {
        DateUtil.setToPeriodStart(mCal, Period.DAY);
        mCal.add(Calendar.DAY_OF_MONTH, 1);
      } else if (periodMs == DateUtil.WEEK_MS) {
        DateUtil.setToPeriodStart(mCal, Period.WEEK);
        mCal.add(Calendar.WEEK_OF_YEAR, 1);
      } else if (periodMs == DateUtil.MONTH_MS) {
        DateUtil.setToPeriodStart(mCal, Period.MONTH);
        mCal.add(Calendar.MONTH, 1);
      } else if (periodMs == DateUtil.QUARTER_MS) {
        DateUtil.setToPeriodStart(mCal, Period.QUARTER);
        mCal.add(Calendar.MONTH, 3);
      } else if (periodMs == DateUtil.YEAR_MS) {
        DateUtil.setToPeriodStart(mCal, Period.YEAR);
        mCal.add(Calendar.YEAR, 1);
      }

      long ms = mCal.getTimeInMillis();
      if (ms + periodMs >= now)
        break;
      
      entry.setCategoryId(catId);
      entry.setTimestamp(mCal.getTimeInMillis());
      entry.setValue(0.0f);
      entry.setNEntries(1);
      mTSC.getDbh().createEntry(entry);
    }

    if (updateTrend == true) {
      mTSC.updateCategoryTrend(entry.getCategoryId());
    }

    return;
  }
}