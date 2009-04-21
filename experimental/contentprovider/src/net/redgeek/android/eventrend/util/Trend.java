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

package net.redgeek.android.eventrend.util;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import net.redgeek.android.eventrecorder.DateMapCache;
import net.redgeek.android.eventrecorder.TimeSeriesData;
import net.redgeek.android.eventrend.util.Aggregator.Aggregate;

import java.util.ArrayList;

public class Trend {
  public static final int TREND_DOWN_15_GOOD = 1;
  public static final int TREND_DOWN_15_BAD = 2;
  public static final int TREND_DOWN_30_GOOD = 3;
  public static final int TREND_DOWN_30_BAD = 4;
  public static final int TREND_DOWN_45_GOOD = 5;
  public static final int TREND_DOWN_45_BAD = 6;
  public static final int TREND_UP_15_GOOD = 7;
  public static final int TREND_UP_15_BAD = 8;
  public static final int TREND_UP_30_GOOD = 9;
  public static final int TREND_UP_30_BAD = 10;
  public static final int TREND_UP_45_GOOD = 11;
  public static final int TREND_UP_45_BAD = 12;
  public static final int TREND_DOWN_15 = 13;
  public static final int TREND_UP_15 = 14;
  public static final int TREND_FLAT = 15;
  public static final int TREND_FLAT_GOAL = 16;
  public static final int TREND_UNKNOWN = 17;

  public static int getTrendIconState(float oldTrend, float newTrend,
      float goal, float sensitivity, float stdDev) {
    sensitivity = sensitivity * stdDev;
    float half = sensitivity / 2.0f;
    float quarter = sensitivity / 4.0f;

    if (oldTrend == newTrend) {
      // truly flat trend
      if (newTrend == goal)
        // perfect!
        return TREND_FLAT_GOAL;
      else if (newTrend < goal && newTrend + quarter > goal)
        // flat near the goal!
        return TREND_FLAT_GOAL;
      else if (newTrend > goal && newTrend - quarter < goal)
        // flat near the goal!
        return TREND_FLAT_GOAL;
      else
        return TREND_FLAT;
    } else if (oldTrend > newTrend) {
      // going down
      if (oldTrend > goal && newTrend > goal) {
        // toward goal
        if (oldTrend - newTrend > sensitivity)
          // huge drop
          return TREND_DOWN_45_GOOD;
        else if (oldTrend - newTrend > half)
          // big drop
          return TREND_DOWN_30_GOOD;
        else if (oldTrend - newTrend > quarter)
          // little drop
          return TREND_DOWN_15_GOOD;
        else {
          // under bounds for flat
          if (newTrend - quarter < goal)
            // flat near the goal!
            return TREND_FLAT_GOAL;
          else
            // flat elsewhere
            return TREND_FLAT;
        }
      } else if (oldTrend < goal && newTrend < goal) {
        // away from goal
        if (oldTrend - newTrend > sensitivity)
          // huge drop
          return TREND_DOWN_45_BAD;
        else if (oldTrend - newTrend > half)
          // big drop
          return TREND_DOWN_30_BAD;
        else if (oldTrend - newTrend > quarter)
          // little drop
          return TREND_DOWN_15_BAD;
        else {
          // under bounds for flat
          if (newTrend + quarter > goal)
            // flat near the goal!
            return TREND_FLAT_GOAL;
          else
            // flat elsewhere
            return TREND_FLAT;
        }
      } else
        // crossing goal line
        return TREND_DOWN_15;
    } else if (oldTrend < newTrend) {
      // going up
      if (oldTrend < goal && newTrend < goal) {
        // toward goal
        if (newTrend - oldTrend > sensitivity)
          // big rise
          return TREND_UP_45_GOOD;
        else if (newTrend - oldTrend > half)
          // little rise
          return TREND_UP_30_GOOD;
        else if (newTrend - oldTrend > quarter)
          // little rise
          return TREND_UP_15_GOOD;
        else {
          // under bounds for flat
          if (newTrend + quarter > goal)
            // flat near the goal!
            return TREND_FLAT_GOAL;
          else
            // flat elsewhere
            return TREND_FLAT;
        }
      } else if (oldTrend > goal && newTrend > goal) {
        // away from goal
        if (newTrend - oldTrend > sensitivity)
          // big rise
          return TREND_UP_45_BAD;
        else if (newTrend - oldTrend > half)
          // little rise
          return TREND_UP_30_BAD;
        else if (newTrend - oldTrend > quarter)
          // little rise
          return TREND_UP_15_BAD;
        else {
          // under bounds for flat
          if (newTrend - quarter < goal)
            // flat near the goal!
            return TREND_FLAT_GOAL;
          else
            // flat elsewhere
            return TREND_FLAT;
        }
      } else {
        // crossing goal line
        return TREND_UP_15;
      }
    } else
      // ??
      return TREND_UNKNOWN;
  }
}
