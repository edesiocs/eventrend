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

package net.redgeek.android.eventgrapher.plugins;

import java.util.ArrayList;

import net.redgeek.android.eventrend.primitives.TimeSeries;

/**
 * Interface for plugging in different correlators, currently only used by the
 * TimeSeriesCollector. Correlators must be able to take an ArrayList of
 * TimerSeries, and return a Float[][] (matrix) such that the values of
 * (Float[i][j] == Float[j][i] == the calculated correlation of TimeSeries i to
 * TimeSeries j). Float[i][i] should always be 1.0f, as it is the correlation of
 * a timeseries to itself, which should be perfectly correlated.
 * 
 * @author barclay
 */
public interface TimeSeriesCorrelator {
  Float[][] correlate(ArrayList<TimeSeries> timeseries);
}
