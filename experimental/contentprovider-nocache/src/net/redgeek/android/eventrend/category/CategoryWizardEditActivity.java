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

package net.redgeek.android.eventrend.category;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ViewFlipper;

import net.redgeek.android.eventrecorder.TimeSeriesData;
import net.redgeek.android.eventrecorder.TimeSeriesProvider;
import net.redgeek.android.eventrecorder.TimeSeriesData.TimeSeries;
import net.redgeek.android.eventrend.EvenTrendActivity;
import net.redgeek.android.eventrend.R;
import net.redgeek.android.eventrend.util.ColorPickerDialog;
import net.redgeek.android.eventrend.util.ComboBox;
import net.redgeek.android.eventrend.util.DynamicSpinner;
import net.redgeek.android.eventrend.util.Number;

import java.util.ArrayList;

public class CategoryWizardEditActivity extends CategoryEditActivity {  
  // UI elements
  private ViewFlipper    mFlipper;
  private LayoutInflater mInflater;

  // Page 1:  name + group editing:
  private Button   mCancelPage1;
  private Button   mNextPage1;
  
  // Page 2:  category type:
  private Button   mCancelPage2;
  private Button   mBackPage2;
  private Button   mNextPage2;

  // Page 3a: discrete settings:
  private Button   mCancelPage3a;
  private Button   mBackPage3a;
  private Button   mNextPage3a;

  // Page 3b: synthetics setting:
  private TextView mFormulaText;
  private Button   mCancelPage3b;
  private Button   mBackPage3b;
  private Button   mNextPage3b;

  // Page 4:  aggregation
  private Button   mCancelPage4;
  private Button   mBackPage4;
  private Button   mNextPage4;
  private TextView mZerofillHelp;
  private LinearLayout mZerofillLayout;
  
  // Page 5:  customization
  private Button   mCancelPage5;
  private Button   mSavePage5;
  private Button   mAdvancedPage5;

  // page 6:  trending
  private Button   mCancelPage6;
  private Button   mSavePage6;

  // Listeners:
  private Spinner.OnItemSelectedListener mAggregatePeriodListenerChild;
  private RadioGroup.OnCheckedChangeListener mAggListenerChild;
    
  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    
    setContentView(R.layout.timeseries_edit_root);
    setupUIPages();
    super.setupUI(findViewById(R.id.category_wizard_root));
    setupUIPost();
    
