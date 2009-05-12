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

package net.redgeek.android.eventgrapher.primitives;

import java.util.ArrayList;

/**
 * Transform absolute value into the viewable size.
 * 
 * @author barclay
 * 
 */
public class Transformation {
  private FloatTuple mPlotSize; // dimension of on-screen plottable area
  private FloatTuple mVirtualSize; // logical dimension of plot
  private FloatTuple mScale;
  private FloatTuple mShift; // so we can calculate and plot closer to 0,0

  boolean hasData = false;

  public Transformation() {
    setup(new FloatTuple(0.0f, 0.0f));
  }

  public Transformation(float width, float height) {
    setup(new FloatTuple(width, height));
  }

  public Transformation(FloatTuple plotSize) {
    setup(plotSize);
  }

  private void setup(FloatTuple plotSize) {
    mPlotSize = new FloatTuple(plotSize);
    mVirtualSize = new FloatTuple(plotSize);
    mScale = new FloatTuple(1.0f, 1.0f);
    mShift = new FloatTuple(0.0f, 0.0f);
  }

  public void setPlotSize(FloatTuple plotSize) {
    mPlotSize = plotSize;
    mVirtualSize.set(mPlotSize);
    updateScale();
  }

  public void setVirtualSize(FloatTuple mins, FloatTuple maxs) {
    mVirtualSize.set(maxs.x - mins.x, maxs.y - mins.y);
    mShift.set(mins);
    updateScale();
  }

  public FloatTuple getPlotSize() {
    return new FloatTuple(mPlotSize);
  }

  public FloatTuple getVirtualSize() {
    return new FloatTuple(mVirtualSize);
  }

  private void updateScale() {
    mScale.set(mPlotSize);
    if (mVirtualSize.x == 0 || mVirtualSize.y == 0
        || mVirtualSize.y < (Float.MIN_VALUE * 10))
      mScale.set(1.0f, 1.0f);
    else
      mScale.divide(mVirtualSize);
  }

  public void clear() {
    setup(mPlotSize);
  }

  // Virtual to Plot
  private void transformV2Pinplace(FloatTuple t) {
    t.minus(mShift).multiply(mScale);
    t.y = mPlotSize.y - t.y;
  }

  // Plot to Virtual
  private void transformP2Vinplace(FloatTuple t) {
    t.y = mPlotSize.y + t.y;
    t.divide(mScale).plus(mShift);
  }

  // Virtual to Plot
  private FloatTuple transformV2P(FloatTuple in) {
    if (hasData == false)
      return null;
    FloatTuple out = new FloatTuple(in);
    transformV2Pinplace(out);
    return out;
  }

  // Virtual to Plot
  private void transformV2P(FloatTuple in, FloatTuple out) {
    if (hasData == false)
      return;
    out.set(in);
    transformV2Pinplace(out);
  }

  // Plot to Virtual
  private FloatTuple transformP2V(FloatTuple in) {
    if (hasData == false)
      return null;

    FloatTuple out = new FloatTuple(in);
    transformP2Vinplace(out);
    return out;
  }

  // Plot to Virtual
  private void transformP2V(FloatTuple in, FloatTuple out) {
    if (hasData == false)
      return;
    out.set(in);
    transformP2Vinplace(out);
  }

  public void transformPath(ArrayList<Datapoint> path) {
    if (path == null)
      return;

    for (int i = 0; i < path.size(); i++) {
      transformDatapoint(path.get(i));
    }
    return;
  }

  public void transformDatapoint(Datapoint point) {
    hasData = true;
    FloatTuple t = new FloatTuple(point.mTsStart, point.mValue);
    transformV2P(t, point.mScreenValue1);
    t.set(point.mTsEnd, point.mValue);
    transformV2P(t, point.mScreenValue2);
    t.set(point.mTsStart, point.mTrend);
    transformV2P(t, point.mScreenTrend1);
    t.set(point.mTsEnd, point.mTrend);
    transformV2P(t, point.mScreenTrend2);
  }

  // Virtual to Plot
  public ArrayList<FloatTuple> pathV2P(ArrayList<FloatTuple> in) {
    if (in == null)
      return null;

    ArrayList<FloatTuple> out = new ArrayList<FloatTuple>(in.size());
    for (int i = 0; i < in.size(); i++) {
      out.add(transformV2P(in.get(i)));
    }

    return out;
  }

  // Plot to Virtual
  public ArrayList<FloatTuple> pathP2V(ArrayList<FloatTuple> in) {
    if (in == null)
      return null;

    ArrayList<FloatTuple> out = new ArrayList<FloatTuple>(in.size());
    for (int i = 0; i < in.size(); i++) {
      out.add(transformP2V(in.get(i)));
    }

    return out;
  }

  // Virtual to Plot
  public FloatTuple tupleV2P(FloatTuple in) {
    return transformP2V(in);
  }

  // Plot to Virtual
  public FloatTuple tupleP2V(FloatTuple in) {
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
