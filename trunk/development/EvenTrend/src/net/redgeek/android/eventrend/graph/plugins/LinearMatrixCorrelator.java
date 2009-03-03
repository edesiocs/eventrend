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

import java.util.ArrayList;
import java.util.List;

import net.redgeek.android.eventrend.primitives.Datapoint;
import net.redgeek.android.eventrend.primitives.TimeSeries;
import net.redgeek.android.eventrend.util.Number;

public class LinearMatrixCorrelator implements TimeSeriesCorrelator {
	public LinearMatrixCorrelator() {}

	public Float[][] correlate(ArrayList<TimeSeries> timeseries) {
		TimeSeries ts1, ts2;
		Datapoint  d;

		if (timeseries == null)
			return null;

		int nSeries = timeseries.size();
		Float[] values = new Float[nSeries];

		Number.LinearMatrixCorrelation lcm;
		List<Datapoint> datapoints;

		lcm = new Number.LinearMatrixCorrelation(nSeries);
		for (int i = 0; i < nSeries; i++) {
			ts1 = timeseries.get(i);
			datapoints = ts1.getVisible();

			for (int j = 0; j < datapoints.size(); j++) {
				d = datapoints.get(j);
				values[i] = new Float(d.mValue.y);

				for (int k = i + 1; k < nSeries; k++) {
					ts2 = timeseries.get(k);
					values[k] = ts2.interpolateScreenCoord((long)d.mValue.x);
				}

				lcm.update(values);
			}
		}

		return lcm.getCorrelations();
	}
}

