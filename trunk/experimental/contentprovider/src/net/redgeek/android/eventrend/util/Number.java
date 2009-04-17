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

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Various number-related routines and classes that are frequently used.
 * 
 * @author barclay
 */
public class Number {
  /**
   * Default number of decimal places to round to:
   */
  public static final int DECIMAL_PLACES = 2;

  /**
   * Round a float to the default number of decimal places.
   * 
   * @param value
   *          The value to round.
   * @return The rounded value as a float.
   * @see #DECIMAL_PLACES
   */
  public static float Round(float value) {
    return Round(value, DECIMAL_PLACES);
  }

  /**
   * Round a float to the specified number of decimal places.
   * 
   * @param value
   *          The value to round.
   * @param places
   *          The number of decimal points.
   * @return The rounded value as a float.
   */
  public static float Round(float value, int places) {
    float p = (float) Math.pow(10, places);
    value = value * p;
    float tmp = Math.round(value);
    return (float) tmp / p;
  }

  /**
   * Clamp a <code>value</code> to <code>min</code> or <code>max</code>,
   * inclusive.
   * 
   * @param value
   *          The value to clamp.
   * @param min
   *          The minimum value.
   * @param max
   *          The maximum value.
   * @return If <code>value</code> is greater than <code>max</code> then
   *         <code>max</code>, else if <code>value</code> is less than
   *         <code>min</code> then <code>min</code>, else <code>value</code>.
   */
  public static float Clamp(float value, float min, float max) {
    if (value > max)
      return max;
    if (value < min)
      return min;
    return value;
  }

  public enum TrendState {
    DOWN_15_GOOD,
    DOWN_15_BAD,
    DOWN_30_GOOD,
    DOWN_30_BAD,
    DOWN_45_GOOD,
    DOWN_45_BAD,
    UP_15_GOOD,
    UP_15_BAD,
    UP_30_GOOD,
    UP_30_BAD,
    UP_45_GOOD,
    UP_45_BAD,
    DOWN_15,
    UP_15,
    FLAT,
    FLAT_GOAL,
    UNKNOWN
  };

