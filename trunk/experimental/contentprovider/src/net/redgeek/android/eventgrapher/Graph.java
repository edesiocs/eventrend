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

package net.redgeek.android.eventgrapher;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Region;

import net.redgeek.android.eventgrapher.primitives.Datapoint;
import net.redgeek.android.eventgrapher.primitives.FloatTuple;
import net.redgeek.android.eventgrapher.primitives.TimeSeries;
import net.redgeek.android.eventgrapher.primitives.TimeSeriesCollector;
import net.redgeek.android.eventgrapher.primitives.Transformation;
import net.redgeek.android.eventrecorder.TimeSeriesData;
import net.redgeek.android.eventrend.Preferences;
import net.redgeek.android.eventrend.datum.EntryEditActivity;
import net.redgeek.android.eventrend.util.DateUtil;
import net.redgeek.android.eventrend.util.Number;
import net.redgeek.android.eventrend.util.DateUtil.Period;

import java.util.ArrayList;

public class Graph {
  // Set by the GraphView when lookup up a datapoint/series from screen
  // coordinates:
  public TimeSeries mSelectedSeries;
  public Datapoint mSelectedDatapoint;
  public int mSelectedColor;

  // Dialogs
  private static final int DIALOG_POINT_INFO = 0;
  private static final int DIALOG_RANGE_INFO = 1;

  // UI elements
  private Path mAxis;
  private Paint mAxisPaint;
  private Paint mBackgroundPaint;
  private Paint mMarkerPaint;
  private Paint mLabelPrimaryPaint;
  private Paint mLabelHighlightPaint;

  // Private data
  private Context mCtx;
  private TimeSeriesCollector mTSC;
  private Transformation mTransform;
  private DateUtil mDates;

  private FloatTuple mGraphSize;
  private FloatTuple mPlotSize;
  private int mStartMS;
  private int mEndMS;
  private String mAggregation;
  private FloatTuple mPlotOffset;
  private FloatTuple mBoundsMins;
  private FloatTuple mBoundsMaxs;
  private Period mSpan;

  private boolean mShowTrends = true;
  private boolean mShowGoals = true;
  private boolean mShowMarkers = false;

  public Graph(Context context, TimeSeriesCollector tsc, float viewWidth,
      float viewHeight) {
    mTSC = tsc;
    mCtx = context;

    setupData(viewWidth, viewHeight);
    setupUI();
  }

  private void setupData(float viewWidth, float viewHeight) {
    mGraphSize = new FloatTuple();
    mPlotSize = new FloatTuple();
    mTransform = new Transformation();

    mDates = new DateUtil();
    mPlotOffset = new FloatTuple(GraphView.LEFT_MARGIN, GraphView.TOP_MARGIN
        + GraphView.PLOT_TOP_PAD);
    mBoundsMins = new FloatTuple();
    mBoundsMaxs = new FloatTuple();

    setGraphSize(viewWidth, viewHeight);
  }

  private void setupUI() {
    mAxisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    mAxisPaint.setStyle(Paint.Style.STROKE);
    mAxisPaint.setStrokeWidth(GraphView.AXIS_WIDTH);

    mMarkerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    mMarkerPaint.setStyle(Paint.Style.STROKE);
    mMarkerPaint.setStrokeWidth(1);

    mLabelPrimaryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    mLabelPrimaryPaint.setStyle(Paint.Style.STROKE);
    mLabelPrimaryPaint.setStrokeWidth(GraphView.LABEL_WIDTH);

    mLabelHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    mLabelHighlightPaint.setStyle(Paint.Style.STROKE);
    mLabelHighlightPaint.setStrokeWidth(GraphView.LABEL_WIDTH);

    mBackgroundPaint = new Paint();
    mBackgroundPaint.setStyle(Paint.Style.FILL);

    setColorScheme();

    mAxis = new Path();
  }

