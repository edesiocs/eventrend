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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import net.redgeek.android.eventrend.EvenTrendActivity;
import net.redgeek.android.eventrend.R;
import net.redgeek.android.eventrend.backgroundtasks.ImportTask;
import net.redgeek.android.eventrend.util.GUITask;
import net.redgeek.android.eventrend.util.GUITaskQueue;
import net.redgeek.android.eventrend.util.ProgressIndicator;

import java.io.File;
import java.util.Iterator;

/**
 * ImportActivity handles the listing of importable files, spawning the actual
 * importing into a background task, and displaying progress dialogs and
 * results.
 * 
 * <p>
 * Currently only supports importing from a pre-defined directory, and only
 * replace-importing, not merge-importing.
 * 
 * @author barclay
 * 
 */
public class ImportActivity extends EvenTrendActivity {
  // Dialogs
  private static final int DIALOG_IMPORT_SUCCESS = 0;
  private static final int DIALOG_ERR_FILEREAD = 1;
  private static final int DIALOG_COUNT_PROGRESS = 2;
  private static final int DIALOG_CONFIRM = 3;
  private static final int DIALOG_INSERT_PROGRESS = 4;
  private static final int DIALOG_CONVERT = 5;

  // UI elements
  private ImportListAdapter mILA;
  private TextView mEmptyList;
  private ProgressIndicator.DialogSoft mCountProgress;
  private ProgressIndicator.DialogSoft mInsertProgress;
  private DialogInterface.OnClickListener mConfirmImport;
  private DialogInterface.OnClickListener mCancelImport;
  private Spinner mConversionSpinner;
  private Spinner.OnItemSelectedListener mConversionSpinnerListener;
  private Spinner mConversionUnitsSpinner;
  private Spinner.OnItemSelectedListener mConversionUnitsSpinnerListener;
  private int mEndDialogId;
  private int mEndFailDialogId;
  
  private AlertDialog mImportSuccessDialog;
  private AlertDialog mImportErrDialog;
  private AlertDialog mImportCountDialog;
  private AlertDialog mImportConfirmDialog;
  private ProgressDialog mProgressBarDialog;
  private AlertDialog mImportConvertDialog;

  // Data
  private String mFilename;
  private String mErrMsg;
  private String mImportDir;
  private boolean mConfirmed = false;
  private boolean mConverted = false;
  private String mConvertSeries = "";
  
