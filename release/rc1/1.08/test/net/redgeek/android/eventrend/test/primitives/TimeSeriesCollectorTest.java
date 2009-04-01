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

import net.redgeek.android.eventrend.db.CategoryDbTable;
import net.redgeek.android.eventrend.graph.plugins.LinearInterpolator;
import net.redgeek.android.eventrend.graph.plugins.TimeSeriesInterpolator;
import net.redgeek.android.eventrend.primitives.TimeSeriesCollector;
import net.redgeek.android.eventrend.test.common.DbTestReader;
import net.redgeek.android.eventrend.test.common.MockEvenTrendDbAdapter;
import net.redgeek.android.eventrend.test.common.MockTimeSeriesPainter;

import java.util.ArrayList;
import java.util.HashMap;

// TODO: add tests that use aggregation
public class TimeSeriesCollectorTest extends TestCase {
  private TimeSeriesCollector newTSC(MockEvenTrendDbAdapter dbh) {
    MockTimeSeriesPainter painter = new MockTimeSeriesPainter();
    ArrayList<TimeSeriesInterpolator> interpolators = new ArrayList<TimeSeriesInterpolator>();
    interpolators.add(new LinearInterpolator());

    TimeSeriesCollector tsc = new TimeSeriesCollector(dbh, painter);
    tsc.setHistory(20);
    tsc.setSmoothing(0.1f);
    tsc.setSensitivity(1.0f);
    tsc.setInterpolators(interpolators);

    return tsc;
  }
  
  private String makeFilePath(String filename) {
    String sep = System.getProperty("file.separator");
    String path = System.getProperty("user.dir");
    String[] subdir = new String[] { "test", "net", "redgeek", "android",
        "eventrend", "test", "primitives", "dbtestdata" };

    for (int i = 0; i < subdir.length; i++) {
      path += sep + subdir[i];
    }
    path += sep + filename;
    return path;
  }

  public void testConstructor() {
    MockEvenTrendDbAdapter dbh = new MockEvenTrendDbAdapter();
    TimeSeriesCollector tsc = newTSC(dbh);

    assertNotNull(tsc);
    assertNotNull(tsc.getDbh());
    assertNotNull(tsc.getAllSeries());
    assertEquals(0, tsc.getAllSeries().size());
    assertNotNull(tsc.getAllEnabledSeries());
    assertEquals(0, tsc.getAllEnabledSeries().size());
    assertNull(tsc.getLastDatapoint(0));
    assertNull(tsc.getLastDatapoint(1));
    assertNull(tsc.getSeriesByIdLocking(1));
    assertNull(tsc.getSeriesByNameLocking("foo"));
    assertNull(tsc.getVisibleFirstDatapointLocking());
    assertNull(tsc.getVisibleLastDatapointLocking());
    assertFalse(tsc.getAutoAggregation());
    assertFalse(tsc.isSeriesEnabled(1));
  }

