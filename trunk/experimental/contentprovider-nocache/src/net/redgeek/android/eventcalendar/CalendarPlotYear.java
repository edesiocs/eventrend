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

package net.redgeek.android.eventcalendar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import net.redgeek.android.eventcalendar.CalendarPlot.PaintIndex;
import net.redgeek.android.eventgrapher.primitives.Datapoint;
import net.redgeek.android.eventgrapher.primitives.FloatTuple;
import net.redgeek.android.eventgrapher.primitives.TimeSeries;
import net.redgeek.android.eventgrapher.primitives.TimeSeriesCollector;
import net.redgeek.android.eventrecorder.DateMapCache;
import net.redgeek.android.eventrecorder.TimeSeriesData;
import net.redgeek.android.eventrecorder.DateMapCache.DateItem;
import net.redgeek.android.eventrecorder.TimeSeriesData.DateMap;
import net.redgeek.android.eventrend.Preferences;
import net.redgeek.android.eventrend.util.DateUtil;
import net.redgeek.android.eventrend.util.Number;
import net.redgeek.android.eventrend.util.Number.TrendState;

import java.util.ArrayList;
import java.util.Calendar;

public class CalendarPlotYear {
  private static final int YEAR_PADDING = 5;
  
  // UI elements
  private ArrayList<Paint> mPaints;

  // Private data
  private Context mCtx;
  private TimeSeriesCollector mTSC;
  private DateMapCache mDateMap;
  private float mCellWidth;
  private float mCellHeight;
  private float mColorHeight;
  private int mNYears = 5;

  private FloatTuple mDimensions;
  private int mStartTs;
  private int mFocusStart;
  private int mFocusEnd;

  public CalendarPlotYear(Context context, TimeSeriesCollector tsc,
      ArrayList<Paint> paints, FloatTuple dimensions) {
    mTSC = tsc;
    mCtx = context;
    mPaints = paints;
    setupData(dimensions);
  }

  private void setupData(FloatTuple dimensions) {
    mDimensions = new FloatTuple();
    setDimensions(dimensions);
    mDateMap = new DateMapCache();
    mDateMap.populateCache(mCtx);
    setCellSizes();
  }

  public void setDimensions(FloatTuple dimensions) {
    mDimensions.set(dimensions);
    if (mDimensions.y > mDimensions.x)
      mNYears = 5;
    else
      mNYears = 3;
    setCellSizes();
  }

  public void setStart(int start) {
    mStartTs = start;
  }

  private void setCellSizes() {
    mCellHeight = (mDimensions.y 
                    - (CalendarView.TEXT_HEIGHT + YEAR_PADDING)
                    - (YEAR_PADDING * (mNYears + 1))
                    - (CalendarView.TEXT_HEIGHT * (mNYears + 1)))
                    / mNYears;
    mCellWidth = (mDimensions.x) / 12.0f;
    mColorHeight = mCellHeight / mTSC.numSeries();
  }

  public synchronized void plot(Canvas canvas) {
    if (canvas == null)
      return;

    if (mDimensions.x <= 0 || mDimensions.y <= 0)
      return;

    drawYear(canvas);

    return;
  }

  private int positionToRow(int position) {
    return position / 12;
  }

  private int positionToColumn(int position) {
    return position % 12;
  }

  private FloatTuple getCellTopLeft(int position) {
    FloatTuple t = new FloatTuple();
    int row = positionToRow(position);
    t.x = (positionToColumn(position) * mCellWidth);
    t.y = (CalendarView.TEXT_HEIGHT * 2) + YEAR_PADDING;
    t.y += (row * mCellHeight);
    t.y += row * (YEAR_PADDING + CalendarView.TEXT_HEIGHT + YEAR_PADDING);
    return t;
  }

  private FloatTuple getCellBottomRight(int position) {
    FloatTuple t = new FloatTuple();
    int row = positionToRow(position);
    t.x = (positionToColumn(position) * mCellWidth) + mCellWidth;
    t.y = (CalendarView.TEXT_HEIGHT * 2) + YEAR_PADDING;
    t.y += (row * mCellHeight) + mCellHeight;
    t.y += row * (YEAR_PADDING + CalendarView.TEXT_HEIGHT + YEAR_PADDING);
    return t;
  }

