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
import net.redgeek.android.eventrend.primitives.Tuple;
import android.graphics.Path;

/** Interpolates a cubic spline, with the controls points set such that
 * the slope of the path entering and exiting a point will be as close
 * to 0 as possible while being monotonically increasing on the X-axis.
 * 
 * <p>Note that this uses the native cubicTo() routines to draw the path,
 * and as such, to prevent re-calculating the cubics for interpolate{X,Y},
 * a linear interpolation (approximation) is used.
 * 
 * @author barclay
 */
public class CubicInterpolator implements TimeSeriesInterpolator {
	public static final String NAME        = "Cubic";
	public static final int    HELP_RES_ID = R.string.interpolation_help_cubic;

	public CubicInterpolator() {}
	
	public String getName() {
		return NAME;
	}

	public int getHelpResId() {
		return HELP_RES_ID;
	}

	// This is the same StepMid, but we'll be generating control
	// point for a cubic curve, since android alread has a cubicTo
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

	// To interpolate a position on the line, since I don't want to
	// re-calculate the cubics that cubicTo already has, I just use
	// linear interpolation
	public Float interpolateX(Tuple first, Tuple second, float atY) {
		if (first.equals(second) == true)
			return first.x;
		float slope = (second.y - first.y) / (second.x - first.x);
		return new Float(first.x + (slope * (atY - first.y)));
	}

	public Float interpolateY(Tuple first, Tuple second, float atX) {
		if (first.equals(second) == true)
			return first.y;
		float slope = (second.y - first.y) / (second.x - first.x);
		return new Float(first.y + (slope * (atX - first.x)));
	}
	
	public void updatePath(Path path, Tuple first, Tuple second) {
		if (first == null && second != null)
			path.moveTo(second.x, second.y);
		else if (first != null && second == null)
			path.moveTo(first.x, first.y);
		else {
			Tuple[] controls = interpolate(first, second);
			if (controls != null && controls.length == 2) {
				path.cubicTo(controls[0].x, controls[0].y, 
							 controls[1].x, controls[1].y, 
							 second.x, second.y);    
			}
		}
	}
}

