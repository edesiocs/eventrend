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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.TextView;

import net.redgeek.android.eventrecorder.TimeSeriesData;
import net.redgeek.android.eventrecorder.TimeSeriesProvider;
import net.redgeek.android.eventrecorder.TimeSeriesData.TimeSeries;
import net.redgeek.android.eventrecorder.synthetic.Formula;
import net.redgeek.android.eventrend.EvenTrendActivity;
import net.redgeek.android.eventrend.R;
import net.redgeek.android.eventrend.util.ColorPickerDialog;
import net.redgeek.android.eventrend.util.ComboBox;
import net.redgeek.android.eventrend.util.DynamicSpinner;
import net.redgeek.android.eventrend.util.Number;

import java.util.ArrayList;

public class CategoryAdvancedEditActivity extends CategoryEditActivity {
  // Additional UI elements
  private LinearLayout mPeriodRow;
  private TableRow mGroupRow;
  private Button mOk;
  private TableRow mSeriesTypeRow;
  private TableRow mFormulaRow;
  private TableRow mDefaultValueRow;
  private TableRow mIncrementRow;
  private TableRow mZeroFillRow;
  private TableRow mTrendLabelRow;
  private TableRow mUnitsRow;
  private TableRow mHistoryRow;
  private TableRow mDecimalsRow;
  private TableRow mSmoothingRow;
  private TableRow mSensitivityRow;

  // Listeners
  private RadioGroup.OnCheckedChangeListener mAggListenerChild;
  private Spinner.OnItemSelectedListener mAggregatePeriodListenerChild;
  private Spinner.OnItemSelectedListener mSeriesTypeListenerChild;
  private CompoundButton.OnCheckedChangeListener mAdvancedListener;
  private View.OnClickListener mOkListener;

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
 
    setContentView(R.layout.category_edit_advanced);
    super.setupUI(findViewById(R.id.category_advanced_root));
    setupUILocal();

