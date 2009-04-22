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

package net.redgeek.android.eventrend.util;

import net.redgeek.android.eventrend.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Map;

/**
 * Implements a "ComboBox", that is, an EditText box coupled with an "pop-up"
 * menu of values that will populate the EditText if selected.
 * 
 * @author barclay
 */
public class ComboBox extends LinearLayout {
  private Context mCtx;
  private LinearLayout mLayout;
  private EditText mEditText;
  private ImageButton mButton;
  private Drawable mButtonIcon;
  private Dialog mMenu;
  private ArrayAdapter<String> mAdapter;
  private View.OnClickListener mButtonListener;

  /**
   * Constructor. Creates an empty ComboBox associated with the context of
   * <code>context</code>.
   * 
   * @param context
   *          The Context responsible for this view.
   */
  public ComboBox(Context context) {
    super(context);
    setup(context);
  }

  /**
   * Constructor. Creates an empty ComboBox associated with the context of
   * <code>context</code>. <br>
   * <strong>attrs is currently ignored and only present to satisfy the same
   * constructor set as an EditText.</strong>
   * 
   * @param context
   *          The Context responsible for this view.
   * @param attrs
   *          (Ignored.)
   */
  public ComboBox(Context context, AttributeSet attrs) {
    super(context, attrs);
    setup(context);
  }

  /**
   * Initialization common to all constructors. Stores away the context,
   * allocates various member variables, sets up the UI elements of the view,
   * creates the popup menu (Dialog), and establishes a listener for the menu.
   * 
   * @param context
   *          The context associated with this view.
   */
  private void setup(Context context) {
    mCtx = context;

    LayoutInflater inflater = (LayoutInflater) context
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    View inflatedView = inflater.inflate(R.layout.combobox, this);

    mButtonListener = new OnClickListener() {
      public void onClick(View v) {
        mMenu.show();
      }
    };

    mButtonIcon = getResources().getDrawable(
        android.R.drawable.arrow_down_float);
    mButton = (ImageButton) findViewById(R.id.combobox_button);
    mButton.setImageDrawable(mButtonIcon);

    mAdapter = new ArrayAdapter<String>(mCtx,
        android.R.layout.select_dialog_singlechoice);

    mMenu = dialog();
    mButton.setOnClickListener(mButtonListener);
  }

  /**
   * Handler for dialog creation.
   * 
   * @param id
   *          (ignored)
   * @return A Dialog object.
   */
  protected Dialog onCreateDialog(int id) {
    return mMenu;
  }

  /**
   * Dialog (menu) creation. Save the Dialog internally.
   * 
   */
  private Dialog dialog() {
    Builder b = new AlertDialog.Builder(mCtx);
    b.setAdapter(mAdapter, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        mEditText.setText(mAdapter.getItem(whichButton));
      }
    });
    Dialog d = b.create();
    return d;
  }

  /**
   * Sets the default value of the text in the EditText portion of the ComboBox
   * 
   * @param text
   *          The text to set in the EditText view.
   */
  public void setDefaultValue(String text) {
    setText(text);
  }

  /**
   * Adds an item to the popup menu
   * 
   * @param text
   *          The text to add
   */
  public void addMenuItem(String text) {
    mAdapter.add(text);
  }

  /**
   * Clears all menu items from the popup menu.
   */
  public void clearMenuItems() {
    mAdapter.clear();
  }

  /**
   * Calls extendSelection(int index) on the EditText portion of the ComboBox
   * 
   * @see EditText#extendSelection(int)
   */
  public void extendSelection(int index) {
    mEditText.extendSelection(index);
  }

  /**
   * Calls getText() on the EditText portion of the ComboBox
   * 
   * @see EditText#getText()
   */
  public Editable getText() {
    return mEditText.getText();
  }

  /**
   * Calls selectAll() on the EditText portion of the ComboBox
   * 
   * @see EditText#selectAll()
   */
  public void selectAll() {
    mEditText.selectAll();
  }

  /**
   * Calls setSelection(int start, int stop) on the EditText portion of the
   * ComboBox
   * 
   * @see EditText#setSelection(int, int)
   */
  public void setSelection(int start, int stop) {
    mEditText.setSelection(start, stop);
  }

  /**
   * Calls setSelection(int index) on the EditText portion of the ComboBox
   * 
   * @see EditText#setSelection(int)
   */
  public void setSelection(int index) {
    mEditText.setSelection(index);
  }

  /**
   * Calls setText(CharSequence text, TextView.BufferType type) on the EditText
   * portion of the ComboBox
   * 
   * @see EditText#setText(CharSequence, TextView.BufferType)
   */
  public void setText(CharSequence text, TextView.BufferType type) {
    mEditText.setText(text, type);
  }

  /**
   * Calls setText(String text) on the EditText portion of the ComboBox
   * 
   * @see EditText#setText(String)
   */
  public void setText(String text) {
    mEditText.setText(text);
  }
}
