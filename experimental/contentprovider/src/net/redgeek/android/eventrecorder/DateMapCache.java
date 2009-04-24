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

import net.redgeek.android.eventrecorder.TimeSeriesData.DateMap;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

public class DateMapCache {
  private Calendar mCal;
  private TreeMap<Integer, DateMapCacheEntry> mSecToEntry;
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
    public int    mSeconds;
    
    public DateMapCacheEntry() {      
    }

    public DateMapCacheEntry(int year, int month, int dow, int seconds) {
      mYear = year;
      mMonth = month;
      mDOW = dow;
      mSeconds = seconds;
    }  
  }
  
  public DateMapCache() {
    mCal = Calendar.getInstance();
    mSecToEntry = new TreeMap<Integer, DateMapCacheEntry>();
    mYearToEntry = new TreeMap<Integer, ArrayList<DateMapCacheEntry>>();
  }
  
  public void clear() {
    mSecToEntry.clear();
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
      
      int dow = mCal.get(Calendar.DAY_OF_WEEK);
      int seconds = (int) (mCal.getTimeInMillis() / SECOND_MS);
      entry = new DateMapCacheEntry(year, month, dow, seconds);
    }
    
    return entry;
  }

  public DateMapCacheEntry getEntry(int seconds, boolean before) {
    DateMapCacheEntry entry = null;
    Iterator<DateMapCacheEntry> iterator = null;

    SortedMap<Integer, DateMapCacheEntry> range;
    SortedMap<Integer, DateMapCacheEntry> reverse;
    
    try {
      entry = mSecToEntry.get(Integer.valueOf(seconds));
    } catch (Exception e) {
    }
    if (entry != null)
      return entry;
      
    try {
      if (before == true) {
        range = mSecToEntry.headMap(Integer.valueOf(seconds));
        reverse = new TreeMap<Integer, DateMapCacheEntry>(java.util.Collections.reverseOrder());
        reverse.putAll(range);
        iterator = reverse.values().iterator();
      } else {
        range = mSecToEntry.tailMap(Integer.valueOf(seconds));
        iterator = range.values().iterator();
      }
    } catch (Exception e) {
    }
    
    if (iterator != null && iterator.hasNext())
      entry = iterator.next();

    if (entry == null) {
      mCal.setTimeInMillis(seconds * SECOND_MS);
      mCal.set(Calendar.DAY_OF_MONTH, 1);
      mCal.set(Calendar.HOUR_OF_DAY, 0);
      mCal.set(Calendar.MINUTE, 0);
      mCal.set(Calendar.SECOND, 0);
      mCal.set(Calendar.MILLISECOND, 0);
      entry = new DateMapCacheEntry(
          mCal.get(Calendar.YEAR),
          mCal.get(Calendar.MONTH),
          mCal.get(Calendar.DAY_OF_WEEK),
          (int) (mCal.getTimeInMillis() / SECOND_MS));
    }

    return entry;
  }
  
  public int secondsOfPeriodStart(int seconds, int period) {
    int secs;
    int nPeriods = 0;
    DateMapCacheEntry entry;

    entry = getEntry(seconds, true);
    secs = entry.mSeconds;
    
    if (entry.mSeconds == seconds || period == (MONTH_MS / SECOND_MS))
      return secs;

    if (period < (WEEK_MS / SECOND_MS)) {
      while (secs <= seconds - period) {
        secs += period;
      }
    }
    else if (period == (WEEK_MS / SECOND_MS)) {
      // get the DOW of the first day of the month, and subtract out the 
      // seconds from then until the start of the week from the last month:
      secs = (int) ((entry.mSeconds) - ((DAY_MS / SECOND_MS) * (entry.mDOW - 1)));
      if (seconds - period >= entry.mSeconds) {
        // start of week is in the same month,
        // advance until we get to the week before this one:
        while (secs <= seconds - period) {
          secs += period;
        }
      }
    }
    else if (period == (QUARTER_MS / SECOND_MS)) {
      while (!(entry.mMonth == 0 || entry.mMonth == 3 
          || entry.mMonth == 6 || entry.mMonth == 9)) {
        entry = getEntry(entry.mSeconds - 1, true);        
      }
      secs = entry.mSeconds;
    }
    else { // YEAR_MS / SECOND_MS
      while (entry.mMonth != 0) {
        entry = getEntry(entry.mSeconds - 1, true);        
      }
      secs = entry.mSeconds;
    }

    return secs;
  }
  
  public int secondsOfPeriodEnd(int seconds, int period) {
    DateMapCacheEntry entry;
    int secs = secondsOfPeriodStart(seconds, period);

    if (period < (MONTH_MS / SECOND_MS)) {
      secs = secs + period - 1;
    } else if (period == (MONTH_MS / SECOND_MS)) {
      entry = getEntry(seconds + 1, false);
      secs = entry.mSeconds - 1;
    } else if (period == (QUARTER_MS / SECOND_MS)) {
      entry = getEntry(seconds + 1, false);
      while (!(entry.mMonth == 0 || entry.mMonth == 3 || entry.mMonth == 6 || entry.mMonth == 9)) {
        entry = getEntry(entry.mSeconds + 1, false);
      }
      secs = entry.mSeconds - 1;
    } else { // YEAR_MS / SECOND_MS
      entry = getEntry(seconds + 1, false);
      while (entry.mMonth != 0) {
        entry = getEntry(entry.mSeconds + 1, false);
      }
      secs = entry.mSeconds - 1;
    }
      
    return secs;
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
        int secs = TimeSeriesData.DateMap.getSeconds(c);
        
        entry = new DateMapCacheEntry(year, month, dow, secs);
        insertEntry(entry);
      }      
    }
    c.close();
    
    return;
  }
  
  // not intended for use outside of the TimeSeriesProvider:
  public void populateCacheLocalToProvider(SQLiteDatabase db) {
    DateMapCacheEntry entry = null;
    SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
    qb.setTables(DateMap.TABLE_NAME);
    Cursor c = qb.query(db, null, null, null, null, null, DateMap.DEFAULT_SORT_ORDER, null);    
    if (c.moveToFirst()) {
      int count = c.getCount();
      for (int i = 0; i < count; i++) {
        int year = TimeSeriesData.DateMap.getYear(c);
        int month = TimeSeriesData.DateMap.getMonth(c);
        int dow = TimeSeriesData.DateMap.getDOW(c);
        int secs = TimeSeriesData.DateMap.getSeconds(c);
        
        entry = new DateMapCacheEntry(year, month, dow, secs);
        insertEntry(entry);
        c.moveToNext();
      }      
    }
    c.close();
    
    return;
  }

  private void insertEntry(DateMapCacheEntry dmce) {
    ArrayList<DateMapCacheEntry> months = null;
    if (dmce == null)
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
    mSecToEntry.put(Integer.valueOf(dmce.mSeconds), dmce);

    return;
  }  
}
