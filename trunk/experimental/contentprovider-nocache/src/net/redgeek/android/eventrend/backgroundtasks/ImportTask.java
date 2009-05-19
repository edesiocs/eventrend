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

package net.redgeek.android.eventrend.backgroundtasks;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import net.redgeek.android.eventrecorder.TimeSeriesData;
import net.redgeek.android.eventrecorder.TimeSeriesProvider;
import net.redgeek.android.eventrecorder.TimeSeriesData.Datapoint;
import net.redgeek.android.eventrecorder.TimeSeriesData.DateMap;
import net.redgeek.android.eventrecorder.TimeSeriesData.TimeSeries;
import net.redgeek.android.eventrend.category.CategoryRow;
import net.redgeek.android.eventrend.datum.EntryRow;
import net.redgeek.android.eventrend.importing.CSV;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Import a CSV file into the database, replacing existing data. (Merge import
 * is planned but not yet implemented.) Categories are created on demand, as the
 * export format currently flattens the database into one file, meaning that
 * most of the category fields are duplicated across every datapoint row.
 * Furthermore, this means that if an entry exists without any associated
 * datapoints, it will not exist in the output file, and thus will not be
 * created when importing.
 * 
 * <p>
 * Note that since this is "backgroundtask", no UI operations may be performed.
 * 
 * @author barclay
 * 
 */
public class ImportTask {
  private ContentResolver mResolver = null;
  private String mFilename = null;
  private HashMap <String,Long> mCatMap;

  // This is just a giant hack ... but I just kind of want to get the I/O
  // done with:
  private static HashMap<String, String> sDb1To2mapping;
  static {
    sDb1To2mapping = new HashMap<String, String>();
    sDb1To2mapping.put("category_name", TimeSeries.TIMESERIES_NAME);
    sDb1To2mapping.put("group_name", TimeSeries.GROUP_NAME);
    sDb1To2mapping.put("default_value", TimeSeries.DEFAULT_VALUE);
    // last_trend is ignored, it was a cached value
    sDb1To2mapping.put("increment", TimeSeries.INCREMENT);
    sDb1To2mapping.put("goal", TimeSeries.GOAL);
    sDb1To2mapping.put("color", TimeSeries.COLOR);
    sDb1To2mapping.put("type", TimeSeries.AGGREGATION); // .toLowerCase()
    sDb1To2mapping.put("period_in_ms", TimeSeries.PERIOD); // / 1000
    sDb1To2mapping.put("rank", TimeSeries.RANK);
    sDb1To2mapping.put("interpolation", TimeSeries.INTERPOLATION);
    sDb1To2mapping.put("zerofill", TimeSeries.ZEROFILL);
    sDb1To2mapping.put("synthetic", TimeSeries.TYPE); // -> string
    sDb1To2mapping.put("formula", TimeSeries.FORMULA);
    sDb1To2mapping.put("timestamp", Datapoint.TS_START);
    sDb1To2mapping.put("value", Datapoint.VALUE);
    sDb1To2mapping.put("n_entries", Datapoint.ENTRIES);
  }
    
  // Intended for future discrete progress bar indicator:
  public int mNumRecords;
  public int mNumRecordsDone;

  public ImportTask() {
    mCatMap = new HashMap <String,Long>();
  }

  public ImportTask(ContentResolver resolver) {
    mResolver = resolver;
    mCatMap = new HashMap <String,Long>();
  }

  public ImportTask(ContentResolver resolver, String filename) {
    mResolver = resolver;
    mFilename = filename;
    mCatMap = new HashMap <String,Long>();
  }

  public void setFilename(String filename) {
    mFilename = filename;
  }

  public void doImport() throws IOException {
    if (mResolver == null || mFilename == null)
      return;

    String line;
    ArrayList<String> columns = null;
    ArrayList<String> data = null;
    int dbFormat = 1; // super-ugly horrible hack
    
    // delete the existing timeseries
    Uri allSeries = TimeSeries.CONTENT_URI;
    Cursor c = mResolver.query(allSeries, null, null, null, null);
    if (c.moveToFirst()) {
      int tsCount = c.getCount();
      for (int i = 0; i < tsCount; i++) {
        long id = TimeSeries.getId(c);
        Uri ts = ContentUris.withAppendedId(TimeSeries.CONTENT_URI, id);
        mResolver.delete(ts, null, null);
        c.moveToNext();
      }
    }
    if (c != null)
      c.close();

    mCatMap.clear();

    // Now read in the new ones:
    BufferedReader input = null;
    FileInputStream f = new FileInputStream(mFilename);
    input = new BufferedReader(new InputStreamReader(new DataInputStream(f)));
    
    int i = 0;
    while ((line = input.readLine()) != null) {
      if (i == 0) {
        columns = CSV.parseHeader(line);
        if (columns.contains(TimeSeries.RECORDING_DATAPOINT_ID)) {
          dbFormat = 2;
        }
      } else {
        data = CSV.getNextLine(line);
        if (dbFormat == 1) {
          insertEntryV1(data, columns);
        } else {
          insertEntryV2(data, columns);          
        }
      }
      i++;
    }
    input.close();

    return;
  }