  public void setColorScheme() {
    if (Preferences.getDefaultGraphIsBlack(mCtx) == true) {
      mAxisPaint.setColor(Color.LTGRAY);
      mMarkerPaint.setColor(Color.DKGRAY);
      mLabelPrimaryPaint.setColor(Color.LTGRAY);
      mLabelHighlightPaint.setColor(Color.WHITE);
      mBackgroundPaint.setColor(Color.BLACK);
    } else {
      mAxisPaint.setColor(Color.DKGRAY);
      mMarkerPaint.setColor(Color.LTGRAY);
      mLabelPrimaryPaint.setColor(Color.DKGRAY);
      mLabelHighlightPaint.setColor(Color.BLACK);
      mBackgroundPaint.setColor(Color.WHITE);
    }
  }

  public float getXMargins() {
    return GraphView.LEFT_MARGIN + GraphView.RIGHT_MARGIN;
  }

  public float getYMargins() {
    return GraphView.TOP_MARGIN + GraphView.BOTTOM_MARGIN;
  }

  public void setGraphSize(float width, float height) {
    mGraphSize = new FloatTuple(width, height);
    mPlotSize.x = mGraphSize.x - getXMargins();
    mPlotSize.y = mGraphSize.y - getYMargins() - GraphView.PLOT_TOP_PAD
        - GraphView.PLOT_BOTTOM_PAD;
    if (width == 0.0f)
      mPlotSize.x = 1;
    if (height == 0.0f)
      mPlotSize.y = 1;
    mTransform.setPlotSize(mPlotSize);
  }

  public Period getSpan() {
    return mSpan;
  }

  public int getGraphStart() {
    return mStartMS;
  }

  public int getGraphEnd() {
    return mEndMS;
  }

  public String getGraphAggregation() {
    return mAggregation;
  }

  public void setGraphRange(int start, int end) {
    mStartMS = start;
    mEndMS = end;
    resetBounds(start, end);
  }

  public FloatTuple getGraphSize() {
    return mGraphSize;
  }

  public FloatTuple getPlotOffset() {
    return mPlotOffset;
  }

  public void viewTrends(boolean b) {
    mShowTrends = b;
  }

  public void viewGoals(boolean b) {
    mShowGoals = b;
  }

  public void viewMarkers(boolean b) {
    mShowMarkers = b;
  }

  public TimeSeriesCollector getTimeSeriesCollector() {
    return mTSC;
  }

  public void resetBounds(int secStart, int secEnd) {
    mStartMS = secStart;
    mEndMS = secEnd;

    mBoundsMins.x = (float) mStartMS;
    mBoundsMaxs.x = (float) mEndMS;
    mBoundsMins.y = 0;
    mBoundsMaxs.y = mPlotSize.y;
  }

  public void setupRange(TimeSeries ts, boolean showGoals) {
    mBoundsMins.y = ts.getVisibleValueMin();
    mBoundsMaxs.y = ts.getVisibleValueMax();

    if (mBoundsMins.y >= mBoundsMaxs.y) {
      mBoundsMins.y = 0;
      mBoundsMaxs.y = mPlotSize.y;
    }

    if (showGoals) {
      double goal = ts.mRow.mGoal;
      if (goal > mBoundsMaxs.y)
        mBoundsMaxs.y = (float) goal;
      if (goal < mBoundsMins.y)
        mBoundsMins.y = (float) goal;
    }

    if (mBoundsMins.x == mBoundsMaxs.x) {
      mBoundsMins.x--;
      mBoundsMaxs.x++;
    }
    if (Math.abs(mBoundsMins.y - mBoundsMaxs.y) < GraphView.MINIMUM_DELTA) {
      mBoundsMins.y--;
      mBoundsMaxs.y++;
    }
    
    return;
  }

  public synchronized void plot(Canvas canvas) {
    int offset;
    if (canvas == null)
      return;

    if (mGraphSize.x <= 0 || mGraphSize.y <= 0 || mPlotSize.x <= 0
        || mPlotSize.y <= 0)
      return;

    if (mTSC.lock() == false)
      return;

    TimeSeries ts = null;
    ArrayList<TimeSeries> series = mTSC.getAllEnabledSeries();

    mTransform.clear();

    drawBaseAxis(canvas);
    drawXLabels(canvas);

    offset = 0;
    for (int i = 0; i < series.size(); i++) {
      ts = series.get(i);
      if (ts.isEnabled() == false)
        continue;

      canvas.save();
      drawPlotClipRect(canvas);

      setupRange(ts, mShowGoals);

      mTransform.setVirtualSize(mBoundsMins, mBoundsMaxs);
      mTransform.transformPath(ts.getDatapoints());

      ts.setPointRadius(GraphView.POINT_RADIUS);
      ts.drawPath(canvas);

      if (mShowGoals == true) {
        drawGoal(canvas, ts, offset);
      }
      if (mShowTrends == true) {
        ts.drawTrend(canvas);
      }
      if (mShowMarkers == true) {
        drawTrendMarker(canvas, ts);
      }

      canvas.restore();

      drawYLabels(canvas, ts, offset);
      offset++;
    }

    mTSC.unlock();

    return;
  }

