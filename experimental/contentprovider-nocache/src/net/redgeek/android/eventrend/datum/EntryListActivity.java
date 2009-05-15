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

package net.redgeek.android.eventrend.datum;

import android.app.Dialog;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

import net.redgeek.android.eventrecorder.TimeSeriesData;
import net.redgeek.android.eventrecorder.TimeSeriesData.Datapoint;
import net.redgeek.android.eventrecorder.TimeSeriesData.TimeSeries;
import net.redgeek.android.eventrend.EvenTrendActivity;
import net.redgeek.android.eventrend.R;
import net.redgeek.android.eventrend.backgroundtasks.ExportTask;
import net.redgeek.android.eventrend.importing.ImportActivity;
import net.redgeek.android.eventrend.util.DateUtil;
import net.redgeek.android.eventrend.util.DynamicSpinner;
import net.redgeek.android.eventrend.util.GUITaskQueue;
import net.redgeek.android.eventrend.util.ProgressIndicator;

import java.util.Calendar;
import java.util.HashMap;

public class EntryListActivity extends EvenTrendActivity {
  // Menu
  private static final int MENU_ENTRY_EXPORT_MAIL_ID = Menu.FIRST;
  private static final int MENU_ENTRY_EXPORT_FILE_ID = Menu.FIRST + 1;
  private static final int MENU_ENTRY_IMPORT_REPLACE_ID = Menu.FIRST + 2;
  private static final int MENU_ENTRY_IMPORT_MERGE_ID = Menu.FIRST + 3;

  // Dialogs
  private static final int DIALOG_EXPORT_SUCCESS = 0;
  private static final int DIALOG_ERR_DIRECTORY = 1;
  private static final int DIALOG_ERR_FILEWRITE = 2;
  private static final int DIALOG_PROGRESS = 3;

  // UI elements
  private EntryListAdapter mEla;
  private LinearLayout mCategoryMenuRow;
  private DynamicSpinner mCategoryMenu;
  private TextView mStatus;
  private String mFilename;
  private String mErrMsg;
  ProgressIndicator.DialogSoft mProgress;

  // Listeners
  private OnItemSelectedListener mCategoryMenuListener;

  // Private Data
  private long mCatId;
  private String mCatName;
  private HashMap<Long, String> mIdMap;
  
  private String mImportDir;

  // Tasks
  private ExportTask mExporter;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setupPrefs();
    setupTasksAndData();
    setupUI();

