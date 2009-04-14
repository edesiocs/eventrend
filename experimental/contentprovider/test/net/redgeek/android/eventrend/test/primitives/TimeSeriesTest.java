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

import junit.framework.TestCase;

import net.redgeek.android.eventrecorder.CategoryDbTable;
import net.redgeek.android.eventrend.graph.plugins.LinearInterpolator;
import net.redgeek.android.eventrend.primitives.Datapoint;
import net.redgeek.android.eventrend.primitives.TimeSeries;
import net.redgeek.android.eventrend.test.common.MockTimeSeriesPainter;
import net.redgeek.android.eventrend.util.DateUtil;

import java.util.ArrayList;

// Note that several tests use equality comparison on float, which could be 
// dangerous in general, but should be safe for such small predefined values.
public class TimeSeriesTest extends TestCase {
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
    assertEquals(Long.MAX_VALUE, ts.getVisibleTimestampMin());
    assertEquals(Long.MIN_VALUE, ts.getVisibleTimestampMax());
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
    ArrayList<Datapoint> lpre = new ArrayList<Datapoint>();
    ArrayList<Datapoint> lrange = new ArrayList<Datapoint>();
    ArrayList<Datapoint> lpost = new ArrayList<Datapoint>();
    Datapoint d1 = new Datapoint(100L, 1.0f, 1, 10, 1);
    Datapoint d2 = new Datapoint(150L, 2.0f, 1, 11, 1);
    Datapoint d3 = new Datapoint(250L, 3.0f, 1, 11, 1);

    TimeSeries ts = newDefaultTimeSeries();

    // (null) (null) (null)
    ts.setDatapoints(null, null, null, true);
    assertEquals(0, ts.getDatapoints().size());
    assertEquals(Float.MAX_VALUE, ts.getVisibleValueMin());
    assertEquals(Float.MIN_VALUE, ts.getVisibleValueMax());
    assertEquals(Long.MAX_VALUE, ts.getVisibleTimestampMin());
    assertEquals(Long.MIN_VALUE, ts.getVisibleTimestampMax());
    assertNull(ts.getFirstVisible());
    assertNull(ts.getLastVisible());
    assertNull(ts.getFirstPostVisible());
    assertNull(ts.getLastPreVisible());

    // (d1) (null) (null)
    lpre.clear();
    lpre.add(d1);
    ts.setDatapoints(lpre, null, null, true);

    assertEquals(1, ts.getDatapoints().size());
    assertEquals(Float.MAX_VALUE, ts.getVisibleValueMin());
    assertEquals(Float.MIN_VALUE, ts.getVisibleValueMax());
    assertEquals(Long.MAX_VALUE, ts.getVisibleTimestampMin());
    assertEquals(Long.MIN_VALUE, ts.getVisibleTimestampMax());
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
    ts.setDatapoints(lpre, null, null, true);

    assertEquals(2, ts.getDatapoints().size());
    assertEquals(Float.MAX_VALUE, ts.getVisibleValueMin());
    assertEquals(Float.MIN_VALUE, ts.getVisibleValueMax());
    assertEquals(Long.MAX_VALUE, ts.getVisibleTimestampMin());
    assertEquals(Long.MIN_VALUE, ts.getVisibleTimestampMax());
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
    ts.setDatapoints(lpre, lrange, null, true);

    assertEquals(2, ts.getDatapoints().size());
    assertEquals(1.0f, ts.getVisibleValueMin()); // due to offscreen
                                                 // interpolation
    assertEquals(2.0f, ts.getVisibleValueMax());
    assertEquals(150L, ts.getVisibleTimestampMin());
    assertEquals(150L, ts.getVisibleTimestampMax());
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
    ts.setDatapoints(null, lrange, null, true);

    assertEquals(2, ts.getDatapoints().size());
    assertEquals(1.0f, ts.getVisibleValueMin());
    assertEquals(2.0f, ts.getVisibleValueMax());
    assertEquals(100L, ts.getVisibleTimestampMin());
    assertEquals(150L, ts.getVisibleTimestampMax());
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
    ts.setDatapoints(null, lrange, null, true);

