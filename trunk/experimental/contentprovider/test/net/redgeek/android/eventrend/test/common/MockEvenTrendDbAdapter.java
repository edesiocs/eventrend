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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;

import net.redgeek.android.eventrend.db.EvenTrendDbAdapter;
import net.redgeek.android.eventrend.util.DateUtil;
import net.redgeek.android.eventrend.util.DateUtil.Period;
import net.redgeek.android.timeseries.CategoryDbTable;
import net.redgeek.android.timeseries.EntryDbTable;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.test.mock.MockContext;


// Urgh ... this mock probably needs it's own unittests ... :(
public class MockEvenTrendDbAdapter implements EvenTrendDbAdapter {
  private HashMap<String, ArrayList<HashMap<String, String>>> mTables;
  private HashMap<String, HashMap<Integer, String>> mColumnMap;
  
  public MockEvenTrendDbAdapter() {
    initialize();
  }

  public MockEvenTrendDbAdapter(MockContext context) {
    initialize();
  }
  
  private void initialize() {
    mTables = new HashMap<String, ArrayList<HashMap<String, String>>>();
    mColumnMap = new HashMap<String, HashMap<Integer, String>>(); 
  }

  public void setColumnMap(String table, HashMap<Integer, String> columnMap) {
    HashMap<Integer, String> map = new HashMap<Integer, String>(columnMap);
    mColumnMap.put(table, map);
  }

  public void addContent(String table, HashMap<String, String> row) {
    ArrayList<HashMap<String, String>> rows;
    rows = mTables.get(table);
    if (rows == null) {
      rows = new ArrayList<HashMap<String, String>>();
      mTables.put(table, rows);
    }
    rows.add(row);
  }
  
  public void close() {
    mTables.clear();
    mColumnMap.clear();
  }

  public long createCategory(CategoryDbTable.Row category) {
    ArrayList<HashMap<String, String>> rows = mTables.get(CategoryDbTable.TABLE_NAME);
    
    long max = 1;
    for (int i = 0; i < rows.size(); i++) {
      long id = Long.parseLong(rows.get(i).get(CategoryDbTable.KEY_ROWID));
      if (id > max)
        max = id;
    }
    max++;
    category.setId(max);
    rows.add(categoryRowToHashMap(category));
    return max;
  }

  public long createEntry(EntryDbTable.Row entry) {
    ArrayList<HashMap<String, String>> rows = mTables.get(EntryDbTable.TABLE_NAME);
    
    long max = 1;
    for (int i = 0; i < rows.size(); i++) {
      long id = Long.parseLong(rows.get(i).get(EntryDbTable.KEY_ROWID));
      if (id > max)
        max = id;
    }
    max++;
    entry.setId(max);
    rows.add(entryRowToHashMap(entry));
    return max;
  }

  public boolean deleteAllCategories() {
    ArrayList<HashMap<String, String>> rows = mTables.get(CategoryDbTable.TABLE_NAME);
    if (rows == null)
      return false;
    rows.clear();
    return true;
  }

  public boolean deleteAllEntries() {
    ArrayList<HashMap<String, String>> rows = mTables.get(EntryDbTable.TABLE_NAME);
    if (rows == null)
      return false;
    rows.clear();
    return true;
  }

  public boolean deleteCategory(long rowId) {
    ArrayList<HashMap<String, String>> rows = mTables.get(CategoryDbTable.TABLE_NAME);
    
    for (int i = 0; i < rows.size(); i++) {
      long id = Long.parseLong(rows.get(i).get(CategoryDbTable.KEY_ROWID));
      if (id == rowId) {
        rows.remove(i);
        return true;
      }
    }
    return false;
  }

  public boolean deleteCategoryEntries(long catId) {
    ArrayList<HashMap<String, String>> rows = mTables.get(EntryDbTable.TABLE_NAME);
    boolean removed = false;
    for (int i = 0; i < rows.size(); i++) {
      long id = Long.parseLong(rows.get(i).get(EntryDbTable.KEY_CATEGORY_ID));
      if (id == catId) {
        rows.remove(i);
        removed = true;
      }
    }
    return removed;
  }

  public boolean deleteEntry(long rowId) {
    ArrayList<HashMap<String, String>> rows = mTables.get(EntryDbTable.TABLE_NAME);
    
    for (int i = 0; i < rows.size(); i++) {
      long id = Long.parseLong(rows.get(i).get(CategoryDbTable.KEY_ROWID));
      if (id == rowId) {
        rows.remove(i);
        return true;
      }
    }
    return false;
  }

