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

package net.redgeek.android.eventrend.primitives;

import java.util.ArrayList;
import java.util.List;

import net.redgeek.android.eventrend.db.CategoryDbTable;
import net.redgeek.android.eventrend.graph.GraphView;
import net.redgeek.android.eventrend.graph.plugins.TimeSeriesInterpolator;
import net.redgeek.android.eventrend.synthetic.AST;
import net.redgeek.android.eventrend.util.DateUtil;
import net.redgeek.android.eventrend.util.Number;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;

/** A representation of series of Datapoints plottable on screen.  
 * Specific per-category.
 * 
 * @author barclay
 *
 */
public final class TimeSeries {	
	// All of the following are ordered via x-values (time), and are references to
	// datapoints in the DataCache.  These are just the datapoints necessary
	// for graphing the time series on screen, not an exhaustive list.  Multiple
	// datapoints are required for VisiblePre (if available), as they are needed 
	// to calculate the trend line accurately and continue the graph line to the
	// left edge of the screen, and at least one datapoint is needed
	// in VisiblePost (if available) in order to connect the last on-screen point
	// to something offscreen, in order to continue drawing the line to the edge of
	// the graph.
	private ArrayList<Datapoint> mDatapoints; // a concatenation of the following:
	private int mVisiblePreFirstIdx;
	private int mVisiblePreLastIdx;
	private int mVisibleFirstIdx;
	private int mVisibleLastIdx;
	private int mVisiblePostFirstIdx;
	private int mVisiblePostLastIdx;
	
	// Various stats used for bounding
	private Tuple 	 mVisibleMins;
	private Tuple 	 mVisibleMaxs;
	private Tuple 	 mDatapointMins;
	private Tuple 	 mDatapointMaxs;
	
	private long     mTimestampLast;
	private int	     mNumEntries = 0;
	
	private boolean     mEnabled;
	CategoryDbTable.Row mDbRow;
	
	private ArrayList<TimeSeries> mDependents;
	private ArrayList<TimeSeries> mDependees;	
	
	// Drawing-related
	private Path     mPath;
	private Path     mTrend;
	private Path     mPoints;
	private float    mPointRadius;
	private float    mTouchRadius = GraphView.POINT_TOUCH_RADIUS;
	private int      mColor;
	private Paint    mPathPaint;
	private Paint    mPointsPaint;
	private Paint    mTrendPaint;
	private Paint    mTrendMarkerPaint;
	private Paint    mGoalPaint;
	private Paint    mLabelPaint;

	// Various stats
	private Number.RunningStats   mValueStats;
	private Number.Trend          mValueTrend;
	private Number.RunningStats   mTimestampStats;
	private Number.WindowedStdDev mStdDevWindow;
	
	// Interpolator
	private TimeSeriesInterpolator mInterpolator;
		
	public TimeSeries(CategoryDbTable.Row row, int history, float smoothing) {
		mDbRow = row;
		
		mDatapoints = new ArrayList<Datapoint>();
		mVisiblePreFirstIdx  = Integer.MIN_VALUE;
		mVisiblePreLastIdx   = Integer.MAX_VALUE;
		mVisibleFirstIdx     = Integer.MIN_VALUE;
		mVisibleLastIdx      = Integer.MAX_VALUE;
		mVisiblePostFirstIdx = Integer.MIN_VALUE;
		mVisiblePostLastIdx  = Integer.MAX_VALUE;
		
		mEnabled     = false;
		
		mVisibleMins   = new Tuple(Float.MAX_VALUE, Float.MAX_VALUE);
		mVisibleMaxs   = new Tuple(Float.MIN_VALUE, Float.MIN_VALUE);
		mDatapointMins = new Tuple(Float.MAX_VALUE, Float.MAX_VALUE);
		mDatapointMaxs = new Tuple(Float.MIN_VALUE, Float.MIN_VALUE);
		
		mPath   = new Path();
		mTrend  = new Path();
		mPoints = new Path();
		
		setColor(row.getColorInt());

		mValueStats     = new Number.RunningStats();
		mTimestampStats = new Number.RunningStats();
		mValueTrend     = new Number.Trend(smoothing);
		mStdDevWindow   = new Number.WindowedStdDev(history);
		
		mDependents = new ArrayList<TimeSeries>();
		mDependees  = new ArrayList<TimeSeries>();	
	}
	