  private void drawPlotClipRect(Canvas canvas) {
    canvas.clipRect(0, 0, mGraphSize.x, mGraphSize.y);
    canvas.clipRect(0 + GraphView.LEFT_MARGIN, 0 + GraphView.TOP_MARGIN,
        mGraphSize.x - GraphView.RIGHT_MARGIN, mGraphSize.y
            - GraphView.BOTTOM_MARGIN, Region.Op.INTERSECT);
    canvas.translate(mPlotOffset.x, mPlotOffset.y);
  }

  private void drawBaseAxis(Canvas canvas) {
    mAxis.rewind();
    mAxis.moveTo(GraphView.LEFT_MARGIN, GraphView.TOP_MARGIN);
    mAxis.lineTo(GraphView.LEFT_MARGIN, mGraphSize.y - GraphView.BOTTOM_MARGIN);
    mAxis.lineTo(mGraphSize.x - GraphView.RIGHT_MARGIN, mGraphSize.y
        - GraphView.BOTTOM_MARGIN);
    canvas.drawPath(mAxis, mAxisPaint);
  }

  private void drawXLabels(Canvas canvas) {
    float tick = 0.0f;
    float tickStep = 0.0f;
    float tickExtra = 0.0f;
    int tickMultiplier = 1;
    long msToNextPeriod = 0;
    Paint p;

    mTransform.setVirtualSize(mBoundsMins, mBoundsMaxs);

    mDates.setSpan(mStartMS, mEndMS);
    mDates.setBaseTime(mStartMS);
    mSpan = mDates.getSpan();
    msToNextPeriod = mDates.msToNextPeriod(mSpan);

    tick = (float) (mStartMS + msToNextPeriod);
    mDates.advanceInMs(msToNextPeriod);
    tickStep = mDates.msInPeriod(mSpan);

    tick = mTransform.shiftXDimension(tick);
    tick = mTransform.scaleXDimension(tick);
    tickStep = mTransform.scaleXDimension(tickStep);
    if (tickStep < GraphView.TICK_MIN_DISTANCE)
      tickMultiplier = (int) (GraphView.TICK_MIN_DISTANCE / tickStep) + 1;

    for (int i = 0; tick <= mPlotSize.x; i++) {
      String[] label = mDates.getLabel(mSpan);

      tickExtra = 0.0f;
      p = mLabelPrimaryPaint;
      if (mDates.isUnitChanged() == true) {
        tickExtra = GraphView.TICK_LENGTH;
        p = mLabelHighlightPaint;

        if (mShowMarkers == true) {
          canvas.drawLine(tick + GraphView.LEFT_MARGIN, GraphView.TOP_MARGIN,
              tick + GraphView.LEFT_MARGIN, mGraphSize.y
                  - GraphView.BOTTOM_MARGIN, mMarkerPaint);
        }
      }

      if (i % tickMultiplier == 0) {
        canvas.drawLine(tick + GraphView.LEFT_MARGIN, mGraphSize.y
            - GraphView.BOTTOM_MARGIN, tick + GraphView.LEFT_MARGIN,
            mGraphSize.y - GraphView.BOTTOM_MARGIN + GraphView.TICK_LENGTH
                + tickExtra, mAxisPaint);

        canvas.drawText(label[0], tick + GraphView.LEFT_MARGIN + 2,
            mGraphSize.y - GraphView.BOTTOM_MARGIN + GraphView.TEXT_HEIGHT, p);

        if (label[1].equals("") == false) {
          canvas.drawText(label[1], tick + GraphView.LEFT_MARGIN + 2,
              mGraphSize.y - GraphView.BOTTOM_MARGIN
                  + (GraphView.TEXT_HEIGHT * 2), p);
        }
      }

      mDates.advance(mSpan, 1);
      tick += tickStep;
    }
  }

