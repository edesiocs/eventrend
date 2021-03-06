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

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;

import net.redgeek.android.eventgrapher.primitives.Datapoint;
import net.redgeek.android.eventgrapher.primitives.FloatTuple;
import net.redgeek.android.eventgrapher.primitives.TimeSeries;

import java.util.ArrayList;

public interface TimeSeriesPainter {
  public void setColor(String colorStr);

  public void setPointRadius(float size);

  public void drawPath(Canvas canvas, TimeSeries ts);

  public void drawTrend(Canvas canvas, TimeSeries ts);

  public void drawMarker(Canvas canvas, FloatTuple start, FloatTuple end);

  public void drawGoal(Canvas canvas, FloatTuple start, FloatTuple end);

  public void drawText(Canvas canvas, String text, float x, float y);

  public static class Default implements TimeSeriesPainter {
    private Path mPath;
    private Path mTrend;
    private Path mPoints;
    private Paint mPathPaint;
    private Paint mPointsPaint;
    private Paint mTrendPaint;
    private Paint mTrendMarkerPaint;
    private Paint mGoalPaint;
    private Paint mLabelPaint;
    private float mPointRadius;

    public Default() {
      defaultInit();
    }

    public Default(TimeSeriesPainter.Default other) {
      if (other == null) {
        defaultInit();
      } else
        mPath = new Path(other.mPath);
      mTrend = new Path(other.mTrend);
      mPoints = new Path(other.mPoints);

      mPathPaint = new Paint(other.mPathPaint);
      mPointsPaint = new Paint(other.mPointsPaint);
      mTrendPaint = new Paint(other.mTrendPaint);
      mTrendMarkerPaint = new Paint(other.mTrendMarkerPaint);
      mGoalPaint = new Paint(other.mGoalPaint);
      mLabelPaint = new Paint(other.mLabelPaint);

      mPointRadius = other.mPointRadius;
    }

    private void defaultInit() {
      mPath = new Path();
      mTrend = new Path();
      mPoints = new Path();

      mPathPaint = new Paint();
      mPointsPaint = new Paint();
      mTrendPaint = new Paint();
      mTrendMarkerPaint = new Paint();
      mGoalPaint = new Paint();
      mLabelPaint = new Paint();

      mPointRadius = 3.0f;
    }

    public void setPointRadius(float size) {
      mPointRadius = size;
    }

    public void drawText(Canvas canvas, String text, float x, float y) {
      canvas.drawText(text, x, y, mLabelPaint);
    }

    public void drawGoal(Canvas canvas, FloatTuple start, FloatTuple end) {
      canvas.drawLine(start.x, start.y, end.x, end.y, mGoalPaint);
    }

    public void drawMarker(Canvas canvas, FloatTuple start, FloatTuple end) {
      canvas.drawLine(start.x, start.y, end.x, end.y, mTrendMarkerPaint);
    }

    public void drawPath(Canvas canvas, TimeSeries ts) {
      Datapoint thisPoint = null;
      Datapoint prevPoint = null;
      FloatTuple first = null;
      FloatTuple second = null;
      int datapointsSize;

      if (ts.isEnabled() == false || ts.getDatapoints() == null)
        return;

      mPath.rewind();
      mPoints.rewind();

      // we don't have to draw all the point preceding the first visible on,
      // just the last of them, up until one after the last visible
      ArrayList<Datapoint> datapoints = ts.getDatapoints();
      datapointsSize = datapoints.size();
      
//      int offset = ts.getVisiblePreLastIdx();
//      if (offset < 0 || offset == Integer.MAX_VALUE)
//        offset = 0;
//      int end = ts.getVisiblePostFirstIdx();
//      if (end < 0 || end == Integer.MAX_VALUE)
//        end = datapointsSize - 1;
//      for (int i = offset; i < datapointsSize && i <= end; i++) {

      for (int i = 0; i < datapointsSize; i++) {
        thisPoint = datapoints.get(i);
        if (thisPoint != null)
          second = thisPoint.mScreenValue1;
        if (prevPoint != null)
          first = prevPoint.mScreenValue1;

        ts.getInterpolator().updatePath(mPath, first, second);

        mPoints.moveTo(thisPoint.mScreenValue1.x, thisPoint.mScreenValue1.y);
        mPoints.addCircle(thisPoint.mScreenValue1.x, thisPoint.mScreenValue1.y,
            mPointRadius, Path.Direction.CW);

        prevPoint = thisPoint;
      }

      if (thisPoint != null) {
        mPath.setLastPoint(thisPoint.mScreenValue1.x, thisPoint.mScreenValue1.y);
        canvas.drawPath(mPath, mPathPaint);
        canvas.drawPath(mPoints, mPointsPaint);
      }
    }

