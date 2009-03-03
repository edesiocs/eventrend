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

package net.redgeek.android.eventrend.graph;

import java.util.ArrayList;

import net.redgeek.android.eventrend.primitives.Datapoint;
import net.redgeek.android.eventrend.primitives.Tuple;

/** Transform absolute value into the viewable size.
 * 
 * @author barclay
 *
 */
public class Transformation {
	private Tuple mPlotSize;    // dimension of on-screen plottable area
	private Tuple mVirtualSize; // logical dimension of plot
	private Tuple mScale;
	private Tuple mShift;       // so we can calculate and plot closer to 0,0
	
	boolean hasData = false;

	public Transformation() {
		setup(new Tuple(0.0f, 0.0f));
	}

	public Transformation(float width, float height) {
		setup(new Tuple(width, height));
	}

	public Transformation(Tuple plotSize) {
		setup(plotSize);
	}

	private void setup(Tuple plotSize) {
		mPlotSize = new Tuple(plotSize);
		mVirtualSize = new Tuple(plotSize);
		mScale = new Tuple(1.0f, 1.0f);
		mShift = new Tuple(0.0f, 0.0f);
	}
	
	public void setPlotSize(Tuple plotSize) {
		mPlotSize = plotSize;
		mVirtualSize.set(mPlotSize);
		updateScale();
	}

	public void setVirtualSize(Tuple mins, Tuple maxs) {
		mVirtualSize.set(maxs.x - mins.x, maxs.y - mins.y);
		mShift.set(mins);
		updateScale();
	}
		
	public Tuple getPlotSize() {
		return new Tuple(mPlotSize);
	}

	public Tuple getVirtualSize() {
		return new Tuple(mVirtualSize);
	}

	private void updateScale() {
		mScale.set(mPlotSize);
		if (mVirtualSize.x == 0 || mVirtualSize.y == 0)
			mScale.set(0.0f, 0.0f);
		else
			mScale.divide(mVirtualSize);
	}
	
	public void clear() {
		setup(mPlotSize);
	}

	// Virtual to Plot
	private void transformV2Pinplace(Tuple t) {
		t.minus(mShift).multiply(mScale);
		t.y = mPlotSize.y - t.y;
	}
	
	// Plot to Virtual
	private void transformP2Vinplace(Tuple t) {
		t.y = mPlotSize.y + t.y;
		t.divide(mScale).plus(mShift);
	}
	
	// Virtual to Plot
	private Tuple transformV2P(Tuple in) {
		if (hasData == false)
			return null;	
		Tuple out = new Tuple(in);
		transformV2Pinplace(out);
		return out;
	}

	// Virtual to Plot
	private void transformV2P(Tuple in, Tuple out) {
		if (hasData == false)
			return;	
		out.set(in);
		transformV2Pinplace(out);
	}

	// Plot to Virtual
	private Tuple transformP2V(Tuple in) {
		if (hasData == false)
			return null;
		
		Tuple out = new Tuple(in);
		transformP2Vinplace(out);
		return out;
	}

	// Plot to Virtual
	private void transformP2V(Tuple in, Tuple out) {
		if (hasData == false)
			return;
		out.set(in);
		transformP2Vinplace(out);
	}

	public void transformPath(ArrayList<Datapoint> path) {
		if (path == null)
			return;
		
		for (int i = 0; i< path.size(); i++) {
			transformDatapoint(path.get(i));
		}
		return;
	}

	public void transformDatapoint(Datapoint point) {
		hasData = true;
		transformV2P(point.mTrend, point.mTrendScreen);
		transformV2P(point.mValue, point.mValueScreen);
	}

	// Virtual to Plot
	public ArrayList<Tuple> pathV2P(ArrayList<Tuple> in) {
		if (in == null)
			return null;
		
		ArrayList<Tuple> out = new ArrayList<Tuple>(in.size());
		for (int i = 0; i< in.size(); i++) {
			out.add(transformV2P(in.get(i)));
		}
		
		return out;
	}

	// Plot to Virtual
	public ArrayList<Tuple> pathP2V(ArrayList<Tuple> in) {
		if (in == null)
			return null;
		
		ArrayList<Tuple> out = new ArrayList<Tuple>(in.size());
		for (int i = 0; i< in.size(); i++) {
			out.add(transformP2V(in.get(i)));
		}

		return out;
	}

	// Virtual to Plot
	public Tuple tupleV2P(Tuple in) {
		return transformP2V(in);
	}

	// Plot to Virtual
	public Tuple tupleP2V(Tuple in) {
		return transformV2P(in);		
	}
	
	public float scaleXDimension(float in) {
		return in * mScale.x;
	}

	public float scaleYDimension(float in) {
		return in * mScale.y;		
	}

	public float shiftXDimension(float in) {
		return in - mShift.x;
	}

	public float shiftYDimension(float in) {
		return in - mShift.y;
	}
}