	public TimeSeries(TimeSeries series) {
		mDbRow = new CategoryDbTable.Row(series.mDbRow);
		
		mDatapoints = new ArrayList<Datapoint>();
		mVisiblePreFirstIdx  = series.mVisiblePreFirstIdx;
		mVisiblePreLastIdx   = series.mVisiblePreLastIdx;
		mVisibleFirstIdx     = series.mVisibleFirstIdx;
		mVisibleLastIdx      = series.mVisibleLastIdx;
		mVisiblePostFirstIdx = series.mVisiblePostFirstIdx;
		mVisiblePostLastIdx  = series.mVisiblePostLastIdx;

		copyDatapoints(series);
		mEnabled     = series.mEnabled;

		mVisibleMins   = new Tuple(series.mVisibleMins);
		mVisibleMaxs   = new Tuple(series.mVisibleMaxs);
		mDatapointMins = new Tuple(series.mDatapointMins);
		mDatapointMaxs = new Tuple(series.mDatapointMaxs);

		mTimestampLast     = series.mTimestampLast;
		mNumEntries        = series.mNumEntries;
		mEnabled           = series.mEnabled;
		
		mValueStats     = new Number.RunningStats(series.mValueStats);
		mTimestampStats = new Number.RunningStats(series.mTimestampStats);
		mValueTrend     = new Number.Trend(series.mValueTrend);
		mStdDevWindow   = new Number.WindowedStdDev(series.mStdDevWindow);

		mDependents = new ArrayList<TimeSeries>();
		for (int i = 0; i < mDependents.size(); i++) {
			mDependents.add(series.mDependents.get(i));
		}
		mDependees  = new ArrayList<TimeSeries>();	
		for (int i = 0; i < mDependees.size(); i++) {
			mDependees.add(series.mDependees.get(i));
		}

		// this should be fine to share; it should be stateless
		mInterpolator = series.mInterpolator;
	}
	
	@Override
    public String toString() {
		return mDbRow.getCategoryName() + " (id: " + mDbRow.getId() + ")";
	}
	
	public void clearSeries() {
		mDatapoints.clear();
	}
	
	public void setPointRadius(float f) {
		mPointRadius = f;
	}
	
	public CategoryDbTable.Row getDbRow() {
		return mDbRow;
	}

	public void setDbRow(CategoryDbTable.Row row) {
		mDbRow = row;
	}

	public void setInterpolator(TimeSeriesInterpolator i) {
		mInterpolator = i;
	}

	public TimeSeriesInterpolator getInterpolator() {
		return mInterpolator;
	}

	public void setEnabled(boolean b) {
		mEnabled = b;
	}
	
	public boolean isEnabled() {
		return mEnabled;
	}
	
	public void addDependent(TimeSeries ts) {
		if (mDependents.contains(ts) != true)
			mDependents.add(ts);
	} 

	public ArrayList<TimeSeries> getDependents() {
		return mDependents;
	} 

	public void addDependee(TimeSeries ts) {
		if (mDependees.contains(ts) != true)
			mDependees.add(ts);
	} 

	public ArrayList<TimeSeries> getDependees() {
		return mDependees;
	} 

	public int getVisibleNumEntries() { 
		return mNumEntries; 
	}
	
	public float getVisibleValueMin() { 
		return mVisibleMins.y; 
	}
	
	public float getVisibleValueMax() { 
		return mVisibleMaxs.y; 
	}
	
	public float getDatapointValueMin() { 
		return mDatapointMins.y;
	}
	
	public float getDatapointValueMax() { 
		return mDatapointMaxs.y; 
	}

	public Number.RunningStats getValueStats() {
	    return mValueStats;
	}