  // Tasks
  private ImportTask mImporter;
  private Handler mProgressHandler;

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    setupTasks();
    setupUI();
    populateFilenameList();
  }

  private void setupTasks() {
    mProgressHandler = new Handler() {
      @Override
      public void handleMessage(Message msg) {
        int done = msg.getData().getInt("done");
        int total = msg.getData().getInt("total");
        int parsed = msg.getData().getInt("parsed");
        mProgressBarDialog.setMax(total);
        mProgressBarDialog.setProgress(done);
        mProgressBarDialog.setSecondaryProgress(parsed);
        if (done >= total){
          mImporter.mNumRecords = -1;
          dismissDialog(DIALOG_INSERT_PROGRESS);
        }
      }
    };
    mImporter = new ImportTask(getContentResolver(), mProgressHandler); 
  }

  private void setupUI() {
    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    setContentView(R.layout.import_list);

    mCountProgress = new ProgressIndicator.DialogSoft(mCtx, DIALOG_COUNT_PROGRESS);
    mInsertProgress = new ProgressIndicator.DialogSoft(mCtx, DIALOG_INSERT_PROGRESS);
    mImportDir = getResources().getString(R.string.import_dir);
    mEmptyList = (TextView) findViewById(android.R.id.empty);
    mEmptyList.setText("No importable files found in " + mImportDir);
    
    mConfirmImport = new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface arg0, int arg1) {
        mEndDialogId = DIALOG_IMPORT_SUCCESS;
        mEndFailDialogId = DIALOG_ERR_FILEREAD;
        mConfirmed = true;
        
        GUITaskQueue.getInstance().addTask(mInsertProgress, (GUITask)mCtx);
      }
    };

    mCancelImport = new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface arg0, int arg1) {
        mConfirmed = false;
      }
    };
    
    mConversionSpinnerListener = new Spinner.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
          long arg3) {
        int conversion = mConversionSpinner.getSelectedItemPosition();
        mImporter.mCatConversionMap.put(mConvertSeries, conversion);
      }

      @Override
      public void onNothingSelected(AdapterView<?> arg0) {
      }      
    };
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
    if (mConfirmed == false) {
      mImporter.getFileMetaData();
    } else {
      mImporter.doImport();
    }
  }

  @Override
  public void afterExecute() {
    showDialog(mEndDialogId);
    if (mConfirmed == false) {
      if (mImporter.mCatConversionMap.size() > 0) {
        Iterator<String> iterator = mImporter.mCatConversionMap.keySet()
            .iterator();
        for (int i = 0; iterator.hasNext(); i++) {
          mConvertSeries = iterator.next();
          showDialog(DIALOG_CONVERT);
        }
      }
    }    
  }

  @Override
  public void onFailure(Throwable t) {
    mConfirmed = false;
    mErrMsg = t.getMessage();
    showDialog(mEndFailDialogId);
  }

  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    super.onListItemClick(l, v, position, id);
    mFilename = ((ImportRowView) v).getFilename();
    mImporter.setFilename(mImportDir + "/" + mFilename);
    mEndDialogId = DIALOG_CONFIRM;
    mEndFailDialogId = DIALOG_ERR_FILEREAD;
    GUITaskQueue.getInstance().addTask(mCountProgress, this);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    populateFilenameList();
  }

  @Override
  protected void onPrepareDialog(int id, Dialog dialog) {
    switch (id) {
      case DIALOG_IMPORT_SUCCESS:
        mImportSuccessDialog.setMessage("Imported from " + mFilename);
        break;
      case DIALOG_ERR_FILEREAD:
        mImportErrDialog.setMessage("Error reading from " + mFilename + ": " + mErrMsg);
        break;
      case DIALOG_COUNT_PROGRESS:
        mImportCountDialog.setMessage("Gathering data about the contents of " + mFilename + " ...");
        break;
      case DIALOG_CONFIRM:
        mImportConfirmDialog.setMessage("Are you sure you want to replace the contents of the database " 
            + "with the ~ " + mImporter.mNumRecords + " entries from " 
            + mFilename + "? This may take a while, and is cannot be undone.");
        break;
      case DIALOG_INSERT_PROGRESS:
        mProgressBarDialog.setMessage("Importing data from " + mFilename + " ...");        
        break;
      case DIALOG_CONVERT:
        mImportConvertDialog.setTitle(mConvertSeries);
        TextView text = (TextView) mImportConvertDialog.findViewById(R.id.message);
        text.setText("It looks like you're importing an old file format.  Please" +
        		"select a conversion for the category \"" + mConvertSeries + "\"");        
        break;
      default:
    }
  }
  
  @Override
  protected Dialog onCreateDialog(int id) {
    Dialog d;
    String title, msg;
    switch (id) {
      case DIALOG_IMPORT_SUCCESS:
        mImportSuccessDialog = mDialogUtil.newOkDialog("Import Success", "");
        mConfirmed = false;
        return mImportSuccessDialog;
      case DIALOG_ERR_FILEREAD:
        mImportErrDialog = mDialogUtil.newOkDialog("Import Failure", "");
        mConfirmed = false;
        return mImportErrDialog;
      case DIALOG_COUNT_PROGRESS:
        mImportCountDialog = mDialogUtil.newProgressDialog("");
        return mImportCountDialog;
      case DIALOG_CONFIRM:
        title = "Import Confirmation";
        msg = "";
        mImportConfirmDialog = mDialogUtil.newOkCancelDialog(title, msg, mConfirmImport, mCancelImport);
        return mImportConfirmDialog;
      case DIALOG_INSERT_PROGRESS:
        title = "Import Progress";
        msg = "Importing data from " + mFilename + " ...";
        mProgressBarDialog = mDialogUtil.newProgressBarDialog(title, msg);
        return mProgressBarDialog;
      case DIALOG_CONVERT:
        LayoutInflater inflater = (LayoutInflater) mCtx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.dialog_series_conversion, null);
        
        mConversionSpinner = (Spinner) layout.findViewById(R.id.menu);
        ArrayAdapter<CharSequence> conversions = ArrayAdapter.createFromResource(mCtx, R.array.import_conversion_types,
          android.R.layout.simple_spinner_dropdown_item);
        mConversionSpinner.setAdapter(conversions);
        mConversionSpinner.setOnItemSelectedListener(mConversionSpinnerListener);

        mConversionUnitsSpinner = (Spinner) layout.findViewById(R.id.units);
        ArrayAdapter<CharSequence> units = ArrayAdapter.createFromResource(mCtx, R.array.import_conversion_units,
          android.R.layout.simple_spinner_dropdown_item);
        mConversionUnitsSpinner.setAdapter(units);
        mConversionUnitsSpinner.setOnItemSelectedListener(mConversionUnitsSpinnerListener);

        Builder builder = new AlertDialog.Builder(mCtx);
        builder.setTitle("Conversion");
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            return;
          }});
        builder.setView(layout);

        mImportConvertDialog = builder.create();
        return mImportConvertDialog;
      default:
    }
    return null;
  }
}