  public Cursor fetchAllCategories() {
    // TODO:  order this
    HashMap<Integer, String> colMap = mColumnMap.get(CategoryDbTable.TABLE_NAME);
    ArrayList<HashMap<String, String>> rows = mTables.get(CategoryDbTable.TABLE_NAME);

    MockCursor c = new MockCursor();
    c.setColumnMap(colMap);
    c.setQueryResults(rows);
    
    return c;
  }

  public Cursor fetchAllEntries() {
    HashMap<Integer, String> colMap = mColumnMap.get(EntryDbTable.TABLE_NAME);
    ArrayList<HashMap<String, String>> rows = mTables.get(EntryDbTable.TABLE_NAME);

    MockCursor c = new MockCursor();
    c.setColumnMap(colMap);
    c.setQueryResults(rows);
    
    return c;
  }

  public Cursor fetchAllGroups() {
    ArrayList<HashMap<String, String>> rows = mTables.get(CategoryDbTable.TABLE_NAME);
    ArrayList<HashMap<String, String>> result = new ArrayList<HashMap<String, String>>();
    HashMap<String, Boolean> distinct = new HashMap<String, Boolean>();
    HashMap<String, String> row;
    
    for (int i = 0; i < rows.size(); i++) {
      String group = rows.get(i).get(CategoryDbTable.KEY_GROUP_NAME);
      distinct.put(group, true);
    }
    
    Iterator<String> iterator = distinct.keySet().iterator();
    while (iterator.hasNext()) {
      row = new HashMap<String, String>();
      row.put(CategoryDbTable.KEY_GROUP_NAME, iterator.next());
      result.add(row);
    }
    
    MockCursor c = new MockCursor();

    HashMap<Integer, String> colMap = new HashMap<Integer, String>();
    colMap.put(0, CategoryDbTable.KEY_GROUP_NAME);
    c.setColumnMap(colMap);
    c.setQueryResults(result);
    return c;
  }

  public Cursor fetchAllSynthetics() {
    // TODO:  order this

    HashMap<Integer, String> colMap = mColumnMap.get(EntryDbTable.TABLE_NAME);
    ArrayList<HashMap<String, String>> rows = mTables.get(CategoryDbTable.TABLE_NAME);
    ArrayList<HashMap<String, String>> result = new ArrayList<HashMap<String, String>>();
    
    for (int i = 0; i < rows.size(); i++) {
      int synthetic = Integer.parseInt(rows.get(i).get(CategoryDbTable.KEY_SYNTHETIC));
      if (synthetic != 0)
        result.add(rows.get(i));
    }

    MockCursor c = new MockCursor();
    c.setColumnMap(colMap);
    c.setQueryResults(rows);
    
    return c;
  }

  public CategoryDbTable.Row fetchCategory(long rowId) {
    CategoryDbTable.Row row = null;
    ArrayList<HashMap<String, String>> rows = mTables.get(CategoryDbTable.TABLE_NAME);
    
    for (int i = 0; i < rows.size(); i++) {
      long id = Long.parseLong(rows.get(i).get(CategoryDbTable.KEY_ROWID));
      if (id == rowId) {
        row = hashMapToCategoryRow(rows.get(i));
        break;
      }
    }

    return row;
  }

  public CategoryDbTable.Row fetchCategory(String category) {
    CategoryDbTable.Row row = null;
    ArrayList<HashMap<String, String>> rows = mTables.get(CategoryDbTable.TABLE_NAME);
    
    for (int i = 0; i < rows.size(); i++) {
      String name = rows.get(i).get(CategoryDbTable.KEY_CATEGORY_NAME);
      if (name.equals(category)) {
        row = hashMapToCategoryRow(rows.get(i));
        break;
      }
    }

    return row;
  }

