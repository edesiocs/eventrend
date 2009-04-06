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

package net.redgeek.android.eventrend.test.common;

import android.graphics.Canvas;

import net.redgeek.android.eventrend.graph.TimeSeriesPainter;
import net.redgeek.android.timeseries.TimeSeries;
import net.redgeek.android.timeseries.Tuple;

// Not testing any drawing calls yet
public class MockTimeSeriesPainter implements TimeSeriesPainter {
  public MockTimeSeriesPainter() {
  }

  public void drawGoal(Canvas canvas, Tuple start, Tuple end) {
  }

  public void drawMarker(Canvas canvas, Tuple start, Tuple end) {
  }

  public void drawPath(Canvas canvas, TimeSeries ts) {
  }

  public void drawText(Canvas canvas, String text, float x, float y) {
  }

  public void drawTrend(Canvas canvas, TimeSeries ts) {
  }

  public void setColor(String color) {
  }

  public void setPointRadius(float size) {
  }
}
