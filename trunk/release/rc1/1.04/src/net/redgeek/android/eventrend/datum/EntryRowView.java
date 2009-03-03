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

import net.redgeek.android.eventrend.util.DateUtil;
import android.content.Context;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class EntryRowView extends TableLayout {
	private TextView mCategoryText;
	private TextView mValueText;
	private TextView mTimestampText;

	private long mRowId;
	
	private TableRow mRow;
	private int mPad = 2;
         
    public EntryRowView(Context context, EntryRow anEntry) {
    	super(context);

    	setupUI(context);
    	populateFields(anEntry);
    }
    
    private void setupUI(Context context) {
        mRow = new TableRow(context);
    	
        mCategoryText = new TextView(context);
        mCategoryText.setPadding(mPad, mPad, mPad, mPad);

        mTimestampText = new TextView(context);
        mTimestampText.setPadding(mPad, mPad, mPad, mPad);

        mValueText = new TextView(context);
        mValueText.setPadding(mPad, mPad, mPad, mPad);

        mRow.addView(mCategoryText, new TableRow.LayoutParams(1));
        mRow.addView(mTimestampText, new TableRow.LayoutParams(1));
        mRow.addView(mValueText, new TableRow.LayoutParams(1));
        
        this.addView(mRow, new TableLayout.LayoutParams());
    }
    
    private void populateFields(EntryRow entry) {
        mCategoryText.setText(entry.getCategoryName());
        mTimestampText.setText(DateUtil.toTimestamp(entry.getDbRow().getTimestamp()));
        mValueText.setText(Float.valueOf(entry.getDbRow().getValue()).toString());
        mRowId = entry.getDbRow().getId();
    }

    public void setCategoryName(String words) {
        mCategoryText.setText(words);
    }

    public void setValue(float value) {
        mValueText.setText(Float.valueOf(value).toString());
    }

    public void setTimestamp(long timestamp) {
    	mTimestampText.setText(DateUtil.toTimestamp(timestamp));
    }
    
    public long getRowId() {
    	return mRowId;
   	
    }
}