  public Cursor fetchCategoryEntries(long catId) {
    HashMap<Integer, String> colMap = mColumnMap.get(EntryDbTable.TABLE_NAME);
    ArrayList<HashMap<String, String>> rows = mTables.get(EntryDbTable.TABLE_NAME);
    ArrayList<HashMap<String, String>> result = new ArrayList<HashMap<String, String>>();
    TreeMap<Long, HashMap<String, String>> ordered = new TreeMap<Long, HashMap<String, String>>();
    HashMap<String, String> row;
    
    for (int i = 0; i < rows.size(); i++) {
      long id = Long.parseLong(rows.get(i).get(EntryDbTable.KEY_CATEGORY_ID));
      if (id == catId) {
        long time = Long.parseLong(rows.get(i).get(EntryDbTable.KEY_TIMESTAMP));
        ordered.put(new Long(time), rows.get(i));
      }
    }
    
    Iterator<Long> iterator = ordered.keySet().iterator();
    while (iterator.hasNext()) {
      row = ordered.get(iterator.next());
      result.add(row);
    }
    
    MockCursor c = new MockCursor();
    c.setColumnMap(colMap);
    c.setQueryResults(result);
    return c;
  }

  public Cursor fetchCategoryEntriesRange(long catId, long milliStart,
      long milliEnd) {
    HashMap<Integer, String> colMap = mColumnMap.get(EntryDbTable.TABLE_NAME);
    ArrayList<HashMap<String, String>> rows = mTables.get(EntryDbTable.TABLE_NAME);
    ArrayList<HashMap<String, String>> result = new ArrayList<HashMap<String, String>>();
    TreeMap<Long, HashMap<String, String>> ordered = new TreeMap<Long, HashMap<String, String>>();
    HashMap<String, String> row;
    
    for (int i = 0; i < rows.size(); i++) {
      long id = Long.parseLong(rows.get(i).get(EntryDbTable.KEY_CATEGORY_ID));
      if (id == catId) {
        long time = Long.parseLong(rows.get(i).get(EntryDbTable.KEY_TIMESTAMP));
        if (time >= milliStart && time <= milliEnd) {
          ordered.put(new Long(time), rows.get(i));
        }
      }
    }
    
    Iterator<Long> iterator = ordered.keySet().iterator();
    while (iterator.hasNext()) {
      row = ordered.get(iterator.next());
      result.add(row);
    }
    
    MockCursor c = new MockCursor();
    c.setColumnMap(colMap);
    c.setQueryResults(result);
    return c;
  }

  // TODO:  abstract out the date calculation part of this and the real 
  // DB adapter
  public EntryDbTable.Row fetchCategoryEntryInPeriod(long catId, long period,
      long date_ms) {
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(date_ms);

    Period p = DateUtil.mapLongToPeriod(period);
    DateUtil.setToPeriodStart(cal, p);
    long min = cal.getTimeInMillis();

    int step = 1;
    if (p == Period.QUARTER)
      step = 3;
    cal.add(DateUtil.mapLongToCal(period), step);
    long max = cal.getTimeInMillis();

    ArrayList<HashMap<String, String>> rows = mTables.get(EntryDbTable.TABLE_NAME);
    TreeMap<Long, HashMap<String, String>> ordered = new TreeMap<Long, HashMap<String, String>>();
    HashMap<String, String> map = null;
    
    for (int i = 0; i < rows.size(); i++) {
      long id = Long.parseLong(rows.get(i).get(EntryDbTable.KEY_CATEGORY_ID));
      if (id == catId) {
        long time = Long.parseLong(rows.get(i).get(EntryDbTable.KEY_TIMESTAMP));
        if (time >= min && time < max) {
          ordered.put(new Long(time), rows.get(i));
        }
      }
    }
    
    Iterator<Long> iterator = ordered.keySet().iterator();
    while (iterator.hasNext()) {
      map = ordered.get(iterator.next());
    }
    return hashMapToEntryRow(map);
  }

  public long fetchCategoryId(String category) {
    CategoryDbTable.Row row = null;
    ArrayList<HashMap<String, String>> rows = mTables.get(CategoryDbTable.TABLE_NAME);
    
    for (int i = 0; i < rows.size(); i++) {
      String name = rows.get(i).get(CategoryDbTable.KEY_CATEGORY_NAME);
      if (name.equals(category)) {
        row = hashMapToCategoryRow(rows.get(i));
        break;
      }
    }

    return row.getId();
  }

  public int fetchCategoryMaxRank() {
    ArrayList<HashMap<String, String>> rows = mTables.get(CategoryDbTable.TABLE_NAME);
    
    int max = 1;
    for (int i = 0; i < rows.size(); i++) {
      int rank = Integer.parseInt(rows.get(i).get(CategoryDbTable.KEY_RANK));
      if (rank > max)
        max = rank;
    }
    return max;
  }

