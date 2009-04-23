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

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.redgeek.android.eventrecorder.DateMapCache;
import net.redgeek.android.eventrecorder.IEventRecorderService;
import net.redgeek.android.eventrecorder.TimeSeriesData;
import net.redgeek.android.eventrend.Preferences;
import net.redgeek.android.eventrend.R;
import net.redgeek.android.eventrend.input.InputActivity;
import net.redgeek.android.eventrend.util.Aggregator;
import net.redgeek.android.eventrend.util.GUITask;
import net.redgeek.android.eventrend.util.Number;
import net.redgeek.android.eventrend.util.ProgressIndicator;
import net.redgeek.android.eventrend.util.Trend;

public class CategoryRowView extends LinearLayout implements GUITask {
  // UI elements
  private CategoryRowView mRowView;
  private EditText mDefaultValue;
  private Button mPlusButton;
  private Button mMinusButton;
  private Button mAddButton;
  private Drawable mTrendIconDrawable;
  private ImageView mTrendIconImage;
  private TextView mCategoryNameView;
  private TextView mCategoryUpdateView;
  private TextView mTrendValueView;
  private ProgressIndicator.Titlebar mProgress;

  // Listeners
  private View.OnClickListener mPlusButtonListener;
  private View.OnClickListener mMinusButtonListener;
  private View.OnClickListener mAddListener;

  // Private data
  private CategoryRow mRow;
  private long mNewDatapointId;
  
  private int mColorInt;
  private float mAddValue;
  
  private Context mCtx;
  private ContentResolver mContent;
  private DateMapCache mDateCache;

  private boolean mSelectable = true;

  public CategoryRowView(Context context, CategoryRow viewRow) {
    super(context);
    mCtx = context;
    mRowView = this;
    mRow = viewRow;
    mNewDatapointId = 0;
    mContent = mCtx.getContentResolver();
    mDateCache = ((InputActivity) mCtx).getDateMapCache();
    
    setupTasks();
    setupUI();
    populateFields();
  }

  public boolean isSelectable() {
    return mSelectable;
  }

  public void setSelectable(boolean selectable) {
    mSelectable = selectable;
  }

  public CategoryRow getRow() {
    return mRow;
  }

  public void executeNonGuiTask() throws Exception {
  }

  public void afterExecute() {
//    String status;
//    String toast;
//
//    float newValue = mAddEntryTask.mLastAddValue;
//    float oldValue = mAddEntryTask.mLastAddOldValue;
//    long timestamp = mAddEntryTask.mLastAddTimestamp;
//
//    if (mAddEntryTask.mLastAddUpdate == true) {
//      ((InputActivity) mCtx).setLastAdd(mAddEntryTask.mLastAddId, oldValue,
//          timestamp, mCategoryUpdateView, mRowView);
//      status = "Update @ " + DateUtil.toShortTimestamp(timestamp) + ": "
//          + oldValue + " -> " + newValue;
//      mCategoryUpdateView.setText(status);
//    } else {
//      ((InputActivity) mCtx).setLastAdd(mAddEntryTask.mLastAddId, newValue,
//          timestamp, mCategoryUpdateView, mRowView);
//
//      status = "Add @ "
//          + DateUtil.toShortTimestamp(mAddEntryTask.mLastAddTimestamp) + ": "
//          + newValue;
//      mCategoryUpdateView.setText(status);
//    }
//
//    CategoryDbTable.Row cat = mDbh.fetchCategory(mDbRow.getId());
//    updateTrendIcon(cat.getTrendState());
//    float trendValue = Number.Round(cat.getLastTrend(), mDecimals);
//    mTrendValueView.setText(Float.valueOf(trendValue).toString());
//
//  ((InputActivity) mCtx).redrawSyntheticViews();

    mAddButton.setClickable(true);
    mAddButton.setTextColor(Color.BLACK);
  }

  public void onFailure(Throwable t) {
    Toast.makeText(mCtx, "add entry failed", Toast.LENGTH_SHORT).show();
    mAddButton.setClickable(true);
    mAddButton.setTextColor(Color.BLACK);
  }

  private void setupTasks() {
//    mAddEntryTask = new AddEntryTask(mTSC, mDecimals, mHistory);
  }

  private void setupUI() {
    this.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
    this.setOrientation(VERTICAL);
    this.setLongClickable(true);

    setupListeners();

    LayoutInflater inflater = (LayoutInflater) mCtx
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    inflater.inflate(R.layout.category_row, this);

    mCategoryNameView = (TextView) findViewById(R.id.category_name);
    mCategoryUpdateView = (TextView) findViewById(R.id.category_status);

    mMinusButton = (Button) findViewById(R.id.category_input_minus);
    mPlusButton = (Button) findViewById(R.id.category_input_plus);
    mDefaultValue = (EditText) findViewById(R.id.category_input_value);
    mAddButton = (Button) findViewById(R.id.category_input_add);
    mTrendIconImage = (ImageView) findViewById(R.id.category_input_trend_icon);
    mTrendValueView = (TextView) findViewById(R.id.category_input_trend_value);

    mPlusButton.setOnClickListener(mPlusButtonListener);
    mMinusButton.setOnClickListener(mMinusButtonListener);
    mAddButton.setOnClickListener(mAddListener);

    mProgress = new ProgressIndicator.Titlebar(mCtx);

  }

