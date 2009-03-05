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

package net.redgeek.android.eventrend;

import net.redgeek.android.eventrend.db.CategoryDbTable;
import net.redgeek.android.eventrend.db.EvenTrendDbAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

/** Seriously considering replacing this with a custom preferences screen, adding
 * pop-up help dialogs, etc.
 *
 * @author barclay
 *
 */
public class Preferences extends PreferenceActivity {
	public static final String PREFS_NAME = "EvenTrendPrefs";

    public static final String PREFS_DEFAULT_VIEW        = "DefaultView";
    public static final String PREFS_DEFAULT_GRAPH_BLACK = "BlackGraphBackground";
    public static final String PREFS_DEFAULT_TO_LAST     = "DefaultToLast";
    public static final String PREFS_DECIMAL_PLACES      = "DecimalPlaces";
    public static final String PREFS_SMOOTHING_PERCENT   = "SmoothingPercentage";
    public static final String PREFS_HISTORY			 = "History";
    public static final String PREFS_TREND_STDDEV        = "DeviationSensitivity";
    
    public static final String  PREFS_VIEW_DEFAULT = "";
    public static final boolean PREFS_GRAPH_BACKGROUND_BLACK = true;
    public static final boolean PREFS_DEFAULT_TO_LAST_DEFAULT = false;
    public static final int     PREFS_DECIMAL_PLACES_DEFAULT = 2;
    public static final float   PREFS_SMOOTHING_PERCENT_DEFAULT = 0.1f;
    public static final int     PREFS_HISTORY_DEFAULT = 20;
    public static final float   PREFS_TREND_STDDEV_DEFAULT = 1.0f;

	EvenTrendDbAdapter mDbh;
	
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        setPreferenceScreen(createPreferenceHierarchy());
    }

    private PreferenceScreen createPreferenceHierarchy() {
        mDbh = new EvenTrendDbAdapter.SqlAdapter(this);
        mDbh.open();

        // Root
        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);
        
        // Data input prefs
        PreferenceCategory dataInput = new PreferenceCategory(this);
        dataInput.setTitle("Data Input");
        root.addPreference(dataInput);
        
        // default group displayed pref
        ListPreference defaultGroup = new ListPreference(this);        

        Cursor c = mDbh.fetchAllGroups();
        c.moveToFirst();
        String[] values = new String[c.getCount()];
        
        for(int i=0; i < c.getCount(); i++) {
        	String group = c.getString(c.getColumnIndexOrThrow(CategoryDbTable.KEY_GROUP_NAME));
        	values[i] = new String(group);
        	c.moveToNext();
        }
        c.close();

        defaultGroup.setEntries(values);        
        defaultGroup.setEntryValues(values);
        defaultGroup.setDialogTitle("Default Group");
        defaultGroup.setKey(PREFS_DEFAULT_VIEW);
        defaultGroup.setTitle("Default Group");
        defaultGroup.setSummary("Default group to display when launched");
        dataInput.addPreference(defaultGroup);        
        
        // Black or white graph background
        CheckBoxPreference defaultGraphBlack = new CheckBoxPreference(this);
        defaultGraphBlack.setKey(PREFS_DEFAULT_GRAPH_BLACK);
        defaultGraphBlack.setTitle("Graph Background");
        defaultGraphBlack.setSummary("Change the graph background from black to white.");
        defaultGraphBlack.setDefaultValue(new Boolean(PREFS_GRAPH_BACKGROUND_BLACK));
        dataInput.addPreference(defaultGraphBlack);
        
        // Default value or last value