  public void testUpdateTimeSeriesMeta() {
    TimeSeriesCollector tsc;
    CategoryDbTable.Row cat1, cat2;

    MockEvenTrendDbAdapter dbh = new MockEvenTrendDbAdapter();
    DbTestReader reader = new DbTestReader(dbh);
    reader.populateFromFile(makeFilePath("tsc_two_categories.xml"));
    tsc = newTSC(dbh);

    cat1 = dbh.fetchCategory(1);
    cat2 = dbh.fetchCategory(2);

    // initial: 2 categories
    tsc.updateTimeSeriesMetaLocking(true);
    assertEquals(2, tsc.getAllSeries().size());
    assertNotNull(tsc.getSeriesByIdLocking(1).getDbRow());
    assertNotSame(tsc.getSeriesByIdLocking(1), tsc.getSeries(1));
    assertNotNull(tsc.getSeriesByIdLocking(1).getDbRow());
    assertEquals(cat1.getId(), tsc.getSeriesByIdLocking(1).getDbRow().getId());
    assertNotNull(tsc.getSeriesByIdLocking(2).getDbRow());
    assertEquals(cat2.getId(), tsc.getSeriesByIdLocking(2).getDbRow().getId());
    assertNotNull(tsc.getSeriesByIdLocking(1).getInterpolator());
    assertNotNull(tsc.getSeriesByIdLocking(2).getInterpolator());
    assertNotSame(tsc.getSeriesByIdLocking(1), tsc.getSeriesByIdLocking(2));
    assertEquals(1, tsc.getSeriesByIdLocking(1).getDependents().size());
    assertEquals(0, tsc.getSeriesByIdLocking(1).getDependees().size());
    assertEquals(0, tsc.getSeriesByIdLocking(2).getDependents().size());
    assertEquals(1, tsc.getSeriesByIdLocking(2).getDependees().size());
    assertEquals(tsc.getSeriesByIdLocking(2), tsc.getSeriesByIdLocking(1).getDependents()
        .get(0));
    assertEquals(tsc.getSeriesByIdLocking(1), tsc.getSeriesByIdLocking(2).getDependees().get(
        0));

    // Note: these tests being to get quite state-based, hence not really
    // unit tests, but it's a pain to do otherwise. Probably indicative that
    // the entire class(es) need to be refactored.

    // enabled series 1 and 2, add a category
    tsc.setSeriesEnabled(1, true);
    tsc.toggleSeriesEnabled(2);

    CategoryDbTable.Row cat3 = new CategoryDbTable.Row();
    cat3.setId(3);
    cat3.setCategoryName("cat 3");
    dbh.addContent("categories", dbh.categoryRowToHashMap(cat3));

    tsc.updateTimeSeriesMetaLocking(false);
    assertEquals(3, tsc.getAllSeries().size());
    assertEquals(cat1.getId(), tsc.getSeriesByIdLocking(1).getDbRow().getId());
    assertEquals(cat2.getId(), tsc.getSeriesByIdLocking(2).getDbRow().getId());
    assertEquals(cat3.getId(), tsc.getSeriesByIdLocking(3).getDbRow().getId());
    assertNotSame(tsc.getSeriesByIdLocking(1), tsc.getSeriesByIdLocking(2));
    assertEquals(1, tsc.getSeriesByIdLocking(1).getDependents().size());
    assertEquals(0, tsc.getSeriesByIdLocking(1).getDependees().size());
    assertEquals(0, tsc.getSeriesByIdLocking(2).getDependents().size());
    assertEquals(1, tsc.getSeriesByIdLocking(2).getDependees().size());
    assertTrue(tsc.isSeriesEnabled(1));
    assertTrue(tsc.isSeriesEnabled(2));
    assertFalse(tsc.isSeriesEnabled(3));
    assertEquals("cat 1", tsc.getSeriesByIdLocking(1).getDbRow().getCategoryName());
    assertEquals("cat 2", tsc.getSeriesByIdLocking(2).getDbRow().getCategoryName());
    assertEquals("cat 3", tsc.getSeriesByIdLocking(3).getDbRow().getCategoryName());
    assertEquals("cat 1", tsc.getSeriesByNameLocking("cat 1").getDbRow()
        .getCategoryName());
    assertEquals("cat 2", tsc.getSeriesByNameLocking("cat 2").getDbRow()
        .getCategoryName());
    assertEquals("cat 3", tsc.getSeriesByNameLocking("cat 3").getDbRow()
        .getCategoryName());
  }

