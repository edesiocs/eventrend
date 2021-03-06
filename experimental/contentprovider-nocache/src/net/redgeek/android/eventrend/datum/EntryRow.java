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

package net.redgeek.android.eventrend.datum;

import android.database.Cursor;

import net.redgeek.android.eventrecorder.TimeSeriesData.Datapoint;

public class EntryRow implements Comparable<EntryRow> {
  public long mId;
  public long mTimeSeriesId;
  public double mValue;
  public int mEntries;
  public int mTsStart;
  public int mTsEnd;

  private boolean mSelectable = true;

  public EntryRow() {
  }
  
  public void populateFromCursor(Cursor c) {
    mId = Datapoint.getId(c);
    mTimeSeriesId = Datapoint.getTimeSeriesId(c);
    mValue = Datapoint.getValue(c);
    mEntries = Datapoint.getEntries(c);
    mTsStart = Datapoint.getTsStart(c);
    mTsEnd = Datapoint.getTsEnd(c);
  }

  public boolean isSelectable() {
    return mSelectable;
  }

  public void setSelectable(boolean selectable) {
    mSelectable = selectable;
  }

  public int compareTo(EntryRow other) {
    if (this.mTsStart < other.mTsStart)
      return -1;
    else if (this.mTsStart > other.mTsStart)
      return 1;
    return 0;
  }
}