  public static TrendState getTrendState(float oldTrend, float newTrend,
      float goal, float sensitivity, float stdDev) {
    sensitivity = sensitivity * stdDev;
    float half = sensitivity / 2.0f;
    float quarter = sensitivity / 4.0f;

    if (oldTrend == newTrend) {
      // truly flat trend
      if (newTrend == goal)
        // perfect!
        return TrendState.FLAT_GOAL;
      else if (newTrend < goal && newTrend + quarter > goal)
        // flat near the goal!
        return TrendState.FLAT_GOAL;
      else if (newTrend > goal && newTrend - quarter < goal)
        // flat near the goal!
        return TrendState.FLAT_GOAL;
      else
        return TrendState.FLAT;
    } else if (oldTrend > newTrend) {
      // going down
      if (oldTrend > goal && newTrend > goal) {
        // toward goal
        if (oldTrend - newTrend > sensitivity)
          // huge drop
          return TrendState.DOWN_45_GOOD;
        else if (oldTrend - newTrend > half)
          // big drop
          return TrendState.DOWN_30_GOOD;
        else if (oldTrend - newTrend > quarter)
          // little drop
          return TrendState.DOWN_15_GOOD;
        else {
          // under bounds for flat
          if (newTrend - quarter < goal)
            // flat near the goal!
            return TrendState.FLAT_GOAL;
          else
            // flat elsewhere
            return TrendState.FLAT;
        }
      } else if (oldTrend < goal && newTrend < goal) {
        // away from goal
        if (oldTrend - newTrend > sensitivity)
          // huge drop
          return TrendState.DOWN_45_BAD;
        else if (oldTrend - newTrend > half)
          // big drop
          return TrendState.DOWN_30_BAD;
        else if (oldTrend - newTrend > quarter)
          // little drop
          return TrendState.DOWN_15_BAD;
        else {
          // under bounds for flat
          if (newTrend + quarter > goal)
            // flat near the goal!
            return TrendState.FLAT_GOAL;
          else
            // flat elsewhere
            return TrendState.FLAT;
        }
      } else
        // crossing goal line
        return TrendState.DOWN_15;
    } else if (oldTrend < newTrend) {
      // going up
      if (oldTrend < goal && newTrend < goal) {
        // toward goal
        if (newTrend - oldTrend > sensitivity)
          // big rise
          return TrendState.UP_45_GOOD;
        else if (newTrend - oldTrend > half)
          // little rise
          return TrendState.UP_30_GOOD;
        else if (newTrend - oldTrend > quarter)
          // little rise
          return TrendState.UP_15_GOOD;
        else {
          // under bounds for flat
          if (newTrend + quarter > goal)
            // flat near the goal!
            return TrendState.FLAT_GOAL;
          else
            // flat elsewhere
            return TrendState.FLAT;
        }
      } else if (oldTrend > goal && newTrend > goal) {
        // away from goal
        if (newTrend - oldTrend > sensitivity)
          // big rise
          return TrendState.UP_45_BAD;
        else if (newTrend - oldTrend > half)
          // little rise
          return TrendState.UP_30_BAD;
        else if (newTrend - oldTrend > quarter)
          // little rise
          return TrendState.UP_15_BAD;
        else {
          // under bounds for flat
          if (newTrend - quarter < goal)
            // flat near the goal!
            return TrendState.FLAT_GOAL;
          else
            // flat elsewhere
            return TrendState.FLAT;
        }
      } else {
        // crossing goal line
        return TrendState.UP_15;
      }
    } else
      // ??
      return TrendState.UNKNOWN;
  }

//  public static String mapTrendStateToString(TrendState state) {
//    String trendStr;
//    
//    if (state == TrendState.DOWN_45_GOOD)
//      trendStr = CategoryDbTable.KEY_TREND_DOWN_45_GOOD;
//    else if (state == TrendState.DOWN_45_BAD)
//      trendStr = CategoryDbTable.KEY_TREND_DOWN_45_BAD;
//    else if (state == TrendState.DOWN_15_GOOD)
//      trendStr = CategoryDbTable.KEY_TREND_DOWN_15_GOOD;
//    else if (state == TrendState.DOWN_30_BAD)
//      trendStr = CategoryDbTable.KEY_TREND_DOWN_30_BAD;
//    else if (state == TrendState.DOWN_30_GOOD)
//      trendStr = CategoryDbTable.KEY_TREND_DOWN_30_GOOD;
//    else if (state == TrendState.DOWN_15_BAD)
//      trendStr = CategoryDbTable.KEY_TREND_DOWN_15_BAD;
//    else if (state == TrendState.UP_45_GOOD)
//      trendStr = CategoryDbTable.KEY_TREND_UP_45_GOOD;
//    else if (state == TrendState.UP_45_BAD)
//      trendStr = CategoryDbTable.KEY_TREND_UP_45_BAD;
//    else if (state == TrendState.UP_30_GOOD)
//      trendStr = CategoryDbTable.KEY_TREND_UP_30_GOOD;
//    else if (state == TrendState.UP_30_BAD)
//      trendStr = CategoryDbTable.KEY_TREND_UP_30_BAD;
//    else if (state == TrendState.UP_15_GOOD)
//      trendStr = CategoryDbTable.KEY_TREND_UP_15_GOOD;
//    else if (state == TrendState.UP_15_BAD)
//      trendStr = CategoryDbTable.KEY_TREND_UP_15_BAD;
//    else if (state == TrendState.DOWN_15)
//      trendStr = CategoryDbTable.KEY_TREND_DOWN_15;
//    else if (state == TrendState.UP_15)
//      trendStr = CategoryDbTable.KEY_TREND_UP_15;
//    else if (state == TrendState.FLAT)
//      trendStr = CategoryDbTable.KEY_TREND_FLAT;
//    else if (state == TrendState.FLAT_GOAL)
//      trendStr = CategoryDbTable.KEY_TREND_FLAT_GOAL;
//    else
//      // if (state == TrendState.UNKNOWN)
//      trendStr = CategoryDbTable.KEY_TREND_UNKNOWN;
//    return trendStr;
//  }

  /**
   * An exponentially smoothed weighted moving average. Not thread safe. Trend
   * is calculated thusly: <br>
   * <code>
	 * trend[n] := trend[n-1] + smoothing_percentage * (value[n] - value[n-1])
	 * </code>
   * 
   * @author barclay
   */
  public static class SmoothedTrend {
    /**
     * Default smoothing percentage. This value will be used to scale the
     * previous entry by multiplying the previous entry's value and adding that
     * to the current value, so a smoothing percentage of 0.1 is 10%.
     */
    private static final float DEFAULT_SMOOTHING = 0.1f;