  public void testUpdateTimeSeriesData() {
    TimeSeriesCollector tsc;

    MockEvenTrendDbAdapter dbh = new MockEvenTrendDbAdapter();
    DbTestReader reader = new DbTestReader(dbh);
    reader.populateFromFile(makeFilePath("tsc_category_with_2entries.xml"));
    tsc = newTSC(dbh);

    tsc.updateTimeSeriesMetaLocking(true);
    
    // no series enabled, nothing should happen
    tsc.updateTimeSeriesData(1000, 2000, false);
    assertNotNull(tsc.getSeriesByIdLocking(1).getDatapoints());
    assertEquals(0, tsc.getSeriesByIdLocking(1).getDatapoints().size());
        
    tsc.setSeriesEnabled(1, true);

    // enabled, but no flush
    tsc.updateTimeSeriesData(1000, 2000, false);
    assertNotNull(tsc.getSeriesByIdLocking(1).getDatapoints());
    assertEquals(2, tsc.getSeriesByIdLocking(1).getDatapoints().size());
    assertEquals(1.0f, tsc.getSeriesByIdLocking(1).getDatapoints().get(0).mValue.y);
    assertEquals(2.0f, tsc.getSeriesByIdLocking(1).getDatapoints().get(1).mValue.y);

    reader.populateFromFile(makeFilePath("tsc_category_with_3entries.xml"));

    // ensure the old values are the same and didn't pick up the new value
    // outside the range
    tsc.updateTimeSeriesData(1000, 2000, false);
    assertNotNull(tsc.getSeriesByIdLocking(1).getDatapoints());
    assertEquals(2, tsc.getSeriesByIdLocking(1).getDatapoints().size());
    assertEquals(1.0f, tsc.getSeriesByIdLocking(1).getDatapoints().get(0).mValue.y);
    assertEquals(2.0f, tsc.getSeriesByIdLocking(1).getDatapoints().get(1).mValue.y);

    // pick up the new value and ensure the old values are the same
    tsc.updateTimeSeriesData(1000, 3000, false);
    assertNotNull(tsc.getSeriesByIdLocking(1).getDatapoints());
    assertEquals(3, tsc.getSeriesByIdLocking(1).getDatapoints().size());
    assertEquals(1.0f, tsc.getSeriesByIdLocking(1).getDatapoints().get(0).mValue.y);
    assertEquals(2.0f, tsc.getSeriesByIdLocking(1).getDatapoints().get(1).mValue.y);
    assertEquals(30.0f, tsc.getSeriesByIdLocking(1).getDatapoints().get(2).mValue.y);

    // flush the cache and re-read everything
    tsc.updateTimeSeriesData(1000, 3000, true);
    assertNotNull(tsc.getSeriesByIdLocking(1).getDatapoints());
    assertEquals(3, tsc.getSeriesByIdLocking(1).getDatapoints().size());
    assertEquals(10.0f, tsc.getSeriesByIdLocking(1).getDatapoints().get(0).mValue.y);
    assertEquals(20.0f, tsc.getSeriesByIdLocking(1).getDatapoints().get(1).mValue.y);
    assertEquals(30.0f, tsc.getSeriesByIdLocking(1).getDatapoints().get(2).mValue.y);
  }
  
