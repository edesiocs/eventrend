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

package net.redgeek.android.eventrend.importing;

public class ImportRow implements Comparable<ImportRow> {
  private String mFilename;
  private String mSize;

  private boolean mSelectable = true;

  public ImportRow(String filename, String size) {
    mFilename = filename;
    mSize = size;
  }

  public boolean isSelectable() {
    return mSelectable;
  }

  public void setSelectable(boolean selectable) {
    mSelectable = selectable;
  }

  public void setFilename(String filename) {
    mFilename = filename;
  }

  public String getFilename() {
    return mFilename;
  }

  public void setSize(String size) {
    mSize = size;
  }

  public String getSize() {
    return mSize;
  }

  public int compareTo(ImportRow other) {
    return this.mFilename.compareTo(other.mFilename);
  }
}