    private int mNEntries = 0;
    private float mSmoothing = DEFAULT_SMOOTHING;
    private float mSum = 0.0f;
    private boolean mFirst = true;
    private float mTrendLast = 0.0f;

    public float mTrendPrev = 0.0f;
    public float mTrend = 0.0f;
    public float mMin = 0.0f;
    public float mMax = 0.0f;
    public float mMean = 0.0f;

    /**
     * Default constructor. Set the smoothing percentage to the default.
     * 
     * @see #DEFAULT_SMOOTHING
     */
    public SmoothedTrend() {
    }

    /**
     * Constructor
     * 
     * @param smoothing
     *          Sets the smoothing percentage to the specified value.
     * @see #DEFAULT_SMOOTHING
     */
    public SmoothedTrend(float smoothing) {
      mSmoothing = smoothing;
    }

    /**
     * Copy Constructor
     * 
     * @param source
     *          Returns a new instance of Trend with all data set to source.
     * @see #DEFAULT_SMOOTHING
     */
    public SmoothedTrend(SmoothedTrend source) {
      mNEntries = source.mNEntries;
      mSmoothing = source.mSmoothing;
      mSum = source.mSum;
      mFirst = source.mFirst;
      mTrendLast = source.mTrendLast;
      mTrendPrev = source.mTrendPrev;
      mTrend = source.mTrend;
      mMin = source.mMin;
      mMax = source.mMax;
      mMean = source.mMean;
    }

    /**
     * Return the smoothing constant used by the Trend.
     * 
     * @return The smoothing constant as a float.
     */
    public float getSmoothing() {
      return mSmoothing;
    }

    /**
     * Update the trend with a new value. The value is implicitly "later" in the
     * sequence than all previous values.
     * 
     * @param val
     *          The value to add to the series.
     */
    public void update(float val) {
      mNEntries++;
      mSum += val;

      float oldMean = mMean;
      mMean += (val - oldMean) / mNEntries;

      // T(n) = T(n-1) + 0.1(V(n) - T(n-1))
      // : T(n) is the trend number for day n
      // : V(n) is the value number for day n
      // : S is the smoothing factor (default 0.1)
      if (mFirst == true) {
        mFirst = false;
        mTrend = val;
        mMin = val;
        mMax = val;
      } else {
        mTrend = mTrendLast + (mSmoothing * (val - mTrendLast));
        if (mTrend < mMin)
          mMin = mTrend;
        if (mTrend > mMax)
          mMax = mTrend;
      }
      mTrendPrev = mTrendLast;
      mTrendLast = mTrend;
    }
  }

  /**
   * Class for keeping track of various statistics intended to be updated
   * incrementally, including total number of updates, sum, mean, variance, and
   * standard deviation of the series. Not thread safe.
   * 
   * @author barclay
   */
  public static class RunningStats {
    public int mNDatapoints = 0;
    public int mNEntries = 0;
    public float mSum = 0.0f;
    public float mMean = 0.0f;
    public float mEntryMean = 0.0f;
    public float mVarSum = 0.0f;
    public float mVar = 0.0f;
    public float mStdDev = 0.0f;

    /**
     * Sole constructor. Initializes all stats to 0.
     */
    public RunningStats() {
    }

    /**
     * Copy Constructor
     * 
     * @param source
     *          Returns a new instance of RunningStats with all data set to
     *          source.
     */
    public RunningStats(RunningStats source) {
      mNDatapoints = source.mNDatapoints;
      mNEntries = source.mNEntries;
      mSum = source.mSum;
      mMean = source.mMean;
      mEntryMean = source.mEntryMean;
      mVarSum = source.mVarSum;
      mVar = source.mVar;
      mStdDev = source.mStdDev;
    }

