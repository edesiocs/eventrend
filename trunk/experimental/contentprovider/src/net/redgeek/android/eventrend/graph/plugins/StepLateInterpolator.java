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

package net.redgeek.android.eventrend.graph.plugins;

import net.redgeek.android.eventrend.R;
import net.redgeek.android.timeseries.Tuple;
import android.graphics.Path;

/**
 * Interpolates a continuous step function between two points. The Y value of
 * <code>first</code> is retained between the two datapoints, hence step occurs
 * "Late" (at the entrance of the <code>second</code> datapoint.)
 * 
 * @author barclay
 */
public class StepLateInterpolator implements TimeSeriesInterpolator {
  public static final String NAME = "StepLate";
  public static final int HELP_RES_ID = R.string.interpolation_help_steplate;

  public StepLateInterpolator() {
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

    r1.x = second.x;
    r1.y = first.y;
    return result;
  }

  public Float interpolateX(Tuple first, Tuple second, float atY) {
    if (first.equals(second))
      return null;
    if (first.x == second.x)
      return null;

    if (atY > first.x && atY < second.x)
      return new Float(second.x);
    return null;
  }

  public Float interpolateY(Tuple first, Tuple second, float atX) {
    if (first.equals(second))
      return null;
    if (first.x == second.x)
      return null;

    if (atX > first.x && atX < second.x)
      return new Float(first.y);
    return null;
  }

  public void updatePath(Path path, Tuple first, Tuple second) {
    if (first == null && second != null)
      path.moveTo(second.x, second.y);
    else if (first != null && second == null)
      path.moveTo(first.x, first.y);
    else {
      Tuple[] tuples = interpolate(first, second);
      if (tuples != null) {
        for (int j = 0; j < tuples.length; j++) {
          path.lineTo(tuples[j].x, tuples[j].y);
        }
      }
      path.lineTo(second.x, second.y);
    }
  }
}