	public Number.RunningStats getTimestampStats() {
	    return mTimestampStats;
	}
	
	public Number.Trend getTrendStats() {
	    return mValueTrend;
	}
	
	public Tuple getVisibleMins() {
		return mVisibleMins;
	}

	public Tuple getVisibleMaxs() {
		return mVisibleMaxs;
	}
	
	public void recalcStatsAndBounds(float smoothing, int history) {
		mValueStats     = new Number.RunningStats();
		mTimestampStats = new Number.RunningStats();
		mValueTrend     = new Number.Trend(smoothing);
		mStdDevWindow   = new Number.WindowedStdDev(history);
		calcStatsAndBounds();
	}

	private void calcStatsAndBounds() {
		int firstNEntries = 0;
		
		mVisibleMins.set(Float.MAX_VALUE, Float.MAX_VALUE);
		mVisibleMaxs.set(Float.MIN_VALUE, Float.MIN_VALUE);
		mDatapointMins.set(Float.MAX_VALUE, Float.MAX_VALUE);
		mDatapointMaxs.set(Float.MIN_VALUE, Float.MIN_VALUE);

		if (mDatapoints == null)
			return;

		for (int i = 0; i < mDatapoints.size(); i++) {
			Datapoint d = mDatapoints.get(i);
			if (i < mVisibleFirstIdx || i > mVisibleLastIdx) {
				mValueTrend.update(d.mValue.y);
				d.mTrend.x = d.mValue.x;
				d.mTrend.y = mValueTrend.mTrend;

				mStdDevWindow.update(d.mValue.y);
				d.mStdDev = mStdDevWindow.getStandardDev();
			} else {
				mVisibleMins.min(d.mValue);
				mVisibleMaxs.max(d.mValue);
				mDatapointMins.set(mVisibleMins);
				mDatapointMaxs.set(mVisibleMaxs);					
				
				// we run stats on the y-values themselves ...
				mValueStats.update(d.mValue.y, d.mNEntries);
				mValueTrend.update(d.mValue.y);
				mStdDevWindow.update(d.mValue.y);
				d.mStdDev = mStdDevWindow.getStandardDev();
				// but use the delta of the timestamps for x-stats:
				if (i > 0) {
					long delta = (d.mMillis - mTimestampLast);
					mTimestampStats.update(delta, d.mNEntries + firstNEntries);
				}
				mTimestampLast = (d.mMillis);
				firstNEntries = 0;
				if (i == 0)
					firstNEntries = d.mNEntries;
				
				// d.mTrend will be used for plotting the trend line,
				// so we don't want to change the x value, since that
				// should still be an absolute time
				d.mTrend.x = d.mValue.x;
				d.mTrend.y = mValueTrend.mTrend;
				
				mNumEntries += d.mNEntries;				
			}
		}
		interpolateBoundsToOffscreen();
	}

	public void setDatapoints(ArrayList<Datapoint> pre, ArrayList<Datapoint> range,
			ArrayList<Datapoint> post) {

		clearSeries();
		if (pre != null && pre.size() > 0) {
			mVisiblePreFirstIdx = 0;
			mVisiblePreLastIdx = pre.size() - 1;
			mDatapoints.addAll(pre);
		}
		if (range != null && range.size() > 0) {
			mVisibleFirstIdx = mDatapoints.size();
			mVisibleLastIdx  = mVisibleFirstIdx + range.size() - 1;
			mDatapoints.addAll(range);		
		}
		if (post != null && post.size() > 0) {
			mVisiblePostFirstIdx = mDatapoints.size();
			mVisiblePostLastIdx  = mVisiblePostFirstIdx + post.size() - 1;
			mDatapoints.addAll(post);		
		}
		calcStatsAndBounds();
		return;
	}

	public int getColor() {
		return mColor;
	}
        
	public Paint getPathPaint() {
		return mPathPaint;
	}

	public Paint getPointsPaint() {
		return mPointsPaint;
	}

	public Paint getTrendPaint() {
		return mTrendPaint;
	}

	public Paint getTrendMarkerPaint() {
		return mTrendMarkerPaint;
	}