  private void insertEntryV2(ArrayList<String> data, ArrayList<String> columns) {
    ContentValues newSeries = new ContentValues();
    ContentValues newEntry = new ContentValues();
    int i = 0;
    int tsCols = TimeSeries.EXPORTABLE_COLS.length;
    String[] catname = new String[1];
    Uri tsByName = TimeSeries.CONTENT_URI;
    Uri datapoint;
    String datum, column;

    Long catId = new Long(0);
    for (i = 0; i < data.size() && i < columns.size(); i++) {
      datum = data.get(i);
      column = columns.get(i);

      if (column.equals(TimeSeries.TIMESERIES_NAME)
          || column.equals(TimeSeries.GROUP_NAME)
          || column.equals(TimeSeries.COLOR)
          || column.equals(TimeSeries.UNITS)
          || column.equals(TimeSeries.AGGREGATION)
          || column.equals(TimeSeries.TYPE)
          || column.equals(TimeSeries.FORMULA)
          || column.equals(TimeSeries.INTERPOLATION)) {
        if (column.equals(TimeSeries.TIMESERIES_NAME)) {
          catname[0] = datum;
        }
        newSeries.put(column, datum);
      }
      else if (column.equals(TimeSeries.RECORDING_DATAPOINT_ID)) {
        newSeries.put(column, Long.valueOf(datum));
      }
      else if (column.equals(TimeSeries.DEFAULT_VALUE)
          || column.equals(TimeSeries.INCREMENT)
          || column.equals(TimeSeries.GOAL)
          || column.equals(TimeSeries.SENSITIVITY)
          || column.equals(TimeSeries.SMOOTHING)) {
        newSeries.put(column, Double.valueOf(datum));
      }
      else if (column.equals(TimeSeries.PERIOD)
          || column.equals(TimeSeries.RANK)
          || column.equals(TimeSeries.ZEROFILL)
          || column.equals(TimeSeries.UNITS)
          || column.equals(TimeSeries.HISTORY)
          || column.equals(TimeSeries.DECIMALS)) {
        newSeries.put(column, Integer.valueOf(datum));
      }
      else if (column.equals(Datapoint.VALUE)) {
        newEntry.put(column, Double.valueOf(datum));
      }
      else if (column.equals(Datapoint.ENTRIES)
          || column.equals(Datapoint.TS_START)
          || column.equals(Datapoint.TS_END)) {
        newEntry.put(column, Integer.valueOf(datum));
      }
    }

    catId = mCatMap.get(catname[0]);
    if (catId == null) {
      Cursor c = mResolver.query(tsByName, null, 
          TimeSeries.TIMESERIES_NAME + " = ? ", catname, null);
      if (c.moveToFirst() && c.getCount() > 0) {
        catId = TimeSeries.getId(c);
        mCatMap.put(catname[0], catId);
      }
      if (c != null)
        c.close();

      if (catId <= 0) {
        Uri uri = mResolver.insert(tsByName, newSeries);
        if (uri != null) {
          String rowIdStr = uri.getPathSegments().get(TimeSeriesProvider.PATH_SEGMENT_TIMESERIES_ID);
          catId = Long.valueOf(rowIdStr);
          mCatMap.put(catname[0], catId);
        }
      }
    }
    
    if (catId > 0) {
      newEntry.put(Datapoint.TIMESERIES_ID, catId);
      datapoint = ContentUris.withAppendedId(TimeSeries.CONTENT_URI, catId)
          .buildUpon().appendPath("datapoints").build();
      Uri uri = mResolver.insert(datapoint, newEntry);
    }

    return;
  }
  