    assertEquals(3, ts.getDatapoints().size());
    assertEquals(1.0f, ts.getVisibleValueMin());
    assertEquals(3.0f, ts.getVisibleValueMax());
    assertEquals(100L, ts.getVisibleTimestampMin());
    assertEquals(250L, ts.getVisibleTimestampMax());
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
    ts.setDatapoints(null, lrange, lpost, true);

    assertEquals(2, ts.getDatapoints().size());
    assertEquals(1.0f, ts.getVisibleValueMin());
    assertEquals(2.0f, ts.getVisibleValueMax()); // due to offscreen
                                                 // interpolation
    assertEquals(100L, ts.getVisibleTimestampMin());
    assertEquals(100L, ts.getVisibleTimestampMax());
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
    ts.setDatapoints(null, null, lpost, true);

    assertEquals(2, ts.getDatapoints().size());
    assertEquals(Float.MAX_VALUE, ts.getVisibleValueMin());
    assertEquals(Float.MIN_VALUE, ts.getVisibleValueMax());
    assertEquals(Long.MAX_VALUE, ts.getVisibleTimestampMin());
    assertEquals(Long.MIN_VALUE, ts.getVisibleTimestampMax());
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
    ts.setDatapoints(lpre, lrange, lpost, true);

    assertEquals(3, ts.getDatapoints().size());
    assertEquals(1.0f, ts.getVisibleValueMin()); // due to offscreen
                                                 // interpolation
    assertEquals(3.0f, ts.getVisibleValueMax()); // due to offscreen
                                                 // interpolation
    assertEquals(150L, ts.getVisibleTimestampMin());
    assertEquals(150L, ts.getVisibleTimestampMax());
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
    ts.setDatapoints(null, lrange, null, true);

    assertEquals(3, ts.getDatapoints().size());
    assertEquals(1.0f, ts.getVisibleValueMin());
    assertEquals(3.0f, ts.getVisibleValueMax());
    assertEquals(100L, ts.getVisibleTimestampMin());
    assertEquals(250L, ts.getVisibleTimestampMax());
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
    ts.setDatapoints(null, range, null, true);

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
    ts.setDatapoints(null, range, null, true);

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
    ts.setDatapoints(null, range, null, true);

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

    // closely packed
    d1 = new Datapoint(100L, 1.0f, 1, 10, 1);
    d2 = new Datapoint(101L, 2.0f, 1, 11, 1);
    d3 = new Datapoint(102L, 3.0f, 1, 11, 1);
    range.clear();
    range.add(d1);
    range.add(d2);
    range.add(d3);

    ts.setDatapoints(null, range, null, true);
    result = ts.findPreNeighbor(99);
    assertNull(result);
    result = ts.findPostNeighbor(99);
    assertNotNull(result);
    assertSame(d1, result);

    result = ts.findPreNeighbor(100);
    assertNotNull(result);
    assertSame(d1, result);
    result = ts.findPostNeighbor(100);
    assertNotNull(result);
    assertSame(d1, result);

    result = ts.findPreNeighbor(101);
    assertNotNull(result);
    assertSame(d2, result);
    result = ts.findPostNeighbor(101);
    assertNotNull(result);
    assertSame(d2, result);

    result = ts.findPreNeighbor(102);
    assertNotNull(result);
    assertSame(d3, result);
    result = ts.findPostNeighbor(102);
    assertNotNull(result);
    assertSame(d3, result);

    result = ts.findPreNeighbor(103);
    assertNotNull(result);
    assertSame(d3, result);
    result = ts.findPostNeighbor(103);
    assertNull(result);
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
    assertEquals(1.0f, ts.getDatapoints().get(0).mValue.y);
    assertEquals(0.0f, ts.getDatapoints().get(1).mValue.y);
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
    assertEquals(0.0f, ts.getDatapoints().get(1).mValue.y);
    assertEquals(2.0f, ts.getDatapoints().get(2).mValue.y);

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
    assertEquals(1.0f, ts.getDatapoints().get(0).mValue.y);
    assertEquals(0.0f, ts.getDatapoints().get(1).mValue.y);
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
    assertEquals(0.0f, ts.getDatapoints().get(1).mValue.y);
    assertEquals(2.0f, ts.getDatapoints().get(2).mValue.y);

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
    ts.setDatapoints(null, range, null, true);
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

