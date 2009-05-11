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

import net.redgeek.android.eventgrapher.primitives.Tuple;
import net.redgeek.android.eventrend.R;
import android.graphics.Path;

/**
 * Interpolates a continuous step function between two points. The change in the
 * Y-value occurs at the midpoint of the two points.
 * 
 * @author barclay
 */
public class StepMidInterpolator implements TimeSeriesInterpolator {
  public static final String NAME = "StepMid";
  public static final int HELP_RES_ID = R.string.interpolation_help_stepmid;

  public StepMidInterpolator() {
  }

  public int getHelpResId() {
    return HELP_RES_ID;
  }

  public String getName() {
    return NAME;
  }

  public Tuple[] interpolate(Tuple first, Tuple second) {
    if (first.equals(second) == true)
      return new Tuple[] { new Tuple(first) };
    if (first.x == second.x)
      return null;

    Tuple[] result = new Tuple[2];
    Tuple r1 = new Tuple(first);
    Tuple r2 = new Tuple(second);

    r1.x = first.x + ((second.x - first.x) / 2.0f);
    r2.x = r1.x;

    result[0] = r1;
    result[1] = r2;
    return result;
  }

  public Float interpolateX(Tuple first, Tuple second, float atY) {
    return null;
  }

  public Float interpolateY(Tuple first, Tuple second, float atX) {
    if (first.equals(second))
      return null;
    if (first.x == second.x)
      return null;

    if (atX > first.x && atX < second.x) {
      if (atX <= first.x + ((second.x - first.x) / 2.0f))
        return new Float(first.y);
      return new Float(second.y);
    }
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