	public Paint getGoalPaint() {
		return mGoalPaint;
	}

	public Paint getLabelPaint() {
		return mLabelPaint;
	}
	
	public Datapoint getFirstVisible() {
	    if (mVisibleFirstIdx >= 0 && mVisibleFirstIdx < mDatapoints.size())
	        return mDatapoints.get(mVisibleFirstIdx);
	    return null;
	}

	public Datapoint getLastVisible() {
        if (mVisibleLastIdx >= 0 && mVisibleLastIdx < mDatapoints.size())
            return mDatapoints.get(mVisibleLastIdx);
        return null;
	}

	public Datapoint getFirstPostVisible() {
        if (mVisibleLastIdx + 1 >= 0 && mVisibleLastIdx + 1< mDatapoints.size())
			return mDatapoints.get(mVisibleLastIdx + 1);
		return null;
	}

	public Datapoint getLastPreVisible() {
        if (mVisibleFirstIdx - 1 >= 0 && mVisibleFirstIdx - 1 < mDatapoints.size())
			return mDatapoints.get(mVisibleFirstIdx - 1);
		return null;
	}

	public void drawPath(Canvas canvas) {
		Datapoint thisPoint = null;
		Datapoint prevPoint = null;
		Tuple first  = null;
		Tuple second = null;
		int datapointsSize;
		
		if (isEnabled() == false)
			return;
		
		mPath.rewind();
		mPoints.rewind();
		
		// we don't have to draw all the point preceding the first visible on,
		// just the last of them, up until one after the last visible
		int offset = mVisibleFirstIdx - 1;
		if (offset < 0)
			offset = 0;
		
		datapointsSize = mDatapoints.size();
	    for (int i = offset; i <  datapointsSize && i <= mVisibleLastIdx + 1; i++ ) {
	    	thisPoint = mDatapoints.get(i);
	    	if (thisPoint != null) 
	    		second = thisPoint.mValueScreen;
	    	if (prevPoint != null) 
	    		first = prevPoint.mValueScreen;
	    	
			mInterpolator.updatePath(mPath, first, second);
    		
			mPoints.moveTo(thisPoint.mValueScreen.x, thisPoint.mValueScreen.y);
    		mPoints.addCircle(thisPoint.mValueScreen.x, thisPoint.mValueScreen.y, 
    				mPointRadius, Path.Direction.CW);
    		
			prevPoint = thisPoint;
	    }
		
	    if (thisPoint != null) {
	    	mPath.setLastPoint(thisPoint.mValueScreen.x, thisPoint.mValueScreen.y);    	
	    	canvas.drawPath(mPath, mPathPaint);
	    	canvas.drawPath(mPoints, mPointsPaint);
	    }
	}

	public void drawTrend(Canvas canvas) {
		Datapoint thisPoint = null;
		Datapoint prevPoint = null;
		Tuple first  = null;
		Tuple second = null;
		int datapointsSize;

		if (isEnabled() == false)
			return;
		
		// we don't have to draw all the point preceding the first visible on,
		// just the last of them, up until one after the last visible
		int offset = mVisibleFirstIdx - 1;
		if (offset < 0)
			offset = 0;
		
		mTrend.rewind();
		datapointsSize = mDatapoints.size();
	    for (int i = offset; i <  datapointsSize && i <= mVisibleLastIdx + 1; i++ ) {
	    	thisPoint = mDatapoints.get(i);
	    	if (thisPoint != null) 
	    		second = thisPoint.mTrendScreen;
	    	if (prevPoint != null) 
	    		first = prevPoint.mTrendScreen;
	    	
			mInterpolator.updatePath(mTrend, first, second);

			prevPoint = thisPoint;
    	}

	    if (thisPoint != null) {
			canvas.drawPath(mTrend, getTrendPaint());
		}
	}

