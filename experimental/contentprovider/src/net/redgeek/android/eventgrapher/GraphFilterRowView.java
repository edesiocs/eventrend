///*
// * Copyright (C) 2007 The Android Open Source Project
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package net.redgeek.android.eventgrapher;
//
//import net.redgeek.android.eventrend.input.primitives.TimeSeriesCollector;
//import android.content.Context;
//import android.graphics.Color;
//import android.view.Gravity;
//import android.widget.CheckBox;
//import android.widget.CompoundButton;
//import android.widget.LinearLayout;
//import android.widget.TextView;
//
//public class GraphFilterRowView extends LinearLayout {
//  // UI elements
//  private TextView mCategoryNameView;
//  private CheckBox mVisibility;
//  private LinearLayout mCategoryLayout;
//
//  // Listeners
//  CheckBox.OnCheckedChangeListener mVisibilityListener;
//
//  // Private data
//  private long mCatId;
//  private String mCategoryName;
//  private String mColor;
//  private int mColorInt;
//  private TimeSeriesCollector mTSC;
//  private boolean mEnabled;
//
//  private Context mCtx;
//  private boolean mSelectable = true;
//
//  public GraphFilterRowView(Context context, GraphFilterRow aRow,
//      TimeSeriesCollector tsc) {
//    super(context);
//    mCtx = context;
//    mTSC = tsc;
//
//    setupData(aRow);
//    setupUI();
//    populateFields();
//  }
//
//  private void setupData(GraphFilterRow row) {
//    mCatId = row.getCategoryId();
//    mCategoryName = row.getCategoryName();
//    mColor = row.getColor();
//
//    try {
//      mColorInt = Color.parseColor(mColor);
//    } catch (IllegalArgumentException e) {
//      mColorInt = Color.BLACK;
//    }
//    mEnabled = mTSC.isSeriesEnabled(mCatId);
//    return;
//  }
//
//  private void setupUI() {
//    setupListeners();
//
//    this.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
//    this.setOrientation(HORIZONTAL);
//
//    mCategoryLayout = new LinearLayout(mCtx);
//    mCategoryLayout.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
//    mCategoryLayout.setOrientation(HORIZONTAL);
//    addView(mCategoryLayout, new LinearLayout.LayoutParams(
//        LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
//
//    mVisibility = new CheckBox(mCtx);
//    mVisibility.setOnCheckedChangeListener(mVisibilityListener);
//    mCategoryLayout.addView(mVisibility, new LinearLayout.LayoutParams(
//        LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
//
//    mCategoryNameView = new TextView(mCtx);
//    mCategoryNameView.setPadding(6, 0, 0, 0);
//    mCategoryNameView.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
//    mCategoryLayout.addView(mCategoryNameView, new LinearLayout.LayoutParams(
//        LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
//  }
//
//  private void setupListeners() {
//    mVisibilityListener = new CheckBox.OnCheckedChangeListener() {
//      public void onCheckedChanged(CompoundButton group, boolean isChecked) {
//        mTSC.setSeriesEnabled(mCatId, isChecked);
//      }
//    };
//  }
//
//  private void populateFields() {
//    mVisibility.setChecked(mEnabled);
//    mCategoryNameView.setText(mCategoryName);
//    mCategoryNameView.setTextColor(mColorInt);
//  }
//
//  public boolean isSelectable() {
//    return mSelectable;
//  }
//
//  public void setSelectable(boolean selectable) {
//    mSelectable = selectable;
//  }
//
//  public void display(long id, String name, String color) {
//    mCatId = id;
//    try {
//      mColorInt = Color.parseColor(color);
//    } catch (IllegalArgumentException e) {
//      mColorInt = Color.BLACK;
//    }
//
//    mEnabled = mTSC.isSeriesEnabled(mCatId);
//    mVisibility.setChecked(mEnabled);
//
//    mCategoryNameView.setText(name);
//    mCategoryNameView.setTextColor(mColorInt);
//  }
//}
