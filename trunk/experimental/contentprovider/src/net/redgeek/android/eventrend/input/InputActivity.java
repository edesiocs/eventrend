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

package net.redgeek.android.eventrend.input;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnTouchListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.ViewFlipper;

import net.redgeek.android.eventrecorder.DateMapCache;
import net.redgeek.android.eventrecorder.IEventRecorderService;
import net.redgeek.android.eventrecorder.TimeSeriesData;
import net.redgeek.android.eventrecorder.TimeSeriesData.TimeSeries;
import net.redgeek.android.eventrend.EvenTrendActivity;
import net.redgeek.android.eventrend.Preferences;
import net.redgeek.android.eventrend.R;
import net.redgeek.android.eventrend.category.CategoryEditActivity;
import net.redgeek.android.eventrend.category.CategoryListAdapter;
import net.redgeek.android.eventrend.category.CategoryRow;
import net.redgeek.android.eventrend.category.CategoryRowView;
import net.redgeek.android.eventrend.util.DateUtil;
import net.redgeek.android.eventrend.util.ProgressIndicator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// TODO:  change everything to content providers
// TODO:  setup listeners for CP changes
public class InputActivity extends EvenTrendActivity {
  // Menu-button IDs
  private static final int MENU_ADD_ID = Menu.FIRST;
  private static final int MENU_GRAPH_ID = Menu.FIRST + 1;
  private static final int MENU_CALENDAR_ID = Menu.FIRST + 2;
  private static final int MENU_EDIT_ID = Menu.FIRST + 3;
  private static final int MENU_PREFS_ID = Menu.FIRST + 4;
  private static final int MENU_HELP_ID = Menu.FIRST + 5;

  // Context menu IDs
  private static final int CONTEXT_EDIT = Menu.FIRST + 10;
  private static final int CONTEXT_MOVE_UP = Menu.FIRST + 11;
  private static final int CONTEXT_MOVE_DOWN = Menu.FIRST + 12;
  private static final int CONTEXT_DELETE = Menu.FIRST + 13;

  // Dialog IDs
  private static final int TIME_DIALOG_ID = 0;
  private static final int DATE_DIALOG_ID = 1;
  private static final int HELP_DIALOG_ID = 2;

  // Generated IDs for flipper listview
  public static final int SCROLL_VIEW_ID_BASE = 1000;
  public static final int LIST_VIEW_ID_BASE = 2000;

  // UI elements
  private ViewFlipper mFlipper;
  private Button mPickDate;
  private Button mPickTime;
  private CheckBox mPickNow;
  private Button mUndo;
  private TextView mTimestampView;
  private ArrayList<ListView> mCategories;
  private ArrayList<CategoryListAdapter> mCLAs;
  private ListView mVisibleCategoriesLayout;

  ProgressIndicator.Titlebar mProgress;
  private GestureDetector mGestureDetector;

  // Trend state:
  public static final String TREND_DOWN_15_GOOD = "trend_down_15_good";
  public static final String TREND_DOWN_15_BAD = "trend_down_15_bad";
  public static final String TREND_DOWN_30_GOOD = "trend_down_30_good";
  public static final String TREND_DOWN_30_BAD = "trend_down_30_bad";
  public static final String TREND_DOWN_45_GOOD = "trend_down_45_good";
  public static final String TREND_DOWN_45_BAD = "trend_down_45_bad";
  public static final String TREND_UP_15_GOOD = "trend_up_15_good";
  public static final String TREND_UP_15_BAD = "trend_up_15_bad";
  public static final String TREND_UP_30_GOOD = "trend_up_30_good";
  public static final String TREND_UP_30_BAD = "trend_up_30_bad";
  public static final String TREND_UP_45_GOOD = "trend_up_45_good";
  public static final String TREND_UP_45_BAD = "trend_up_45_bad";
  public static final String TREND_FLAT = "trend_flat";
  public static final String TREND_FLAT_GOAL = "trend_flat_goal";
  public static final String TREND_DOWN_15 = "trend_down_15";
  public static final String TREND_UP_15 = "trend_up_15";
  public static final String TREND_UNKNOWN = "trend_unknown";
  