  public void testVisibility() {
    TimeSeriesCollector tsc;

    MockEvenTrendDbAdapter dbh = new MockEvenTrendDbAdapter();
    DbTestReader reader = new DbTestReader(dbh);
    reader.populateFromFile(makeFilePath("tsc_category_with_3entries.xml"));
    tsc = newTSC(dbh);

    tsc.updateTimeSeriesMetaLocking(true);
    
    // no series enabled, nothing should happen
    tsc.updateTimeSeriesData(1000, 2000, false);
    assertNotNull(tsc.getSeriesByIdLocking(1).getDatapoints());
    assertEquals(0, tsc.getSeriesByIdLocking(1).getDatapoints().size());
    assertNull(tsc.getVisibleFirstDatapointLocking());
    assertNull(tsc.getVisibleLastDatapointLocking());
    
    tsc.setSeriesEnabled(1, true);

    // no data within query range
    tsc.updateTimeSeriesData(100, 200, false);
    assertEquals(0, tsc.getSeriesByIdLocking(1).getDatapoints().size());
    assertNull(tsc.getVisibleFirstDatapointLocking());
    assertNull(tsc.getVisibleLastDatapointLocking());

    // one datapoint within range
    tsc.updateTimeSeriesData(100, 1000, false);
    assertEquals(1, tsc.getSeriesByIdLocking(1).getDatapoints().size());
    assertNotNull(tsc.getVisibleFirstDatapointLocking());
    assertNotNull(tsc.getVisibleLastDatapointLocking());
    assertSame(tsc.getVisibleFirstDatapointLocking(), tsc.getVisibleLastDatapointLocking());
    assertEquals(10.0f, tsc.getSeriesByIdLocking(1).getDatapoints().get(0).mValue.y);

    // two datapoints within range
    tsc.updateTimeSeriesData(1000, 2000, false);
    assertEquals(2, tsc.getSeriesByIdLocking(1).getDatapoints().size());
    assertNotNull(tsc.getVisibleFirstDatapointLocking());
    assertNotNull(tsc.getVisibleLastDatapointLocking());
    assertNotSame(tsc.getVisibleFirstDatapointLocking(), tsc.getVisibleLastDatapointLocking());
    assertEquals(10.0f, tsc.getVisibleFirstDatapointLocking().mValue.y);
    assertEquals(10.0f, tsc.getSeriesByIdLocking(1).getDatapoints().get(0).mValue.y);
    assertEquals(20.0f, tsc.getVisibleLastDatapointLocking().mValue.y);
    assertEquals(20.0f, tsc.getSeriesByIdLocking(1).getDatapoints().get(1).mValue.y);
    
    // three datapoints within range
    tsc.updateTimeSeriesData(1000, 3000, false);
    assertEquals(3, tsc.getSeriesByIdLocking(1).getDatapoints().size());
    assertNotNull(tsc.getVisibleFirstDatapointLocking());
    assertNotNull(tsc.getVisibleLastDatapointLocking());
    assertNotSame(tsc.getVisibleFirstDatapointLocking(), tsc.getVisibleLastDatapointLocking());
    assertEquals(10.0f, tsc.getVisibleFirstDatapointLocking().mValue.y);
    assertEquals(10.0f, tsc.getSeriesByIdLocking(1).getDatapoints().get(0).mValue.y);
    assertEquals(20.0f, tsc.getSeriesByIdLocking(1).getDatapoints().get(1).mValue.y);
    assertEquals(30.0f, tsc.getVisibleLastDatapointLocking().mValue.y);
    assertEquals(30.0f, tsc.getSeriesByIdLocking(1).getDatapoints().get(2).mValue.y);
    
    // three datapoints gathered (from cache), but only 2 within range
    tsc.updateTimeSeriesData(1001, 3000, false);
    assertEquals(3, tsc.getSeriesByIdLocking(1).getDatapoints().size());
    assertNotNull(tsc.getVisibleFirstDatapointLocking());
    assertNotNull(tsc.getVisibleLastDatapointLocking());
    assertNotSame(tsc.getVisibleFirstDatapointLocking(), tsc.getVisibleLastDatapointLocking());
    assertEquals(20.0f, tsc.getVisibleFirstDatapointLocking().mValue.y);
    assertEquals(30.0f, tsc.getVisibleLastDatapointLocking().mValue.y);

    // three datapoints gathered (from cache), but only 1 within range
    tsc.updateTimeSeriesData(1001, 2999, false);
    assertEquals(3, tsc.getSeriesByIdLocking(1).getDatapoints().size());
    assertNotNull(tsc.getVisibleFirstDatapointLocking());
    assertNotNull(tsc.getVisibleLastDatapointLocking());
    assertSame(tsc.getVisibleFirstDatapointLocking(), tsc.getVisibleLastDatapointLocking());
    assertEquals(20.0f, tsc.getVisibleFirstDatapointLocking().mValue.y);
    assertEquals(20.0f, tsc.getVisibleLastDatapointLocking().mValue.y);    
  }
  
