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
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

import net.redgeek.android.eventrecorder.TimeSeriesData.DateMap;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.TreeMap;

public class DateMapCache {
  private Calendar mCal;
  private DateMapCacheEntry[] mSecToEntry;
  private TreeMap<Integer, ArrayList<DateMapCacheEntry>> mYearToEntry;

  public static class DateMapCacheEntry {
    public long mId = 0;
    public int mYear;
    public int mMonth;
    public int mDOW;
    public int mSeconds;

    public DateMapCacheEntry() {
    }

    public DateMapCacheEntry(int year, int month, int dow, int seconds) {
      mYear = year;
      mMonth = month;
      mDOW = dow;
      mSeconds = seconds;
    }
  }

  public static class DateItem {
    public int mYear;
    public int mMonth;
    public int mDOW;
    public int mDay;
    public int mHour;
    public int mMinute;
    public int mSecond;
    public int mEpochSeconds;

    public DateItem() {
    }

    public void set(DateItem d) {
      mYear = d.mYear;
      mMonth = d.mMonth;
      mDOW = d.mDOW;
      mDay = d.mDay;
      mHour = d.mHour;
      mMinute = d.mMinute;
      mSecond = d.mSecond;
      mEpochSeconds = d.mEpochSeconds;
    }
    
    public boolean isEqual(DateItem d) {
      if (mEpochSeconds == d.mEpochSeconds)
        return true;
      return false;
    }
  }

  public String toDisplayTime(int seconds, int period) {
    DateItem d = new DateItem();
    String s;
    if (getDateItem(seconds, d) == false)
      return "?/?/? ?:?:?";

    if (period == DateMap.YEAR_SECS) {
      s = "" + d.mYear;
    } else if (period == DateMap.QUARTER_SECS) {
      int quarter = (d.mMonth / 3) + 1;
      s = d.mYear + " Q" + quarter;
    } else if (period == DateMap.MONTH_SECS) {
      s = d.mYear + "/" + (d.mMonth < 10 ? "0" + d.mMonth : d.mMonth);
    } else if (period == DateMap.WEEK_SECS || period == DateMap.DAY_SECS) {
      s = d.mYear + "/" + (d.mMonth < 10 ? "0" + d.mMonth : d.mMonth) + "/"
          + (d.mDay < 10 ? "0" + d.mDay : d.mDay);
    } else {
      s = d.mYear + "/" + (d.mMonth < 10 ? "0" + d.mMonth : d.mMonth) + "/"
          + (d.mDay < 10 ? "0" + d.mDay : d.mDay) + " "
          + (d.mHour < 10 ? "0" + d.mHour : d.mHour) + ":"
          + (d.mMinute < 10 ? "0" + d.mMinute : d.mMinute) + ":"
          + (d.mSecond < 10 ? "0" + d.mSecond : d.mSecond);
    }
    return s;
  }

  public DateMapCache() {
    mCal = Calendar.getInstance();
    mYearToEntry = new TreeMap<Integer, ArrayList<DateMapCacheEntry>>();
  }

  public void clear() {
    mSecToEntry = null;
    mYearToEntry.clear();
  }

  public DateMapCacheEntry getEntry(int year, int month) {
    ArrayList<DateMapCacheEntry> months = null;
    DateMapCacheEntry entry = null;

    try {
      months = mYearToEntry.get(Integer.valueOf(year));
      entry = months.get(month);
    } catch (Exception e) {
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
      int seconds = (int) (mCal.getTimeInMillis() / DateMap.SECOND_MS);
      entry = new DateMapCacheEntry(year, month, dow, seconds);
    }

    return entry;
  }

