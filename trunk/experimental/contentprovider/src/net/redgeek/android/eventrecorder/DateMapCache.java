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

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

public class DateMapCache {
  private Calendar mCal;
  private TreeMap<Long, DateMapCacheEntry> mMsToEntry;
  private TreeMap<Integer, ArrayList<DateMapCacheEntry>> mYearToEntry;
  
  /** Number of milliseconds in a second */
  public static final long SECOND_MS = 1000;
  /** Number of milliseconds in a minute */
  public static final long MINUTE_MS = SECOND_MS * 60;
  /** Number of milliseconds in an hour */
  public static final long HOUR_MS = MINUTE_MS * 60;
  /** Number of milliseconds in a morning or evening (1/2 day) */
  public static final long AMPM_MS = HOUR_MS * 12;
  /** Number of milliseconds in a day */
  public static final long DAY_MS = HOUR_MS * 24;
  /** Number of milliseconds in a week */
  public static final long WEEK_MS = DAY_MS * 7;
  /** Number of milliseconds in a year */
  public static final long YEAR_MS = WEEK_MS * 52;
  /** Number of milliseconds in a quarter (as defined by 1/4 of a year) */
  public static final long QUARTER_MS = WEEK_MS * 13;
  /** Number of milliseconds in a month (as defined by 1/12 of a year) */
  public static final long MONTH_MS = YEAR_MS / 12;
  
  public static class DateMapCacheEntry {
    public long   mId = 0;
    public int    mYear;
    public int    mMonth;
    public int    mDOW;
    public long   mMilliseconds;
    
    public DateMapCacheEntry() {      
    }

    public DateMapCacheEntry(int year, int month, int dow, long ms) {
      mYear = year;
      mMonth = month;
      mDOW = dow;
      mMilliseconds = ms;
    }  
  }
  
  public DateMapCache() {
    mCal = Calendar.getInstance();
    mMsToEntry = new TreeMap<Long, DateMapCacheEntry>();
    mYearToEntry = new TreeMap<Integer, ArrayList<DateMapCacheEntry>>();
  }
  
  public void clear() {
    mMsToEntry.clear();
    mYearToEntry.clear();
  }

  public DateMapCacheEntry getEntry(int year, int month) {
    ArrayList<DateMapCacheEntry> months = null;
    DateMapCacheEntry entry = null;
    
    try {
      months = mYearToEntry.get(Integer.valueOf(year));
      entry = months.get(month);
    } catch(Exception e) {
    }

    if (entry == null) {
      mCal.set(Calendar.YEAR, year);
      mCal.set(Calendar.MONTH, month);
      mCal.set(Calendar.DAY_OF_MONTH, 1);
      mCal.set(Calendar.HOUR_OF_DAY, 0);
      mCal.set(Calendar.MINUTE, 0);
      mCal.set(Calendar.SECOND, 0);
      mCal.set(Calendar.MILLISECOND, 0);
      entry = new DateMapCacheEntry(year, month, mCal.get(Calendar.DAY_OF_WEEK),
          mCal.getTimeInMillis());
    }
    
    return entry;
  }

  public DateMapCacheEntry getEntry(long milliseconds, boolean before) {
    DateMapCacheEntry entry = null;
    Iterator<DateMapCacheEntry> iterator = null;

    SortedMap<Long, DateMapCacheEntry> range;
    SortedMap<Long, DateMapCacheEntry> reverse;
    
    try {
      entry = mMsToEntry.get(Long.valueOf(milliseconds));
    } catch (Exception e) {
    }
    if (entry != null)
      return entry;
      
    try {
      if (before == true) {
        range = mMsToEntry.headMap(Long.valueOf(milliseconds));
        reverse = new TreeMap<Long, DateMapCacheEntry>(java.util.Collections.reverseOrder());
        reverse.putAll(range);
        iterator = reverse.values().iterator();
      } else {
        range = mMsToEntry.tailMap(Long.valueOf(milliseconds));
        iterator = range.values().iterator();
      }
    } catch (Exception e) {
    }
    
    if (iterator != null && iterator.hasNext())
      entry = iterator.next();

    if (entry == null) {
      mCal.set(Calendar.MILLISECOND, (int) milliseconds);
      mCal.set(Calendar.DAY_OF_MONTH, 1);
      mCal.set(Calendar.HOUR_OF_DAY, 0);
      mCal.set(Calendar.MINUTE, 0);
      mCal.set(Calendar.SECOND, 0);
      mCal.set(Calendar.MILLISECOND, 0);
      entry = new DateMapCacheEntry(
          mCal.get(Calendar.YEAR),
          mCal.get(Calendar.MONTH),
          mCal.get(Calendar.DAY_OF_WEEK),
          mCal.getTimeInMillis());
    }

    return entry;
  }
  
  public long millisecondsOfPeriodStart(long milliseconds, long period) {
    long ms;
    long nPeriods = 0;
    DateMapCacheEntry entry;

    entry = getEntry(milliseconds, true);
    ms = entry.mMilliseconds;
    
    if (entry.mMilliseconds == milliseconds || period == MONTH_MS)
      return ms;

    if (period < MONTH_MS) {
      while (ms < milliseconds - period) {
        ms += period;
      }
    }
    else {
      while (entry.mMilliseconds + period > milliseconds) {
        ms = entry.mMilliseconds;
        entry = getEntry(entry.mMilliseconds - 1, true);
      }
    }

    return ms;
  }
  
  public void populateCache(Context ctx) {
    DateMapCacheEntry entry = null;

    Uri datemap = TimeSeriesData.DateMap.CONTENT_URI;
    Cursor c = ctx.getContentResolver().query(datemap, null, null, null, null);
    if (c.moveToFirst()) {
      int count = c.getCount();
      for (int i = 0; i < count; i++) {
        int year = TimeSeriesData.DateMap.getYear(c);
        int month = TimeSeriesData.DateMap.getMonth(c);
        int dow = TimeSeriesData.DateMap.getDOW(c);
        long ms = TimeSeriesData.DateMap.getMilliseconds(c);
        
        entry = new DateMapCacheEntry(year, month, dow, ms);
        insertEntry(entry);
      }      
    }
    c.close();
    
    return;
  }
  
  private void insertEntry(DateMapCacheEntry dmce) {
    ArrayList<DateMapCacheEntry> months = null;
    if (dmce == null || dmce.mId < 1)
      return;

    Integer year = Integer.valueOf(dmce.mYear);    
    try {
      months = mYearToEntry.get(year);
    } catch(Exception e) {
    }
    
    if (months == null) {
      months = new ArrayList<DateMapCacheEntry>(12);
      mYearToEntry.put(year, months);
    }    
    months.add(dmce.mMonth, dmce);
    mMsToEntry.put(Long.valueOf(dmce.mMilliseconds), dmce);

    return;
  }  
}
