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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import net.redgeek.android.eventrend.db.CategoryDbTable;
import net.redgeek.android.eventrend.graph.plugins.TimeSeriesCorrelator;
import net.redgeek.android.eventrend.primitives.TimeSeries;
import net.redgeek.android.eventrend.util.Number;

/** Performs a correlation of data according the the Correlator that has been
 * set for the TimeSeriesCorrleator.setCorrelator.  After the correlation is complete,
 * this also orders the correlations from highest correlation to lowest, and returns
 * the result as a string.
 * 
 * <p>Note that since this is "backgroundtask", no UI operations may be performed.
 * 
 * @author barclay
 *
 */
public class CorrelateTask {	
	private TimeSeriesCorrelator  mCorrelator;
	private ArrayList<TimeSeries> mSeries;
	
	public String mOutput;
	
	public HashMap<Float,String> mOutputMap;
	
	public CorrelateTask() {
		mOutputMap = new HashMap<Float,String>();
	}

	public void setCorrelator(TimeSeriesCorrelator c) {
		mCorrelator = c;
	}
	
	public void setTimeSeries(ArrayList<TimeSeries> timeseries) {
		mSeries = timeseries;
	}

	public void correlate() {
        ArrayList<String> l;
		int nSeries = mSeries.size();
        HashMap<Float,ArrayList<String>> hm = new HashMap<Float,ArrayList<String>>();
        String[] categories = new String[nSeries];
        
        Float[][] correlations = mCorrelator.correlate(mSeries);

        for (int i = 0; i < nSeries; i++) {
        	TimeSeries ts1 = mSeries.get(i);
            categories[i] = ts1.getDbRow().getCategoryName();
        }

        for (int i = 0; i < nSeries; i++) {
            for (int j = i + 1; j < nSeries; j++) {
            	float key;
            	Float corr = correlations[i][j];
            	if (corr == null || corr.isNaN()) {
            		key = 0.0f;
            	} else {
            		key = corr.floatValue();
            	}

                l = hm.get(Math.abs(key));
                if (l == null) {
                	l = new ArrayList<String>();
                	hm.put(Math.abs(key), l);
                }

                key = Number.Round(corr, 3);
                String s = categories[i]+ " and " + categories[j]
                	     + "\n  " + key + ": " + Number.LinearMatrixCorrelation.correlationToString(corr) + "\n";
                l.add(s);
            }
        }
                	
        Map.Entry entry;
        SortedMap<Float, ArrayList<String>> sorted = new TreeMap<Float, ArrayList<String>>(java.util.Collections.reverseOrder());
        sorted.putAll(hm);
        Iterator iterator = sorted.entrySet().iterator();        

        mOutput = "";
        while (iterator.hasNext()) {
            entry = (Map.Entry) iterator.next();
            l = (ArrayList<String>) entry.getValue();
            for (int i = 0; i < l.size(); i++) {
            	mOutput += l.get(i);
            }
		}
	}
}