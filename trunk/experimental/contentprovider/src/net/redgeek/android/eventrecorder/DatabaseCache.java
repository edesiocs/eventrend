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

package net.redgeek.android.eventrecorder;

import java.util.HashMap;
import java.util.TreeMap;

import net.redgeek.android.eventrecorder.TimeSeriesData.Datapoint;
import net.redgeek.android.eventrecorder.TimeSeriesData.TimeSeries;
import android.content.ContentValues;

public class DatabaseCache {
  private HashMap<Long, TimeSeriesEntry> mCache;
  
  public static class TimeSeriesEntry {
    public long   mId;
    public String mName;
    public long   mRecordingDatapointId;
    public String mGroupName;
    public float  mDefaultValue;
    public float  mIncrement;
    public float  mGoal;
    public String mColor;
    public int    mPeriod;
    public String mUnits;
    public int    mRank;
    public String mAggregation;
    public String mType;
    public int    mZerofill;
    public String mFormula;
    public String mInterpolation;
    public TreeMap<Long, DatapointEntry> mDatapointCache;
    public HashMap<Long, Long> mIdTimestampMap;
    public TreeMap<Long, Long> mQueryRangeMap;
    
    public TimeSeriesEntry() {      
      mDatapointCache = new TreeMap<Long, DatapointEntry>();
      mIdTimestampMap = new HashMap<Long, Long>();
      mQueryRangeMap = new TreeMap<Long, Long>();
    }
  }

  public static class DatapointEntry {
    public long  mId;
    public long  mTimeSeriesId;
    public long  mTsStart;
    public long  mTsEnd;
    public float mValue;
    public int   mUpdates;
    
    public DatapointEntry() {      
    }
  }
  
  public DatabaseCache() {
    mCache = new HashMap<Long, TimeSeriesEntry>();
  }

  public void removeTimeSeries(Long timeSeriesId) {
    mCache.remove(timeSeriesId);
    return;
  }

  public void removeAllTimeSeries() {
    mCache.clear();
    return;
  }

  public void removeDatapoint(Long timeSeriesId, Long datapointId) {
    TimeSeriesEntry cache = mCache.get(timeSeriesId);
    if (cache != null) {
      Long timestamp = cache.mIdTimestampMap.get(datapointId);
      if (timestamp != null) {
        cache.mDatapointCache.remove(timestamp);
        cache.mIdTimestampMap.remove(Long.valueOf(datapointId));
      }
    }
    return;
  }

  public void insertTimeSeries(Long timeSeriesId, ContentValues values) {
    if (timeSeriesId < 1)
      return;
    
    TimeSeriesEntry entry = new TimeSeriesEntry();
    entry.mId = timeSeriesId;
    entry.mName = values.getAsString(TimeSeries.TIMESERIES_NAME);
    entry.mRecordingDatapointId = values.getAsLong(TimeSeries.RECORDING_DATAPOINT_ID);
    entry.mGroupName = values.getAsString(TimeSeries.GROUP_NAME);
    entry.mDefaultValue = values.getAsFloat(TimeSeries.DEFAULT_VALUE);
    entry.mIncrement = values.getAsFloat(TimeSeries.INCREMENT);
    entry.mGoal = values.getAsFloat(TimeSeries.GOAL);
    entry.mColor = values.getAsString(TimeSeries.COLOR);
    entry.mPeriod = values.getAsInteger(TimeSeries.PERIOD);
    entry.mUnits = values.getAsString(TimeSeries.UNITS);
    entry.mRank = values.getAsInteger(TimeSeries.RANK);
    entry.mAggregation = values.getAsString(TimeSeries.AGGREGATION);
    entry.mType = values.getAsString(TimeSeries.TYPE);
    entry.mZerofill = values.getAsInteger(TimeSeries.ZEROFILL);
    entry.mFormula = values.getAsString(TimeSeries.FORMULA);
    entry.mInterpolation = values.getAsString(TimeSeries.INTERPOLATION);

    mCache.put(timeSeriesId, entry);

    return;
  }

  public void insertDatapoint(Long timeSeriesId, Long datapointId, 
      ContentValues values) {
    if (timeSeriesId < 1 || datapointId < 1)
      return;
    
    Long timestamp = values.getAsLong(Datapoint.TS_START);
    TimeSeriesEntry cache = mCache.get(timeSeriesId);
    if (cache != null && timestamp != null) {
      DatapointEntry dpe = new DatapointEntry();
      dpe.mId = datapointId;
      dpe.mTimeSeriesId = timeSeriesId;
      dpe.mTsStart = values.getAsLong(Datapoint.TS_START);
      dpe.mTsEnd = values.getAsLong(Datapoint.TS_END);
      dpe.mValue = values.getAsFloat(Datapoint.VALUE);
      dpe.mUpdates = values.getAsInteger(Datapoint.UPDATES);

      cache.mDatapointCache.put(timestamp, dpe);
      cache.mIdTimestampMap.put(datapointId, timestamp);
      // TODO: update range map
    }
    return;
  }
  
  private static class QueryRange {
    public long mStart;
    public long mStop;
    
    public QueryRange(long start, long stop) {
      mStart = start;
      mStop = stop;
    }
    
    public boolean rangeOverlaps(long start, long stop) {
      if (mStart <= start && mStop >= stop)
        return true;
      return false;
    }
  }  
}