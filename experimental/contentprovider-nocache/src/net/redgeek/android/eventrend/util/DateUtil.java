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

package net.redgeek.android.eventrend.util;

import net.redgeek.android.eventrecorder.TimeSeriesData.DateMap;

import java.util.Calendar;

/**
 * Encapsulates a truck-load of commonly used date functions.
 * 
 * @author barclay
 */
public final class DateUtil {
  /**
   * Encapsulation of a date broken down by both milliseconds since epoch (as
   * defined by the system), and year, month, day, hour, minute, and second. The
   * reason for storing both is essentially to cache information and glue the
   * variable together into a single item. Purely convenience.
   * 
   * @author barclay
   */
  public static class DateItem {
    public int mYear = 0;
    public int mMonth = 0;
    public int mDay = 0;
    public int mHour = 0;
    public int mMinute = 0;
    public int mSecond = 0;
    public long mMillis = 0;

    /**
     * Set all the fields of the DateItem to the date/time represented by the
     * current value of the Calendar passed in.
     * 
     * @param c
     *          The Calendar that's the source data.
     */
    public void setTo(Calendar c) {
      mYear = c.get(Calendar.YEAR);
      mMonth = c.get(Calendar.MONTH);
      mDay = c.get(Calendar.DAY_OF_MONTH);
      mHour = c.get(Calendar.HOUR_OF_DAY);
      mMinute = c.get(Calendar.MINUTE);
      mSecond = c.get(Calendar.SECOND);
      mMillis = c.getTimeInMillis();
    }

    /**
     * Compares all the fields of the DateItem to another DateItem. All fields
     * are compared, instead of just the millisecond field, in the event that
     * all the fields are not in sync for some reason.
     * 
     * @param other
     * @return true if the two DateItems are equal in all fields, else false.
     */
    public boolean isEqual(DateItem other) {
      if (this.mYear == other.mYear && this.mMonth == other.mMonth
          && this.mDay == other.mDay && this.mHour == other.mHour
          && this.mMinute == other.mMinute && this.mSecond == other.mSecond
          && this.mMillis == other.mMillis)
        return true;
      return false;
    }
  }

  /**
   * Returns a description of the seconds, scaled to the largest unit and
   * rounded to the default number of decimal places, with the associated label
   * (e.g., "years", "weeks", etc.)
   * 
   * @param millis
   *          The milliseconds since epoch to format.
   * @return The descriptive string.
   */
  public static String toString(double seconds) {
    if (seconds > DateMap.YEAR_SECS / DateMap.SECOND_MS) {
      return Number.Round(seconds / DateMap.YEAR_SECS) + " yr";
    } else if (seconds > DateMap.QUARTER_SECS/ DateMap.SECOND_MS) {
      return Number.Round(seconds / DateMap.QUARTER_SECS) + " qtr";
    } else if (seconds > DateMap.MONTH_SECS/ DateMap.SECOND_MS) {
      return Number.Round(seconds / DateMap.MONTH_SECS) + " mo";
    } else if (seconds > DateMap.WEEK_SECS/ DateMap.SECOND_MS) {
      return Number.Round(seconds / DateMap.WEEK_SECS) + " wk";
    } else if (seconds > DateMap.DAY_SECS/ DateMap.SECOND_MS) {
      return Number.Round(seconds / DateMap.DAY_SECS) + " day";
    } else if (seconds > DateMap.HOUR_SECS/ DateMap.SECOND_MS) {
      return Number.Round(seconds / DateMap.HOUR_SECS) + " hr";
    } else if (seconds > DateMap.MINUTE_SECS/ DateMap.SECOND_MS) {
      return Number.Round(seconds / DateMap.MINUTE_SECS) + " min";
    } else { // if (millis > SECOND_MS) {
      return Number.Round(seconds) + " sec";
    }
  }

  /**
   * Returns the "timestamp" string representation of the time in milliseconds:
   * yyyy/mm/dd HH:MM:SS
   * 
   * @param seconds
   *          The seconds since epoch to format.
   * @return The timestamp string.
   */
  public static String toTimestamp(int seconds) {
    Calendar c = Calendar.getInstance();
    c.setTimeInMillis(seconds * DateMap.SECOND_MS);
    return DateUtil.toTimestamp(c);
  }

  /**
   * Returns the "short timestamp" string representation of the time in
   * milliseconds: HH:MM:SS
   * 
   * @param millis
   *          The milliseconds since epoch to format.
   * @return The short timestamp string.
   */
  public static String toShortTimestamp(long millis) {
    Calendar c = Calendar.getInstance();
    c.setTimeInMillis(millis);
    return DateUtil.toShortTimestamp(c);
  }

  /**
   * Utility routine for padding zeros on the left side of an integer out to two
   * digits, since string concatenations this small are much more efficient that
   * using String.format("%02d",foo).
   * 
   * @param i
   *          The integer to format.
   * @return A zero-padded string representation of the integer.
   */
  private static String l2pad(int i) {
    if (i < 10)
      return "0" + i;
    return "" + i;
  }

  /**
   * Returns a "timestamp" formated string representing the time:
   * "yyyy/mm/dd HH:MM:SS"
   * 
   * @param d
   *          The DateItem to format.
   * @return The timestamp string.
   */
  public static String toTimestamp(DateItem d) {
    return d.mYear + "/" + l2pad(d.mMonth + 1) + "/" + l2pad(d.mDay) + " "
        + l2pad(d.mHour) + ":" + l2pad(d.mMinute) + ":" + l2pad(d.mSecond);
  }

  /**
   * Returns a "timestamp" formated string representing the time:
   * "yyyy/mm/dd HH:MM:SS"
   * 
   * @param cal
   *          The Calendar to format.
   * @return The timestamp string.
   */
  public static String toTimestamp(Calendar cal) {
    return cal.get(Calendar.YEAR) + "/" + l2pad(cal.get(Calendar.MONTH) + 1)
        + "/" + l2pad(cal.get(Calendar.DAY_OF_MONTH)) + " "
        + l2pad(cal.get(Calendar.HOUR_OF_DAY)) + ":"
        + l2pad(cal.get(Calendar.MINUTE)) + ":"
        + l2pad(cal.get(Calendar.SECOND));
  }

  /**
   * Returns a "short timestamp" formated string representing the time:
   * "HH:MM:SS"
   * 
   * @param cal
   *          The Calendar to format.
   * @return The timestamp string.
   */
  public static String toShortTimestamp(Calendar cal) {
    return l2pad(cal.get(Calendar.HOUR_OF_DAY)) + ":"
        + l2pad(cal.get(Calendar.MINUTE)) + ":"
        + l2pad(cal.get(Calendar.SECOND));
  }

  /**
   * Returns a (generally) filesystem-safe formated string representing the
   * time: "yyyy-mm-dd_HH.MM.SS"
   * 
   * @param cal
   *          The Calendar to format.
   * @return The timestamp string.
   */
  public static String toFSTimestamp(Calendar cal) {
    return cal.get(Calendar.YEAR) + "-" + l2pad(cal.get(Calendar.MONTH) + 1)
        + "-" + l2pad(cal.get(Calendar.DAY_OF_MONTH)) + "_"
        + l2pad(cal.get(Calendar.HOUR_OF_DAY)) + "."
        + l2pad(cal.get(Calendar.MINUTE)) + "."
        + l2pad(cal.get(Calendar.SECOND));
  }

}
