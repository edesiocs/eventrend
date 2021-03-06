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

package net.redgeek.android.eventrend.primitives;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class CategoryDatapointCache {
	private long                    mCatId;
	private boolean                 mValid;
	private TreeMap<Long,Datapoint> mCache;
	private long					mStart;
	private long					mEnd;
	private int  					mHistory;
	
	public CategoryDatapointCache(long catId, int history) {
		mCatId = catId;
		mValid = false;
		mCache = new TreeMap<Long,Datapoint>();
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

	public long getStart() {
		return mStart;
	}

	public void updateStart(long start) {
		if (start < mStart)
			mStart = start;
	}

	public long getEnd() {
		return mEnd;
	}

	public void updateEnd(long end) {
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
		if (d.mMillis < mStart)
			mStart = d.mMillis;
		if (d.mMillis > mEnd)
			mEnd = d.mMillis;
		mValid = true;
		return mCache.put(d.mMillis, d);
	}
	
	public Datapoint updateDatapoint(Datapoint d) {
		if (mCache.get(d.mMillis) == null)
			return null;
		return mCache.put(d.mMillis, d);
	}
	
	public ArrayList<Datapoint> getDataInRange(long msStart, long msEnd) {
		ArrayList<Datapoint> range = new ArrayList<Datapoint>();
		SortedMap<Long, Datapoint> map = mCache.subMap(Long.valueOf(msStart), Long.valueOf(msEnd+1));

        Iterator iterator = map.entrySet().iterator();        
		while (iterator.hasNext()) {
			Map.Entry entry = (Map.Entry) iterator.next();
            Datapoint d = (Datapoint) entry.getValue();
            if (d != null) {
            	range.add(d);
            }
		}
		
		return range;
	}

	public ArrayList<Datapoint> getDataBefore(int number, long ms) {
		ArrayList<Datapoint> pre = new ArrayList<Datapoint>();
		SortedMap<Long, Datapoint> range = mCache.headMap(Long.valueOf(ms));
		SortedMap<Long, Datapoint> reverse = new TreeMap<Long, Datapoint>(java.util.Collections.reverseOrder());
		reverse.putAll(range);
		
        Iterator iterator = reverse.entrySet().iterator();        
		for (int i = 0; i < number && iterator.hasNext(); ) {
            Map.Entry entry = (Map.Entry) iterator.next();
            Datapoint d = (Datapoint) entry.getValue();
            if (d != null) {
            	i++;
            	pre.add(0, d);
            }
		}
		
		return pre;
	}

	public ArrayList<Datapoint> getDataAfter(int number, long ms) {
		ArrayList<Datapoint> post = new ArrayList<Datapoint>();
		SortedMap<Long, Datapoint> range = mCache.tailMap(Long.valueOf(ms) + 1);		
        Iterator iterator = range.entrySet().iterator();      
        
		for (int i = 0; i < number && iterator.hasNext(); ) {
            Map.Entry entry = (Map.Entry) iterator.next();
            Datapoint d = (Datapoint) entry.getValue();
            if (d != null) {
            	i++;
            	post.add(d);
            }
		}
		
		return post;
	}	

	public ArrayList<Datapoint> getLast(int number) {
		ArrayList<Datapoint> last = new ArrayList<Datapoint>();
		SortedMap<Long, Datapoint> reverse = new TreeMap<Long, Datapoint>(java.util.Collections.reverseOrder());
		reverse.putAll(mCache);
		
        Iterator iterator = reverse.entrySet().iterator();        
		for (int i = 0; i < number && iterator.hasNext(); ) {
            Map.Entry entry = (Map.Entry) iterator.next();
            Datapoint d = (Datapoint) entry.getValue();
            if (d != null) {
            	i++;
            	last.add(0, d);
            }
		}
		
		return last;
	}	

	private void resetRangeMarkers() {
		mValid = false;
		mStart = Long.MAX_VALUE;
		mEnd   = Long.MIN_VALUE;		
	}
}
