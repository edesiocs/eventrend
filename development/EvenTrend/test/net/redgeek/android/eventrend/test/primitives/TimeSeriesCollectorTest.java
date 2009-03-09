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

// TODO: implement
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
    assertNull(tsc.getSeriesById(1));
    assertNull(tsc.getSeriesByName("foo"));
    assertNull(tsc.getVisibleFirstDatapoint());
    assertNull(tsc.getVisibleLastDatapoint());
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
    tsc.updateTimeSeriesMeta(true);
    assertEquals(2, tsc.getAllSeries().size());
    assertNotNull(tsc.getSeriesById(1).getDbRow());
    assertNotSame(tsc.getSeriesById(1), tsc.getSeries(1));
    assertNotNull(tsc.getSeriesById(1).getDbRow());
    assertEquals(cat1.getId(), tsc.getSeriesById(1).getDbRow().getId());
    assertNotNull(tsc.getSeriesById(2).getDbRow());
    assertEquals(cat2.getId(), tsc.getSeriesById(2).getDbRow().getId());
    assertNotNull(tsc.getSeriesById(1).getInterpolator());
    assertNotNull(tsc.getSeriesById(2).getInterpolator());
    assertNotSame(tsc.getSeriesById(1), tsc.getSeriesById(2));
    assertEquals(1, tsc.getSeriesById(1).getDependents().size());
    assertEquals(0, tsc.getSeriesById(1).getDependees().size());
    assertEquals(0, tsc.getSeriesById(2).getDependents().size());
    assertEquals(1, tsc.getSeriesById(2).getDependees().size());
    assertEquals(tsc.getSeriesById(2), tsc.getSeriesById(1).getDependents()
        .get(0));
    assertEquals(tsc.getSeriesById(1), tsc.getSeriesById(2).getDependees().get(
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

    tsc.updateTimeSeriesMeta(false);
    assertEquals(3, tsc.getAllSeries().size());
    assertEquals(cat1.getId(), tsc.getSeriesById(1).getDbRow().getId());
    assertEquals(cat2.getId(), tsc.getSeriesById(2).getDbRow().getId());
    assertEquals(cat3.getId(), tsc.getSeriesById(3).getDbRow().getId());
    assertNotSame(tsc.getSeriesById(1), tsc.getSeriesById(2));
    assertEquals(1, tsc.getSeriesById(1).getDependents().size());
    assertEquals(0, tsc.getSeriesById(1).getDependees().size());
    assertEquals(0, tsc.getSeriesById(2).getDependents().size());
    assertEquals(1, tsc.getSeriesById(2).getDependees().size());
    assertTrue(tsc.isSeriesEnabled(1));
    assertTrue(tsc.isSeriesEnabled(2));
    assertFalse(tsc.isSeriesEnabled(3));
    assertEquals("cat 1", tsc.getSeriesById(1).getDbRow().getCategoryName());
    assertEquals("cat 2", tsc.getSeriesById(2).getDbRow().getCategoryName());
    assertEquals("cat 3", tsc.getSeriesById(3).getDbRow().getCategoryName());
    assertEquals("cat 1", tsc.getSeriesByName("cat 1").getDbRow()
        .getCategoryName());
    assertEquals("cat 2", tsc.getSeriesByName("cat 2").getDbRow()
        .getCategoryName());
    assertEquals("cat 3", tsc.getSeriesByName("cat 3").getDbRow()
        .getCategoryName());
  }

  public void testUpdateTimeSeriesData() {
    TimeSeriesCollector tsc;

    MockEvenTrendDbAdapter dbh = new MockEvenTrendDbAdapter();
    DbTestReader reader = new DbTestReader(dbh);
    reader.populateFromFile(makeFilePath("tsc_category_with_2entries.xml"));
    tsc = newTSC(dbh);

    tsc.updateTimeSeriesMeta(true);
    
    // no series enabled, nothing should happen
    tsc.updateTimeSeriesData(1000, 2000, false);
    assertNotNull(tsc.getSeriesById(1).getDatapoints());
    assertEquals(0, tsc.getSeriesById(1).getDatapoints().size());
        
    tsc.setSeriesEnabled(1, true);

    // enabled, but no flush
    tsc.updateTimeSeriesData(1000, 2000, false);
    assertNotNull(tsc.getSeriesById(1).getDatapoints());
    assertEquals(2, tsc.getSeriesById(1).getDatapoints().size());
    assertEquals(1.0f, tsc.getSeriesById(1).getDatapoints().get(0).mValue.y);
    assertEquals(2.0f, tsc.getSeriesById(1).getDatapoints().get(1).mValue.y);

    reader.populateFromFile(makeFilePath("tsc_category_with_3entries.xml"));

    // ensure the old values are the same and didn't pick up the new value
    // outside the range
    tsc.updateTimeSeriesData(1000, 2000, false);
    assertNotNull(tsc.getSeriesById(1).getDatapoints());
    assertEquals(2, tsc.getSeriesById(1).getDatapoints().size());
    assertEquals(1.0f, tsc.getSeriesById(1).getDatapoints().get(0).mValue.y);
    assertEquals(2.0f, tsc.getSeriesById(1).getDatapoints().get(1).mValue.y);

    // pick up the new value and ensure the old values are the same
    tsc.updateTimeSeriesData(1000, 3000, false);
    assertNotNull(tsc.getSeriesById(1).getDatapoints());
    assertEquals(3, tsc.getSeriesById(1).getDatapoints().size());
    assertEquals(1.0f, tsc.getSeriesById(1).getDatapoints().get(0).mValue.y);
    assertEquals(2.0f, tsc.getSeriesById(1).getDatapoints().get(1).mValue.y);
    assertEquals(30.0f, tsc.getSeriesById(1).getDatapoints().get(2).mValue.y);

    // flush the cache and re-read everything
    tsc.updateTimeSeriesData(1000, 3000, true);
    assertNotNull(tsc.getSeriesById(1).getDatapoints());
    assertEquals(3, tsc.getSeriesById(1).getDatapoints().size());
    assertEquals(10.0f, tsc.getSeriesById(1).getDatapoints().get(0).mValue.y);
    assertEquals(20.0f, tsc.getSeriesById(1).getDatapoints().get(1).mValue.y);
    assertEquals(30.0f, tsc.getSeriesById(1).getDatapoints().get(2).mValue.y);
  }
  
  public void testVisibility() {
    TimeSeriesCollector tsc;

    MockEvenTrendDbAdapter dbh = new MockEvenTrendDbAdapter();
    DbTestReader reader = new DbTestReader(dbh);
    reader.populateFromFile(makeFilePath("tsc_category_with_3entries.xml"));
    tsc = newTSC(dbh);

    tsc.updateTimeSeriesMeta(true);
    
    // no series enabled, nothing should happen
    tsc.updateTimeSeriesData(1000, 2000, false);
    assertNotNull(tsc.getSeriesById(1).getDatapoints());
    assertEquals(0, tsc.getSeriesById(1).getDatapoints().size());
    assertNull(tsc.getVisibleFirstDatapoint());
    assertNull(tsc.getVisibleLastDatapoint());
    
    tsc.setSeriesEnabled(1, true);

    // no data within query range
    tsc.updateTimeSeriesData(100, 200, false);
    assertEquals(0, tsc.getSeriesById(1).getDatapoints().size());
    assertNull(tsc.getVisibleFirstDatapoint());
    assertNull(tsc.getVisibleLastDatapoint());

    // one datapoint within range
    tsc.updateTimeSeriesData(100, 1000, false);
    assertEquals(1, tsc.getSeriesById(1).getDatapoints().size());
    assertNotNull(tsc.getVisibleFirstDatapoint());
    assertNotNull(tsc.getVisibleLastDatapoint());
    assertSame(tsc.getVisibleFirstDatapoint(), tsc.getVisibleLastDatapoint());
    assertEquals(10.0f, tsc.getSeriesById(1).getDatapoints().get(0).mValue.y);

    // two datapoints within range
    tsc.updateTimeSeriesData(1000, 2000, false);
    assertEquals(2, tsc.getSeriesById(1).getDatapoints().size());
    assertNotNull(tsc.getVisibleFirstDatapoint());
    assertNotNull(tsc.getVisibleLastDatapoint());
    assertNotSame(tsc.getVisibleFirstDatapoint(), tsc.getVisibleLastDatapoint());
    assertEquals(10.0f, tsc.getVisibleFirstDatapoint().mValue.y);
    assertEquals(10.0f, tsc.getSeriesById(1).getDatapoints().get(0).mValue.y);
    assertEquals(20.0f, tsc.getVisibleLastDatapoint().mValue.y);
    assertEquals(20.0f, tsc.getSeriesById(1).getDatapoints().get(1).mValue.y);
    
    // three datapoints within range
    tsc.updateTimeSeriesData(1000, 3000, false);
    assertEquals(3, tsc.getSeriesById(1).getDatapoints().size());
    assertNotNull(tsc.getVisibleFirstDatapoint());
    assertNotNull(tsc.getVisibleLastDatapoint());
    assertNotSame(tsc.getVisibleFirstDatapoint(), tsc.getVisibleLastDatapoint());
    assertEquals(10.0f, tsc.getVisibleFirstDatapoint().mValue.y);
    assertEquals(10.0f, tsc.getSeriesById(1).getDatapoints().get(0).mValue.y);
    assertEquals(20.0f, tsc.getSeriesById(1).getDatapoints().get(1).mValue.y);
    assertEquals(30.0f, tsc.getVisibleLastDatapoint().mValue.y);
    assertEquals(30.0f, tsc.getSeriesById(1).getDatapoints().get(2).mValue.y);
    
    // three datapoints gathered (from cache), but only 2 within range
    tsc.updateTimeSeriesData(1001, 3000, false);
    assertEquals(3, tsc.getSeriesById(1).getDatapoints().size());
    assertNotNull(tsc.getVisibleFirstDatapoint());
    assertNotNull(tsc.getVisibleLastDatapoint());
    assertNotSame(tsc.getVisibleFirstDatapoint(), tsc.getVisibleLastDatapoint());
    assertEquals(20.0f, tsc.getVisibleFirstDatapoint().mValue.y);
    assertEquals(30.0f, tsc.getVisibleLastDatapoint().mValue.y);

    // three datapoints gathered (from cache), but only 1 within range
    tsc.updateTimeSeriesData(1001, 2999, false);
    assertEquals(3, tsc.getSeriesById(1).getDatapoints().size());
    assertNotNull(tsc.getVisibleFirstDatapoint());
    assertNotNull(tsc.getVisibleLastDatapoint());
    assertSame(tsc.getVisibleFirstDatapoint(), tsc.getVisibleLastDatapoint());
    assertEquals(20.0f, tsc.getVisibleFirstDatapoint().mValue.y);
    assertEquals(20.0f, tsc.getVisibleLastDatapoint().mValue.y);    
  }

  public void testLocking() {

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
}
