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

import net.redgeek.android.eventrecorder.TimeSeriesData;
import android.database.Cursor;

/**
 * Class encapsulating the database table definition, exportable contents,
 * acceptable values, and convenience routines for interacting with the DB
 * table.
 * 
 * @author barclay
 * 
 */
public class DatapointDbTable {
  public static final int EDIT_LIMIT = 20;

  public static long getId(Cursor c) {
    return c.getLong(c.getColumnIndexOrThrow("_id"));
  }

  public static long getCategoryId(Cursor c) {
    return c.getLong(c.getColumnIndexOrThrow(TimeSeriesData.DatapointRow.TIMESERIES_ID));
  }

  public static long getTsStart(Cursor c) {
    return c.getLong(c.getColumnIndexOrThrow(TimeSeriesData.DatapointRow.TS_START));
  }

  public static long getTsEnd(Cursor c) {
    return c.getLong(c.getColumnIndexOrThrow(TimeSeriesData.DatapointRow.TS_END));
  }

  public static float getValue(Cursor c) {
    return c.getFloat(c.getColumnIndexOrThrow(TimeSeriesData.DatapointRow.VALUE));
  }

  public static int getUpdates(Cursor c) {
    return c.getInt(c.getColumnIndexOrThrow(TimeSeriesData.DatapointRow.UPDATES));
  }

  public static class Row {
    private long mId = 0;
    private long mTimeSeriesId = 0;
    private long mTsStart = 0;
    private long mTsEnd = 0;
    private float mValue = 0;
    private int mUpdates = 0;

    public Row() {
    }

    public Row(Row r) {
      set(r);
    }

    public void set(Row r) {
      mId = r.mId;
      mTimeSeriesId = r.mTimeSeriesId;
      mTsStart = r.mTsStart;
      mTsEnd = r.mTsEnd;
      mValue = r.mValue;
      mUpdates = r.mUpdates;
    }

    public void populateFromCursor(Cursor c) {
      if (c == null)
        return;

      mId = c.getLong(c.getColumnIndexOrThrow("_id"));
      mTimeSeriesId = c.getLong(c.getColumnIndexOrThrow(TimeSeriesData.DatapointRow.TIMESERIES_ID));
      mTsStart = c.getLong(c.getColumnIndexOrThrow(TimeSeriesData.DatapointRow.TS_START));
      mTsEnd = c.getLong(c.getColumnIndexOrThrow(TimeSeriesData.DatapointRow.TS_START));
      mValue = c.getFloat(c.getColumnIndexOrThrow(TimeSeriesData.DatapointRow.VALUE));
      mUpdates = c.getInt(c.getColumnIndexOrThrow(TimeSeriesData.DatapointRow.UPDATES));

      return;
    }

    public long getId() {
      return mId;
    }

    public void setId(long id) {
      mId = id;
    }

    public long getTimeSeriesId() {
      return mTimeSeriesId;
    }

    public void setTimeSeriesId(long id) {
      mTimeSeriesId = id;
    }

    public float getValue() {
      return mValue;
    }

    public void setValue(float value) {
      mValue = value;
    }

    public long getTsStart() {
      return mTsStart;
    }

    public void setTsStart(long timestamp) {
      mTsStart = timestamp;
    }

    public long getTsEnd() {
      return mTsEnd;
    }

    public void setTsEnd(long timestamp) {
      mTsEnd = timestamp;
    }

    public int getUpdates() {
      return mUpdates;
    }

    public void setUpdates(int updates) {
      mUpdates = updates;
    }
  }
}