  public Cursor fetchEntriesRange(long milliStart, long milliEnd) {
    HashMap<Integer, String> colMap = mColumnMap.get(EntryDbTable.TABLE_NAME);
    ArrayList<HashMap<String, String>> rows = mTables.get(EntryDbTable.TABLE_NAME);
    ArrayList<HashMap<String, String>> result = new ArrayList<HashMap<String, String>>();
    TreeMap<Long, HashMap<String, String>> ordered = new TreeMap<Long, HashMap<String, String>>();
    HashMap<String, String> row;
    
    for (int i = 0; i < rows.size(); i++) {
      long time = Long.parseLong(rows.get(i).get(EntryDbTable.KEY_TIMESTAMP));
      if (time >= milliStart && time <= milliEnd) {
        ordered.put(new Long(time), rows.get(i));
      }
    }
    
    Iterator<Long> iterator = ordered.keySet().iterator();
    while (iterator.hasNext()) {
      row = ordered.get(iterator.next());
      result.add(row);
    }
    
    MockCursor c = new MockCursor();
    c.setColumnMap(colMap);
    c.setQueryResults(result);
    return c;
  }

  public EntryDbTable.Row fetchEntry(long rowId) {
    EntryDbTable.Row row = null;
    ArrayList<HashMap<String, String>> rows = mTables.get(EntryDbTable.TABLE_NAME);
    
    for (int i = 0; i < rows.size(); i++) {
      long id = Long.parseLong(rows.get(i).get(EntryDbTable.KEY_ROWID));
      if (id == rowId) {
        row = hashMapToEntryRow(rows.get(i));
        break;
      }
    }

    return row;
  }

  public EntryDbTable.Row fetchLastCategoryEntry(long catId) {
    ArrayList<HashMap<String, String>> rows = mTables.get(EntryDbTable.TABLE_NAME);
    TreeMap<Long, HashMap<String, String>> ordered = new TreeMap<Long, HashMap<String, String>>();
    HashMap<String, String> row = null;
    
    for (int i = 0; i < rows.size(); i++) {
      long id = Long.parseLong(rows.get(i).get(EntryDbTable.KEY_CATEGORY_ID));
      if (id == catId) {
        long time = Long.parseLong(rows.get(i).get(EntryDbTable.KEY_TIMESTAMP));
        ordered.put(new Long(time), rows.get(i));
      }
    }
    
    Iterator<Long> iterator = ordered.keySet().iterator();
    while (iterator.hasNext()) {
      row = ordered.get(iterator.next());
    }
    return hashMapToEntryRow(row);
  }

  public Cursor fetchRecentCategoryEntries(long catId, int items) {
    HashMap<Integer, String> colMap = mColumnMap.get(EntryDbTable.TABLE_NAME);
    ArrayList<HashMap<String, String>> rows = mTables.get(EntryDbTable.TABLE_NAME);
    ArrayList<HashMap<String, String>> result = new ArrayList<HashMap<String, String>>();
    TreeMap<Long, HashMap<String, String>> ordered = new TreeMap<Long, HashMap<String, String>>(java.util.Collections.reverseOrder());
    HashMap<String, String> row;
    
    for (int i = 0; i < rows.size(); i++) {
      long id = Long.parseLong(rows.get(i).get(EntryDbTable.KEY_CATEGORY_ID));
      if (id == catId) {
        long time = Long.parseLong(rows.get(i).get(EntryDbTable.KEY_TIMESTAMP));
        ordered.put(new Long(time), rows.get(i));
      }
    }

    // need to return these in reverse chronological to match the sql adapter
    int i = 0;
    Iterator<Long> iterator = ordered.keySet().iterator();
    while (iterator.hasNext() && i < items) {
      row = ordered.get(iterator.next());
      result.add(row);
      i++;
    }
    
    MockCursor c = new MockCursor();
    c.setColumnMap(colMap);
    c.setQueryResults(result);
    return c;
  }

