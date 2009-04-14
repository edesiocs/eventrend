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
//package net.redgeek.android.eventrend.primitives;
//
//import java.util.Calendar;
//import java.util.Comparator;
//
//import net.redgeek.android.eventrend.db.EntryDbTable;
//import net.redgeek.android.eventrend.db.EntryDbTable.Row;
//
//
///**
// * The basic element used in graphing.
// * 
// * @author barclay
// * 
// */
//public final class Datapoint implements Comparator<Datapoint> {
//  public long mMillis = 0; // timestamp as long
//  public long mCatId = 0; // associated category id
//  public long mEntryId = 0; // associated entry id
//  public float mStdDev = 0.0f; // StdDev at this point in time
//  public int mNEntries = 0; // number of entries that compromises this datapoint
//  public Tuple mValue; // mValue.x = timestamp as float, mValue.y = value
//  public Tuple mValueScreen; // mValue mapped to canvas coordinates
//  public Tuple mTrend; // mTrend.x = timestamp as float, mTrend.x = trend value
//  public Tuple mTrendScreen; // mTrend mapped to canvas coordinates
//  public boolean mSynthetic = false;
//
//  public Datapoint() {
//    mValue = new Tuple(0, 0);
//    mValueScreen = new Tuple(0, 0);
//    mTrend = new Tuple(0, 0);
//    mTrendScreen = new Tuple(0, 0);
//  }
//
//  public Datapoint(long timestamp, float value, long catId, long entryId,
//      int mEntries) {
//    float x = (float) timestamp;
//    float y = value;
//
//    mValue = new Tuple(x, y);
//    mValueScreen = new Tuple(0, 0);
//    mTrend = new Tuple(0, 0);
//    mTrendScreen = new Tuple(0, 0);
//    mCatId = catId;
//    mEntryId = entryId;
//    mNEntries = mEntries;
//    mMillis = timestamp;
//    mSynthetic = false;
//  }
//
//  public Datapoint(Datapoint d) {
//    mCatId = d.mCatId;
//    mEntryId = d.mEntryId;
//    mNEntries = d.mNEntries;
//    mValue = new Tuple(d.mValue.x, d.mValue.y);
//    mValueScreen = new Tuple(d.mValueScreen.x, d.mValueScreen.y);
//    mTrend = new Tuple(d.mTrend.x, d.mTrend.y);
//    mTrendScreen = new Tuple(d.mTrendScreen.x, d.mTrendScreen.y);
//    mMillis = d.mMillis;
//    mStdDev = d.mStdDev;
//    mSynthetic = d.mSynthetic;
//  }
//
//  public Datapoint(EntryDbTable.Row row) {
//    float x = (float) row.getTimestamp();
//    float y = row.getValue();
//
//    mCatId = row.getCategoryId();
//    mEntryId = row.getId();
//    mNEntries = row.getNEntries();
//    mValue = new Tuple(x, y);
//    mValueScreen = new Tuple(0, 0);
//    mTrend = new Tuple(0, 0);
//    mTrendScreen = new Tuple(0, 0);
//    mMillis = row.getTimestamp();
//  }
//
//  @Override
//  public String toString() {
//    return String.format("(%d, %f)", mMillis, mValue.y);
//  }
//
//  public String toValueString() {
//    return String.format("(%f, %f)", mValue.x, mValue.y);
//  }
//
//  public String toCoordString() {
//    return String.format("(%f, %f)", mValueScreen.x, mValueScreen.y);
//  }
//
//  public String toLabelString() {
//    Calendar cal = Calendar.getInstance();
//    cal.setTimeInMillis(mMillis);
//    long year = cal.get(Calendar.YEAR);
//    long month = cal.get(Calendar.MONTH) + 1;
//    long day = cal.get(Calendar.DAY_OF_MONTH);
//    long hour = cal.get(Calendar.HOUR_OF_DAY);
//    long minute = cal.get(Calendar.MINUTE);
//    return String
//        .format("%d/%02d/%02d %d:%02d", year, month, day, hour, minute);
//  }
//
//  @Override
//  public boolean equals(Object obj) {
//    if (obj == null || !(obj instanceof Datapoint))
//      return false;
//    Datapoint other = (Datapoint) obj;
//    if (mMillis == other.mMillis && mCatId == other.mCatId
//        && mEntryId == other.mEntryId && mValue.equals(other.mValue)
//        && mTrend.equals(other.mTrend) && mSynthetic == other.mSynthetic) {
//      return true;
//    }
//    return false;
//  }
//
//  public boolean timestampEqual(Datapoint other) {
//    if (this.mMillis == other.mMillis)
//      return true;
//    return false;
//  }
//
//  public int compare(Datapoint d1, Datapoint d2) {
//    if (d1.mMillis < d2.mMillis)
//      return -1;
//    if (d1.mMillis > d2.mMillis)
//      return 1;
//    return 0;
//  }
//}
