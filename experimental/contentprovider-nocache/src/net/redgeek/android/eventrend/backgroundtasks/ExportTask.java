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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

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

  public String mDirectory;
  public String mFilename;
  public String mSubject = "";
  public String mBody = "";
  public boolean mToFile = true;

  public ExportTask() {
  }

  public ExportTask(ContentResolver resolver) {
    mResolver = resolver;
  }

  public ExportTask(ContentResolver resolver, String filename) {
    mResolver = resolver;
    mFilename = filename;
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
    
    
    
    // TODO:
//    mBody = mDbh.flattenDB();
  }

  private void exportToFile() throws IOException {
    // TODO:
//  mBody = mDbh.flattenDB();

    File f = new File(mDirectory);
    f.mkdirs();
    f = new File(mFilename);
    Writer output = null;
    f.createNewFile();
    output = new BufferedWriter(new FileWriter(f));
    output.write(mBody);
    output.close();
  }
}