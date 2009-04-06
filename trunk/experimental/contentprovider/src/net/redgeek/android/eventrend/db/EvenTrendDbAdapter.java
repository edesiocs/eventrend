package net.redgeek.android.eventrend.db;

import java.util.Calendar;

import net.redgeek.android.eventrend.importing.CSV;
import net.redgeek.android.eventrend.util.DateUtil;
import net.redgeek.android.eventrend.util.DateUtil.Period;
import net.redgeek.android.timeseries.CategoryDbTable;
import net.redgeek.android.timeseries.EntryDbTable;
import net.redgeek.android.timeseries.CategoryDbTable.Row;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Contains all the DB/SQL calls necessary for the program. Really needs
 * documentation.
 * 
 * @author barclay
 */
public interface EvenTrendDbAdapter {
  public EvenTrendDbAdapter open() throws SQLException;

  public void close();

  public String flattenDB();

  // Category-focused Operations
  public long createCategory(CategoryDbTable.Row category);

  public boolean deleteCategory(long rowId);

  public boolean deleteAllCategories();

  public Cursor fetchAllCategories();

  public Cursor fetchAllSynthetics();

  public Cursor fetchAllGroups();

  public CategoryDbTable.Row fetchCategory(long rowId);

  public CategoryDbTable.Row fetchCategory(String category);

  public long fetchCategoryId(String category);

  public int fetchCategoryMaxRank();

  public boolean updateCategory(CategoryDbTable.Row category);

  public boolean updateCategory(long id, ContentValues args);

  public boolean updateCategoryRank(long rowId, int rank);

  public boolean updateCategoryLastValue(long rowId, float value);

  public boolean updateCategoryTrend(long catId, String trendStr, float newTrend);

  // Entry-focused Operations
  public Cursor fetchAllEntries();

  public boolean deleteAllEntries();

  public boolean deleteCategoryEntries(long catId);

  public Cursor fetchCategoryEntries(long catId);

  public Cursor fetchEntriesRange(long milliStart, long milliEnd);

  public Cursor fetchCategoryEntriesRange(long catId, long milliStart,
      long milliEnd);

  public long createEntry(EntryDbTable.Row entry);

  public boolean deleteEntry(long rowId);

  public Cursor fetchRecentEntries(int nItems, int skip);

  public Cursor fetchRecentEntries(int nItems, long catId, int skip);

  public EntryDbTable.Row fetchLastCategoryEntry(long catId);

  public EntryDbTable.Row fetchCategoryEntryInPeriod(long catId, long period,
      long date_ms);

  public EntryDbTable.Row fetchEntry(long rowId);

  public boolean updateEntry(EntryDbTable.Row entry);

  public Cursor fetchRecentCategoryEntries(long catId, int nItems);

  public static class SqlAdapter implements EvenTrendDbAdapter {
    private static final String TAG = "EvenTrendDbAdapter";
    protected static final String DATABASE_NAME = "data";
    protected static final int DATABASE_VERSION = 3;

    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    private Context mCtx;
    private Calendar mCal;

    private static class DatabaseHelper extends SQLiteOpenHelper {
      DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
      }
     