    /**
     * update the statistics with the specified value. The value may be an
     * aggregate of several other values, as indicated by the second parameter.
     * A separate value will be recored for per-entry and and per-update means.
     * 
     * @param val
     *          The value to update the statistics with.
     * @param nEntries
     *          The number of entries this value is an aggregate of.
     */
    public void update(float val, int nEntries) {
      mNDatapoints++;
      mNEntries += nEntries;
      mSum += val;

      // Mean is calculated thusly to avoid float expansion and contraction,
      // which
      // would minimize accuracy.
      float oldMean = mMean;
      mMean += (val - oldMean) / mNDatapoints;
      mVarSum += (val - oldMean) * (val - mMean);
      mVar = mVarSum / mNDatapoints;
      mStdDev = (float) Math.sqrt(mVar);

      if (mNEntries > 0) {
        float oldEntryMean = mEntryMean;
        mEntryMean += (val - oldEntryMean) / mNEntries;
      }

      return;
    }
  }

  /**
   * Class for keeping track of the Standard Deviation over the last X values.
   * 
   * @author barclay
   */
  public static class WindowedStdDev {
    private Lock mLock;
    private ArrayList<Float> mValues;
    private int mHistory;

    /**
     * Sole constructor. Initializes all stats to 0.
     */
    public WindowedStdDev(int history) {
      mHistory = history;
      mValues = new ArrayList<Float>(mHistory);
      mLock = new ReentrantLock();
    }

    /**
     * Copy Constructor
     * 
     * @param source
     *          Returns a new instance of WindowedStdDev with all data set to
     *          source.
     */
    public WindowedStdDev(WindowedStdDev source) {
      mLock = new ReentrantLock();
      source.waitForLock();
      mHistory = source.mHistory;
      mValues = new ArrayList<Float>(mHistory);
      for (int i = 0; i < source.mValues.size(); i++) {
        try {
          mValues.add(new Float(source.mValues.get(i)));          
        } catch(IndexOutOfBoundsException e) {
          break;
        }
      }
      source.unlock();
    }

    public void waitForLock() {
      while (lock() == false) {
      }
    }

    public boolean lock() {
      try {
        return mLock.tryLock(250L, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        return false;
      }
    }

    public void unlock() {
      mLock.unlock();
    }

    /**
     * update the std dev with the specified value.
     * 
     * @param val
     *          The value to update the statistics with.
     */
    public void update(float val) {
      waitForLock();
      mValues.add(new Float(val));
      if (mValues.size() > mHistory) {
        try {
          mValues.remove(0);
        } catch(IndexOutOfBoundsException e) {
          // nothing
        }
      }
      unlock();
      return;
    }

    /**
     * Fetch current Standard Deviation.
     * 
     * @param val
     *          The value to update the statistics with.
     */
    public float getStandardDev() {
      float mean = 0.0f;
      float meanSqr = 0.0f;
      float variance = 0.0f;
      float delta = 0.0f;
      float val = 0.0f;

      waitForLock();

      int nValues = mValues.size();
      for (int i = 0; i < nValues; i++) {
        try {
          val = mValues.get(i);
          delta = val - mean;
          mean = mean + delta / (i + 1);
          meanSqr = meanSqr + delta * (val - mean);
        } catch(IndexOutOfBoundsException e) {
          break;
        }
      }
      unlock();
      
      variance = meanSqr / nValues;
      return (float) Math.sqrt(variance);
    }
  }

  /**
   * Performs a standard Pearson linear correlation on multiple series of data
   * at one, returning the results in a matrix.
   * 
   * @author barclay
   */
  public static class LinearMatrixCorrelation {
    private int mNumSeries = 0;
    private int mNEntries = 0;

    private Float[] mSum;
    private Float[] mMean;
    private Float[] mSumSquare;
    private Float[] mStdDev;
    private Float[][] mSumCoproduct;
    private Float[][] mCovariance;
    private Float[][] mCorrelation;

    /**
     * Constructor. Allocates internal data structures and zero's out the output
     * matrix data.
     * 
     * @param numSeries
     *          The number of series that will be included in each call to
     *          <code>update()</code>.
     */
    public LinearMatrixCorrelation(int numSeries) {
      mNumSeries = numSeries;

      mSum = new Float[numSeries];
      mMean = new Float[numSeries];
      mSumSquare = new Float[numSeries];
      mStdDev = new Float[numSeries];

      mSumCoproduct = new Float[numSeries][];
      mCovariance = new Float[numSeries][];
      mCorrelation = new Float[numSeries][];

      for (int i = 0; i < numSeries; i++) {
        mSum[i] = 0.0f;
        mMean[i] = 0.0f;
        mSumSquare[i] = 0.0f;
        mStdDev[i] = 0.0f;

        mSumCoproduct[i] = new Float[numSeries];
        mCovariance[i] = new Float[numSeries];
        mCorrelation[i] = new Float[numSeries];

        for (int j = 0; j < numSeries; j++) {
          mSumCoproduct[i][j] = 0.0f;
          mCovariance[i][j] = 0.0f;
          mCorrelation[i][j] = 0.0f;
        }
      }
    }

