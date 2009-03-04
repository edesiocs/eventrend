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

package net.redgeek.android.eventrend.test.primitives;

import android.graphics.Canvas;

import junit.framework.TestCase;

import net.redgeek.android.eventrend.db.CategoryDbTable;
import net.redgeek.android.eventrend.graph.TimeSeriesPainter;
import net.redgeek.android.eventrend.primitives.TimeSeries;
import net.redgeek.android.eventrend.primitives.Tuple;

// Note that several tests use equality comparison on float, which could be 
// dangerous in general, but should be safe for such small predefined values.
public class TimeSeriesTest extends TestCase {
  // Not testing any drawing calls yet
  public static class MockTimeSeriesPainter implements TimeSeriesPainter {
      public MockTimeSeriesPainter() { }
      public void drawGoal(Canvas canvas, Tuple start, Tuple end) { }
      public void drawMarker(Canvas canvas, Tuple start, Tuple end) { }
      public void drawPath(Canvas canvas, TimeSeries ts) { }
      public void drawText(Canvas canvas, String text, float x, float y) { }
      public void drawTrend(Canvas canvas, TimeSeries ts) { }
      public void setColor(String color) { }
      public void setPointRadius(float size) { } 
    }
    
    public void testConstructor() {
      MockTimeSeriesPainter painter = new MockTimeSeriesPainter();
      CategoryDbTable.Row row = new CategoryDbTable.Row();
      TimeSeries ts = new TimeSeries(row, 10, 0.1f, painter);

      assertFalse(ts.isEnabled());
      assertNotNull(ts.getDatapoints());
      assertEquals(0, ts.getDatapoints().size());
      assertNotNull(ts.getVisibleMins());
      assertEquals(Float.MAX_VALUE, ts.getVisibleMins().x);
      assertEquals(Float.MAX_VALUE, ts.getVisibleMins().y);
      assertNotNull(ts.getVisibleMins());
      assertEquals(Float.MIN_VALUE, ts.getVisibleMaxs().x);
      assertEquals(Float.MIN_VALUE, ts.getVisibleMaxs().y);
      assertEquals(Float.MAX_VALUE, ts.getDatapointValueMin());
      assertEquals(Float.MIN_VALUE, ts.getDatapointValueMax());
      assertNotNull(ts.getDependents());
      assertEquals(0, ts.getDependents().size());
      assertNotNull(ts.getDependees());
      assertEquals(0, ts.getDependees().size());
      assertNull(ts.getFirstVisible());
      assertNull(ts.getLastVisible());
      assertNull(ts.getFirstPostVisible());
      assertNull(ts.getLastPreVisible());
      assertEquals(0, ts.getVisibleNumEntries());
      assertNotNull(ts.getValueStats());
      assertNotNull(ts.getTrendStats());
      assertNotNull(ts.getTimestampStats());      
    }
}

