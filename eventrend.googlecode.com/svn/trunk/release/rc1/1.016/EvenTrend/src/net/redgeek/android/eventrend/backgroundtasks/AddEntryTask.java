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

import net.redgeek.android.eventrend.db.CategoryDbTable;
import net.redgeek.android.eventrend.db.EntryDbTable;
import net.redgeek.android.eventrend.primitives.TimeSeriesCollector;
import net.redgeek.android.eventrend.util.DateUtil;
import net.redgeek.android.eventrend.util.Number;
import android.os.Debug;

/**
 * Task to add an entry to the database. This may also trigger an instantiation
 * of UpdateRecentDataTask in order to zero-fill data for any missing periods,
 * although that task will be run sequentially in the same thread as this task,
 * not as a separate background task.
 * 
 * <p>
 * Adding an entry require checking the category to see if any datapoints should
 * be aggregated, and if so, performing the update instead of and addition. The
 * trend is also recalculated for the category.
 * 
 * <p>
 * The previous values are also stored int some public member variables (which
 * should be encapsulated and accessors established, but I haven't gotten around
 * to it yet) that can be referenced by the calling activity in order to "undo"
 * this addition.
 * 
 * <p>
 * Note that since this is "backgroundtask", no UI operations may be performed.
 * 
 * @author barclay
 * 
 */
public class AddEntryTask {
  // Input
  private TimeSeriesCollector mTSC;
  private int mHistory;
  private int mDecimals;

  // Output
  public long mLastAddId;
  public float mLastAddValue;
  public float mLastAddOldValue;
  public long mLastAddTimestamp;
  public boolean mLastAddUpdate;

  public AddEntryTask(TimeSeriesCollector tsc) {
    mTSC = tsc;
  }

  public AddEntryTask(TimeSeriesCollector tsc, int decimals, int history) {
    mTSC = tsc;
    mHistory = history;
    mDecimals = decimals;
  }

  public void AddEntry(CategoryDbTable.Row category, long timestamp, float value) {
    EntryDbTable.Row entry;
    Calendar entryTScal = Calendar.getInstance();
    Calendar lastTScal;
    long periodInMs = category.getPeriodMs();

    // float value = Float.valueOf(mComboBox.getText().toString()).floatValue();
    entryTScal.setTimeInMillis(timestamp);

    if (periodInMs > 0) {
      UpdateRecentDataTask updater = new UpdateRecentDataTask(mTSC, mHistory);
      updater.setZerofill(true);
      updater.setUpdateTrend(false);
      updater.fillCategory(category.getId());
    }

    entry = mTSC.getDbh().fetchCategoryEntryInPeriod(category.getId(),
        category.getPeriodMs(), timestamp);
    if (entry == null || periodInMs == 0) {
      value = Number.Round(value, mDecimals);
      entry = new EntryDbTable.Row();
      entry.setTimestamp(timestamp);
      entry.setValue(value);
      entry.setCategoryId(category.getId());
      entry.setNEntries(1);

      mLastAddId = mTSC.getDbh().createEntry(entry);
      mLastAddValue = value;
      mLastAddOldValue = 0.0f;
      mLastAddTimestamp = timestamp;
      mLastAddUpdate = false;

    } else {
      lastTScal = Calendar.getInstance();
      lastTScal.setTimeInMillis(entry.getTimestamp());

      if ((periodInMs == DateUtil.YEAR_MS && DateUtil.inSameYear(entryTScal,
          lastTScal))
          || (periodInMs == DateUtil.QUARTER_MS && DateUtil.inSameQuarter(
              entryTScal, lastTScal))
          || (periodInMs == DateUtil.MONTH_MS && DateUtil.inSameMonth(
              entryTScal, lastTScal))
          || (periodInMs == DateUtil.WEEK_MS && DateUtil.inSameWeek(entryTScal,
              lastTScal))
          || (periodInMs == DateUtil.DAY_MS && DateUtil.inSameDay(entryTScal,
              lastTScal))
          || (periodInMs == DateUtil.AMPM_MS && DateUtil.inSameAMPM(entryTScal,
              lastTScal))
          || (periodInMs == DateUtil.HOUR_MS && DateUtil.inSameHour(entryTScal,
              lastTScal))) {
        if (category.getType().equals(CategoryDbTable.KEY_TYPE_SUM))
          value += entry.getValue();
        else if (category.getType().equals(CategoryDbTable.KEY_TYPE_AVERAGE))
          value = ((entry.getValue() * entry.getNEntries()) + value)
              / (entry.getNEntries() + 1);
      }

      value = Number.Round(value, mDecimals);

      // entry.setTimestamp(mTimestamp);
      float oldValue = entry.getValue();
      entry.setValue(value);
      entry.setNEntries(entry.getNEntries() + 1);

      mLastAddId = entry.getId();
      mLastAddValue = value;
      mLastAddOldValue = oldValue;
      mLastAddTimestamp = timestamp;
      mLastAddUpdate = true;

      mTSC.getDbh().updateEntry(entry);
    }

    mTSC.updateCategoryTrend(category.getId());
  }
}
