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

package net.redgeek.android.eventrend.test.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.Map.Entry;

import net.redgeek.android.eventrend.primitives.Datapoint;

import android.content.ContentResolver;
import android.database.CharArrayBuffer;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;

public class MockCursor implements Cursor {
	private HashMap<Integer, String> 			mColumnMap;
	private HashMap<String, Integer> 			mReverseColumnMap;
	private HashMap<String, String> 			mCursor;
	private ArrayList<HashMap<String, String>>  mContents;
	private int								    mPosition;

	public MockCursor() {
		mContents = new ArrayList<HashMap<String, String>>();
		mReverseColumnMap = new HashMap<String, Integer>();
		mPosition = -1;
	}
	
	public void setColumnMap(HashMap<Integer, String> columnMap) {
		mColumnMap = new HashMap<Integer, String>(columnMap);
		Iterator<Entry<Integer, String>> iterator = mColumnMap.entrySet().iterator();
		while (iterator.hasNext()) {
			HashMap.Entry<Integer, String> entry = (HashMap.Entry<Integer, String>) iterator.next();
			Integer i = (Integer) entry.getKey();
			String  s = (String) entry.getValue();
			mReverseColumnMap.put(s, i);
		}		
	}

	public void setQueryResults(ArrayList<HashMap<String, String>> contents) {
		mContents = contents;
	}

	public void close() { 
		mColumnMap.clear();
		mReverseColumnMap.clear();
		mContents.clear();
		return; 
	}
	
	// These should be implemented to really flush out the mock, but I'm not using
	// them right now, so I' haven't implemented them.
	public void copyStringToBuffer(int arg0, CharArrayBuffer arg1) { return; }
	public void deactivate() { return; }
	public byte[] getBlob(int arg0) { return null; }
	public Bundle getExtras() { return null; }
	public boolean getWantsAllOnMoveCalls() { return false; }
	public boolean isClosed() { return false; }
	public void registerContentObserver(ContentObserver arg0) { return; }
	public void registerDataSetObserver(DataSetObserver arg0) { return; }
	public boolean requery() { return false; }
	public Bundle respond(Bundle arg0) { return null; }
	public void setNotificationUri(ContentResolver arg0, Uri arg1) { return; }
	public void unregisterContentObserver(ContentObserver arg0) { return; }
	public void unregisterDataSetObserver(DataSetObserver arg0) { return; }

	public int getColumnCount() {
		if (mColumnMap.keySet() != null)
			return mColumnMap.keySet().size();
		return 0;
	}

	public int getColumnIndex(String arg0) {
		Integer i = null;
		if (getColumnCount() != 0)
			i = mReverseColumnMap.get(arg0);
		return (i == null ? -1 : i);
	}

	public int getColumnIndexOrThrow(String arg0) throws IllegalArgumentException {
		Integer i = null;
		if (getColumnCount() != 0)
			i = mReverseColumnMap.get(arg0);
		if (i == null)
			throw new IllegalArgumentException();
		return i;
	}

	public String getColumnName(int arg0) {
		String s = null;
		if (getColumnCount() != 0)
			s = mColumnMap.get(Integer.valueOf(arg0));
		return s;
	}

	public String[] getColumnNames() {
		String[] out = new String[getColumnCount()];
		for (int i = 0; i < getColumnCount(); i++) {
			out[i] = getColumnName(i);
		}
		return out;
	}

	public int getCount() {
		return mContents.size();
	}

	public double getDouble(int arg0) {
		return Double.valueOf(mCursor.get(mColumnMap.get(Integer.valueOf(arg0))));
	}


	public float getFloat(int arg0) {
		return Float.valueOf(mCursor.get(mColumnMap.get(Integer.valueOf(arg0))));
	}

	public int getInt(int arg0) {
		return Integer.valueOf(mCursor.get(mColumnMap.get(Integer.valueOf(arg0))));
	}

	public long getLong(int arg0) {
		return Long.valueOf(mCursor.get(mColumnMap.get(Integer.valueOf(arg0))));
	}

	public int getPosition() {
		return mPosition;
	}

	public short getShort(int arg0) {
		return Short.valueOf(mCursor.get(mColumnMap.get(Integer.valueOf(arg0))));
	}

	public String getString(int arg0) {
		return mCursor.get(mColumnMap.get(Integer.valueOf(arg0)));
	}

	public boolean isAfterLast() {
		if (mPosition >= mContents.size())
			return true;
		return false;
	}

	public boolean isBeforeFirst() {
		if (mPosition < 0)
			return true;
		return false;
	}

	public boolean isFirst() {
		if (mPosition == 0)
			return true;
		return false;
	}

	public boolean isLast() {
		if (mPosition == mContents.size() - 1)
			return true;
		return false;
	}

	public boolean isNull(int arg0) {
		if (getString(arg0) == null)
			return true;
		return false;
	}

	public boolean move(int arg0) {
		mPosition += arg0;
		return updateCursor();
	}

	public boolean moveToFirst() {
		mPosition = 0;
		return updateCursor();
	}

	public boolean moveToLast() {
		mPosition = mContents.size() - 1;
		return updateCursor();
	}

	public boolean moveToNext() {
		mPosition++;
		return updateCursor();
	}

	public boolean moveToPosition(int arg0) {
		mPosition = arg0;
		return updateCursor();
	}

	public boolean moveToPrevious() {
		mPosition--;
		return updateCursor();
	}
	
	private boolean updateCursor() {
		if (mPosition >= 0 && mPosition < mContents.size()) {
			mCursor = mContents.get(mPosition);
			return true;
		}
		return false;
	}
}

