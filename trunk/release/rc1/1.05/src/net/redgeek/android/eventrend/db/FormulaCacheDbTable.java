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

package net.redgeek.android.eventrend.db;

import java.util.ArrayList;

import android.database.Cursor;

/**
 * Class encapsulating the database table definition, exportable contents,
 * acceptable values, and convenience routines for interacting with the DB
 * table.
 * 
 * @author barclay
 */
public class FormulaCacheDbTable {
  public static final String TABLE_NAME = "formulas";

  public static final String KEY_ROWID = "_id";
  public static final String KEY_CATEGORY_ID = "category_id";
  public static final String KEY_DEPENDENT_ID = "dependent_id";

  public static final String KEY_STAR = TABLE_NAME + ".*";
  public static final String[] KEY_ALL = { KEY_ROWID, KEY_CATEGORY_ID,
      KEY_DEPENDENT_ID, };

  public static final String[] EXPORTABLE = {};

  public static final String TABLE_CREATE = "create table " + TABLE_NAME + " ("
      + KEY_ROWID + " integer primary key autoincrement, " + KEY_CATEGORY_ID
      + " integer key not null, " + KEY_DEPENDENT_ID
      + " integer key not null);";

  public static long getId(Cursor c) {
    return c.getLong(c.getColumnIndexOrThrow(KEY_ROWID));
  }

  public static long getCategoryId(Cursor c) {
    return c.getLong(c.getColumnIndexOrThrow(KEY_CATEGORY_ID));
  }

  public static long getDependentId(Cursor c) {
    return c.getLong(c.getColumnIndexOrThrow(KEY_DEPENDENT_ID));
  }

  public static class Item {
    private long mCategoryId = 0;
    private ArrayList<Long> mDependentIds = null;

    public Item() {
      mDependentIds = new ArrayList<Long>();
    }

    public Item(Item item) {
      mDependentIds = new ArrayList<Long>();
      copyDepedents(item);
      setCategoryId(item.getCategoryId());
    }

    public long getCategoryId() {
      return mCategoryId;
    }

    public void setCategoryId(long categoryId) {
      mCategoryId = categoryId;
    }

    public long[] getDependentIds() {
      long[] ids = new long[mDependentIds.size()];
      for (int i = 0; i < mDependentIds.size(); i++) {
        ids[i] = mDependentIds.get(i).longValue();
      }
      return ids;
    }

    public void setDependentIds(long[] dependentIds) {
      mDependentIds.clear();
      for (int i = 0; i < dependentIds.length; i++) {
        mDependentIds.add(new Long(dependentIds[i]));
      }
      return;
    }

    public void addDependentId(long id) {
      if (mDependentIds.contains(Long.valueOf(id)))
        return;
      mDependentIds.add(new Long(id));
      return;
    }

    private void copyDepedents(Item item) {
      if (mDependentIds != null)
        mDependentIds.clear();

      if (item.mDependentIds != null) {
        for (int i = 0; i < item.mDependentIds.size(); i++) {
          mDependentIds.add(item.mDependentIds.get(i));
        }
      }
    }
  }
}