//        CheckBoxPreference defaultIsLastValue = new CheckBoxPreference(this);
//        defaultIsLastValue.setKey(PREFS_DEFAULT_TO_LAST);
//        defaultIsLastValue.setTitle("Default to Last Value");
//        defaultIsLastValue.setSummary("Set the default value to the last value entered");
//        defaultIsLastValue.setDefaultValue(PREFS_DEFAULT_TO_LAST_DEFAULT);
//        dataInput.addPreference(defaultIsLastValue);
        
        // Decimal places
        EditTextPreference decimalPlaces = new EditTextPreference(this);
        decimalPlaces.setDialogTitle("Number of Decimal Places");
        decimalPlaces.setKey(PREFS_DECIMAL_PLACES);
        decimalPlaces.setTitle("Decimal Places");
        decimalPlaces.setSummary("The number of decimal places to round to");
        decimalPlaces.setDefaultValue(new Integer(PREFS_DECIMAL_PLACES_DEFAULT).toString());
        dataInput.addPreference(decimalPlaces);

        // Trending prefs
        PreferenceCategory trendingPrefs = new PreferenceCategory(this);
        trendingPrefs.setTitle("Trending Parameters");
        root.addPreference(trendingPrefs);

        // History
        EditTextPreference history = new EditTextPreference(this);
        history.setDialogTitle("Trending History");
        history.setKey(PREFS_HISTORY);
        history.setTitle("History");
        history.setSummary("The number of datapoints to include in weighted averaging.");
        history.setDefaultValue(new Integer(PREFS_HISTORY_DEFAULT).toString());
        trendingPrefs.addPreference(history);

        // Standard Deviation Sensitivity
        EditTextPreference sensitivity = new EditTextPreference(this);
        sensitivity.setDialogTitle("Deviation Sensitivity");
        sensitivity.setKey(PREFS_TREND_STDDEV);
        sensitivity.setTitle("Standard Deviation Sensitivity");
        sensitivity.setSummary("A scaling influencing trend icons.  Bigger == less sensitive.");
        sensitivity.setDefaultValue(new Float(PREFS_TREND_STDDEV_DEFAULT).toString());
        trendingPrefs.addPreference(sensitivity);

        // Smoothing
        EditTextPreference smoothing = new EditTextPreference(this);
        smoothing.setDialogTitle("Smoothing Constant");
        smoothing.setKey(PREFS_SMOOTHING_PERCENT);
        smoothing.setTitle("Smoothing Percentage");
        smoothing.setSummary("Weight to decay moving average weighting by.");
        smoothing.setDefaultValue(new Float(PREFS_SMOOTHING_PERCENT_DEFAULT).toString());
        trendingPrefs.addPreference(smoothing);
        
        return root;
    }
    
    public static String getDefaultGroup(Context ctx) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
    	return settings.getString(PREFS_DEFAULT_VIEW, PREFS_VIEW_DEFAULT);
    }

    public static boolean getDefaultGraphIsBlack(Context ctx) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);		
		return settings.getBoolean(PREFS_DEFAULT_GRAPH_BLACK, new Boolean(PREFS_GRAPH_BACKGROUND_BLACK));
    }

    public static boolean getDefaultIsLastValue(Context ctx) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
		return settings.getBoolean(PREFS_DEFAULT_TO_LAST, new Boolean(PREFS_DEFAULT_TO_LAST_DEFAULT));
    }

    public static int getDecimalPlaces(Context ctx) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
		String s = settings.getString(PREFS_DECIMAL_PLACES, new Integer(PREFS_DECIMAL_PLACES_DEFAULT).toString());
		return Integer.parseInt(s);
    }

    public static float getSmoothingConstant(Context ctx) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
		String s = settings.getString(PREFS_SMOOTHING_PERCENT, new Float(PREFS_SMOOTHING_PERCENT_DEFAULT).toString());
		return Float.parseFloat(s);
    }

    public static int getHistory(Context ctx) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
		String s = settings.getString(PREFS_HISTORY, new Integer(PREFS_HISTORY_DEFAULT).toString());
		return Integer.parseInt(s);
    }    
    
    public static float getStdDevSensitivity(Context ctx) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
		String s = settings.getString(PREFS_TREND_STDDEV, new Float(PREFS_TREND_STDDEV_DEFAULT).toString());
		return Float.parseFloat(s);
    }    
}