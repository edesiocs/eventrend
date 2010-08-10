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

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.redgeek.android.eventrend.util.Number;

/**
 * Implements the view of each row in the ListView of importable content. Each
 * row is currently jsut a filename and filesize. Could certainly be made much
 * prettier.
 * 
 * @author barclay
 */
public class ImportRowView extends LinearLayout {
  private TextView mFilename;
  private TextView mSize;
  private Button   mImport;
  private Context mCtx;
  private boolean mSelectable = true;

  public ImportRowView(Context context, ImportRow aRow) {
    super(context);
    mCtx = context;

    setupUI();
    populateFields(aRow);
  }

  private void setupUI() {
    this.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
    // this.setLongClickable(true);

    mFilename = new TextView(mCtx);
    mFilename.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
    addView(mFilename, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
        LayoutParams.WRAP_CONTENT));

    mSize = new TextView(mCtx);
    mSize.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
    addView(mSize, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
        LayoutParams.WRAP_CONTENT));
    
    mImport = new Button(mCtx);
    mImport.setText("Import");
    mImport.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
    addView(mImport, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
        LayoutParams.WRAP_CONTENT));
    mImport.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        ((ImportActivity)mCtx).doImport((String) mFilename.getText());
      }
    });
  }

  private void populateFields(ImportRow row) {
    mFilename.setText(row.getFilename());
    long bytes = Long.valueOf(row.getSize()).longValue();
    if (bytes < 1024) {
      mSize.setText(": " + bytes + " Bytes");
    } else {
      float size = ((float) bytes) / 1024.0f;
      if (size < 1024)
        mSize.setText(": " + Number.Round(size) + " KBytes");
      else
        mSize.setText(": " + Number.Round(size / 1024.0f) + " MBytes");
    }
  }

  public boolean isSelectable() {
    return mSelectable;
  }

  public void setSelectable(boolean selectable) {
    mSelectable = selectable;
  }

  public void setFilename(String filename) {
    mFilename.setText(filename);
  }

  public String getFilename() {
    return mFilename.getText().toString();
  }

  public void setSize(String size) {
    mSize.setText(size);
  }
}
