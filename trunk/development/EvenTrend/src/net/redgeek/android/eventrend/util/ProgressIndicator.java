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

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.widget.ProgressBar;

/** Abstract base class of progress indicators for use with GUITask.  Implementing
 * class must implement showProgressIndicator() and hideProgressIndicator().
 * 
 * @author barclay
 * @see GUITask
 */
public interface ProgressIndicator {
	void showProgressIndicator();
	void hideProgressIndicator();

	/** A ProgressIndicator for setting the visiblity of an indeterminate progress
	 * indicator in the titlebar.  Note that the activity must have called
	 * <code>requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);</code>
	 * 
	 * @author barclay
	 * @see ProgressIndicator
	 */
	public static class Titlebar implements ProgressIndicator {
		Context mCtx;

		/** Sole constructor.
		 * 
		 * @param ctx The Context of the activity to with which to set the
		 * indeterminate progress window decoration visible or invisible.  
		 * Typically this will a class implementing GUITask.
		 * @see GUITask
		 * @see ProgressIndicator
		 */
		public Titlebar(Context ctx) {
			mCtx = ctx;
		}

		/** Sets the indeterminate progress decoration visible.
		 */
		public void showProgressIndicator() {
			((Activity) mCtx).setProgressBarIndeterminateVisibility(true);
		}

		/** Sets the indeterminate progress decoration invisible.
		 */
		public void hideProgressIndicator() {
			((Activity) mCtx).setProgressBarIndeterminateVisibility(false);
		}
	}

	/** A ProgressIndicator that displays and dismissed a dialog box, most likely
	 * with an indeterminate progress indicator.  This dialog is "soft" because it
	 * is cancel-able.
	 * 
	 * @author barclay
	 * @see ProgressIndicator
	 * @see DialogUtil
	 */
	public static class DialogSoft implements ProgressIndicator {
		private Context mCtx;
		private int     mId;

		/** Constructor.
		 * 
		 * @param context The Context of the activity with which to generate the dialog.
		 * @param id The id of the dialog, which must be present in the calling
		 * activity's <code>onCreateDialog()</code> in order to display the dialog.
		 * @see ProgressIndicator
		 * @see DialogUtil
		 */
		public DialogSoft(Context context, int id) {
			mCtx = context;
			mId  = id;
		}

		/** Calls showDialog() on the previously generated dialog.
		 */
		public void showProgressIndicator() {
			((Activity) mCtx).showDialog(mId);
		}

		/** Calls dismissDialog() on the previously generated dialog.
		 */
		public void hideProgressIndicator() {
			((Activity) mCtx).dismissDialog(mId);
		}
	}

	/** A ProgressIndicator composed of a dialog box with an updateable progress
	 * bar.  <strong>Not yet implemented</strong>
	 * 
	 * @author barclay
	 * @see ProgressIndicator
	 * @see DialogUtil
	 */
	public static class DialogWithBar implements ProgressIndicator {
		private DialogUtil   mDU;
		private Dialog       mDialog;
		private int          mId;
	    private ProgressBar  mProgress;

		public DialogWithBar(DialogUtil du, int id, String msg) {
			mDU = du;
			mId = id;
			mProgress = new ProgressBar(mDU.getContext());
			mProgress.setIndeterminate(false);
        	mDialog = mDU.newViewDialog(msg, mProgress);
		}

		public DialogWithBar(DialogUtil du, int id, String title, String msg) {
			mDU = du;
			mId = id;
			mProgress = new ProgressBar(mDU.getContext());
			mProgress.setIndeterminate(false);
        	mDialog = mDU.newViewDialog(title, msg, mProgress);
		}

		public Dialog getDialog() {
			return mDialog;
		}
		
		public void showProgressIndicator() {
			mProgress.setProgress(0);
			mProgress.setSecondaryProgress(0);
			((Activity) mDU.getContext()).showDialog(mId);
		}

		public void hideProgressIndicator() {
			((Activity) mDU.getContext()).dismissDialog(mId);
		}
	}
}