  private void drawYLabels(Canvas canvas, TimeSeries ts, int seriesIndex) {
    float x = 5;
    float y = (seriesIndex * GraphView.TEXT_HEIGHT) + GraphView.TOP_MARGIN
        + GraphView.PLOT_TOP_PAD;
    int maxLabels = (int) (((mPlotSize.y - GraphView.TEXT_HEIGHT) / 2) / GraphView.TEXT_HEIGHT);
    if (seriesIndex >= maxLabels) {
      x += GraphView.LEFT_MARGIN;
      y = seriesIndex * GraphView.TEXT_HEIGHT;
    }

    if (y > (mPlotSize.y / 2)) {
      x += 20;
      y -= maxLabels * GraphView.TEXT_HEIGHT;
    }

    ts.drawText(canvas, "" + Number.Round(mBoundsMaxs.y, ts.mRow.mDecimals), x, y);
    y = mGraphSize.y - y - GraphView.TEXT_HEIGHT;
    ts.drawText(canvas, "" + Number.Round(mBoundsMins.y, ts.mRow.mDecimals), x, y);

    canvas.drawLine(GraphView.LEFT_MARGIN, GraphView.TOP_MARGIN
        + GraphView.PLOT_TOP_PAD,
        GraphView.LEFT_MARGIN + GraphView.TICK_LENGTH, GraphView.TOP_MARGIN
            + GraphView.PLOT_TOP_PAD, mAxisPaint);
    canvas.drawLine(GraphView.LEFT_MARGIN, mGraphSize.y
        - GraphView.BOTTOM_MARGIN - GraphView.PLOT_BOTTOM_PAD,
        GraphView.LEFT_MARGIN + GraphView.TICK_LENGTH, mGraphSize.y
            - GraphView.BOTTOM_MARGIN - GraphView.PLOT_BOTTOM_PAD, mAxisPaint);
  }

  private void drawTrendMarker(Canvas canvas, TimeSeries ts) {
    Datapoint d = ts.getLastVisible();
    if (d == null)
      d = ts.getFirstPostVisible();
    if (d == null)
      d = ts.getLastPreVisible();
    if (d == null)
      return;

    float y = d.mScreenTrend1.y;
    float label = Number.Round(d.mTrend, ts.mRow.mDecimals);

    ts.drawText(canvas, "" + label, 2, y + GraphView.TEXT_HEIGHT - 3);
    ts.drawMarker(canvas, new FloatTuple(0, y), new FloatTuple(mGraphSize.x
        - GraphView.RIGHT_MARGIN, y));
  }

  public void drawGoal(Canvas canvas, TimeSeries ts, int i) {
    float goal = (float) ts.mRow.mGoal;
    float y = goal;
    y = mTransform.shiftYDimension(y);
    y = mTransform.scaleYDimension(y);
    y = mGraphSize.y - GraphView.BOTTOM_MARGIN
        - (GraphView.PLOT_BOTTOM_PAD * 2) - y;

    if (y < 0 || y > mGraphSize.y)
      return;

    ts.drawText(canvas, "" + Number.Round(goal, ts.mRow.mDecimals),
        (i * GraphView.LEFT_MARGIN) + 2, y + GraphView.TEXT_HEIGHT - 3);
    ts.drawGoal(canvas, new FloatTuple(0, y), new FloatTuple(mGraphSize.x
        - GraphView.RIGHT_MARGIN, y));
  }