	public Datapoint lookupVisibleDatapoint(Tuple press) {
		Datapoint d;
		
		if (isEnabled() == false)
			return null;

		if (mVisibleFirstIdx != Integer.MIN_VALUE && mVisibleLastIdx != Integer.MAX_VALUE) {		
			for (int i = mVisibleFirstIdx; i <= mVisibleLastIdx; i++ ) {
				d = mDatapoints.get(i);

				if (press.x >= d.mValueScreen.x - mTouchRadius &&
						press.x <= d.mValueScreen.x + mTouchRadius && 
						press.y >= d.mValueScreen.y - mTouchRadius && 
						press.y <= d.mValueScreen.y + mTouchRadius) {
					return d;
				}
			}
		}
	    return null;
	}

	public Datapoint findNeighbor(long timestamp, boolean pre) {
		Datapoint d = null;

		if (mDatapoints == null || mDatapoints.size() < 1)
			return d;
		
		int min = 0;
		int max = mDatapoints.size() - 1;
		int mid = max / 2;
		
		d = mDatapoints.get(mid);
		while (d != null) {
			if (d.mMillis == timestamp) {
				return d;
			}
			else if (max < min) {
				if (pre == true) {
					if (d.mMillis > timestamp) {
						if (mid - 1 > 0)
							d = mDatapoints.get(mid - 1);
						else
							d = null;
					}
				} else {
					if (d.mMillis < timestamp) {
						if (mid + 1 >= mDatapoints.size())
							d = mDatapoints.get(mid + 1);					
						else
							d = null;
					}
				}
				return d;
			}			
			else if (d.mMillis < timestamp) {
				min = mid + 1;
			}
			else if (d.mMillis > timestamp) {
				max = mid - 1;
			}
			mid = min + ((max - min) / 2);
			
			// Check to see if we were trying to run off the end, if so, just
			// return the first or last entry.
			if (mid >= mDatapoints.size() && pre == true)
				return d;
			if (mid < 0 && pre == false)
				return d;
			
			if (mid < 0 || mid > mDatapoints.size() - 1)
				break;
			d = mDatapoints.get(mid);
		}
		
		return null;
	}
	
	public ArrayList<Datapoint> getDatapoints() {
		return mDatapoints;
	}

	public List<Datapoint> getVisiblePre() {
		if (mVisiblePreFirstIdx != Integer.MIN_VALUE && mVisiblePreLastIdx != Integer.MAX_VALUE)
			return mDatapoints.subList(mVisiblePreFirstIdx, mVisiblePreLastIdx + 1);
		return null;
	}

	public List<Datapoint> getVisible() {
		if (mVisibleFirstIdx != Integer.MIN_VALUE && mVisibleLastIdx != Integer.MAX_VALUE)
			return mDatapoints.subList(mVisibleFirstIdx, mVisibleLastIdx + 1);
		return null;
	}

	public List<Datapoint> getVisiblePost() {
		if (mVisiblePostFirstIdx != Integer.MIN_VALUE && mVisiblePostLastIdx != Integer.MAX_VALUE)
			return mDatapoints.subList(mVisiblePostFirstIdx, mVisiblePostLastIdx + 1);
		return null;
	}

	public Datapoint findPreNeighbor(long timestamp) {
		return findNeighbor(timestamp, true);
	}

	public Datapoint findPostNeighbor(long timestamp) {
		return findNeighbor(timestamp, false);
	}

	public Float interpolateScreenCoord(long timestamp) {
		Datapoint d1 = this.findPreNeighbor(timestamp - 1);
		Datapoint d2 = this.findPostNeighbor(timestamp);
		if (d1 == null || d2 == null)
			return null;
		return mInterpolator.interpolateY(d1.mValueScreen, d2.mValueScreen, timestamp);
	}

	public Float interpolateValue(long timestamp) {
		Datapoint d1 = this.findPreNeighbor(timestamp - 1);
		Datapoint d2 = this.findPostNeighbor(timestamp);
		if (d1 == null || d2 == null)
			return null;
		return mInterpolator.interpolateY(d1.mValue, d2.mValue, timestamp);
	}