  public Cursor fetchRecentEntries(int items, int skip) {
    HashMap<Integer, String> colMap = mColumnMap.get(EntryDbTable.TABLE_NAME);
    ArrayList<HashMap<String, String>> rows = mTables.get(EntryDbTable.TABLE_NAME);
    ArrayList<HashMap<String, String>> result = new ArrayList<HashMap<String, String>>();
    TreeMap<Long, HashMap<String, String>> ordered = new TreeMap<Long, HashMap<String, String>>(java.util.Collections.reverseOrder());
    HashMap<String, String> row;
    
    for (int i = 0; i < rows.size(); i++) {
      long time = Long.parseLong(rows.get(i).get(EntryDbTable.KEY_TIMESTAMP));
      ordered.put(new Long(time), rows.get(i));
    }

    int i = 0;
    Iterator<Long> iterator = ordered.keySet().iterator();
    while (iterator.hasNext() && i < skip) {
      row = ordered.get(iterator.next());
      i++;
    }
    
    while (iterator.hasNext() && i < items) {
      row = ordered.get(iterator.next());
      result.add(0, row);
      i++;
    }
    
    MockCursor c = new MockCursor();
    c.setColumnMap(colMap);
    c.setQueryResults(result);
    return c;
  }

  public Cursor fetchRecentEntries(int items, long catId, int skip) {
    HashMap<Integer, String> colMap = mColumnMap.get(EntryDbTable.TABLE_NAME);
    ArrayList<HashMap<String, String>> rows = mTables.get(EntryDbTable.TABLE_NAME);
    ArrayList<HashMap<String, String>> result = new ArrayList<HashMap<String, String>>();
    TreeMap<Long, HashMap<String, String>> ordered = new TreeMap<Long, HashMap<String, String>>(java.util.Collections.reverseOrder());
    HashMap<String, String> row;
    
    for (int i = 0; i < rows.size(); i++) {
      long id = Long.parseLong(rows.get(i).get(EntryDbTable.KEY_CATEGORY_ID));
      if (id == catId) {
        long time = Long.parseLong(rows.get(i).get(EntryDbTable.KEY_TIMESTAMP));
        ordered.put(new Long(time), rows.get(i));
      }
    }

    int i = 0;
    Iterator<Long> iterator = ordered.keySet().iterator();
    while (iterator.hasNext() && i < skip) {
      row = ordered.get(iterator.next());
      i++;
    }
    
    while (iterator.hasNext() && i < items) {
      row = ordered.get(iterator.next());
      result.add(0, row);
      i++;
    }
    
    MockCursor c = new MockCursor();
    c.setColumnMap(colMap);
    c.setQueryResults(result);
    return c;
  }

  // TODO: abstract out the non-db stuff from this method in the sql adapter,
  // so it can actually be tests
  public String flattenDB() {
    // TODO Auto-generated method stub
    return null;
  }

  public EvenTrendDbAdapter open() throws SQLException {
    return this;
  }

  public boolean updateCategory(CategoryDbTable.Row category) {
    ArrayList<HashMap<String, String>> rows = mTables.get(CategoryDbTable.TABLE_NAME);
    
    for (int i = 0; i < rows.size(); i++) {
      long id = Long.parseLong(rows.get(i).get(CategoryDbTable.KEY_ROWID));
      if (id == category.getId()) {
        HashMap<String, String> map = categoryRowToHashMap(category);
        rows.remove(i);
        rows.add(i, map);
        return true;
      }
    }

    return false;
  }

  public boolean updateCategory(long id, ContentValues args) {
    // TODO
    return false;
  }

  public boolean updateCategoryLastValue(long rowId, float value) {
    ArrayList<HashMap<String, String>> rows = mTables.get(CategoryDbTable.TABLE_NAME);
    
    for (int i = 0; i < rows.size(); i++) {
      long id = Long.parseLong(rows.get(i).get(CategoryDbTable.KEY_ROWID));
      if (id == rowId) {
        rows.get(i).put(CategoryDbTable.KEY_LAST_VALUE, Float.toString(value));
        return true;
      }
    }

    return false;
  }

  public boolean updateCategoryRank(long rowId, int rank) {
    ArrayList<HashMap<String, String>> rows = mTables.get(CategoryDbTable.TABLE_NAME);
    
    for (int i = 0; i < rows.size(); i++) {
      long id = Long.parseLong(rows.get(i).get(CategoryDbTable.KEY_ROWID));
      if (id == rowId) {
        rows.get(i).put(CategoryDbTable.KEY_RANK, Integer.toString(rank));
        return true;
      }
    }

    return false;
  }

  public boolean updateCategoryTrend(long catId, String trendStr, float newTrend) {
    ArrayList<HashMap<String, String>> rows = mTables.get(CategoryDbTable.TABLE_NAME);
    
    for (int i = 0; i < rows.size(); i++) {
      long id = Long.parseLong(rows.get(i).get(CategoryDbTable.KEY_ROWID));
      if (id == catId) {
        rows.get(i).put(CategoryDbTable.KEY_LAST_TREND, Float.toString(newTrend));
        rows.get(i).put(CategoryDbTable.KEY_TREND_STATE, trendStr);
        return true;
      }
    }

    return false;
  }

