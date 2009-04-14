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

package net.redgeek.android.eventrecorder.interpolators;

import net.redgeek.android.eventrend.primitives.Tuple;
import android.graphics.Path;

/**
 * Interface for plugging in different interpolators, currently only used by the
 * TimeSeris.
 * 
 * Interpolators must support the following operations:
 * <ul>
 * <li>Given two Tuples (coordinates), return an array of Tuples that represent
 * the interpolation between the two points. Depending on the function, this may
 * be one point, such a with linear interpolation, or it may be multiple points,
 * such as with cubic interpolation.
 * <li>Given two Tuples (coordinates) and a Y-value, return a Float representing
 * the X-value associated with the interpolation of the two tuples at the
 * Y-value.
 * <li>Given two Tuples (coordinates) and an X-value, return a Float
 * representing the Y-value associated with the interpolation of the two tuples
 * at the X-value.
 * <li>Give two Tuples (coordinates), update a path on a Canvas between them.
 * The reason for including this is to allow interpolators to take advantage of
 * native drawing routines: for example, it is much faster to use the native
 * cubicTo() routine than to calculate the cubic curve between the points and
 * plot them with a series of lineTo() calls. Thus, the current interpolation
 * for the CubicInterpolator actually returns a list of the control points via
 * <code>interpolate(first, second)</code>, which are used for the native
 * cubicTo() inside of it's drawPath() function.
 * <li>A function that returns a name of the interpolator.
 * <li>A function that returne a resource identifier for a descriptive help
 * string.
 * </ul>
 * 
 * @author barclay
 */
public interface TimeSeriesInterpolator {
  /**
   * The display name of this interpolator. Must be unique and instantiated set
   * in the implementing class.
   */
  String getName();

  /**
   * The resource id of a textual description of how the Interpolator operates.
   * Displayed in a pop-up help dialog.
   */
  int getHelpResId();

  /**
   * Interpolate between two points, returning a Tuple[] of points defining the
   * function between the two points. If there is no point between the two
   * points, it is highly likely that null should be returned, although that is
   * particular to the interpolator being defined.
   * 
   * <p>
   * Note that some interpolations have a concept of order/directionality, such
   * as step functions, so it should be assuming that <code>first</code> comes
   * "before" <code>second</code> for whatever ordering is appropriate for the
   * interpolator.
   * 
   * @param first
   * @param second
   * @return Tuple[] of points between the two points, may be null.
   */
  Tuple[] interpolate(Tuple first, Tuple second);

  /**
   * Calculates the X-value of the interpolation between points
   * <code>first</code> and <code>second</code> at the value Y-value of
   * <code>atY</code>. Note that null may be a valid return value. For example,
   * X may have multiple values at <code>atY</code>, or none at all.
   * 
   * @param first
   * @param second
   * @param atY
   * @return The X value at value Y, or null.
   */
  Float interpolateX(Tuple first, Tuple second, float atY);

  /**
   * Calculates the Y-value of the interpolation between points
   * <code>first</code> and <code>second</code> at the value X-value of
   * <code>atX</code>. Note that null may be a valid return value. For example,
   * Y may have multiple values at <code>atX</code>, or none at all.
   * 
   * @param first
   * @param second
   * @param atX
   * @return The Y value at value X, or null.
   */
  Float interpolateY(Tuple first, Tuple second, float atX);

  /**
   * Draws a path between the two points. Note that for the first point of the
   * series, one of the two datapoints will be null, as there is nothing to
   * connect "from".
   * 
   * @param path
   *          The Path to update.
   * @param first
   * @param second
   */
  void updatePath(Path path, Tuple first, Tuple second);
}