	public void floatOp(Float f, AST.Opcode op, boolean pre) {
		if (f == null || f.isNaN() || f.isInfinite())
			return;
		
		Datapoint d;
		for (int i = 0; i < mDatapoints.size(); i++) {
			d = mDatapoints.get(i);
			if (pre == false) {
				if (op == AST.Opcode.PLUS)
					d.mValue.y += f;
				else if (op == AST.Opcode.MINUS)
					d.mValue.y -= f;
				else if (op == AST.Opcode.MULTIPLY)
					d.mValue.y *= f;
				else if (op == AST.Opcode.DIVIDE) {
					if (f != 0)
						d.mValue.y /= f;
				}
			} else {
				if (op == AST.Opcode.PLUS)
					d.mValue.y = f + d.mValue.y;
				else if (op == AST.Opcode.MINUS)
					d.mValue.y = f - d.mValue.y;
				else if (op == AST.Opcode.MULTIPLY)
					d.mValue.y = f * d.mValue.y;
				else if (op == AST.Opcode.DIVIDE) {
					if (d.mValue.y != 0)
						d.mValue.y = f / d.mValue.y;
				}
			}
		}
	}

	public void plusPre(Float f) { 
		floatOp(f, AST.Opcode.PLUS, true); 
	}

	public void minusPre(Float f) { 
		floatOp(f, AST.Opcode.MINUS, true); 
	}

	public void multiplyPre(Float f) { 
		floatOp(f, AST.Opcode.MULTIPLY, true); 
	}

	public void dividePre(Float f) { 
		floatOp(f, AST.Opcode.DIVIDE, true); 
	}

	public void plusPost(Float f) { 
		floatOp(f, AST.Opcode.PLUS, false); 
	}

	public void minusPost(Float f) { 
		floatOp(f, AST.Opcode.MINUS, false); 
	}

	public void multiplyPost(Float f) { 
		floatOp(f, AST.Opcode.MULTIPLY, false); 
	}

	public void dividePost(Float f) { 
		floatOp(f, AST.Opcode.DIVIDE, false); 
	}
	
	public void longOp(Long l, AST.Opcode op, boolean pre) {
		if (l == null)
			return;
		
		Datapoint d;
		for (int i = 0; i < mDatapoints.size(); i++) {
			d = mDatapoints.get(i);
			if (pre == false) {
				if (op == AST.Opcode.PLUS)
					d.mValue.y += l;
				else if (op == AST.Opcode.MINUS)
					d.mValue.y -= l;
				else if (op == AST.Opcode.MULTIPLY)
					d.mValue.y *= l;
				else if (op == AST.Opcode.DIVIDE) {
					if (l != 0)
						d.mValue.y /= l;
				}
			} else {
				if (op == AST.Opcode.PLUS)
					d.mValue.y = l + d.mValue.y;
				else if (op == AST.Opcode.MINUS)
					d.mValue.y = l - d.mValue.y;
				else if (op == AST.Opcode.MULTIPLY)
					d.mValue.y = l * d.mValue.y;
				else if (op == AST.Opcode.DIVIDE) {
					if (d.mValue.y != 0)
						d.mValue.y = l / d.mValue.y;
				}
			}
		}
	}
	
	public void plusPre(Long l) { 
		longOp(l, AST.Opcode.PLUS, true); 
	}
	
	public void minusPre(Long l) { 
		longOp(l, AST.Opcode.MINUS, true); 
	}
	
	public void multiplyPre(Long l) { 
		longOp(l, AST.Opcode.MULTIPLY, true); 
	}
	
	public void dividePre(Long l) { 
		longOp(l, AST.Opcode.DIVIDE, true); 
	}
	
	public void plusPost(Long l) { 
		longOp(l, AST.Opcode.PLUS, false); 
	}
	
	public void minusPost(Long l) { 
		longOp(l, AST.Opcode.MINUS, false); 
	}
	
	public void multiplyPost(Long l) { 
		longOp(l, AST.Opcode.MULTIPLY, false); 
	}
	
	public void dividePost(Long l) { 
		longOp(l, AST.Opcode.DIVIDE, false); 
	}
	