  // Content observers
  private Handler mContentChangeHandler = new Handler();
  private TimeSeriesContentObserver mTimeSeriesObserver = null;
  
  // For undo
  private long mLastAddId = -1;
  private float mLastAddValue = 0.0f;
  private long mLastAddTimestamp = 0;
  private TextView mLastAddTextView;
  private CategoryRowView mLastAddRowView;
  private int mContextPosition = -1;
  private Lock mUndoLock;

  private int mOldHour = 0;
  private DateUtil.DateItem mTimestamp;
  private Calendar mCal;

  // From preferences
  private int mHistory;
  private float mSmoothing;
  private float mSensitivity;

  // Listeners
  private OnTouchListener mTouchListener;
  private View.OnClickListener mPickDateListener;
  private View.OnClickListener mPickTimeListener;
  private View.OnClickListener mPickNowListener;
  private View.OnClickListener mUndoListener;
  private DatePickerDialog.OnDateSetListener mDateSetListener;
  private TimePickerDialog.OnTimeSetListener mTimeSetListener;

  // Tasks, handlers, etc
  private Runnable mUpdateNowTime;
  private Handler mNowHandler;

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);

    getPrefs();
    setupTasksAndData();
    setupUI();

    fillCategoryData(-1);

    setCurrentViews(true);
  }

  public TextView getTimestampView() {
    return mTimestampView;
  }

  public long getTimestampSeconds() {
    return mTimestamp.mMillis / DateMapCache.SECOND_MS;
  }

  public long getLastAddId() {
    return mLastAddId;
  }

  // *** background tasks ***/
  @Override
  public void executeNonGuiTask() throws Exception {
  }

  @Override
  public void onFailure(Throwable t) {
  }

  // *** main setup routines ***/
  private void getPrefs() {
    mHistory = Preferences.getHistory(mCtx);
    mSmoothing = Preferences.getSmoothingConstant(mCtx);
    mSensitivity = Preferences.getStdDevSensitivity(mCtx);
  }

  private void setupTasksAndData() {
    mUndoLock = new ReentrantLock();

    startAndBind();
    mTimestamp = new DateUtil.DateItem();
    mCal = Calendar.getInstance();
    mOldHour = mCal.get(Calendar.HOUR_OF_DAY);

    mUpdateNowTime = new Runnable() {
      public void run() {
        mCal.setTimeInMillis(System.currentTimeMillis());
        int newHour = mCal.get(Calendar.HOUR_OF_DAY);

        if (mPickNow.isChecked() == true)
          setTimestampNow();

        mNowHandler.postDelayed(mUpdateNowTime, DateUtil.SECOND_MS);
      }
    };

    mNowHandler = new Handler();
    scheduleUpdateNow();
  }

  private void setupUI() {
    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    setContentView(R.layout.category_list);

    mProgress = new ProgressIndicator.Titlebar(mCtx);

    mCategories = new ArrayList<ListView>();
    mCLAs = new ArrayList<CategoryListAdapter>();

    initListeners();

    mTimestampView = (TextView) findViewById(R.id.entry_timestamp);

    mPickDate = (Button) findViewById(R.id.entry_set_date);
    mPickDate.setOnClickListener(mPickDateListener);

    mPickTime = (Button) findViewById(R.id.entry_set_time);
    mPickTime.setOnClickListener(mPickTimeListener);

    mPickNow = (CheckBox) findViewById(R.id.entry_set_now);
    mPickNow.setOnClickListener(mPickNowListener);

    mUndo = (Button) findViewById(R.id.entry_undo);
    mUndo.setClickable(false);
    mUndo.setTextColor(Color.DKGRAY);
    mUndo.setOnClickListener(mUndoListener);

    mFlipper = (ViewFlipper) findViewById(R.id.view_flipper);
    mFlipper.setInAnimation(AnimationUtils.loadAnimation(this,
        android.R.anim.slide_in_left));
    mFlipper.setOutAnimation(AnimationUtils.loadAnimation(this,
        android.R.anim.slide_out_right));

    setTimestampNow();
    setupGestures();
  }

  private void initListeners() {
    mPickDateListener = new View.OnClickListener() {
      public void onClick(View v) {
        mPickNow.setChecked(false);
        mTimestampView.setBackgroundColor(Color.RED);
        showDialog(DATE_DIALOG_ID);
      }
    };

    mPickTimeListener = new View.OnClickListener() {
      public void onClick(View v) {
        mPickNow.setChecked(false);
        mTimestampView.setBackgroundColor(Color.RED);
        showDialog(TIME_DIALOG_ID);
      }
    };

    mPickNowListener = new View.OnClickListener() {
      public void onClick(View v) {
        setTimestampNow();
      }
    };

    mUndoListener = new View.OnClickListener() {
      public void onClick(View v) {
        undo();
      }
    };

    mDateSetListener = new DatePickerDialog.OnDateSetListener() {
      public void onDateSet(DatePicker view, int year, int monthOfYear,
          int dayOfMonth) {
        mTimestamp.mYear = year;
        mTimestamp.mMonth = monthOfYear;
        mTimestamp.mDay = dayOfMonth;
        updateDisplay();
      }
    };

    mTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
      public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        mTimestamp.mHour = hourOfDay;
        mTimestamp.mMinute = minute;
        updateDisplay();
      }
    };
  }

  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    super.onListItemClick(l, v, position, id);
    mContextPosition = position;
    l.showContextMenu();
  }

  // *** All things gesture-related ***//

  public void setupGestures() {
    mGestureDetector = new GestureDetector(
        new GestureDetector.SimpleOnGestureListener() {
          @Override
          public boolean onFling(MotionEvent e1, MotionEvent e2,
              float velocityX, float velocityY) {
            float deltaX = e2.getRawX() - e1.getRawX();
            float deltaY = e2.getRawY() - e1.getRawY();
            int minSlideWidth = mFlipper.getWidth() / 2;

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
    mFlipper.setLongClickable(true);
    mFlipper.setOnTouchListener(mTouchListener);
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent ev) {
    mGestureDetector.onTouchEvent(ev);
    return super.dispatchTouchEvent(ev);
  }

  // *** some overrides ***/

  @Override
  protected void onPause() {
    scheduleUpdateStop();
    unregisterContentObservers();
    super.onPause();
  }

  @Override
  protected void onResume() {
    scheduleUpdateNow();
    registerContentObservers();
    getPrefs();

    super.onResume();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    switch (requestCode) {
      case ARC_CATEGORY_EDIT:
        // TODO:  only re-draw if the group or rank has changed.
      case ARC_CATEGORY_CREATE:
        switch (resultCode) {
          case CategoryEditActivity.CATEGORY_CREATED:
          default:
            // TODO: figure out why resultCode isn't propogated,
            // display error dialog if the category isn't created
            fillCategoryData(-1);
            setCurrentViews(true);
            break;
        }
        break;
      default:
        break;
    }
  }

  // *** clock ***//

  private void scheduleUpdateNow() {
    mNowHandler.postDelayed(mUpdateNowTime, DateUtil.SECOND_MS);
  }

  private void scheduleUpdateStop() {
    mNowHandler.removeCallbacks(mUpdateNowTime);
  }
  
  private void registerContentObservers() {
    mTimeSeriesObserver = new TimeSeriesContentObserver(mContentChangeHandler);
    mContent.registerContentObserver(TimeSeriesData.TimeSeries.CONTENT_URI, 
        true, mTimeSeriesObserver);
  }

  private void unregisterContentObservers() {
    if (mTimeSeriesObserver != null) {
      mContent.unregisterContentObserver(mTimeSeriesObserver);
      mTimeSeriesObserver = null;
    }
  }

  // *** oh, the menus .... ***//

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    boolean result = super.onCreateOptionsMenu(menu);
    menu.add(0, MENU_ADD_ID, 0, R.string.menu_category_add).setIcon(
        android.R.drawable.ic_menu_add);
    menu.add(0, MENU_GRAPH_ID, 0, R.string.menu_graph)
        .setIcon(R.drawable.graph);
    menu.add(0, MENU_CALENDAR_ID, 0, R.string.menu_calendar).setIcon(
        android.R.drawable.ic_menu_today);
    menu.add(0, MENU_EDIT_ID, 0, R.string.menu_entry_edit).setIcon(
        android.R.drawable.ic_menu_edit);
    menu.add(0, MENU_PREFS_ID, 0, R.string.menu_app_prefs).setIcon(
        android.R.drawable.ic_menu_preferences);
    menu.add(0, MENU_HELP_ID, 0, R.string.menu_app_help).setIcon(
        android.R.drawable.ic_menu_help);
    return result;
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v,
      ContextMenu.ContextMenuInfo menuInfo) {
    menu.add(0, CONTEXT_EDIT, 0, R.string.context_edit_category);
    menu.add(0, CONTEXT_MOVE_UP, 0, R.string.context_move_up);
    menu.add(0, CONTEXT_MOVE_DOWN, 0, R.string.context_move_down);
    menu.add(0, CONTEXT_DELETE, 0, R.string.context_delete_category);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case MENU_ADD_ID:
        createCategory();
        return true;
//      case MENU_EDIT_ID:
//        editEntries();
//        return true;
//      case MENU_GRAPH_ID:
//        graphEntries();
//        return true;
//      case MENU_CALENDAR_ID:
//        calendarView();
//        return true;
//      case MENU_PREFS_ID:
//        editPrefs();
//        return true;
      case MENU_HELP_ID:
        showDialog(HELP_DIALOG_ID);
        return true;
    }
    return super.onOptionsItemSelected(item);
  }

  public boolean onContextItemSelected(MenuItem item) {
    AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item
        .getMenuInfo();

    int position;
    if (menuInfo == null || menuInfo.position < 0) {
      position = mContextPosition;
    } else {
      position = menuInfo.position;
      menuInfo.position = -1;
    }

    int child = mFlipper.getDisplayedChild();
    CategoryListAdapter cla = mCLAs.get(child);

    long catId;
    switch (item.getItemId()) {
      case CONTEXT_EDIT:
        catId = ((CategoryRow) cla.getItem(position)).mId;
        editCategory(catId);
        break;
      case CONTEXT_MOVE_UP:
        if (position > 0)
          swapCategoryPositions(cla, position - 1, position);
        break;
      case CONTEXT_MOVE_DOWN:
        if (position + 1 < cla.getCount())
          swapCategoryPositions(cla, position, position + 1);
        break;
      default:
        return super.onContextItemSelected(item);
    }

    return true;
  }

  // *** dialogs ***//

  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case TIME_DIALOG_ID:
        return new TimePickerDialog(this, mTimeSetListener, mTimestamp.mHour,
            mTimestamp.mMinute, false);
      case DATE_DIALOG_ID:
        return new DatePickerDialog(this, mDateSetListener, mTimestamp.mYear,
            mTimestamp.mMonth, mTimestamp.mDay);
      case HELP_DIALOG_ID:
        String str = getResources().getString(R.string.overview);
        return mDialogUtil.newOkDialog("Help", str);
    }
    return null;
  }

  @Override
  protected void onPrepareDialog(int id, Dialog dialog) {
    switch (id) {
      case TIME_DIALOG_ID:
        ((TimePickerDialog) dialog).updateTime(mTimestamp.mHour,
            mTimestamp.mMinute);
        break;
      case DATE_DIALOG_ID:
        ((DatePickerDialog) dialog).updateDate(mTimestamp.mYear,
            mTimestamp.mMonth, mTimestamp.mDay);
        break;
    }
  }

  // *** Filling out the main listview ***//

  private void setCurrentViews(boolean animate) {
    // These can't be set until the views have been populated from
    // DB in fillCategoryData()
    mVisibleCategoriesLayout = (ListView) mFlipper.getCurrentView();
    if (mVisibleCategoriesLayout != null) {
      mVisibleCategoriesLayout.setOnCreateContextMenuListener(this);
      if (animate == true)
        slideDown(mVisibleCategoriesLayout, mCtx);
    }
  }

  private void fillCategoryData(int switchToView) {
    int list = 0;
    HashMap<String, Integer> hm = new HashMap<String, Integer>();
    CategoryListAdapter cla = null;
    ListView lv = null;
    TimeSeries ts;

    int defaultGroupId = 0;

    mCLAs.clear();
    mCategories.clear();
    mFlipper.removeAllViews();
    
    Uri timeSeries = TimeSeriesData.TimeSeries.CONTENT_URI;
    Cursor c = managedQuery(timeSeries, null, null, null, null);
    if (c.moveToFirst()) {
      int count = c.getCount();
      for (int i = 0; i < count; i++) {
        String group = c.getString(c.getColumnIndex(TimeSeriesData.TimeSeries.GROUP_NAME));
      
        Integer listNum = hm.get(group);
        if (listNum == null) {
          listNum = new Integer(list);
          cla = new CategoryListAdapter(this);
          mCLAs.add(cla);
          lv = new ListView(this);
          lv.setId(LIST_VIEW_ID_BASE + i);
          mCategories.add(lv);
          mFlipper.addView(lv);
          hm.put(group, listNum);
          list++;
        }

        cla = mCLAs.get(listNum.intValue());
        
        CategoryRow row = new CategoryRow(c);
        row.mTimestamp = mTimestamp.mMillis;
        row.mGroup = group;
        
        cla.addItem(row);
        c.moveToNext();
      }
    }

    for (int i = 0; i < list; i++) {
      lv = mCategories.get(i);
      cla = mCLAs.get(i);
      lv.setAdapter(cla);
    }

    if (switchToView < 0)
      switchToView = defaultGroupId;

    if (switchToView >= 0 && switchToView < mFlipper.getChildCount())
      mFlipper.setDisplayedChild(switchToView);
  }

  class TimeSeriesContentObserver extends ContentObserver {
    public TimeSeriesContentObserver(Handler h) {
      super(h);
    }

    public void onChange(boolean selfChange) {
      // TODO: implement
    }
  }

  public void redrawSyntheticViews() {
    for (int i = 0; i < mFlipper.getChildCount(); i++) {
      ListView lv = (ListView) mFlipper.getChildAt(i);

      for (int j = 0; j < lv.getChildCount(); j++) {
        CategoryRowView row = (CategoryRowView) lv.getChildAt(j);
        if (row.mRow.mType.toLowerCase().equals(TimeSeries.TYPE_SYNTHETIC) == true) {
          row.populateFields();
//          CategoryRowView.setLayoutAnimationSlideOutLeftIn(row, mCtx);
        }
      }
    }
  }

  // *** Transitions elsewhere ... ***//

  private void createCategory() {
    Uri uri = TimeSeriesData.TimeSeries.CONTENT_URI;
    Intent i = new Intent(Intent.ACTION_INSERT, uri);
    startActivityForResult(i, ARC_CATEGORY_CREATE);
  }

  private void editCategory(long catId) {
      Uri uri = ContentUris.withAppendedId(TimeSeriesData.TimeSeries.CONTENT_URI, catId);
      Intent i = new Intent(Intent.ACTION_EDIT, uri);
      startActivityForResult(i, ARC_CATEGORY_EDIT);
  }

