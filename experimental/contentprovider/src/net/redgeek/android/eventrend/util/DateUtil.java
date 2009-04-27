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

import net.redgeek.android.eventrecorder.DateMapCache;

import java.util.Calendar;

/**
 * Encapsulates a truck-load of commonly used date functions.
 * 
 * @author barclay
 */
public final class DateUtil {
  private Calendar mCal;
  private DateUtil.Period mSpan;
  private int mSpanOffset = 0;
  private boolean mUnitChange = false;

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

  private DateItem mBase;
  private DateItem mCursor;

  /**
   * Code shoulder reference these Periods when refering to spans of time
   * instead of Calendar.*, as Calendar doesn't support the notion of a strict
   * QUARTER, and we use WEEK slightly differently.
   * 
   * @author barclay
   */
  public enum Period {
    MINUTE, HOUR, AMPM, DAY, WEEK, MONTH, QUARTER, YEAR
  }

  public Period[] PERIODS = { Period.MINUTE, Period.HOUR, Period.AMPM,
      Period.DAY, Period.WEEK, Period.MONTH, Period.QUARTER, Period.YEAR };

  public static final String[] MONTHS = { "Jan", "Feb", "Mar", "Apr", "May",
      "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };

  public static final String[] DAYS = { "Sun", "M", "Tu", "W", "Th", "F",
      "Sat", };

  public static final String[] QUARTERS = { "Q1", "Q2", "Q3", "Q4", };

  /**
   * Constructor. Instantiates a Calendar member variable to prevent having to
   * continually fetch an instance, which is moderately expensive, and create
   * cursor used to walk a timeline.
   */
  public DateUtil() {
    mCal = Calendar.getInstance();
    mBase = new DateItem();
    mCursor = new DateItem();
  }

  /**
   * Returns the internal Calendar object in it's current state.
   * 
   * @return The Calendar.
   */
  public Calendar getCalendar() {
    return mCal;
  }

  /**
   * Used when going to stride along a timeline. This sets the start time of the
   * walk.
   * 
   * @param ms
   *          The start time in milliseconds since epoch.
   */
  public void setBaseTime(long ms) {
    mBase.mMillis = ms;
    millisToComponent(mBase);
    copyDate(mBase, mCursor);
  }

  /**
   * Returns the milliseconds until the next Period, as based on the difference
   * between the current cursor and the Period. If the current cursor is at the
   * start of a Period, ignoring milliseconds, 0 is returned.
   * 
   * @param u
   * @return milliseconds until next period.
   */
  public long msToNextPeriod(Period u) {
    long ms = 0;

    switch (u) {
      case YEAR:
        mCal.set(mCursor.mYear + 1, 0, 0, 0, 0, 0);
        ms = mCal.getTimeInMillis() - mCursor.mMillis;
        if (ms == YEAR_MS)
          ms = 0;
        break;
      case QUARTER:
        if (mCursor.mMonth >= 9)
          mCal.set(mCursor.mYear + 1, 0, 0, 0, 0, 0);
        else if (mCursor.mMonth >= 6)
          mCal.set(mCursor.mYear, 9, 0, 0, 0, 0);
        else if (mCursor.mMonth >= 3)
          mCal.set(mCursor.mYear, 6, 0, 0, 0, 0);
        else
          mCal.set(mCursor.mYear, 3, 0, 0, 0, 0);
        ms = mCal.getTimeInMillis() - mCursor.mMillis;
        if (ms == QUARTER_MS)
          ms = 0;
        break;
      case MONTH:
        if (mCursor.mMonth == 11)
          mCal.set(mCursor.mYear + 1, 0, 0, 0, 0, 0);
        else
          mCal.set(mCursor.mYear, mCursor.mMonth + 1, 0, 0, 0, 0);
        ms = mCal.getTimeInMillis() - mCursor.mMillis;
        if (ms == MONTH_MS)
          ms = 0;
        break;
      case WEEK:
        mCal.setTimeInMillis(mCursor.mMillis);

        int first = mCal.getFirstDayOfWeek();

        mCal.add(Calendar.WEEK_OF_YEAR, 1);
        mCal.set(Calendar.DAY_OF_WEEK, first);
        mCal.set(Calendar.HOUR_OF_DAY, 0);
        mCal.set(Calendar.MINUTE, 0);
        mCal.set(Calendar.SECOND, 0);
        mCal.set(Calendar.MILLISECOND, 0);
        ms = mCal.getTimeInMillis() - mCursor.mMillis;
        if (ms == WEEK_MS)
          ms = 0;
        break;
      case DAY:
        if (mCursor.mMinute == 0 && mCursor.mHour == 0)
          return 0;
        ms = ((60 - mCursor.mMinute) + (60 * (24 - mCursor.mHour))) * MINUTE_MS;
        break;
      case AMPM:
        if (mCursor.mMinute == 0 && (mCursor.mHour == 0 || mCursor.mHour == 12))
          return 0;
        ms = ((60 - mCursor.mMinute) + (60 * (24 - (mCursor.mHour % 12))))
            * MINUTE_MS;
        break;
      case HOUR:
        if (mCursor.mMinute == 0)
          return 0;
        ms = (60 - mCursor.mMinute) * MINUTE_MS;
        break;
      case MINUTE:
      default:
        if (mCursor.mSecond == 0)
          return 0;
        ms = (60 - mCursor.mSecond) * SECOND_MS;
        break;
    }

    return ms;
  }

  /**
   * Sets an offset of the internal marker recording the class of time spanned
   * (as a Period). This offset is indexes into the Period enum, e.g., if the
   * span is calculated to be Period.YEAR with an offset of 0, then it will be
   * calculated to be Period.MONTH with an offset of -2.
   * 
   * @param milliStart
   *          The milliseconds since epoch in start time, inclusive.
   * @param milliEnd
   *          The milliseconds since epoch in end time, inclusive.
   */
  public void setSpanOffset(int offset) {
    mSpanOffset = offset;
  }

  /**
   * Sets the internal marker recording the class of time spanned (as a Period)
   * for the range of time specified. This is used to determine how to generate
   * labels while striding through time. If milliStart == milliEnd, the span
   * will be set to the smallest known span.
   * 
   * @param milliStart
   *          The milliseconds since epoch in start time, inclusive.
   * @param milliEnd
   *          The milliseconds since epoch in end time, inclusive.
   */
  public void setSpan(long milliStart, long milliEnd) {
    int index = 0;
    long range = milliEnd - milliStart;
    if (range == 0)
      range = 1;
    if (range < 0)
      range = -range;

    if (range > (long) (DateUtil.YEAR_MS * 3)) {
      index = DateUtil.Period.YEAR.ordinal();
    } else if (range > (long) (DateUtil.QUARTER_MS * 6)) {
      index = DateUtil.Period.QUARTER.ordinal();
    } else if (range > (long) (DateUtil.MONTH_MS * 6)) {
      index = DateUtil.Period.MONTH.ordinal();
    } else if (range > (long) (DateUtil.WEEK_MS * 4)) {
      index = DateUtil.Period.WEEK.ordinal();
    } else if (range > (long) (DateUtil.DAY_MS * 5)) {
      index = DateUtil.Period.DAY.ordinal();
    } else if (range > (long) (DateUtil.HOUR_MS * 24)) {
      index = DateUtil.Period.AMPM.ordinal();
    } else if (range > (long) (DateUtil.HOUR_MS * 5)) {
      index = DateUtil.Period.HOUR.ordinal();
    } else {
      index = DateUtil.Period.MINUTE.ordinal();
    }

    index += mSpanOffset;
    if (index < 0)
      index = 0;
    else if (index >= PERIODS.length)
      index = PERIODS.length - 1;

    mSpan = PERIODS[index];
    return;
  }

  /**
   * Returns the span calculated by {@link #setSpan(long, long)}
   * 
   * @return The span as a DateUtil.Period
   * @see DateUtil#Period
   */
  public DateUtil.Period getSpan() {
    return mSpan;
  }

  /**
   * Returns the selected Calendar.* field of the time under the current cursor
   * when striding.
   * 
   * @param p
   *          The Period in which to format the output.
   * @return The field datum.
   */
  public int get(int field) {
    return mCal.get(field);
  }

  /**
   * Returns an array of two strings yielding a textual representation of the
   * time under the current cursor when striding. Neither string will be null,
   * but either may be the empty ("") string. Typically, the second string will
   * be empty rather than the first, and will contain additional information
   * about the label, such as the the month when the days roll over into the
   * next month, or the day of the week. This method sets an internal marker
   * recording if current label has rolled past a period boundary, such as from
   * one week to the next or one year to the next, which is queryable via
   * {@link #isUnitChanged()}
   * 
   * @param p
   *          The Period in which to format the output.
   * @return String[2], containing two description strings of the date/time. The
   *         first string will be withing the Period <code>p</code>, and the
   *         second is typically auxiliary information.
   */
  public String[] getLabel(Period p) {
    String[] strings = new String[2];
    int minute;
    int hour;
    int day;
    int month;
    int year;
    int dow;

    mUnitChange = false;

    switch (p) {
      case YEAR:
        strings[0] = "" + mCal.get(Calendar.YEAR);
        strings[1] = "";
        break;
      case QUARTER:
        year = mCal.get(Calendar.YEAR);
        month = mCal.get(Calendar.MONTH);
        if (month >= 9)
          strings[0] = QUARTERS[3];
        else if (month >= 6)
          strings[0] = QUARTERS[2];
        else if (month >= 3)
          strings[0] = QUARTERS[1];
        else
          strings[0] = QUARTERS[0];
        strings[1] = "";
        if (year != mBase.mYear) {
          strings[1] = "" + mCal.get(Calendar.YEAR);
          mUnitChange = true;
        }
        break;
      case MONTH:
        year = mCal.get(Calendar.YEAR);
        month = mCal.get(Calendar.MONTH);
        strings[0] = MONTHS[month];
        if (year != mBase.mYear) {
          strings[1] = "" + mCal.get(Calendar.YEAR);
          mUnitChange = true;
        } else {
          strings[1] = "";
        }
        break;
      case WEEK:
      case DAY:
        month = mCal.get(Calendar.MONTH);
        day = mCal.get(Calendar.DAY_OF_MONTH);
        strings[0] = "" + day;
        if (month != mBase.mMonth) {
          strings[1] = MONTHS[month];
          mUnitChange = true;
        } else {
          dow = mCal.get(Calendar.DAY_OF_WEEK);
          strings[1] = DAYS[dow - 1];
          if (dow == 1)
            mUnitChange = true;
        }
        break;
      case AMPM:
      case HOUR:
        day = mCal.get(Calendar.DAY_OF_MONTH);
        hour = mCal.get(Calendar.HOUR_OF_DAY);
        if (hour == 0) {
          strings[0] = "12a";
          strings[1] = "midnight";
        } else if (hour == 12) {
          strings[0] = "12p";
          strings[1] = "noon";
        } else if (hour > 11) {
          strings[0] = (hour - 12) + "p";
          strings[1] = "";
        } else {
          strings[0] = hour + "a";
          strings[1] = "";
        }

        if (day != mBase.mDay) {
          dow = mCal.get(Calendar.DAY_OF_WEEK);
          strings[0] = mCal.get(Calendar.MONTH) + 1 + "/" + day;
          strings[1] = DAYS[dow - 1];
          mUnitChange = true;
        }
        break;
      case MINUTE:
      default:
        minute = mCal.get(Calendar.MINUTE);
        hour = mCal.get(Calendar.HOUR_OF_DAY);
        strings[0] = l2pad(minute);
        strings[1] = "";
        if (hour != mBase.mHour) {
          if (hour == 0) {
            day = mCal.get(Calendar.DAY_OF_MONTH);
            dow = mCal.get(Calendar.DAY_OF_WEEK);
            strings[0] = mCal.get(Calendar.MONTH) + 1 + "/" + day;
            strings[1] = DAYS[dow - 1];
          } else if (hour == 12) {
            strings[0] = "12";
            strings[1] = "noon";
          } else if (hour > 11) {
            strings[0] = (hour - 12) + "p";
          } else {
            strings[0] = hour + "a";
          }
          mUnitChange = true;
        } else
          break;
    }

    return strings;
  }

  /**
   * Advances the internal cursor <code>milliseconds</code> in time.
   * 
   * @param milliseconds
   *          The number of milliseconds to advance.
   */
  public void advanceInMs(long milliseconds) {
    copyDate(mCursor, mBase);
    mCal.setTimeInMillis(mCursor.mMillis);
    mCal.add(Calendar.MILLISECOND, (int) milliseconds);
    mCursor.mMillis = mCal.getTimeInMillis();
  }

  /**
   * Advances the internal cursor <code>step</code> units of Period
   * <code>p</code> in time. Note that for MONTH and QUARTER, this works out to
   * 1 and 3 months respectively, as defined by the Calendar class and based on
   * the current cursor, not precisely MONTH_MS or QUARTER_MS milliseconds.
   * 
   * @param p
   *          The DateUtil.Period unit.
   * @param step
   *          The number of Period units to advance.
   */
  public void advance(Period p, int step) {
    copyDate(mCursor, mBase);

    switch (p) {
      case YEAR:
        mCal.setTimeInMillis(mCursor.mMillis);
        mCal.add(Calendar.YEAR, step);
        break;
      case QUARTER:
        mCal.setTimeInMillis(mCursor.mMillis);
        mCal.add(Calendar.MONTH, step * 3);
        break;
      case MONTH:
        mCal.setTimeInMillis(mCursor.mMillis);
        mCal.add(Calendar.MONTH, step);
        break;
      case WEEK:
        mCal.setTimeInMillis(mCursor.mMillis);
        mCal.add(Calendar.WEEK_OF_YEAR, step);
        break;
      case DAY:
        mCal.setTimeInMillis(mCursor.mMillis);
        mCal.add(Calendar.DAY_OF_MONTH, step);
        break;
      case HOUR:
        mCal.setTimeInMillis(mCursor.mMillis);
        mCal.add(Calendar.HOUR_OF_DAY, step);
        break;
      case MINUTE:
      default:
        mCal.setTimeInMillis(mCursor.mMillis);
        mCal.add(Calendar.MINUTE, step);
        break;
    }

    mCursor.mMillis = mCal.getTimeInMillis();
    millisToComponent(mCursor);

    return;
  }

  /**
   * Return whether or not the last getLabel() noted a rollover from one period
   * to another, as determine by the Period passed to getLabel().
   * 
   * @return boolean
   * @see #getLabel(Period)
   */
  public boolean isUnitChanged() {
    return mUnitChange;
  }

  /**
   * Returns the average number of milliseconds in a Period. These are constant.
   * 
   * @param u
   * @return the number of millseconds
   * @see #YEAR_MS
   * @see #QUARTER_MS
   * @see #MONTH_MS
   * @see #DAY_MS
   * @see #HOUR_MS
   * @see #MINUTE_MS
   */
  public long msInPeriod(Period u) {
    long ms = 0;
    switch (u) {
      case YEAR:
        ms = YEAR_MS;
        break;
      case QUARTER:
        ms = QUARTER_MS;
        break;
      case MONTH:
        ms = MONTH_MS;
        break;
      case WEEK:
        ms = WEEK_MS;
        break;
      case DAY:
        ms = DAY_MS;
        break;
      case AMPM:
        ms = AMPM_MS;
        break;
      case HOUR:
        ms = HOUR_MS;
        break;
      case MINUTE:
      default:
        ms = MINUTE_MS;
        break;
    }

    return ms;
  }

  /**
   * Some external entities still use Calendar.* fields to do some of their own
   * date calculations, so this provides a mapping from DateUtil.*_MS to the
   * closest Calendar.* field. Note that if the milliseconds is not one of the
   * DateUtil constants, the smallest known field will be returned.
   * 
   * @param millis
   *          The DateUtil.*_MS field to map from.
   * @return The int representing the closest Calendar.* field.
   */
  public static int mapLongToCal(long millis) {
    if (millis == YEAR_MS)
      return Calendar.YEAR;
    else if (millis == QUARTER_MS)
      return Calendar.MONTH; // There is no Calendar.QUARTER, return MONTH
    else if (millis == MONTH_MS)
      return Calendar.MONTH;
    else if (millis == WEEK_MS)
      return Calendar.WEEK_OF_YEAR;
    else if (millis == DAY_MS)
      return Calendar.DAY_OF_MONTH;
    else if (millis == AMPM_MS)
      return Calendar.AM_PM;
    else if (millis == HOUR_MS)
      return Calendar.HOUR_OF_DAY;
    return Calendar.MINUTE;
  }

  /**
   * Provide a mapping from number of millisecond (DateUtil.*_MS) to a
   * DateUtil.Period. Note that if the milliseconds is not one of the DateUtil
   * constants, the smallest known field will be returned.
   * 
   * @param millis
   *          The DateUtil.*_MS field to map from.
   * @return The Period enum representing the associated DateUtil.Period.
   */
  public static Period mapLongToPeriod(long millis) {
    if (millis == YEAR_MS)
      return Period.YEAR;
    else if (millis == QUARTER_MS)
      return Period.QUARTER;
    else if (millis == MONTH_MS)
      return Period.MONTH;
    else if (millis == WEEK_MS)
      return Period.WEEK;
    else if (millis == DAY_MS)
      return Period.DAY;
    else if (millis == AMPM_MS)
      return Period.AMPM;
    else if (millis == HOUR_MS)
      return Period.HOUR;
    return Period.MINUTE;
  }

  /**
   * Provide a mapping from a Period to the number of millisecond
   * (DateUtil.*_MS)
   * 
   * @param The
   *          Period enum representing the associated DateUtil.Period.
   * @return A String describing the period..
   */
  public static String mapPeriodToString(Period p) {
    if (p == Period.YEAR)
      return "year";
    if (p == Period.QUARTER)
      return "quarter";
    if (p == Period.MONTH)
      return "month";
    if (p == Period.WEEK)
      return "week";
    if (p == Period.DAY)
      return "day";
    if (p == Period.AMPM)
      return "am/pm";
    if (p == Period.HOUR)
      return "hour";
    return "minute";
  }

  /**
   * Provide a mapping from string to a Period.
   * 
   * @param s
   *          The string to map from. Case insensitive.
   * @return The associated DateUtil.Period
   */
  public static Period mapStringToPeriod(String s) {
    if (s.toLowerCase().equals("year"))
      return Period.YEAR;
    if (s.toLowerCase().equals("quarter"))
      return Period.QUARTER;
    if (s.toLowerCase().equals("month"))
      return Period.MONTH;
    if (s.toLowerCase().equals("week"))
      return Period.WEEK;
    if (s.toLowerCase().equals("day"))
      return Period.DAY;
    if (s.toLowerCase().equals("am/pm"))
      return Period.AMPM;
    if (s.toLowerCase().equals("hour"))
      return Period.HOUR;
    return Period.MINUTE;
  }

  /**
   * Provide a mapping from a Period to the number of millisecond
   * (DateUtil.*_MS)
   * 
   * @param millis
   *          The DateUtil.*_MS field to map from.
   * @param The
   *          Period enum representing the associated DateUtil.Period.
   * @return the DateUtil.*_MS constant representing the number of milliseconds
   *         in the period.
   */
  public static long mapPeriodToLong(Period p) {
    if (p == Period.YEAR)
      return YEAR_MS;
    if (p == Period.QUARTER)
      return QUARTER_MS;
    if (p == Period.MONTH)
      return MONTH_MS;
    if (p == Period.WEEK)
      return WEEK_MS;
    if (p == Period.DAY)
      return DAY_MS;
    if (p == Period.AMPM)
      return AMPM_MS;
    if (p == Period.HOUR)
      return HOUR_MS;
    return MINUTE_MS;
  }

  /**
   * Returns a description of the milliseconds, scaled to the largest unit and
   * rounded to the default number of decimal places, with the associated label
   * (e.g., "years", "weeks", etc.)
   * 
   * @param millis
   *          The milliseconds since epoch to format.
   * @return The descriptive string.
   */
  public static String toString(float seconds) {
    if (seconds > YEAR_MS / SECOND_MS) {
      return Number.Round(seconds / (YEAR_MS / SECOND_MS)) + " yr";
    } else if (seconds > QUARTER_MS/ SECOND_MS) {
      return Number.Round(seconds / (QUARTER_MS / SECOND_MS)) + " qtr";
    } else if (seconds > MONTH_MS/ SECOND_MS) {
      return Number.Round(seconds / (MONTH_MS / SECOND_MS)) + " mo";
    } else if (seconds > WEEK_MS/ SECOND_MS) {
      return Number.Round(seconds / (WEEK_MS / SECOND_MS)) + " wk";
    } else if (seconds > DAY_MS/ SECOND_MS) {
      return Number.Round(seconds / (DAY_MS / SECOND_MS)) + " day";
    } else if (seconds > HOUR_MS/ SECOND_MS) {
      return Number.Round(seconds / (HOUR_MS / SECOND_MS)) + " hr";
    } else if (seconds > MINUTE_MS/ SECOND_MS) {
      return Number.Round(seconds / (MINUTE_MS / SECOND_MS)) + " min";
    } else { // if (millis > SECOND_MS) {
      return Number.Round(seconds) + " sec";
    }
  }

  /**
   * Returns a description of the square root of the milliseconds, scaled to the
   * largest unit and rounded to the default number of decimal places, with the
   * associated label (e.g., "years", "weeks", etc.). Note this is only used for
   * displaying the variance, as the variance the a squared value, so this tests
   * (millis > (unit^2)) ? and displays the value (millis/(unit^2)). Otherwise
   * it is identical to {@link #toString(float)}.
   * 
   * @param millis
   *          The (squared) milliseconds since epoch to format.
   * @return The descriptive string.
   */
  public static String toStringSquared(float millis) {
    if (millis > (float) YEAR_MS * (float) YEAR_MS) {
      return Number.Round(millis / ((float) YEAR_MS * (float) YEAR_MS))
          + " years";
    } else if (millis > (float) QUARTER_MS * (float) QUARTER_MS) {
      return Number.Round(millis / ((float) QUARTER_MS * (float) QUARTER_MS))
          + " quarters";
    } else if (millis > (float) MONTH_MS * (float) MONTH_MS) {
      return Number.Round(millis / ((float) MONTH_MS * (float) MONTH_MS))
          + " months";
    } else if (millis > (float) WEEK_MS * (float) WEEK_MS) {
      return Number.Round(millis / ((float) WEEK_MS * (float) WEEK_MS))
          + " weeks";
    } else if (millis > (float) DAY_MS * (float) DAY_MS) {
      return Number.Round(millis / ((float) DAY_MS * (float) DAY_MS)) + " days";
    } else if (millis > (float) HOUR_MS * (float) HOUR_MS) {
      return Number.Round(millis / ((float) HOUR_MS * (float) HOUR_MS))
          + " hours";
    } else { // if (millis > MINUTE_MS) {
      return Number.Round(millis / ((float) MINUTE_MS * (float) MINUTE_MS))
          + " minutes";
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
    c.setTimeInMillis(seconds * DateMapCache.SECOND_MS);
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
   * @param d
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
   * Returns a "timestamp" formated string representing the time formatted for
   * the period.
   * 
   * @param d
   *          The Calendar to format.
   * @return The timestamp string.
   */
  public static String toDisplayTime(int seconds, int period) {
    String s;
    Calendar c = Calendar.getInstance();
    c.setTimeInMillis(seconds * DateMapCache.SECOND_MS);
    if (period == DateMapCache.YEAR_MS / DateMapCache.SECOND_MS) {
      s = "" + c.get(Calendar.YEAR);
    }
    else if (period == DateMapCache.QUARTER_MS / DateMapCache.SECOND_MS) {
      int month = c.get(Calendar.MONTH);
      int quarter = (int)(month / 3) + 1;
      s = c.get(Calendar.YEAR) + " Q" + quarter;
    }
    else if (period == DateMapCache.MONTH_MS / DateMapCache.SECOND_MS) {
      s = c.get(Calendar.YEAR) + "/" + l2pad(c.get(Calendar.MONTH));
    }
    else if (period == DateMapCache.WEEK_MS / DateMapCache.SECOND_MS) {
      s = c.get(Calendar.YEAR) + "/" 
        +l2pad(c.get(Calendar.MONTH)) + "/" 
        + l2pad(c.get(Calendar.DAY_OF_MONTH));
    }
    else if (period == DateMapCache.DAY_MS / DateMapCache.SECOND_MS) {
      s = c.get(Calendar.YEAR) + "/" 
        + l2pad(c.get(Calendar.MONTH))
        + "/" + l2pad(c.get(Calendar.DAY_OF_MONTH));
    }
    else {
      s = c.get(Calendar.YEAR) + "/" 
        + l2pad(c.get(Calendar.MONTH)) + "/" 
        + l2pad(c.get(Calendar.DAY_OF_MONTH)) + " "
        + l2pad(c.get(Calendar.HOUR_OF_DAY)) + ":"
        + l2pad(c.get(Calendar.MINUTE)) + ":"
        + l2pad(c.get(Calendar.SECOND));
    }
    return s;
  }

  /**
   * Returns a "short timestamp" formated string representing the time:
   * "HH:MM:SS"
   * 
   * @param d
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
   * @param d
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

  /**
   * Returns true if the two calendars represent dates that fall in the same
   * year, else false.
   * 
   * @param c1
   *          Calendar one.
   * @param c2
   *          Calendar two.
   * @return boolean.
   */
  public static boolean inSameYear(Calendar c1, Calendar c2) {
    if (c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR))
      return true;
    return false;
  }

  /**
   * Returns true if the two calendars represent dates that fall in the same
   * quarter, else false. A quarter here is defined as the each group of three
   * consecutive months, starting with the month designated as the first month
   * by the Calendar package. Thus, it is not defined as the average number of
   * milliseconds in a quarter, which would be {@link #YEAR_MS}/4.
   * 
   * @param c1
   *          Calendar one.
   * @param c2
   *          Calendar two.
   * @return boolean.
   */
  public static boolean inSameQuarter(Calendar c1, Calendar c2) {
    if (inSameYear(c1, c2)) {
      int m1 = c1.get(Calendar.MONTH);
      int m2 = c2.get(Calendar.MONTH);
      if (m1 >= 9 && m2 >= 9)
        return true;
      if (m1 >= 6 && m1 < 9 && m2 >= 6 && m2 < 9)
        return true;
      if (m1 >= 3 && m1 < 6 && m2 >= 3 && m2 < 6)
        return true;
      if (m1 >= 0 && m1 < 3 && m2 >= 0 && m2 < 3)
        return true;
    }
    return false;
  }

  /**
   * Returns true if the two calendars represent dates that fall in the same
   * month, else false.
   * 
   * @param c1
   *          Calendar one.
   * @param c2
   *          Calendar two.
   * @return boolean.
   */
  public static boolean inSameMonth(Calendar c1, Calendar c2) {
    if (inSameYear(c1, c2)
        && (c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH)))
      return true;
    return false;
  }

  /**
   * Returns true if the two calendars represent dates that fall in the same
   * week, else false. A week here is defined by the Calendar.WEEK_OF_YEAR
   * package. Special provisions have been made to test weeks than may span the
   * end/beginning of a year, and returning true if the two calendars are
   * specifying dates within such a week, despite Calendar.WEEK_OF_YEAR being
   * unequal for the two Calendars.
   * 
   * @param c1
   *          Calendar one.
   * @param c2
   *          Calendar two.
   * @return boolean.
   */
  public static boolean inSameWeek(Calendar c1, Calendar c2) {
    if (inSameYear(c1, c2)
        && (c1.get(Calendar.WEEK_OF_YEAR) == c2.get(Calendar.WEEK_OF_YEAR)))
      return true;

    Calendar tmp;
    if (c1.before(c2)) {
      tmp = c2;
      c2 = c1;
      c1 = tmp;
    }

    int c1week = c1.get(Calendar.WEEK_OF_YEAR);
    int c2week = c1.get(Calendar.WEEK_OF_YEAR);

    if (c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) + 1) {
      if (c1week == c1.getActualMinimum(Calendar.WEEK_OF_YEAR)
          && c2week == c2.getActualMaximum(Calendar.WEEK_OF_YEAR)) {
        tmp = (Calendar) c2.clone();
        tmp.add(Calendar.DAY_OF_YEAR, 7);
        if (tmp.get(Calendar.WEEK_OF_YEAR) > c1week)
          return true;
      }
    }

    return false;
  }

