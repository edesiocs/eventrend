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

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

/** Dynamic spinner convenience class, allows for easy adding of items
 * and clearing of a Spinner view.
 * 
 * @author barclay
 *
 */
public class DynamicSpinner extends Spinner {
    private List<Long> mSpinnerAdapterMapping = new ArrayList<Long>();
	private ArrayAdapter<CharSequence> mSpinnerAdapter;
	private Context mCtx;
	
	/**  Constructor.  Creates an empty dynamic spinner associated with
	 * the context of <code>context</code>.
	 * 
	 * @param context The Context responsible for this view.
	 */
	public DynamicSpinner(Context context) {
		super(context);
		setup(context);
	}

	/**  Constructor.  Creates an empty dynamic spinner associated with
	 * the context of <code>context</code>.
	 * <br>
	 * <strong>attrs is currently ignored and only present to satisfy
	 * the same constructor set as a Spinner.</strong>
	 * 
	 * @param context The Context responsible for this view.
	 * @param attrs (Ignored.)
	 */
	public DynamicSpinner(Context context, AttributeSet attrs) {
		super(context);
		setup(context);
	}

	/**  Constructor.  Creates an empty dynamic spinner associated with
	 * the context of <code>context</code>.
	 * <br>
	 * <strong>attrs and defStyle are currently ignored and only present to satisfy
	 * the same constructor set as a Spinner.</strong>
	 * 
	 * @param context The Context responsible for this view.
	 * @param attrs (Ignored.)
	 * @param defStyle (Ignored.)
	 */
	public DynamicSpinner(Context context, AttributeSet attrs, int defStyle) {
		super(context);
		setup(context);
	}

	/** Initialization common to all constructors.  Stores away the context, allocates
	 * and ArrayAdapter for the Spinner, sets it's resources, and sets the adapter for
	 * the Spinner.
	 * 
	 * @param context The context associated with this view.
	 */
	private void setup(Context context) {
		mCtx = context;
	    mSpinnerAdapter = new ArrayAdapter<CharSequence>(mCtx, android.R.layout.simple_spinner_item);
	    mSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    setAdapter(mSpinnerAdapter);
	}
    
	/** Appends a menu item to the Spinner.
	 * 
	 * @param text The text for the menu item.
	 * @param mapping A number associated with the entry, can be arbitrary.
	 * @see #getMappingFromPosition(int position)
	 */
	public void addSpinnerItem(String text, Long mapping) {
		CharSequence textHolder = "" + text;
		mSpinnerAdapter.add(textHolder);
		mSpinnerAdapterMapping.add(mapping);
    }

	/** Removes all spinner menu items (and associated mappings) from the Spinner.
	 */
	public void clearSpinnerItems() {
    	mSpinnerAdapter.clear();
    	mSpinnerAdapterMapping.clear();
    }

	/** Returns the mapping associated with the menu item at <code>position</code>.
	 * @param position
	 * @return
	 * @see #addSpinnerItem(String, Long)
	 */
	public long getMappingFromPosition(int position) {
		return Long.valueOf(mSpinnerAdapterMapping.get(position));
	}

	/** Sets the Spiner to visible or invisible.
	 * 
	 * @param b boolean, true or false
	 */
	public void setVisibility(boolean b) {
		setVisibility(b);		
	}
}
