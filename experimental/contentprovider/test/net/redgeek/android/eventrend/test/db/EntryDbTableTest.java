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
import net.redgeek.android.eventrend.test.common.MockCursor;
import net.redgeek.android.timeseries.EntryDbTable;

// Note that several tests use equality comparison on float, which could be 
// dangerous in general, but should be safe for such small predefined values.
public class EntryDbTableTest extends TestCase {
  private HashMap<Integer, String> getColumnMap() {
    HashMap<Integer, String> map = new HashMap<Integer, String>();

    for (int i = 0; i < EntryDbTable.KEY_ALL.length; i++) {
      map.put(new Integer(i), EntryDbTable.KEY_ALL[i]);
    }

    return map;
  }

  public void testConstructors() {
    HashMap<String, String> map = new HashMap<String, String>();
    map.put(EntryDbTable.KEY_ROWID, "1");
    map.put(EntryDbTable.KEY_CATEGORY_ID, "2");
    map.put(EntryDbTable.KEY_TIMESTAMP, "3");
    map.put(EntryDbTable.KEY_VALUE, "4.0");
    map.put(EntryDbTable.KEY_N_ENTRIES, "5");

    ArrayList<HashMap<String, String>> contents = new ArrayList<HashMap<String, String>>();
    contents.add(map);

    MockCursor c = new MockCursor();
    c.setColumnMap(getColumnMap());
    c.setQueryResults(contents);
    c.moveToFirst();

    EntryDbTable.Row row = new EntryDbTable.Row();
    row.populateFromCursor(c);

    assertEquals(1, row.getId());
    assertEquals(2, row.getCategoryId());
    assertEquals(3, row.getTimestamp());
    assertEquals(4.0f, row.getValue());
    assertEquals(5, row.getNEntries());

    EntryDbTable.Row copy = new EntryDbTable.Row(row);

    assertNotSame(copy, row);
    assertEquals(1, row.getId());
    assertEquals(2, row.getCategoryId());
    assertEquals(3, row.getTimestamp());
    assertEquals(4.0f, row.getValue());
    assertEquals(5, row.getNEntries());
  }
}
