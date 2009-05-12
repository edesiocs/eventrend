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

package net.redgeek.android.eventgrapher;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import net.redgeek.android.eventgrapher.primitives.TimeSeriesCollector;

import java.util.ArrayList;
import java.util.List;

public class GraphFilterListAdapter extends BaseAdapter {
  private Context mCtx;
  private List<GraphFilterRow> mItems = new ArrayList<GraphFilterRow>();
  private TimeSeriesCollector mTSC;

  public GraphFilterListAdapter(Context context, TimeSeriesCollector tsc) {
    mCtx = context;
    mTSC = tsc;
  }

  public void addItem(GraphFilterRow it) {
    mItems.add(it);
  }

  public void setListItems(List<GraphFilterRow> lit) {
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
    return new GraphFilterRowView(mCtx, mItems.get(position), mTSC);
  }
}
