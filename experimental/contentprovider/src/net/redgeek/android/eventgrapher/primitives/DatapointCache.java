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

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.net.Uri.Builder;
import android.text.TextUtils;

import net.redgeek.android.eventrecorder.TimeSeriesData;
import net.redgeek.android.eventrend.util.DateUtil;
import net.redgeek.android.eventrend.util.DateUtil.Period;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;

public class DatapointCache {
  private HashMap<Long, CategoryDatapointCache> mCache;
  private ContentResolver mResolver;

  public DatapointCache(ContentResolver resolver) {
    mResolver = resolver;
    mCache = new HashMap<Long, CategoryDatapointCache>();
  }

  public void clearCache() {
    Iterator<CategoryDatapointCache> itr = mCache.values().iterator();
    while (itr.hasNext()) {
      ((CategoryDatapointCache) itr.next()).clear();
    }
  }

  public void clearCache(long catId) {
    CategoryDatapointCache catCache = mCache.get(Long.valueOf(catId));
    if (catCache != null)
      catCache.clear();
    return;
  }

  public void addCacheableCategory(long catId, int history) {
    mCache.put(new Long(catId), new CategoryDatapointCache(catId, history));
  }

  public void setHistory(long catId, int history) {
    CategoryDatapointCache catCache = mCache.get(Long.valueOf(catId));
    if (catCache != null) {
      catCache.setHistory(history);
    }
    return;
  }

  public long getCategoryCacheStart(long catId) {
    CategoryDatapointCache catCache = mCache.get(Long.valueOf(catId));
    if (catCache == null || catCache.isValid() == false)
      return Long.MAX_VALUE;

    return catCache.getStart();
  }

  public long getCategoryCacheEnd(long catId) {
    CategoryDatapointCache catCache = mCache.get(Long.valueOf(catId));
    if (catCache == null || catCache.isValid() == false)
      return Long.MIN_VALUE;

    return catCache.getEnd();
  }

  public boolean isCategoryCacheValid(long catId) {
    CategoryDatapointCache catCache = mCache.get(Long.valueOf(catId));
    if (catCache == null)
      return false;
    return catCache.isValid();
  }

  public synchronized void refresh(long catId, String aggregation) {
    CategoryDatapointCache catCache = mCache.get(Long.valueOf(catId));
    if (catCache == null || catCache.isValid() == false)
      return;

    int start = catCache.getStart();
    int end = catCache.getEnd();

    catCache.clear();
    populateRangeFromDb(catCache, catId, start, end, aggregation);
  }

  public synchronized void populateLatest(long catId, int nItems, String aggregation) {
    ArrayList<Datapoint> append = new ArrayList<Datapoint>();
    CategoryDatapointCache catCache;
    catCache = mCache.get(Long.valueOf(catId));
    if (catCache == null)
      return;

    boolean initialized = catCache.isValid();
    int oldStart = catCache.getStart();
    int oldEnd = catCache.getEnd();
    boolean overlap = false;
    int lastEntryTS = Integer.MAX_VALUE;

    Builder builder = ContentUris.withAppendedId(
        TimeSeriesData.TimeSeries.CONTENT_URI, catId).buildUpon()
        .appendPath("recent").appendPath(""+nItems);
    if (TextUtils.isEmpty(aggregation))
      builder.appendPath(aggregation);
    
    Uri uri = builder.build();
    Cursor c = mResolver.query(uri, null, null, null, null);
    int count = c.getCount();
    if (count < 1) {
      c.close();
      return;
    }

    c.moveToFirst();
    for (int i = 0; i < count; i++) {
      Datapoint d = new Datapoint();
      d.mDatapointId = TimeSeriesData.Datapoint.getId(c);
      d.mTsStart = TimeSeriesData.Datapoint.getTsStart(c);
      d.mTsEnd = TimeSeriesData.Datapoint.getTsEnd(c);
      d.mValue = (float) TimeSeriesData.Datapoint.getValue(c);
      d.mTrend = (float) TimeSeriesData.Datapoint.getTrend(c);      
      d.mDatapointId = TimeSeriesData.Datapoint.getId(c);
      d.mEntries = TimeSeriesData.Datapoint.getEntries(c);
      d.mStdDev = (float) TimeSeriesData.Datapoint.getStdDev(c);
      d.mTimeSeriesId = catId;
      
      if (d.mTsStart < lastEntryTS)
        lastEntryTS = d.mTsStart;
      if (d.mTsStart <= oldEnd && d.mTsStart >= oldStart)
        overlap = true;
      else
        // we can't append the datapoints directly to the cache here, since that
        // may create a hole (and the caches are expected to be contiguous., so
        // save the datapoints away and add them after we fetch the hole
        append.add(d);
      c.moveToNext();
    }
    c.close();

    if (initialized == true && overlap == false) {
      // Fill in any hole between the old cache and the recent data
      populateRangeFromDb(catCache, catId, oldEnd + 1, lastEntryTS - 1, aggregation);
    }

    for (int i = 0; i < append.size(); i++)
      catCache.addDatapoint(append.get(i));

    return;
  }

