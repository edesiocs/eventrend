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
 
package net.redgeek.android.eventrecorder;

interface IEventRecorderService {
  /** Returns the status of the service, either ERSERVICE_OK or ERSERVICE_BUSY
   */
  int getServiceStatus();

  /** Returns the total number of zerofills being processed
   */
  int getServiceFillsTotal();

  /** Returns number of zerofills completed
   */
  int getServiceFillsPerformed();
    
  /** Returns the _id of the timeseries, 0 for not found, < 0 for error.
   *  See TimeSeriesProvider
   */
  long getTimeSeriesId( in String name );

  /** Create a new event for the timeseries and set a start marker.
   *  Returns the _id of the datapoint.
   *  See TimeSeriesProvider
   */
  long recordEventStart( in long timeSeriesId );

  /** Stops the currently running event for the time series.
   * Returns the _id of the datapoint.
   * See TimeSeriesProvider
   */
  long recordEventStop( in long timeSeriesId );

  /** Records a discrete event for the current time.
   *  Returns the _id of the datapoint.
   *  See TimeSeriesProvider
   */
  long recordEventNow( in long timeSeriesId, in double value );

  /** Records a discrete event with the timestamp specified.
   *  Returns the _id of the datapoint.
   *  See TimeSeriesProvider
   */
  long recordEvent( in long timeSeriesId, in long timestamp, in double value );
}