  public void testInterpolateValue() {
    TimeSeries ts;

    ts = arithOpTimeSeriesSource1();

    assertEquals(1.0f, ts.interpolateValue(100L));
    assertEquals(2.0f, ts.interpolateValue(150L));
    assertEquals(4.0f, ts.interpolateValue(250L));

    assertNull(ts.interpolateValue(99L));
    assertEquals(1.5f, ts.interpolateValue(125L));
    assertEquals(3.0f, ts.interpolateValue(200L));
    assertNull(ts.interpolateValue(251L));
  }

  private TimeSeries arithOpTimeSeriesSource1() {
    LinearInterpolator i = new LinearInterpolator();
    ArrayList<Datapoint> range = new ArrayList<Datapoint>();
    Datapoint d1 = new Datapoint(100L, 1.0f, 1, 10, 1);
    Datapoint d2 = new Datapoint(150L, 2.0f, 1, 11, 1);
    Datapoint d3 = new Datapoint(250L, 4.0f, 1, 11, 1);
    range.add(d1);
    range.add(d2);
    range.add(d3);

    TimeSeries ts = newDefaultTimeSeries();
    ts.setDatapoints(null, range, null, true);
    ts.setInterpolator(i);
    return ts;
  }

  private TimeSeries arithOpTimeSeriesSource2Matching() {
    LinearInterpolator i = new LinearInterpolator();
    ArrayList<Datapoint> range = new ArrayList<Datapoint>();
    Datapoint d1 = new Datapoint(100L, 2.0f, 1, 10, 1);
    Datapoint d2 = new Datapoint(150L, 4.0f, 1, 11, 1);
    Datapoint d3 = new Datapoint(250L, 8.0f, 1, 11, 1);
    range.add(d1);
    range.add(d2);
    range.add(d3);

    TimeSeries ts = newDefaultTimeSeries();
    ts.setDatapoints(null, range, null, true);
    ts.setInterpolator(i);
    return ts;
  }

  private TimeSeries arithOpTimeSeriesSource2Offset() {
    LinearInterpolator i = new LinearInterpolator();
    ArrayList<Datapoint> range = new ArrayList<Datapoint>();
    Datapoint d1 = new Datapoint(125L, 2.0f, 1, 10, 1);
    Datapoint d2 = new Datapoint(200L, 4.0f, 1, 11, 1);
    Datapoint d3 = new Datapoint(300L, 8.0f, 1, 11, 1);
    range.add(d1);
    range.add(d2);
    range.add(d3);

    TimeSeries ts = newDefaultTimeSeries();
    ts.setDatapoints(null, range, null, true);
    ts.setInterpolator(i);
    return ts;
  }

  private float interp(long x1, float y1, long x2, float y2, long atX) {
    return y1 + ((y2 - y1) / (x2 - x1)) * (atX - x1);
  }