  private void setupListeners() {
    mPlusButtonListener = new OnClickListener() {
      public void onClick(View v) {
        float value = Float.valueOf(mDefaultValue.getText().toString())
            .floatValue();
        value += mRow.mIncrement;
        value = Number.Round(value, mRow.mDecimals);
        mDefaultValue.setText(Float.toString(value));
      }
    };

    mMinusButtonListener = new OnClickListener() {
      public void onClick(View v) {
        float value = Float.valueOf(mDefaultValue.getText().toString())
            .floatValue();
        value -= mRow.mIncrement;
        value = Number.Round(value, mRow.mDecimals);
        mDefaultValue.setText(Float.toString(value));
      }
    };

    mAddListener = new OnClickListener() {
      public void onClick(View v) {
        addEntry();
        mDefaultValue.setText(Float.valueOf(mRow.mDefaultValue).toString());
      }
    };
  }

  public void populateFields() {
    try {
      mColorInt = Color.parseColor(mRow.mColor);
    } catch (IllegalArgumentException e) {
      mColorInt = Color.WHITE;
    }

    mCategoryNameView.setText(mRow.mTimeSeriesName);
    mCategoryNameView.setTextColor(mColorInt);

    if (mRow.mType.equals(TimeSeriesData.TimeSeries.TYPE_DISCRETE)) {
      mAddButton.setText("Add");
    } else if (mRow.mType.equals(TimeSeriesData.TimeSeries.TYPE_RANGE)) {
      mMinusButton.setVisibility(View.INVISIBLE);
      mPlusButton.setVisibility(View.INVISIBLE);
      mDefaultValue.setVisibility(View.INVISIBLE);
      if (mRow.mRecordingDatapointId > 0)
        mAddButton.setText("Stop");
      else
        mAddButton.setText("Start");
    } else if (mRow.mType.equals(TimeSeriesData.TimeSeries.TYPE_SYNTHETIC)) {
      mMinusButton.setVisibility(View.INVISIBLE);
      mPlusButton.setVisibility(View.INVISIBLE);
      mDefaultValue.setVisibility(View.INVISIBLE);
      mAddButton.setVisibility(View.INVISIBLE);
    }
    
    mDefaultValue.setText(Float.valueOf(mRow.mDefaultValue).toString());

    int trendState = Trend.TREND_UNKNOWN;
    Uri lastDatapoint = ContentUris.withAppendedId(
        TimeSeriesData.TimeSeries.CONTENT_URI, mRow.mId).buildUpon()
        .appendPath("recent").appendPath("2").build();
    if (lastDatapoint != null) {
      Cursor c = mCtx.getContentResolver().query(lastDatapoint, null, null, null, null);
      if (c != null && c.getCount() > 0) {
        c.moveToFirst();
        float newTrend = TimeSeriesData.Datapoint.getTrend(c);
        float displayTrend = Number.Round(newTrend, mRow.mDecimals);
        mTrendValueView.setText(Float.valueOf(displayTrend).toString());

        c.moveToLast();
        float oldTrend = TimeSeriesData.Datapoint.getTrend(c);
        float stdDev = TimeSeriesData.Datapoint.getStdDev(c);
    
        trendState = Trend.getTrendIconState(oldTrend, newTrend, mRow.mGoal, 
            mRow.mSensitivity, stdDev);
      }
      
      if (c != null)
        c.close();
    }
        
    updateTrendIcon(trendState);
  }

  public static void setLayoutAnimationSlideOutLeftIn(ViewGroup panel,
      Context ctx) {
    AnimationSet set = new AnimationSet(true);
    Animation animation;

    animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
        Animation.RELATIVE_TO_SELF, -1.0f, Animation.RELATIVE_TO_SELF, 0.0f,
        Animation.RELATIVE_TO_SELF, 0.0f);
    animation.setStartOffset(0);
    animation.setDuration(500);
    animation.setRepeatCount(1);
    animation.setRepeatMode(Animation.REVERSE);
    set.addAnimation(animation);
    set.setDuration(500);