	public void previousValue() {
		Datapoint d1, d2;
		for (int i = mDatapoints.size() - 1; i > 0; i--) {
			d1 = mDatapoints.get(i-1);
			d2 = mDatapoints.get(i);
			d2.mValue.y = d2.mValue.y - d1.mValue.y;
		}
	}

	public void previousTimestamp() {
		Datapoint d1, d2;
		for (int i = mDatapoints.size() - 1; i > 0; i--) {
			d1 = mDatapoints.get(i-1);
			d2 = mDatapoints.get(i);
			d2.mValue.y = d2.mMillis - d1.mMillis;
		}
		if (mDatapoints.size() > 0) {
			d1 = mDatapoints.get(0);
			d1.mValue.y = 0;
		}
	}

	public void inPeriod(DateUtil.Period p) {
		Datapoint d;
		long ms = DateUtil.mapPeriodToLong(p);
		for (int i = 0; i < mDatapoints.size(); i++) {
			d = mDatapoints.get(i);
			if (ms != 0)
				d.mValue.y /= ms;
		}
	}

	public void asPeriod(DateUtil.Period p) {
		Datapoint d;
		long ms = DateUtil.mapPeriodToLong(p);
		for (int i = 0; i < mDatapoints.size(); i++) {
			d = mDatapoints.get(i);
			d.mValue.y *= ms;
		}
	}
	
	public void timeseriesOp(TimeSeries ts, AST.Opcode op, boolean strict) {
		Datapoint d1, d2;
		Float f;
		for (int i = 0; i < mDatapoints.size(); i++) {
			d1 = mDatapoints.get(i);
			if (strict) {
				d2 = ts.getDatapoints().get(i);
				if (d2 != null) {
					if (op == AST.Opcode.PLUS)
						d1.mValue.y += d2.mValue.y;				
					else if (op == AST.Opcode.MINUS)
						d1.mValue.y -= d2.mValue.y;				
					else if (op == AST.Opcode.MULTIPLY)
						d1.mValue.y *= d2.mValue.y;				
					else if (op == AST.Opcode.DIVIDE) {
						if (d2.mValue.y != 0)
							d1.mValue.y /= d2.mValue.y;				
					}
				}
			} else {
				f = ts.interpolateValue(d1.mMillis);
				if (!(f == null || f.isNaN() || f.isInfinite())) {
					if (op == AST.Opcode.PLUS)
						d1.mValue.y += f;				
					else if (op == AST.Opcode.MINUS)
						d1.mValue.y -= f;				
					else if (op == AST.Opcode.MULTIPLY)
						d1.mValue.y *= f;				
					else if (op == AST.Opcode.DIVIDE) {
						if (f != 0)
							d1.mValue.y /= f;				
					}
				}
			}
		}
	}

	public void timeserisPlus(TimeSeries ts, boolean strict) { 
		timeseriesOp(ts, AST.Opcode.PLUS, strict); 
	}
	
	public void timeserisMinus(TimeSeries ts, boolean strict) { 
		timeseriesOp(ts, AST.Opcode.MINUS, strict); 
	}
	
	public void timeserisMultiply(TimeSeries ts, boolean strict) { 
		timeseriesOp(ts, AST.Opcode.MULTIPLY, strict); 
	}
	
	public void timeserisDivide(TimeSeries ts, boolean strict) { 
		timeseriesOp(ts, AST.Opcode.DIVIDE, strict); 
	}
	
