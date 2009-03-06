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

package net.redgeek.android.eventrend.graph;

public class GraphFilterRow implements Comparable<GraphFilterRow> {
  private long mCategoryId;
  private String mCategoryName;
  private String mColor;
  private int mRank;

  private boolean mSelectable = true;

  public GraphFilterRow() {
  }

  public GraphFilterRow(long id, String name, String color, int rank) {
    mCategoryId = id;
    mCategoryName = name;
    mColor = color;
    mRank = rank;
  }

  public boolean isSelectable() {
    return mSelectable;
  }

  public void setSelectable(boolean selectable) {
    mSelectable = selectable;
  }

  public long getCategoryId() {
    return mCategoryId;
  }

  public void setCategoryId(long categoryId) {
    mCategoryId = categoryId;
  }

  public String getCategoryName() {
    return mCategoryName;
  }

  public void setCategoryName(String categoryName) {
    mCategoryName = categoryName;
  }

  public String getColor() {
    return mColor;
  }

  public void setColor(String color) {
    mColor = color;
  }

  public int getRank() {
    return mRank;
  }

  public void setRank(int rank) {
    mRank = rank;
  }

  public int compareTo(GraphFilterRow other) {
    if (this.mRank < other.mRank)
      return -1;
    else if (this.mRank > other.mRank)
      return 1;
    return 0;
  }
}
