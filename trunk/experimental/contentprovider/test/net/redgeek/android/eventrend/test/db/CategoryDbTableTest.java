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

package net.redgeek.android.eventrend.test.db;

import java.util.ArrayList;
import java.util.HashMap;

import junit.framework.TestCase;
import net.redgeek.android.eventrecorder.CategoryDbTable;
import net.redgeek.android.eventrend.test.common.MockCursor;

// Note that several tests use equality comparison on float, which could be 
// dangerous in general, but should be safe for such small predefined values.
public class CategoryDbTableTest extends TestCase {
  private HashMap<Integer, String> getColumnMap() {
    HashMap<Integer, String> map = new HashMap<Integer, String>();

    for (int i = 0; i < CategoryDbTable.KEY_ALL.length; i++) {
      map.put(new Integer(i), CategoryDbTable.KEY_ALL[i]);
    }

    return map;
  }

  public void testConstructors() {
    HashMap<String, String> map = new HashMap<String, String>();
    map.put(CategoryDbTable.KEY_ROWID, "10");
    map.put(CategoryDbTable.KEY_GROUP_NAME, "group");
    map.put(CategoryDbTable.KEY_CATEGORY_NAME, "catName");
    map.put(CategoryDbTable.KEY_DEFAULT_VALUE, "11");
    map.put(CategoryDbTable.KEY_LAST_VALUE, "12");
    map.put(CategoryDbTable.KEY_LAST_TREND, "13");
    map.put(CategoryDbTable.KEY_INCREMENT, "14");
    map.put(CategoryDbTable.KEY_GOAL, "15");
    map.put(CategoryDbTable.KEY_COLOR, "#123456");
    map.put(CategoryDbTable.KEY_TYPE, "Average");
    map.put(CategoryDbTable.KEY_PERIOD_MS, "16");
    map.put(CategoryDbTable.KEY_RANK, "17");
    map.put(CategoryDbTable.KEY_TREND_STATE, "trend_state_bogus");
    map.put(CategoryDbTable.KEY_INTERPOLATION, "Linear");
    map.put(CategoryDbTable.KEY_ZEROFILL, "1");
    map.put(CategoryDbTable.KEY_SYNTHETIC, "1");
    map.put(CategoryDbTable.KEY_FORMULA, "series foo + series bar");
    ArrayList<HashMap<String, String>> contents = new ArrayList<HashMap<String, String>>();
    contents.add(map);

    MockCursor c = new MockCursor();
    c.setColumnMap(getColumnMap());
    c.setQueryResults(contents);
    c.moveToFirst();

    CategoryDbTable.Row row = new CategoryDbTable.Row();
    row.populateFromCursor(c);

    assertEquals(10, row.getId());
    assertEquals("group", row.getGroupName());
    assertEquals("catName", row.getCategoryName());
    assertEquals(11.0f, row.getDefaultValue());
    assertEquals(12.0f, row.getLastValue());
    assertEquals(13.0f, row.getLastTrend());
    assertEquals(14.0f, row.getIncrement());
    assertEquals(15.0f, row.getGoal());
    assertEquals("#123456", row.getColor());
    assertEquals("Average", row.getType());
    assertEquals(16, row.getPeriodMs());
    assertEquals(17, row.getRank());
    assertEquals("trend_state_bogus", row.getTrendState());
    assertEquals("Linear", row.getInterpolation());
    assertEquals(true, row.getZeroFill());
    assertEquals(true, row.getSynthetic());
    assertEquals("series foo + series bar", row.getFormula());

    CategoryDbTable.Row copy = new CategoryDbTable.Row(row);

    assertNotSame(copy, row);
    assertEquals(10, copy.getId());
    assertEquals("group", copy.getGroupName());
    assertEquals("catName", copy.getCategoryName());
    assertEquals(11.0f, copy.getDefaultValue());
    assertEquals(12.0f, copy.getLastValue());
    assertEquals(13.0f, copy.getLastTrend());
    assertEquals(14.0f, copy.getIncrement());
    assertEquals(15.0f, copy.getGoal());
    assertEquals("#123456", copy.getColor());
    assertEquals("Average", copy.getType());
    assertEquals(16, copy.getPeriodMs());
    assertEquals(17, copy.getRank());
    assertEquals("trend_state_bogus", copy.getTrendState());
    assertEquals("Linear", copy.getInterpolation());
    assertEquals(true, copy.getZeroFill());
    assertEquals(true, copy.getSynthetic());
    assertEquals("series foo + series bar", copy.getFormula());
  }
}