	private void interpolateBoundsToOffscreen() {
		int nDatapoints;
		Datapoint d1 = null;
		Datapoint d2 = null;
		float iy = 0.0f;

		List<Datapoint> preVisible = getVisiblePre();
		List<Datapoint> visible = getVisible();
		List<Datapoint> postVisible = getVisiblePost();

		nDatapoints = mDatapoints.size();
		if (nDatapoints < 2)
			return;
		
		if (postVisible != null) {
			// datapoints after visible range to interpolate to
			d2 = postVisible.get(0);
			if (visible != null) {
				// also datapoints in visible range
				d1 = visible.get(visible.size() - 1);
			} else if (preVisible != null) {
				// no datapoints in visible range, grab last from before the visible range
				d1 = preVisible.get(preVisible.size() - 1);
			}
			if (d2.mMillis > d1.mMillis) {
				iy = (d2.mValue.y - d1.mValue.y) / (d2.mMillis - d1.mMillis);
				if (iy > 0 && d2.mValue.y > mVisibleMaxs.y) {
					mVisibleMaxs.y = d2.mValue.y;
				}
				if (iy < 0 && d2.mValue.y < mVisibleMins.y) {
					mVisibleMins.y = d2.mValue.y;
				}
			}
		}
		if (preVisible != null) {
			// we have a datapoint before the beginning to interpolate
			d1 = preVisible.get(preVisible.size()-1);
			if (visible != null) {
				// also datapoints in visible range
				d2 = visible.get(0);
			} else if (postVisible != null) {
				// no datapoints in visible range, grab first from beyond the visible range
				d2 = postVisible.get(0);
			} else {
				// no datapoints to connect it to
				return;
			}
			if (d1.mMillis < d2.mMillis) {
				iy = (d2.mValue.y - d1.mValue.y) / (d2.mMillis - d1.mMillis);
				if (iy < 0 && d1.mValue.y > mVisibleMaxs.y) {
					mVisibleMaxs.y = d1.mValue.y;
				}
				if (iy > 0 && d1.mValue.y < mVisibleMins.y) {
					mVisibleMins.y = d1.mValue.y;
				}
			}
		}
	}
	
	private void copyDatapoints(TimeSeries source) {
		mDatapoints.clear();
		if (source.mDatapoints == null)
			return;

		for (int i = 0; i < source.mDatapoints.size(); i++) {
			Datapoint d = new Datapoint(source.mDatapoints.get(i));
			mDatapoints.add(d);
		}
		
		return;
	}
	
	private void setColor(int color) {
		mColor = color;
		
		mPathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mPathPaint.setStyle(Paint.Style.STROKE);
		mPathPaint.setStrokeWidth(GraphView.PATH_WIDTH);
		mPathPaint.setColor(color);	

		mPointsPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mPointsPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		mPointsPaint.setStrokeWidth(GraphView.PATH_WIDTH);
		mPointsPaint.setColor(color);	

		mTrendPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mTrendPaint.setStyle(Paint.Style.STROKE);
		mTrendPaint.setStrokeWidth(GraphView.TREND_WIDTH);
		mTrendPaint.setColor(color);	
        DashPathEffect trendDashes = new DashPathEffect(
        		new float[] {GraphView.TREND_DASH_WIDTH, 
        				GraphView.TREND_DASH_WIDTH}, 0);
        mTrendPaint.setPathEffect(trendDashes);

        mTrendMarkerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTrendMarkerPaint.setStyle(Paint.Style.STROKE);
        mTrendMarkerPaint.setStrokeWidth(GraphView.TREND_WIDTH);
        mTrendMarkerPaint.setColor(color);	
        DashPathEffect trendMarkerDashes = new DashPathEffect(
        		new float[] {
        				GraphView.GOAL_DASH_WIDTH/2, 
        				GraphView.GOAL_DASH_WIDTH, 
        				}, 0);
        mTrendMarkerPaint.setPathEffect(trendMarkerDashes);
        
		mGoalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mGoalPaint.setStyle(Paint.Style.STROKE);
		mGoalPaint.setStrokeWidth(GraphView.GOAL_WIDTH);
		mGoalPaint.setColor(color);	
        DashPathEffect goalDashes = new DashPathEffect(
        		new float[] {GraphView.GOAL_DASH_WIDTH, 
        				GraphView.GOAL_DASH_WIDTH}, 0);
        mGoalPaint.setPathEffect(goalDashes);

		mLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mLabelPaint.setStyle(Paint.Style.STROKE);
		mLabelPaint.setStrokeWidth(GraphView.LABEL_WIDTH);
		mLabelPaint.setColor(color);	
	}
}