  public void testGatherLatest() {
    TimeSeriesCollector tsc;

    MockEvenTrendDbAdapter dbh = new MockEvenTrendDbAdapter();
    DbTestReader reader = new DbTestReader(dbh);
    reader.populateFromFile(makeFilePath("tsc_category_with_5entries.xml"));
    tsc = newTSC(dbh);

    tsc.updateTimeSeriesMetaLocking(true);
    
    // no series enabled, but gatherLatestDatapoints should ignore isEnabled()
    tsc.gatherLatestDatapointsLocking(1, 10);
    assertNotNull(tsc.getSeriesByIdLocking(1).getDatapoints());
    assertEquals(5, tsc.getSeriesByIdLocking(1).getDatapoints().size());
    assertNull(tsc.getVisibleFirstDatapointLocking());
    assertNull(tsc.getVisibleLastDatapointLocking());
 
    // reset, enabled one (so getVisible*Datapoint() is returns a value)
    tsc.clearSeriesLocking();
    tsc.updateTimeSeriesMetaLocking(true);
    tsc.setSeriesEnabled(1, true);

    // no datapoints
    tsc.gatherLatestDatapointsLocking(1, 0);
    assertEquals(0, tsc.getSeriesByIdLocking(1).getDatapoints().size());
    assertNull(tsc.getVisibleFirstDatapointLocking());
    assertNull(tsc.getVisibleLastDatapointLocking());

    // latest one
    tsc.gatherLatestDatapointsLocking(1, 1);
    assertEquals(1, tsc.getSeriesByIdLocking(1).getDatapoints().size());
    assertNotNull(tsc.getVisibleFirstDatapointLocking());
    assertNotNull(tsc.getVisibleLastDatapointLocking());
    assertSame(tsc.getVisibleFirstDatapointLocking(), tsc.getVisibleLastDatapointLocking());
    assertEquals(50.0f, tsc.getSeriesByIdLocking(1).getDatapoints().get(0).mValue.y);

    // latest two, old last visible is 5000ms, make sure 4000ms is pulled in
    tsc.gatherLatestDatapointsLocking(1, 2);
    assertEquals(2, tsc.getSeriesByIdLocking(1).getDatapoints().size());
    assertNotNull(tsc.getVisibleFirstDatapointLocking());
    assertNotNull(tsc.getVisibleLastDatapointLocking());
    assertNotSame(tsc.getVisibleFirstDatapointLocking(), tsc.getVisibleLastDatapointLocking());
    assertEquals(40.0f, tsc.getVisibleFirstDatapointLocking().mValue.y);
    assertEquals(40.0f, tsc.getSeriesByIdLocking(1).getDatapoints().get(0).mValue.y);
    assertEquals(50.0f, tsc.getVisibleLastDatapointLocking().mValue.y);
    assertEquals(50.0f, tsc.getSeriesByIdLocking(1).getDatapoints().get(1).mValue.y);
        
    // latest five
    tsc.gatherLatestDatapointsLocking(1, 5);
    assertEquals(5, tsc.getSeriesByIdLocking(1).getDatapoints().size());
    assertNotNull(tsc.getVisibleFirstDatapointLocking());
    assertNotNull(tsc.getVisibleLastDatapointLocking());
    assertNotSame(tsc.getVisibleFirstDatapointLocking(), tsc.getVisibleLastDatapointLocking());
    assertEquals(10.0f, tsc.getVisibleFirstDatapointLocking().mValue.y);
    assertEquals(10.0f, tsc.getSeriesByIdLocking(1).getDatapoints().get(0).mValue.y);
    assertEquals(20.0f, tsc.getSeriesByIdLocking(1).getDatapoints().get(1).mValue.y);
    assertEquals(30.0f, tsc.getSeriesByIdLocking(1).getDatapoints().get(2).mValue.y);
    assertEquals(40.0f, tsc.getSeriesByIdLocking(1).getDatapoints().get(3).mValue.y);
    assertEquals(50.0f, tsc.getVisibleLastDatapointLocking().mValue.y);
    assertEquals(50.0f, tsc.getSeriesByIdLocking(1).getDatapoints().get(4).mValue.y);

    // latest ten, only five to gather
    tsc.gatherLatestDatapointsLocking(1, 10);
    assertEquals(5, tsc.getSeriesByIdLocking(1).getDatapoints().size());
    assertNotNull(tsc.getVisibleFirstDatapointLocking());
    assertNotNull(tsc.getVisibleLastDatapointLocking());
    assertNotSame(tsc.getVisibleFirstDatapointLocking(), tsc.getVisibleLastDatapointLocking());
    assertEquals(10.0f, tsc.getVisibleFirstDatapointLocking().mValue.y);
    assertEquals(10.0f, tsc.getSeriesByIdLocking(1).getDatapoints().get(0).mValue.y);
    assertEquals(20.0f, tsc.getSeriesByIdLocking(1).getDatapoints().get(1).mValue.y);
    assertEquals(30.0f, tsc.getSeriesByIdLocking(1).getDatapoints().get(2).mValue.y);
    assertEquals(40.0f, tsc.getSeriesByIdLocking(1).getDatapoints().get(3).mValue.y);
    assertEquals(50.0f, tsc.getVisibleLastDatapointLocking().mValue.y);
    assertEquals(50.0f, tsc.getSeriesByIdLocking(1).getDatapoints().get(4).mValue.y);

    // reset, pull in two datapoints, then add some points to the DB, and make
    // sure that the new points are pulled when:
    //   (1) the new points don't overlap the old range
    //   (2) the new points overlap and end in the old range
    //   (3) the new points extend past the old range

    // case (1)
    tsc.clearSeriesLocking();
    tsc.updateTimeSeriesMetaLocking(true);
    tsc.setSeriesEnabled(1, true);
    tsc.updateTimeSeriesData(1000, 2000, false);
    tsc.gatherLatestDatapointsLocking(1, 2);
    assertEquals(2, tsc.getSeriesByIdLocking(1).getDatapoints().size());
    assertNotNull(tsc.getVisibleFirstDatapointLocking());
    assertNotNull(tsc.getVisibleLastDatapointLocking());
    assertNotSame(tsc.getVisibleFirstDatapointLocking(), tsc.getVisibleLastDatapointLocking());
    assertEquals(40.0f, tsc.getVisibleFirstDatapointLocking().mValue.y);
    assertEquals(40.0f, tsc.getSeriesByIdLocking(1).getDatapoints().get(0).mValue.y);
    assertEquals(50.0f, tsc.getVisibleLastDatapointLocking().mValue.y);
    assertEquals(50.0f, tsc.getSeriesByIdLocking(1).getDatapoints().get(1).mValue.y);

    // case (2)
    tsc.clearSeriesLocking();
    tsc.updateTimeSeriesMetaLocking(true);
    tsc.setSeriesEnabled(1, true);
    tsc.updateTimeSeriesData(1000, 3000, false);
    tsc.gatherLatestDatapointsLocking(1, 3);
    assertEquals(3, tsc.getSeriesByIdLocking(1).getDatapoints().size());
    assertNotNull(tsc.getVisibleFirstDatapointLocking());
    assertNotNull(tsc.getVisibleLastDatapointLocking());
    assertNotSame(tsc.getVisibleFirstDatapointLocking(), tsc.getVisibleLastDatapointLocking());
    assertEquals(30.0f, tsc.getVisibleFirstDatapointLocking().mValue.y);
    assertEquals(30.0f, tsc.getSeriesByIdLocking(1).getDatapoints().get(0).mValue.y);
    assertEquals(40.0f, tsc.getSeriesByIdLocking(1).getDatapoints().get(1).mValue.y);
    assertEquals(50.0f, tsc.getVisibleLastDatapointLocking().mValue.y);
    assertEquals(50.0f, tsc.getSeriesByIdLocking(1).getDatapoints().get(2).mValue.y);

    // case (3)
    tsc.clearSeriesLocking();
    tsc.updateTimeSeriesMetaLocking(true);
    tsc.setSeriesEnabled(1, true);
    tsc.updateTimeSeriesData(2000, 3000, false);
    tsc.gatherLatestDatapointsLocking(1, 5);
    assertEquals(5, tsc.getSeriesByIdLocking(1).getDatapoints().size());
    assertNotNull(tsc.getVisibleFirstDatapointLocking());
    assertNotNull(tsc.getVisibleLastDatapointLocking());
    assertNotSame(tsc.getVisibleFirstDatapointLocking(), tsc.getVisibleLastDatapointLocking());
    assertEquals(10.0f, tsc.getVisibleFirstDatapointLocking().mValue.y);
    assertEquals(10.0f, tsc.getSeriesByIdLocking(1).getDatapoints().get(0).mValue.y);
    assertEquals(20.0f, tsc.getSeriesByIdLocking(1).getDatapoints().get(1).mValue.y);
    assertEquals(30.0f, tsc.getSeriesByIdLocking(1).getDatapoints().get(2).mValue.y);
    assertEquals(40.0f, tsc.getSeriesByIdLocking(1).getDatapoints().get(3).mValue.y);
    assertEquals(50.0f, tsc.getVisibleLastDatapointLocking().mValue.y);
    assertEquals(50.0f, tsc.getSeriesByIdLocking(1).getDatapoints().get(4).mValue.y);
  }
  