  public boolean updateEntry(EntryDbTable.Row entry) {
    ArrayList<HashMap<String, String>> rows = mTables.get(EntryDbTable.TABLE_NAME);
    
    for (int i = 0; i < rows.size(); i++) {
      long id = Long.parseLong(rows.get(i).get(EntryDbTable.KEY_ROWID));
      if (id == entry.getId()) {
        HashMap<String, String> map = entryRowToHashMap(entry);
        rows.remove(i);
        rows.add(i, map);
        return true;
      }
    }

    return false;
  }
  
  public HashMap<String, String> categoryRowToHashMap(CategoryDbTable.Row row) {
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

  public HashMap<String, String> entryRowToHashMap(EntryDbTable.Row row) {
    HashMap<String, String> map = new HashMap<String, String>();
    map.put(EntryDbTable.KEY_ROWID, Long.toString(row.getId()));
    map.put(EntryDbTable.KEY_CATEGORY_ID, Long.toString(row.getCategoryId()));
    map.put(EntryDbTable.KEY_TIMESTAMP, Long.toString(row.getTimestamp()));
    map.put(EntryDbTable.KEY_VALUE, Float.toString(row.getValue()));
    map.put(EntryDbTable.KEY_N_ENTRIES, Integer.toString(row.getNEntries()));
    return map;
  }
  
  public CategoryDbTable.Row hashMapToCategoryRow(HashMap<String, String> map) {
    if (map == null)
      return null;
    
    CategoryDbTable.Row row = new CategoryDbTable.Row();
    row.setId(Long.valueOf(map.get(CategoryDbTable.KEY_ROWID)));
    row.setGroupName(map.get(CategoryDbTable.KEY_GROUP_NAME));
    row.setCategoryName(map.get(CategoryDbTable.KEY_CATEGORY_NAME));
    row.setDefaultValue(Float.valueOf(map.get(CategoryDbTable.KEY_DEFAULT_VALUE)));
    row.setLastValue(Float.valueOf(map.get(CategoryDbTable.KEY_LAST_VALUE)));
    row.setLastTrend(Float.valueOf(map.get(CategoryDbTable.KEY_LAST_TREND)));
    row.setIncrement(Float.valueOf(map.get(CategoryDbTable.KEY_INCREMENT)));
    row.setGoal(Float.valueOf(map.get(CategoryDbTable.KEY_GOAL)));
    row.setColor(map.get(CategoryDbTable.KEY_COLOR));
    row.setType(map.get(CategoryDbTable.KEY_TYPE));
    row.setPeriodMs(Long.valueOf(map.get(CategoryDbTable.KEY_PERIOD_MS)));
    row.setRank(Integer.valueOf(map.get(CategoryDbTable.KEY_RANK)));
    row.setTrendState(map.get(CategoryDbTable.KEY_TREND_STATE));
    row.setInterpolation(map.get(CategoryDbTable.KEY_INTERPOLATION));
    if (map.get(CategoryDbTable.KEY_ZEROFILL).equals("0"))
      row.setZeroFill(false);
    else
      row.setZeroFill(true);
    if (map.get(CategoryDbTable.KEY_SYNTHETIC).equals("0"))
      row.setSynthetic(false);
    else
      row.setSynthetic(true);
    row.setFormula(map.get(CategoryDbTable.KEY_FORMULA));
    return row;
  }

  public EntryDbTable.Row hashMapToEntryRow(HashMap<String, String> map) {
    if (map == null)
      return null;

    EntryDbTable.Row row = new EntryDbTable.Row();
    row.setId(Long.valueOf(map.get(EntryDbTable.KEY_ROWID)));
    row.setCategoryId(Long.valueOf(map.get(EntryDbTable.KEY_CATEGORY_ID)));
    row.setTimestamp(Long.valueOf(map.get(EntryDbTable.KEY_TIMESTAMP)));
    row.setValue(Float.valueOf(map.get(EntryDbTable.KEY_VALUE)));
    row.setNEntries(Integer.valueOf(map.get(EntryDbTable.KEY_N_ENTRIES)));
    return row;
  }
}
