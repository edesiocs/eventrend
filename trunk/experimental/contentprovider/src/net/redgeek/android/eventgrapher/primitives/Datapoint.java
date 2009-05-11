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
  public long  mTsStart;
  public long  mTsEnd;
  public float mValue;
  public float mTrend;
  public FloatTuple mScreenValue1;
  public FloatTuple mScreenValue2;
  public FloatTuple mScreenTrend1;
  public FloatTuple mScreenTrend2;
  public float mStdDev;
  public int mEntries;
    
  public long mTimeSeriesId;
  public long mDatapointId;  
  
  public Datapoint() {
  }

  public Datapoint(long start, long stop, float value, float trend, float stddev, 
      long tsId, long entryId, int entries) {
    mScreenValue1 = new FloatTuple(0.0f, 0.0f);
    mScreenValue2 = new FloatTuple(0.0f, 0.0f);
    mScreenTrend1 = new FloatTuple(0.0f, 0.0f);
    mScreenTrend2 = new FloatTuple(0.0f, 0.0f);
    
    mTimeSeriesId = tsId;
    mDatapointId = entryId;
    mEntries = entries;
    mStdDev = stddev;
  }

  public Datapoint(Datapoint d) {
    mScreenValue1 = new FloatTuple(d.mScreenValue1);
    mScreenValue2 = new FloatTuple(d.mScreenValue2);
    mScreenTrend1 = new FloatTuple(d.mScreenTrend1);
    mScreenTrend2 = new FloatTuple(d.mScreenTrend2);

    mTimeSeriesId = d.mTimeSeriesId;
    mDatapointId = d.mDatapointId;
    mEntries = d.mEntries;
    mStdDev = d.mStdDev;
  }

  @Override
  public String toString() {
    return String.format("(%d -> %d, %f)", mTsStart, mTsEnd, mValue);
  }

  public String toLabelString() {
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(mTsStart);
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
    if (mTsStart == other.mTsStart && mTsEnd == other.mTsEnd
        && mTimeSeriesId == other.mTimeSeriesId
        && mDatapointId == other.mDatapointId && mValue == other.mValue
        && mTrend == other.mTrend) {
      return true;
    }
    return false;
  }

  public boolean timestampEqual(Datapoint other) {
    if (this.mTsStart == other.mTsStart && mTsEnd == other.mTsEnd)
      return true;
    return false;
  }

  public int compare(Datapoint d1, Datapoint d2) {
    if (d1.mTsStart < d2.mTsStart)
      return -1;
    if (d1.mTsStart > d2.mTsStart)
      return 1;
    return 0;
  }
}
