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

package net.redgeek.android.eventrend.graph;

import java.util.ArrayList;

import net.redgeek.android.eventrend.Preferences;
import net.redgeek.android.eventrend.datum.EntryEditActivity;
import net.redgeek.android.eventrend.db.CategoryDbTable;
import net.redgeek.android.eventrend.db.EntryDbTable;
import net.redgeek.android.eventrend.db.EvenTrendDbAdapter;
import net.redgeek.android.eventrend.primitives.Datapoint;
import net.redgeek.android.eventrend.primitives.TimeSeries;
import net.redgeek.android.eventrend.primitives.TimeSeriesCollector;
import net.redgeek.android.eventrend.primitives.Tuple;
import net.redgeek.android.eventrend.util.DateUtil;
import net.redgeek.android.eventrend.util.Number;
import net.redgeek.android.eventrend.util.DateUtil.Period;
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
  private int mDecimals;

  private Tuple mGraphSize;
  private Tuple mPlotSize;
  private long mStartMS;
  private long mEndMS;
  private Tuple mPlotOffset;
  private Tuple mBoundsMins;
  private Tuple mBoundsMaxs;
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
    mGraphSize = new Tuple();
    mPlotSize = new Tuple();
    mTransform = new Transformation();

    mDates = new DateUtil();
    mPlotOffset = new Tuple(GraphView.LEFT_MARGIN, GraphView.TOP_MARGIN
        + GraphView.PLOT_TOP_PAD);
    mBoundsMins = new Tuple();
    mBoundsMaxs = new Tuple();

    setGraphSize(viewWidth, viewHeight);

    mDecimals = Preferences.getDecimalPlaces(mCtx);
  }

  public void setDecimals(int decimals) {
    mDecimals = decimals;
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
    mGraphSize = new Tuple(width, height);
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

  public long getGraphStart() {
    return mStartMS;
  }

  public long getGraphEnd() {
    return mEndMS;
  }

  public void setGraphRange(long start, long end) {
    mStartMS = start;
    mEndMS = end;
    resetBounds(start, end);
  }

  public Tuple getGraphSize() {
    return mGraphSize;
  }

  public Tuple getPlotOffset() {
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

  public void resetBounds(long milliStart, long milliEnd) {
    mStartMS = milliStart;
    mEndMS = milliEnd;

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
      float goal = ts.getDbRow().getGoal();
      if (goal > mBoundsMaxs.y)
        mBoundsMaxs.y = goal;
      if (goal < mBoundsMins.y)
        mBoundsMins.y = goal;
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

    ts.drawText(canvas, "" + Number.Round(mBoundsMaxs.y, mDecimals), x, y);
    y = mGraphSize.y - y - GraphView.TEXT_HEIGHT;
    ts.drawText(canvas, "" + Number.Round(mBoundsMins.y, mDecimals), x, y);

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

    float y = d.mTrendScreen.y;
    float label = Number.Round(d.mTrend.y, mDecimals);

    ts.drawText(canvas, "" + label, 2, y + GraphView.TEXT_HEIGHT - 3);
    ts.drawMarker(canvas, new Tuple(0, y), new Tuple(mGraphSize.x
        - GraphView.RIGHT_MARGIN, y));
  }

  public void drawGoal(Canvas canvas, TimeSeries ts, int i) {
    float goal = ts.getDbRow().getGoal();
    float y = goal;
    y = mTransform.shiftYDimension(y);
    y = mTransform.scaleYDimension(y);
    y = mGraphSize.y - GraphView.BOTTOM_MARGIN
        - (GraphView.PLOT_BOTTOM_PAD * 2) - y;

    if (y < 0 || y > mGraphSize.y)
      return;

    ts.drawText(canvas, "" + Number.Round(goal, mDecimals),
        (i * GraphView.LEFT_MARGIN) + 2, y + GraphView.TEXT_HEIGHT - 3);
    ts.drawGoal(canvas, new Tuple(0, y), new Tuple(mGraphSize.x
        - GraphView.RIGHT_MARGIN, y));
  }

  public void lookupDatapoint(Tuple t) {
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
        return rangeInfoDialog(mSelectedDatapoint.mCatId);
      default:
    }
    return null;
  }

  private Dialog pointInfoDialog(Datapoint mSelected) {
    Builder b = new AlertDialog.Builder(mCtx);

    TimeSeries ts = mTSC.getSeriesByIdLocking(mSelected.mCatId);
    int decimals = Preferences.getDecimalPlaces(mCtx);

    EvenTrendDbAdapter dbh = ((GraphActivity) mCtx).getDbh();
    CategoryDbTable.Row cat = dbh.fetchCategory(mSelected.mCatId);

    Number.RunningStats stats = ts.getValueStats();

    float value = mSelected.mValue.y;
    float trend = Number.Round(mSelected.mTrend.y, decimals);
    float pointDeviation = Number.Round((value - trend) / stats.mStdDev,
        decimals);
    String devStr = "" + pointDeviation;
    if (pointDeviation > 0)
      devStr = "+" + pointDeviation;

    String info = "Category: " + cat.getCategoryName() + "\n" + "Timestamp: "
        + mSelected.toLabelString() + "\n" + "Value: " + value + " (" + devStr
        + " Std Dev)\n" + "Trend: " + trend + "\n";

    if (mSelected.mSynthetic == false) {
      info += "Aggregate of: " + mSelected.mNEntries + " entries\n";
      info += "Type: " + cat.getType() + "\n";
    } else {
      info += "Type: Calculated\n";
    }

    b.setTitle("Entry Info");
    b.setMessage(info);

    if (mSelected.mSynthetic == false) {
      b.setNegativeButton("Edit", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {
          mTSC.clearCache(); // TODO: just invalidate/update the one
          // point
          Intent i = new Intent(mCtx, EntryEditActivity.class);
          i.putExtra(EntryDbTable.KEY_ROWID, mSelectedDatapoint.mEntryId);
          ((GraphActivity) mCtx).startActivity(i);
        }
      });
    }
    b.setNeutralButton("Range Info", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        rangeInfoDialog(mSelectedDatapoint.mCatId).show();
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
    EvenTrendDbAdapter dbh = ((GraphActivity) mCtx).getDbh();
    CategoryDbTable.Row cat = dbh.fetchCategory(catId);
    TimeSeries ts = mTSC.getSeriesByIdLocking(catId);

    int decimals = Preferences.getDecimalPlaces(mCtx);

    String info = "Category: " + cat.getCategoryName() + "\n";
    if (ts != null) {
      Number.RunningStats valueStats = ts.getValueStats();
      Number.RunningStats timestampStats = ts.getTimestampStats();

      String tsAvgPeriod = DateUtil.toString(timestampStats.mMean);
      String tsAvgEntry = DateUtil.toString(timestampStats.mEntryMean);
      String tsVar = DateUtil.toStringSquared(timestampStats.mVar);
      String tsSD = DateUtil.toString(timestampStats.mStdDev);

      Datapoint first = ts.getFirstVisible();
      Datapoint last = ts.getLastVisible();

      info += "Values:\n" + "  " + DateUtil.toTimestamp(first.mMillis) + " -\n"
          + "  " + DateUtil.toTimestamp(last.mMillis) + "\n"
          + "  Range:       " + ts.getVisibleValueMin() + " - "
          + ts.getVisibleValueMax() + "\n" + "  Average:   "
          + Number.Round(valueStats.mMean, decimals) + "\n" + "  Std Dev.:    "
          + Number.Round(valueStats.mStdDev, decimals) + "\n"
          + "  Variance:   " + Number.Round(valueStats.mVar, decimals) + "\n"
          + "  Trend:       " + Number.Round(ts.getTrendStats().mMin, decimals)
          + " - " + Number.Round(ts.getTrendStats().mMax, decimals) + "\n"
          + "Date Goal is Reached:\n"

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