    fillEntryData();
  }

  private void setupPrefs() {
    mImportDir = getResources().getString(R.string.import_dir);
  }

  private void setupTasksAndData() {
    // TODO:
    //    mExporter = new ExportTask(getDbh());
    mIdMap = new HashMap<Long, String>();
  }

  private void setupUI() {
    setupListeners();

    setContentView(R.layout.entry_list);

    mCategoryMenuRow = (LinearLayout) findViewById(R.id.entry_list_category_menu_row);
    mCategoryMenu = (DynamicSpinner) new DynamicSpinner(this);
    mCategoryMenuRow.addView(mCategoryMenu, new LinearLayout.LayoutParams(
        LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    mCategoryMenuRow.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
    mCategoryMenu.setOnItemSelectedListener(mCategoryMenuListener);
    setupMenu();

    mStatus = (TextView) findViewById(R.id.edit_list_status);

    mProgress = new ProgressIndicator.DialogSoft(mCtx, DIALOG_PROGRESS);
  }

  private void setupListeners() {
    mCategoryMenuListener = new OnItemSelectedListener() {
      public void onItemSelected(AdapterView parent, View v, int position,
          long id) {
        mCatId = (long) mCategoryMenu.getMappingFromPosition(position);
        mCatName = mIdMap.get(mCatId);
        fillEntryData();
      }

      public void onNothingSelected(AdapterView arg0) {
        return;
      }
    };
  }

  
  public void setupMenu() {
    String[] projection = new String[] { 
        TimeSeries._ID, TimeSeries.TIMESERIES_NAME };
    Uri timeseries = TimeSeriesData.TimeSeries.CONTENT_URI;
    Cursor c = getContentResolver().query(timeseries, projection, null, null,
          TimeSeries.TIMESERIES_NAME + " asc ");
    if (c != null) {
      String name;
      int count = c.getCount();
      c.moveToFirst();
      for (int i = 0; i < count; i++) {
        long catId = TimeSeries.getId(c);
        String label = TimeSeries.getTimeSeriesName(c);
        mCategoryMenu.addSpinnerItem(label, catId);
        mIdMap.put(Long.valueOf(catId), label);
        c.moveToNext();
      }
      c.close();
    }
  }
  
  private void fillEntryData() {
    EntryRow row;
    String catName;

    Uri uri = ContentUris.withAppendedId(
        TimeSeriesData.TimeSeries.CONTENT_URI, mCatId).buildUpon()
        .appendPath("recent").appendPath(""+Integer.MAX_VALUE).build();
    Cursor c = mCtx.getContentResolver().query(uri, null, null, null, 
      Datapoint.TS_START + " desc ");
    
    mEla = new EntryListAdapter(this);
    c.moveToFirst();
    for (int i = 0; i < c.getCount(); i++) {
      row = new EntryRow(mCatName);
      row.mId = Datapoint.getId(c);
      row.mTimeSeriesId = Datapoint.getTimeSeriesId(c);
      row.mValue = Datapoint.getValue(c);
      row.mEntries = Datapoint.getEntries(c);
      row.mTsStart = Datapoint.getTsStart(c);
      row.mTsEnd = Datapoint.getTsEnd(c);
      row.mName = "";
      
      mEla.addItem(row);
      c.moveToNext();
    }

    setListAdapter(mEla);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    boolean result = super.onCreateOptionsMenu(menu);
    menu.add(0, MENU_ENTRY_EXPORT_MAIL_ID, 0, R.string.menu_entry_export_mail)
        .setIcon(android.R.drawable.ic_menu_send);
    menu.add(0, MENU_ENTRY_EXPORT_FILE_ID, 0, R.string.menu_entry_export_file)
        .setIcon(android.R.drawable.ic_menu_save);
    menu.add(0, MENU_ENTRY_IMPORT_REPLACE_ID, 0,
        R.string.menu_entry_import_replace).setIcon(
        android.R.drawable.ic_menu_revert);
    // TODO: find better icon:
    menu.add(0, MENU_ENTRY_IMPORT_MERGE_ID, 0,
        R.string.menu_entry_import_merge).setIcon(
            android.R.drawable.ic_menu_revert);
    return result;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    Intent i;
    switch (item.getItemId()) {
      case MENU_ENTRY_EXPORT_MAIL_ID:
        exportToMail();
        return true;
      case MENU_ENTRY_EXPORT_FILE_ID:
        exportToFile();
        return true;
      case MENU_ENTRY_IMPORT_REPLACE_ID:
        i = new Intent(this, ImportActivity.class);
        startActivityForResult(i, ARC_IMPORT_REPLACE);
        return true;
      case MENU_ENTRY_IMPORT_MERGE_ID:
        i = new Intent(this, ImportActivity.class);
        startActivityForResult(i, ARC_IMPORT_MERGE);
        return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void executeNonGuiTask() throws Exception {
    mExporter.doExport();
  }

  @Override
  public void afterExecute() {
    if (mExporter.mToFile == true) {
      showDialog(DIALOG_EXPORT_SUCCESS);
    } else {
      Intent sendIntent = new Intent(Intent.ACTION_SEND);
      sendIntent.putExtra(Intent.EXTRA_SUBJECT, mExporter.mSubject);
      sendIntent.putExtra(Intent.EXTRA_TEXT, mExporter.mBody);
      sendIntent.setType("text/plain");
      startActivity(Intent.createChooser(sendIntent, "Email Backup"));
    }
  }

  @Override
  public void onFailure(Throwable t) {
    mErrMsg = t.getMessage();
    showDialog(DIALOG_ERR_FILEWRITE);
  }

  private void exportToMail() {
    Calendar cal = Calendar.getInstance();
    String prettyDate = DateUtil.toTimestamp(cal);
    String appName = getResources().getString(R.string.app_name);

    mExporter.setSubject(appName + " backup " + prettyDate);
    mExporter.setToFile(false);
    GUITaskQueue.getInstance().addTask(mProgress, this);
  }

  private void exportToFile() {
    Calendar cal = Calendar.getInstance();
    String safeDate = DateUtil.toFSTimestamp(cal);

    mFilename = safeDate + ".csv";
    mExporter.setDirectory(mImportDir);
    mExporter.setFilename(mImportDir + "/" + mFilename);
    mExporter.setToFile(true);
    GUITaskQueue.getInstance().addTask(mProgress, this);
  }

  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    super.onListItemClick(l, v, position, id);
    long rowId = ((EntryRowView) v).getRowId();
    Intent i = new Intent(this, EntryEditActivity.class);
    i.putExtra(Datapoint._ID, rowId);
    startActivityForResult(i, ARC_ENTRY_EDIT);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    fillEntryData();
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case DIALOG_EXPORT_SUCCESS:
        return mDialogUtil.newOkDialog("Export Success",
            "Exported to " + mFilename);
      case DIALOG_ERR_DIRECTORY:
        return mDialogUtil.newOkDialog("Export Failure",
            "Couldn't open/create " + mImportDir);
      case DIALOG_ERR_FILEWRITE:
        return mDialogUtil.newOkDialog("Export Failure",
            "Error writing to " + mFilename + ": " + mErrMsg);
      case DIALOG_PROGRESS:
        return mDialogUtil.newProgressDialog("Exporting database ...");
      default:
    }
    return null;
  }

  @Override
  protected void onPause() {
    super.onPause();
  }

  @Override
  protected void onResume() {
    super.onResume();
    fillEntryData();
  }
}