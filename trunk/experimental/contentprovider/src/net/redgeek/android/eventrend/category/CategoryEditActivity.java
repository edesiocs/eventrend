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
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
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
import net.redgeek.android.eventrecorder.TimeSeriesData.Datapoint;
import net.redgeek.android.eventrecorder.TimeSeriesData.TimeSeries;
import net.redgeek.android.eventrend.EvenTrendActivity;
import net.redgeek.android.eventrend.R;
import net.redgeek.android.eventrend.util.ColorPickerDialog;
import net.redgeek.android.eventrend.util.ComboBox;
import net.redgeek.android.eventrend.util.DynamicSpinner;
import net.redgeek.android.eventrend.util.Number;

import java.util.ArrayList;

public class CategoryEditActivity extends EvenTrendActivity {
  public static final int CATEGORY_CREATED  = RESULT_FIRST_USER + 1;
  public static final int CATEGORY_MODIFIED = RESULT_FIRST_USER + 2;
  public static final int CATEGORY_DELETED  = RESULT_FIRST_USER + 3;
  public static final int CATEGORY_OP_ERR   = RESULT_FIRST_USER + 10;
  
  static final int DELETE_DIALOG_ID = 0;
  static final int DIALOG_HELP_GROUP = 1;
  static final int DIALOG_HELP_CATEGORY = 2;
  static final int DIALOG_HELP_GOAL = 3;
  static final int DIALOG_HELP_COLOR = 4;
  static final int DIALOG_HELP_AGGREGATE = 5;
  static final int DIALOG_HELP_AGGREGATE_PERIOD = 6;
  static final int DIALOG_HELP_UNITS = 8;
  // synthetic config:
  static final int DIALOG_HELP_SYNTHETIC = 9;
  static final int DIALOG_HELP_FORMULA = 10;
  // standard config:
  static final int DIALOG_HELP_DEFAULT_VALUE = 11;
  static final int DIALOG_HELP_INCREMENT = 12;
  static final int DIALOG_HELP_SERIES_TYPE = 13;
  static final int DIALOG_HELP_ZEROFILL = 14;
  // previously preferences:
  static final int DIALOG_HELP_HISTORY = 15;
  static final int DIALOG_HELP_DECIMALS = 16;
  static final int DIALOG_HELP_SMOOTHING = 17;
  static final int DIALOG_HELP_SENSITIVITY = 18;

  // UI elements
  private LinearLayout mPeriodRow;
  private TableRow mGroupRow;
  private ComboBox mGroupCombo;
  private EditText mCategoryText;
  private Button mColorButton;
  private DynamicSpinner mAggregatePeriodSpinner;
  private EditText mGoalText;
  private Button mOk;
  private Button mDelete;
  private CheckBox mAdvancedCheck;
  private TableRow mSeriesTypeRow;
  private DynamicSpinner mSeriesTypeSpinner;
  private RadioGroup mAggRadioGroup;
  private RadioButton mAggRadio;
  // synthetic elements:
  private TableRow mFormulaRow;
  private Button mFormulaEdit;
  // standard elements:
  private TableRow mDefaultValueRow;
  private EditText mDefaultValueText;
  private TableRow mIncrementRow;
  private EditText mIncrementText;
  private TableRow mZeroFillRow;
  private CheckBox mZeroFillCheck;
  // previously in prefs
  private TableRow mTrendLabelRow;
  private TableRow mUnitsRow;
  private EditText mUnitsText;
  private TableRow mHistoryRow;
  private EditText mHistoryText;
  private TableRow mDecimalsRow;
  private EditText mDecimalsText;
  private TableRow mSmoothingRow;
  private EditText mSmoothingText;
  private TableRow mSensitivityRow;
  private EditText mSensitivityText;

  // Private data
  private CategoryRow mRow;
  private String mGroupName;
  private int mPeriodSeconds;

  private Paint mPickerPaint;
  private String mColorStr;
  private Long mRowId;
  private int mMaxRank;
  private boolean mSave = false;

  // TODO: formula-related stuff
  // private Formula mFormula;

