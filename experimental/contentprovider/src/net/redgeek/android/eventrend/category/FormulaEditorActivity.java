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
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import net.redgeek.android.eventrecorder.TimeSeriesData;
import net.redgeek.android.eventrecorder.TimeSeriesData.TimeSeries;
import net.redgeek.android.eventrecorder.synthetic.Formula;
import net.redgeek.android.eventrecorder.synthetic.Tokenizer;
import net.redgeek.android.eventrend.EvenTrendActivity;
import net.redgeek.android.eventrend.R;
import net.redgeek.android.eventrend.util.DynamicSpinner;

public class FormulaEditorActivity extends EvenTrendActivity {
  private static final int FORMULA_HELP_MENU_ITEM = Menu.FIRST;

  private static final int HELP_DIALOG = 0;
  private static final int PARSE_ERROR_DIALOG = 1;

  // UI elements
  private EditText mPage;
  private DynamicSpinner mAddSeries;
  private Spinner mAddOperator;
  private DynamicSpinner mAddConstant;
  private Button mAddGroupButton;
  private Button mClearButton;
  private Button mSaveButton;
  private Button mCancelButton;

  // Listeners
  private Spinner.OnItemSelectedListener mAddSeriesListener;
  private Spinner.OnItemSelectedListener mAddOperatorListener;
  private Spinner.OnItemSelectedListener mAddConstantListener;
  private View.OnClickListener mAddGroupListener;
  private View.OnClickListener mClearListener;
  private View.OnClickListener mSaveListener;
  private View.OnClickListener mCancelListener;

  // private data
  private Formula mFormula;
  private long mCatId;
  private boolean mSave;
  private String mText;

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    setContentView(R.layout.formula_edit);

