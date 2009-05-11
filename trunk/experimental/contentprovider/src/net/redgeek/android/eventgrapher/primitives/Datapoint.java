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



import java.util.Calendar;
import java.util.Comparator;


/**
 * The basic element used in graphing.
 * 
 * @author barclay
 * 
 */
public final class Datapoint implements Comparator<Datapoint> {
  public GraphTuple mValue;
  public GraphTuple mTrend;
  public float mStdDev;
  public int mEntries;
    
  public long mTimeSeriesId;
  public long mDatapointId;  
  
  public Datapoint() {
  }

  public Datapoint(long start, long stop, float value, float trend, float stddev, 
      long tsId, long entryId, int entries) {
    mValue = new GraphTuple(start, stop, value);
    mTrend = new GraphTuple(start, stop, trend);
    
    mTimeSeriesId = tsId;
    mDatapointId = entryId;
    mEntries = entries;
    mStdDev = stddev;
  }

  public Datapoint(Datapoint d) {
    mValue = new GraphTuple(d.mValue);
    mTrend = new GraphTuple(d.mTrend);
    
    mTimeSeriesId = d.mTimeSeriesId;
    mDatapointId = d.mDatapointId;
    mEntries = d.mEntries;
    mStdDev = d.mStdDev;
  }

  @Override
  public String toString() {
    return String.format("(%d -> %d, %f)", mValue.mX1, mValue.mX2, mValue);
  }

  public String toLabelString() {
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(mValue.mX1);
    long year = cal.get(Calendar.YEAR);
    long month = cal.get(Calendar.MONTH) + 1;
    long day = cal.get(Calendar.DAY_OF_MONTH);
    long hour = cal.get(Calendar.HOUR_OF_DAY);
    long minute = cal.get(Calendar.MINUTE);
    return String
        .format("%d/%02d/%02d %d:%02d", year, month, day, hour, minute);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof Datapoint))
      return false;
    Datapoint other = (Datapoint) obj;
    if (mValue.mX1 == other.mValue.mX1 && mValue.mX2 == other.mValue.mX2
        && mTimeSeriesId == other.mTimeSeriesId
        && mDatapointId == other.mDatapointId && mValue == other.mValue
        && mTrend == other.mTrend) {
      return true;
    }
    return false;
  }

  public boolean timestampEqual(Datapoint other) {
    if (this.mValue.mX1 == other.mValue.mX1 && mValue.mX2 == other.mValue.mX2)
      return true;
    return false;
  }

  public int compare(Datapoint d1, Datapoint d2) {
    if (d1.mValue.mX1 < d2.mValue.mX1)
      return -1;
    if (d1.mValue.mX1 > d2.mValue.mX1)
      return 1;
    return 0;
  }
}