  private Paint mapTrendStateToPaint(TrendState state) {
    if (state == TrendState.UP_45_GOOD || state == TrendState.DOWN_45_GOOD)
      return mPaints.get(PaintIndex.DATUM_GOOD4.ordinal());
    else if (state == TrendState.UP_30_GOOD || state == TrendState.DOWN_30_GOOD)
      return mPaints.get(PaintIndex.DATUM_GOOD3.ordinal());
    else if (state == TrendState.UP_15_GOOD || state == TrendState.DOWN_15_GOOD)
      return mPaints.get(PaintIndex.DATUM_GOOD2.ordinal());
    else if (state == TrendState.UP_45_BAD || state == TrendState.DOWN_45_BAD)
      return mPaints.get(PaintIndex.DATUM_BAD4.ordinal());
    else if (state == TrendState.UP_30_BAD || state == TrendState.DOWN_30_BAD)
      return mPaints.get(PaintIndex.DATUM_BAD3.ordinal());
    else if (state == TrendState.UP_15_BAD || state == TrendState.DOWN_15_BAD)
      return mPaints.get(PaintIndex.DATUM_BAD2.ordinal());    
    else if (state == TrendState.UP_15 || state == TrendState.DOWN_15 || state == TrendState.FLAT)
      return mPaints.get(PaintIndex.DATUM_EVEN.ordinal());
    else if (state == TrendState.FLAT_GOAL)
      return mPaints.get(PaintIndex.DATUM_EVEN_GOAL.ordinal());
    return null;
  }

  private int setStartTime() {
    int ms = mDateMap.secondsOfPeriodStart(mStartTs,
        TimeSeriesData.DateMap.YEAR_SECS);
    DateItem d = new DateItem();
    mDateMap.getDateItem(ms, d);

    int focusYear = d.mYear;

//    long ms = cal.getTimeInMillis();
//    mDates.setBaseTime(ms);
//    mDates.advance(Period.YEAR, -2);
    
    return focusYear;
  }
  
  private void drawYearMonthBackground(Canvas canvas, boolean focused,
      int position, int month, float lastTrend, float thisTrend, float goal,
      float stdDev) {
    Paint p = null;

    FloatTuple topLeft = getCellTopLeft(position);
    FloatTuple bottomRight = getCellBottomRight(position);
    // RectF cell = new RectF(topLeft.x, topLeft.y, bottomRight.x,
    // bottomRight.y);
    RectF cell;
    if (mNYears == 5) {
      cell = new RectF(topLeft.x, topLeft.y, bottomRight.x, topLeft.y + 
          ((bottomRight.y - topLeft.y) / 3.0f));
    } else {
      cell = new RectF(topLeft.x, topLeft.y, bottomRight.x, topLeft.y + 
          ((bottomRight.y - topLeft.y) / 2.0f));      
    }

    // TODO: fetch timeseries sensitivity:
    // float unit = stdDev * mSensitivity;
    float unit = stdDev;
    float half = unit / 2;
    float quarter = half / 2;

    float delta = thisTrend - lastTrend;
    float absDelta = Math.abs(delta);
    if (absDelta > 0 && absDelta > quarter) {
      if ((delta > 0 && goal > thisTrend) || (delta < 0 && goal < thisTrend)) {
        if (delta > unit)
          p = mPaints.get(PaintIndex.DATUM_GOOD4.ordinal());
        else if (delta > half + quarter)
          p = mPaints.get(PaintIndex.DATUM_GOOD3.ordinal());
        else if (delta > half)
          p = mPaints.get(PaintIndex.DATUM_GOOD2.ordinal());
        else
          // if (delta > quarter)
          p = mPaints.get(PaintIndex.DATUM_GOOD1.ordinal());
      } else {
        if (delta > unit)
          p = mPaints.get(PaintIndex.DATUM_BAD4.ordinal());
        else if (delta > half + quarter)
          p = mPaints.get(PaintIndex.DATUM_BAD3.ordinal());
        else if (delta > half)
          p = mPaints.get(PaintIndex.DATUM_BAD2.ordinal());
        else
          // if (delta > quarter)
          p = mPaints.get(PaintIndex.DATUM_BAD1.ordinal());
      }
    } else {
      // even
      if (Math.abs(thisTrend - goal) < half)
        p = mPaints.get(PaintIndex.DATUM_EVEN_GOAL.ordinal());
      else
        p = null;
      // p = mPaints.get(PaintIndex.DATUM_EVEN.ordinal());
    }

    if (p != null) {
      canvas.drawRect(cell, p);
    }
  }
  
