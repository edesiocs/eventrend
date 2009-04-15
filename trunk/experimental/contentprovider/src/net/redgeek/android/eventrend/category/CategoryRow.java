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

package net.redgeek.android.eventrend.category;

public class CategoryRow implements Comparable<CategoryRow> {
  public long   mId;
  public String mTimeSeriesName;
  public long   mRecordingDatapointId;
  public String mGroup;
  public float  mDefaultValue;
  public float  mIncrement;
  public float  mGoal;
  public String mColor;
  public int    mPeriod;
  public String mUnits;
  public int    mRank;
  public String mAggregation;
  public String mType;
  public int    mZerofill;
  public String mFormula;
  public String mInterpolation;

  public long   mTimestamp;
  
  private boolean mSelectable = true;

  public CategoryRow() {
    mTimestamp = 0;
  }

  public boolean isSelectable() {
    return mSelectable;
  }

  public void setSelectable(boolean selectable) {
    mSelectable = selectable;
  }

  public int compareTo(CategoryRow other) {
    if (this.mRank < other.mRank)
      return -1;
    else if (this.mRank > other.mRank)
      return 1;
    return 0;
  }
}