      @Override
      public void onCreate(SQLiteDatabase db) {
        db.execSQL(CategoryDbTable.TABLE_CREATE);
        db.execSQL(EntryDbTable.TABLE_CREATE);
        
        // Install some defaults:
        ContentValues args = new ContentValues();
        args.put(CategoryDbTable.KEY_GROUP_NAME, "Health");
        args.put(CategoryDbTable.KEY_CATEGORY_NAME, "Feel free to edit or delete "
            + "these sample categories. There's a lot of help in the program and "
            + "on the website, click Menu > Help for details.");
        args.put(CategoryDbTable.KEY_DEFAULT_VALUE, 5.0f);
        args.put(CategoryDbTable.KEY_LAST_VALUE, 5.0f);
        args.put(CategoryDbTable.KEY_LAST_TREND, 0.0f);
        args.put(CategoryDbTable.KEY_INCREMENT, 1.0f);
        args.put(CategoryDbTable.KEY_GOAL, 10.0f);
        args.put(CategoryDbTable.KEY_TYPE, "Sum");
        args.put(CategoryDbTable.KEY_COLOR, "#cccccc");
        args.put(CategoryDbTable.KEY_PERIOD_MS, 0);
        args.put(CategoryDbTable.KEY_RANK, 1);
        args.put(CategoryDbTable.KEY_TREND_STATE, CategoryDbTable.KEY_TREND_UNKNOWN);
        args.put(CategoryDbTable.KEY_INTERPOLATION, "Linear");
        args.put(CategoryDbTable.KEY_ZEROFILL, false);
        args.put(CategoryDbTable.KEY_SYNTHETIC, false);
        args.put(CategoryDbTable.KEY_FORMULA, "");
        db.insert(CategoryDbTable.TABLE_NAME, null, args);
        
        args = new ContentValues();
        args.put(CategoryDbTable.KEY_GROUP_NAME, "Health");
        args.put(CategoryDbTable.KEY_CATEGORY_NAME, "Weight");
        args.put(CategoryDbTable.KEY_DEFAULT_VALUE, 150.0f);
        args.put(CategoryDbTable.KEY_LAST_VALUE, 150.0f);
        args.put(CategoryDbTable.KEY_LAST_TREND, 0.0f);
        args.put(CategoryDbTable.KEY_INCREMENT, 0.5f);
        args.put(CategoryDbTable.KEY_GOAL, 150.f);
        args.put(CategoryDbTable.KEY_TYPE, "Average");
        args.put(CategoryDbTable.KEY_COLOR, "#00ff00");
        args.put(CategoryDbTable.KEY_PERIOD_MS, DateUtil.DAY_MS);
        args.put(CategoryDbTable.KEY_RANK, 2);
        args.put(CategoryDbTable.KEY_TREND_STATE, CategoryDbTable.KEY_TREND_UNKNOWN);
        args.put(CategoryDbTable.KEY_INTERPOLATION, "Cubic");
        args.put(CategoryDbTable.KEY_ZEROFILL, false);
        args.put(CategoryDbTable.KEY_SYNTHETIC, false);
        args.put(CategoryDbTable.KEY_FORMULA, "");
        db.insert(CategoryDbTable.TABLE_NAME, null, args);

        args = new ContentValues();
        args.put(CategoryDbTable.KEY_GROUP_NAME, "Health");
        args.put(CategoryDbTable.KEY_CATEGORY_NAME, "Body Fat %");
        args.put(CategoryDbTable.KEY_DEFAULT_VALUE, 25.0f);
        args.put(CategoryDbTable.KEY_LAST_VALUE, 25.0f);
        args.put(CategoryDbTable.KEY_LAST_TREND, 0.0f);
        args.put(CategoryDbTable.KEY_INCREMENT, 1.0f);
        args.put(CategoryDbTable.KEY_GOAL, 20.0f);
        args.put(CategoryDbTable.KEY_TYPE, "Average");
        args.put(CategoryDbTable.KEY_COLOR, "#ff0000");
        args.put(CategoryDbTable.KEY_PERIOD_MS, DateUtil.DAY_MS);
        args.put(CategoryDbTable.KEY_RANK, 3);
        args.put(CategoryDbTable.KEY_TREND_STATE, CategoryDbTable.KEY_TREND_UNKNOWN);
        args.put(CategoryDbTable.KEY_INTERPOLATION, "Cubic");
        args.put(CategoryDbTable.KEY_ZEROFILL, false);
        args.put(CategoryDbTable.KEY_SYNTHETIC, false);
        args.put(CategoryDbTable.KEY_FORMULA, "");
        db.insert(CategoryDbTable.TABLE_NAME, null, args);

        args = new ContentValues();
        args.put(CategoryDbTable.KEY_GROUP_NAME, "Health");
        args.put(CategoryDbTable.KEY_CATEGORY_NAME, "Hours Exercised");
        args.put(CategoryDbTable.KEY_DEFAULT_VALUE, 1.0f);
        args.put(CategoryDbTable.KEY_LAST_VALUE, 1.0f);
        args.put(CategoryDbTable.KEY_LAST_TREND, 0.0f);
        args.put(CategoryDbTable.KEY_INCREMENT, 0.25f);
        args.put(CategoryDbTable.KEY_GOAL, 2.0f);
        args.put(CategoryDbTable.KEY_TYPE, "Sum");
        args.put(CategoryDbTable.KEY_COLOR, "#0000ff");
        args.put(CategoryDbTable.KEY_PERIOD_MS, DateUtil.DAY_MS);
        args.put(CategoryDbTable.KEY_RANK, 4);
        args.put(CategoryDbTable.KEY_TREND_STATE, CategoryDbTable.KEY_TREND_UNKNOWN);
        args.put(CategoryDbTable.KEY_INTERPOLATION, "Cubic");
        args.put(CategoryDbTable.KEY_ZEROFILL, true);
        args.put(CategoryDbTable.KEY_SYNTHETIC, false);
        args.put(CategoryDbTable.KEY_FORMULA, "");
        db.insert(CategoryDbTable.TABLE_NAME, null, args);
      }

