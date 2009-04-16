///*
// * Copyright (C) 2007 The Android Open Source Project
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package net.redgeek.android.eventrend.db;
//
//import android.database.Cursor;
//import android.graphics.Color;
//
//import net.redgeek.android.eventrecorder.TimeSeriesData;
//import net.redgeek.android.eventrend.util.DateUtil;
//
///**
// * Class encapsulating the database table definition, exportable contents,
// * acceptable values, and convenience routines for interacting with the DB
// * table.
// * 
// * @author barclay
// * 
// */
//public class EvenTrendTimeSeriesData {
//  public static final String AGGREGATION_SUM = "sum";
//  public static final String AGGREGATION_AVG = "average";
//  
//  public static final String TYPE_DISCRETE = "discrete";
//  public static final String TYPE_RANGE = "range";
//  public static final String TYPE_SYNTHETIC = "synthetic";
//
//  public static final String TREND_DOWN_15_GOOD = "trend_down_15_good";
//  public static final String TREND_DOWN_15_BAD = "trend_down_15_bad";
//  public static final String TREND_DOWN_30_GOOD = "trend_down_30_good";
//  public static final String TREND_DOWN_30_BAD = "trend_down_30_bad";
//  public static final String TREND_DOWN_45_GOOD = "trend_down_45_good";
//  public static final String TREND_DOWN_45_BAD = "trend_down_45_bad";
//  public static final String TREND_UP_15_GOOD = "trend_up_15_good";
//  public static final String TREND_UP_15_BAD = "trend_up_15_bad";
//  public static final String TREND_UP_30_GOOD = "trend_up_30_good";
//  public static final String TREND_UP_30_BAD = "trend_up_30_bad";
//  public static final String TREND_UP_45_GOOD = "trend_up_45_good";
//  public static final String TREND_UP_45_BAD = "trend_up_45_bad";
//  public static final String TREND_FLAT = "trend_flat";
//  public static final String TREND_FLAT_GOAL = "trend_flat_goal";
//  public static final String TREND_DOWN_15 = "trend_down_15";
//  public static final String TREND_UP_15 = "trend_up_15";
//  public static final String TREND_UNKNOWN = "trend_unknown";
//}