  /**
   * Returns true if the two calendars represent dates that fall in the same
   * day, else false.
   * 
   * @param c1
   *          Calendar one.
   * @param c2
   *          Calendar two.
   * @return boolean.
   */
  public static boolean inSameDay(Calendar c1, Calendar c2) {
    if (inSameYear(c1, c2)
        && (c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)))
      return true;
    return false;
  }

  /**
   * Returns true if the two calendars represent dates that fall in the same
   * morning or evening, as defined by [midnight,noon) and [noon,midnight), else
   * false.
   * 
   * @param c1
   *          Calendar one.
   * @param c2
   *          Calendar two.
   * @return boolean.
   */
  public static boolean inSameAMPM(Calendar c1, Calendar c2) {
    if (inSameDay(c1, c2) && (c1.get(Calendar.AM_PM) == c2.get(Calendar.AM_PM)))
      return true;
    return false;
  }

  /**
   * Returns true if the two calendars represent dates that fall in the same
   * hour, else false.
   * 
   * @param c1
   *          Calendar one.
   * @param c2
   *          Calendar two.
   * @return boolean.
   */
  public static boolean inSameHour(Calendar c1, Calendar c2) {
    if (inSameDay(c1, c2)
        && (c1.get(Calendar.HOUR_OF_DAY) == c2.get(Calendar.HOUR_OF_DAY)))
      return true;
    return false;
  }

  /**
   * Returns true if the two calendars represent dates that fall in the same
   * period, else false.
   * 
   * @param aggregationMillis
   *          The period as specified in milliseconds, e.g., DateUtil.YEAR_MS
   * @param c1
   *          Calendar one.
   * @param c2
   *          Calendar two.
   * @return boolean.
   */
  public static boolean inSamePeriod(Calendar c1, Calendar c2,
      long aggregationMillis) {
    if (aggregationMillis == 0)
      return false;

    if ((aggregationMillis == YEAR_MS && inSameYear(c1, c2))
        || (aggregationMillis == QUARTER_MS && inSameQuarter(c1, c2))
        || (aggregationMillis == MONTH_MS && inSameMonth(c1, c2))
        || (aggregationMillis == WEEK_MS && inSameWeek(c1, c2))
        || (aggregationMillis == DAY_MS && inSameDay(c1, c2))
        || (aggregationMillis == AMPM_MS && inSameAMPM(c1, c2))
        || (aggregationMillis == HOUR_MS && inSameHour(c1, c2))) {
      return true;
    }

    return false;
  }

  /**
   * Sets the date/time of the Calendar object to the beginning of the Period by
   * setting all fields smaller than the specified period to the minimum value.
   * 
   * @param c
   *          The calendar to set.
   * @param p
   *          The DateUtil.Period to set.
   */
  public static void setToPeriodStart(Calendar c, Period p) {
    switch (p) {
      case YEAR:
        c.set(Calendar.MONTH, 0);
      case MONTH:
        c.set(Calendar.DAY_OF_MONTH, 1);
      case DAY:
        c.set(Calendar.HOUR_OF_DAY, 0);
      case HOUR:
        c.set(Calendar.MINUTE, 0);
      case MINUTE:
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        break;
      case WEEK:
        c.set(Calendar.DAY_OF_WEEK, c.getFirstDayOfWeek());
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        break;
      case AMPM:
        if (c.get(Calendar.AM_PM) == Calendar.AM)
          c.set(Calendar.HOUR_OF_DAY, 0);
        else
          c.set(Calendar.HOUR_OF_DAY, 12);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        break;
      case QUARTER:
        int month = c.get(Calendar.MONTH);
        if (month >= 9)
          c.set(Calendar.MONTH, 9);
        else if (month >= 9)
          c.set(Calendar.MONTH, 6);
        else if (month >= 9)
          c.set(Calendar.MONTH, 3);
        else
          c.set(Calendar.MONTH, 0);
        c.set(Calendar.DAY_OF_MONTH, 0);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        break;
    }
    return;
  }

  /**
   * Utility routine to set each DateTime component field to that specified by
   * the DateItem's millisecond field.
   * 
   * @param d
   *          The DateItem to modify.
   */
  private void millisToComponent(DateItem d) {
    mCal.setTimeInMillis(d.mMillis);
    d.mYear = mCal.get(Calendar.YEAR);
    d.mMonth = mCal.get(Calendar.MONTH);
    d.mDay = mCal.get(Calendar.DAY_OF_MONTH);
    d.mHour = mCal.get(Calendar.HOUR_OF_DAY);
    d.mMinute = mCal.get(Calendar.MINUTE);
  }

  /**
   * Copy all member variable of one DateItem to that of another DateItem.
   * 
   * @param src
   *          The DateItem to copy from.
   * @param dst
   *          The DateItem to copy to.
   */
  private void copyDate(DateItem src, DateItem dst) {
    dst.mYear = src.mYear;
    dst.mMonth = src.mMonth;
    dst.mDay = src.mDay;
    dst.mHour = src.mHour;
    dst.mMinute = src.mMinute;
    dst.mMillis = src.mMillis;
  }
}