  // inclusive
  public synchronized void populateRange(long catId, int milliStart,
      int milliEnd, int aggregationMs, String aggregation) {
    CategoryDatapointCache catCache = mCache.get(Long.valueOf(catId));
    if (catCache == null)
      return;

    if (aggregationMs == 0) {
      // we actually double the range, hoping to gather data from each category
      // that we can use to connect the first datapoint to an offscreen
      // datapoint
      // (earlier in time) and to use the previous data to make the trend lines
      // accurate at the beginning of the chart. Same goes for the last
      // offscreen
      // datapoint (later in time).
      // TODO: fix to guarantee this, instead of hoping -- and do it with a
      // miminum
      // of sql queries.
      long quarter = (milliEnd - milliStart) / 4;
      milliStart -= quarter * 2;
      milliEnd += quarter;
    } else {
      // This fixup looks similar to the previous in
      // TimerSeriesCollector::gatherSeries,
      // however, the purpose of this one is to make sure we grab try to grab X
      // datapoints,
      // which during aggregation, are 1 per period. Note this is still a guess,
      // as the
      // previous 2*X periods may not actually contain >= X datapoints, so we
      // still may
      // mess up the trend because we're short on datapoints. Same goes for the
      // endMillis
      // adjustment.
      Calendar c1 = Calendar.getInstance();
      Calendar c2 = Calendar.getInstance();
      Period p = DateUtil.mapLongToPeriod(aggregationMs);
      int step = 2;
      if (p == Period.QUARTER)
        step = 6;

      c1.setTimeInMillis(milliStart);
      DateUtil.setToPeriodStart(c1, p);
      c1.add(DateUtil.mapLongToCal(aggregationMs),
          -(catCache.getHistory() * step));
      milliStart = (int) (c1.getTimeInMillis() / DateUtil.SECOND_MS);

      c2.setTimeInMillis(milliEnd);
      DateUtil.setToPeriodStart(c2, p);
      c2.add(DateUtil.mapLongToCal(aggregationMs), step);
      milliEnd = (int) (c2.getTimeInMillis() / DateUtil.SECOND_MS);
    }

    populateRangeFromDb(catCache, catId, milliStart, milliEnd, aggregation);
  }

  private synchronized void populateRangeFromDb(
      CategoryDatapointCache catCache, long catId, int milliStart, 
      int milliEnd, String aggregation) {
    int query1Start = milliStart;
    int query1End = milliEnd;
    int query2Start = -1;
    int query2End = -1;

    if (milliStart > milliEnd)
      return;

    // SQL ops are the expensive thing, so see if we can minimize or obviate the
    // query
    // by checking the cache. Note that if we're extending the cache in two
    // direction
    // (asking for a superset of the cache), we turn this into two queries for
    // the pre
    // and post.
    if (catCache.isValid() == true) {
      if (milliStart >= catCache.getStart() && milliEnd <= catCache.getEnd())
        return;
    }

    if (catCache.isValid() == false) {
      query1Start = milliStart;
      query1End = milliEnd;
    } else {
      if (milliStart < catCache.getStart()) {
        query1Start = milliStart;
        query1End = catCache.getStart() - 1;
        if (milliEnd > catCache.getEnd()) {
          query2Start = catCache.getEnd() + 1;
          query2End = milliEnd;
        }
      }
      if (milliStart >= catCache.getStart()) {
        query1Start = catCache.getEnd() + 1;
        query1End = milliEnd;
      }
    }

    addDatapoints(catCache, catId, query1Start, query1End, aggregation);
    if (query2Start >= 0 && query2End >= 0)
      addDatapoints(catCache, catId, query2Start, query2End, aggregation);

    return;
  }

  private void addDatapoints(CategoryDatapointCache catCache, long catId,
      int start, int end, String aggregation) {
    Builder builder = ContentUris.withAppendedId(
        TimeSeriesData.TimeSeries.CONTENT_URI, catId).buildUpon()
        .appendPath("range").appendPath(""+start).appendPath(""+end);
    if (TextUtils.isEmpty(aggregation))
      builder.appendPath(aggregation);
    
    Uri uri = builder.build();
    Cursor c = mResolver.query(uri, null, null, null, null);
    int count = c.getCount();
    if (count < 1) {
      c.close();
      return;
    }

    if (c != null && c.getCount() > 0) {
      c.moveToFirst();
      for (int j = 0; j < c.getCount(); j++) {
        Datapoint d = new Datapoint();
        d.mDatapointId = TimeSeriesData.Datapoint.getId(c);
        d.mTsStart = TimeSeriesData.Datapoint.getTsStart(c);
        d.mTsEnd = TimeSeriesData.Datapoint.getTsEnd(c);
        d.mValue = (float) TimeSeriesData.Datapoint.getValue(c);
        d.mTrend = (float) TimeSeriesData.Datapoint.getTrend(c);      
        d.mDatapointId = TimeSeriesData.Datapoint.getId(c);
        d.mEntries = TimeSeriesData.Datapoint.getEntries(c);
        d.mStdDev = (float) TimeSeriesData.Datapoint.getStdDev(c);
        d.mTimeSeriesId = catId;

        catCache.addDatapoint(d);
        c.moveToNext();
      }
    }
    c.close();
    catCache.updateStart(start);
    catCache.updateEnd(end);
  }

  public ArrayList<Datapoint> getDataInRange(long catId, int msStart,
      int msEnd) {
    CategoryDatapointCache catCache = mCache.get(Long.valueOf(catId));
    if (catCache == null)
      return null;

    return catCache.getDataInRange(msStart, msEnd);
  }

  public ArrayList<Datapoint> getDataBefore(long catId, int number, int ms) {
    CategoryDatapointCache catCache = mCache.get(Long.valueOf(catId));
    if (catCache == null)
      return null;

    return catCache.getDataBefore(number, ms);
  }

  public ArrayList<Datapoint> getDataAfter(long catId, int number, int ms) {
    CategoryDatapointCache catCache = mCache.get(Long.valueOf(catId));
    if (catCache == null)
      return null;

    return catCache.getDataAfter(number, ms);
  }

  public ArrayList<Datapoint> getLast(long catId, int number) {
    CategoryDatapointCache catCache = mCache.get(Long.valueOf(catId));
    if (catCache == null)
      return null;

    return catCache.getLast(number);
  }
}