  public void testTimeSeriesPlusOp() {
    TimeSeries ts1, ts2;
    float y[] = new float[6];

    // ts1 + ts2 (matching timestamps)
    ts1 = arithOpTimeSeriesSource1();
    ts2 = arithOpTimeSeriesSource2Matching();
    ts1.timeseriesPlus(ts2);
    assertEquals(3.0f, ts1.getDatapoints().get(0).mValue.y);
    assertEquals(6.0f, ts1.getDatapoints().get(1).mValue.y);
    assertEquals(12.0f, ts1.getDatapoints().get(2).mValue.y);

    // ts2 + ts1 (matching timestamps)
    ts1 = arithOpTimeSeriesSource1();
    ts2 = arithOpTimeSeriesSource2Matching();
    ts2.timeseriesPlus(ts1);
    assertEquals(3.0f, ts2.getDatapoints().get(0).mValue.y);
    assertEquals(6.0f, ts2.getDatapoints().get(1).mValue.y);
    assertEquals(12.0f, ts2.getDatapoints().get(2).mValue.y);

    // ts1 + ts2 (offset timestamps)
    // time: 100 125 150 200 250 300
    // val1: 1 2 4
    // val2: 2 4 8
    ts1 = arithOpTimeSeriesSource1();
    ts2 = arithOpTimeSeriesSource2Offset();
    y[0] = 1.0f;
    y[1] = interp(100, 1.0f, 150, 2.0f, 125) + 2.0f;
    y[2] = 2.0f + interp(125, 2.0f, 200, 4.0f, 150);
    y[3] = interp(150, 2.0f, 250, 4.0f, 200) + 4.0f;
    y[4] = 4.0f + interp(200, 4.0f, 300, 8.0f, 250);
    y[5] = 8.0f;

    ts1.timeseriesPlus(ts2);
    assertEquals(6, ts1.getDatapoints().size());
    assertEquals(y[0], ts1.getDatapoints().get(0).mValue.y);
    assertEquals(y[1], ts1.getDatapoints().get(1).mValue.y);
    assertEquals(y[2], ts1.getDatapoints().get(2).mValue.y);
    assertEquals(y[3], ts1.getDatapoints().get(3).mValue.y);
    assertEquals(y[4], ts1.getDatapoints().get(4).mValue.y);
    assertEquals(y[5], ts1.getDatapoints().get(5).mValue.y);

    // ts2 + ts1 (offset timestamps)
    ts1 = arithOpTimeSeriesSource1();
    ts2 = arithOpTimeSeriesSource2Offset();
    y[0] = 1.0f;
    y[1] = 2.0f + interp(100, 1.0f, 150, 2.0f, 125);
    y[2] = interp(125, 2.0f, 200, 4.0f, 150) + 2.0f;
    y[3] = 4.0f + interp(150, 2.0f, 250, 4.0f, 200);
    y[4] = interp(200, 4.0f, 300, 8.0f, 250) + 4.0f;
    y[5] = 8.0f;

    ts1 = arithOpTimeSeriesSource1();
    ts2 = arithOpTimeSeriesSource2Offset();
    ts2.timeseriesPlus(ts1);
    assertEquals(6, ts2.getDatapoints().size());
    assertEquals(y[0], ts2.getDatapoints().get(0).mValue.y);
    assertEquals(y[1], ts2.getDatapoints().get(1).mValue.y);
    assertEquals(y[2], ts2.getDatapoints().get(2).mValue.y);
    assertEquals(y[3], ts2.getDatapoints().get(3).mValue.y);
    assertEquals(y[4], ts2.getDatapoints().get(4).mValue.y);
    assertEquals(y[5], ts2.getDatapoints().get(5).mValue.y);
  }

  public void testTimeSeriesMinusOp() {
    TimeSeries ts1, ts2;
    float y[] = new float[6];

    // ts1 - ts2 (matching timestamps)
    ts1 = arithOpTimeSeriesSource1();
    ts2 = arithOpTimeSeriesSource2Matching();
    ts1.timeseriesMinus(ts2);
    assertEquals(-1.0f, ts1.getDatapoints().get(0).mValue.y);
    assertEquals(-2.0f, ts1.getDatapoints().get(1).mValue.y);
    assertEquals(-4.0f, ts1.getDatapoints().get(2).mValue.y);

    // ts2 - ts1 (matching timestamps)
    ts1 = arithOpTimeSeriesSource1();
    ts2 = arithOpTimeSeriesSource2Matching();
    ts2.timeseriesMinus(ts1);
    assertEquals(1.0f, ts2.getDatapoints().get(0).mValue.y);
    assertEquals(2.0f, ts2.getDatapoints().get(1).mValue.y);
    assertEquals(4.0f, ts2.getDatapoints().get(2).mValue.y);

    // ts1 - ts2 (offset timestamps)
    // time: 100 125 150 200 250 300
    // val1: 1 2 4
    // val2: 2 4 8
    ts1 = arithOpTimeSeriesSource1();
    ts2 = arithOpTimeSeriesSource2Offset();
    y[0] = 1.0f;
    y[1] = interp(100, 1.0f, 150, 2.0f, 125) - 2.0f;
    y[2] = 2.0f - interp(125, 2.0f, 200, 4.0f, 150);
    y[3] = interp(150, 2.0f, 250, 4.0f, 200) - 4.0f;
    y[4] = 4.0f - interp(200, 4.0f, 300, 8.0f, 250);
    y[5] = -8.0f;

    ts1.timeseriesMinus(ts2);
    assertEquals(6, ts1.getDatapoints().size());
    assertEquals(y[0], ts1.getDatapoints().get(0).mValue.y);
    assertEquals(y[1], ts1.getDatapoints().get(1).mValue.y);
    assertEquals(y[2], ts1.getDatapoints().get(2).mValue.y);
    assertEquals(y[3], ts1.getDatapoints().get(3).mValue.y);
    assertEquals(y[4], ts1.getDatapoints().get(4).mValue.y);
    assertEquals(y[5], ts1.getDatapoints().get(5).mValue.y);

    // ts2 - ts1 (offset timestamps)
    ts1 = arithOpTimeSeriesSource1();
    ts2 = arithOpTimeSeriesSource2Offset();
    y[0] = -1.0f;
    y[1] = 2.0f - interp(100, 1.0f, 150, 2.0f, 125);
    y[2] = interp(125, 2.0f, 200, 4.0f, 150) - 2.0f;
    y[3] = 4.0f - interp(150, 2.0f, 250, 4.0f, 200);
    y[4] = interp(200, 4.0f, 300, 8.0f, 250) - 4.0f;
    y[5] = 8.0f;

    ts1 = arithOpTimeSeriesSource1();
    ts2 = arithOpTimeSeriesSource2Offset();
    ts2.timeseriesMinus(ts1);
    assertEquals(6, ts2.getDatapoints().size());
    assertEquals(y[0], ts2.getDatapoints().get(0).mValue.y);
    assertEquals(y[1], ts2.getDatapoints().get(1).mValue.y);
    assertEquals(y[2], ts2.getDatapoints().get(2).mValue.y);
    assertEquals(y[3], ts2.getDatapoints().get(3).mValue.y);
    assertEquals(y[4], ts2.getDatapoints().get(4).mValue.y);
    assertEquals(y[5], ts2.getDatapoints().get(5).mValue.y);
  }