      @Override
      public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
            + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + CategoryDbTable.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + EntryDbTable.TABLE_NAME);
        onCreate(db);
      }
    }

    public SqlAdapter(Context context) {
      mCtx = context;
      mCal = Calendar.getInstance();
    }

    public EvenTrendDbAdapter open() throws SQLException {
      mDbHelper = new DatabaseHelper(mCtx);
      mDb = mDbHelper.getWritableDatabase();
      return this;
    }

    public void close() {
      mDbHelper.close();
    }

    // Flatten the entries db for export. Note this may substantially increase
    // the
    // DB size,
    // since we'll be duplicating category/trend columns for each datapoint
    public String flattenDB() {
      CategoryDbTable.Row cr = new CategoryDbTable.Row();
      EntryDbTable.Row er = new EntryDbTable.Row();
      String[] line = new String[2];

      line[0] = CSV.joinCSV(CategoryDbTable.EXPORTABLE);
      line[1] = CSV.joinCSV(EntryDbTable.EXPORTABLE);

      String db = CSV.joinCSVTerminated(line);

      Cursor c = mDb.rawQuery("SELECT " + CategoryDbTable.KEY_STAR + ", "
          + EntryDbTable.KEY_STAR + " " + " FROM " + CategoryDbTable.TABLE_NAME
          + ", " + EntryDbTable.TABLE_NAME + " " + " WHERE "
          + CategoryDbTable.TABLE_NAME + "." + CategoryDbTable.KEY_ROWID
          + " = " + EntryDbTable.TABLE_NAME + "."
          + EntryDbTable.KEY_CATEGORY_ID + " ORDER BY "
          + EntryDbTable.KEY_TIMESTAMP, null);
      if (c != null) {
        c.moveToFirst();

        for (int i = 0; i < c.getCount(); i++) {
          cr.populateFromCursor(c);
          er.populateFromCursor(c);

          line[0] = CSV.joinCSV(cr);
          line[1] = CSV.joinCSV(er);

          db += CSV.joinCSVTerminated(line);
          c.moveToNext();
        }
      }
      c.close();

      c = fetchAllSynthetics();
      if (c != null) {
        c.moveToFirst();
        for (int i = 0; i < c.getCount(); i++) {
          cr.populateFromCursor(c);
          er = new EntryDbTable.Row();

          line[0] = CSV.joinCSV(cr);
          line[1] = CSV.joinCSV(er);

          db += CSV.joinCSVTerminated(line);
          c.moveToNext();
        }
      }
      c.close();

      return db;
    }

    //
    // Category-focused Operations
    //

    public long createCategory(CategoryDbTable.Row category) {
      ContentValues args = new ContentValues();

      args.put(CategoryDbTable.KEY_GROUP_NAME, category.getGroupName());
      args.put(CategoryDbTable.KEY_CATEGORY_NAME, category.getCategoryName());
      args.put(CategoryDbTable.KEY_DEFAULT_VALUE, category.getDefaultValue());
      // Note we set the last value to the default value initially:
      args.put(CategoryDbTable.KEY_LAST_VALUE, category.getDefaultValue());
      args.put(CategoryDbTable.KEY_LAST_TREND, category.getLastTrend());
      args.put(CategoryDbTable.KEY_INCREMENT, category.getIncrement());
      args.put(CategoryDbTable.KEY_GOAL, category.getGoal());
      args.put(CategoryDbTable.KEY_TYPE, category.getType());
      args.put(CategoryDbTable.KEY_COLOR, category.getColor());
      args.put(CategoryDbTable.KEY_PERIOD_MS, category.getPeriodMs());
      args.put(CategoryDbTable.KEY_RANK, category.getRank());
      args.put(CategoryDbTable.KEY_TREND_STATE, category.getTrendState());
      args.put(CategoryDbTable.KEY_INTERPOLATION, category.getInterpolation());
      args.put(CategoryDbTable.KEY_ZEROFILL, category.getZeroFill());
      args.put(CategoryDbTable.KEY_SYNTHETIC, category.getSynthetic());
      args.put(CategoryDbTable.KEY_FORMULA, category.getFormula());
      return mDb.insert(CategoryDbTable.TABLE_NAME, null, args);
    }

    public boolean deleteCategory(long rowId) {
      return mDb.delete(CategoryDbTable.TABLE_NAME, CategoryDbTable.KEY_ROWID
          + "=" + rowId, null) > 0;
    }

    public boolean deleteAllCategories() {
      return mDb.delete(CategoryDbTable.TABLE_NAME, null, null) > 0;
    }

    public Cursor fetchAllCategories() {
      return mDb.query(CategoryDbTable.TABLE_NAME, CategoryDbTable.KEY_ALL,
          null, null, null, null, CategoryDbTable.KEY_RANK);
    }

    public Cursor fetchAllSynthetics() {
      return mDb.query(CategoryDbTable.TABLE_NAME, CategoryDbTable.KEY_ALL,
          CategoryDbTable.KEY_SYNTHETIC + "=?", new String[] { "1" }, null,
          null, CategoryDbTable.KEY_RANK);
    }

    public Cursor fetchAllGroups() {
      return mDb.rawQuery("SELECT DISTINCT " + CategoryDbTable.KEY_GROUP_NAME
          + " FROM " + CategoryDbTable.TABLE_NAME, null);
    }

    public CategoryDbTable.Row fetchCategory(long rowId) {
      CategoryDbTable.Row row = null;
      Cursor c = mDb.query(true, CategoryDbTable.TABLE_NAME,
          CategoryDbTable.KEY_ALL, CategoryDbTable.KEY_ROWID + "=?",
          new String[] { Long.valueOf(rowId).toString() }, null, null, null,
          null);
      c.moveToFirst();
      if (c.getCount() > 0) {
        row = new CategoryDbTable.Row();
        row.populateFromCursor(c);
      }
      c.close();
      return row;
    }

    public CategoryDbTable.Row fetchCategory(String category) {
      CategoryDbTable.Row row = null;
      Cursor c = mDb.query(true, CategoryDbTable.TABLE_NAME,
          CategoryDbTable.KEY_ALL, CategoryDbTable.KEY_CATEGORY_NAME + "=?",
          new String[] { category }, null, null, null, null);
      c.moveToFirst();
      if (c.getCount() > 0) {
        row = new CategoryDbTable.Row();
        row.populateFromCursor(c);
      }
      c.close();
      return row;
    }

    public long fetchCategoryId(String category) {
      long rowId = 0;
      Cursor c = mDb.query(true, CategoryDbTable.TABLE_NAME,
          new String[] { CategoryDbTable.KEY_ROWID },
          CategoryDbTable.KEY_CATEGORY_NAME + "=?", new String[] { category },
          null, null, null, null);
      c.moveToFirst();
      if (c.getCount() > 0) {
        rowId = CategoryDbTable.getId(c);
      }
      c.close();
      return rowId;
    }

    public int fetchCategoryMaxRank() {
      int maxRank = 0;
      Cursor c = mDb.rawQuery("SELECT MAX(" + CategoryDbTable.KEY_RANK
          + ") AS MAX FROM " + CategoryDbTable.TABLE_NAME, null);
      c.moveToFirst();
      if (c.getCount() > 0) {
        try {
          maxRank = c.getInt(c.getColumnIndexOrThrow("MAX"));
        } catch (IllegalArgumentException e) {
          ; // do nothing, no categories yet
        }
      }
      c.close();
      return maxRank;
    }

    public boolean updateCategory(CategoryDbTable.Row category) {
      ContentValues args = new ContentValues();

      args.put(CategoryDbTable.KEY_GROUP_NAME, category.getGroupName());
      args.put(CategoryDbTable.KEY_CATEGORY_NAME, category.getCategoryName());
      args.put(CategoryDbTable.KEY_DEFAULT_VALUE, category.getDefaultValue());
      args.put(CategoryDbTable.KEY_LAST_VALUE, category.getLastValue());
      args.put(CategoryDbTable.KEY_LAST_TREND, category.getLastTrend());
      args.put(CategoryDbTable.KEY_INCREMENT, category.getIncrement());
      args.put(CategoryDbTable.KEY_GOAL, category.getGoal());
      args.put(CategoryDbTable.KEY_TYPE, category.getType());
      args.put(CategoryDbTable.KEY_COLOR, category.getColor());
      args.put(CategoryDbTable.KEY_PERIOD_MS, category.getPeriodMs());
      args.put(CategoryDbTable.KEY_TREND_STATE, category.getTrendState());
      args.put(CategoryDbTable.KEY_INTERPOLATION, category.getInterpolation());
      args.put(CategoryDbTable.KEY_ZEROFILL, category.getZeroFill());
      args.put(CategoryDbTable.KEY_SYNTHETIC, category.getSynthetic());
      args.put(CategoryDbTable.KEY_FORMULA, category.getFormula());

      if (category.getRank() > 0)
        args.put(CategoryDbTable.KEY_RANK, category.getRank());

      return mDb.update(CategoryDbTable.TABLE_NAME, args,
          CategoryDbTable.KEY_ROWID + "=" + category.getId(), null) > 0;
    }

    public boolean updateCategory(long id, ContentValues args) {
      return mDb.update(CategoryDbTable.TABLE_NAME, args,
          CategoryDbTable.KEY_ROWID + "=" + id, null) > 0;
    }

    public boolean updateCategoryRank(long rowId, int rank) {
      ContentValues args = new ContentValues();
      args.put(CategoryDbTable.KEY_RANK, rank);

      return mDb.update(CategoryDbTable.TABLE_NAME, args,
          CategoryDbTable.KEY_ROWID + "=" + rowId, null) > 0;
    }

    public boolean updateCategoryLastValue(long rowId, float value) {
      ContentValues args = new ContentValues();
      args.put(CategoryDbTable.KEY_LAST_VALUE, value);

      return mDb.update(CategoryDbTable.TABLE_NAME, args,
          CategoryDbTable.KEY_ROWID + "=" + rowId, null) > 0;
    }

    public boolean updateCategoryTrend(long catId, String trendStr,
        float newTrend) {
      ContentValues args = new ContentValues();
      args.put(CategoryDbTable.KEY_TREND_STATE, trendStr);
      args.put(CategoryDbTable.KEY_LAST_TREND, newTrend);

      return mDb.update(CategoryDbTable.TABLE_NAME, args,
          CategoryDbTable.KEY_ROWID + "=" + catId, null) > 0;
    }

    //
    // Entry-focused Operations
    //

    public Cursor fetchAllEntries() {
      return mDb.query(EntryDbTable.TABLE_NAME, CategoryDbTable.KEY_ALL, null,
          null, null, null, EntryDbTable.KEY_TIMESTAMP);
    }

    public boolean deleteAllEntries() {
      return mDb.delete(EntryDbTable.TABLE_NAME, null, null) > 0;
    }

    public boolean deleteCategoryEntries(long catId) {
      return mDb.delete(EntryDbTable.TABLE_NAME, EntryDbTable.KEY_CATEGORY_ID
          + "=" + catId, null) > 0;
    }

    public Cursor fetchCategoryEntries(long catId) {
      return mDb.query(true, EntryDbTable.TABLE_NAME, EntryDbTable.KEY_ALL,
          EntryDbTable.KEY_CATEGORY_ID + "=" + catId, null, null, null,
          EntryDbTable.KEY_TIMESTAMP, null);
    }

    public Cursor fetchEntriesRange(long milliStart, long milliEnd) {
      // these will be ordered in-core
      return mDb.query(true, EntryDbTable.TABLE_NAME, EntryDbTable.KEY_ALL,
          EntryDbTable.KEY_TIMESTAMP + " >= ? and "
              + EntryDbTable.KEY_TIMESTAMP + " <= ?", new String[] {
              Long.toString(milliStart), Long.toString(milliEnd) }, null, null,
          null, null);
    }

    public Cursor fetchCategoryEntriesRange(long catId, long milliStart,
        long milliEnd) {
      // these will be ordered in-core
      return mDb.query(true, EntryDbTable.TABLE_NAME, EntryDbTable.KEY_ALL,
          EntryDbTable.KEY_CATEGORY_ID + "= ? and "
              + EntryDbTable.KEY_TIMESTAMP + " >= ? and "
              + EntryDbTable.KEY_TIMESTAMP + " <= ?", new String[] {
              Long.toString(catId), Long.toString(milliStart),
              Long.toString(milliEnd) }, null, null, null, null);
    }

    public long createEntry(EntryDbTable.Row entry) {
      ContentValues args = new ContentValues();

      args.put(EntryDbTable.KEY_CATEGORY_ID, entry.getCategoryId());
      args.put(EntryDbTable.KEY_VALUE, entry.getValue());
      args.put(EntryDbTable.KEY_TIMESTAMP, entry.getTimestamp());
      args.put(EntryDbTable.KEY_N_ENTRIES, entry.getNEntries());
      return mDb.insert(EntryDbTable.TABLE_NAME, null, args);
    }

    public boolean deleteEntry(long rowId) {
      return mDb.delete(EntryDbTable.TABLE_NAME, EntryDbTable.KEY_ROWID + "="
          + rowId, null) > 0;
    }

    public Cursor fetchRecentEntries(int nItems, int skip) {
      return mDb.rawQuery("SELECT " + CategoryDbTable.KEY_STAR + ", "
          + EntryDbTable.KEY_STAR + " FROM " + CategoryDbTable.TABLE_NAME
          + ", " + EntryDbTable.TABLE_NAME + " WHERE "
          + EntryDbTable.TABLE_NAME + "." + EntryDbTable.KEY_CATEGORY_ID
          + " = " + CategoryDbTable.TABLE_NAME + "."
          + CategoryDbTable.KEY_ROWID + " ORDER BY "
          + EntryDbTable.KEY_TIMESTAMP + " DESC LIMIT " + skip + ", " + nItems,
          null);
    }

    public Cursor fetchRecentEntries(int nItems, long catId, int skip) {
      return mDb.rawQuery("SELECT " + CategoryDbTable.KEY_STAR + ", "
          + EntryDbTable.KEY_STAR + " FROM " + CategoryDbTable.TABLE_NAME
          + ", " + EntryDbTable.TABLE_NAME + " WHERE "
          + EntryDbTable.TABLE_NAME + "." + EntryDbTable.KEY_CATEGORY_ID
          + " = " + CategoryDbTable.TABLE_NAME + "."
          + CategoryDbTable.KEY_ROWID + " AND " + CategoryDbTable.TABLE_NAME
          + "." + CategoryDbTable.KEY_ROWID + " = " + catId + " ORDER BY "
          + EntryDbTable.KEY_TIMESTAMP + " DESC LIMIT " + skip + ", " + nItems,
          null);
    }

    public EntryDbTable.Row fetchLastCategoryEntry(long catId) {
      EntryDbTable.Row row = null;
      Cursor c = mDb.rawQuery("SELECT * FROM " + EntryDbTable.TABLE_NAME
          + " WHERE " + EntryDbTable.KEY_CATEGORY_ID + " = " + catId
          + " ORDER BY " + EntryDbTable.KEY_TIMESTAMP + " DESC LIMIT 1", null);
      c.moveToFirst();
      if (c.getCount() > 0) {
        row = new EntryDbTable.Row();
        row.populateFromCursor(c);
      }
      c.close();
      return row;
    }

    public EntryDbTable.Row fetchCategoryEntryInPeriod(long catId, long period,
        long date_ms) {
      EntryDbTable.Row row = null;
      mCal.setTimeInMillis(date_ms);

      Period p = DateUtil.mapLongToPeriod(period);
      DateUtil.setToPeriodStart(mCal, p);
      long min = mCal.getTimeInMillis();

      int step = 1;
      if (p == Period.QUARTER)
        step = 3;
      mCal.add(DateUtil.mapLongToCal(period), step);

      long max = mCal.getTimeInMillis();

      Cursor c = mDb.rawQuery("SELECT * FROM " + EntryDbTable.TABLE_NAME
          + " WHERE " + EntryDbTable.KEY_CATEGORY_ID + " = " + catId + " AND "
          + EntryDbTable.KEY_TIMESTAMP + " >= " + min + " AND "
          + EntryDbTable.KEY_TIMESTAMP + " < " + max + " ORDER BY "
          + EntryDbTable.KEY_TIMESTAMP + " DESC LIMIT 1", null);
      c.moveToFirst();
      if (c.getCount() > 0) {
        row = new EntryDbTable.Row();
        row.populateFromCursor(c);
      }
      c.close();
      return row;
    }

    public EntryDbTable.Row fetchEntry(long rowId) {
      EntryDbTable.Row row = null;
      Cursor c = mDb.query(true, EntryDbTable.TABLE_NAME, EntryDbTable.KEY_ALL,
          EntryDbTable.KEY_ROWID + "=?", new String[] { Long.valueOf(rowId)
              .toString() }, null, null, null, null);
      c.moveToFirst();
      if (c.getCount() > 0) {
        row = new EntryDbTable.Row();
        row.populateFromCursor(c);
      }
      c.close();
      return row;
    }

    public boolean updateEntry(EntryDbTable.Row entry) {
      ContentValues args = new ContentValues();

      args.put(EntryDbTable.KEY_CATEGORY_ID, entry.getCategoryId());
      args.put(EntryDbTable.KEY_VALUE, entry.getValue());
      args.put(EntryDbTable.KEY_TIMESTAMP, entry.getTimestamp());
      args.put(EntryDbTable.KEY_N_ENTRIES, entry.getNEntries());

      return mDb.update(EntryDbTable.TABLE_NAME, args, EntryDbTable.KEY_ROWID
          + "=" + entry.getId(), null) > 0;
    }

    public Cursor fetchRecentCategoryEntries(long catId, int nItems) {
      return mDb.rawQuery(
          "SELECT * FROM " + EntryDbTable.TABLE_NAME + " WHERE "
              + EntryDbTable.KEY_CATEGORY_ID + " = " + catId + " ORDER BY "
              + EntryDbTable.KEY_TIMESTAMP + " DESC LIMIT " + nItems, null);
    }
  }
}
