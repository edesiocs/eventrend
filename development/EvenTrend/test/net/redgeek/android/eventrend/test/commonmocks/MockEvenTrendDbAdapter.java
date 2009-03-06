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

package net.redgeek.android.eventrend.test.commonmocks;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.test.mock.MockContext;

import net.redgeek.android.eventrend.db.CategoryDbTable;
import net.redgeek.android.eventrend.db.EntryDbTable;
import net.redgeek.android.eventrend.db.EvenTrendDbAdapter;
import net.redgeek.android.eventrend.db.FormulaCacheDbTable.Item;

import java.util.ArrayList;
import java.util.HashMap;

public class MockEvenTrendDbAdapter implements EvenTrendDbAdapter {
  private MockCursor mCursor;
  private long mReturnValue;

  public MockEvenTrendDbAdapter() {
    mCursor = new MockCursor();
  }

  public MockEvenTrendDbAdapter(MockContext context) {
    mCursor = new MockCursor();
  }

  public void setColumnMap(HashMap<Integer, String> columnMap) {
    mCursor.setColumnMap(columnMap);
  }

  public void setQueryResults(ArrayList<HashMap<String, String>> contents) {
    mCursor.setQueryResults(contents);
  }

  public void setQueryReturn(long l) {
    mReturnValue = l;
  }

  public void setQueryReturn(int i) {
    mReturnValue = i;
  }

  public void setQueryReturn(boolean b) {
    mReturnValue = (b == true ? 1 : 0);
  }

  public void setCursor(MockCursor cursor) {
    mCursor = cursor;
  }

  public MockCursor getCursor() {
    return mCursor;
  }

  public void close() {
    mCursor.close();
    mCursor = null;
  }

  public long createCategory(CategoryDbTable.Row category) {
    return mReturnValue;
  }

  public long createEntry(EntryDbTable.Row entry) {
    return mReturnValue;
  }

  public void createFormulaCacheItem(Item item) {
    return;
  }

  public boolean deleteAllCategories() {
    return (mReturnValue == 1 ? true : false);
  }

  public boolean deleteAllEntries() {
    return (mReturnValue == 1 ? true : false);
  }

  public boolean deleteAllFormulaCacheEntries() {
    return (mReturnValue == 1 ? true : false);
  }

  public boolean deleteCategory(long rowId) {
    return (mReturnValue == 1 ? true : false);
  }

  public boolean deleteCategoryEntries(long catId) {
    return (mReturnValue == 1 ? true : false);
  }

  public boolean deleteEntry(long rowId) {
    return (mReturnValue == 1 ? true : false);
  }

  public boolean deleteFormula(long catId) {
    return (mReturnValue == 1 ? true : false);
  }

  public boolean deleteFormulaDependent(long rowId) {
    return (mReturnValue == 1 ? true : false);
  }

  public Cursor fetchAllCategories() {
    return mCursor;
  }

  public Cursor fetchAllEntries() {
    return mCursor;
  }

  public Cursor fetchAllFormulaCacheEntries() {
    return mCursor;
  }

  public Cursor fetchAllGroups() {
    return mCursor;
  }

  public Cursor fetchAllSynthetics() {
    return mCursor;
  }

  public CategoryDbTable.Row fetchCategory(long rowId) {
    // TODO Auto-generated method stub
    return null;
  }

  public CategoryDbTable.Row fetchCategory(String category) {
    // TODO Auto-generated method stub
    return null;
  }

  public Cursor fetchCategoryCursor(long rowId) {
    return mCursor;
  }

  public long[] fetchCategoryDependees(long catId) {
    // TODO Auto-generated method stub
    return null;
  }

  public long[] fetchCategoryDependents(long catId) {
    // TODO Auto-generated method stub
    return null;
  }

  public Cursor fetchCategoryEntries(long catId) {
    return mCursor;
  }

  public Cursor fetchCategoryEntriesRange(long catId, long milliStart,
      long milliEnd) {
    return mCursor;
  }

  public EntryDbTable.Row fetchCategoryEntryInPeriod(long catId, long period,
      long date_ms) {
    // TODO Auto-generated method stub
    return null;
  }

  public long fetchCategoryId(String category) {
    return mReturnValue;
  }

  public int fetchCategoryMaxRank() {
    return (int) mReturnValue;
  }

  public Cursor fetchEntriesRange(long milliStart, long milliEnd) {
    return mCursor;
  }

  public EntryDbTable.Row fetchEntry(long rowId) {
    // TODO Auto-generated method stub
    return null;
  }

  public Item fetchFormulaCacheItem(long catId) {
    // TODO Auto-generated method stub
    return null;
  }

  public long[] fetchFormulaDependents(long formulaId) {
    // TODO Auto-generated method stub
    return null;
  }

  public EntryDbTable.Row fetchLastCategoryEntry(long catId) {
    // TODO Auto-generated method stub
    return null;
  }

  public Cursor fetchRecentCategoryEntries(long catId, int items) {
    return mCursor;
  }

  public Cursor fetchRecentEntries(int items, int skip) {
    return mCursor;
  }

  public Cursor fetchRecentEntries(int items, long catId, int skip) {
    return mCursor;
  }

  public String flattenDB() {
    // TODO Auto-generated method stub
    return null;
  }

  public EvenTrendDbAdapter open() throws SQLException {
    return this;
  }

  public boolean updateCategory(CategoryDbTable.Row category) {
    return (mReturnValue == 1 ? true : false);
  }

  public boolean updateCategory(long id, ContentValues args) {
    return (mReturnValue == 1 ? true : false);
  }

  public boolean updateCategoryLastValue(long rowId, float value) {
    return (mReturnValue == 1 ? true : false);
  }

  public boolean updateCategoryPeriodEntries(long rowId, int items) {
    return (mReturnValue == 1 ? true : false);
  }

  public boolean updateCategoryRank(long rowId, int rank) {
    return (mReturnValue == 1 ? true : false);
  }

  public boolean updateCategoryTrend(long catId, String trendStr, float newTrend) {
    return (mReturnValue == 1 ? true : false);
  }

  public boolean updateEntry(EntryDbTable.Row entry) {
    return (mReturnValue == 1 ? true : false);
  }
}