  public void testTimeSeriesMultiplyOp() {
    TimeSeries ts1, ts2;
    float y[] = new float[6];

    // ts1 * ts2 (matching timestamps)
    ts1 = arithOpTimeSeriesSource1();
    ts2 = arithOpTimeSeriesSource2Matching();
    ts1.timeseriesMultiply(ts2);
    assertEquals(2.0f, ts1.getDatapoints().get(0).mValue.y);
    assertEquals(8.0f, ts1.getDatapoints().get(1).mValue.y);
    assertEquals(32.0f, ts1.getDatapoints().get(2).mValue.y);

    // ts2 * ts1 (matching timestamps)
    ts1 = arithOpTimeSeriesSource1();
    ts2 = arithOpTimeSeriesSource2Matching();
    ts2.timeseriesMultiply(ts1);
    assertEquals(2.0f, ts2.getDatapoints().get(0).mValue.y);
    assertEquals(8.0f, ts2.getDatapoints().get(1).mValue.y);
    assertEquals(32.0f, ts2.getDatapoints().get(2).mValue.y);

    // ts1 * ts2 (offset timestamps)
    // time: 100 125 150 200 250 300
    // val1: 1 2 4
    // val2: 2 4 8
    ts1 = arithOpTimeSeriesSource1();
    ts2 = arithOpTimeSeriesSource2Offset();
    y[0] = interp(100, 1.0f, 150, 2.0f, 125) * 2.0f;
    y[1] = 2.0f * interp(125, 2.0f, 200, 4.0f, 150);
    y[2] = interp(150, 2.0f, 250, 4.0f, 200) * 4.0f;
    y[3] = 4.0f * interp(200, 4.0f, 300, 8.0f, 250);

    ts1.timeseriesMultiply(ts2);
    assertEquals(4, ts1.getDatapoints().size());
    assertEquals(y[0], ts1.getDatapoints().get(0).mValue.y);
    assertEquals(y[1], ts1.getDatapoints().get(1).mValue.y);
    assertEquals(y[2], ts1.getDatapoints().get(2).mValue.y);
    assertEquals(y[3], ts1.getDatapoints().get(3).mValue.y);

    // ts2 * ts1 (offset timestamps)
    ts1 = arithOpTimeSeriesSource1();
    ts2 = arithOpTimeSeriesSource2Offset();
    y[0] = 2.0f * interp(100, 1.0f, 150, 2.0f, 125);
    y[1] = interp(125, 2.0f, 200, 4.0f, 150) * 2.0f;
    y[2] = 4.0f * interp(150, 2.0f, 250, 4.0f, 200);
    y[3] = interp(200, 4.0f, 300, 8.0f, 250) * 4.0f;

    ts1 = arithOpTimeSeriesSource1();
    ts2 = arithOpTimeSeriesSource2Offset();
    ts2.timeseriesMultiply(ts1);
    assertEquals(4, ts2.getDatapoints().size());
    assertEquals(y[0], ts2.getDatapoints().get(0).mValue.y);
    assertEquals(y[1], ts2.getDatapoints().get(1).mValue.y);
    assertEquals(y[2], ts2.getDatapoints().get(2).mValue.y);
    assertEquals(y[3], ts2.getDatapoints().get(3).mValue.y);
  }

