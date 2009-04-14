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
//import java.util.ArrayList;
//import java.util.Iterator;
//import java.util.SortedMap;
//import java.util.TreeMap;
//
//
//public class TimeSeriesCache {
//  private long mSeriesId;
//  private boolean mValid;
//  private TreeMap<Long, Datapoint> mCache;
//  private long mStart;
//  private long mEnd;
//
//  public TimeSeriesCache(long seriesId) {
//    mSeriesId = seriesId;
//    mValid = false;
//    mCache = new TreeMap<Long, Datapoint>();
//    resetRangeMarkers();
//  }
//
//  public void clear() {
//    mCache.clear();
//    resetRangeMarkers();
//  }
//
//  public boolean isValid() {
//    return mValid;
//  }
//
//  public long getSeriesId() {
//    return mSeriesId;
//  }
//
//  public long getStart() {
//    return mStart;
//  }
//
//  public void updateStart(long start) {
//    if (start < mStart)
//      mStart = start;
//  }
//
//  public long getEnd() {
//    return mEnd;
//  }
//
//  public void updateEnd(long end) {
//    if (end > mEnd)
//      mEnd = end;
//  }
//
//  public Datapoint addDatapoint(Datapoint d) {
//    if (d.mMillis < mStart)
//      mStart = d.mMillis;
//    if (d.mMillis > mEnd)
//      mEnd = d.mMillis;
//    mValid = true;
//    return mCache.put(d.mMillis, d);
//  }
//
//  public Datapoint updateDatapoint(Datapoint d) {
//    if (mCache.get(d.mMillis) == null)
//      return null;
//    return mCache.put(d.mMillis, d);
//  }
//  
//  public void deleteDatapoint(Long id) {
//    if (mCache.get(id) == null)
//      return;
//    mCache.remove(id);    
//  }
//
//  public ArrayList<Datapoint> getDataInRange(long msStart, long msEnd) {
//    ArrayList<Datapoint> range = new ArrayList<Datapoint>();
//    SortedMap<Long, Datapoint> map;
//
//    if (msStart > msEnd)
//      return range;
//    
//    try {
//      map = mCache.subMap(Long.valueOf(msStart), Long.valueOf(msEnd + 1));
//    } catch (NullPointerException e) {
//      return range;
//    }
//
//    Iterator<Datapoint> iterator = map.values().iterator();
//    while (iterator.hasNext()) {
//      Datapoint d = iterator.next();
//      if (d != null)
//        range.add(d);
//    }
//
//    return range;
//  }
//
//  public ArrayList<Datapoint> getDataBefore(int number, long ms) {
//    ArrayList<Datapoint> pre = new ArrayList<Datapoint>();
//    SortedMap<Long, Datapoint> range;
//    SortedMap<Long, Datapoint> reverse;
//    
//    try {
//      range = mCache.headMap(Long.valueOf(ms));
//    } catch (NullPointerException e) {
//      return pre;
//    } catch (IllegalArgumentException e) {
//      return pre;
//    }
//    
//    reverse = new TreeMap<Long, Datapoint>(java.util.Collections.reverseOrder());
//    reverse.putAll(range);
//
//    Iterator<Datapoint> iterator = reverse.values().iterator();
//    for (int i = 0; i < number && iterator.hasNext();) {
//      Datapoint d = iterator.next();
//      if (d != null) {
//        i++;
//        pre.add(0, d);
//      }
//    }
//
//    return pre;
//  }
//
//  public ArrayList<Datapoint> getDataAfter(int number, long ms) {
//    ArrayList<Datapoint> post = new ArrayList<Datapoint>();
//    SortedMap<Long, Datapoint> range;
//
//    try {
//      range = mCache.tailMap(Long.valueOf(ms) + 1);
//    } catch (NullPointerException e) {
//      return post;
//    } catch (IllegalArgumentException e) {
//      return post;
//    }
//
//    Iterator<Datapoint> iterator = range.values().iterator();
//    for (int i = 0; i < number && iterator.hasNext();) {
//      Datapoint d = iterator.next();
//      if (d != null) {
//        i++;
//        post.add(d);
//      }
//    }
//
//    return post;
//  }
//
//  public ArrayList<Datapoint> getLast(int number) {
//    ArrayList<Datapoint> last = new ArrayList<Datapoint>();
//    SortedMap<Long, Datapoint> reverse = new TreeMap<Long, Datapoint>(
//        java.util.Collections.reverseOrder());
//    reverse.putAll(mCache);
//
//    Iterator<Datapoint> iterator = reverse.values().iterator();
//    for (int i = 0; i < number && iterator.hasNext();) {
//      Datapoint d = iterator.next();
//      if (d != null) {
//        i++;
//        last.add(0, d);
//      }
//    }
//
//    return last;
//  }
//
//  private void resetRangeMarkers() {
//    mValid = false;
//    mStart = Long.MAX_VALUE;
//    mEnd = Long.MIN_VALUE;
//  }
//}
