/*
 * Copyright (C) 2007 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package net.redgeek.android.eventcalendar;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import net.redgeek.android.eventgrapher.GraphActivity;
import net.redgeek.android.eventgrapher.primitives.Datapoint;
import net.redgeek.android.eventgrapher.primitives.TimeSeries;
import net.redgeek.android.eventgrapher.primitives.TimeSeriesCollector;
import net.redgeek.android.eventrecorder.TimeSeriesData;
import net.redgeek.android.eventrecorder.TimeSeriesData.DateMap;
import net.redgeek.android.eventrend.EvenTrendActivity;
import net.redgeek.android.eventrend.Preferences;
import net.redgeek.android.eventrend.R;
import net.redgeek.android.eventrend.util.DateUtil;
import net.redgeek.android.eventrend.util.DynamicSpinner;
import net.redgeek.android.eventrend.util.ProgressIndicator;

import java.util.ArrayList;
import java.util.Calendar;

public class CalendarActivity extends EvenTrendActivity {
  public static final String VISUALIZATION_VIEW_IDS = "visViewIds";
  public static final String VISUALIZATION_START_TS = "visEndTs";
  public static final String VISUALIZATION_END_TS = "visStartTs";
  public static final String VISUALIZATION_AGGREGATION = "visAggregation";

  // Menu items
  private static final int MENU_CALENDAR_RANGE_ID = Menu.FIRST;
  private static final int MENU_CALENDAR_GRAPH_ID = Menu.FIRST + 1;
  private static final int MENU_CALENDAR_PREFS_ID = Menu.FIRST + 2;
  private static final int MENU_HELP_ID = Menu.FIRST + 3;

  // Dialogs
  private static final int DIALOG_CALENDAR_FILTER = 0;
  private static final int DIALOG_RANGE_INFO = 1;
  private static final int HELP_DIALOG_ID = 2;

  // UI elements
  private LinearLayout mCalendarControls;
  private LinearLayout mCalendarViewLayout;
  private TextView mCalendarStatus;
  private CalendarView mCalendarView;
  private DynamicSpinner mCategorySpinner;
  private DynamicSpinner mPeriodSpinner;
  private Dialog mFilterDialog;
  private ProgressIndicator.Titlebar mProgress;

  // Listeners
  private Spinner.OnItemSelectedListener mAggregationSpinnerListener;
  private Spinner.OnItemSelectedListener mCategorySpinnerListener;
  private Spinner.OnItemSelectedListener mPeriodSpinnerListener;
  private GestureDetector mGestureDetector;
  private OnTouchListener mTouchListener;

  // Private data
  private TimeSeriesCollector mTSC;
  private ArrayList<String> mSeriesEnabled;
  private int mDecimals;
  private int mPeriod;

  // Saved across orientation changes
  private int mStartTs;

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);

    setupData(icicle);
    setupUI();
    populateFields();
  }

  private void setupData(Bundle icicle) {
    mTSC = new TimeSeriesCollector(getContentResolver());
    mTSC.fetchTimeSeries();

    mSeriesEnabled = getIntent().getStringArrayListExtra(VISUALIZATION_VIEW_IDS);
    if (mSeriesEnabled != null) {
      for (int i = 0; i < mSeriesEnabled.size(); i++) {
        Integer j = Integer.valueOf(mSeriesEnabled.get(i));
        mTSC.setSeriesEnabled(j.longValue(), true);
      }
    }
    
    Calendar cal = Calendar.getInstance();
    int defaultStartTs = (int) (cal.getTimeInMillis() / DateMap.SECOND_MS);
    mStartTs = getIntent().getIntExtra(VISUALIZATION_START_TS, defaultStartTs);

    if (icicle != null) {
      mStartTs = icicle.getInt(VISUALIZATION_START_TS);
      mSeriesEnabled = icicle.getStringArrayList(VISUALIZATION_VIEW_IDS);
      if (mSeriesEnabled != null) {
        for (int i = 0; i < mSeriesEnabled.size(); i++) {
          Integer j = Integer.valueOf(mSeriesEnabled.get(i));
          mTSC.setSeriesEnabled(j.longValue(), true);
        }
      }
    }

    mPeriod = TimeSeriesData.DateMap.MONTH_SECS;   
    mTSC.fetchTimeSeries();
  }

  private void setupUI() {
    setupListeners();

    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    setContentView(R.layout.calendar_view);

    mCalendarStatus = (TextView) findViewById(R.id.calendar_status_text);

    mCalendarView = new CalendarView(this, mTSC);
    mCalendarView.resetZoom();
    mCalendarView.setFadingEdgeLength(CalendarView.BORDER_Y * 2);
    mCalendarView.setVerticalFadingEdgeEnabled(true);
    mCalendarView.setPeriod(mPeriod);

    mCalendarViewLayout = (LinearLayout) findViewById(R.id.calendar_plot);
    mCalendarViewLayout.addView(mCalendarView);

    mCalendarControls = (LinearLayout) findViewById(R.id.calendar_controls);

    mCategorySpinner = new DynamicSpinner(mCtx);
    mCategorySpinner.setPrompt("Category");
    mCalendarControls.addView(mCategorySpinner, new LinearLayout.LayoutParams(
        LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    // TODO: spin on mTSC.series
//    Cursor c = getDbh().fetchAllCategories();
//    c.moveToFirst();
//    for (int i = 0; i < c.getCount(); i++) {
//      long id = CategoryDbTable.getId(c);
//      String name = CategoryDbTable.getCategoryName(c);
//      mCategorySpinner.addSpinnerItem(name, new Long(id));
//      if (id == mSeriesEnabled.get(0)) {
//        mCategorySpinner.setSelection(i);
//      }
//      c.moveToNext();
//    }
//    c.close();
    mCategorySpinner.setOnItemSelectedListener(mCategorySpinnerListener);

    mPeriodSpinner = new DynamicSpinner(mCtx);
    mPeriodSpinner.setPrompt("View");
    mCalendarControls.addView(mPeriodSpinner, new LinearLayout.LayoutParams(
        LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    // TODO:  add month/year items to spinner:
//    mPeriodSpinner.addSpinnerItem(CategoryDbTable.KEY_PERIOD_MONTH, new Long(1));
//    mPeriodSpinner.addSpinnerItem(CategoryDbTable.KEY_PERIOD_YEAR, new Long(2));
//    if (mPeriod == Period.MONTH)
//      mPeriodSpinner.setSelection(0);
//    else if(mPeriod == Period.YEAR)
//      mPeriodSpinner.setSelection(1);
//    mPeriodSpinner.setOnItemSelectedListener(mPeriodSpinnerListener);

    mProgress = new ProgressIndicator.Titlebar(mCtx);

    setupGestures();
  }

  private void setupListeners() {
    mCategorySpinnerListener = new Spinner.OnItemSelectedListener() {
      public void onItemSelected(AdapterView parent, View v, int position,
          long id) {
        long catId = mCategorySpinner.getMappingFromPosition(position);
        mCalendarView.setCategoryId(catId);
        display();
        return;
      }

      public void onNothingSelected(AdapterView arg0) {
        return;
      }
    };

    mPeriodSpinnerListener = new Spinner.OnItemSelectedListener() {
      public void onItemSelected(AdapterView parent, View v, int position,
          long id) {
        String period = ((TextView) v).getText().toString();
        // TODO: implement
//        if (period.equals(CategoryDbTable.KEY_PERIOD_YEAR)) {
//          mPeriod = Period.YEAR;
//          mCalendarView.setPeriod(mPeriod);
//          mTSC.setAggregationMs(DateUtil.mapPeriodToLong(Period.MONTH));
//        } else if (period.equals(CategoryDbTable.KEY_PERIOD_MONTH)) {
//          mPeriod = Period.MONTH;
//          mCalendarView.setPeriod(mPeriod);
//          mTSC.setAggregationMs(DateUtil.mapPeriodToLong(Period.DAY));
//        }
        display();
        return;
      }

      public void onNothingSelected(AdapterView arg0) {
        return;
      }
    };
  }

  public void setupGestures() {
    mGestureDetector = new GestureDetector(
        new GestureDetector.SimpleOnGestureListener() {
          public boolean onFling(MotionEvent e1, MotionEvent e2,
              float velocityX, float velocityY) {
            float deltaX = e2.getRawX() - e1.getRawX();
            float deltaY = e2.getRawY() - e1.getRawY();
            int minSlideWidth = mCalendarView.getWidth() / 2;
            int minSlideHeight = mCalendarView.getHeight() / 2;

            if (Math.abs(deltaX) < 100) {
              if (deltaY > minSlideHeight) {
                slideDown();
                return true;
              }

              if (deltaY < -minSlideHeight) {
                slideUp();
                return true;
              }
            }
            if (Math.abs(deltaY) < 100) {
              if (deltaX > minSlideWidth) {
                slideRight();
                return true;
              }

              if (deltaX < -minSlideWidth) {
                slideLeft();
                return true;
              }
            }
            return false;
          }
        });
    mGestureDetector.setIsLongpressEnabled(true);

    mTouchListener = new OnTouchListener() {
      public boolean onTouch(View v, MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
      }
    };
    mCalendarView.setLongClickable(true);
    mCalendarView.setOnTouchListener(mTouchListener);
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent ev) {
    mGestureDetector.onTouchEvent(ev);
    return super.dispatchTouchEvent(ev);
  }

  private void populateFields() {
    mCalendarView.getCalendar().setCalendarStart(mStartTs);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    boolean result = super.onCreateOptionsMenu(menu);
    menu.add(0, MENU_CALENDAR_RANGE_ID, 0, R.string.menu_calendar_range)
        .setIcon(android.R.drawable.ic_menu_info_details);
    menu.add(0, MENU_CALENDAR_GRAPH_ID, 0, R.string.menu_visualize).setIcon(
        R.drawable.graph);
    menu.add(0, MENU_CALENDAR_PREFS_ID, 0, R.string.menu_app_prefs).setIcon(
        android.R.drawable.ic_menu_preferences);
    menu.add(0, MENU_HELP_ID, 0, R.string.menu_app_help).setIcon(
        android.R.drawable.ic_menu_help);
    return result;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case MENU_CALENDAR_RANGE_ID:
        // TODO: implement
        showDialog(DIALOG_RANGE_INFO);
        return true;
      case MENU_CALENDAR_GRAPH_ID:
        // TODO: implement
        graph();
        return true;
      case MENU_CALENDAR_PREFS_ID:
        editPrefs();
        return true;
      case MENU_HELP_ID:
        showDialog(HELP_DIALOG_ID);
        return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case DIALOG_RANGE_INFO:
        return rangeInfoDialog(mCalendarView.getCategoryId());
      case HELP_DIALOG_ID:
        String str = getResources().getString(R.string.calendar_overview);
        return mDialogUtil.newOkDialog("Help", str);
    }
    return null;
  }

  private void editPrefs() {
    Intent i = new Intent(this, Preferences.class);
    startActivityForResult(i, ARC_PREFS_EDIT);
  }

  private void display() {
    mCalendarView.updateData();
    mCalendarView.invalidate();
  }

  public void graph() {
    Uri uri = TimeSeriesData.TimeSeries.CONTENT_URI;
    Intent i = new Intent(Intent.ACTION_VIEW, uri);
    
    i.putStringArrayListExtra(GraphActivity.VISUALIZATION_VIEW_IDS, mSeriesEnabled);
    i.putExtra(VISUALIZATION_START_TS, mCalendarView.getCalendar().getFocusStart());
    i.putExtra(VISUALIZATION_END_TS, mCalendarView.getCalendar().getFocusEnd());
    if (mPeriod == TimeSeriesData.DateMap.YEAR_SECS)
      i.putExtra(VISUALIZATION_AGGREGATION, TimeSeriesData.DateMap.MONTH_SECS);
    else if (mPeriod == TimeSeriesData.DateMap.MONTH_SECS)
      i.putExtra(VISUALIZATION_AGGREGATION, TimeSeriesData.DateMap.DAY_SECS);
    
    startActivityForResult(i, ARC_VISUALIZE_VIEW);
  }

  public ArrayList<String> getSeriesEnabledState() {
    return mSeriesEnabled;
  }

  public TextView getStatusTextView() {
    return mCalendarStatus;
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    display();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    // TODO: implement
//    if (mSeriesEnabled != null)
//      outState.putIntegerArrayList("defaultCatIds", mSeriesEnabled);
//
//    long periodMs = DateUtil.mapPeriodToLong(mPeriod);
//    outState.putLong(GRAPH_START_MS, mCalendarView.getCalendar()
//        .getCalendarStart());
//    outState.putLong(CALENDAR_PERIOD, periodMs);
    super.onSaveInstanceState(outState);
  }

  @Override
  protected void onResume() {
    mCalendarView.setColorScheme();
    display();
    super.onResume();
  }

  private Dialog rangeInfoDialog(long catId) {
    Builder b = new AlertDialog.Builder(mCtx);
    // TODO: implement
//    CategoryDbTable.Row cat = dbh.fetchCategory(catId);
//    TimeSeries ts = mTSC.getSeriesByIdLocking(catId);
//
//    int decimals = Preferences.getDecimalPlaces(getCtx());
//
//    String info = "Category: " + cat.getCategoryName() + "\n";
//    if (ts != null) {
//      Number.RunningStats valueStats = ts.getValueStats();
//      Number.RunningStats timestampStats = ts.getTimestampStats();
//
//      String tsAvgPeriod = DateUtil.toString(timestampStats.mMean);
//      String tsAvgEntry = DateUtil.toString(timestampStats.mEntryMean);
//      String tsVar = DateUtil.toStringSquared(timestampStats.mVar);
//      String tsSD = DateUtil.toString(timestampStats.mStdDev);
//
//      Datapoint first = ts.getFirstVisible();
//      Datapoint last = ts.getLastVisible();
//
//      info += "Values:\n" + "  " + DateUtil.toTimestamp(first.mMillis) + " -\n"
//          + "  " + DateUtil.toTimestamp(last.mMillis) + "\n"
//          + "  Range:       " + ts.getVisibleValueMin() + " - "
//          + ts.getVisibleValueMax() + "\n" + "  Average:   "
//          + Number.Round(valueStats.mMean, decimals) + "\n" + "  Std Dev.:    "
//          + Number.Round(valueStats.mStdDev, decimals) + "\n"
//          + "  Variance:   " + Number.Round(valueStats.mVar, decimals) + "\n"
//          + "  Trend:       " + Number.Round(ts.getTrendStats().mMin, decimals)
//          + " - " + Number.Round(ts.getTrendStats().mMax, decimals) + "\n"
//          + "Date Goal is Reached:\n" + "Time Between Datapoints:\n"
//          + "  Avgerage:  " + tsAvgPeriod + "\n" + "  Std Dev.:   " + tsSD
//          + "\n" + "  Variance:   " + tsVar + "\n" + "Time Between Entries:\n"
//          + "  Avg/Entry:   " + tsAvgEntry + "\n";
//    }
//
//    b.setTitle("Visible Range Info");
//    b.setMessage(info);
//    b.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
//      public void onClick(DialogInterface dialog, int whichButton) {
//        // do nothing
//      }
//    });
    Dialog d = b.create();
    return d;
  }

  public void slideRight() {
    // AnimationSet set = new AnimationSet(true);
    // set.setStartOffset(0);
    // set.setDuration(500);
    //    	
    // Animation slideOutLeft = new TranslateAnimation(
    // Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 1.0f,
    // Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f
    // );
    // slideOutLeft.setStartOffset(0);
    // slideOutLeft.setDuration(500);
    // slideOutLeft.setFillAfter(true);
    // set.addAnimation(slideOutLeft);
    //
    // Animation slideInRight = new TranslateAnimation(
    // Animation.RELATIVE_TO_SELF, -1.0f, Animation.RELATIVE_TO_SELF, 0.0f,
    // Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f
    // );
    // slideInRight.setStartOffset(0);
    // slideInRight.setDuration(500);
    // slideInRight.setFillAfter(true);
    // set.addAnimation(slideInRight);
    //    	
    // mCalendarView.startAnimation(set);

    mCalendarView.prevJump();
    display();
  }

  public void slideLeft() {
    mCalendarView.nextJump();
    display();
  }

  public void slideDown() {
    mCalendarView.prevPeriod();
    display();
  }

  public void slideUp() {
    mCalendarView.nextPeriod();
    display();
  }
}