    setupData(icicle);
    setupUI();
    populateFields();
  }

  private void setupData(Bundle icicle) {
    mText = new String("");
    mCatId = -1;
    if (icicle != null) {
      mCatId = icicle.getLong(TimeSeries._ID);
      if (icicle.getString(TimeSeries.FORMULA) != null)
        mText = new String(icicle.getString(TimeSeries.FORMULA));
    }
    if (mCatId < 0) {
      Bundle extras = getIntent().getExtras();
      if (extras != null) {
        mCatId = extras.getLong(TimeSeries._ID);
        if (mText.equals(""))
          if (extras.getString(TimeSeries.FORMULA) != null)
            mText = new String(extras.getString(TimeSeries.FORMULA));
      }
    }

    mSave = false;
    mFormula = new Formula();
  }

  private void setupUI() {
    setupListeners();
    
    LinearLayout addMenuLayout = (LinearLayout) findViewById(R.id.formula_add_series_layout);
    
    mAddSeries = new DynamicSpinner(mCtx);
    mAddSeries.setPrompt("Insert Category");
    addMenuLayout.addView(mAddSeries, new LinearLayout.LayoutParams(
        LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    mAddSeries.addSpinnerItem("(Series)", new Long(0));
    
    Uri timeSeries = TimeSeriesData.TimeSeries.CONTENT_URI;
    Cursor c = managedQuery(timeSeries, null, null, null, null);
    if (c.moveToFirst()) {
      int count = c.getCount();
      for (int i = 0; i < count; i++) {
        long id = TimeSeries.getId(c);
        String name = TimeSeries.getTimeSeriesName(c);
        mAddSeries.addSpinnerItem(name, new Long(id));
        c.moveToNext();
      }
      c.close();
    }
    mAddSeries.setSelected(false);
    mAddSeries.setOnItemSelectedListener(mAddSeriesListener);
    
    mAddOperator = (Spinner) findViewById(R.id.formula_operator_menu);
    mAddOperator.setPrompt("Insert Operator");
    ArrayAdapter adapter = ArrayAdapter.createFromResource(
            this, R.array.formula_operators, android.R.layout.simple_spinner_item);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    mAddOperator.setAdapter(adapter);
    mAddOperator.setOnItemSelectedListener(mAddOperatorListener);
    
    mAddConstant = new DynamicSpinner(mCtx);
    mAddConstant.setPrompt("Insert Contant");
    LinearLayout addConstantLayout = (LinearLayout) findViewById(R.id.formula_add_constant_layout);
    addConstantLayout.addView(mAddConstant,
        new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
            LayoutParams.WRAP_CONTENT));
    mAddConstant.addSpinnerItem("(Constant)", new Long(0));
    for (int i = 0; i < Tokenizer.PERIODS.length; i++) {
      mAddConstant.addSpinnerItem(Tokenizer.PERIODS[i], new Long(i));
    }
    mAddConstant.setOnItemSelectedListener(mAddConstantListener);

    mAddGroupButton = (Button) findViewById(R.id.formula_add_group);
    mAddGroupButton.setOnClickListener(mAddGroupListener);    

    mClearButton = (Button) findViewById(R.id.formula_clear);
    mClearButton.setOnClickListener(mClearListener);    
    
    mSaveButton = (Button) findViewById(R.id.formula_save);
    mSaveButton.setOnClickListener(mSaveListener);

    mCancelButton = (Button) findViewById(R.id.formula_cancel);
    mCancelButton.setOnClickListener(mCancelListener);

    mPage = (EditText) findViewById(R.id.formula_edit);
    mPage.setText(mText);
  }

  private void setupListeners() {
    mAddSeriesListener = new Spinner.OnItemSelectedListener() {
      public void onItemSelected(AdapterView parent, View v, int position,
          long id) {
        if (position != 0) {
          int cursor = mPage.getSelectionStart();
          String series = Tokenizer.escape(((TextView) v).getText().toString());
          mPage.getText().insert(cursor, "series \"" + series + "\"");
        }
        mAddSeries.setSelection(0);
        return;
      }

      public void onNothingSelected(AdapterView arg0) {
        return;
      }
    };

    mAddOperatorListener = new Spinner.OnItemSelectedListener() {
      public void onItemSelected(AdapterView parent, View v, int position,
          long id) {
        if (position != 0) {
          int cursor = mPage.getSelectionStart();
          String operator = ((TextView) v).getText().toString();
          mPage.getText().insert(cursor, " " + operator + " ");
        }
        mAddOperator.setSelection(0);
        return;
      }

      public void onNothingSelected(AdapterView arg0) {
        return;
      }
    };
    
    mAddConstantListener = new Spinner.OnItemSelectedListener() {
      public void onItemSelected(AdapterView parent, View v, int position,
          long id) {
        if (position != 0) {
          int cursor = mPage.getSelectionStart();
          String constant = ((TextView) v).getText().toString();
          mPage.getText().insert(cursor, constant);
        }
        mAddSeries.setSelection(0);
        return;
      }

      public void onNothingSelected(AdapterView arg0) {
        return;
      }
    };
    
    mAddGroupListener = new View.OnClickListener() {
      public void onClick(View view) {
        int cursor = mPage.getSelectionStart();
        mPage.getText().insert(cursor, "( )");
      }
    };

    mClearListener = new View.OnClickListener() {
      public void onClick(View view) {
        mPage.getText().clear();
      }
    };

    mSaveListener = new View.OnClickListener() {
      public void onClick(View view) {
        // TODO: try parsing, check result
        mFormula.setFormula(mPage.getText().toString());
        if (mFormula.isValid()) {
          mSave = true;
          setResult(RESULT_OK);
          finish();
        } else {
          showDialog(PARSE_ERROR_DIALOG);
        }
      }
    };

    mCancelListener = new View.OnClickListener() {
      public void onClick(View view) {
        finish();
      }
    };
  }

  public void setupMenu() {
  }
  
  private String escape(String escape) {
    String escapedName = "";
    for (int i = 0; i < escape.length(); i++) {
      char c = escape.charAt(i);
      if (c == '\\' || c == '"') {
        escapedName += '\\';
      }
      escapedName += c;
    }
    return escapedName;
  }

  private void populateFields() {
    String[] projection = new String[] { TimeSeries.FORMULA };
    Uri timeseries = ContentUris.withAppendedId(
        TimeSeriesData.TimeSeries.CONTENT_URI, mCatId);
    Cursor c = getContentResolver().query(timeseries, projection, null, null, null);
    if (c.getCount() < 1) {
      c.close();
      return;
    }

    c.moveToFirst();
    String formula = TimeSeries.getFormula(c);
    c.close();

    if (mText.equals(""))
      mPage.setText(formula);
    else
      mPage.setText(mText);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    boolean result = super.onCreateOptionsMenu(menu);
    menu.add(0, FORMULA_HELP_MENU_ITEM, 0, R.string.menu_app_help).setIcon(
        R.drawable.help);
    return result;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case FORMULA_HELP_MENU_ITEM:
        showDialog(HELP_DIALOG);
        return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    String str;
    switch (id) {
      case HELP_DIALOG:
        str = getResources().getString(R.string.formula_help);
        return mDialogUtil.newOkDialog("Help", str);
      case PARSE_ERROR_DIALOG:
        str = mFormula.getErrorString();
        return mDialogUtil.newOkDialog("Parse Eror", str);
      default:
    }
    return null;
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    outState.putLong(TimeSeries._ID, mCatId);
    outState.putString(TimeSeries.FORMULA, mPage.getText().toString());
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
    populateFields();
  }
  
  private void saveState() {
    if (mSave == true) {
      ContentValues values = new ContentValues();
      values.put(TimeSeries.FORMULA, mFormula.toString());
      Uri timeseries = ContentUris.withAppendedId(
          TimeSeriesData.TimeSeries.CONTENT_URI, mCatId);
      getContentResolver().update(timeseries, values, null, null);
    }
  }
}
