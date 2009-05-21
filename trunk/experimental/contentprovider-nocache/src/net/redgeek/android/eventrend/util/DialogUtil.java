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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import net.redgeek.android.eventrend.R;

/**
 * A factor for generating commonly generated dialogs
 * 
 * @author barclay
 */
public class DialogUtil {
  private Context mCtx;
  private Builder mBuilder;

  /**
   * A generic listener that does nothing which can be shared by many dialogs.
   */
  DialogInterface.OnClickListener mDoNothing = new DialogInterface.OnClickListener() {
    public void onClick(DialogInterface dialog, int which) {
      return;
    }
  };

  /**
   * Constructor, instantiates the factory, which will be associated with the
   * context passed in.
   * 
   * @param ctx
   *          The context to associate with the factory.
   */
  public DialogUtil(Context context) {
    mCtx = context;
    mBuilder = new AlertDialog.Builder(mCtx);
  }

  /**
   * Returns the context associated with the factory.
   * 
   * @return Context
   */
  public Context getContext() {
    return mCtx;
  }

  /**
   * Create a new Dialog the the specified view and message.
   * 
   * @param message
   *          The message to display in the dialog.
   * @param v
   *          The View to add to the dialog.
   * @return The Dialog.
   */
  public Dialog newViewDialog(String message, View v) {
    mBuilder.setMessage(message);
    Dialog d = mBuilder.create();
    d.addContentView(v, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
        LayoutParams.WRAP_CONTENT));
    return d;
  }

  /**
   * Create a new Dialog the the specified title, view, and message.
   * 
   * @param title
   *          The title of the dialog.
   * @param message
   *          The message to display in the dialog.
   * @param v
   *          The View to add to the dialog.
   * @return The Dialog.
   */
  public Dialog newViewDialog(String title, String message, View v) {
    mBuilder.setTitle(title);
    mBuilder.setMessage(message);
    Dialog d = mBuilder.create();
    d.addContentView(v, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
        LayoutParams.WRAP_CONTENT));
    return d;
  }

  /**
   * Creates a new Dialog with the specified title and message with a single
   * "Ok" button that does nothing. Equivalent to calling
   * {@link #newOkDialog(String, String, DialogInterface.OnClickListener)} with
   * a listener of {@link #getDoNothingListener()}
   * 
   * @param title
   *          The title of the dialog.
   * @param message
   *          The message to display in the dialog.
   * @return The Dialog.
   * @see #newOkDialog(String, String, DialogInterface.OnClickListener)
   * @see #getDoNothingListener()
   */
  public AlertDialog newOkDialog(String title, String message) {
    return newOkDialog(title, message,
        (DialogInterface.OnClickListener) mDoNothing);
  }

  /**
   * Creates a new Dialog with the specified title, message, and
   * onClickListener, associated with the "Ok" button.
   * 
   * @param title
   *          The title of the dialog.
   * @param message
   *          The message to display in the dialog.
   * @return The Dialog.
   */
  public AlertDialog newOkDialog(String title, String message,
      DialogInterface.OnClickListener ok) {
    mBuilder.setTitle(title);
    mBuilder.setMessage(message);
    mBuilder.setPositiveButton("Ok", ok);
    return mBuilder.create();
  }

  /**
   * Creates a new Dialog with the specified title and message with a single
   * "Ok" associate with <code>ok</code>, and a cancel button that does nothing.
   * Equivalent to calling
   * {@link #newOkCancelDialog(String, String, DialogInterface.OnClickListener, DialogInterface.OnClickListener)}
   * with a <code>cancel</code> listener of {@link #getDoNothingListener()}
   * 
   * @param title
   *          The title of the dialog.
   * @param message
   *          The message to display in the dialog.
   * @param ok
   *          The listener for the "Ok" button.
   * @return The Dialog.
   * @see #newOkCancelDialog(String, String, DialogInterface.OnClickListener,
   *      DialogInterface.OnClickListener)
   * @see #getDoNothingListener()
   */
  public AlertDialog newOkCancelDialog(String title, String message,
      DialogInterface.OnClickListener ok) {
    return newOkCancelDialog(title, message, ok,
        (DialogInterface.OnClickListener) mDoNothing);
  }

  /**
   * Creates a new Dialog with the specified title, message, and
   * onClickListeners, associated with the "Ok" button and "Cancel" buttons
   * respectively.
   * 
   * @param title
   *          The title of the dialog.
   * @param message
   *          The message to display in the dialog.
   * @return The Dialog.
   */
  public AlertDialog newOkCancelDialog(String title, String message,
      DialogInterface.OnClickListener ok, DialogInterface.OnClickListener cancel) {
    mBuilder.setTitle(title);
    mBuilder.setMessage(message);
    mBuilder.setPositiveButton("Ok", ok);
    mBuilder.setNegativeButton("Cancel", cancel);
    return mBuilder.create();
  }

  /**
   * Creates a new ProgressDialog will the message specified. The dialog is
   * cancel-able and progress is indeterminate.
   * 
   * @param message
   *          The message to display in the dialog.
   * @return The ProgressDialog.
   */
  public ProgressDialog newProgressDialog(String message) {
    return newProgressDialog(null, message);
  }

  /**
   * Creates a new ProgressDialog will the message and title specified. The
   * dialog is cancel-able and progress is indeterminate.
   * 
   * @param message
   *          The title of the dialog.
   * @param title
   *          The message to display in the dialog.
   * @return The ProgressDialog.
   */
  public ProgressDialog newProgressDialog(String title, String message) {
    ProgressDialog dialog = new ProgressDialog(mCtx);
    if (title != null)
      dialog.setTitle(title);
    dialog.setMessage(message);
    dialog.setIndeterminate(true);
    return dialog;
  }
  
  /**
   * Creates a new ProgressDialog will the message specified. The dialog is
   * cancel-able and progress is indeterminate.
   * 
   * @param message
   *          The message to display in the dialog.
   * @return The Dialog.
   */
  public ProgressDialog newProgressBarDialog(String message) {
    return newProgressBarDialog(null, message);
  }

  /**
   * Creates a new ProgressDialog will the message and title specified. The
   * dialog is cancel-able and progress is indeterminate.
   * 
   * @param message
   *          The title of the dialog.
   * @param title
   *          The message to display in the dialog.
   * @return The ProgressDialog.
   */
  public ProgressDialog newProgressBarDialog(String title, String message) {
    ProgressDialog d = new ProgressDialog(mCtx);
    d.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

    if (title != null)
      d.setTitle(title);
    d.setMessage(message);
    d.setCancelable(true);
    return d;
  }


  /**
   * Retrieve a reference to a listener that does nothing. Multiple calls will
   * result in the same reference, and new instance is not created.
   * 
   * @return The DialogInterface.OnClickListener.
   */
  public DialogInterface.OnClickListener getDoNothingListener() {
    return mDoNothing;
  }
}