    super.populateFields();
    super.updatePaint(mColorStr);
  }
  
  private void setupUILocal() {
    setupListeners();

    mOk = (Button) findViewById(R.id.category_edit_ok);
    mOk.setOnClickListener(mOkListener);

    mFormulaRow = (TableRow) findViewById(R.id.category_edit_formula_row);
    mFormulaRow.setVisibility(View.GONE);
    mDefaultValueRow = (TableRow) findViewById(R.id.category_edit_default_value_row);
    mIncrementRow = (TableRow) findViewById(R.id.category_edit_increment_row);
    mZeroFillRow = (TableRow) findViewById(R.id.category_edit_zerofill_row);
    mZeroFillRow.setVisibility(View.GONE);
    mSeriesTypeRow = (TableRow) findViewById(R.id.category_edit_series_type_row);
    mPeriodRow = (TableRow) findViewById(R.id.category_edit_agg_period_row);
    mUnitsRow = (TableRow) findViewById(R.id.category_edit_units_row);
    mGroupRow = (TableRow) findViewById(R.id.category_edit_group_row);
    mHistoryRow = (TableRow) findViewById(R.id.category_edit_history_row);
    mDecimalsRow = (TableRow) findViewById(R.id.category_edit_decimals_row);
    mSmoothingRow = (TableRow) findViewById(R.id.category_edit_smoothing_row);
    mSensitivityRow = (TableRow) findViewById(R.id.category_edit_sensitivity_row);
    mTrendLabelRow = (TableRow) findViewById(R.id.category_edit_trend_row);
      
    mAggRadioGroup.setOnCheckedChangeListener(mAggListenerChild);
    mSeriesTypeSpinner.setOnItemSelectedListener(mSeriesTypeListenerChild);
    mAggregatePeriodSpinner.setOnItemSelectedListener(mAggregatePeriodListenerChild);

    // these changes the layout, so needs to be last
    mAdvancedCheck = (CheckBox) findViewById(R.id.category_edit_advanced);
    mAdvancedCheck.setOnCheckedChangeListener(mAdvancedListener);
    mAdvancedCheck.setChecked(false);
    setVisibleViews();
    
  }

  private void setupListeners() {
    mAggListenerChild = new RadioGroup.OnCheckedChangeListener() {
      public void onCheckedChanged(RadioGroup group, int checkedId) {
        CategoryEditActivity act = (CategoryEditActivity) mCtx;
        act.mAggListener.onCheckedChanged(group, checkedId);
        setVisibleViews();
      }
    };

    mAggregatePeriodListenerChild = new Spinner.OnItemSelectedListener() {
      public void onItemSelected(AdapterView parent, View v, int position,
          long id) {
        CategoryEditActivity act = (CategoryEditActivity) mCtx;
        act.mAggregatePeriodListener.onItemSelected(parent, v, position, id);
        setVisibleViews();
        return;
      }
      public void onNothingSelected(AdapterView arg0) {
        return;
      }
    };

    mSeriesTypeListenerChild = new Spinner.OnItemSelectedListener() {
      public void onItemSelected(AdapterView parent, View v, int position, long id) {
        CategoryEditActivity act = (CategoryEditActivity) mCtx;
        act.mSeriesTypeListener.onItemSelected(parent, v, position, id);
        setVisibleViews();
        return;
      }
      public void onNothingSelected(AdapterView arg0) {
        return;
      }
    };

    mAdvancedListener = new CompoundButton.OnCheckedChangeListener() {
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        setVisibleViews();
      }
    };

    mOkListener = new View.OnClickListener() {
      public void onClick(View view) {
        CategoryEditActivity act = (CategoryEditActivity) mCtx;
        setResult(act.saveState());
        finish();
      }
    };
  }

  private void setZerofillCheckStatus() {
    if (mAggRadio == null || mAdvancedCheck == null || mType == null)
      return;
      
    if (mPeriodSeconds == 0 
        || mType.toLowerCase().equals(TimeSeries.TYPE_SYNTHETIC)
        || mAggRadio.getText().toString().toLowerCase().equals(TimeSeries.AGGREGATION_AVG)
        || mAdvancedCheck.isChecked() == false) {
      mZeroFillRow.setVisibility(View.GONE);
    } else {
      mZeroFillRow.setVisibility(View.VISIBLE);
    }
  }
  
  private void setUserDefinedInput() {
    if (mType == null)
      return;
    
    if (mType.toLowerCase().equals(TimeSeries.TYPE_SYNTHETIC)) {
      mFormulaRow.setVisibility(View.VISIBLE);
      mDefaultValueRow.setVisibility(View.GONE);
      mIncrementRow.setVisibility(View.GONE);
    } else if (mType.toLowerCase().equals(TimeSeries.TYPE_RANGE)) {
      mFormulaRow.setVisibility(View.GONE);
      mDefaultValueRow.setVisibility(View.GONE);
      mIncrementRow.setVisibility(View.GONE);
    } else {
      mFormulaRow.setVisibility(View.GONE);
      mDefaultValueRow.setVisibility(View.VISIBLE);
      mIncrementRow.setVisibility(View.VISIBLE);
    }
  }
  
  private void setVisibleViews() {
    if (mAdvancedCheck == null)
      return;

    if (mAdvancedCheck != null && mAdvancedCheck.isChecked() == true) {
      mPeriodRow.setVisibility(View.VISIBLE);
      mSeriesTypeRow.setVisibility(View.VISIBLE);
      mGroupRow.setVisibility(View.VISIBLE);
      mTrendLabelRow.setVisibility(View.VISIBLE);
      mHistoryRow.setVisibility(View.VISIBLE);
      mDecimalsRow.setVisibility(View.VISIBLE);
      mSmoothingRow.setVisibility(View.VISIBLE);
      mSensitivityRow.setVisibility(View.VISIBLE);
      mUnitsRow.setVisibility(View.VISIBLE);
    } else {
      mPeriodRow.setVisibility(View.GONE);
      mSeriesTypeRow.setVisibility(View.GONE);
      mGroupRow.setVisibility(View.GONE);
      mTrendLabelRow.setVisibility(View.GONE);
      mHistoryRow.setVisibility(View.GONE);
      mDecimalsRow.setVisibility(View.GONE);
      mSmoothingRow.setVisibility(View.GONE);
      mSensitivityRow.setVisibility(View.GONE);
      mUnitsRow.setVisibility(View.GONE);
    }
    setZerofillCheckStatus();
    setUserDefinedInput();
  }    
}
