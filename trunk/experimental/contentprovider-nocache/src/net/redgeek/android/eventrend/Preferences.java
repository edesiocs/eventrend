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

import net.redgeek.android.eventrend.util.DialogUtil;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.method.DigitsKeyListener;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Seriously considering replacing this with a custom preferences screen, adding
 * pop-up help dialogs, etc.
 * 
 * @author barclay
 * 
 */
public class Preferences extends PreferenceActivity {
  private static final int MENU_HELP_ID   = Menu.FIRST;
  
  private static final int HELP_DIALOG_ID = 0;

  public static final String PREFS_NAME = "EvenTrendPrefs";

  public static final String PREFS_DEFAULT_GRAPH_BLACK = "BlackGraphBackground";

  public static final String PREFS_VIEW_DEFAULT = "";
  public static final boolean PREFS_GRAPH_BACKGROUND_BLACK = true;

  private ContentResolver mContent;
  private DialogUtil      mDialogUtil;

  @Override
  protected void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    mDialogUtil = new DialogUtil(this);
    setPreferenceScreen(createPreferenceHierarchy());
  }

  private PreferenceScreen createPreferenceHierarchy() {
    DigitsKeyListener integer = new DigitsKeyListener(false, false);
    DigitsKeyListener decimal = new DigitsKeyListener(false, true);

    // Root
    PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);

    // Data input prefs
    PreferenceCategory dataInput = new PreferenceCategory(this);
    dataInput.setTitle("Data Input");
    root.addPreference(dataInput);

    // Black or white graph background
    CheckBoxPreference defaultGraphBlack = new CheckBoxPreference(this);
    defaultGraphBlack.setKey(PREFS_DEFAULT_GRAPH_BLACK);
    defaultGraphBlack.setTitle("Graph Background");
    defaultGraphBlack
        .setSummary("Change the graph background from black to white.");
    defaultGraphBlack
        .setDefaultValue(new Boolean(PREFS_GRAPH_BACKGROUND_BLACK));
    dataInput.addPreference(defaultGraphBlack);

    return root;
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    boolean result = super.onCreateOptionsMenu(menu);
    menu.add(0, MENU_HELP_ID, 0, R.string.menu_app_help).setIcon(
        android.R.drawable.ic_menu_help);
    return result;
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case MENU_HELP_ID:
        showDialog(HELP_DIALOG_ID);
        return true;
    }
    return super.onOptionsItemSelected(item);
  }
  
  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case HELP_DIALOG_ID:
        String str = getResources().getString(R.string.prefs_overview);
        return mDialogUtil.newOkDialog("Help", str);
    }
    return null;
  }

  public static boolean getDefaultGraphIsBlack(Context ctx) {
    SharedPreferences settings = PreferenceManager
        .getDefaultSharedPreferences(ctx);
    return settings.getBoolean(PREFS_DEFAULT_GRAPH_BLACK, new Boolean(
        PREFS_GRAPH_BACKGROUND_BLACK));
  }
}