    /**
     * Adds a vector (array) of values, 1 per series, to the calculations.
     * Graphically, all values are considered to be the at the same X (or Y)
     * position, and the values in the array argument denote the corresponding Y
     * (or X) value specific to the each series. Thus, the parameter x is an
     * array of values x[0 .. numSeries-1], one value per series, all of which
     * occured at the same "time." If a series has no such value, the entry in
     * the array should be null, as the length of the array must match the
     * <code>numSeries</code> past into the constructor at each invocation.
     * 
     * @param x
     *          The values for each series as Floats.
     * @return true if the input acceptable, else false if the length of
     *         <code>x[]</code> does not match <code>numSeries</code> or a value
     *         less than 1 was passed to the constructor.
     * @see LinearMatrixCorrelation#LinearMatrixCorrelation
     */
    public boolean update(Float[] x) {
      float oldMean;

      if (x.length != mNumSeries)
        return false;

      if (mNEntries + 1 > mNumSeries)
        return false;

      mNEntries++;

      float sweep = (mNEntries - 1.0f) / mNEntries;
      for (int i = 0; i < x.length; i++) {
        if (x[i] != null) {
          mSum[i] += x[i];
          oldMean = mMean[i];
          mMean[i] += (x[i] - oldMean) / mNEntries;
          mSumSquare[i] += (x[i] - oldMean) * (x[i] - mMean[i]);
          mStdDev[i] = (float) Math.sqrt(mSumSquare[i] * sweep);
        }
      }

      for (int i = 0; i < x.length; i++) {
        if (x[i] != null) {
          for (int j = i + 1; j < x.length; j++) {
            if (x[j] != null) {
              mSumCoproduct[i][j] += (x[i] - mMean[i]) * (x[j] - mMean[j]);
              mCovariance[i][j] = mSumCoproduct[i][j] * sweep;
              mCorrelation[i][j] = mCovariance[i][j]
                  / (mStdDev[i] * mStdDev[j]);
              mCorrelation[j][i] = mCovariance[i][j]
                  / (mStdDev[i] * mStdDev[j]);
              mCorrelation[i][j] = mCovariance[i][j]
                  / (mStdDev[i] * mStdDev[j]);
            }
          }
        }
        mCorrelation[i][i] = 1.0f;
      }

      return true;
    }

    /**
     * Returns a reference to the correlation output matrix. The upper right
     * triangle is a mirror of the lower left, and the dividing diagonal the
     * identity correlation, i.e., 1.0f. In order to run through the matrix
     * without duplicates (e.g., processing both output[i][j] and output[j][i],
     * use a construct like: <br>
     * 
     * <pre>
     * for (int i = 0; i &lt; output.length; i++) {
     *   for (int j = i+1; j &lt; output.length; j++) {
     *     if (output[i][j] != null) { ... }
     *   }
     * }
     * </pre>
     * 
     * Note that any correlations that could not be calculated, either due to
     * lack of datapoints or structure of the data, will be null.
     * 
     * @return Float[][], the output correlation matrix.
     */
    public Float[][] getCorrelations() {
      return mCorrelation;
    }

    /**
     * Return a string interpretation of the linear correlation value. Note that
     * this is highly dependent on the data being correlated, and should be no
     * means be taken as gospel.
     * 
     * @param c
     *          The correlation value, should be -1.0f <= c <= 1.0f.
     * @return A string interpretation.
     */
    public static String correlationToString(float c) {
      if (c > 0.5)
        return "Strong";
      if (c < -0.5)
        return "Inverse Strong";
      if (c > 0.3)
        return "Medium";
      if (c < -0.3)
        return "Inverse Medium";
      return "Weak";
    }
  }
}
