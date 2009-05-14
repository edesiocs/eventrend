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
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

import net.redgeek.android.eventgrapher.primitives.FloatTuple;
import net.redgeek.android.eventgrapher.primitives.TimeSeriesCollector;
import net.redgeek.android.eventrecorder.DateMapCache;
import net.redgeek.android.eventrecorder.TimeSeriesData;
import net.redgeek.android.eventrend.Preferences;
import net.redgeek.android.eventrend.R;

import java.util.ArrayList;

public class CalendarPlot {
  // UI elements
  private ArrayList<Paint> mPaints;
  private Paint mBorderPrimaryPaint;
  private Paint mBorderSecondaryPaint;
  private Paint mBorderHighlightPaint;
  private Paint mBackgroundPaint;

  private CalendarPlotMonth mMonthPlot;
  private CalendarPlotYear mYearPlot;

  public enum PaintIndex {
    BORDER_PRIMARY, BORDER_HIGHLIGHT, BORDER_SECONDARY, DATUM_GOOD1, DATUM_GOOD2, DATUM_GOOD3, DATUM_GOOD4, DATUM_BAD1, DATUM_BAD2, DATUM_BAD3, DATUM_BAD4, DATUM_EVEN, DATUM_EVEN_GOAL, LABEL, VALUE,
  }

  // Private data
  private Context mCtx;
  private TimeSeriesCollector mTSC;
  private DateMapCache mDateMap;

  private FloatTuple mPlotSize;
  private FloatTuple mPlotBorder;
  private int mStartTs;
  private int mSpan;

  public CalendarPlot(Context context, TimeSeriesCollector tsc,
      float viewWidth, float viewHeight) {
    mTSC = tsc;
    mCtx = context;

    setupData(viewWidth, viewHeight);
  }

  private void newCellPaint(int resId, PaintIndex idx) {
    Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    p.setStyle(Paint.Style.FILL);
    p.setColor(mCtx.getResources().getColor(resId));
    p.setTypeface(Typeface.DEFAULT_BOLD);
    mPaints.add(idx.ordinal(), p);
  }

  private void setupData(float viewWidth, float viewHeight) {
    mPlotSize = new FloatTuple();
    mPlotBorder = new FloatTuple(CalendarView.BORDER_X, CalendarView.BORDER_Y);
    mDateMap = new DateMapCache();
    mDateMap.populateCache(mCtx);

    mPaints = new ArrayList<Paint>();

    mBorderPrimaryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    mBorderPrimaryPaint.setStyle(Paint.Style.STROKE);
    mBorderPrimaryPaint.setColor(Color.parseColor("#6666ff"));
    mBorderPrimaryPaint.setStrokeWidth(CalendarView.CELL_BORDER_FOCUSED);
    mPaints.add(PaintIndex.BORDER_PRIMARY.ordinal(), mBorderPrimaryPaint);

    mBorderHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    mBorderHighlightPaint.setStyle(Paint.Style.STROKE);
    mBorderHighlightPaint.setColor(Color.parseColor("#bbbbff"));
    mBorderHighlightPaint.setStrokeWidth(CalendarView.CELL_BORDER_HIGHLIGHT);
    mPaints.add(PaintIndex.BORDER_HIGHLIGHT.ordinal(), mBorderHighlightPaint);

    mBorderSecondaryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    mBorderSecondaryPaint.setStyle(Paint.Style.STROKE);
    mBorderSecondaryPaint.setStrokeWidth(CalendarView.CELL_BORDER_AUX);
    mPaints.add(PaintIndex.BORDER_SECONDARY.ordinal(), mBorderSecondaryPaint);

    newCellPaint(R.color.calendar_good1, PaintIndex.DATUM_GOOD1);
    newCellPaint(R.color.calendar_good2, PaintIndex.DATUM_GOOD2);
    newCellPaint(R.color.calendar_good3, PaintIndex.DATUM_GOOD3);
    newCellPaint(R.color.calendar_good4, PaintIndex.DATUM_GOOD4);

    newCellPaint(R.color.calendar_bad1, PaintIndex.DATUM_BAD1);
    newCellPaint(R.color.calendar_bad2, PaintIndex.DATUM_BAD2);
    newCellPaint(R.color.calendar_bad3, PaintIndex.DATUM_BAD3);
    newCellPaint(R.color.calendar_bad4, PaintIndex.DATUM_BAD4);

    newCellPaint(R.color.calendar_flat, PaintIndex.DATUM_EVEN);
    newCellPaint(R.color.calendar_goal, PaintIndex.DATUM_EVEN_GOAL);

    newCellPaint(R.color.calendar_dark_label, PaintIndex.LABEL);
    newCellPaint(R.color.calendar_value, PaintIndex.VALUE);

    mBackgroundPaint = new Paint();
    mBackgroundPaint.setStyle(Paint.Style.FILL);

    setColorScheme();

    mMonthPlot = new CalendarPlotMonth(mCtx, mTSC, mPaints, FloatTuple.minus(
        mPlotSize, mPlotBorder));
    mYearPlot = new CalendarPlotYear(mCtx, mTSC, mPaints, FloatTuple.minus(
        mPlotSize, mPlotBorder));

    setCalendarSize(viewWidth, viewHeight);
  }

