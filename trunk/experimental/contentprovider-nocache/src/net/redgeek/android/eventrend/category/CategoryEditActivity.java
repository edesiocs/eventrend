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

import android.app.Dialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
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
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import net.redgeek.android.eventrecorder.TimeSeriesData;
import net.redgeek.android.eventrecorder.TimeSeriesProvider;
import net.redgeek.android.eventrecorder.TimeSeriesData.TimeSeries;
import net.redgeek.android.eventrend.EvenTrendActivity;
import net.redgeek.android.eventrend.R;
import net.redgeek.android.eventrend.util.ColorPickerDialog;
import net.redgeek.android.eventrend.util.ComboBox;
import net.redgeek.android.eventrend.util.DynamicSpinner;
import net.redgeek.android.eventrend.util.Number;

import java.util.ArrayList;

public class CategoryEditActivity extends EvenTrendActivity {
  static final int DIALOG_HELP_GROUP = 1;
  static final int DIALOG_HELP_CATEGORY = 2;
  static final int DIALOG_HELP_GOAL = 3;
  static final int DIALOG_HELP_COLOR = 4;
  static final int DIALOG_HELP_AGGREGATE = 5;
  static final int DIALOG_HELP_AGGREGATE_PERIOD = 6;
  static final int DIALOG_HELP_UNITS = 8;
  static final int DIALOG_HELP_SYNTHETIC = 9;
  static final int DIALOG_HELP_FORMULA = 10;
  static final int DIALOG_HELP_DEFAULT_VALUE = 11;
  static final int DIALOG_HELP_INCREMENT = 12;
  static final int DIALOG_HELP_SERIES_TYPE = 13;
  static final int DIALOG_HELP_ZEROFILL = 14;
  static final int DIALOG_HELP_HISTORY = 15;
  static final int DIALOG_HELP_DECIMALS = 16;
  static final int DIALOG_HELP_SMOOTHING = 17;
  static final int DIALOG_HELP_SENSITIVITY = 18;

  // Common UI elements
  protected ComboBox mGroupCombo;
  protected EditText mCategoryText;
  protected Button mColorButton;
  protected DynamicSpinner mAggregatePeriodSpinner;
  protected EditText mGoalText;
  protected CheckBox mAdvancedCheck;
  protected DynamicSpinner mSeriesTypeSpinner;
  protected RadioGroup mAggRadioGroup;
  protected RadioButton mAggRadio;
  protected Button mFormulaEdit;
  protected EditText mDefaultValueText;
  protected EditText mIncrementText;
  protected CheckBox mZeroFillCheck;
  protected EditText mUnitsText;
  protected EditText mHistoryText;
  protected EditText mDecimalsText;
  protected EditText mSmoothingText;
  protected EditText mSensitivityText;
  protected TextView mFormulaText;

  // protected data
  protected View mRoot;
  protected Context mCtx;
  protected CategoryRow mRow;
  protected String mGroupName;
  protected int mPeriodSeconds;

  protected Paint mPickerPaint;
  protected String mColorStr;
  protected Long mRowId;
  protected int mMaxRank;
  protected String mType;
  
