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

import net.redgeek.android.eventrend.db.EntryDbTable;


public class EntryRow implements Comparable<EntryRow>{
	private EntryDbTable.Row mRow;
	private String mCategoryName;
	private boolean mSelectable = true;

	public EntryRow() {
	}

	public EntryRow(EntryDbTable.Row row, String categoryName) {
		mRow = new EntryDbTable.Row(row); 
		mCategoryName = categoryName;
	}
     
    public boolean isSelectable() {
    	return mSelectable;
    }
     
    public void setSelectable(boolean selectable) {
        mSelectable = selectable;
    }
    
    public EntryDbTable.Row getDbRow() {
    	return mRow;
    }
    
    public void setDbRow(EntryDbTable.Row row) {
    	mRow = row;
    }

    public String getCategoryName() {
    	return mCategoryName;
    }
    
    public void setCategoryName(String name) {
    	mCategoryName = name;
    }

    public int compareTo(EntryRow other) {
    	if (this.mRow.getTimestamp() < other.mRow.getTimestamp())
    		return -1;
    	else if (this.mRow.getTimestamp() > other.mRow.getTimestamp())
    		return 1;
    	return 0;
    }
} 