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

import net.redgeek.android.eventrend.graph.plugins.LinearInterpolator;
import net.redgeek.android.eventrend.graph.plugins.TimeSeriesInterpolator;
import net.redgeek.android.eventrend.primitives.TimeSeriesCollector;
import net.redgeek.android.eventrend.test.commonmocks.MockEvenTrendContext;
import net.redgeek.android.eventrend.test.commonmocks.MockEvenTrendDbAdapter;

import java.util.ArrayList;

// TODO: implement
public class TimeSeriesCollectorTest extends TestCase {
  private TimeSeriesCollector newTSC() {
    MockEvenTrendDbAdapter dbh = new MockEvenTrendDbAdapter();
    ArrayList<TimeSeriesInterpolator> interpolators = new ArrayList<TimeSeriesInterpolator>();
    interpolators.add(new LinearInterpolator());

    TimeSeriesCollector tsc = new TimeSeriesCollector(dbh);
    tsc.setHistory(20);
    tsc.setSmoothing(0.1f);
    tsc.setSensitivity(1.0f);
    tsc.setInterpolators(interpolators);
    
    return tsc;
  }
  
  public void testConstructor() {
    TimeSeriesCollector tsc = newTSC();
    
    assertNotNull(tsc);
    assertNotNull(tsc.getDbh());
    assertNotNull(tsc.getAllSeries());
    assertEquals(0, tsc.getAllSeries().size());
    assertNotNull(tsc.getAllEnabledSeries());
    assertEquals(0, tsc.getAllEnabledSeries().size());
    assertNull(tsc.getLastDatapoint(0));
    assertNull(tsc.getLastDatapoint(1));
    assertNull(tsc.getSeriesById(1));
    assertNull(tsc.getSeriesByName("foo"));
    assertNull(tsc.getVisibleFirstDatapoint());
    assertNull(tsc.getVisibleLastDatapoint());
    assertNull(tsc.getVisibleRange());
    assertFalse(tsc.getAutoAggregation());
    assertFalse(tsc.isSeriesEnabled(1));
  }
}

