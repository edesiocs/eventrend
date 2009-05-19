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

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import net.redgeek.android.eventrecorder.DateMapCache;
import net.redgeek.android.eventrecorder.TimeSeriesData.TimeSeries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class EntryListAdapter extends BaseAdapter {
  private Context mCtx;
  private DateMapCache mDateMap;
  private HashMap<Long,String> mTsType;
  private List<EntryRow> mItems = new ArrayList<EntryRow>();

  public EntryListAdapter(Context context, DateMapCache dateMap, 
      HashMap<Long,String> tsType) {
    mCtx = context;
    mDateMap = dateMap;
    mTsType = tsType;
  }

  public void addItem(EntryRow it) {
    mItems.add(it);
  }

  public void setListItems(List<EntryRow> lit) {
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
    EntryRowView row;
    String type = mTsType.get(Long.valueOf(mItems.get(position).mTimeSeriesId));
    if (TextUtils.isEmpty(type) == true) {
      type = TimeSeries.TYPE_DISCRETE;
    }
    
    if (convertView == null) {
      row = new EntryRowView(mCtx, mItems.get(position), mDateMap, type);
      row.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
    } else {
      row = (EntryRowView) convertView;

      row.setTimestamp(mItems.get(position).mTsStart);
      row.setValue(mItems.get(position).mValue);
    }
    return row;
  }
}