//  private void editEntries() {
//    Intent i = new Intent(this, EntryListActivity.class);
//    mTSC.clearSeriesLocking();
//    startActivityForResult(i, ENTRY_LIST);
//  }
//
//  private void editPrefs() {
//    Intent i = new Intent(this, Preferences.class);
//    startActivityForResult(i, PREFS_EDIT);
//  }
//
//  private void graphEntries() {
//    Intent i = new Intent(this, GraphActivity.class);
//    ArrayList<Integer> catIds = new ArrayList<Integer>();
//
//    ArrayList<TimeSeries> series = mTSC.getAllEnabledSeries();
//    for (int j = 0; j < series.size(); j++) {
//      TimeSeries ts = series.get(j);
//      if (ts.isEnabled())
//        catIds.add(new Integer((int) ts.getDbRow().getId()));
//    }
//
//    i.putIntegerArrayListExtra(VIEW_DEFAULT_CATIDS, catIds);
//    startActivityForResult(i, GRAPH_VIEW);
//  }
//
//  private void calendarView() {
//    Intent i = new Intent(this, CalendarActivity.class);
//    ArrayList<Integer> catIds = new ArrayList<Integer>();
//
//    ArrayList<TimeSeries> series = mTSC.getAllEnabledSeries();
//    for (int j = 0; j < series.size(); j++) {
//      TimeSeries ts = series.get(j);
//      catIds.add(new Integer((int) ts.getDbRow().getId()));
//    }
//
//    i.putIntegerArrayListExtra(VIEW_DEFAULT_CATIDS, catIds);
//    startActivityForResult(i, CALENDAR_VIEW);
//  }

  // *** display update routines ***/

  public void setTimestampNow() {
    mCal.setTimeInMillis(System.currentTimeMillis());
    mTimestamp.setTo(mCal);
    updateDisplay();
  }

  private void updateDisplay() {
    mCal.set(Calendar.YEAR, mTimestamp.mYear);
    mCal.set(Calendar.MONTH, mTimestamp.mMonth);
    mCal.set(Calendar.DAY_OF_MONTH, mTimestamp.mDay);
    mCal.set(Calendar.HOUR_OF_DAY, mTimestamp.mHour);
    mCal.set(Calendar.MINUTE, mTimestamp.mMinute);
    mCal.set(Calendar.SECOND, mTimestamp.mSecond);
    mTimestamp.setTo(mCal);

    mTimestampView.setText(DateUtil.toTimestamp(mCal));
    mTimestampView.setTextColor(Color.LTGRAY);
    if (mPickNow.isChecked() == true) {
      mTimestampView.setBackgroundColor(Color.BLACK);
    } else {
      mTimestampView.setBackgroundColor(Color.RED);
    }
  }

  // used for undo
  public void setLastAdd(long id, float val, long timestamp, TextView textView,
      CategoryRowView rowView) {
    while (mUndoLock.tryLock() == false) {
    }
    mUndo.setClickable(true);
    mUndo.setTextColor(Color.BLACK);
    mLastAddId = id;
    mLastAddValue = val;
    mLastAddTimestamp = timestamp;
    mLastAddTextView = textView;
    mLastAddRowView = rowView;
    mUndoLock.unlock();
  }

  public void undo() {
//    while (mUndoLock.tryLock() == false) {
//    }
//    EntryDbTable.Row entry = getDbh().fetchEntry(mLastAddId);
//    if (entry == null)
//      return;
//
//    CategoryDbTable.Row cat = getDbh().fetchCategory(entry.getCategoryId());
//    if (cat == null)
//      return;
//
//    float oldValue = entry.getValue();
//    String newValueStr;
//    if (entry.getNEntries() == 1) {
//      getDbh().deleteEntry(mLastAddId);
//      newValueStr = "(deleted)";
//    } else {
//      newValueStr = "" + mLastAddValue;
//      entry.setValue(mLastAddValue);
//      entry.setNEntries(entry.getNEntries() - 1);
//      getDbh().updateEntry(entry);
//    }
//    mUndoLock.unlock();
//
//    mTSC.updateCategoryTrend(cat.getId());
//
//    String shortStr = "Undid @ " + DateUtil.toShortTimestamp(mLastAddTimestamp)
//        + ": " + oldValue + " -> " + newValueStr;
//    String longStr = "Undid " + cat.getCategoryName() + " @ "
//        + DateUtil.toTimestamp(mLastAddTimestamp) + ": " + oldValue + " -> "
//        + newValueStr;
//    mLastAddTextView.setText(shortStr);
//    Toast.makeText(this, longStr, Toast.LENGTH_LONG).show();
//    slideOutRightIn(mLastAddRowView, getCtx());
//
//    mUndo.setClickable(false);
//    mUndo.setTextColor(Color.DKGRAY);
  }

  public IEventRecorderService getRecorderService() {
    return mRecorderService;
  }  

  public DateMapCache getDateMapCache() {
    return mDateMapCache;
  }  

  // *** Animations ***//
  private void swapCategoryPositions(CategoryListAdapter cla, int higher,
      int lower) {
    ContentValues values = new ContentValues();
    Uri uri;
    CategoryRow above = (CategoryRow) cla.getItem(higher);
    CategoryRow below = (CategoryRow) cla.getItem(lower);

    int rank = below.mRank;
    below.mRank = above.mRank;
    above.mRank = rank;

    values.put(TimeSeries.RANK, above.mRank);
    uri = ContentUris.withAppendedId(TimeSeriesData.TimeSeries.CONTENT_URI, above.mId);
    getContentResolver().update(uri, values, null, null);

    values.put(TimeSeries.RANK, below.mRank);
    uri = ContentUris.withAppendedId(TimeSeriesData.TimeSeries.CONTENT_URI, below.mId);
    getContentResolver().update(uri, values, null, null);

    CategoryRowView top = (CategoryRowView) mVisibleCategoriesLayout
        .getChildAt(higher);
    CategoryRowView bottom = (CategoryRowView) mVisibleCategoriesLayout
        .getChildAt(lower);

//    swapUpDown(top, bottom, mCtx);    
    fillCategoryData(mFlipper.getDisplayedChild());
    setCurrentViews(true);
  }

  public static void slideDown(ViewGroup group, Context ctx) {
    AnimationSet set = new AnimationSet(true);

    Animation animation = new AlphaAnimation(0.0f, 1.0f);
    animation.setDuration(100);
    set.addAnimation(animation);

    animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
        Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, -1.0f,
        Animation.RELATIVE_TO_SELF, 0.0f);

    animation.setDuration(500);
    set.addAnimation(animation);

    LayoutAnimationController controller = new LayoutAnimationController(set,
        0.25f);
    group.setLayoutAnimation(controller);
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

    setCurrentViews(false);
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

    setCurrentViews(false);
  }

  public static void swapUpDown(ViewGroup top, ViewGroup bottom, Context ctx) {
    AnimationSet s1 = new AnimationSet(true);
    AnimationSet s2 = new AnimationSet(true);

    Animation moveUp = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
        Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
        Animation.RELATIVE_TO_SELF, -1.0f);
    moveUp.setStartOffset(0);
    moveUp.setDuration(500);
    s1.addAnimation(moveUp);
    s1.setDuration(500);

    Animation moveDown = new TranslateAnimation(Animation.RELATIVE_TO_SELF,
        0.0f, Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
        0.0f, Animation.RELATIVE_TO_SELF, 1.0f);
    moveUp.setStartOffset(0);
    moveUp.setDuration(500);
    s2.addAnimation(moveDown);
    s2.setDuration(500);

    LayoutAnimationController c1 = new LayoutAnimationController(s1, 0.25f);
    LayoutAnimationController c2 = new LayoutAnimationController(s2, 0.25f);

    top.setLayoutAnimation(c2);
    bottom.setLayoutAnimation(c1);
  }

  public static void slideOutRightIn(ViewGroup group, Context ctx) {
    AnimationSet set = new AnimationSet(true);

    Animation animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF,
        0.0f, Animation.RELATIVE_TO_SELF, 1.0f, Animation.RELATIVE_TO_SELF,
        0.0f, Animation.RELATIVE_TO_SELF, 0.0f);

    animation.setStartOffset(0);
    animation.setDuration(500);
    animation.setRepeatCount(1);
    animation.setRepeatMode(Animation.REVERSE);
    set.addAnimation(animation);
    set.setDuration(500);

    LayoutAnimationController controller = new LayoutAnimationController(set,
        0.25f);
    controller.setOrder(LayoutAnimationController.ORDER_NORMAL);
    group.setLayoutAnimation(controller);
  }
}