  private void drawYearMonthValue(Canvas canvas, int position, float prevValue,
      float thisValue, float goal, float stdDev) {
    Paint p;
    FloatTuple topLeft = getCellTopLeft(position);
    FloatTuple bottomRight = getCellBottomRight(position);

    // TODO: fetch timeseries sensitivity:
    // float unit = stdDev * mSensitivity;
    float unit = stdDev;
    float half = unit / 2;
    float quarter = half / 2;

    float delta = thisValue - prevValue;
    float absDelta = Math.abs(delta);
    if (absDelta > 0 && absDelta > quarter) {
      if ((delta > 0 && goal > thisValue) || (delta < 0 && goal < thisValue)) {
        if (delta > unit)
          p = mPaints.get(PaintIndex.DATUM_GOOD4.ordinal());
        else if (delta > half + quarter)
          p = mPaints.get(PaintIndex.DATUM_GOOD3.ordinal());
        else if (delta > half)
          p = mPaints.get(PaintIndex.DATUM_GOOD2.ordinal());
        else
          // if (delta > quarter)
          p = mPaints.get(PaintIndex.DATUM_GOOD1.ordinal());
      } else {
        if (delta > unit)
          p = mPaints.get(PaintIndex.DATUM_BAD4.ordinal());
        else if (delta > half + quarter)
          p = mPaints.get(PaintIndex.DATUM_BAD3.ordinal());
        else if (delta > half)
          p = mPaints.get(PaintIndex.DATUM_BAD2.ordinal());
        else
          // if (delta > quarter)
          p = mPaints.get(PaintIndex.DATUM_BAD1.ordinal());
      }
    } else {
      // even
      if (Math.abs(thisValue - goal) < half)
        p = mPaints.get(PaintIndex.DATUM_EVEN_GOAL.ordinal());
      else
        p = mPaints.get(PaintIndex.DATUM_EVEN.ordinal());
    }

    int offset = 0;
    if (mNYears == 5) {
      if (position % 2 == 0)
        offset = -CalendarView.TEXT_HEIGHT - 3;
    }
    
    // Paint p = mPaints.get(PaintIndex.VALUE.ordinal());
    canvas.drawText("" + Number.Round(thisValue), topLeft.x + 3,
        bottomRight.y + offset - 4, p);
  }

  private void drawYearMonthBorder(Canvas canvas, boolean focused, int position,
      int month) {
    FloatTuple topLeft = getCellTopLeft(position);
    FloatTuple bottomRight = getCellBottomRight(position);

    Paint p;
    if (focused == true) {
      p = mPaints.get(PaintIndex.BORDER_SECONDARY.ordinal());
      RectF cell = new RectF(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y);
      canvas.drawRect(cell, p);
    } else {
      p = mPaints.get(PaintIndex.BORDER_SECONDARY.ordinal());
      RectF cell = new RectF(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y);
      canvas.drawRect(cell, p);
    }
  }

  private void drawFocusBorder(Canvas canvas, int firstPosition,
      int lastPosition) {
    FloatTuple start = new FloatTuple();
    FloatTuple topLeft = getCellTopLeft(firstPosition);
    FloatTuple bottomRight = getCellBottomRight(lastPosition);

    RectF cell = new RectF(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y);

    Paint p = mPaints.get(PaintIndex.BORDER_PRIMARY.ordinal());
    canvas.drawRect(cell, p);
    p = mPaints.get(PaintIndex.BORDER_HIGHLIGHT.ordinal());
    canvas.drawRect(cell, p);
  }
  
