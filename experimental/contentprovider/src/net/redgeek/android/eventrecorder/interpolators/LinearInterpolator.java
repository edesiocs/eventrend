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

import net.redgeek.android.eventrend.R;
import net.redgeek.android.eventrend.primitives.Tuple;
import android.graphics.Path;

/**
 * Interpolates a straight line between two points
 * 
 * @author barclay
 */
public class LinearInterpolator implements TimeSeriesInterpolator {
  public static final String NAME = "Linear";
  public static final int HELP_RES_ID = R.string.interpolation_help_linear;

  public LinearInterpolator() {
  }

  public int getHelpResId() {
    return HELP_RES_ID;
  }

  public String getName() {
    return NAME;
  }

  public Tuple[] interpolate(Tuple first, Tuple second) {
    Tuple[] result = new Tuple[1];
    Tuple r1 = new Tuple(second);
    result[0] = r1;
    if (first.equals(second) == true)
      return result;
    if (first.x == second.x)
      return null;

    r1.minus(first).divide(2.0f).plus(first);
    return result;
  }

  public Float interpolateX(Tuple first, Tuple second, float atY) {
    if (first.equals(second) == true)
      return null;
    if (first.x == second.x)
      return null;

    float slope = (second.y - first.y) / (second.x - first.x);
    return new Float(first.x + (slope * (atY - first.y)));
  }

  public Float interpolateY(Tuple first, Tuple second, float atX) {
    if (first.equals(second) == true)
      return null;
    if (first.x == second.x)
      return null;

    float slope = (second.y - first.y) / (second.x - first.x);
    return new Float(first.y + (slope * (atX - first.x)));
  }

  public void updatePath(Path path, Tuple first, Tuple second) {
    if (first == null && second != null)
      path.moveTo(second.x, second.y);
    else if (first != null && second == null)
      path.moveTo(first.x, first.y);
    else
      path.lineTo(second.x, second.y);
  }
}
