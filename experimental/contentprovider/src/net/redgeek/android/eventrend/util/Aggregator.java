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

package net.redgeek.android.eventrend.util;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import net.redgeek.android.eventrecorder.DateMapCache;
import net.redgeek.android.eventrecorder.TimeSeriesData;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Aggregator {
  private DateMapCache mDateMap;

  public static class Aggregate {
    public int   mTsStart;
    public int   mTsEnd;
    public float mValue;
    public int   mCount;
    
    public Aggregate(int start, int end, float value) {
      mTsStart = start;
      mTsEnd = end;
      mValue = value;
      mCount = 1;
    }
  }
  
  public Aggregator(DateMapCache cache) {
    mDateMap = cache;
  }
  
  public ArrayList<Aggregate> aggregate(Cursor c, int period) {
    ArrayList<Aggregate> aggregates = new ArrayList<Aggregate>();
    if (c == null || c.getCount() < 1)
      return aggregates;    
    
    int count = c.getCount();    
    
    Aggregate accumulator = null;
    Aggregate tmp = null;
    
    c.moveToFirst();
    for (int i = 0; i < count; i++) {
      int start = mDateMap.secondsOfPeriodStart(
          TimeSeriesData.Datapoint.getTsStart(c), period);

      tmp = new Aggregate(
          start,
          start + period - 1,
          TimeSeriesData.Datapoint.getValue(c));

      if (i == 0) {
        accumulator = new Aggregate(tmp.mTsStart, tmp.mTsEnd, tmp.mValue);
        continue;
      }
      
      if (accumulator.mTsStart != tmp.mTsStart) {
        aggregates.add(accumulator);
        accumulator = new Aggregate(tmp.mTsStart, tmp.mTsEnd, tmp.mValue);
      }
      else {
        accumulator.mValue += tmp.mValue;
        accumulator.mCount++;
      }
      
      c.moveToNext();
    }
    c.close();
    
    if (accumulator != null)
      aggregates.add(accumulator);

    return aggregates;
  }  
}