  // Listeners
  private RadioGroup.OnCheckedChangeListener mAggListener;
  private ColorPickerDialog.OnColorChangedListener mColorChangeListener;
  private View.OnClickListener mColorButtonListener;
  private Spinner.OnItemSelectedListener mAggregatePeriodListener;
  private CompoundButton.OnCheckedChangeListener mAdvancedListener;
  private Spinner.OnItemSelectedListener mSeriesTypeListener;
  private View.OnClickListener mFormulaEditListener;
  private View.OnClickListener mOkListener;
  private View.OnClickListener mDeleteListener;

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);

    getSavedStateContent(icicle);
    setupUI();
    populateFields();
    updatePaint(mColorStr);
  }

  private void getSavedStateContent(Bundle icicle) {
    Intent it = getIntent();
    // prefer uris ...
    if (it != null) {
      Uri uri = it.getData();
      if (uri != null) {
        try {
          String rowIdStr = uri.getPathSegments().get(TimeSeriesProvider.PATH_SEGMENT_TIMERSERIES_ID);
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
      // lastly, fall back on the icicle ...
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

  private void setupUI() {
    setContentView(R.layout.category_edit_advanced);

    setupListeners();

    mCategoryText = (EditText) findViewById(R.id.category_edit_name);
    InputFilter[] FilterArray = new InputFilter[1];
    FilterArray[0] = new RestrictedNameFilter("[\"\\\\]");
    mCategoryText.setFilters(FilterArray);

    setHelpDialog(R.id.category_edit_name_view, DIALOG_HELP_CATEGORY);

    mGoalText = (EditText) findViewById(R.id.category_edit_goal);
    setHelpDialog(R.id.category_edit_goal_view, DIALOG_HELP_GOAL);

    mColorButton = (Button) findViewById(R.id.category_edit_color);
    mColorButton.setOnClickListener(mColorButtonListener);
    setHelpDialog(R.id.category_edit_color_view, DIALOG_HELP_COLOR);

    mPeriodRow = (TableRow) findViewById(R.id.category_edit_agg_period_row);
    mAggregatePeriodSpinner = (DynamicSpinner) findViewById(R.id.category_edit_agg_period_menu);
    for (int i = 0; i < TimeSeries.AGGREGATION_PERIOD_NAMES.length; i++) {
      mAggregatePeriodSpinner.addSpinnerItem(
          TimeSeries.AGGREGATION_PERIOD_NAMES[i], new Long(
              TimeSeries.AGGREGATION_PERIOD_TIMES[i]));
    }
    mAggregatePeriodSpinner.setOnItemSelectedListener(mAggregatePeriodListener);
    mAggregatePeriodSpinner.setSelection(0);
    setHelpDialog(R.id.category_edit_agg_view, DIALOG_HELP_AGGREGATE_PERIOD);

    mSeriesTypeRow = (TableRow) findViewById(R.id.category_edit_series_type_row);
    mSeriesTypeSpinner = (DynamicSpinner) findViewById(R.id.category_edit_series_type_menu);
    for (int i = 0; i < TimeSeries.TYPES.length; i++) {
      mSeriesTypeSpinner.addSpinnerItem(TimeSeries.TYPES[i], new Long(i));
    }
    mSeriesTypeSpinner.setOnItemSelectedListener(mSeriesTypeListener);
    mSeriesTypeSpinner.setSelection(0);
    setHelpDialog(R.id.category_edit_agg_view, DIALOG_HELP_SERIES_TYPE);

    mOk = (Button) findViewById(R.id.category_edit_ok);
    mOk.setOnClickListener(mOkListener);

    mDelete = (Button) findViewById(R.id.category_edit_delete);
    mDelete.setOnClickListener(mDeleteListener);
    if (mRowId == null) {
      mDelete.setVisibility(View.INVISIBLE);
    }

    mAggRadioGroup = (RadioGroup) findViewById(R.id.category_edit_agg);
    mAggRadioGroup.setOnCheckedChangeListener(mAggListener);
    mAggRadio = (RadioButton) findViewById(mAggRadioGroup
        .getCheckedRadioButtonId());
    setHelpDialog(R.id.category_edit_agg_view, DIALOG_HELP_AGGREGATE);

    // synthetic elements:
    mFormulaRow = (TableRow) findViewById(R.id.category_edit_formula_row);
    mFormulaEdit = (Button) findViewById(R.id.category_edit_formula);
    mFormulaEdit.setOnClickListener(mFormulaEditListener);
    setHelpDialog(R.id.category_edit_formula_view, DIALOG_HELP_FORMULA);

    // standard elements:
    mDefaultValueRow = (TableRow) findViewById(R.id.category_edit_default_value_row);
    mDefaultValueText = (EditText) findViewById(R.id.category_edit_default_value);
    setHelpDialog(R.id.category_edit_default_value_view,
        DIALOG_HELP_DEFAULT_VALUE);

    mIncrementRow = (TableRow) findViewById(R.id.category_edit_increment_row);
    mIncrementText = (EditText) findViewById(R.id.category_edit_increment);
    setHelpDialog(R.id.category_edit_increment_view, DIALOG_HELP_INCREMENT);

    mZeroFillRow = (TableRow) findViewById(R.id.category_edit_zerofill_row);
    mZeroFillCheck = (CheckBox) findViewById(R.id.category_edit_zerofill);
    setHelpDialog(R.id.category_edit_zerofill_view, DIALOG_HELP_ZEROFILL);

    mUnitsRow = (TableRow) findViewById(R.id.category_edit_units_row);
    mUnitsText = (EditText) findViewById(R.id.category_edit_units);
    setHelpDialog(R.id.category_edit_units_view, DIALOG_HELP_UNITS);

    mGroupRow = (TableRow) findViewById(R.id.category_edit_group_row);
    mGroupCombo = (ComboBox) findViewById(R.id.category_edit_group);
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

    mHistoryRow = (TableRow) findViewById(R.id.category_edit_history_row);
    mHistoryText = (EditText) findViewById(R.id.category_edit_history);
    setHelpDialog(R.id.category_edit_history_view, DIALOG_HELP_HISTORY);

    mDecimalsRow = (TableRow) findViewById(R.id.category_edit_decimals_row);
    mDecimalsText = (EditText) findViewById(R.id.category_edit_decimals);
    setHelpDialog(R.id.category_edit_decimals_view, DIALOG_HELP_DECIMALS);

    mSmoothingRow = (TableRow) findViewById(R.id.category_edit_smoothing_row);
    mSmoothingText = (EditText) findViewById(R.id.category_edit_smoothing);
    setHelpDialog(R.id.category_edit_smoothing_view, DIALOG_HELP_SMOOTHING);

    mSensitivityRow = (TableRow) findViewById(R.id.category_edit_sensitivity_row);
    mSensitivityText = (EditText) findViewById(R.id.category_edit_sensitivity);
    setHelpDialog(R.id.category_edit_sensitivity_view, DIALOG_HELP_SENSITIVITY);

    mTrendLabelRow = (TableRow) findViewById(R.id.category_edit_trend_row);
      
    // these changes the layout, so needs to be last
    mAdvancedCheck = (CheckBox) findViewById(R.id.category_edit_advanced);
    mAdvancedCheck.setOnCheckedChangeListener(mAdvancedListener);
    mAdvancedCheck.setChecked(false);
    setSyntheticView(false);
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

  private void setZerofillCheckStatus() {
    if (mPeriodSeconds == 0 
        || mAggRadio.getText().toString().toLowerCase().equals(TimeSeries.AGGREGATION_AVG)
        || mAdvancedCheck.isChecked() == false) {
      mZeroFillRow.setVisibility(View.GONE);
    } else {
      mZeroFillRow.setVisibility(View.VISIBLE);
    }
  }
  
  private void setSyntheticView(boolean synthetic) {
    if (synthetic == true) {
      mFormulaRow.setVisibility(View.VISIBLE);
      mDefaultValueRow.setVisibility(View.GONE);
      mIncrementRow.setVisibility(View.GONE);
    } else {
      mFormulaRow.setVisibility(View.GONE);
      mDefaultValueRow.setVisibility(View.VISIBLE);
      mIncrementRow.setVisibility(View.VISIBLE);
    }
    setZerofillCheckStatus();
  }

  private void setAdvancedView(boolean advanced) {
    if (advanced == true) {
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
    if (mRow != null && mRow.mType != null && mRow.mType.toLowerCase().equals(TimeSeries.TYPE_SYNTHETIC))
      setSyntheticView(true);
    else
      setSyntheticView(false);
  }

  private void setupListeners() {
    mAggListener = new RadioGroup.OnCheckedChangeListener() {
      public void onCheckedChanged(RadioGroup group, int checkedId) {
        mAggRadio = (RadioButton) findViewById(checkedId);
        setZerofillCheckStatus();
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
        setZerofillCheckStatus();
        return;
      }

      public void onNothingSelected(AdapterView arg0) {
        return;
      }
    };

    mSeriesTypeListener = new Spinner.OnItemSelectedListener() {
      public void onItemSelected(AdapterView parent, View v, int position,
          long id) {
        String type = ((TextView) v).getText().toString();
        if (type.toLowerCase().equals(TimeSeries.TYPE_SYNTHETIC)) {
          setSyntheticView(true);
        } else {
          setSyntheticView(false);
        }
        return;
      }

      public void onNothingSelected(AdapterView arg0) {
        return;
      }
    };

    mAdvancedListener = new CompoundButton.OnCheckedChangeListener() {
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        setAdvancedView(isChecked);
      }
    };

    mFormulaEditListener = new View.OnClickListener() {
      public void onClick(View view) {
        // TODO: formula-related stuff
        // mSave = true;
        // saveState();
        // mSave = false;
        // Intent i = new Intent(mCtx, FormulaEditorActivity.class);
        // i.putExtra(TimeSeries._ID, mRowId);
        // startActivityForResult(i, FORMULA_EDIT);
      }
    };

    mOkListener = new View.OnClickListener() {
      public void onClick(View view) {
        mSave = true;
        setResult(RESULT_OK);
        finish();
      }
    };

    mDeleteListener = new View.OnClickListener() {
      public void onClick(View view) {
        showDialog(DELETE_DIALOG_ID);
      }
    };
  }

  private void updatePaint(String color) {
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

  private void populateFields() {
    if (mRowId != null && mRow != null) {
      mCategoryText.setText(mRow.mTimeSeriesName);
      mDefaultValueText.setText(Float.valueOf(mRow.mDefaultValue).toString());
      mIncrementText.setText(Float.valueOf(mRow.mIncrement).toString());
      mGoalText.setText(Float.valueOf(mRow.mGoal).toString());
      mSmoothingText.setText(Float.valueOf(mRow.mSmoothing).toString());
      mSensitivityText.setText(Float.valueOf(mRow.mSensitivity).toString());
      mHistoryText.setText(Integer.valueOf(mRow.mHistory).toString());
      mDecimalsText.setText(Integer.valueOf(mRow.mDecimals).toString());
      mUnitsText.setText(mRow.mUnits);

      mColorStr = mRow.mColor;
      mGroupName = mRow.mGroup;
      mGroupCombo.setText(mGroupName);

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
        mAggRadio = (RadioButton) findViewById(R.id.category_edit_agg_sum);
      } else {
        mAggRadio = (RadioButton) findViewById(R.id.category_edit_agg_sum);
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
    saveState();
  }

  @Override
  protected void onResume() {
    super.onResume();
    // TODO: check to see if we need this
    // populateFields();
  }

  private void saveState() {
    if (mSave == true) {
      float f;
      String value;
      ContentValues values = new ContentValues();

      values.put(TimeSeries.TIMESERIES_NAME, mCategoryText.getText().toString());
      values.put(TimeSeries.GROUP_NAME, mGroupCombo.getText().toString());
      f = Number.Round(Float.valueOf(mGoalText.getText().toString()).floatValue(), mRow.mDecimals);
      values.put(TimeSeries.GOAL, f);
      values.put(TimeSeries.COLOR, mColorStr);
      values.put(TimeSeries.PERIOD, mPeriodSeconds);
      values.put(TimeSeries.UNITS, mUnitsText.getText().toString());
      values.put(TimeSeries.ZEROFILL, mZeroFillCheck.isChecked());
      f = Number.Round(Float.valueOf(mSensitivityText.getText().toString()).floatValue(), mRow.mDecimals * 2);
      values.put(TimeSeries.SENSITIVITY, f);
      f = Number.Round(Float.valueOf(mSmoothingText.getText().toString()).floatValue(), mRow.mDecimals * 2);
      values.put(TimeSeries.SMOOTHING, f);
      values.put(TimeSeries.HISTORY, Integer.valueOf(mHistoryText.getText().toString()).intValue());
      values.put(TimeSeries.DECIMALS, Integer.valueOf(mDecimalsText.getText().toString()).intValue());
      values.put(TimeSeries.TYPE, mSeriesTypeSpinner.getSelectedItem().toString());
      values.put(TimeSeries.INTERPOLATION, "");
      // TODO:  formula-related stuff
      values.put(TimeSeries.FORMULA, "");
      
      mAggRadio = (RadioButton) findViewById(mAggRadioGroup.getCheckedRadioButtonId());
      values.put(TimeSeries.AGGREGATION, mAggRadio.getText().toString().toLowerCase());

      if (mSeriesTypeSpinner.getSelectedItem().toString().toLowerCase().equals(TimeSeries.TYPE_SYNTHETIC)) {
        values.put(TimeSeries.DEFAULT_VALUE, 0.0f);
        values.put(TimeSeries.INCREMENT, 1.0f);
      } else {
        f = Number.Round(Float.valueOf(mDefaultValueText.getText().toString()).floatValue(), mRow.mDecimals);
        values.put(TimeSeries.DEFAULT_VALUE, f);
        f = Number.Round(Float.valueOf(mIncrementText.getText().toString()).floatValue(), mRow.mDecimals);
        values.put(TimeSeries.INCREMENT, f);
      }

      if (mRowId == null) {
        // insert
        values.put(TimeSeries.RECORDING_DATAPOINT_ID, 0);
        values.put(TimeSeries.RANK, mMaxRank + 1);
        Uri uri = getContentResolver().insert(TimeSeriesData.TimeSeries.CONTENT_URI, values);
        if (uri != null) {
          String rowIdStr = uri.getPathSegments().get(TimeSeriesProvider.PATH_SEGMENT_TIMERSERIES_ID);
          mRowId = Long.valueOf(rowIdStr);
          setResult(CATEGORY_CREATED);
        } else {
          setResult(CATEGORY_OP_ERR);
        }
      } else {
        // update
        values.put(TimeSeries.RANK, mRow.mRank);
        Uri uri = ContentUris.withAppendedId(TimeSeriesData.TimeSeries.CONTENT_URI, mRowId);
        getContentResolver().update(uri, values, null, null);
        setResult(CATEGORY_MODIFIED);
      }
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    String formula = null;
    // TODO: formula stuff
    // if (intent != null)
    // formula = intent.getStringExtra(FORMULA);
    // if (formula != null)
    // mFormula.setFormula(formula);
    //
    // if (mRowId > 0)
    // mRow = getDbh().fetchCategory(mRowId);
//    populateFields();
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    String title;
    String msg;
    switch (id) {
      case DELETE_DIALOG_ID:
        title = "Delete " + mCategoryText.getText().toString() + "?";
        msg = "All associated entries will also be deleted!";
        return dialog(title, msg);
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

  private Dialog dialog(String title, String msg) {
    Builder b = new AlertDialog.Builder(mCtx);
    b.setTitle(title);
    b.setMessage(msg);
    b.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        Uri timeseries = ContentUris.withAppendedId(
            TimeSeriesData.TimeSeries.CONTENT_URI, mRow.mId);
        mCtx.getContentResolver().delete(timeseries, null, null);
        setResult(CATEGORY_DELETED);
        finish();
      }
    });
    b.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        setResult(RESULT_CANCELED);
      }
    });
    Dialog d = b.create();
    return d;
  }

  private void setHelpDialog(int resId, final int dialog) {
    TextView tv = (TextView) findViewById(resId);
    tv.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        showDialog(dialog);
      }
    });
  }
}
