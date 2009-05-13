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

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.net.Uri.Builder;
import android.text.TextUtils;

import net.redgeek.android.eventgrapher.TimeSeriesPainter;
import net.redgeek.android.eventrecorder.TimeSeriesData;
import net.redgeek.android.eventrend.category.CategoryRow;

import java.util.ArrayList;

public class TimeSeriesCollector {
  private ArrayList<TimeSeries> mSeries;
  private ContentResolver mResolver;
  private String mAggregation;

  public TimeSeriesCollector(ContentResolver resolver) {
    initialize(resolver, null);
  }

  public TimeSeriesCollector(ContentResolver resolver, TimeSeriesPainter painter) {
    initialize(resolver, painter);
  }

  public void initialize(ContentResolver resolver, TimeSeriesPainter painter) {
    mResolver = resolver;
    mSeries = new ArrayList<TimeSeries>();
  }

  @Override
  public String toString() {
    return mSeries.toString();
  }

  public void setAggregationPeriod(String period) {
    mAggregation = period;
  }
  
  public void fetchTimeSeries() {
    Uri uri = TimeSeriesData.TimeSeries.CONTENT_URI;
    Cursor c = mResolver.query(uri, null, null, null, null);
    int count = c.getCount();
    if (count < 1) {
      c.close();
      return;
    }

    c.moveToFirst();
    for (int i = 0; i < c.getCount(); i++) {
      CategoryRow row = new CategoryRow(c);
      TimeSeries ts = new TimeSeries(row);
      mSeries.add(ts);
      c.moveToNext();
    }
    c.close();
  }

  public void fetchTimeSeriesData(long timeSeriesId) {
    ArrayList<Datapoint> entries = new ArrayList<Datapoint>();
    
    TimeSeries ts = getSeriesById(timeSeriesId);
    
    Builder builder = ContentUris.withAppendedId(
        TimeSeriesData.TimeSeries.CONTENT_URI, timeSeriesId).buildUpon()
        .appendPath("range").appendPath("0").appendPath(""+Integer.MAX_VALUE);
    if (TextUtils.isEmpty(mAggregation) == false)
      builder.appendPath(mAggregation);
    
    Uri uri = builder.build();
    Cursor c = mResolver.query(uri, null, null, null, null);
    int count = c.getCount();
    if (count < 1) {
      c.close();
      return;
    }

    if (c != null && count > 0) {
      c.moveToFirst();
      for (int j = 0; j < count; j++) {
        Datapoint d = new Datapoint();
        d.mDatapointId = TimeSeriesData.Datapoint.getId(c);
        d.mTsStart = TimeSeriesData.Datapoint.getTsStart(c);
        d.mTsEnd = TimeSeriesData.Datapoint.getTsEnd(c);
        d.mValue = (float) TimeSeriesData.Datapoint.getValue(c);
        d.mTrend = (float) TimeSeriesData.Datapoint.getTrend(c);      
        d.mDatapointId = TimeSeriesData.Datapoint.getId(c);
        d.mEntries = TimeSeriesData.Datapoint.getEntries(c);
        d.mStdDev = (float) TimeSeriesData.Datapoint.getStdDev(c);
        d.mTimeSeriesId = timeSeriesId;
        entries.add(d);
        c.moveToNext();
      }
    }
    c.close();
    
    ts.setDatapoints(entries);
  }

  public boolean isSeriesEnabled(long catId) {
    boolean b;
    TimeSeries ts = getSeriesById(catId);
    if (ts == null)
      b = false;
    else
      b = ts.isEnabled();
    return b;
  }

  public void setSeriesEnabled(long catId, boolean b) {
    TimeSeries ts = getSeriesById(catId);
    if (ts != null)
      ts.setEnabled(b);
    return;
  }

  public void toggleSeriesEnabled(long catId) {
    TimeSeries ts = getSeriesById(catId);

    if (ts.isEnabled())
      ts.setEnabled(false);
    else
      ts.setEnabled(true);
    return;
  }

  public int numSeries() {
    return mSeries.size();
  }

  public TimeSeries getSeriesByIndex(int i) {
    TimeSeries ts = null;
    try {
      ts = mSeries.get(i);
    } catch (IndexOutOfBoundsException e) {
      ts = null;
    }
    return ts;
  }

  public TimeSeries getSeriesById(long catId) {
    for (int i = 0; i < mSeries.size(); i++) {
      TimeSeries ts = mSeries.get(i);
      if (ts != null && ts.mRow.mId == catId)
        return ts;
    }
    return null;
  }

  public TimeSeries getSeriesByName(String name) {
    for (int i = 0; i < mSeries.size(); i++) {
      TimeSeries ts = mSeries.get(i);
      if (ts != null && ts.mRow.mTimeSeriesName.equals(name))
        return ts;
    }
    return null;
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
}
