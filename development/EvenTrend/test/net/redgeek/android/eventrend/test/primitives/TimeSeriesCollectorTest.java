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

  private HashMap<Integer, String> newCategoryColumnMap() {
    HashMap<Integer, String> map = new HashMap<Integer, String>();
    for (int i = 0; i < CategoryDbTable.KEY_ALL.length; i++) {
      map.put(new Integer(i), CategoryDbTable.KEY_ALL[i]);
    }
    return map;
  }

  private HashMap<String, String> newCategoryMap(CategoryDbTable.Row row) {
    HashMap<String, String> map = new HashMap<String, String>();
    map.put(CategoryDbTable.KEY_ROWID, Long.toString(row.getId()));
    map.put(CategoryDbTable.KEY_GROUP_NAME, row.getGroupName());
    map.put(CategoryDbTable.KEY_CATEGORY_NAME, row.getCategoryName());
    map.put(CategoryDbTable.KEY_DEFAULT_VALUE, Float.toString(row
        .getDefaultValue()));
    map.put(CategoryDbTable.KEY_LAST_VALUE, Float.toString(row.getLastValue()));
    map.put(CategoryDbTable.KEY_LAST_TREND, Float.toString(row.getLastTrend()));
    map.put(CategoryDbTable.KEY_INCREMENT, Float.toString(row.getIncrement()));
    map.put(CategoryDbTable.KEY_GOAL, Float.toString(row.getGoal()));
    map.put(CategoryDbTable.KEY_COLOR, row.getColor());
    map.put(CategoryDbTable.KEY_TYPE, row.getType());
    map.put(CategoryDbTable.KEY_PERIOD_MS, Long.toString(row.getPeriodMs()));
    map.put(CategoryDbTable.KEY_RANK, Integer.toString(row.getRank()));
    map.put(CategoryDbTable.KEY_PERIOD_ENTRIES, Integer.toString(row
        .getPeriodEntries()));
    map.put(CategoryDbTable.KEY_TREND_STATE, row.getTrendState());
    map.put(CategoryDbTable.KEY_INTERPOLATION, row.getInterpolation());
    if (row.getZeroFill() == true)
      map.put(CategoryDbTable.KEY_ZEROFILL, "1");
    else
      map.put(CategoryDbTable.KEY_ZEROFILL, "0");
    if (row.getSynthetic() == true)
      map.put(CategoryDbTable.KEY_SYNTHETIC, "1");
    else
      map.put(CategoryDbTable.KEY_SYNTHETIC, "0");
    map.put(CategoryDbTable.KEY_FORMULA, row.getFormula());
    return map;
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
    ArrayList<HashMap<String, String>> results;
    HashMap<Integer, String> catCols;
    MockEvenTrendDbAdapter dbh;

    dbh = new MockEvenTrendDbAdapter();
    results = new ArrayList<HashMap<String, String>>();
    catCols = newCategoryColumnMap();

    tsc = newTSC(dbh);
    cat1 = new CategoryDbTable.Row();
    cat2 = new CategoryDbTable.Row();
    cat1.setId(1);
    cat1.setCategoryName("cat1");
    cat1.setSynthetic(true);
    cat1.setFormula("series \"cat2\" + series \"cat2\"");
    cat2.setId(2);
    cat2.setCategoryName("cat2");
    HashMap<String, String> res1 = newCategoryMap(cat1);
    HashMap<String, String> res2 = newCategoryMap(cat2);
    results.add(res1);
    results.add(res2);
//    dbh.setColumnMap(catCols);
//    dbh.setQueryResults(results);

    // initial: 2 categories
    tsc.updateTimeSeriesMeta(true);
    assertEquals(2, tsc.getAllSeries().size());
    assertNotNull(tsc.getSeries(0).getDbRow());
    assertNotSame(tsc.getSeries(0), tsc.getSeries(1));
    assertNotNull(tsc.getSeries(0).getDbRow());
    assertEquals(cat1.getId(), tsc.getSeries(0).getDbRow().getId());
    assertNotNull(tsc.getSeries(1).getDbRow());
    assertEquals(cat2.getId(), tsc.getSeries(1).getDbRow().getId());
    assertNotNull(tsc.getSeries(0).getInterpolator());
    assertNotNull(tsc.getSeries(1).getInterpolator());
    assertNotSame(tsc.getSeries(0), tsc.getSeries(1));
    assertEquals(1, tsc.getSeries(0).getDependents().size());
    assertEquals(0, tsc.getSeries(0).getDependees().size());
    assertEquals(0, tsc.getSeries(1).getDependents().size());
    assertEquals(1, tsc.getSeries(1).getDependees().size());
    assertEquals(tsc.getSeries(1), tsc.getSeries(0).getDependents().get(0));
    assertEquals(tsc.getSeries(0), tsc.getSeries(1).getDependees().get(0));

    // Note: these tests being to get quite state-based, hence not really
    // unittests, but it's a pain to do otherwise. Probably indicative that
    // the entire class(es) need to be refactored.

    // enabled series 1 and 2, add a category
    tsc.setSeriesEnabled(1, true);
    tsc.toggleSeriesEnabled(2);
    CategoryDbTable.Row cat3 = new CategoryDbTable.Row();
    cat3.setId(3);
    cat3.setCategoryName("cat3");
    HashMap<String, String> res3 = newCategoryMap(cat3);
    results.add(res3);
//    dbh.setColumnMap(catCols);
//    dbh.setQueryResults(results);

    tsc.updateTimeSeriesMeta(false);
    assertEquals(3, tsc.getAllSeries().size());
    assertEquals(cat1.getId(), tsc.getSeries(0).getDbRow().getId());
    assertEquals(cat2.getId(), tsc.getSeries(1).getDbRow().getId());
    assertEquals(cat3.getId(), tsc.getSeries(2).getDbRow().getId());
    assertNotSame(tsc.getSeries(0), tsc.getSeries(1));
    assertEquals(1, tsc.getSeries(0).getDependents().size());
    assertEquals(0, tsc.getSeries(0).getDependees().size());
    assertEquals(0, tsc.getSeries(1).getDependents().size());
    assertEquals(1, tsc.getSeries(1).getDependees().size());
    assertTrue(tsc.isSeriesEnabled(1));
    assertTrue(tsc.isSeriesEnabled(2));
    assertFalse(tsc.isSeriesEnabled(3));
    assertEquals("cat1", tsc.getSeriesById(1).getDbRow().getCategoryName());
    assertEquals("cat2", tsc.getSeriesById(2).getDbRow().getCategoryName());
    assertEquals("cat3", tsc.getSeriesById(3).getDbRow().getCategoryName());
    assertEquals("cat1", tsc.getSeriesByName("cat1").getDbRow()
        .getCategoryName());
    assertEquals("cat2", tsc.getSeriesByName("cat2").getDbRow()
        .getCategoryName());
    assertEquals("cat3", tsc.getSeriesByName("cat3").getDbRow()
        .getCategoryName());
  }

  public void testLocking() {

  }
}