  private void drawMonthHeader(Canvas canvas) {
    Paint p = mPaints.get(PaintIndex.LABEL.ordinal());
    for (int i = 0; i < 12; i++) {
      FloatTuple topLeft = getCellTopLeft(i);
      canvas.drawText(TimeSeriesData.DateMap.MONTHS[i], topLeft.x + 3, CalendarView.TEXT_HEIGHT - 3, p);
    }
  }

  private void drawYearHeader(Canvas canvas, int position, int year) {
    Paint p = mPaints.get(PaintIndex.LABEL.ordinal());
    FloatTuple topLeft = getCellTopLeft(position);
    FloatTuple bottomRight = getCellBottomRight(position + 11);
    
    canvas.drawText("" + year, topLeft.x + YEAR_PADDING + 3, topLeft.y
        - 3, p);
  }

  private void drawYear(Canvas canvas) {
    Calendar tmp = Calendar.getInstance();
    TimeSeries ts;
    Datapoint prev, current;
    int focusYear;
    int month, year;
    int firstPosition = 0;
    int lastPosition = 0;

    focusYear = setStartTime();

    // TODO: impelement
//    Calendar cal = mDates.getCalendar();
//    long ms = cal.getTimeInMillis();
//
//    drawMonthHeader(canvas);
//
//    int position = 0;
//    for (int i = 0; i < mNYears * 12; i++, position++) {
//      year = mDates.get(Calendar.YEAR);
//      month = mDates.get(Calendar.MONTH);
//
//      ms = mDates.getCalendar().getTimeInMillis();
//      for (int s = 0; s < mTSC.numSeries(); s++) {
//        ts = (TimeSeries) mTSC.getSeries(s);
//        if (ts == null || mTSC.isSeriesEnabled(ts.getDbRow().getId()) == false)
//          continue;
//
//        current = ts.findPostNeighbor(ms);
//        prev = ts.findPreNeighbor(ms - 1);
//
//        if (current != null && prev != null) {
//          if (focusYear == mDates.get(Calendar.YEAR)) {
//            drawYearMonthBackground(canvas, true, position, month, prev.mTrend.y,
//                current.mTrend.y, ts.getDbRow().getGoal(), current.mStdDev);
//          } else {
//            drawYearMonthBackground(canvas, false, position, month, prev.mTrend.y,
//                current.mTrend.y, ts.getDbRow().getGoal(), current.mStdDev);
//          }
//        }
//
//        if (current != null) {
//          float oldVal = current.mValue.y;
//          if (prev != null)
//            oldVal = prev.mValue.y;
//
//          tmp.setTimeInMillis(current.mMillis);
//          if (month == tmp.get(Calendar.MONTH) && year == tmp.get(Calendar.YEAR)) {
//            drawYearMonthValue(canvas, position, oldVal, current.mValue.y, ts
//                .getDbRow().getGoal(), current.mStdDev);
//          }
//        }
//      }
//      
//      if (month == 0) {
//        drawYearHeader(canvas, position, year);
//      }
//
//      if (focusYear == mDates.get(Calendar.YEAR)) {
//        if (month == 0) {
//          firstPosition = position;
//          mFocusStart = ms;
//        }
//        if (month == 11) {
//          lastPosition = position;
//          mFocusEnd = ms + DateUtil.YEAR_MS - 1;
//        }
//
//        drawYearMonthBorder(canvas, true, position, month);
//      } else {
//        drawYearMonthBorder(canvas, false, position, month);
//      }
//      mDates.advance(Period.MONTH, 1);
//    }

    drawFocusBorder(canvas, firstPosition, lastPosition);
  }
  
  public int getFocusStart() {
    return mFocusStart;
  }

  public int getFocusEnd() {
    return mFocusEnd;
  }
}
