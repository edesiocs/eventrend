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

package net.redgeek.android.eventrend.calendar;

import java.util.ArrayList;
import java.util.Calendar;

import net.redgeek.android.eventrend.Preferences;
import net.redgeek.android.eventrend.calendar.CalendarPlot.PaintIndex;
import net.redgeek.android.eventrend.primitives.TimeSeriesCollector;
import net.redgeek.android.eventrend.primitives.Tuple;
import net.redgeek.android.eventrend.util.DateUtil;
import net.redgeek.android.eventrend.util.DateUtil.Period;
import net.redgeek.android.eventrend.util.Number.TrendState;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;

public class CalendarPlotYear {
  // UI elements
  private ArrayList<Paint> mPaints;

  // Private data
  private Context mCtx;
  private TimeSeriesCollector mTSC;
  private DateUtil mDates;
  private float mSensitivity;
  private float mCellWidth;
  private float mCellHeight;
  private float mColorHeight;

  private Tuple mDimensions;
  private long mStartMS;

  public CalendarPlotYear(Context context, TimeSeriesCollector tsc,
      ArrayList<Paint> paints, Tuple dimensions) {
    mTSC = tsc;
    mCtx = context;
    mPaints = paints;
    setupData(dimensions);
  }

  private void setupData(Tuple dimensions) {
    mDimensions = new Tuple();
    setDimensions(dimensions);
    mDates = new DateUtil();
    mSensitivity = Preferences.getStdDevSensitivity(mCtx);
    setCellSizes();
  }

  public void setDimensions(Tuple dimensions) {
    mDimensions.set(dimensions);
    setCellSizes();
  }

  public void setStart(long startMs) {
    mStartMS = startMs;
  }

  private void setCellSizes() {
    mCellHeight = (mDimensions.y) / 6.0f;
    mCellWidth = (mDimensions.x) / 7.0f;
    mColorHeight = mCellHeight / mTSC.numSeries();
  }

  public synchronized void plot(Canvas canvas) {
    if (canvas == null)
      return;

    if (mDimensions.x <= 0 || mDimensions.y <= 0)
      return;

    // drawMonth(canvas);

    return;
  }

  private int positionToRow(int position) {
    return position / 7;
  }

  private int positionToColumn(int position) {
    return position % 7;
  }

  private Tuple getCellTopLeft(int position) {
    Tuple t = new Tuple();
    t.x = (positionToColumn(position) * mCellWidth);
    t.y = (positionToRow(position) * mCellHeight);
    return t;
  }

  private Tuple getCellBottomRight(int position) {
    Tuple t = new Tuple();
    t.x = (positionToColumn(position) * mCellWidth) + mCellWidth;
    t.y = (positionToRow(position) * mCellHeight) + mCellHeight;
    return t;
  }

  private Paint mapTrendStateToPaint(TrendState state) {
    if (state == TrendState.UP_GOOD_BIG || state == TrendState.DOWN_GOOD_BIG)
      return mPaints.get(PaintIndex.DATUM_GOOD4.ordinal());
    else if (state == TrendState.UP_GOOD_SMALL
        || state == TrendState.DOWN_GOOD_SMALL)
      return mPaints.get(PaintIndex.DATUM_GOOD2.ordinal());
    else if (state == TrendState.DOWN_BAD_BIG
        || state == TrendState.DOWN_BAD_SMALL)
      return mPaints.get(PaintIndex.DATUM_BAD4.ordinal());
    else if (state == TrendState.UP_BAD_BIG || state == TrendState.UP_BAD_SMALL)
      return mPaints.get(PaintIndex.DATUM_BAD2.ordinal());
    else if (state == TrendState.UP_SMALL || state == TrendState.DOWN_SMALL
        || state == TrendState.EVEN)
      return mPaints.get(PaintIndex.DATUM_EVEN.ordinal());
    else if (state == TrendState.EVEN_GOAL)
      return mPaints.get(PaintIndex.DATUM_EVEN_GOAL.ordinal());
    return null;
  }

  private int setStartTime() {
    int focusMonth;

    Calendar cal = mDates.getCalendar();
    cal.setTimeInMillis(mStartMS);
    DateUtil.setToPeriodStart(cal, Period.MONTH);
    long ms = cal.getTimeInMillis();
    mDates.setBaseTime(ms);

    int month = mDates.get(Calendar.MONTH);
    int position = mDates.get(Calendar.DAY_OF_WEEK);

    focusMonth = month;

    if (position == cal.getFirstDayOfWeek()) {
      mDates.advance(Period.DAY, -7);
    } else {
      mDates.advance(Period.DAY, -position + 1);
    }

    return focusMonth;
  }
}
