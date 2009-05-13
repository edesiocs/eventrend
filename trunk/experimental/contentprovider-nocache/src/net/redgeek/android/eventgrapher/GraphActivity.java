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

package net.redgeek.android.eventgrapher;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.ZoomControls;

import net.redgeek.android.eventgrapher.plugins.LinearMatrixCorrelator;
import net.redgeek.android.eventgrapher.primitives.TimeSeries;
import net.redgeek.android.eventgrapher.primitives.TimeSeriesCollector;
import net.redgeek.android.eventrecorder.TimeSeriesData;
import net.redgeek.android.eventrecorder.TimeSeriesData.DateMap;
import net.redgeek.android.eventrend.EvenTrendActivity;
import net.redgeek.android.eventrend.R;
import net.redgeek.android.eventrend.util.DynamicSpinner;
import net.redgeek.android.eventrend.util.GUITaskQueue;
import net.redgeek.android.eventrend.util.ProgressIndicator;

import java.util.ArrayList;
import java.util.Calendar;

public class GraphActivity extends EvenTrendActivity {
  public static final String DEFAULT_VIEW_IDS = "graphViewIds";
  public static final String GRAPH_START_TS = "graphEndTs";
  public static final String GRAPH_END_TS = "graphStartTs";
  public static final String GRAPH_AGGREGATION = "graphAggregation";

  // Menu items
  private static final int MENU_GRAPH_FILTER_ID = Menu.FIRST;
  private static final int MENU_GRAPH_CORRELATE_ID = Menu.FIRST + 1;
  private static final int MENU_GRAPH_SNAP_TO_PERIOD_ID = Menu.FIRST + 2;
  private static final int MENU_CALENDAR_VIEW_ID = Menu.FIRST + 3;
  private static final int MENU_GRAPH_PREFS_ID = Menu.FIRST + 4;

  // Dialogs
  private static final int DIALOG_GRAPH_FILTER = 0;
  private static final int DIALOG_GRAPH_CORRELATE = 1;

  // UI elements
  private ToggleButton mShowTrendsToggle;
  private ToggleButton mShowGoalsToggle;
  private ToggleButton mShowMarkersToggle;
  private ZoomControls mZoomControls;
  private LinearLayout mGraphControls;
  private GraphView mGraphView;
  private DynamicSpinner mAggregationSpinner;
  private LinearLayout mAggregationSpinnerLayout;
  private LinearLayout mGraphPlotLayout;
  private LinearLayout mGraphZoomLayout;
  private TextView mGraphStatus;
  private GraphFilterListAdapter mGFLA;
  private Dialog mFilterDialog;
  private ProgressIndicator.Titlebar mProgress;

  // Special aggregation period:
  private static final String AUTO_AGGREGATION = "Automatic";

  // Listeners
  private Spinner.OnItemSelectedListener mAggregationSpinnerListener;
  private CompoundButton.OnCheckedChangeListener mShowTrendsToggleListener;
  private CompoundButton.OnCheckedChangeListener mShowGoalsToggleListener;
  private CompoundButton.OnCheckedChangeListener mShowMarkersToggleListener;

  // Private data
  private TimeSeriesCollector mTSC;
  private ArrayList<String> mSeriesEnabled;

  // Saved across orientation changes
  private int mStartTs;
  private int mEndTs;
  // TODO: change this to a constant
  private int mAggregation = -1;

