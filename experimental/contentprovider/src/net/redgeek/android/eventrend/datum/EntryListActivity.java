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

import java.util.Calendar;

import net.redgeek.android.eventrecorder.CategoryDbTable;
import net.redgeek.android.eventrend.backgroundtasks.ExportTask;
import net.redgeek.android.eventrend.importing.ImportActivity;
import net.redgeek.android.eventrend.input.EvenTrendActivity;
import net.redgeek.android.eventrend.input.R;
import net.redgeek.android.eventrend.primitives.EntryDbTable;
import net.redgeek.android.eventrend.util.DateUtil;
import net.redgeek.android.eventrend.util.DynamicSpinner;
import net.redgeek.android.eventrend.util.GUITaskQueue;
import net.redgeek.android.eventrend.util.ProgressIndicator;
import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.TableRow.LayoutParams;

public class EntryListActivity extends EvenTrendActivity {
  // Menu
  private static final int MENU_ENTRY_EXPORT_MAIL_ID = Menu.FIRST;
  private static final int MENU_ENTRY_EXPORT_FILE_ID = Menu.FIRST + 1;
  private static final int MENU_ENTRY_IMPORT_REPLACE_ID = Menu.FIRST + 2;
  // private static final int MENU_ENTRY_IMPORT_MERGE_ID = Menu.FIRST + 3;

  // Dialogs
  private static final int DIALOG_EXPORT_SUCCESS = 0;
  private static final int DIALOG_ERR_DIRECTORY = 1;
  private static final int DIALOG_ERR_FILEWRITE = 2;
  private static final int DIALOG_PROGRESS = 3;

  // UI elements
  private EntryListAdapter mEla;
  private LinearLayout mCategoryMenuRow;
  private DynamicSpinner mCategoryMenu;
  private Button mPrevButton;
  private Button mNextButton;
  private TextView mStatus;
  private String mFilename;
  private String mErrMsg;
  ProgressIndicator.DialogSoft mProgress;

  // Listeners
  private OnItemSelectedListener mCategoryMenuListener;
  private View.OnClickListener mPrevButtonListener;
  private View.OnClickListener mNextButtonListener;

  // Private Data
  private long mFilterCategoryId;
  private static final long FILTER_ALL_ID = -1;

  private String mImportDir;
  private int mSkip = 0;

  // Tasks
  private ExportTask mExporter;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setupPrefs();
    setupTasks();
    setupUI();

    fillEntryData();
  }

  private void setupPrefs() {
    mFilterCategoryId = FILTER_ALL_ID;
    mImportDir = getResources().getString(R.string.import_dir);
  }

  private void setupTasks() {
    mExporter = new ExportTask(getDbh());
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

    mPrevButton = (Button) findViewById(R.id.edit_list_prev);
    mPrevButton.setOnClickListener(mPrevButtonListener);

    mNextButton = (Button) findViewById(R.id.edit_list_next);
    mNextButton.setOnClickListener(mNextButtonListener);

    mProgress = new ProgressIndicator.DialogSoft(getCtx(), DIALOG_PROGRESS);
  }

  private void setupListeners() {
    mCategoryMenuListener = new OnItemSelectedListener() {
      public void onItemSelected(AdapterView parent, View v, int position,
          long id) {
        mFilterCategoryId = (long) mCategoryMenu
            .getMappingFromPosition(position);
        fillEntryData();
      }

      public void onNothingSelected(AdapterView arg0) {
        return;
      }
    };

    mPrevButtonListener = new View.OnClickListener() {
      public void onClick(View v) {
        mSkip += EntryDbTable.EDIT_LIMIT;
        fillEntryData();
      }
    };

    mNextButtonListener = new View.OnClickListener() {
      public void onClick(View v) {
        mSkip -= EntryDbTable.EDIT_LIMIT;
        if (mSkip < 0)
          mSkip = 0;
        fillEntryData();
      }
    };
  }

  public void setupMenu() {
    Cursor c = getDbh().fetchAllCategories();
    c.moveToFirst();

    mCategoryMenu.addSpinnerItem("All", FILTER_ALL_ID);
    for (int i = 0; i < c.getCount(); i++) {
      long catId = CategoryDbTable.getId(c);
      String label = CategoryDbTable.getCategoryName(c);
      mCategoryMenu.addSpinnerItem(label, catId);
      c.moveToNext();
    }
    c.close();
  }

  private void fillEntryData() {
    EntryDbTable.Row row;
    String catName;
    Cursor c;

    if (mFilterCategoryId == -1)
      c = getDbh().fetchRecentEntries(EntryDbTable.EDIT_LIMIT, mSkip);
    else
      c = getDbh().fetchRecentEntries(EntryDbTable.EDIT_LIMIT,
          mFilterCategoryId, mSkip);
    startManagingCursor(c);

    String status = "Rows " + mSkip + " - " + (mSkip + EntryDbTable.EDIT_LIMIT);
    mStatus.setText(status);

    mEla = new EntryListAdapter(this);
    c.moveToFirst();
    for (int i = 0; i < c.getCount(); i++) {
      row = new EntryDbTable.Row();
      row.populateFromCursor(c);
      catName = c.getString(c
          .getColumnIndexOrThrow(CategoryDbTable.KEY_CATEGORY_NAME));
      mEla.addItem(new EntryRow(row, catName));
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
    // menu.add(0, MENU_ENTRY_IMPORT_MERGE_ID, 0,
    // R.string.menu_entry_import_merge);
    return result;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case MENU_ENTRY_EXPORT_MAIL_ID:
        exportToMail();
        return true;
      case MENU_ENTRY_EXPORT_FILE_ID:
        exportToFile();
        return true;
      case MENU_ENTRY_IMPORT_REPLACE_ID:
        Intent i = new Intent(this, ImportActivity.class);
        startActivityForResult(i, IMPORT_REPLACE);
        return true;
        // case MENU_ENTRY_IMPORT_MERGE_ID:
        // Intent i = new Intent(this, ImportActivity.class);
        // startActivityForResult(i, IMPORT_MERGE);
        // return true;
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
    i.putExtra(EntryDbTable.KEY_ROWID, rowId);
    startActivityForResult(i, ENTRY_EDIT);
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
        return getDialogUtil().newOkDialog("Export Success",
            "Exported to " + mFilename);
      case DIALOG_ERR_DIRECTORY:
        return getDialogUtil().newOkDialog("Export Failure",
            "Couldn't open/create " + mImportDir);
      case DIALOG_ERR_FILEWRITE:
        return getDialogUtil().newOkDialog("Export Failure",
            "Error writing to " + mFilename + ": " + mErrMsg);
      case DIALOG_PROGRESS:
        return getDialogUtil().newProgressDialog("Exporting database ...");
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