  public void testGatherSeries() {
    TimeSeriesCollector tsc;

    MockEvenTrendDbAdapter dbh = new MockEvenTrendDbAdapter();
    DbTestReader reader = new DbTestReader(dbh);
    reader.populateFromFile(makeFilePath("tsc_3cats_5entries.xml"));
    tsc = newTSC(dbh);

    tsc.updateTimeSeriesMetaLocking(true);

    // one non-synthetic series enabled:
    tsc.setSeriesEnabled(1, true);
    tsc.gatherSeriesLocking(900, 1100);
    assertEquals(3, tsc.getAllSeries().size());
    assertEquals(1, tsc.getAllEnabledSeries().size());
    assertEquals(1, tsc.getSeriesByIdLocking(1).getDatapoints().size());
    assertEquals(0, tsc.getSeriesByIdLocking(2).getDatapoints().size());
    assertEquals(0, tsc.getSeriesByIdLocking(3).getDatapoints().size());
    assertNotNull(tsc.getVisibleFirstDatapointLocking());
    assertNotNull(tsc.getVisibleLastDatapointLocking());
    assertSame(tsc.getVisibleFirstDatapointLocking(), tsc.getVisibleLastDatapointLocking());
    assertEquals(1000, tsc.getVisibleFirstDatapointLocking().mMillis);
    assertEquals(10.0f, tsc.getVisibleFirstDatapointLocking().mValue.y);
    assertEquals(1000, tsc.getSeriesByIdLocking(1).getDatapoints().get(0).mMillis);
    assertEquals(10.0f, tsc.getSeriesByIdLocking(1).getDatapoints().get(0).mValue.y);
    assertEquals(1000, tsc.getLastDatapoint(1).mMillis);

    // one synthetic series enabled, dependents not enabled
    tsc.clearSeriesLocking();
    tsc.updateTimeSeriesMetaLocking(true);
    tsc.setSeriesEnabled(3, true);
    tsc.gatherSeriesLocking(900, 1100);
    assertEquals(3, tsc.getAllSeries().size());
    assertEquals(1, tsc.getAllEnabledSeries().size());
    assertEquals(1, tsc.getSeriesByIdLocking(1).getDatapoints().size());
    assertEquals(1, tsc.getSeriesByIdLocking(2).getDatapoints().size());
    assertEquals(1, tsc.getSeriesByIdLocking(3).getDatapoints().size());
    assertNotNull(tsc.getVisibleFirstDatapointLocking());
    assertNotNull(tsc.getVisibleLastDatapointLocking());
    assertSame(tsc.getVisibleFirstDatapointLocking(), tsc.getVisibleLastDatapointLocking());
    assertEquals(1000, tsc.getVisibleFirstDatapointLocking().mMillis);
    assertEquals(110.0f, tsc.getVisibleFirstDatapointLocking().mValue.y);
    assertEquals(1000, tsc.getSeriesByIdLocking(3).getDatapoints().get(0).mMillis);
    assertEquals(110.0f, tsc.getSeriesByIdLocking(3).getDatapoints().get(0).mValue.y);
    assertEquals(10.0f, tsc.getLastDatapoint(1).mValue.y);
    assertEquals(100.0f, tsc.getLastDatapoint(2).mValue.y);
    assertNull(tsc.getLastDatapoint(3));

    // all enabled, query entire range
    tsc.clearSeriesLocking();
    tsc.updateTimeSeriesMetaLocking(true);
    tsc.setSeriesEnabled(1, true);
    tsc.setSeriesEnabled(2, true);
    tsc.setSeriesEnabled(3, true);
    tsc.gatherSeriesLocking(500, 6000);
    assertEquals(3, tsc.getAllSeries().size());
    assertEquals(3, tsc.getAllEnabledSeries().size());
    assertEquals(5, tsc.getSeriesByIdLocking(1).getDatapoints().size());
    assertEquals(5, tsc.getSeriesByIdLocking(2).getDatapoints().size());
    assertEquals(5, tsc.getSeriesByIdLocking(3).getDatapoints().size());
    assertNotNull(tsc.getVisibleFirstDatapointLocking());
    assertNotNull(tsc.getVisibleLastDatapointLocking());
    assertNotSame(tsc.getVisibleFirstDatapointLocking(), tsc.getVisibleLastDatapointLocking());
    assertEquals(1000, tsc.getVisibleFirstDatapointLocking().mMillis);
    assertEquals(5000, tsc.getVisibleLastDatapointLocking().mMillis);
    assertEquals(50.0f, tsc.getLastDatapoint(1).mValue.y);
    assertEquals(500.0f, tsc.getLastDatapoint(2).mValue.y);
    assertNull(tsc.getLastDatapoint(3));

    // all enabled, query middle of range
    tsc.clearSeriesLocking();
    tsc.updateTimeSeriesMetaLocking(true);
    tsc.setSeriesEnabled(1, true);
    tsc.setSeriesEnabled(2, true);
    tsc.setSeriesEnabled(3, true);
    tsc.gatherSeriesLocking(2000, 4000);
    assertEquals(3, tsc.getAllSeries().size());
    assertEquals(3, tsc.getAllEnabledSeries().size());
    assertEquals(4, tsc.getSeriesByIdLocking(1).getDatapoints().size());
    assertEquals(4, tsc.getSeriesByIdLocking(2).getDatapoints().size());
    assertEquals(4, tsc.getSeriesByIdLocking(3).getDatapoints().size());
    assertNotNull(tsc.getVisibleFirstDatapointLocking());
    assertNotNull(tsc.getVisibleLastDatapointLocking());
    assertNotSame(tsc.getVisibleFirstDatapointLocking(), tsc.getVisibleLastDatapointLocking());
    assertEquals(2000, tsc.getVisibleFirstDatapointLocking().mMillis);
    assertEquals(4000, tsc.getVisibleLastDatapointLocking().mMillis);
    assertEquals(40.0f, tsc.getLastDatapoint(1).mValue.y);
    assertEquals(400.0f, tsc.getLastDatapoint(2).mValue.y);
    assertNull(tsc.getLastDatapoint(3));

    // TODO: need more tests here, short on coverage
  }
  