  public void testTimeSeriesDivideOp() {
    TimeSeries ts1, ts2;
    float y[] = new float[6];

    // ts1 / ts2 (matching timestamps)
    ts1 = arithOpTimeSeriesSource1();
    ts2 = arithOpTimeSeriesSource2Matching();
    ts1.timeseriesDivide(ts2);
    assertEquals(0.5f, ts1.getDatapoints().get(0).mValue.y);
    assertEquals(0.5f, ts1.getDatapoints().get(1).mValue.y);
    assertEquals(0.5f, ts1.getDatapoints().get(2).mValue.y);

    // ts2 / ts1 (matching timestamps)
    ts1 = arithOpTimeSeriesSource1();
    ts2 = arithOpTimeSeriesSource2Matching();
    ts2.timeseriesDivide(ts1);
    assertEquals(2.0f, ts2.getDatapoints().get(0).mValue.y);
    assertEquals(2.0f, ts2.getDatapoints().get(1).mValue.y);
    assertEquals(2.0f, ts2.getDatapoints().get(2).mValue.y);

    // ts1 / ts2 (offset timestamps)
    // time: 100 125 150 200 250 300
    // val1: 1 2 4
    // val2: 2 4 8
    ts1 = arithOpTimeSeriesSource1();
    ts2 = arithOpTimeSeriesSource2Offset();
    y[0] = interp(100, 1.0f, 150, 2.0f, 125) / 2.0f;
    y[1] = 2.0f / interp(125, 2.0f, 200, 4.0f, 150);
    y[2] = interp(150, 2.0f, 250, 4.0f, 200) / 4.0f;
    y[3] = 4.0f / interp(200, 4.0f, 300, 8.0f, 250);

    ts1.timeseriesDivide(ts2);
    assertEquals(4, ts1.getDatapoints().size());
    assertEquals(y[0], ts1.getDatapoints().get(0).mValue.y);
    assertEquals(y[1], ts1.getDatapoints().get(1).mValue.y);
    assertEquals(y[2], ts1.getDatapoints().get(2).mValue.y);
    assertEquals(y[3], ts1.getDatapoints().get(3).mValue.y);

    // ts2 / ts1 (offset timestamps)
    ts1 = arithOpTimeSeriesSource1();
    ts2 = arithOpTimeSeriesSource2Offset();
    y[0] = 2.0f / interp(100, 1.0f, 150, 2.0f, 125);
    y[1] = interp(125, 2.0f, 200, 4.0f, 150) / 2.0f;
    y[2] = 4.0f / interp(150, 2.0f, 250, 4.0f, 200);
    y[3] = interp(200, 4.0f, 300, 8.0f, 250) / 4.0f;

    ts1 = arithOpTimeSeriesSource1();
    ts2 = arithOpTimeSeriesSource2Offset();
    ts2.timeseriesDivide(ts1);
    assertEquals(4, ts2.getDatapoints().size());
    assertEquals(y[0], ts2.getDatapoints().get(0).mValue.y);
    assertEquals(y[1], ts2.getDatapoints().get(1).mValue.y);
    assertEquals(y[2], ts2.getDatapoints().get(2).mValue.y);
    assertEquals(y[3], ts2.getDatapoints().get(3).mValue.y);
  }

  // These will be tested in Number.* unittests:
  // testCalcStatsAndBounds()
  // testRecalcStatsAndBounds()

}
