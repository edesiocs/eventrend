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

import net.redgeek.android.eventgrapher.primitives.TimeSeriesCollector;

/**
 * This is the potentially the most frequently executed task, with the possible
 * exception of UpdateRecentDataTask. It is called every time a parameter of the
 * graph changes. It only responsibility to to query the database to collect the
 * datapoints required for displaying the graph lines, trends, labels, etc, and
 * shoving them in a cache. Since SQL operation are expensive, including
 * getColumnIndexOrThrow, they should be minimized, hence the cache.
 * 
 * <p>
 * Note that since this is "backgroundtask", no UI operations may be performed.
 * 
 * @author barclay
 * 
 */
public class DataCollectionTask {
  private TimeSeriesCollector mTSC = null;
  private String mAggregation;
  private int mStartMS = 0;
  private int mEndMS = 0;

  public DataCollectionTask() {
  }

  public DataCollectionTask(TimeSeriesCollector tsc) {
    mTSC = tsc;
  }

  public void setTimeSeriesCollector(TimeSeriesCollector tsc) {
    mTSC = tsc;
  }

  public void setSpan(int startMs, int endMs, String aggregation) {
    mStartMS = startMs;
    mEndMS = endMs;
    mAggregation = aggregation;
  }

  public void doCollection() {
    mTSC.gatherSeriesLocking(mStartMS, mEndMS, mAggregation);
  }
}