    public void drawTrend(Canvas canvas, TimeSeries ts) {
      Datapoint thisPoint = null;
      Datapoint prevPoint = null;
      FloatTuple first = null;
      FloatTuple second = null;
      int datapointsSize;

      if (ts.isEnabled() == false || ts.getDatapoints() == null)
        return;

      // we don't have to draw all the point preceding the first visible on,
      // just the last of them, up until one after the last visible
      ArrayList<Datapoint> datapoints = ts.getDatapoints();
      datapointsSize = datapoints.size();
      
//    int offset = ts.getVisiblePreLastIdx();
//    if (offset < 0 || offset == Integer.MAX_VALUE)
//      offset = 0;
//    int end = ts.getVisiblePostFirstIdx();
//    if (end < 0 || end == Integer.MAX_VALUE)
//      end = datapointsSize - 1;
//    for (int i = offset; i < datapointsSize && i <= end; i++) {

      mTrend.rewind();
      for (int i = 0; i < datapointsSize; i++) {
        thisPoint = datapoints.get(i);
        if (thisPoint != null)
          second = thisPoint.mScreenTrend1;
        if (prevPoint != null)
          first = prevPoint.mScreenTrend1;

        ts.getInterpolator().updatePath(mTrend, first, second);

        prevPoint = thisPoint;
      }

      if (thisPoint != null) {
        canvas.drawPath(mTrend, mTrendPaint);
      }
    }

    public void setColor(String color) {
      int colorInt = Color.BLACK;
      try {
        colorInt = Color.parseColor(color);
      } catch (IllegalArgumentException e) {
        colorInt = Color.BLACK;
      }

      mPathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
      mPathPaint.setStyle(Paint.Style.STROKE);
      mPathPaint.setStrokeWidth(GraphView.PATH_WIDTH);
      mPathPaint.setColor(colorInt);

      mPointsPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
      mPointsPaint.setStyle(Paint.Style.FILL_AND_STROKE);
      mPointsPaint.setStrokeWidth(GraphView.PATH_WIDTH);
      mPointsPaint.setColor(colorInt);

      mTrendPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
      mTrendPaint.setStyle(Paint.Style.STROKE);
      mTrendPaint.setStrokeWidth(GraphView.TREND_WIDTH);
      mTrendPaint.setColor(colorInt);
      DashPathEffect trendDashes = new DashPathEffect(new float[] {
          GraphView.TREND_DASH_WIDTH, GraphView.TREND_DASH_WIDTH }, 0);
      mTrendPaint.setPathEffect(trendDashes);

      mTrendMarkerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
      mTrendMarkerPaint.setStyle(Paint.Style.STROKE);
      mTrendMarkerPaint.setStrokeWidth(GraphView.TREND_WIDTH);
      mTrendMarkerPaint.setColor(colorInt);
      DashPathEffect trendMarkerDashes = new DashPathEffect(new float[] {
          GraphView.GOAL_DASH_WIDTH / 2, GraphView.GOAL_DASH_WIDTH, }, 0);
      mTrendMarkerPaint.setPathEffect(trendMarkerDashes);

      mGoalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
      mGoalPaint.setStyle(Paint.Style.STROKE);
      mGoalPaint.setStrokeWidth(GraphView.GOAL_WIDTH);
      mGoalPaint.setColor(colorInt);
      DashPathEffect goalDashes = new DashPathEffect(new float[] {
          GraphView.GOAL_DASH_WIDTH, GraphView.GOAL_DASH_WIDTH }, 0);
      mGoalPaint.setPathEffect(goalDashes);

      mLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
      mLabelPaint.setStyle(Paint.Style.STROKE);
      mLabelPaint.setStrokeWidth(GraphView.LABEL_WIDTH);
      mLabelPaint.setColor(colorInt);
    }
  }
}