  public void lookupDatapoint(FloatTuple t) {
    TimeSeries ts = null;
    Datapoint d = null;

    for (int i = 0; i < mTSC.numSeries(); i++) {
      ts = (TimeSeries) mTSC.getSeries(i);
      d = ts.lookupVisibleDatapoint(t);
      if (d != null)
        break;
    }

    mSelectedDatapoint = d;
    if (mSelectedDatapoint != null) {
      try {
        mSelectedColor = Color.parseColor(ts.getColor());
      } catch (IllegalArgumentException e) {
        mSelectedColor = Color.BLACK;
      }
      pointInfoDialog(mSelectedDatapoint).show();
    }

    return;
  }

  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case DIALOG_POINT_INFO:
        return pointInfoDialog(mSelectedDatapoint);
      case DIALOG_RANGE_INFO:
        return rangeInfoDialog(mSelectedDatapoint.mTimeSeriesId);
      default:
    }
    return null;
  }

  private Dialog pointInfoDialog(Datapoint mSelected) {
    boolean synthetic = false;
    Builder b = new AlertDialog.Builder(mCtx);

    TimeSeries ts = mTSC.getSeriesByIdLocking(mSelected.mTimeSeriesId);

    float value = mSelected.mValue;
    float trend = Number.Round(mSelected.mTrend, ts.mRow.mDecimals);
    float pointDeviation = Number.Round((value - trend) / mSelected.mStdDev,
        ts.mRow.mDecimals);
    String devStr = "" + pointDeviation;
    if (pointDeviation > 0)
      devStr = "+" + pointDeviation;

    String info = "Category: " + ts.mRow.mTimeSeriesName + "\n" + "Timestamp: "
        + mSelected.toLabelString() + "\n" + "Value: " + value + " (" + devStr
        + " Std Dev)\n" + "Trend: " + trend + "\n";

    if (ts.mRow.mType.equals(TimeSeriesData.TimeSeries.TYPE_SYNTHETIC) == true)
      synthetic = true;

    if (synthetic == true) {
      info += "Aggregate of: " + mSelected.mEntries + " entries\n";
      info += "Type: " + ts.mRow.mAggregation + "\n";
    } else {
      info += "Type: Calculated\n";
    }

    b.setTitle("Entry Info");
    b.setMessage(info);

    if (synthetic == false) {
      b.setNegativeButton("Edit", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {
          mTSC.clearCache(); 
          // TODO: just invalidate/update the one point
          // TODO: entry editor intent
//          Intent i = new Intent(mCtx, EntryEditActivity.class);
//          i.putExtra(EntryDbTable.KEY_ROWID, mSelectedDatapoint.mEntryId);
//          ((GraphActivity) mCtx).startActivity(i);
        }
      });
    }
    b.setNeutralButton("Range Info", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        rangeInfoDialog(mSelectedDatapoint.mTimeSeriesId).show();
      }
    });
    b.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        // do nothing
      }
    });
    Dialog d = b.create();
    return d;
  }

  private Dialog rangeInfoDialog(long catId) {
    Builder b = new AlertDialog.Builder(mCtx);
    TimeSeries ts = mTSC.getSeriesByIdLocking(catId);

    String info = "Category: " + ts.mRow.mTimeSeriesName + "\n";
    if (ts != null) {
      Number.RunningStats timestampStats = ts.getTimestampStats();

      String tsAvgPeriod = DateUtil.toString(timestampStats.mMean);
      String tsAvgEntry = DateUtil.toString(timestampStats.mEntryMean);
      String tsVar = DateUtil.toStringSquared(timestampStats.mVar);
      String tsSD = DateUtil.toString(timestampStats.mStdDev);

      Datapoint first = ts.getFirstVisible();
      Datapoint last = ts.getLastVisible();

      info += "Values:\n" + "  " + DateUtil.toTimestamp(first.mTsStart) + " -\n"
          + "  " + DateUtil.toTimestamp(last.mTsStart) + "\n"
          + "  Range:       " + ts.getVisibleValueMin() + " - "
          + ts.getVisibleValueMax() + "\n"
          + "Time Between Datapoints:\n" + "  Avgerage:  " + tsAvgPeriod + "\n"
          + "  Std Dev.:   " + tsSD + "\n" + "  Variance:   " + tsVar + "\n"
          + "Time Between Entries:\n" + "  Avg/Entry:   " + tsAvgEntry + "\n";
    }

    b.setTitle("Visible Range Info");
    b.setMessage(info);
    b.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        // do nothing
      }
    });
    Dialog d = b.create();
    return d;
  }
}