    super.populateFields();
    super.updatePaint(mColorStr);
  }

  private void setupUIPages() {
    LinearLayout page;
    
    mInflater = (LayoutInflater) mCtx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    mFlipper = (ViewFlipper) findViewById(R.id.wizard_flipper);
    mFlipper.setInAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left));
    mFlipper.setOutAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right));

    mFlipper.addView(setuiUIPage1());
    mFlipper.addView(setuiUIPage2());
    mFlipper.addView(setuiUIPage3a());
    mFlipper.addView(setuiUIPage3b());
    mFlipper.addView(setuiUIPage4());
    mFlipper.addView(setuiUIPage5());
    mFlipper.addView(setuiUIPage6());
  }

  private View setuiUIPage1() {
    LinearLayout page = (LinearLayout) mInflater.inflate(R.layout.timeseries_edit_name, null);

    mCancelPage1 = (Button) page.findViewById(R.id.category_edit_cancel);
    mCancelPage1.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        setResult(CATEGORY_CANCELED);
        finish();
      }
    });
    
    mNextPage1 = (Button) page.findViewById(R.id.category_edit_next);
    mNextPage1.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        slideLeft();
      }
    });
    
    return page;
  }

  private View setuiUIPage2() {
    LinearLayout page = (LinearLayout) mInflater.inflate(R.layout.timeseries_edit_type, null);

    mCancelPage2 = (Button) page.findViewById(R.id.category_edit_cancel);
    mCancelPage2.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        setResult(CATEGORY_CANCELED);
        finish();
      }
    });
    
    mBackPage2 = (Button) page.findViewById(R.id.category_edit_back);
    mBackPage2.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        slideRight();
      }
    });

    mNextPage2 = (Button) page.findViewById(R.id.category_edit_next);
    mNextPage2.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        if (mType.toLowerCase().equals(TimeSeries.TYPE_DISCRETE)) {
          slideLeft();
        }
        else if (mType.toLowerCase().equals(TimeSeries.TYPE_SYNTHETIC)) {
          slideLeft();
          slideLeft();
        }
        else if (mType.toLowerCase().equals(TimeSeries.TYPE_RANGE)) {
          slideLeft();
          slideLeft();
          slideLeft();
        }        
      }
    });
    
    return page;
  }

  private View setuiUIPage3a() {
    LinearLayout page = (LinearLayout) mInflater.inflate(R.layout.timeseries_edit_discrete, null);

    mCancelPage3a = (Button) page.findViewById(R.id.category_edit_cancel);
    mCancelPage3a.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        setResult(CATEGORY_CANCELED);
        finish();
      }
    });
    
    mBackPage3a = (Button) page.findViewById(R.id.category_edit_back);
    mBackPage3a.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        slideRight();
      }
    });

    mNextPage3a = (Button) page.findViewById(R.id.category_edit_next);
    mNextPage3a.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        slideLeft();
        slideLeft();
      }
    });
    
    return page;
  }

  private View setuiUIPage3b() {
    LinearLayout page = (LinearLayout) mInflater.inflate(R.layout.timeseries_edit_formula, null);

    mCancelPage3b = (Button) page.findViewById(R.id.category_edit_cancel);
    mCancelPage3b.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        setResult(CATEGORY_CANCELED);
        finish();
      }
    });
    
    mBackPage3b = (Button) page.findViewById(R.id.category_edit_back);
    mBackPage3b.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        slideRight();
        slideRight();
      }
    });

    mNextPage3b = (Button) page.findViewById(R.id.category_edit_next);
    mNextPage3b.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        slideLeft();
      }
    });
    
    return page;
  }

  private View setuiUIPage4() {
    LinearLayout page = (LinearLayout) mInflater.inflate(R.layout.timeseries_edit_aggregation, null);

    mAggregatePeriodListenerChild = new Spinner.OnItemSelectedListener() {
      public void onItemSelected(AdapterView parent, View v, int position,
          long id) {
        CategoryEditActivity act = (CategoryEditActivity) mCtx;
        act.mAggregatePeriodListener.onItemSelected(parent, v, position, id);
        if (mPeriodSeconds == 0
            || mType.toLowerCase().equals(TimeSeries.TYPE_SYNTHETIC)
            || mAggRadio.getText().toString().toLowerCase().equals(
                TimeSeries.AGGREGATION_AVG)) {
          mZerofillHelp.setVisibility(View.GONE);
          mZerofillLayout.setVisibility(View.GONE);
        } else {
          mZerofillHelp.setVisibility(View.VISIBLE);
          mZerofillLayout.setVisibility(View.VISIBLE);
        }
        return;
      }

      public void onNothingSelected(AdapterView arg0) {
        return;
      }
    };

    // aggregation type: sum or average:
    mAggListenerChild = new RadioGroup.OnCheckedChangeListener() {
      public void onCheckedChanged(RadioGroup group, int checkedId) {
        CategoryEditActivity act = (CategoryEditActivity) mCtx;
        act.mAggListener.onCheckedChanged(group, checkedId);
        if (mAggRadio == null || mType == null)
          return;

        if (mPeriodSeconds == 0
            || mType.toLowerCase().equals(TimeSeries.TYPE_SYNTHETIC)
            || mAggRadio.getText().toString().toLowerCase().equals(
                TimeSeries.AGGREGATION_AVG)) {
          mZerofillHelp.setVisibility(View.GONE);
          mZerofillLayout.setVisibility(View.GONE);
        } else {
          mZerofillHelp.setVisibility(View.VISIBLE);
          mZerofillLayout.setVisibility(View.VISIBLE);
        }
      }
    };

    // Cancel and next:
    mCancelPage4 = (Button) page.findViewById(R.id.category_edit_cancel);
    mCancelPage4.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        setResult(CATEGORY_CANCELED);
        finish();
      }
    });
    
    mBackPage4 = (Button) page.findViewById(R.id.category_edit_back);
    mBackPage4.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        if (mType.toLowerCase().equals(TimeSeries.TYPE_DISCRETE)) {
          slideRight();
          slideRight();
        }
        else if (mType.toLowerCase().equals(TimeSeries.TYPE_SYNTHETIC)) {
          slideRight();
        }
        else if (mType.toLowerCase().equals(TimeSeries.TYPE_RANGE)) {
          slideRight();
          slideRight();
          slideRight();
        }        
      }
    });

    mNextPage4 = (Button) page.findViewById(R.id.category_edit_next);
    mNextPage4.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        slideLeft();
      }
    });
    
    return page;
  }

  private View setuiUIPage5() {
    LinearLayout page = (LinearLayout) mInflater.inflate(R.layout.timeseries_edit_customize, null);

    mCancelPage5 = (Button) page.findViewById(R.id.category_edit_cancel);
    mCancelPage5.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        setResult(CATEGORY_CANCELED);
        finish();
      }
    });
    
    mSavePage5 = (Button) page.findViewById(R.id.category_edit_save);
    mSavePage5.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        CategoryEditActivity act = (CategoryEditActivity) mCtx;
        setResult(act.saveState());
        finish();
      }
    });

    mAdvancedPage5 = (Button) page.findViewById(R.id.category_edit_next);
    mAdvancedPage5.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        slideLeft();
      }
    });
    
    return page;
  }

  private View setuiUIPage6() {
    LinearLayout page = (LinearLayout) mInflater.inflate(R.layout.timeseries_edit_trending, null);

    // Cancel and next:
    mCancelPage6 = (Button) page.findViewById(R.id.category_edit_cancel);
    mCancelPage6.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        setResult(CATEGORY_CANCELED);
        finish();
      }
    });
    
    mSavePage6 = (Button) page.findViewById(R.id.category_edit_save);
    mSavePage6.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
          CategoryEditActivity act = (CategoryEditActivity) mCtx;
          setResult(act.saveState());
        finish();
      }
    });
    
    return page;
  }
  
  private void setupUIPost() {
    mAggregatePeriodSpinner.setOnItemSelectedListener(mAggregatePeriodListener);
    mAggregatePeriodSpinner.setSelection(0);
    mAggRadioGroup.setOnCheckedChangeListener(mAggListener);
  }
  
  protected void slideLeft() {
    Animation slideInLeft = new TranslateAnimation(Animation.RELATIVE_TO_SELF,
        1.0f, Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
        0.0f, Animation.RELATIVE_TO_SELF, 0.0f);
    Animation slideOutRight = new TranslateAnimation(
        Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, -1.0f,
        Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f);
    slideInLeft.setDuration(500);
    slideOutRight.setDuration(500);

    mFlipper.setInAnimation(slideInLeft);
    mFlipper.setOutAnimation(slideOutRight);

    mFlipper.showNext();
  }

  protected void slideRight() {
    Animation slideOutLeft = new TranslateAnimation(Animation.RELATIVE_TO_SELF,
        0.0f, Animation.RELATIVE_TO_SELF, 1.0f, Animation.RELATIVE_TO_SELF,
        0.0f, Animation.RELATIVE_TO_SELF, 0.0f);
    Animation slideInRight = new TranslateAnimation(Animation.RELATIVE_TO_SELF,
        -1.0f, Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
        0.0f, Animation.RELATIVE_TO_SELF, 0.0f);
    slideOutLeft.setDuration(500);
    slideInRight.setDuration(500);

    mFlipper.setInAnimation(slideInRight);
    mFlipper.setOutAnimation(slideOutLeft);
    mFlipper.showPrevious();
  }
}