  public void testUpdateCategoryTrend() {
    TimeSeriesCollector tsc;

    MockEvenTrendDbAdapter dbh = new MockEvenTrendDbAdapter();
    DbTestReader reader = new DbTestReader(dbh);
    reader.populateFromFile(makeFilePath("tsc_3cats_5entries.xml"));
    tsc = newTSC(dbh);

    tsc.updateTimeSeriesMetaLocking(true);
    // should not need to enabled any series

    // cat 1 should update, cat 3 as well, but not cat 2
    assertEquals("trend_unknown", dbh.fetchCategory(1).getTrendState());
    assertEquals(3.0f, dbh.fetchCategory(1).getLastTrend());    
    assertEquals("trend_unknown", dbh.fetchCategory(2).getTrendState());
    assertEquals(3.0f, dbh.fetchCategory(2).getLastTrend());    
    assertEquals("trend_unknown", dbh.fetchCategory(3).getTrendState());
    assertEquals(3.0f, dbh.fetchCategory(3).getLastTrend());    
    tsc.updateCategoryTrend(1);
    assertFalse("trend_unknown".equals(dbh.fetchCategory(1).getTrendState()));
    assertFalse(3.0f == dbh.fetchCategory(1).getLastTrend());    
    assertEquals("trend_unknown", dbh.fetchCategory(2).getTrendState());
    assertEquals(3.0f, dbh.fetchCategory(2).getLastTrend());    
    assertFalse("trend_unknown".equals(dbh.fetchCategory(3).getTrendState()));
    assertFalse(3.0f == dbh.fetchCategory(3).getLastTrend());    
    
    // cat 2 and 3 should update
    tsc.updateCategoryTrend(2);
    assertFalse("trend_unknown".equals(dbh.fetchCategory(1).getTrendState()));
    assertFalse(3.0f == dbh.fetchCategory(1).getLastTrend());    
    assertFalse("trend_unknown".equals(dbh.fetchCategory(2).getTrendState()));
    assertFalse(3.0f == dbh.fetchCategory(2).getLastTrend());    
    assertFalse("trend_unknown".equals(dbh.fetchCategory(3).getTrendState()));
    assertFalse(3.0f == dbh.fetchCategory(3).getLastTrend());    

    // updating cat 3 should be a nop, since it's trend is only calculated when
    // it's dependents' trends are updated
    reader.populateFromFile(makeFilePath("tsc_3cats_5entries.xml"));
    tsc.clearSeriesLocking();
    tsc.updateTimeSeriesMetaLocking(true);
    assertEquals("trend_unknown", dbh.fetchCategory(1).getTrendState());
    assertEquals(3.0f, dbh.fetchCategory(1).getLastTrend());    
    assertEquals("trend_unknown", dbh.fetchCategory(2).getTrendState());
    assertEquals(3.0f, dbh.fetchCategory(2).getLastTrend());    
    assertEquals("trend_unknown", dbh.fetchCategory(3).getTrendState());
    assertEquals(3.0f, dbh.fetchCategory(3).getLastTrend());    
    tsc.updateCategoryTrend(3);
    assertEquals("trend_unknown", dbh.fetchCategory(1).getTrendState());
    assertEquals(3.0f, dbh.fetchCategory(1).getLastTrend());    
    assertEquals("trend_unknown", dbh.fetchCategory(2).getTrendState());
    assertEquals(3.0f, dbh.fetchCategory(2).getLastTrend());    
    assertEquals("trend_unknown", dbh.fetchCategory(3).getTrendState());
    assertEquals(3.0f, dbh.fetchCategory(3).getLastTrend());    

    // TODO: need more tests here, short on coverage
  }

  public void testLocking() {
    // TODO
  }
}
