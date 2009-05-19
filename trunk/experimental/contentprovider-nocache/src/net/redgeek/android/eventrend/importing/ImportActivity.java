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

package net.redgeek.android.eventrend.importing;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ListView;
import android.widget.TextView;

import net.redgeek.android.eventrend.EvenTrendActivity;
import net.redgeek.android.eventrend.Preferences;
import net.redgeek.android.eventrend.R;
import net.redgeek.android.eventrend.backgroundtasks.ImportTask;
import net.redgeek.android.eventrend.util.GUITaskQueue;
import net.redgeek.android.eventrend.util.ProgressIndicator;

import java.io.File;

/**
 * ImportActivity handles the listing of importable files, spawning the actual
 * importing into a background task, and displaying progress dialogs and
 * results.
 * 
 * <p>
 * Currenlty only supports importing from a pre-defined directory, and only
 * replace-importing, not merge-importing.
 * 
 * @author barclay
 * 
 */
public class ImportActivity extends EvenTrendActivity {
  // Dialogs
  private static final int DIALOG_IMPORT_SUCCESS = 0;
  private static final int DIALOG_ERR_FILEREAD = 1;
  private static final int DIALOG_PROGRESS = 2;

  // UI elements
  private ImportListAdapter mILA;
  private TextView mEmptyList;
  ProgressIndicator.DialogSoft mProgress;

  // Data
  private String mFilename;
  private String mErrMsg;
  private String mImportDir;

  // Tasks
  private ImportTask mImporter;

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    setupTasks();
    setupUI();
    populateFilenameList();
  }

  private void setupTasks() {
    mImporter = new ImportTask(getContentResolver());
  }

  private void setupUI() {
    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    setContentView(R.layout.import_list);

    mProgress = new ProgressIndicator.DialogSoft(mCtx, DIALOG_PROGRESS);
    mImportDir = getResources().getString(R.string.import_dir);
    mEmptyList = (TextView) findViewById(android.R.id.empty);
    mEmptyList.setText("No importable files found in " + mImportDir);
  }

  private void populateFilenameList() {
    File dir = new File(mImportDir);
    File[] files = dir.listFiles();

    if (files == null)
      return;

    mFilename = "";
    mILA = new ImportListAdapter(this);
    for (int i = 0; i < files.length; i++) {
      String size = new String(Long.toString(files[i].length()));
      mILA.addItem(new ImportRow(files[i].getName().toString(), size));
    }

    setListAdapter(mILA);
  }

  @Override
  public void executeNonGuiTask() throws Exception {
    mImporter.doImport();
  }

  @Override
  public void afterExecute() {
    showDialog(DIALOG_IMPORT_SUCCESS);
  }

  @Override
  public void onFailure(Throwable t) {
    mErrMsg = t.getMessage();
    showDialog(DIALOG_ERR_FILEREAD);
  }

  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    super.onListItemClick(l, v, position, id);
    mFilename = ((ImportRowView) v).getFilename();
    mImporter.setFilename(mImportDir + "/" + mFilename);
    GUITaskQueue.getInstance().addTask(mProgress, this);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    populateFilenameList();
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case DIALOG_IMPORT_SUCCESS:
        return mDialogUtil.newOkDialog("Import Success",
            "Imported from " + mFilename);
      case DIALOG_ERR_FILEREAD:
        return mDialogUtil.newOkDialog("Import Failure",
            "Error reading from " + mFilename + ": " + mErrMsg);
      case DIALOG_PROGRESS:
        return mDialogUtil.newProgressDialog(
            "Importing data from " + mFilename + " ...");
      default:
    }
    return null;
  }
}