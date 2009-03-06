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

import net.redgeek.android.eventrend.db.CategoryDbTable;

public class CategoryRow implements Comparable<CategoryRow> {
  private CategoryDbTable.Row mRow;
  private long mTimestamp;

  private boolean mSelectable = true;

  public CategoryRow() {
  }

  public CategoryRow(CategoryDbTable.Row row) {
    mRow = new CategoryDbTable.Row(row);
    mTimestamp = 0;
  }

  public CategoryRow(CategoryDbTable.Row row, long timestamp) {
    mRow = new CategoryDbTable.Row(row);
    mTimestamp = timestamp;
  }

  public boolean isSelectable() {
    return mSelectable;
  }

  public void setSelectable(boolean selectable) {
    mSelectable = selectable;
  }

  public CategoryDbTable.Row getDbRow() {
    return mRow;
  }

  public void setDbRow(CategoryDbTable.Row row) {
    mRow = row;
  }

  public long getTimestamp() {
    return mTimestamp;
  }

  public void setTimestamp(long timestamp) {
    mTimestamp = timestamp;
  }

  public int compareTo(CategoryRow other) {
    if (this.mRow.getRank() < other.mRow.getRank())
      return -1;
    else if (this.mRow.getRank() > other.mRow.getRank())
      return 1;
    return 0;
  }
}
