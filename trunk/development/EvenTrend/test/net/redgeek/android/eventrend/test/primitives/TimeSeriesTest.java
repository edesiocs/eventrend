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

import java.util.ArrayList;

import junit.framework.TestCase;
import net.redgeek.android.eventrend.db.CategoryDbTable;
import net.redgeek.android.eventrend.graph.TimeSeriesPainter;
import net.redgeek.android.eventrend.primitives.Datapoint;
import net.redgeek.android.eventrend.primitives.TimeSeries;
import net.redgeek.android.eventrend.primitives.Tuple;
import net.redgeek.android.eventrend.util.DateUtil;
import android.graphics.Canvas;

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

  public TimeSeries newDefaultTimeSeries() {
    MockTimeSeriesPainter painter = new MockTimeSeriesPainter();
    CategoryDbTable.Row row = new CategoryDbTable.Row();
    return new TimeSeries(row, 2, 0.5f, painter);
  }
  
  public void testConstructor() {
    TimeSeries ts = newDefaultTimeSeries();

    assertFalse(ts.isEnabled());
    assertNotNull(ts.getDatapoints());
    assertEquals(0, ts.getDatapoints().size());
    assertEquals(Float.MAX_VALUE, ts.getVisibleValueMin());
    assertEquals(Float.MIN_VALUE, ts.getVisibleValueMax());
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

    TimeSeries copy = new TimeSeries(ts);
    assertNotSame(ts.getDatapoints(), copy.getDatapoints());
    assertNotSame(ts.getDbRow(), copy.getDbRow());
    assertNotSame(ts.getDependents(), copy.getDependents());
    assertNotSame(ts.getDependees(), copy.getDependees());
    assertNotSame(ts.getValueStats(), copy.getValueStats());
    assertNotSame(ts.getTrendStats(), copy.getTrendStats());
    assertNotSame(ts.getTimestampStats(), copy.getTimestampStats());
    assertSame(ts.getInterpolator(), copy.getInterpolator());
  }
  
  public void testSetDatapoints() {
    ArrayList<Datapoint> lpre   = new ArrayList<Datapoint>();
    ArrayList<Datapoint> lrange = new ArrayList<Datapoint>();
    ArrayList<Datapoint> lpost  = new ArrayList<Datapoint>();
    Datapoint d1 = new Datapoint(100L, 1.0f, 1, 10, 1);
    Datapoint d2 = new Datapoint(150L, 2.0f, 1, 11, 1);
    Datapoint d3 = new Datapoint(250L, 3.0f, 1, 11, 1);
   
    TimeSeries ts = newDefaultTimeSeries();
    
    // (null) (null) (null)
    ts.setDatapoints(null, null, null);    
    assertEquals(0, ts.getDatapoints().size());
    assertEquals(Float.MAX_VALUE, ts.getVisibleValueMin());
    assertEquals(Float.MIN_VALUE, ts.getVisibleValueMax());
    assertNull(ts.getFirstVisible());
    assertNull(ts.getLastVisible());
    assertNull(ts.getFirstPostVisible());
    assertNull(ts.getLastPreVisible());

    // (d1) (null) (null)
    lpre.clear();
    lpre.add(d1);
    ts.setDatapoints(lpre, null, null);    

    assertEquals(1, ts.getDatapoints().size());
    assertEquals(Float.MAX_VALUE, ts.getVisibleValueMin());
    assertEquals(Float.MIN_VALUE, ts.getVisibleValueMax());
    assertNotNull(ts.getLastPreVisible());
    assertNull(ts.getFirstVisible());
    assertNull(ts.getLastVisible());
    assertNull(ts.getFirstPostVisible());
    assertSame(d1, ts.getDatapoints().get(0));
    assertSame(d1, ts.getLastPreVisible());

    // (d1, d2) (null) (null)
    lpre.clear();
    lpre.add(d1);
    lpre.add(d2);
    ts.setDatapoints(lpre, null, null);    

    assertEquals(2, ts.getDatapoints().size());
    assertEquals(Float.MAX_VALUE, ts.getVisibleValueMin());
    assertEquals(Float.MIN_VALUE, ts.getVisibleValueMax());
    assertNotNull(ts.getLastPreVisible());
    assertNull(ts.getFirstVisible());
    assertNull(ts.getLastVisible());
    assertNull(ts.getFirstPostVisible());
    assertSame(d1, ts.getDatapoints().get(0));
    assertSame(d2, ts.getDatapoints().get(1));
    assertSame(d2, ts.getLastPreVisible());

    // (d1) (d2) (null)
    lpre.clear();
    lrange.clear();
    lpre.add(d1);
    lrange.add(d2);
    ts.setDatapoints(lpre, lrange, null);    

    assertEquals(2, ts.getDatapoints().size());
    assertEquals(1.0f, ts.getVisibleValueMin()); // due to offscreen interpolation
    assertEquals(2.0f, ts.getVisibleValueMax());
    assertNotNull(ts.getLastPreVisible());
    assertNotNull(ts.getFirstVisible());
    assertNotNull(ts.getLastVisible());
    assertNull(ts.getFirstPostVisible());
    assertSame(d1, ts.getDatapoints().get(0));
    assertSame(d2, ts.getDatapoints().get(1));
    assertSame(d1, ts.getLastPreVisible());
    assertSame(d2, ts.getFirstVisible());

    // (null) (d1, d2) (null)
    lrange.clear();
    lrange.add(d1);
    lrange.add(d2);
    ts.setDatapoints(null, lrange, null);    

    assertEquals(2, ts.getDatapoints().size());
    assertEquals(1.0f, ts.getVisibleValueMin());
    assertEquals(2.0f, ts.getVisibleValueMax());
    assertNull(ts.getLastPreVisible());
    assertNotNull(ts.getFirstVisible());
    assertNotNull(ts.getLastVisible());
    assertNull(ts.getFirstPostVisible());
    assertSame(d1, ts.getDatapoints().get(0));
    assertSame(d2, ts.getDatapoints().get(1));
    assertSame(d1, ts.getFirstVisible());
    assertSame(d2, ts.getLastVisible());

    // (null) (d1, d2, d3) (null)
    lrange.clear();
    lrange.add(d1);
    lrange.add(d2);
    lrange.add(d3);
    ts.setDatapoints(null, lrange, null);    

    assertEquals(3, ts.getDatapoints().size());
    assertEquals(1.0f, ts.getVisibleValueMin());
    assertEquals(3.0f, ts.getVisibleValueMax());
    assertNull(ts.getLastPreVisible());
    assertNotNull(ts.getFirstVisible());
    assertNotNull(ts.getLastVisible());
    assertNull(ts.getFirstPostVisible());
    assertSame(d1, ts.getDatapoints().get(0));
    assertSame(d2, ts.getDatapoints().get(1));
    assertSame(d3, ts.getDatapoints().get(2));
    assertSame(d1, ts.getFirstVisible());
    assertSame(d3, ts.getLastVisible());

    // (null) (d1) (d2)
    lrange.clear();
    lrange.add(d1);
    lpost.clear();
    lpost.add(d2);
    ts.setDatapoints(null, lrange, lpost);    

    assertEquals(2, ts.getDatapoints().size());
    assertEquals(1.0f, ts.getVisibleValueMin());
    assertEquals(2.0f, ts.getVisibleValueMax()); // due to offscreen interpolation
    assertNull(ts.getLastPreVisible());
    assertNotNull(ts.getFirstVisible());
    assertNotNull(ts.getLastVisible());
    assertNotNull(ts.getFirstPostVisible());
    assertSame(d1, ts.getDatapoints().get(0));
    assertSame(d2, ts.getDatapoints().get(1));
    assertSame(d1, ts.getFirstVisible());
    assertSame(d1, ts.getLastVisible());
    assertSame(d2, ts.getFirstPostVisible());

    // (null) (null) (d1, d2)
    lpost.clear();
    lpost.add(d1);
    lpost.add(d2);
    ts.setDatapoints(null, null, lpost);    

    assertEquals(2, ts.getDatapoints().size());
    assertEquals(Float.MAX_VALUE, ts.getVisibleValueMin());
    assertEquals(Float.MIN_VALUE, ts.getVisibleValueMax());
    assertNull(ts.getLastPreVisible());
    assertNull(ts.getFirstVisible());
    assertNull(ts.getLastVisible());
    assertNotNull(ts.getFirstPostVisible());
    assertSame(d1, ts.getDatapoints().get(0));
    assertSame(d2, ts.getDatapoints().get(1));
    assertSame(d1, ts.getFirstPostVisible());

    // (d1) (d2) (d3)
    lpre.clear();
    lpre.add(d1);
    lrange.clear();
    lrange.add(d2);
    lpost.clear();
    lpost.add(d3);
    ts.setDatapoints(lpre, lrange, lpost);    

    assertEquals(3, ts.getDatapoints().size());
    assertEquals(1.0f, ts.getVisibleValueMin()); // due to offscreen interpolation
    assertEquals(3.0f, ts.getVisibleValueMax()); // due to offscreen interpolation
    assertNotNull(ts.getLastPreVisible());
    assertNotNull(ts.getFirstVisible());
    assertNotNull(ts.getLastVisible());
    assertNotNull(ts.getFirstPostVisible());
    assertSame(d1, ts.getDatapoints().get(0));
    assertSame(d2, ts.getDatapoints().get(1));
    assertSame(d3, ts.getDatapoints().get(2));
    assertSame(d1, ts.getLastPreVisible());
    assertSame(d2, ts.getFirstVisible());
    assertSame(d2, ts.getLastVisible());
    assertSame(d3, ts.getFirstPostVisible());

    // (null) (d1, d3, d2) (null)
    // Note that timeseries don't enforce ordering!
    lrange.clear();
    lrange.add(d1);
    lrange.add(d3);
    lrange.add(d2);
    ts.setDatapoints(null, lrange, null);    

    assertEquals(3, ts.getDatapoints().size());
    assertEquals(1.0f, ts.getVisibleValueMin());
    assertEquals(3.0f, ts.getVisibleValueMax());
    assertNull(ts.getLastPreVisible());
    assertNotNull(ts.getFirstVisible());
    assertNotNull(ts.getLastVisible());
    assertNull(ts.getFirstPostVisible());
    assertSame(d1, ts.getDatapoints().get(0));
    assertSame(d3, ts.getDatapoints().get(1));
    assertSame(d2, ts.getDatapoints().get(2));
    assertSame(d1, ts.getFirstVisible());
    assertSame(d2, ts.getLastVisible());
  }
  
  
  public void testFindNeighbor() {
    ArrayList<Datapoint> range = new ArrayList<Datapoint>();
    Datapoint d1 = new Datapoint(100L, 1.0f, 1, 10, 1);
    Datapoint d2 = new Datapoint(150L, 2.0f, 1, 11, 1);
    Datapoint d3 = new Datapoint(250L, 3.0f, 1, 11, 1);
    Datapoint result;
   
    TimeSeries ts = newDefaultTimeSeries();

    // 1-item list
    range.add(d1);
    ts.setDatapoints(null, range, null);    

    result = ts.findPreNeighbor(0);
    assertNull(result);
    result = ts.findPostNeighbor(0);
    assertNotNull(result);
    assertSame(d1, result);

    result = ts.findPreNeighbor(100);
    assertNotNull(result);
    assertSame(d1, result);
    result = ts.findPostNeighbor(100);
    assertNotNull(result);
    assertSame(d1, result);

    result = ts.findPreNeighbor(110);
    assertNotNull(result);
    assertSame(d1, result);
    result = ts.findPostNeighbor(110);
    assertNull(result);
    
    // 2-item list
    range.add(d2);
    ts.setDatapoints(null, range, null);    

    result = ts.findPreNeighbor(0);
    assertNull(result);
    result = ts.findPostNeighbor(0);
    assertNotNull(result);
    assertSame(d1, result);

    result = ts.findPreNeighbor(100);
    assertNotNull(result);
    assertSame(d1, result);
    result = ts.findPostNeighbor(100);
    assertNotNull(result);
    assertSame(d1, result);

    result = ts.findPreNeighbor(125);
    assertNotNull(result);
    assertSame(d1, result);
    result = ts.findPostNeighbor(125);
    assertNotNull(result);
    assertSame(d2, result);

    result = ts.findPreNeighbor(150);
    assertNotNull(result);
    assertSame(d2, result);
    result = ts.findPostNeighbor(150);
    assertNotNull(result);
    assertSame(d2, result);

    result = ts.findPreNeighbor(200);
    assertNotNull(result);
    assertSame(d2, result);
    result = ts.findPostNeighbor(200);
    assertNull(result);
    
    // 3-item list
    range.add(d3);
    ts.setDatapoints(null, range, null);    

    result = ts.findPreNeighbor(0);
    assertNull(result);
    result = ts.findPreNeighbor(99);
    assertNull(result);
    result = ts.findPreNeighbor(100);
    assertNotNull(result);
    assertSame(d1, result);
    result = ts.findPreNeighbor(101);
    assertNotNull(result);
    assertSame(d1, result);    
    result = ts.findPreNeighbor(149);
    assertNotNull(result);
    assertSame(d1, result);
    result = ts.findPreNeighbor(150);
    assertNotNull(result);
    assertSame(d2, result);
    result = ts.findPreNeighbor(151);
    assertNotNull(result);
    assertSame(d2, result);
    result = ts.findPreNeighbor(249);
    assertNotNull(result);
    assertSame(d2, result);
    result = ts.findPreNeighbor(250);
    assertNotNull(result);
    assertSame(d3, result);
    result = ts.findPreNeighbor(251);
    assertNotNull(result);
    assertSame(d3, result);
    

    result = ts.findPostNeighbor(0);
    assertNotNull(result);
    assertSame(d1, result);    
    result = ts.findPostNeighbor(99);
    assertNotNull(result);
    assertSame(d1, result);    
    result = ts.findPostNeighbor(100);
    assertNotNull(result);
    assertSame(d1, result);
    result = ts.findPostNeighbor(101);
    assertNotNull(result);
    assertSame(d2, result);    
    result = ts.findPostNeighbor(149);
    assertNotNull(result);
    assertSame(d2, result);
    result = ts.findPostNeighbor(150);
    assertNotNull(result);
    assertSame(d2, result);
    result = ts.findPostNeighbor(151);
    assertNotNull(result);
    assertSame(d3, result);
    result = ts.findPostNeighbor(249);
    assertNotNull(result);
    assertSame(d3, result);
    result = ts.findPostNeighbor(250);
    assertNotNull(result);
    assertSame(d3, result);
    result = ts.findPostNeighbor(251);
    assertNull(result);    
  }
  
  private TimeSeries arithOpTimeSeriesSource1() {
    ArrayList<Datapoint> range = new ArrayList<Datapoint>();
    Datapoint d1 = new Datapoint(100L, 1.0f, 1, 10, 1);
    Datapoint d2 = new Datapoint(150L, 2.0f, 1, 11, 1);
    Datapoint d3 = new Datapoint(250L, 4.0f, 1, 11, 1);
    range.add(d1);
    range.add(d2);
    range.add(d3);

    TimeSeries ts = newDefaultTimeSeries();
    ts.setDatapoints(null, range, null);    
    return ts;
  }

  private TimeSeries arithOpTimeSeriesSource2Strict() {
    ArrayList<Datapoint> range = new ArrayList<Datapoint>();
    Datapoint d1 = new Datapoint(100L, 2.0f, 1, 10, 1);
    Datapoint d2 = new Datapoint(150L, 4.0f, 1, 11, 1);
    Datapoint d3 = new Datapoint(250L, 8.0f, 1, 11, 1);
    range.add(d1);
    range.add(d2);
    range.add(d3);

    TimeSeries ts = newDefaultTimeSeries();
    ts.setDatapoints(null, range, null);    
    return ts;
  }

  private TimeSeries arithOpTimeSeriesSource2Loose() {
    ArrayList<Datapoint> range = new ArrayList<Datapoint>();
    Datapoint d1 = new Datapoint( 99L, 2.0f, 1, 10, 1);
    Datapoint d2 = new Datapoint(151L, 4.0f, 1, 11, 1);
    Datapoint d3 = new Datapoint(249L, 8.0f, 1, 11, 1);
    range.add(d1);
    range.add(d2);
    range.add(d3);

    TimeSeries ts = newDefaultTimeSeries();
    ts.setDatapoints(null, range, null);    
    return ts;
  }

  public void testFloatOp() {
    TimeSeries ts;
    
    ts = arithOpTimeSeriesSource1();
    ts.plusPre(2.0f);
    assertEquals(3.0f, ts.getDatapoints().get(0).mValue.y);
    assertEquals(4.0f, ts.getDatapoints().get(1).mValue.y);
    assertEquals(6.0f, ts.getDatapoints().get(2).mValue.y);

    ts = arithOpTimeSeriesSource1();
    ts.minusPre(2.0f);
    assertEquals( 1.0f, ts.getDatapoints().get(0).mValue.y);
    assertEquals( 0.0f, ts.getDatapoints().get(1).mValue.y);
    assertEquals(-2.0f, ts.getDatapoints().get(2).mValue.y);

    ts = arithOpTimeSeriesSource1();
    ts.multiplyPre(2.0f);
    assertEquals(2.0f, ts.getDatapoints().get(0).mValue.y);
    assertEquals(4.0f, ts.getDatapoints().get(1).mValue.y);
    assertEquals(8.0f, ts.getDatapoints().get(2).mValue.y);

    ts = arithOpTimeSeriesSource1();
    ts.dividePre(2.0f);
    assertEquals(2.0f, ts.getDatapoints().get(0).mValue.y);
    assertEquals(1.0f, ts.getDatapoints().get(1).mValue.y);
    assertEquals(0.5f, ts.getDatapoints().get(2).mValue.y);

    ts = arithOpTimeSeriesSource1();
    ts.plusPost(2.0f);
    assertEquals(3.0f, ts.getDatapoints().get(0).mValue.y);
    assertEquals(4.0f, ts.getDatapoints().get(1).mValue.y);
    assertEquals(6.0f, ts.getDatapoints().get(2).mValue.y);

    ts = arithOpTimeSeriesSource1();
    ts.minusPost(2.0f);
    assertEquals(-1.0f, ts.getDatapoints().get(0).mValue.y);
    assertEquals( 0.0f, ts.getDatapoints().get(1).mValue.y);
    assertEquals( 2.0f, ts.getDatapoints().get(2).mValue.y);

    ts = arithOpTimeSeriesSource1();
    ts.multiplyPost(2.0f);
    assertEquals(2.0f, ts.getDatapoints().get(0).mValue.y);
    assertEquals(4.0f, ts.getDatapoints().get(1).mValue.y);
    assertEquals(8.0f, ts.getDatapoints().get(2).mValue.y);

    ts = arithOpTimeSeriesSource1();
    ts.dividePost(2.0f);
    assertEquals(0.5f, ts.getDatapoints().get(0).mValue.y);
    assertEquals(1.0f, ts.getDatapoints().get(1).mValue.y);
    assertEquals(2.0f, ts.getDatapoints().get(2).mValue.y);
  }

  public void testLongOp() {
    TimeSeries ts;
    
    ts = arithOpTimeSeriesSource1();
    ts.plusPre(2L);
    assertEquals(3.0f, ts.getDatapoints().get(0).mValue.y);
    assertEquals(4.0f, ts.getDatapoints().get(1).mValue.y);
    assertEquals(6.0f, ts.getDatapoints().get(2).mValue.y);

    ts = arithOpTimeSeriesSource1();
    ts.minusPre(2L);
    assertEquals( 1.0f, ts.getDatapoints().get(0).mValue.y);
    assertEquals( 0.0f, ts.getDatapoints().get(1).mValue.y);
    assertEquals(-2.0f, ts.getDatapoints().get(2).mValue.y);

    ts = arithOpTimeSeriesSource1();
    ts.multiplyPre(2L);
    assertEquals(2.0f, ts.getDatapoints().get(0).mValue.y);
    assertEquals(4.0f, ts.getDatapoints().get(1).mValue.y);
    assertEquals(8.0f, ts.getDatapoints().get(2).mValue.y);

    ts = arithOpTimeSeriesSource1();
    ts.dividePre(2L);
    assertEquals(2.0f, ts.getDatapoints().get(0).mValue.y);
    assertEquals(1.0f, ts.getDatapoints().get(1).mValue.y);
    assertEquals(0.5f, ts.getDatapoints().get(2).mValue.y);

    ts = arithOpTimeSeriesSource1();
    ts.plusPost(2L);
    assertEquals(3.0f, ts.getDatapoints().get(0).mValue.y);
    assertEquals(4.0f, ts.getDatapoints().get(1).mValue.y);
    assertEquals(6.0f, ts.getDatapoints().get(2).mValue.y);

    ts = arithOpTimeSeriesSource1();
    ts.minusPost(2L);
    assertEquals(-1.0f, ts.getDatapoints().get(0).mValue.y);
    assertEquals( 0.0f, ts.getDatapoints().get(1).mValue.y);
    assertEquals( 2.0f, ts.getDatapoints().get(2).mValue.y);

    ts = arithOpTimeSeriesSource1();
    ts.multiplyPost(2L);
    assertEquals(2.0f, ts.getDatapoints().get(0).mValue.y);
    assertEquals(4.0f, ts.getDatapoints().get(1).mValue.y);
    assertEquals(8.0f, ts.getDatapoints().get(2).mValue.y);

    ts = arithOpTimeSeriesSource1();
    ts.dividePost(2L);
    assertEquals(0.5f, ts.getDatapoints().get(0).mValue.y);
    assertEquals(1.0f, ts.getDatapoints().get(1).mValue.y);
    assertEquals(2.0f, ts.getDatapoints().get(2).mValue.y);
  }
  
  public void testPreviousOp() {
    TimeSeries ts;
    
    ts = arithOpTimeSeriesSource1();
    ts.previousValue();
    assertEquals(1.0f, ts.getDatapoints().get(0).mValue.y);
    assertEquals(1.0f, ts.getDatapoints().get(1).mValue.y);
    assertEquals(2.0f, ts.getDatapoints().get(2).mValue.y);

    ts = arithOpTimeSeriesSource1();
    ts.previousTimestamp();
    assertEquals(0.0f, ts.getDatapoints().get(0).mValue.y);
    assertEquals(50.0f, ts.getDatapoints().get(1).mValue.y);
    assertEquals(100.0f, ts.getDatapoints().get(2).mValue.y);    
  }

  private TimeSeries newPeriodOpTimeSeries(DateUtil.Period p, boolean divide) {
    ArrayList<Datapoint> range = new ArrayList<Datapoint>();
    Datapoint d1, d2, d3;

    long modifier = DateUtil.mapPeriodToLong(p);
    if (divide == true) {
      d1 = new Datapoint(100L, 100000.0f / modifier, 1, 10, 1);
      d2 = new Datapoint(150L, 200000.0f / modifier, 1, 11, 1);
      d3 = new Datapoint(250L, 400000.0f / modifier, 1, 11, 1);
    } else {
      d1 = new Datapoint(100L, 1.0f * modifier, 1, 10, 1);
      d2 = new Datapoint(150L, 2.0f * modifier, 1, 11, 1);
      d3 = new Datapoint(250L, 4.0f * modifier, 1, 11, 1);
    }
    range.add(d1);
    range.add(d2);
    range.add(d3);

    TimeSeries ts = newDefaultTimeSeries();
    ts.setDatapoints(null, range, null);    
    return ts;
  }
  
  public void testPeriodOp() {
    TimeSeries ts;

    ts = newPeriodOpTimeSeries(DateUtil.Period.MINUTE, false);
    ts.inPeriod(DateUtil.Period.MINUTE);
    assertEquals(1.0f, ts.getDatapoints().get(0).mValue.y);
    assertEquals(2.0f, ts.getDatapoints().get(1).mValue.y);
    assertEquals(4.0f, ts.getDatapoints().get(2).mValue.y);

    ts = newPeriodOpTimeSeries(DateUtil.Period.HOUR, false);
    ts.inPeriod(DateUtil.Period.HOUR);
    assertEquals(1.0f, ts.getDatapoints().get(0).mValue.y);
    assertEquals(2.0f, ts.getDatapoints().get(1).mValue.y);
    assertEquals(4.0f, ts.getDatapoints().get(2).mValue.y);

    ts = newPeriodOpTimeSeries(DateUtil.Period.AMPM, false);
    ts.inPeriod(DateUtil.Period.AMPM);
    assertEquals(1.0f, ts.getDatapoints().get(0).mValue.y);
    assertEquals(2.0f, ts.getDatapoints().get(1).mValue.y);
    assertEquals(4.0f, ts.getDatapoints().get(2).mValue.y);

    ts = newPeriodOpTimeSeries(DateUtil.Period.DAY, false);
    ts.inPeriod(DateUtil.Period.DAY);
    assertEquals(1.0f, ts.getDatapoints().get(0).mValue.y);
    assertEquals(2.0f, ts.getDatapoints().get(1).mValue.y);
    assertEquals(4.0f, ts.getDatapoints().get(2).mValue.y);

    ts = newPeriodOpTimeSeries(DateUtil.Period.WEEK, false);
    ts.inPeriod(DateUtil.Period.WEEK);
    assertEquals(1.0f, ts.getDatapoints().get(0).mValue.y);
    assertEquals(2.0f, ts.getDatapoints().get(1).mValue.y);
    assertEquals(4.0f, ts.getDatapoints().get(2).mValue.y);

    ts = newPeriodOpTimeSeries(DateUtil.Period.MONTH, false);
    ts.inPeriod(DateUtil.Period.MONTH);
    assertEquals(1.0f, ts.getDatapoints().get(0).mValue.y);
    assertEquals(2.0f, ts.getDatapoints().get(1).mValue.y);
    assertEquals(4.0f, ts.getDatapoints().get(2).mValue.y);

    ts = newPeriodOpTimeSeries(DateUtil.Period.QUARTER, false);
    ts.inPeriod(DateUtil.Period.QUARTER);
    assertEquals(1.0f, ts.getDatapoints().get(0).mValue.y);
    assertEquals(2.0f, ts.getDatapoints().get(1).mValue.y);
    assertEquals(4.0f, ts.getDatapoints().get(2).mValue.y);

    ts = newPeriodOpTimeSeries(DateUtil.Period.YEAR, false);
    ts.inPeriod(DateUtil.Period.YEAR);
    assertEquals(1.0f, ts.getDatapoints().get(0).mValue.y);
    assertEquals(2.0f, ts.getDatapoints().get(1).mValue.y);
    assertEquals(4.0f, ts.getDatapoints().get(2).mValue.y);

    
    ts = newPeriodOpTimeSeries(DateUtil.Period.MINUTE, true);
    ts.asPeriod(DateUtil.Period.MINUTE);
    assertEquals(100000.0f, ts.getDatapoints().get(0).mValue.y);
    assertEquals(200000.0f, ts.getDatapoints().get(1).mValue.y);
    assertEquals(400000.0f, ts.getDatapoints().get(2).mValue.y);

    ts = newPeriodOpTimeSeries(DateUtil.Period.HOUR, true);
    ts.asPeriod(DateUtil.Period.HOUR);
    assertEquals(100000.0f, ts.getDatapoints().get(0).mValue.y);
    assertEquals(200000.0f, ts.getDatapoints().get(1).mValue.y);
    assertEquals(400000.0f, ts.getDatapoints().get(2).mValue.y);

    ts = newPeriodOpTimeSeries(DateUtil.Period.AMPM, true);
    ts.asPeriod(DateUtil.Period.AMPM);
    assertEquals(100000.0f, ts.getDatapoints().get(0).mValue.y);
    assertEquals(200000.0f, ts.getDatapoints().get(1).mValue.y);
    assertEquals(400000.0f, ts.getDatapoints().get(2).mValue.y);

    ts = newPeriodOpTimeSeries(DateUtil.Period.DAY, true);
    ts.asPeriod(DateUtil.Period.DAY);
    assertEquals(100000.0f, ts.getDatapoints().get(0).mValue.y);
    assertEquals(200000.0f, ts.getDatapoints().get(1).mValue.y);
    assertEquals(400000.0f, ts.getDatapoints().get(2).mValue.y);

    ts = newPeriodOpTimeSeries(DateUtil.Period.WEEK, true);
    ts.asPeriod(DateUtil.Period.WEEK);
    assertEquals(100000.0f, ts.getDatapoints().get(0).mValue.y);
    assertEquals(200000.0f, ts.getDatapoints().get(1).mValue.y);
    assertEquals(400000.0f, ts.getDatapoints().get(2).mValue.y);

    ts = newPeriodOpTimeSeries(DateUtil.Period.MONTH, true);
    ts.asPeriod(DateUtil.Period.MONTH);
    assertEquals(100000.0f, ts.getDatapoints().get(0).mValue.y);
    assertEquals(200000.0f, ts.getDatapoints().get(1).mValue.y);
    assertEquals(400000.0f, ts.getDatapoints().get(2).mValue.y);

    ts = newPeriodOpTimeSeries(DateUtil.Period.QUARTER, true);
    ts.asPeriod(DateUtil.Period.QUARTER);
    assertEquals(100000.0f, ts.getDatapoints().get(0).mValue.y);
    assertEquals(200000.0f, ts.getDatapoints().get(1).mValue.y);
    assertEquals(400000.0f, ts.getDatapoints().get(2).mValue.y);

    ts = newPeriodOpTimeSeries(DateUtil.Period.YEAR, true);
    ts.asPeriod(DateUtil.Period.YEAR);
    assertEquals(100000.0f, ts.getDatapoints().get(0).mValue.y);
    assertEquals(200000.0f, ts.getDatapoints().get(1).mValue.y);
    assertEquals(400000.0f, ts.getDatapoints().get(2).mValue.y);
  }

  public void testTimeSeriesOp() {
  }

  // These will be tested in Number.* unittests:
  //   testCalcStatsAndBounds()
  //   testRecalcStatsAndBounds()
  
  
}