  private void insertEntryV1(ArrayList<String> data, ArrayList<String> columns) {
    ContentValues newSeries = new ContentValues();
    ContentValues newEntry = new ContentValues();
    EntryRow entry = new EntryRow();
    int i = 0;
    int tsCols = TimeSeries.EXPORTABLE_COLS.length;
    String datum, column, columnMapping;
    String[] catname = new String[1];
    Uri tsByName = TimeSeries.CONTENT_URI;
    Uri datapoint;

    Long catId = new Long(0);
    for (i = 0; i < data.size() && i < columns.size(); i++) {
      datum = data.get(i);
      column = columns.get(i);
      columnMapping = sDb1To2mapping.get(column);
      
      if (TextUtils.isEmpty(columnMapping) == true) {
        continue;
      }
      else if (columnMapping.equals(TimeSeries.AGGREGATION)) {
        newSeries.put(columnMapping, datum.toLowerCase());
      }
      else if (columnMapping.equals(TimeSeries.TYPE)) {
        if (Integer.valueOf(datum) != 0) {
          newSeries.put(columnMapping, TimeSeries.TYPE_SYNTHETIC);          
        } else {
          newSeries.put(columnMapping, TimeSeries.TYPE_DISCRETE);
        }
      }
      else if (columnMapping.equals(TimeSeries.PERIOD)) {
        newSeries.put(columnMapping, Integer.valueOf(datum) / DateMap.SECOND_MS);
      }
      else if (columnMapping.equals(TimeSeries.TIMESERIES_NAME)
          || columnMapping.equals(TimeSeries.GROUP_NAME)
          || columnMapping.equals(TimeSeries.COLOR)
          || columnMapping.equals(TimeSeries.FORMULA)
          || columnMapping.equals(TimeSeries.INTERPOLATION)) {
        if (column.equals(TimeSeries.TIMESERIES_NAME)) {
          catname[0] = datum;
        }
        newSeries.put(columnMapping, datum);
      }
      else if (columnMapping.equals(TimeSeries.DEFAULT_VALUE)
          || columnMapping.equals(TimeSeries.INCREMENT)
          || columnMapping.equals(TimeSeries.GOAL)) {
        newSeries.put(columnMapping, Double.valueOf(datum));
      }
      else if (columnMapping.equals(TimeSeries.RANK)
          || columnMapping.equals(TimeSeries.ZEROFILL)) {
        newSeries.put(columnMapping, Integer.valueOf(datum));
      }
      else if (columnMapping.equals(Datapoint.VALUE)) {
        newEntry.put(columnMapping, Double.valueOf(datum));
      }
      else if (columnMapping.equals(Datapoint.ENTRIES)) {
        newEntry.put(columnMapping, Integer.valueOf(datum));
      }      
      else if (columnMapping.equals(Datapoint.TS_START)) {
        newEntry.put(columnMapping, Integer.valueOf(datum));
        newEntry.put(Datapoint.TS_END, Integer.valueOf(datum));
      }      
    }
    
    newSeries.put(TimeSeries.RECORDING_DATAPOINT_ID, 0);
    newSeries.put(TimeSeries.UNITS, "");
    newSeries.put(TimeSeries.SENSITIVITY, 0.5);
    newSeries.put(TimeSeries.SMOOTHING, 0.1);
    newSeries.put(TimeSeries.HISTORY, 20);
    newSeries.put(TimeSeries.DECIMALS, 2);

    catId = mCatMap.get(catname[0]);
    if (catId == null) {
      Cursor c = mResolver.query(tsByName, null, 
          TimeSeries.TIMESERIES_NAME + " = ? ", catname, null);
      if (c.moveToFirst() && c.getCount() > 0) {
        catId = TimeSeries.getId(c);
        mCatMap.put(catname[0], catId);
      }
      if (c != null)
        c.close();

      if (catId <= 0) {
        Uri uri = mResolver.insert(tsByName, newSeries);
        if (uri != null) {
          String rowIdStr = uri.getPathSegments().get(TimeSeriesProvider.PATH_SEGMENT_TIMESERIES_ID);
          catId = Long.valueOf(rowIdStr);
          mCatMap.put(catname[0], catId);
        }
      }
    }
    
    if (catId > 0) {
      newEntry.put(Datapoint.TIMESERIES_ID, catId);
      datapoint = ContentUris.withAppendedId(TimeSeries.CONTENT_URI, catId)
          .buildUpon().appendPath("datapoints").build();
      Uri uri = mResolver.insert(datapoint, newEntry);
    }

    return;
  }

}