  public DateMapCacheEntry getEntry(int seconds, boolean before) {
    DateMapCacheEntry d = null;

    if (mSecToEntry != null && mSecToEntry.length > 0) {
      int min = 0;
      int max = mSecToEntry.length - 1;
      int mid = max / 2;
      int length = mSecToEntry.length;

      d = mSecToEntry[mid];
      while (d != null) {
        if (d.mSeconds == seconds) {
          return d;
        } else if (max < min) {
          if (before == true) {
            if (d.mSeconds > seconds) {
              if (mid - 1 >= 0)
                d = mSecToEntry[mid - 1];
              else {
                d = null;
                break;
              }
            }
          } else {
            if (d.mSeconds < seconds) {
              if (mid + 1 < length)
                d = mSecToEntry[mid + 1];
              else {
                d = null;
                break;
              }
            }
          }
          return d;
        } else if (d.mSeconds < seconds) {
          min = mid + 1;
        } else if (d.mSeconds > seconds) {
          max = mid - 1;
        }
        mid = min + ((max - min) / 2);

        // Check to see if we were trying to run off the end, if so, just
        // return the first or last entry.
        if (mid >= length && before == true) {
          d = null;
          break;
        }
        if (mid < 0 && before == false) {
          d = null;
          break;
        }

        if (mid < 0 || mid > length - 1) {
          d = null;
          break;
        }
        d = mSecToEntry[mid];
      }
    }

    if (d == null) {
      mCal.setTimeInMillis(seconds * DateMap.SECOND_MS);
      mCal.set(Calendar.DAY_OF_MONTH, 1);
      mCal.set(Calendar.HOUR_OF_DAY, 0);
      mCal.set(Calendar.MINUTE, 0);
      mCal.set(Calendar.SECOND, 0);
      mCal.set(Calendar.MILLISECOND, 0);
      d = new DateMapCacheEntry(mCal.get(Calendar.YEAR), mCal
          .get(Calendar.MONTH), mCal.get(Calendar.DAY_OF_WEEK), (int) (mCal
          .getTimeInMillis() / DateMap.SECOND_MS));
    }

    return d;
  }