  // Listeners
  protected RadioGroup.OnCheckedChangeListener mAggListener;
  protected ColorPickerDialog.OnColorChangedListener mColorChangeListener;
  protected View.OnClickListener mColorButtonListener;
  protected Spinner.OnItemSelectedListener mAggregatePeriodListener;
  protected Spinner.OnItemSelectedListener mSeriesTypeListener;
  protected View.OnClickListener mFormulaEditListener;

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    mCtx = this;
    getSavedStateContent(icicle);
  }

  private void getSavedStateContent(Bundle icicle) {
    Intent it = getIntent();
    // prefer uris ...
    if (it != null) {
      Uri uri = it.getData();
      if (uri != null) {
        try {
          String rowIdStr = uri.getPathSegments().get(TimeSeriesProvider.PATH_SEGMENT_TIMESERIES_ID);
          mRowId = Long.valueOf(rowIdStr);
        } catch (Exception e) { } // nothing
      }
    }

    // try the icicle next ...
    if (mRowId == null || mRowId < 1) {
      if (icicle != null) {
        mRowId = icicle.getLong(TimeSeriesData.TimeSeries._ID);
        if (mRowId < 0)
          mRowId = null;
      }
      // lastly, fall back on the bundle ...
      if (mRowId == null) {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
          mRowId = extras.getLong(TimeSeriesData.TimeSeries._ID);
        }
      }
    }

    Uri timeseries = TimeSeriesData.TimeSeries.CONTENT_URI;
    Cursor c = getContentResolver().query(timeseries, null, null, null, null);
    mMaxRank = 0;
    if (c != null) {
      int count = c.getCount();
      c.moveToFirst();
      for (int i = 0; i < count; i++) {
        int rank = TimeSeries.getRank(c);
        if (rank > mMaxRank)
          mMaxRank = rank;
        if (mRowId != null && TimeSeries.getId(c) == mRowId)
          mRow = new CategoryRow(c);

        c.moveToNext();
      }
      c.close();
    }

    if (mRow == null)
      mRow = new CategoryRow();
  }
  
  public static class RestrictedNameFilter implements InputFilter {
    private String mInvalidRegex;

    public RestrictedNameFilter(String invalidRegex) {
      mInvalidRegex = invalidRegex;
    }

    public CharSequence filter(CharSequence source, int start, int end,
        Spanned dest, int dstart, int dend) {
      String out = "" + source;
      out = out.replaceAll(mInvalidRegex, "");
      return out.subSequence(0, out.length());
    }
  }

  protected void setupUI(View root) {
    mRoot = root;
    
    setupListeners();

    mCategoryText = (EditText) root.findViewById(R.id.category_edit_name);
    InputFilter[] FilterArray = new InputFilter[1];
    FilterArray[0] = new RestrictedNameFilter("[\"\\\\]");
    mCategoryText.setFilters(FilterArray);

    setHelpDialog(R.id.category_edit_name_view, DIALOG_HELP_CATEGORY);

    mGoalText = (EditText) root.findViewById(R.id.category_edit_goal);
    setHelpDialog(R.id.category_edit_goal_view, DIALOG_HELP_GOAL);

    mColorButton = (Button) root.findViewById(R.id.category_edit_color);
    mColorButton.setOnClickListener(mColorButtonListener);
    setHelpDialog(R.id.category_edit_color_view, DIALOG_HELP_COLOR);

    mAggregatePeriodSpinner = (DynamicSpinner) root.findViewById(R.id.category_edit_agg_period_menu);
    for (int i = 0; i < TimeSeries.AGGREGATION_PERIOD_NAMES.length; i++) {
      mAggregatePeriodSpinner.addSpinnerItem(
          TimeSeries.AGGREGATION_PERIOD_NAMES[i], new Long(
              TimeSeries.AGGREGATION_PERIOD_TIMES[i]));
    }
    mAggregatePeriodSpinner.setOnItemSelectedListener(mAggregatePeriodListener);
    mAggregatePeriodSpinner.setSelection(0);
    setHelpDialog(R.id.category_edit_agg_view, DIALOG_HELP_AGGREGATE_PERIOD);

    mSeriesTypeSpinner = (DynamicSpinner) root.findViewById(R.id.category_edit_series_type_menu);
    mSeriesTypeSpinner.setSelection(0);
    for (int i = 0; i < TimeSeries.TYPES.length; i++) {
      mSeriesTypeSpinner.addSpinnerItem(TimeSeries.TYPES[i], new Long(i));
      if (mRow.mType != null && mRow.mType.equals(TimeSeries.TYPES[i])) {
        mSeriesTypeSpinner.setSelection(i);
        mType = mRow.mType;
      }
    }
    mSeriesTypeSpinner.setOnItemSelectedListener(mSeriesTypeListener);
    setHelpDialog(R.id.category_edit_agg_view, DIALOG_HELP_SERIES_TYPE);

    mAggRadioGroup = (RadioGroup) root.findViewById(R.id.category_edit_agg);
    mAggRadioGroup.setOnCheckedChangeListener(mAggListener);
    mAggRadio = (RadioButton) root.findViewById(mAggRadioGroup
        .getCheckedRadioButtonId());
    setHelpDialog(R.id.category_edit_agg_view, DIALOG_HELP_AGGREGATE);

    mFormulaEdit = (Button) root.findViewById(R.id.category_edit_formula);
    mFormulaEdit.setOnClickListener(mFormulaEditListener);
    setHelpDialog(R.id.category_edit_formula_view, DIALOG_HELP_FORMULA);
    mFormulaText = (TextView) root.findViewById(R.id.category_edit_formula_text);
    
    mDefaultValueText = (EditText) root.findViewById(R.id.category_edit_default_value);
    setHelpDialog(R.id.category_edit_default_value_view,
        DIALOG_HELP_DEFAULT_VALUE);

    mIncrementText = (EditText) root.findViewById(R.id.category_edit_increment);
    setHelpDialog(R.id.category_edit_increment_view, DIALOG_HELP_INCREMENT);

    mZeroFillCheck = (CheckBox) root.findViewById(R.id.category_edit_zerofill);
    setHelpDialog(R.id.category_edit_zerofill_view, DIALOG_HELP_ZEROFILL);

    mUnitsText = (EditText) root.findViewById(R.id.category_edit_units);
    setHelpDialog(R.id.category_edit_units_view, DIALOG_HELP_UNITS);

    mGroupCombo = (ComboBox) root.findViewById(R.id.category_edit_group);
    ArrayList<String> groups = fetchAllGroups();
    int size = groups.size();
    String group;
    for (int i = 0; i < size; i++) {
      group = groups.get(i);
      mGroupCombo.addMenuItem(group);
      if (mGroupName != null && mGroupName.equals(group))
        mGroupCombo.setSelection(i);
    }
    setHelpDialog(R.id.category_edit_group_view, DIALOG_HELP_GROUP);

    mHistoryText = (EditText) root.findViewById(R.id.category_edit_history);
    setHelpDialog(R.id.category_edit_history_view, DIALOG_HELP_HISTORY);

    mDecimalsText = (EditText) root.findViewById(R.id.category_edit_decimals);
    setHelpDialog(R.id.category_edit_decimals_view, DIALOG_HELP_DECIMALS);

    mSmoothingText = (EditText) root.findViewById(R.id.category_edit_smoothing);
    setHelpDialog(R.id.category_edit_smoothing_view, DIALOG_HELP_SMOOTHING);

    mSensitivityText = (EditText) root.findViewById(R.id.category_edit_sensitivity);
    setHelpDialog(R.id.category_edit_sensitivity_view, DIALOG_HELP_SENSITIVITY);
  }

  private ArrayList<String> fetchAllGroups() {
    ArrayList<String> groups = new ArrayList<String>();
    String[] projection = new String[] { TimeSeries.GROUP_NAME };
    Uri timeseries = TimeSeriesData.TimeSeries.CONTENT_URI;
    Cursor c = getContentResolver().query(timeseries, projection, null, null,
        TimeSeries.GROUP_NAME + " asc ");
    if (c != null) {
      String group;
      int count = c.getCount();
      c.moveToFirst();
      for (int i = 0; i < count; i++) {
        group = TimeSeries.getGroupName(c);
        if (groups.contains(group) == false) {
          groups.add(group);
        }
        c.moveToNext();
      }
      c.close();
    }
    return groups;
  }
  
  private void setupListeners() {
    mAggListener = new RadioGroup.OnCheckedChangeListener() {
      public void onCheckedChanged(RadioGroup group, int checkedId) {
        mAggRadio = (RadioButton) mRoot.findViewById(checkedId);
      }
    };

    mColorChangeListener = new ColorPickerDialog.OnColorChangedListener() {
      public void colorChanged(int color) {
        mPickerPaint.setColor(color);
        mColorButton.setBackgroundColor(color);
        mColorStr = String.format("#%02x%02x%02x", Color.red(color), Color
            .green(color), Color.blue(color));
      }
    };

    mColorButtonListener = new View.OnClickListener() {
      public void onClick(View view) {
        ColorPickerDialog d = new ColorPickerDialog(mCtx, mColorChangeListener,
            mPickerPaint.getColor());
        d.show();
      }
    };

    mAggregatePeriodListener = new Spinner.OnItemSelectedListener() {
      public void onItemSelected(AdapterView parent, View v, int position,
          long id) {
        mPeriodSeconds = (int) mAggregatePeriodSpinner
            .getMappingFromPosition(position);
        return;
      }

      public void onNothingSelected(AdapterView arg0) {
        return;
      }
    };

    mSeriesTypeListener = new Spinner.OnItemSelectedListener() {
      public void onItemSelected(AdapterView parent, View v, int position,
          long id) {
        mType = ((TextView) v).getText().toString();
        return;
      }

      public void onNothingSelected(AdapterView arg0) {
        return;
      }
    };

    mFormulaEditListener = new View.OnClickListener() {
      public void onClick(View view) {
        // TODO: formula-related stuff
//         saveState();
         Intent i = new Intent(mCtx, FormulaEditorActivity.class);
         i.putExtra(TimeSeries._ID, mRowId);
         startActivityForResult(i, ARC_FORMULA_EDIT);
      }
    };
  }

  protected void updatePaint(String color) {
    int colorInt = Color.LTGRAY;
    try {
      colorInt = Color.parseColor(color);
    } catch (Exception e) {
    }
    mPickerPaint = new Paint();
    mPickerPaint.setAntiAlias(true);
    mPickerPaint.setDither(true);
    mPickerPaint.setColor(colorInt);
    mPickerPaint.setStyle(Paint.Style.FILL);
    mColorButton.setBackgroundColor(colorInt);
  }

  protected void populateFields() {
    if (mRowId != null && mRow != null) {
      mCategoryText.setText(mRow.mTimeSeriesName);
      mDefaultValueText.setText(Double.valueOf(mRow.mDefaultValue).toString());
      mIncrementText.setText(Double.valueOf(mRow.mIncrement).toString());
      mGoalText.setText(Double.valueOf(mRow.mGoal).toString());
      mSmoothingText.setText(Double.valueOf(mRow.mSmoothing).toString());
      mSensitivityText.setText(Double.valueOf(mRow.mSensitivity).toString());
      mHistoryText.setText(Integer.valueOf(mRow.mHistory).toString());
      mDecimalsText.setText(Integer.valueOf(mRow.mDecimals).toString());
      mUnitsText.setText(mRow.mUnits);

      mColorStr = mRow.mColor;
      mGroupName = mRow.mGroup;
      mGroupCombo.setText(mGroupName);

      if (mFormulaText != null && TextUtils.isEmpty(mRow.mFormula) == false)
        mFormulaText.setText(mRow.mFormula);
      
      mZeroFillCheck.setChecked(mRow.mZerofill > 0 ? true : false);
      
      mPeriodSeconds = mRow.mPeriod;
      int index = TimeSeries.periodToIndex(mPeriodSeconds);
      if (index < 0)
        index = 0;
      mAggregatePeriodSpinner.setSelection(index);

      for (int i = 0; i < TimeSeries.TYPES.length; i++) {
        if (mRow.mType.toLowerCase().equals(TimeSeries.TYPES[i])) {
          mSeriesTypeSpinner.setSelection(i);
          break;
        }
      }
        
      String aggregation = mRow.mAggregation;
      if (aggregation.toLowerCase().equals(TimeSeries.AGGREGATION_AVG)) {
        mAggRadio = (RadioButton) mRoot.findViewById(R.id.category_edit_agg_average);
      } else {
        mAggRadio = (RadioButton) mRoot.findViewById(R.id.category_edit_agg_sum);
      }
      mAggRadio.setChecked(true);
    } else {
      mGroupName = "Default";
      mGroupCombo.setText(mGroupName);
      mColorStr = "#cccccc";
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    if (mRowId != null)
      outState.putLong(TimeSeries._ID, mRowId);
    else
      outState.putLong(TimeSeries._ID, -1);
    super.onSaveInstanceState(outState);
  }

  @Override
  protected void onPause() {
    super.onPause();
  }

  @Override
  protected void onResume() {
    super.onResume();
  }

  protected int saveState() {
    double d;
    String value;
    ContentValues values = new ContentValues();

    mRow.mDecimals = Integer.valueOf(mDecimalsText.getText().toString())
        .intValue();

    values.put(TimeSeries.TIMESERIES_NAME, mCategoryText.getText().toString());
    values.put(TimeSeries.GROUP_NAME, mGroupCombo.getText().toString());
    d = Number.Round(Double.valueOf(mGoalText.getText().toString())
        .doubleValue(), mRow.mDecimals);
    values.put(TimeSeries.GOAL, d);
    values.put(TimeSeries.COLOR, mColorStr);
    values.put(TimeSeries.PERIOD, mPeriodSeconds);
    values.put(TimeSeries.UNITS, mUnitsText.getText().toString());
    values.put(TimeSeries.ZEROFILL, mZeroFillCheck.isChecked() ? 1 : 0);
    d = Number.Round(Double.valueOf(mSensitivityText.getText().toString())
        .doubleValue(), mRow.mDecimals * 2);
    values.put(TimeSeries.SENSITIVITY, d);
    d = Number.Round(Double.valueOf(mSmoothingText.getText().toString())
        .doubleValue(), mRow.mDecimals * 2);
    values.put(TimeSeries.SMOOTHING, d);
    values.put(TimeSeries.HISTORY, Integer.valueOf(
        mHistoryText.getText().toString()).intValue());
    values.put(TimeSeries.DECIMALS, Integer.valueOf(
        mDecimalsText.getText().toString()).intValue());
    values
        .put(TimeSeries.TYPE, mSeriesTypeSpinner.getSelectedItem().toString());
    values.put(TimeSeries.INTERPOLATION, "");
    // TODO: formula-related stuff
    if (mRow.mFormula == null || TextUtils.isEmpty(mRow.mFormula))
      values.put(TimeSeries.FORMULA, "");
    else
      values.put(TimeSeries.FORMULA, mRow.mFormula);

    mAggRadio = (RadioButton) mRoot.findViewById(mAggRadioGroup
        .getCheckedRadioButtonId());
    values.put(TimeSeries.AGGREGATION, mAggRadio.getText().toString()
        .toLowerCase());

    if (mSeriesTypeSpinner.getSelectedItem().toString().toLowerCase().equals(
        TimeSeries.TYPE_SYNTHETIC)) {
      values.put(TimeSeries.DEFAULT_VALUE, 0.0f);
      values.put(TimeSeries.INCREMENT, 1.0f);
    } else {
      d = Number.Round(Double.valueOf(mDefaultValueText.getText().toString())
          .doubleValue(), mRow.mDecimals);
      values.put(TimeSeries.DEFAULT_VALUE, d);
      d = Number.Round(Double.valueOf(mIncrementText.getText().toString())
          .doubleValue(), mRow.mDecimals);
      values.put(TimeSeries.INCREMENT, d);
    }

    if (mRowId == null) {
      // insert
      values.put(TimeSeries.RECORDING_DATAPOINT_ID, 0);
      values.put(TimeSeries.RANK, mMaxRank + 1);
      Uri uri = getContentResolver().insert(
          TimeSeriesData.TimeSeries.CONTENT_URI, values);
      if (uri != null) {
        String rowIdStr = uri.getPathSegments().get(
            TimeSeriesProvider.PATH_SEGMENT_TIMESERIES_ID);
        mRowId = Long.valueOf(rowIdStr);
        return CATEGORY_CREATED;
      }
    } else {
      // update
      values.put(TimeSeries.RANK, mRow.mRank);
      Uri uri = ContentUris.withAppendedId(
          TimeSeriesData.TimeSeries.CONTENT_URI, mRowId);
      getContentResolver().update(uri, values, null, null);
      return CATEGORY_MODIFIED;
    }
    
    return CATEGORY_OP_ERR;
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    switch(requestCode) { 
      case ARC_FORMULA_EDIT:
        switch(resultCode) {
          case RESULT_OK:
            mRow.mFormula = intent.getStringExtra(TimeSeries.FORMULA);
            break; 
          default:
            break;
        }
        break;
      default:
        break;
    } 
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    String title;
    String msg;
    switch (id) {
      case DIALOG_HELP_GROUP:
        title = getResources().getString(R.string.cat_group_title);
        msg = getResources().getString(R.string.cat_group_desc);
        return mDialogUtil.newOkDialog(title, msg + "\n");
      case DIALOG_HELP_CATEGORY:
        title = getResources().getString(R.string.cat_category_title);
        msg = getResources().getString(R.string.cat_category_desc);
        return mDialogUtil.newOkDialog(title, msg + "\n");
      case DIALOG_HELP_DEFAULT_VALUE:
        title = getResources().getString(R.string.cat_default_value_title);
        msg = getResources().getString(R.string.cat_default_value_desc);
        return mDialogUtil.newOkDialog(title, msg + "\n");
      case DIALOG_HELP_INCREMENT:
        title = getResources().getString(R.string.cat_increment_title);
        msg = getResources().getString(R.string.cat_increment_desc);
        return mDialogUtil.newOkDialog(title, msg + "\n");
      case DIALOG_HELP_GOAL:
        title = getResources().getString(R.string.cat_goal_title);
        msg = getResources().getString(R.string.cat_goal_desc);
        return mDialogUtil.newOkDialog(title, msg + "\n");
      case DIALOG_HELP_COLOR:
        title = getResources().getString(R.string.cat_color_title);
        msg = getResources().getString(R.string.cat_color_desc);
        return mDialogUtil.newOkDialog(title, msg + "\n");
      case DIALOG_HELP_AGGREGATE:
        title = getResources().getString(R.string.cat_aggregate_title);
        msg = getResources().getString(R.string.cat_aggregate_desc);
        return mDialogUtil.newOkDialog(title, msg + "\n");
      case DIALOG_HELP_AGGREGATE_PERIOD:
        title = getResources().getString(R.string.cat_aggregate_period_title);
        msg = getResources().getString(R.string.cat_aggregate_period_desc);
        return mDialogUtil.newOkDialog(title, msg + "\n");
      case DIALOG_HELP_ZEROFILL:
        title = getResources().getString(R.string.cat_zerofill_title);
        msg = getResources().getString(R.string.cat_zerofill_desc);
        return mDialogUtil.newOkDialog(title, msg + "\n");
      case DIALOG_HELP_SERIES_TYPE:
        title = getResources().getString(R.string.cat_series_type_title);
        msg = getResources().getString(R.string.cat_series_type_desc);
        return mDialogUtil.newOkDialog(title, msg + "\n");
      case DIALOG_HELP_FORMULA:
        title = getResources().getString(R.string.cat_formula_title);
        msg = getResources().getString(R.string.cat_formula_desc);
        return mDialogUtil.newOkDialog(title, msg + "\n");
      case DIALOG_HELP_UNITS:
        title = getResources().getString(R.string.cat_units_title);
        msg = getResources().getString(R.string.cat_units_desc);
        return mDialogUtil.newOkDialog(title, msg + "\n");
      case DIALOG_HELP_HISTORY:
        title = getResources().getString(R.string.cat_history_title);
        msg = getResources().getString(R.string.cat_history_desc);
        return mDialogUtil.newOkDialog(title, msg + "\n");
      case DIALOG_HELP_DECIMALS:
        title = getResources().getString(R.string.cat_decimals_title);
        msg = getResources().getString(R.string.cat_decimals_desc);
        return mDialogUtil.newOkDialog(title, msg + "\n");
      case DIALOG_HELP_SMOOTHING:
        title = getResources().getString(R.string.cat_smoothing_title);
        msg = getResources().getString(R.string.cat_smoothing_desc);
        return mDialogUtil.newOkDialog(title, msg + "\n");
      case DIALOG_HELP_SENSITIVITY:
        title = getResources().getString(R.string.cat_sensitivity_title);
        msg = getResources().getString(R.string.cat_sensitivity_desc);
        return mDialogUtil.newOkDialog(title, msg + "\n");
    }
    return null;
  }

  private void setHelpDialog(int resId, final int dialog) {
    TextView tv = (TextView) mRoot.findViewById(resId);
    tv.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        showDialog(dialog);
      }
    });
  }
}