    LayoutAnimationController controller = new LayoutAnimationController(set,
        0.25f);
    controller.setOrder(LayoutAnimationController.ORDER_NORMAL);
    panel.setLayoutAnimation(controller);
  }

  public void updateTrendIcon(int trendState) {
    switch (trendState) {
      case Trend.TREND_DOWN_45_BAD:
        mTrendIconDrawable = getResources().getDrawable(
            R.drawable.trend_down_45_bad);
        break;
      case Trend.TREND_DOWN_45_GOOD:
        mTrendIconDrawable = getResources().getDrawable(
            R.drawable.trend_down_45_good);
        break;
      case Trend.TREND_DOWN_30_BAD:
        mTrendIconDrawable = getResources().getDrawable(
            R.drawable.trend_down_30_bad);
        break;
      case Trend.TREND_DOWN_30_GOOD:
        mTrendIconDrawable = getResources().getDrawable(
            R.drawable.trend_down_30_good);
        break;
      case Trend.TREND_DOWN_15_BAD:
        mTrendIconDrawable = getResources().getDrawable(
            R.drawable.trend_down_15_bad);
        break;
      case Trend.TREND_DOWN_15_GOOD:
        mTrendIconDrawable = getResources().getDrawable(
            R.drawable.trend_down_15_good);
        break;
      case Trend.TREND_UP_45_BAD:
        mTrendIconDrawable = getResources().getDrawable(
            R.drawable.trend_up_45_bad);
        break;
      case Trend.TREND_UP_45_GOOD:
        mTrendIconDrawable = getResources().getDrawable(
            R.drawable.trend_up_45_good);
        break;
      case Trend.TREND_UP_30_BAD:
        mTrendIconDrawable = getResources().getDrawable(
            R.drawable.trend_up_30_bad);
        break;
      case Trend.TREND_UP_30_GOOD:
        mTrendIconDrawable = getResources().getDrawable(
            R.drawable.trend_up_30_good);
        break;
      case Trend.TREND_UP_15_BAD:
        mTrendIconDrawable = getResources().getDrawable(
            R.drawable.trend_up_15_bad);
        break;
      case Trend.TREND_UP_15_GOOD:
        mTrendIconDrawable = getResources().getDrawable(
            R.drawable.trend_up_15_good);
        break;
      case Trend.TREND_DOWN_15:
        mTrendIconDrawable = getResources().getDrawable(
            R.drawable.trend_down_15);
        break;
      case Trend.TREND_UP_15:
        mTrendIconDrawable = getResources().getDrawable(R.drawable.trend_up_15);
        break;
      case Trend.TREND_FLAT:
        mTrendIconDrawable = getResources().getDrawable(R.drawable.trend_flat);
        break;
      case Trend.TREND_FLAT_GOAL:
        mTrendIconDrawable = getResources().getDrawable(
            R.drawable.trend_flat_goal_glow);
        break;
      default:
        mTrendIconDrawable = getResources().getDrawable(
            R.drawable.trend_unknown);
        break;
    }

    mTrendIconImage.setImageDrawable(mTrendIconDrawable);
  }

  public void addEntry() {
    mRow.mTimestamp = ((InputActivity) mCtx).getTimestampSeconds();

    IEventRecorderService service = ((InputActivity) mCtx).getRecorderService();
    if (service == null) {
      Toast.makeText(mCtx, "RecorderService not available.", 
          Toast.LENGTH_SHORT).show();
    } else {
      try {
        if (mRow.mType.equals(TimeSeriesData.TimeSeries.TYPE_DISCRETE)) {
          mAddValue = Float.valueOf(mDefaultValue.getText().toString()).floatValue();
          mNewDatapointId = service.recordEvent(mRow.mId, mRow.mTimestamp,
              mAddValue);
        } else if (mRow.mType.equals(TimeSeriesData.TimeSeries.TYPE_RANGE)) {
          if (mRow.mRecordingDatapointId > 0) {
            mNewDatapointId = service.recordEventStop(mRow.mId);
            if (mNewDatapointId > 0)
              mRow.mRecordingDatapointId = 0;
          } else {
            mNewDatapointId = service.recordEventStart(mRow.mId);
            if (mNewDatapointId > 0)
              mRow.mRecordingDatapointId = mNewDatapointId;
          }
        }
      } catch (Exception e) {
        Toast.makeText(mCtx, "add entry failed", Toast.LENGTH_SHORT).show();
      }
    }
    
//    GUITaskQueue.getInstance().addTask(mProgress, this);

    if (mRow.mType.equals(TimeSeriesData.TimeSeries.TYPE_DISCRETE)) {
      mAddButton.setClickable(false);
      mAddButton.setTextColor(Color.LTGRAY);
    } else if (mRow.mType.equals(TimeSeriesData.TimeSeries.TYPE_RANGE)) {
      if (mRow.mRecordingDatapointId == 0) {
        mAddButton.setText("Stop");
        mAddButton.setTextColor(Color.RED);
      } else {
        mAddButton.setText("Start");
        mAddButton.setTextColor(Color.BLACK);        
      }
    }

//    CategoryDbTable.Row cat = mDbh.fetchCategory(mDbRow.getId());
//    updateTrendIcon(cat.getTrendState());
    setLayoutAnimationSlideOutLeftIn(mRowView, mCtx);
  }
}
