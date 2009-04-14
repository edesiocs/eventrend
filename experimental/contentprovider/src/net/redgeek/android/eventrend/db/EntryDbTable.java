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
//package net.redgeek.android.eventrend.db;
//
//import android.database.Cursor;
//
///**
// * Class encapsulating the database table definition, exportable contents,
// * acceptable values, and convenience routines for interacting with the DB
// * table.
// * 
// * @author barclay
// * 
// */
//public class EntryDbTable {
//  public static final String TABLE_NAME = "entries";
//
//  public static final String KEY_ROWID = "_id";
//  public static final String KEY_CATEGORY_ID = "category_id";
//  public static final String KEY_TIMESTAMP = "timestamp";
//  public static final String KEY_VALUE = "value";
//  public static final String KEY_N_ENTRIES = "n_entries";
//
//  public static final String KEY_STAR = TABLE_NAME + ".*";
//  public static final String[] KEY_ALL = { KEY_ROWID, KEY_CATEGORY_ID,
//      KEY_TIMESTAMP, KEY_VALUE, KEY_N_ENTRIES };
//
//  public static final String[] EXPORTABLE = { KEY_TIMESTAMP, KEY_VALUE,
//      KEY_N_ENTRIES };
//
//  public static final int EDIT_LIMIT = 20;
//
//  public static final String TABLE_CREATE = "create table " + TABLE_NAME + " ("
//      + KEY_ROWID + " integer primary key autoincrement, " + KEY_CATEGORY_ID
//      + " integer key not null, " + KEY_TIMESTAMP + " long key not null, "
//      + KEY_VALUE + " float not null, " + KEY_N_ENTRIES + " integer not null);";
//
//  public static long getId(Cursor c) {
//    return c.getLong(c.getColumnIndexOrThrow(KEY_ROWID));
//  }
//
//  public static long getCategoryId(Cursor c) {
//    return c.getLong(c.getColumnIndexOrThrow(KEY_CATEGORY_ID));
//  }
//
//  public static long getTimestamp(Cursor c) {
//    return c.getLong(c.getColumnIndexOrThrow(KEY_TIMESTAMP));
//  }
//
//  public static float getValue(Cursor c) {
//    return c.getFloat(c.getColumnIndexOrThrow(KEY_VALUE));
//  }
//
//  public static int getNEntries(Cursor c) {
//    return c.getInt(c.getColumnIndexOrThrow(KEY_N_ENTRIES));
//  }
//
//  public static class Row {
//    private long mId = 0;
//    private long mCategoryId = 0;
//    private long mTimestamp = 0;
//    private float mValue = 0;
//    private int mNEntries = 1;
//
//    public Row() {
//    }
//
//    public Row(Row r) {
//      set(r);
//    }
//
//    public void set(Row r) {
//      setId(r.getId());
//      setCategoryId(r.getCategoryId());
//      setTimestamp(r.getTimestamp());
//      setValue(r.getValue());
//      setNEntries(r.getNEntries());
//    }
//
//    public void populateFromCursor(Cursor c) {
//      if (c == null)
//        return;
//
//      setId(c.getLong(c.getColumnIndexOrThrow(KEY_ROWID)));
//      setCategoryId(c.getLong(c.getColumnIndexOrThrow(KEY_CATEGORY_ID)));
//      setTimestamp(c.getLong(c.getColumnIndexOrThrow(KEY_TIMESTAMP)));
//      setValue(c.getFloat(c.getColumnIndexOrThrow(KEY_VALUE)));
//      setNEntries(c.getInt(c.getColumnIndexOrThrow(KEY_N_ENTRIES)));
//
//      return;
//    }
//
//    public long getId() {
//      return mId;
//    }
//
//    public void setId(long id) {
//      mId = id;
//    }
//
//    public long getCategoryId() {
//      return mCategoryId;
//    }
//
//    public void setCategoryId(long categoryId) {
//      mCategoryId = categoryId;
//    }
//
//    public float getValue() {
//      return mValue;
//    }
//
//    public void setValue(float value) {
//      mValue = value;
//    }
//
//    public int getNEntries() {
//      return mNEntries;
//    }
//
//    public void setNEntries(int nEntries) {
//      mNEntries = nEntries;
//    }
//
//    public long getTimestamp() {
//      return mTimestamp;
//    }
//
//    public void setTimestamp(long timestamp) {
//      mTimestamp = timestamp;
//    }
//  }
//}
