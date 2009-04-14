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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class DateMapCache {
  private Calendar mCal;
  private SQLiteDatabase mDbh;
  private TreeMap<Long, DateMapCacheEntry> mMsToEntry;
  private TreeMap<Integer, ArrayList<DateMapCacheEntry>> mYearToEntry;
  
  private static final int CACHE_START = 2000;
  private static final int CACHE_END   = 2020;
  
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
  
  public DateMapCache(SQLiteDatabase dbh) {
    mDbh = dbh;
    mCal = Calendar.getInstance();
    mMsToEntry = new TreeMap<Long, DateMapCacheEntry>();
    mYearToEntry = new TreeMap<Integer, ArrayList<DateMapCacheEntry>>();
    
    populateCache();
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

  public DateMapCacheEntry getEntry(int milliseconds, boolean before) {
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
        range = mMsToEntry.headMap(Long.valueOf(milliseconds));
        iterator = range.values().iterator();
      }
    } catch (Exception e) {
    }
    
    if (entry == null) {
      mCal.set(Calendar.MILLISECOND, milliseconds);
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
    } else {
      if (iterator != null && iterator.hasNext())
        entry = iterator.next();
    }

    return entry;
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
  
  private void populateCache() {
    DateMapCacheEntry entry = null;

    Cursor c = mDbh.rawQuery("SELECT count(*) FROM " 
        + TimeSeriesData.DateMap.TABLE_NAME, null);
    if (c != null && c.getCount() > 1) {
      c.close();
      return;
    }
    c.close();

    generateData();
    
    c = mDbh.rawQuery("SELECT * FROM " + TimeSeriesData.DateMap.TABLE_NAME, null);
    int count = c.getCount();
    if (count > 0) {
      c.moveToFirst();
      for (int i = 0; i < count; i++) {
        int year = c.getInt(c.getColumnIndexOrThrow(TimeSeriesData.DateMap.YEAR));
        int month = c.getInt(c.getColumnIndexOrThrow(TimeSeriesData.DateMap.MONTH));
        int dow = c.getInt(c.getColumnIndexOrThrow(TimeSeriesData.DateMap.DOW));
        int ms = c.getInt(c.getColumnIndexOrThrow(TimeSeriesData.DateMap.MILLISECONDS));
        
        entry = new DateMapCacheEntry(year, month, dow, ms);
        insertEntry(entry);
      }      
    }
    c.close();
    
    return;
  }
  
  private void generateData() {
    int dow;
    long ms;
    
    ContentValues values = new ContentValues();
    
    mCal.set(Calendar.MONTH, 0);
    mCal.set(Calendar.DAY_OF_MONTH, 1);
    mCal.set(Calendar.HOUR_OF_DAY, 0);
    mCal.set(Calendar.MINUTE, 0);
    mCal.set(Calendar.SECOND, 0);
    mCal.set(Calendar.MILLISECOND, 0);

    for (int yyyy = CACHE_START; yyyy < CACHE_END; yyyy++) {
      mCal.set(Calendar.YEAR, yyyy);
      for (int mm = 0; mm < 12; mm++) {
        mCal.set(Calendar.MONTH, mm);
        dow = mCal.get(Calendar.DAY_OF_WEEK);
        ms = mCal.getTimeInMillis();
        
        values.clear();
        values.put(TimeSeriesData.DateMap.YEAR, yyyy);
        values.put(TimeSeriesData.DateMap.MONTH, mm);
        values.put(TimeSeriesData.DateMap.DOW, dow);
        values.put(TimeSeriesData.DateMap.MILLISECONDS, ms);

        mDbh.insert(TimeSeriesData.DateMap.TABLE_NAME, null, values);
      }
    }
  }
}
