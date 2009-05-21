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
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import net.redgeek.android.eventrecorder.TimeSeriesData.Datapoint;
import net.redgeek.android.eventrecorder.TimeSeriesData.TimeSeries;
import net.redgeek.android.eventrend.category.CategoryRow;
import net.redgeek.android.eventrend.datum.EntryRow;
import net.redgeek.android.eventrend.importing.CSV;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;

/**
 * Exports the user's database to a file or mail. Files are currently written to
 * a predefined directory on the sdcard with a predetermined (timestamp)
 * filename. Note that the actual mailing of the data is not done internally, but
 * shelled out to any other program that has it's intent filters set
 * appropriately.
 * 
 * <p>
 * Note that since this is "backgroundtask", no UI operations may be performed.
 * 
 * @author barclay
 */
public class ExportTask {
  private ContentResolver mResolver;
  private Handler mHandler;
  private int mNumRecords;
  private int mNumRecordsDone;
  
  public String mDirectory;
  public String mFilename;
  public String mSubject = "";
  public String mBody = "";
  public boolean mToFile = true;

  public ExportTask() {
  }

  public ExportTask(ContentResolver resolver, Handler handler) {
    mResolver = resolver;
    mHandler = handler;
  }

  public ExportTask(ContentResolver resolver, String filename, Handler handler) {
    mResolver = resolver;
    mFilename = filename;
    mHandler = handler;
  }

  public void setFilename(String filename) {
    mFilename = filename;
  }

  public void setDirectory(String directory) {
    mDirectory = directory;
  }

  public void setSubject(String subject) {
    mSubject = subject;
  }

  public void setToFile(boolean toFile) {
    mToFile = toFile;
  }

  public void doExport() throws IOException {
    if (mToFile) {
      exportToFile();
    } else {
      exportToMail();
    }
  }

  private void exportToMail() {
    mBody = flattenDb();
  }

  private void exportToFile() throws IOException {
    mBody = flattenDb();

    File f = new File(mDirectory);
    f.mkdirs();
    f = new File(mFilename);
    Writer output = null;
    f.createNewFile();
    output = new BufferedWriter(new FileWriter(f));
    output.write(mBody);
    output.close();
  }
  
  private String flattenDb() {
    StringBuffer output = new StringBuffer();
    String[] line = new String[2];
    CategoryRow cat = new CategoryRow();
    EntryRow ent = new EntryRow();
    int list = 0;
    HashMap<Long, String> catMap = new HashMap<Long, String>();
    
    output.append(CSV.createCSVHeader());
    
    Uri allSeries = TimeSeries.CONTENT_URI;
    Cursor tsCur = mResolver.query(allSeries, null, null, null, null);
    if (tsCur.moveToFirst()) {
      int tsCount = tsCur.getCount();
      for (int i = 0; i < tsCount; i++) {
        CategoryRow row = new CategoryRow(tsCur);
        String rowCSV =  CSV.joinCSV(cat);
        catMap.put(cat.mId, rowCSV);
        tsCur.moveToNext();
      }
    }
    if (tsCur != null)
      tsCur.close();

    Uri datapoints = Datapoint.CONTENT_URI;
    Cursor dpCur = mResolver.query(datapoints, null, null, null, null);
    if (dpCur.moveToFirst()) {
      int dpCount = dpCur.getCount();
      mNumRecords = dpCount;
      for (int j = 0; j < dpCount; j++) {
        ent.populateFromCursor(dpCur);
        line[0] = catMap.get(ent.mTimeSeriesId);
        line[1] = CSV.joinCSV(ent);
        output.append(CSV.joinCSVTerminated(line));
        mNumRecordsDone = j; 
        updateStatus();
        dpCur.moveToNext();
      }
    }
    if (dpCur != null)
      dpCur.close();
    tsCur.moveToNext();

    return output.toString();
  }
  
  private void updateStatus() {
    Message msg = mHandler.obtainMessage();
    Bundle b = new Bundle();
    b.putInt("done", mNumRecordsDone);
    b.putInt("total", mNumRecords);
    msg.setData(b);
    mHandler.sendMessage(msg);
  }
}