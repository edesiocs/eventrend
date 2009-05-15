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

package net.redgeek.android.eventrend;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.app.AlertDialog.Builder;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.ViewFlipper;

import net.redgeek.android.eventgrapher.GraphActivity;
import net.redgeek.android.eventrecorder.DateMapCache;
import net.redgeek.android.eventrecorder.IEventRecorderService;
import net.redgeek.android.eventrecorder.TimeSeriesData;
import net.redgeek.android.eventrecorder.TimeSeriesData.DateMap;
import net.redgeek.android.eventrecorder.TimeSeriesData.TimeSeries;
import net.redgeek.android.eventrend.category.CategoryAdvancedEditActivity;
import net.redgeek.android.eventrend.category.CategoryRow;
import net.redgeek.android.eventrend.category.CategoryRowView;
import net.redgeek.android.eventrend.util.DateUtil;
import net.redgeek.android.eventrend.util.GUITaskQueue;
import net.redgeek.android.eventrend.util.ProgressIndicator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class InputActivity extends EvenTrendActivity {
  // Menu-button IDs
  private static final int MENU_ADD_ID = Menu.FIRST;
  private static final int MENU_VISUALIZE_ID = Menu.FIRST + 1;
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
  private static final int SERVICE_CONNECTING_DIALOG_ID = 3;
  private static final int DELETE_DIALOG_ID = 4;
  private static final int EARLY_ENTRY_DIALOG_ID = 5;
  private static final int ERROR_DIALOG_ID = 10;

  // Generated IDs for flipper scrollviews
  public static final int SCROLL_VIEW_ID_BASE = 1000;
  public static final int LINEAR_LAYOUT_ID_BASE = 2000;

  // UI elements
  private ViewFlipper mFlipper;
  private Button mPickDate;
  private Button mPickTime;
  private CheckBox mPickNow;
  private Button mUndo;
  private TextView mTimestampView;
  private ArrayList<LinearLayout> mCategories;
  private LinearLayout mVisibleCategoriesLayout;

  ProgressIndicator.Titlebar mProgress;
  ProgressIndicator.DialogSoft mProgressBox;
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
  private long mLastAddTimeSeriesId = -1;
  private long mLastAddDatapointId = -1;
  private CategoryRowView mLastAddRowView;
  private int mContextPosition = -1;
  private Lock mUndoLock;

  private int mOldHour = 0;
  private DateUtil.DateItem mTimestamp;
  private Calendar mCal;
  
  // For deletion:
  private String mDeleteCategoryName = "";
  private long mDeleteCategoryId = 0;

  // For error dialogs:
  private String mDialogErrorTitle = "";
  private String mDialogErrorMsg = "";
  
  // Listeners
  private OnTouchListener mTouchListener;
  private View.OnClickListener mPickDateListener;
  private View.OnClickListener mPickTimeListener;
  private View.OnClickListener mPickNowListener;
  private View.OnClickListener mUndoListener;
  private View.OnLongClickListener mRowSelectListener;
  private DatePickerDialog.OnDateSetListener mDateSetListener;
  private TimePickerDialog.OnTimeSetListener mTimeSetListener;

  // Tasks, handlers, etc
  private Runnable mUpdateNowTime;
  private Handler mNowHandler;

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);

    mProgress = new ProgressIndicator.Titlebar(mCtx);
    mProgressBox = new ProgressIndicator.DialogSoft(mCtx, SERVICE_CONNECTING_DIALOG_ID);
    
    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    setContentView(R.layout.category_list);

    setupTasksAndData();
    
    setupUI();
    fillCategoryData(-1);
    setCurrentViews(true);
    
    scheduleUpdateNow();
  }

  public TextView getTimestampView() {
    return mTimestampView;
  }

  public long getTimestampSeconds() {
    return mTimestamp.mMillis / DateMap.SECOND_MS;
  }

  // *** background tasks ***/
  @Override
  public void executeNonGuiTask() throws Exception {
    startAndBind();
  }
  
  @Override
  public void afterExecute() {
  }

  @Override
  public void onFailure(Throwable t) {
  }

  // *** main setup routines ***/
  private void setupTasksAndData() {
    mUndoLock = new ReentrantLock();
    
    GUITaskQueue.getInstance().addTask(mProgressBox, this);
    
    mTimestamp = new DateUtil.DateItem();
    mCal = Calendar.getInstance();
    mOldHour = mCal.get(Calendar.HOUR_OF_DAY);

    mUpdateNowTime = new Runnable() {
      public void run() {
        mCal.setTimeInMillis(System.currentTimeMillis());
        int newHour = mCal.get(Calendar.HOUR_OF_DAY);

        if (mPickNow.isChecked() == true)
          setTimestampNow();

        mNowHandler.postDelayed(mUpdateNowTime, DateMap.SECOND_MS);
      }
    };

    mNowHandler = new Handler();
    
    mCategories = new ArrayList<LinearLayout>();
  }

  private void setupUI() {
    initListeners();

    mTimestampView = (TextView) findViewById(R.id.entry_timestamp);

    mPickDate = (Button) findViewById(R.id.entry_set_date);
    mPickDate.setOnClickListener(mPickDateListener);

    mPickTime = (Button) findViewById(R.id.entry_set_time);
    mPickTime.setOnClickListener(mPickTimeListener);

    mPickNow = (CheckBox) findViewById(R.id.entry_set_now);
    mPickNow.setOnClickListener(mPickNowListener);

    mUndo = (Button) findViewById(R.id.entry_undo);
    mUndo.setTextColor(Color.DKGRAY);
    mUndo.setOnClickListener(mUndoListener);
    mUndo.setClickable(false);

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
        long now = System.currentTimeMillis();
        long delta = now - mTimestamp.mMillis;
        long month = (long)DateMap.MONTH_SECS * (long)DateMap.SECOND_MS;
        if (delta > month) {
          showDialog(EARLY_ENTRY_DIALOG_ID);
        }
      }
    };

    mTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
      public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        mTimestamp.mHour = hourOfDay;
        mTimestamp.mMinute = minute;
        updateDisplay();
      }
    };

    mRowSelectListener = new View.OnLongClickListener() {
      @Override
      public boolean onLongClick(View v) {
        CategoryRowView row = (CategoryRowView) v;
        
        int child = mFlipper.getDisplayedChild();
        LinearLayout list = mCategories.get(child);

        mContextPosition = -1;
        for (int i = 0; i < list.getChildCount(); i++) {
          CategoryRowView view = (CategoryRowView) list.getChildAt(i);
          if (view == row) {
            mContextPosition = i;
          }
        }
        
        return false;
      }
    };
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
//    mFlipper.setLongClickable(true);
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
    super.onResume();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    switch (requestCode) {
      case ARC_CATEGORY_EDIT:
        // TODO:  only re-draw if the group or rank has changed.
        switch (resultCode) {
          case CategoryAdvancedEditActivity.CATEGORY_MODIFIED:
            break;
          default:
            mDialogErrorTitle = "Error";
            mDialogErrorMsg = "Error editing category";
            showDialog(ERROR_DIALOG_ID);
            break;
        }
        break;
      case ARC_CATEGORY_CREATE:
        switch (resultCode) {
          case CategoryAdvancedEditActivity.CATEGORY_CREATED:
            break;
          default:
            mDialogErrorTitle = "Error";
            mDialogErrorMsg = "Error creating category";
            showDialog(ERROR_DIALOG_ID);
            break;
        }
        break;
      default:
        break;
    }
    fillCategoryData(-1);
    setCurrentViews(true);
  }

  // *** clock ***//

  private void scheduleUpdateNow() {
    mNowHandler.postDelayed(mUpdateNowTime, DateMap.SECOND_MS);
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
    menu.add(0, MENU_VISUALIZE_ID, 0, R.string.menu_visualize)
        .setIcon(R.drawable.graph);
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
      case MENU_EDIT_ID:
        editEntries();
        return true;
      case MENU_VISUALIZE_ID:
        visualizeEntries();
        return true;
      case MENU_PREFS_ID:
        editPrefs();
        return true;
      case MENU_HELP_ID:
        showDialog(HELP_DIALOG_ID);
        return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    CategoryRowView rowView;
    CategoryRow row;

    int child = mFlipper.getDisplayedChild();
    LinearLayout list = mCategories.get(child);
    rowView = (CategoryRowView) list.getChildAt(mContextPosition);
    row = rowView.mRow;

    long catId;
    switch (item.getItemId()) {
      case CONTEXT_EDIT:
        catId = row.mId;
        editCategory(catId);
        break;
      case CONTEXT_MOVE_UP:
        if (mContextPosition > 0)
          swapCategoryPositions(list, mContextPosition - 1, mContextPosition);
        break;
      case CONTEXT_MOVE_DOWN:
        if (mContextPosition + 1 < list.getChildCount())
          swapCategoryPositions(list, mContextPosition, mContextPosition + 1);
        break;
      case CONTEXT_DELETE:
        mDeleteCategoryId = row.mId;
        mDeleteCategoryName = row.mTimeSeriesName;
        showDialog(DELETE_DIALOG_ID);
        break;      
      default:
        return super.onContextItemSelected(item);
    }

    return true;
  }

  // *** dialogs ***//

  @Override
  protected Dialog onCreateDialog(int id) {
    String title;
    String msg;
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
      case SERVICE_CONNECTING_DIALOG_ID:
        Dialog d = mDialogUtil.newProgressDialog(
            "Connecting to recorder service ... "
            + "note this can take a while if the service is busy calculating "
            + "stats.");
        d.setCancelable(false);
        return d;
      case DELETE_DIALOG_ID:
        title = "Delete " + mDeleteCategoryName + "?";
        msg = "All associated entries will also be deleted!";
        return deleteDialog(title, msg);
      case EARLY_ENTRY_DIALOG_ID:
        title = "Warning";
        msg = "Adding an entry far in the past can result in a very long delay "
          + "as statistics are re-calculated, particularly if 'zerofill' is "
          + "configured for an entry.";
        return mDialogUtil.newOkDialog(title, msg);        
      case ERROR_DIALOG_ID:
        return mDialogUtil.newOkDialog(mDialogErrorTitle, mDialogErrorMsg);
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

  private Dialog deleteDialog(String title, String msg) {
    Builder b = new AlertDialog.Builder(mCtx);
    b.setTitle(title);
    b.setMessage(msg);
    b.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        deleteCategory(mDeleteCategoryId);
        mDeleteCategoryId = 0;
        mDeleteCategoryName = "";
      }
    });
    b.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
      }
    });
    Dialog d = b.create();
    return d;
  }

  // *** Filling out the main listview ***//

  private void setCurrentViews(boolean animate) {
    // These can't be set until the views have been populated from
    // DB in fillCategoryData()
    ScrollView sv = (ScrollView) mFlipper.getCurrentView();
    if (sv != null) {
      mVisibleCategoriesLayout = (LinearLayout) sv.getChildAt(0);
      if (mVisibleCategoriesLayout != null) {
        mVisibleCategoriesLayout.setOnCreateContextMenuListener(this);
        if (animate == true)
          slideDown(mVisibleCategoriesLayout, mCtx);
      }
    }
  }

  private void fillCategoryData(int switchToView) {
    int list = 0;
    HashMap<String, Integer> hm = new HashMap<String, Integer>();
    ScrollView sv = null;
    LinearLayout ll = null;
    TimeSeries ts;

    int defaultGroupId = 0;

    mCategories.clear();
    mFlipper.removeAllViews();
    
    Uri timeSeries = TimeSeriesData.TimeSeries.CONTENT_URI;
    Cursor c = managedQuery(timeSeries, null, null, null, TimeSeries.DEFAULT_SORT_ORDER);
    if (c.moveToFirst()) {
      int count = c.getCount();
      for (int i = 0; i < count; i++) {
        String group = c.getString(c.getColumnIndex(TimeSeriesData.TimeSeries.GROUP_NAME));
      
        Integer listNum = hm.get(group);
        if (listNum == null) {
          listNum = new Integer(list);
          
          ll = new LinearLayout(this);
          ll.setOrientation(LinearLayout.VERTICAL);
          ll.setId(LINEAR_LAYOUT_ID_BASE + i);

          sv = new ScrollView(this);
          sv.setId(SCROLL_VIEW_ID_BASE+1);
          sv.addView(ll);
          
          mCategories.add(ll);
          mFlipper.addView(sv);
          hm.put(group, listNum);
          list++;
        }

        CategoryRow row = new CategoryRow(c);
        row.mTimestamp = mTimestamp.mMillis;
        row.mGroup = group;

        CategoryRowView rowView = new CategoryRowView(this, row);
        rowView.setLongClickable(true);
        rowView.setOnLongClickListener(mRowSelectListener);

        ll = mCategories.get(listNum.intValue());
        ll.addView(rowView);

        c.moveToNext();
      }
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
    for (int i = 0; i < mCategories.size(); i++) {
      LinearLayout list = mCategories.get(i);
      
      for (int j = 0; j < list.getChildCount(); j++) {
        CategoryRowView row = (CategoryRowView) list.getChildAt(j);
        if (row.mRow.mType.toLowerCase().equals(TimeSeries.TYPE_SYNTHETIC) == true) {
          row.populateFields(row.mRow);
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

  private void deleteCategory(long catId) {
    Uri uri = ContentUris.withAppendedId(TimeSeriesData.TimeSeries.CONTENT_URI, catId);
    int count = getContentResolver().delete(uri, null, null);
    if (count != 1) {
      mDialogErrorTitle = "Error";
      mDialogErrorMsg = "Error deleting category: " + count;
      showDialog(ERROR_DIALOG_ID);
    }
    fillCategoryData(-1);
    setCurrentViews(true);
  }

  private void editEntries() {
//    Intent i = new Intent(this, EntryListActivity.class);
//    mTSC.clearSeriesLocking();
//    startActivityForResult(i, ENTRY_LIST);
  }

  private void editPrefs() {
    Intent i = new Intent(this, Preferences.class);
    startActivityForResult(i, ARC_PREFS_EDIT);
  }

  private void visualizeEntries() {
    Uri uri = TimeSeries.CONTENT_URI;
    Intent i = new Intent(Intent.ACTION_VIEW, uri);
    
    ArrayList<String> catIds = new ArrayList<String>();
    
    int child = mFlipper.getDisplayedChild();
    LinearLayout list = mCategories.get(child);
    for (int j = 0; j < list.getChildCount(); j++) {
      String catId = Long.toString(((CategoryRowView) list.getChildAt(j)).mRow.mId);
      catIds.add(catId);
    }
    i.putStringArrayListExtra(GraphActivity.VISUALIZATION_VIEW_IDS, catIds);
    startActivityForResult(i, ARC_VISUALIZE_VIEW);
  }
  
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
  public void setLastAdd(long tsId, long dpId, CategoryRowView view) {
    while (mUndoLock.tryLock() == false) {
    }
    mUndo.setClickable(true);
    mUndo.setTextColor(Color.BLACK);
    mLastAddTimeSeriesId = tsId;
    mLastAddDatapointId = dpId;
    mLastAddRowView = view;
    mUndoLock.unlock();
  }

  public void undo() {
    while (mUndoLock.tryLock() == false) {
    }

    mUndo.setClickable(false);
    mUndo.setTextColor(Color.DKGRAY);
    
    if (mLastAddTimeSeriesId < 1 || mLastAddDatapointId < 1 || mLastAddRowView == null) {
      mDialogErrorTitle = "Error";
      mDialogErrorMsg = "There is no recent entry to undo.";
      showDialog(ERROR_DIALOG_ID);
      return;
    }
    
    Uri uri = ContentUris.withAppendedId(
        TimeSeriesData.TimeSeries.CONTENT_URI, mLastAddTimeSeriesId).buildUpon()
        .appendPath("datapoints").appendPath(""+mLastAddDatapointId).build();
    int count = getContentResolver().delete(uri, null, null);
    mUndoLock.unlock();
    mLastAddRowView.populateFields(mLastAddRowView.mRow);

    if (count != 1) {
      mDialogErrorTitle = "Error";
      mDialogErrorMsg = "Error undoing add: " + count;
      showDialog(ERROR_DIALOG_ID);
    } else {    
      slideOutRightIn(mLastAddRowView, mCtx);    
    }
  }

  public IEventRecorderService getRecorderService() {
    return mRecorderService;
  }  

  public DateMapCache getDateMapCache() {
    return mDateMapCache;
  }  

  // *** Animations ***//
  private void swapCategoryPositions(LinearLayout list, int higher,
      int lower) {
    ContentValues values = new ContentValues();
    Uri uri;
    CategoryRowView aboveView = (CategoryRowView) list.getChildAt(higher);
    CategoryRowView belowView = (CategoryRowView) list.getChildAt(lower);    
    
    CategoryRow above = aboveView.mRow;
    CategoryRow below = belowView.mRow;

    int rank = below.mRank;
    below.mRank = above.mRank;
    above.mRank = rank;

    values.put(TimeSeries.RANK, above.mRank);
    uri = ContentUris.withAppendedId(TimeSeriesData.TimeSeries.CONTENT_URI, above.mId);
    getContentResolver().update(uri, values, null, null);

    values.put(TimeSeries.RANK, below.mRank);
    uri = ContentUris.withAppendedId(TimeSeriesData.TimeSeries.CONTENT_URI, below.mId);
    getContentResolver().update(uri, values, null, null);

    CategoryRowView top = (CategoryRowView) mVisibleCategoriesLayout.getChildAt(higher);
    CategoryRowView bottom = (CategoryRowView) mVisibleCategoriesLayout.getChildAt(lower);

    swapUpDown(top, bottom, mCtx);    
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