  // Tasks
  private CorrelateTask mCorrelator;

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);

    setupData(icicle);
    setupTasks();
    setupUI();
    populateFields();
  }

  private void setupTasks() {
    mCorrelator = new CorrelateTask();
  }

  private void setupData(Bundle icicle) {
    mTSC = new TimeSeriesCollector(getContentResolver());
    mTSC.fetchTimeSeries();

    mSeriesEnabled = getIntent().getStringArrayListExtra(DEFAULT_VIEW_IDS);
    if (mSeriesEnabled != null) {
      for (int i = 0; i < mSeriesEnabled.size(); i++) {
        Integer j = Integer.valueOf(mSeriesEnabled.get(i));
        mTSC.setSeriesEnabled(j.longValue(), true);
      }
    }
    
    Calendar cal = Calendar.getInstance();
    int defaultEndTs = (int) (cal.getTimeInMillis() / DateMap.SECOND_MS);
    int defaultStartTs = (int) (defaultEndTs - ((DateMap.DAY_SECS) * 7));

    mStartTs = getIntent().getIntExtra(GRAPH_START_TS, defaultStartTs);
    mEndTs = getIntent().getIntExtra(GRAPH_END_TS, defaultEndTs);

    if (icicle != null) {
      mStartTs = icicle.getInt(GRAPH_START_TS);
      mEndTs = icicle.getInt(GRAPH_END_TS);
      mAggregation = icicle.getInt(GRAPH_AGGREGATION);
      mSeriesEnabled = icicle.getStringArrayList(DEFAULT_VIEW_IDS);
      if (mSeriesEnabled != null) {
        for (int i = 0; i < mSeriesEnabled.size(); i++) {
          Integer j = Integer.valueOf(mSeriesEnabled.get(i));
          mTSC.setSeriesEnabled(j.longValue(), true);
        }
      }
    }
  }

  private void setupUI() {
    setupListeners();

    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    setContentView(R.layout.graph_view);

    mGraphStatus = (TextView) findViewById(R.id.graph_status);

    mZoomControls = createZoomControls();
    mGraphView = new GraphView(this, mZoomControls, mTSC);
    mGraphView.resetZoom();

    mGraphControls = (LinearLayout) findViewById(R.id.graph_controls);
    mGraphControls.setGravity(Gravity.CENTER_HORIZONTAL
        | Gravity.CENTER_VERTICAL);

    mAggregationSpinner = new DynamicSpinner(mCtx);
    mAggregationSpinner.setPrompt("Aggregate By");
    mAggregationSpinnerLayout = (LinearLayout) findViewById(R.id.graph_view_agg_menu);
    mAggregationSpinnerLayout.addView(mAggregationSpinner,
        new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
            LayoutParams.WRAP_CONTENT));
    for (int i = 0; i < TimeSeriesData.TimeSeries.AGGREGATION_PERIOD_NAMES.length; i++) {
      mAggregationSpinner.addSpinnerItem(TimeSeriesData.TimeSeries.AGGREGATION_PERIOD_NAMES[i],
          new Long(TimeSeriesData.TimeSeries.AGGREGATION_PERIOD_TIMES[i]));
    }
    mAggregationSpinner.setOnItemSelectedListener(mAggregationSpinnerListener);

    mGraphPlotLayout = (LinearLayout) findViewById(R.id.graph_plot);
    mGraphPlotLayout.addView(mGraphView);
    mGraphZoomLayout = (LinearLayout) findViewById(R.id.graph_zoom);
    mGraphZoomLayout.addView(mZoomControls, new LinearLayout.LayoutParams(
        LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

    mShowTrendsToggle = (ToggleButton) findViewById(R.id.graph_view_trends);
    mShowTrendsToggle.setOnCheckedChangeListener(mShowTrendsToggleListener);

    mShowGoalsToggle = (ToggleButton) findViewById(R.id.graph_view_goals);
    mShowGoalsToggle.setOnCheckedChangeListener(mShowGoalsToggleListener);

    mShowMarkersToggle = (ToggleButton) findViewById(R.id.graph_view_markers);
    mShowMarkersToggle.setOnCheckedChangeListener(mShowMarkersToggleListener);

    mProgress = new ProgressIndicator.Titlebar(mCtx);
  }

  private void setupListeners() {
    mAggregationSpinnerListener = new Spinner.OnItemSelectedListener() {
      public void onItemSelected(AdapterView parent, View v, int position,
          long id) {
        String period = ((TextView) v).getText().toString().toLowerCase();
        mTSC.setAggregationPeriod(period);
        graph();
        return;
      }

      public void onNothingSelected(AdapterView arg0) {
        return;
      }
    };

    mShowTrendsToggleListener = new CompoundButton.OnCheckedChangeListener() {
      public void onCheckedChanged(CompoundButton view, boolean isChecked) {
        mGraphView.setTrendView(isChecked);
        graph();
      }
    };

    mShowGoalsToggleListener = new CompoundButton.OnCheckedChangeListener() {
      public void onCheckedChanged(CompoundButton view, boolean isChecked) {
        mGraphView.setGoalView(isChecked);
        graph();
      }
    };

    mShowMarkersToggleListener = new CompoundButton.OnCheckedChangeListener() {
      public void onCheckedChanged(CompoundButton view, boolean isChecked) {
        mGraphView.setMarkersView(isChecked);
        graph();
      }
    };
  }

  private ZoomControls createZoomControls() {
    ZoomControls zoomControls = new ZoomControls(this);
    zoomControls.setLayoutParams(new FrameLayout.LayoutParams(
        LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM
            + Gravity.CENTER_HORIZONTAL));
    zoomControls.setVisibility(View.INVISIBLE);
    return zoomControls;
  }

  private void populateFields() {
    mGraphView.getGraph().setGraphRange(mStartTs, mEndTs);
//    mTSC.setAggregationPeriod();
//  mAggregationSpinner.setSelection(CategoryDbTable
//  .mapMsToIndex(mAggregation) + 1);
  }

  @Override
  public void executeNonGuiTask() throws Exception {
    mCorrelator.correlate();
  }

  @Override
  public void afterExecute() {
    showDialog(DIALOG_GRAPH_CORRELATE);
  }

  @Override
  public void onFailure(Throwable t) {
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    boolean result = super.onCreateOptionsMenu(menu);
    menu.add(0, MENU_GRAPH_FILTER_ID, 0, R.string.graph_filter).setIcon(
        R.drawable.filter_small);
    menu.add(0, MENU_GRAPH_CORRELATE_ID, 0, R.string.graph_correlate).setIcon(
        R.drawable.correlate_small);
    menu.add(0, MENU_GRAPH_SNAP_TO_PERIOD_ID, 0, R.string.graph_snap_to)
        .setIcon(R.drawable.snapto);
    menu.add(0, MENU_CALENDAR_VIEW_ID, 0, R.string.menu_calendar).setIcon(
        android.R.drawable.ic_menu_today);
    menu.add(0, MENU_GRAPH_PREFS_ID, 0, R.string.menu_app_prefs).setIcon(
        android.R.drawable.ic_menu_preferences);
    return result;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case MENU_GRAPH_FILTER_ID:
        showDialog(DIALOG_GRAPH_FILTER);
        removeDialog(DIALOG_GRAPH_CORRELATE);
        return true;
      case MENU_GRAPH_CORRELATE_ID:
        LinearMatrixCorrelator c = new LinearMatrixCorrelator();
        mCorrelator.setCorrelator(c);
        mCorrelator.setTimeSeries(mTSC.getAllEnabledSeries());
        GUITaskQueue.getInstance().addTask(mProgress, this);
        return true;
      case MENU_GRAPH_SNAP_TO_PERIOD_ID:
        mGraphView.snapToSpan();
        graph();
        return true;
      case MENU_CALENDAR_VIEW_ID:
        calendarView();
        return true;
      case MENU_GRAPH_PREFS_ID:
        editPrefs();
        return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private void editPrefs() {
    // TODO: impelement
//    Intent i = new Intent(this, Preferences.class);
//    startActivityForResult(i, PREFS_EDIT);
  }

  private void graph() {
    mGraphView.updateData();
    mGraphView.invalidate();
  }

  private void calendarView() {
    // TODO: implement
//    mSeriesEnabled.clear();
//    ArrayList<TimeSeries> series = mTSC.getAllEnabledSeries();
//    for (int j = 0; j < series.size(); j++) {
//      TimeSeries ts = series.get(j);
//      if (ts != null)
//        mSeriesEnabled.add(new Integer((int) ts.mRow.mId));
//    }
//
//    Intent i = new Intent(this, CalendarActivity.class);
//    i.putIntegerArrayListExtra(DEFAULT_VIEW_IDS, mSeriesEnabled);
//    i.putExtra(GRAPH_START_MS, mGraphView.getGraph().getGraphStart());
//    startActivityForResult(i, CALENDAR_VIEW);
  }

  public TextView getGraphStatusTextView() {
    return mGraphStatus;
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case DIALOG_GRAPH_FILTER:
        mFilterDialog = filterDialog("Show Series");
        return mFilterDialog;
      case DIALOG_GRAPH_CORRELATE:
        Dialog d;
        d = correlateDialog("Correlations");
        return d;
      default:
    }
    return null;
  }

  private Dialog filterDialog(String title) {
    Builder b = new AlertDialog.Builder(mCtx);
    b.setTitle(title);

    mGFLA = new GraphFilterListAdapter(this, mTSC);
    for (int i = 0; i < mTSC.numSeries(); i++) {
      TimeSeries ts = mTSC.getSeriesByIndex(i);
      long id = ts.mRow.mId;
      String name = ts.mRow.mTimeSeriesName;
      String color = ts.mRow.mColor;
      int rank = ts.mRow.mRank;
      mGFLA.addItem(new GraphFilterRow(id, name, color, rank));
    }

    b.setAdapter(mGFLA, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
      }
    });

    b.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        ((GraphActivity) mCtx).graph();
      }
    });
    Dialog d = b.create();

    return d;
  }

  private Dialog correlateDialog(String title) {
    Builder b = new AlertDialog.Builder(mCtx);
    b.setTitle(title);
    b.setMessage(mCorrelator.mOutput);
    b.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
      }
    });
    Dialog d = b.create();
    return d;
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    graph();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    mSeriesEnabled.clear();
    ArrayList<TimeSeries> series = mTSC.getAllEnabledSeries();
    for (int j = 0; j < series.size(); j++) {
      TimeSeries ts = series.get(j);
      if (ts != null)
        mSeriesEnabled.add(new String(""+ts.mRow.mId));
    }

    outState.putStringArrayList(DEFAULT_VIEW_IDS, mSeriesEnabled);
    outState.putLong(GRAPH_START_TS, mGraphView.getGraph().getGraphStart());
    outState.putLong(GRAPH_END_TS, mGraphView.getGraph().getGraphEnd());
    outState.putLong(GRAPH_AGGREGATION, mAggregation);
    super.onSaveInstanceState(outState);
  }

  @Override
  protected void onResume() {
    mGraphView.setColorScheme();
    graph();
    super.onResume();
  }

  @Override
  protected void onPause() {
    super.onPause();
  }
}
