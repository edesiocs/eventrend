///*
// * Copyright (C) 2007 The Android Open Source Project
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package net.redgeek.android.eventrend.test.common;
//
//import net.redgeek.android.eventrecorder.CategoryDbTable;
//import net.redgeek.android.eventrend.db.EntryDbTable;
//import junit.framework.TestCase;
//
//public class DbTestReaderTest extends TestCase {
//  private String makeFilePath(String filename) {
//    String sep = System.getProperty("file.separator");
//    String path = System.getProperty("user.dir");
//    String[] subdir = new String[] { "test", "net", "redgeek", "android",
//        "eventrend", "test", "common", "dbtestdata" };
//
//    for (int i = 0; i < subdir.length; i++) {
//      path += sep + subdir[i];
//    }
//    path += sep + filename;
//    return path;
//  }
//
//  public void testCategoryPopulate() throws Exception {
//    MockEvenTrendDbAdapter dbh = new MockEvenTrendDbAdapter();
//    DbTestReader reader = new DbTestReader(dbh);
//    reader.populateFromFile(makeFilePath("category.xml"));
//    
//    CategoryDbTable.Row row = dbh.fetchCategory(1);
//    
//    assertEquals(1, row.getId());
//    assertEquals("GroupName", row.getGroupName());
//    assertEquals("CategoryName", row.getCategoryName());
//    assertEquals(1.0f, row.getDefaultValue());
//    assertEquals(2.0f, row.getLastValue());
//    assertEquals(3.0f, row.getLastTrend());
//    assertEquals(4.0f, row.getIncrement());
//    assertEquals(5.0f, row.getGoal());
//    assertEquals("#4499cc", row.getColor());
//    assertEquals("Average", row.getType());
//    assertEquals(1000, row.getPeriodMs());
//    assertEquals(2, row.getRank());
//    assertEquals("trend_flat", row.getTrendState());
//    assertEquals("Linear", row.getInterpolation());
//    assertEquals("Linear", row.getInterpolation());
//    assertEquals(true, row.getZeroFill());
//    assertEquals(true, row.getSynthetic());
//    assertEquals("(series \"one\" + series \"two\")", row.getFormula());
//  }
//
//  public void testEntryPopulate() throws Exception {
//    MockEvenTrendDbAdapter dbh = new MockEvenTrendDbAdapter();
//    DbTestReader reader = new DbTestReader(dbh);
//    reader.populateFromFile(makeFilePath("entry.xml"));
//    
//    EntryDbTable.Row row = dbh.fetchEntry(1);
//    
//    assertEquals(1, row.getId());
//    assertEquals(2, row.getCategoryId());
//    assertEquals(1000, row.getTimestamp());
//    assertEquals(4.0f, row.getValue());
//    assertEquals(5, row.getNEntries());
//  }
//
//  public void testPopulateMultipleEntries() throws Exception {
//    MockEvenTrendDbAdapter dbh = new MockEvenTrendDbAdapter();
//    DbTestReader reader = new DbTestReader(dbh);
//    reader.populateFromFile(makeFilePath("two_entries.xml"));
//    
//    EntryDbTable.Row row = dbh.fetchEntry(1);
//    assertEquals(1, row.getId());
//    assertEquals(2, row.getCategoryId());
//    assertEquals(1000, row.getTimestamp());
//    assertEquals(4.0f, row.getValue());
//    assertEquals(5, row.getNEntries());
//
//    row = dbh.fetchEntry(2);
//    assertEquals(2, row.getId());
//    assertEquals(2, row.getCategoryId());
//    assertEquals(2000, row.getTimestamp());
//    assertEquals(2.0f, row.getValue());
//    assertEquals(2, row.getNEntries());
//  }
//
//  public void testPopulateMultipleTables() throws Exception {
//    MockEvenTrendDbAdapter dbh = new MockEvenTrendDbAdapter();
//    DbTestReader reader = new DbTestReader(dbh);
//    reader.populateFromFile(makeFilePath("two_tables.xml"));
//    
//    CategoryDbTable.Row row = dbh.fetchCategory(1);
//    
//    assertEquals(1, row.getId());
//    assertEquals("GroupName", row.getGroupName());
//    assertEquals("CategoryName", row.getCategoryName());
//    assertEquals(1.0f, row.getDefaultValue());
//    assertEquals(2.0f, row.getLastValue());
//    assertEquals(3.0f, row.getLastTrend());
//    assertEquals(4.0f, row.getIncrement());
//    assertEquals(5.0f, row.getGoal());
//    assertEquals("#4499cc", row.getColor());
//    assertEquals("Average", row.getType());
//    assertEquals(1000, row.getPeriodMs());
//    assertEquals(2, row.getRank());
//    assertEquals("trend_flat", row.getTrendState());
//    assertEquals("Linear", row.getInterpolation());
//    assertEquals("Linear", row.getInterpolation());
//    assertEquals(true, row.getZeroFill());
//    assertEquals(true, row.getSynthetic());
//    assertEquals("(series \"one\" + series \"two\")", row.getFormula());
//
//    EntryDbTable.Row ent = dbh.fetchEntry(1);
//    assertEquals(1, ent.getId());
//    assertEquals(2, ent.getCategoryId());
//    assertEquals(1000, ent.getTimestamp());
//    assertEquals(4.0f, ent.getValue());
//    assertEquals(5, ent.getNEntries());
//  }
//
//}
