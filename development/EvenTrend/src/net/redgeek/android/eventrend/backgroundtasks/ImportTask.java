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

package net.redgeek.android.eventrend.backgroundtasks;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import net.redgeek.android.eventrend.db.CategoryDbTable;
import net.redgeek.android.eventrend.db.EntryDbTable;
import net.redgeek.android.eventrend.db.EvenTrendDbAdapter;
import net.redgeek.android.eventrend.importing.CSV;
import android.database.Cursor;

/** Import a CSV file into the database, replacing existing data.  (Merge import is
 * planned but not yet implemented.)  Categories are created on demand, as the
 * export format currently flattens the database into one file, meaning that most of
 * the category fields are duplicated across every datapoint row.  Furthermore, this
 * means that if an entry exists without any associated datapoints, it will not exist
 * in the output file, and thus will not be created when importing.
 * 
 * <p>Note that since this is "backgroundtask", no UI operations may be performed.
 * 
 * @author barclay
 *
 */
public class ImportTask {	
	private EvenTrendDbAdapter mDbh      = null;
	private String 			   mFilename = null;
	private int  			   mHistory  = -1;
	
	// Intended for future discrete progress bar indicator:
	public int mNumRecords;
	public int mNumRecordsDone;
	
	public ImportTask() {}
	public ImportTask(EvenTrendDbAdapter dbh) {
		mDbh = dbh;
	}
	public ImportTask(EvenTrendDbAdapter dbh, String filename, int history) {
		mDbh = dbh;
		mFilename = filename;
		mHistory = history;		
	}
	
	public void setDbh(EvenTrendDbAdapter dbh) {
		mDbh = dbh;
	}

	public void setFilename(String filename) {
		mFilename = filename;
	}

	public void setHistory(int history) {
		mHistory = history;
	}

	public void doImport() throws IOException {
		if (mDbh == null || mFilename == null || mHistory < 0)
			return;			
		
		String line;
		ArrayList<String> columns = null;
		ArrayList<String> data    = null;

		mDbh.deleteAllCategories();
		mDbh.deleteAllEntries();

		BufferedReader input = null;
		FileInputStream f = new FileInputStream(mFilename);
		input = new BufferedReader(new InputStreamReader(new DataInputStream(f)));

		int i = 0;
		while ((line = input.readLine()) != null) {
			if (i == 0) {
				columns = CSV.parseHeader(line);
			} else {
				data = CSV.getNextLine(line);
				insertEntry(data, columns);
			}
			i++;
		}
		input.close();

//		Cursor c = mDbh.fetchAllCategories();
//		c.moveToFirst();
//		for (i = 0; i < c.getCount(); i++ ) {		
//			long catId = CategoryDbTable.getId(c);
//			float goal = CategoryDbTable.getGoal(c);
//			mDbh.updateCategoryTrend(catId, mHistory, goal, null);
//			c.moveToNext();
//		}
//		c.close();

		return;
	}
	
	private void insertEntry(ArrayList<String> data, ArrayList<String> columns) {
		CategoryDbTable.Row catQuery;
		CategoryDbTable.Row catNew = new CategoryDbTable.Row();
		EntryDbTable.Row    entry = new EntryDbTable.Row();
		int i = 0;

		long   catId = 0;    	
		for (i = 0; i < data.size() && i < columns.size(); i++) {    		
			String datum = data.get(i);

			// TODO:  this can probably be compressed to just walk/index the
			// EXPORTABLE arrays of the *DbTable classes.
			if (columns.get(i).equals(CategoryDbTable.KEY_GROUP_NAME))
				catNew.setGroupName(datum);
			else if (columns.get(i).equals(CategoryDbTable.KEY_CATEGORY_NAME))
				catNew.setCategoryName(datum);
			else if (columns.get(i).equals(CategoryDbTable.KEY_DEFAULT_VALUE))
				catNew.setDefaultValue(Float.valueOf(datum));
			else if (columns.get(i).equals(CategoryDbTable.KEY_GOAL))
				catNew.setGoal(Float.valueOf(datum));
			else if (columns.get(i).equals(CategoryDbTable.KEY_INCREMENT))
				catNew.setIncrement(Float.valueOf(datum));
			else if (columns.get(i).equals(CategoryDbTable.KEY_COLOR))
				catNew.setColor(datum);
			else if (columns.get(i).equals(CategoryDbTable.KEY_TYPE))
				catNew.setType(datum);
			else if (columns.get(i).equals(CategoryDbTable.KEY_PERIOD_MS))
				catNew.setPeriodMs(Long.valueOf(datum));
			else if (columns.get(i).equals(CategoryDbTable.KEY_RANK))    	    	
				catNew.setRank(Integer.valueOf(datum));
			else if (columns.get(i).equals(CategoryDbTable.KEY_INTERPOLATION))
				catNew.setInterpolation(datum);
			else if (columns.get(i).equals(CategoryDbTable.KEY_ZEROFILL))
				catNew.setZeroFill(Integer.valueOf(datum));
			else if (columns.get(i).equals(CategoryDbTable.KEY_SYNTHETIC))
				catNew.setSynthetic(Integer.valueOf(datum));
			else if (columns.get(i).equals(CategoryDbTable.KEY_FORMULA))
				catNew.setFormula(datum);
			else if (columns.get(i).equals(EntryDbTable.KEY_TIMESTAMP))
				entry.setTimestamp(Long.valueOf(datum));
			else if (columns.get(i).equals(EntryDbTable.KEY_VALUE))
				entry.setValue(Float.valueOf(datum));
			else if (columns.get(i).equals(EntryDbTable.KEY_N_ENTRIES))
				entry.setNEntries(Integer.valueOf(datum));
		}

		catQuery = mDbh.fetchCategory(catNew.getCategoryName());
		if (catQuery == null) {
			catId = mDbh.createCategory(catNew);
		} else {
			catId = catQuery.getId();
		}

		if (catNew.getSynthetic() == false) {
			entry.setCategoryId(catId);
			mDbh.createEntry(entry);
		}
			
		return;
	}
}