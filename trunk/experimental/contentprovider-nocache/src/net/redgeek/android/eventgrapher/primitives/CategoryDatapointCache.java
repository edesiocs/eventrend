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

package net.redgeek.android.eventgrapher.primitives;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

public class CategoryDatapointCache {
  private long mCatId;
  private boolean mValid;
  private TreeMap<Integer, Datapoint> mCache;
  private int mStart;
  private int mEnd;
  private int mHistory;

  public CategoryDatapointCache(long catId, int history) {
    mCatId = catId;
    mValid = false;
    mCache = new TreeMap<Integer, Datapoint>();
    resetRangeMarkers();
    mHistory = history;
  }

  public void clear() {
    mCache.clear();
    resetRangeMarkers();
  }

  public boolean isValid() {
    return mValid;
  }

  public long getCategoryId() {
    return mCatId;
  }

  public int getStart() {
    return mStart;
  }

  public void updateStart(int start) {
    if (start < mStart)
      mStart = start;
  }

  public int getEnd() {
    return mEnd;
  }

  public void updateEnd(int end) {
    if (end > mEnd)
      mEnd = end;
  }

  public void setHistory(int history) {
    mHistory = history;
  }

  public int getHistory() {
    return mHistory;
  }

  public Datapoint addDatapoint(Datapoint d) {
    if (d.mTsStart < mStart)
      mStart = d.mTsStart;
    if (d.mTsStart > mEnd)
      mEnd = d.mTsStart;
    mValid = true;
    return mCache.put(d.mTsStart, d);
  }

  public Datapoint updateDatapoint(Datapoint d) {
    if (mCache.get(d.mTsStart) == null)
      return null;
    return mCache.put(d.mTsStart, d);
  }

  public ArrayList<Datapoint> getDataInRange(int msStart, int msEnd) {
    ArrayList<Datapoint> range = new ArrayList<Datapoint>();
    SortedMap<Integer, Datapoint> map;

    if (msStart > msEnd)
      return range;
    
    try {
      map = mCache.subMap(Integer.valueOf(msStart), Integer.valueOf(msEnd + 1));
    } catch (NullPointerException e) {
      return range;
    }

    Iterator<Datapoint> iterator = map.values().iterator();
    while (iterator.hasNext()) {
      Datapoint d = iterator.next();
      if (d != null)
        range.add(d);
    }

    return range;
  }

  public ArrayList<Datapoint> getDataBefore(int number, int ms) {
    ArrayList<Datapoint> pre = new ArrayList<Datapoint>();
    SortedMap<Integer, Datapoint> range;
    SortedMap<Integer, Datapoint> reverse;
    
    try {
      range = mCache.headMap(Integer.valueOf(ms));
    } catch (NullPointerException e) {
      return pre;
    } catch (IllegalArgumentException e) {
      return pre;
    }
    
    reverse = new TreeMap<Integer, Datapoint>(java.util.Collections.reverseOrder());
    reverse.putAll(range);

    Iterator<Datapoint> iterator = reverse.values().iterator();
    for (int i = 0; i < number && iterator.hasNext();) {
      Datapoint d = iterator.next();
      if (d != null) {
        i++;
        pre.add(0, d);
      }
    }

    return pre;
  }

  public ArrayList<Datapoint> getDataAfter(int number, int ms) {
    ArrayList<Datapoint> post = new ArrayList<Datapoint>();
    SortedMap<Integer, Datapoint> range;

    try {
      range = mCache.tailMap(Integer.valueOf(ms) + 1);
    } catch (NullPointerException e) {
      return post;
    } catch (IllegalArgumentException e) {
      return post;
    }

    Iterator<Datapoint> iterator = range.values().iterator();
    for (int i = 0; i < number && iterator.hasNext();) {
      Datapoint d = iterator.next();
      if (d != null) {
        i++;
        post.add(d);
      }
    }

    return post;
  }

  public ArrayList<Datapoint> getLast(int number) {
    ArrayList<Datapoint> last = new ArrayList<Datapoint>();
    SortedMap<Integer, Datapoint> reverse = new TreeMap<Integer, Datapoint>(
        java.util.Collections.reverseOrder());
    reverse.putAll(mCache);

    Iterator<Datapoint> iterator = reverse.values().iterator();
    for (int i = 0; i < number && iterator.hasNext();) {
      Datapoint d = iterator.next();
      if (d != null) {
        i++;
        last.add(0, d);
      }
    }

    return last;
  }

  private void resetRangeMarkers() {
    mValid = false;
    mStart = Integer.MAX_VALUE;
    mEnd = Integer.MIN_VALUE;
  }
}