  public int secondsOfPeriodStart(int seconds, int period) {
    int secs;
    int nPeriods = 0;
    DateMapCacheEntry entry;

    entry = getEntry(seconds, true);
    secs = entry.mSeconds;

    if (entry.mSeconds == seconds || period == DateMap.MONTH_SECS)
      return secs;

    if (period < DateMap.WEEK_SECS) {
      secs += period * ((seconds - secs) / period);
    } else if (period == DateMap.WEEK_SECS) {
      // get the DOW of the first day of the month, and subtract out the
      // seconds from then until the start of the week from the last month:
      secs = (int) ((entry.mSeconds) - (DateMap.DAY_SECS * (entry.mDOW - 1)));
      if (seconds - period >= entry.mSeconds) {
        // start of week is in the same month,
        // advance until we get to the week before this one:
        secs += period * ((seconds - secs) / period);
      }
    } else if (period == DateMap.QUARTER_SECS) {
      while (!(entry.mMonth == 0 || entry.mMonth == 3 || entry.mMonth == 6 || entry.mMonth == 9)) {
        entry = getEntry(entry.mSeconds - 1, true);
      }
      secs = entry.mSeconds;
    } else { // YEAR_MS / SECOND_MS
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

    if (period < DateMap.MONTH_SECS) {
      secs = secs + period - 1;
    } else if (period == DateMap.MONTH_SECS) {
      entry = getEntry(seconds + 1, false);
      secs = entry.mSeconds - 1;
    } else if (period == DateMap.QUARTER_SECS) {
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

  public int secondsToNextPeriod(int timestamp, int period) {
    return secondsOfPeriodEnd(timestamp, period) - timestamp + 1;
  }

  public int calculateDisplayPeriod(int start, int stop) {
    int range = Math.abs(stop - start);

    if (range > (long) DateMap.YEAR_SECS * 3) {
      return DateMap.YEAR_SECS;
    } else if (range > (long) DateMap.QUARTER_SECS * 6) {
      return DateMap.QUARTER_SECS;
    } else if (range > (long) DateMap.MONTH_SECS * 6) {
      return DateMap.MONTH_SECS;
    } else if (range > (long) DateMap.WEEK_SECS * 4) {
      return DateMap.WEEK_SECS;
    } else if (range > (long) DateMap.DAY_SECS * 5) {
      return DateMap.DAY_SECS;
    } else if (range > (long) DateMap.HOUR_SECS * 5) {
      return DateMap.HOUR_SECS;
    } else {
      return DateMap.MINUTE_SECS;
    }
  }

  public boolean getDateItem(int seconds, DateItem item) {
    int delta1, delta2;
    int minute, hour, day, month, year, dow;

    if (item == null)
      return false;

    DateMapCacheEntry entry = getEntry(seconds, true);
    if (entry == null)
      return false;

    item.mYear = entry.mYear;
    item.mMonth = entry.mMonth;
    delta1 = seconds - entry.mSeconds;
    item.mDay = (delta1 / DateMap.DAY_SECS) + 1;
    item.mDOW = ((entry.mDOW + item.mDay - 1) % 7) + 1;
    delta2 = (item.mDay - 1) * DateMap.DAY_SECS;
    item.mHour = (delta1 - delta2) / DateMap.HOUR_SECS;
    delta2 -= (item.mHour * DateMap.HOUR_SECS);
    item.mMinute = (delta1 -  delta2) / DateMap.HOUR_SECS;
    delta2 -= (item.mMinute * DateMap.MINUTE_SECS);
    item.mSecond = (delta1 - delta2) / DateMap.HOUR_SECS;
    item.mEpochSeconds = seconds;
    
    return true;
  }

  public int getEpochSeconds(DateItem item) {
    mCal.set(Calendar.YEAR, item.mYear);
    mCal.set(Calendar.MONTH, item.mMonth);
    mCal.set(Calendar.DAY_OF_MONTH, item.mDay);
    mCal.set(Calendar.HOUR_OF_DAY, item.mHour);
    mCal.set(Calendar.MINUTE, item.mMinute);
    mCal.set(Calendar.SECOND, item.mSecond);
    mCal.set(Calendar.MILLISECOND, 0);
    item.mEpochSeconds = (int) (mCal.getTimeInMillis() / DateMap.SECOND_MS);
    return item.mEpochSeconds;
  }
  
  public void populateCache(Context ctx) {
    DateMapCacheEntry entry = null;

    Uri datemap = TimeSeriesData.DateMap.CONTENT_URI;
    Cursor c = ctx.getContentResolver().query(datemap, null, null, null,
        DateMap.DEFAULT_SORT_ORDER);
    if (c.moveToFirst()) {
      int count = c.getCount();
      mSecToEntry = new DateMapCacheEntry[count];
      for (int i = 0; i < count; i++) {
        int year = TimeSeriesData.DateMap.getYear(c);
        int month = TimeSeriesData.DateMap.getMonth(c);
        int dow = TimeSeriesData.DateMap.getDOW(c);
        int secs = TimeSeriesData.DateMap.getSeconds(c);

        entry = new DateMapCacheEntry(year, month, dow, secs);
        insertEntry(entry, i);
        c.moveToNext();
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
    Cursor c = qb.query(db, null, null, null, null, null,
        DateMap.DEFAULT_SORT_ORDER, null);
    if (c.moveToFirst()) {
      int count = c.getCount();
      mSecToEntry = new DateMapCacheEntry[count];
      for (int i = 0; i < count; i++) {
        int year = TimeSeriesData.DateMap.getYear(c);
        int month = TimeSeriesData.DateMap.getMonth(c);
        int dow = TimeSeriesData.DateMap.getDOW(c);
        int secs = TimeSeriesData.DateMap.getSeconds(c);

        entry = new DateMapCacheEntry(year, month, dow, secs);
        insertEntry(entry, i);
        c.moveToNext();
      }
    }
    c.close();

    return;
  }

  private void insertEntry(DateMapCacheEntry dmce, int pos) {
    ArrayList<DateMapCacheEntry> months = null;
    if (dmce == null)
      return;

    Integer year = Integer.valueOf(dmce.mYear);
    try {
      months = mYearToEntry.get(year);
    } catch (Exception e) {
    }

    if (months == null) {
      months = new ArrayList<DateMapCacheEntry>(12);
      mYearToEntry.put(year, months);
    }
    months.add(dmce.mMonth, dmce);
    mSecToEntry[pos] = dmce;

    return;
  }
}