  public void setColorScheme() {
    int gray = Color.parseColor("#777777");
    mBorderSecondaryPaint.setColor(gray);

    if (Preferences.getDefaultGraphIsBlack(mCtx) == true) {
      mPaints.get(PaintIndex.LABEL.ordinal()).setColor(
          mCtx.getResources().getColor(R.color.calendar_dark_label));
      mBackgroundPaint.setColor(Color.BLACK);
    } else {
      mPaints.get(PaintIndex.LABEL.ordinal()).setColor(
          mCtx.getResources().getColor(R.color.calendar_light_label));
      mBackgroundPaint.setColor(Color.WHITE);
    }
  }

  public void setCalendarSize(float width, float height) {
    mPlotSize = new FloatTuple(width, height);
    mMonthPlot.setDimensions(FloatTuple.minus(mPlotSize, mPlotBorder));
    mYearPlot.setDimensions(FloatTuple.minus(mPlotSize, mPlotBorder));
  }

  public void setSpan(int period) {
    mSpan = period;
  }

  public int getSpan() {
    return mSpan;
  }

  public int getCalendarStart() {
    return mStartTs;
  }

  public void setCalendarStart(int start) {
    mStartTs = start;
  }

  public FloatTuple getCalendarSize() {
    return mPlotSize;
  }

  public TimeSeriesCollector getTimeSeriesCollector() {
    return mTSC;
  }

  public synchronized void plot(Canvas canvas) {
    if (canvas == null)
      return;

    if (mPlotSize.x <= 0 || mPlotSize.y <= 0)
      return;

    canvas.translate(mPlotBorder.x / 2, mPlotBorder.y / 2);
    if (mSpan == TimeSeriesData.DateMap.MONTH_SECS) {
      mMonthPlot.setStart(mStartTs);
      mMonthPlot.plot(canvas);
    }
    else if (mSpan == TimeSeriesData.DateMap.YEAR_SECS) {
      mYearPlot.setStart(mStartTs);
      mYearPlot.plot(canvas);
    }

    return;
  }

  public long getFocusStart() {
    if (mSpan == TimeSeriesData.DateMap.MONTH_SECS) {
      return mMonthPlot.getFocusStart();
    }
    else if (mSpan == TimeSeriesData.DateMap.YEAR_SECS) {
      return mYearPlot.getFocusStart();
    }
    return 0;
  }

  public long getFocusEnd() {
    if (mSpan == TimeSeriesData.DateMap.MONTH_SECS) {
      return mMonthPlot.getFocusEnd();
    }
    else if (mSpan == TimeSeriesData.DateMap.YEAR_SECS) {
      return mYearPlot.getFocusEnd();
    }
    return 0;
  }

  public FloatTuple lookupPeriod(FloatTuple t) {
    // TODO Auto-generated method stub
    return null;
  }
}
