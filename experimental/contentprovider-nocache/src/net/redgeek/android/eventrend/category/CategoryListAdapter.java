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

import java.util.ArrayList;
import java.util.List;

import net.redgeek.android.eventrecorder.IEventRecorderService;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class CategoryListAdapter extends BaseAdapter {
  private Context mCtx;
  private List<CategoryRow> mItems = new ArrayList<CategoryRow>();

  public CategoryListAdapter(Context context) {
    mCtx = context;
  }

  public void addItem(CategoryRow it) {
    for (int i = 0; i < mItems.size(); i++) {
      CategoryRow inPlace = mItems.get(i);
      if (it.compareTo(inPlace) < 0) {
        mItems.add(i, it);
        return;
      }
    }
    mItems.add(it);
  }

  public void setListItems(List<CategoryRow> lit) {
    mItems = lit;
  }

  public int getCount() {
    return mItems.size();
  }

  public Object getItem(int position) {
    return mItems.get(position);
  }

  public boolean areAllItemsSelectable() {
    return false;
  }

  public boolean isSelectable(int position) {
    try {
      return mItems.get(position).isSelectable();
    } catch (IndexOutOfBoundsException aioobe) {
      return false;
    }
  }

  public long getItemId(int position) {
    return position;
  }

  public View getView(int position, View convertView, ViewGroup parent) {
    CategoryRowView row;
    if (convertView == null) {
      row = new CategoryRowView(mCtx, mItems.get(position));
      row.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
    } else {
      row = (CategoryRowView) convertView;
      CategoryRow cat = mItems.get(position);
      row.populateFields(cat);
//      row.mRow = cat;      
//      row.mRow.mId = cat.mId;
//      row.mRow.mTimeSeriesName = new String(cat.mTimeSeriesName);
//      row.mRow.mRecordingDatapointId = cat.mRecordingDatapointId;
//      row.mRow.mGroup = new String(cat.mGroup);
//      row.mRow.mDefaultValue = cat.mDefaultValue;
//      row.mRow.mIncrement = cat.mIncrement;
//      row.mRow.mGoal = cat.mGoal;
//      row.mRow.mColor = new String(cat.mColor);
//      row.mRow.mPeriod = cat.mPeriod;
//      row.mRow.mUnits = new String(cat.mUnits);
//      row.mRow.mRank = cat.mRank;
//      row.mRow.mAggregation = new String(cat.mAggregation);
//      row.mRow.mType = new String(cat.mType);
//      row.mRow.mZerofill = cat.mZerofill;
//      row.mRow.mFormula = new String(cat.mFormula);
//      row.mRow.mInterpolation = new String(cat.mInterpolation);
//      row.mRow.mSensitivity = cat.mSensitivity;
//      row.mRow.mSmoothing = cat.mSmoothing;
//      row.mRow.mHistory = cat.mHistory;
//      row.mRow.mDecimals = cat.mDecimals;
//      row.mRow.mTimestamp = cat.mTimestamp;
    }
    return row;
  }    
}
