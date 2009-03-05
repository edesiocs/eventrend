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

package net.redgeek.android.eventrend.synthetic;

import net.redgeek.android.eventrend.EvenTrendActivity;
import net.redgeek.android.eventrend.R;
import net.redgeek.android.eventrend.db.CategoryDbTable;
import android.app.Dialog;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class FormulaEditorActivity extends EvenTrendActivity {
    private static final int FORMULA_HELP_MENU_ITEM = Menu.FIRST;

    private static final int HELP_DIALOG         = 0;
    private static final int PARSE_ERROR_DIALOG  = 1;

	// UI elements
	private EditText mPage;
	private Button   mSaveButton;
	private Button   mCancelButton;
	
	// Listeners
    private View.OnClickListener   mSaveListener;
    private View.OnClickListener   mCancelListener;

	// private data
    private Formula mFormula;
	private long    mCatId;
	private boolean mSave;
	
    @Override
	public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.formula_edit);
        
        setupData(icicle);
        setupUI();
        populateFields();
    }
    
    private void setupData(Bundle icicle) {
    	mCatId = -1;
		if (icicle != null) {
			mCatId = icicle.getLong(CATEGORY_ID);
		}
		if (mCatId < 0) {
			Bundle extras = getIntent().getExtras();       
			if (extras != null) {
				mCatId = extras.getLong(CATEGORY_ID);
			}
		}
		
		mSave = false;
		mFormula = new Formula();
    }
    
    private void setupUI() {
    	setupListeners();    	

    	mSaveButton = (Button) findViewById(R.id.formula_save);
    	mSaveButton.setOnClickListener(mSaveListener);
    	
    	mCancelButton = (Button) findViewById(R.id.formula_cancel);
    	mCancelButton.setOnClickListener(mCancelListener);

    	mPage = (EditText) findViewById(R.id.formula_edit);
    }

    private void setupListeners() {
        mSaveListener = new View.OnClickListener() {
        	public void onClick(View view) {
        		// TODO:  try parsing, check result
        		mFormula.setFormula(mPage.getText().toString());
        		if (mFormula.isValid()) {        		
        			mSave = true;
        			setResult(RESULT_OK);
        			finish();
        		}
        		else {
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
    
    private void populateFields() {
    	CategoryDbTable.Row row = getDbh().fetchCategory(mCatId);
    	mPage.setText(row.getFormula());
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	boolean result = super.onCreateOptionsMenu(menu);
    	menu.add(0, FORMULA_HELP_MENU_ITEM, 0, R.string.menu_app_help).setIcon(R.drawable.help);
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
        	return getDialogUtil().newOkDialog("Help", str);
        case PARSE_ERROR_DIALOG:
        	str = mFormula.getErrorString();
        	return getDialogUtil().newOkDialog("Parse Eror", str);
        default:
        }
        return null;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	outState.putLong(CATEGORY_ID, mCatId);
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
    		CategoryDbTable.Row cat = getDbh().fetchCategory(mCatId);
    		cat.setFormula(mFormula.toString());
    		getDbh().updateCategory(cat);
//    		mFormula.parseString(mPage.getText().toString());
//    		String s = mFormula.toString();
//    		getIntent().putExtra(FORMULA, s);
    	}
    }
}
