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

package net.redgeek.android.eventrecorder.synthetic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

public final class SeriesData {
  public static class Datum {
    public int    mTsStart;
    public int    mTsEnd;
    public double mValue;
    
    public Datum() {
    }
    
    public Datum(int tsStart, int tsEnd, double value) {
       mTsStart = tsStart;
       mTsEnd = tsEnd;
       mValue = value;
    }
    
    @Override
    public String toString() {
      return "[" + mTsStart + ", " + mTsEnd + "] " + mValue;
    }
  }
  
  public ArrayList<Datum> mData;
  public String           mName;
  public int              mTsEarliest;
  
  public SeriesData() {
    mTsEarliest = Integer.MAX_VALUE;
    mData = new ArrayList<Datum>();
    mName = "";
  }
  
  @Override
  public String toString() {
    int size = mData.size();
    String s = mName + ": ";
    for (int i = 0; i < size; i++) {
      Datum d = mData.get(i);
      s += d.toString() + " ";
    }
    return s;
  }

  public void floatOp(Double f, AST.Opcode op, boolean pre) {
    if (f == null || f.isNaN() || f.isInfinite())
      return;

    Datum d;
    for (int i = 0; i < mData.size(); i++) {
      d = mData.get(i);
      if (pre == false) {
        if (op == AST.Opcode.PLUS)
          d.mValue += f;
        else if (op == AST.Opcode.MINUS)
          d.mValue -= f;
        else if (op == AST.Opcode.MULTIPLY)
          d.mValue *= f;
        else if (op == AST.Opcode.DIVIDE) {
          if (f != 0)
            d.mValue /= f;
        }
      } else {
        if (op == AST.Opcode.PLUS)
          d.mValue = f + d.mValue;
        else if (op == AST.Opcode.MINUS)
          d.mValue = f - d.mValue;
        else if (op == AST.Opcode.MULTIPLY)
          d.mValue = f * d.mValue;
        else if (op == AST.Opcode.DIVIDE) {
          if (d.mValue != 0)
            d.mValue = f / d.mValue;
        }
      }
    }
  }

  public void plusPre(Double f) {
    floatOp(f, AST.Opcode.PLUS, true);
  }

  public void minusPre(Double f) {
    floatOp(f, AST.Opcode.MINUS, true);
  }

  public void multiplyPre(Double f) {
    floatOp(f, AST.Opcode.MULTIPLY, true);
  }

  public void dividePre(Double f) {
    floatOp(f, AST.Opcode.DIVIDE, true);
  }

  public void plusPost(Double f) {
    floatOp(f, AST.Opcode.PLUS, false);
  }

  public void minusPost(Double f) {
    floatOp(f, AST.Opcode.MINUS, false);
  }

  public void multiplyPost(Double f) {
    floatOp(f, AST.Opcode.MULTIPLY, false);
  }

  public void dividePost(Double f) {
    floatOp(f, AST.Opcode.DIVIDE, false);
  }

  public void longOp(Long l, AST.Opcode op, boolean pre) {
    if (l == null)
      return;

    Datum d;
    for (int i = 0; i < mData.size(); i++) {
      d = mData.get(i);
      if (pre == false) {
        if (op == AST.Opcode.PLUS)
          d.mValue += l;
        else if (op == AST.Opcode.MINUS)
          d.mValue -= l;
        else if (op == AST.Opcode.MULTIPLY)
          d.mValue *= l;
        else if (op == AST.Opcode.DIVIDE) {
          if (l != 0)
            d.mValue /= l;
        }
      } else {
        if (op == AST.Opcode.PLUS)
          d.mValue = l + d.mValue;
        else if (op == AST.Opcode.MINUS)
          d.mValue = l - d.mValue;
        else if (op == AST.Opcode.MULTIPLY)
          d.mValue = l * d.mValue;
        else if (op == AST.Opcode.DIVIDE) {
          if (d.mValue != 0)
            d.mValue = l / d.mValue;
        }
      }
    }
  }

  public void plusPre(Long l) {
    longOp(l, AST.Opcode.PLUS, true);
  }

  public void minusPre(Long l) {
    longOp(l, AST.Opcode.MINUS, true);
  }

  public void multiplyPre(Long l) {
    longOp(l, AST.Opcode.MULTIPLY, true);
  }

  public void dividePre(Long l) {
    longOp(l, AST.Opcode.DIVIDE, true);
  }

  public void plusPost(Long l) {
    longOp(l, AST.Opcode.PLUS, false);
  }

  public void minusPost(Long l) {
    longOp(l, AST.Opcode.MINUS, false);
  }

  public void multiplyPost(Long l) {
    longOp(l, AST.Opcode.MULTIPLY, false);
  }

  public void dividePost(Long l) {
    longOp(l, AST.Opcode.DIVIDE, false);
  }

  public void previousValue() {
    Datum d1, d2;
    for (int i = mData.size() - 1; i > 0; i--) {
      d1 = mData.get(i - 1);
      d2 = mData.get(i);
      d2.mValue = d2.mValue - d1.mValue;
    }
  }

  public void previousTimestamp() {
    Datum d1, d2;
    for (int i = mData.size() - 1; i > 0; i--) {
      d1 = mData.get(i - 1);
      d2 = mData.get(i);
      d2.mValue = d2.mTsStart - d1.mTsStart;
    }
    if (mData.size() > 0) {
      d1 = mData.get(0);
      d1.mValue = 0;
    }
  }

  private Datum findNeighbor(int timestamp, boolean pre) {
    Datum d = null;

    if (mData == null || mData.size() < 1)
      return d;

    int min = 0;
    int max = mData.size() - 1;
    int mid = max / 2;

    d = mData.get(mid);
    while (d != null) {
      if (d.mTsStart == timestamp || (timestamp > d.mTsStart && timestamp < d.mTsEnd)) {
        return d;
      } else if (max < min) {
        if (pre == true) {
          if (d.mTsStart > timestamp) {
            if (mid - 1 >= 0)
              d = mData.get(mid - 1);
            else
              d = null;
          }
        } else {
          if (d.mTsStart < timestamp) {
            if (mid + 1 < mData.size())
              d = mData.get(mid + 1);
            else
              d = null;
          }
        }
        return d;
      } else if (d.mTsStart < timestamp) {
        min = mid + 1;
      } else if (d.mTsStart > timestamp) {
        max = mid - 1;
      }
      mid = min + ((max - min) / 2);

      // Check to see if we were trying to run off the end, if so, just
      // return the first or last entry.
      if (mid >= mData.size() && pre == true)
        return d;
      if (mid < 0 && pre == false)
        return d;

      if (mid < 0 || mid > mData.size() - 1)
        break;
      d = mData.get(mid);
    }

    return null;
  }

  public Datum findPreNeighbor(int timestamp) {
    return findNeighbor(timestamp, true);
  }

  public Datum findPostNeighbor(int timestamp) {
    return findNeighbor(timestamp, false);
  }
  
  public Datum interpolateValue(int timestamp) {
    Datum result = new Datum();
    result.mTsStart = timestamp;
    result.mTsEnd = timestamp;

    Datum d2 = findPostNeighbor(timestamp); // inclusive
    if (d2 == null)
      return null;

    if (d2.mTsStart == timestamp) {
      result.mValue = d2.mValue;
    }

    Datum d1 = findPreNeighbor(timestamp); // inclusive
    if (d1 == null)
      return null;

    if (d1.mTsStart == d2.mTsStart && d1.mValue == d2.mValue) {
      result.mValue = d1.mValue;
      return result;
    }

    if (d1.mTsStart == d2.mTsStart)
      return null;
    
    double slope = (d2.mValue - d1.mValue) / (d2.mTsStart - d1.mTsStart);
    result.mValue = d1.mValue + (slope * (timestamp - d1.mTsStart));
      
    return result;
  }

  // We need to gather a list of all timestamps so we can interpolate from
  // ones series to the other, and vice versa, in order to make the operations
  // commutative
  public SeriesData seriesOp(SeriesData ts, AST.Opcode op) {
    Datum res1, res2;
    SeriesData newSeries = new SeriesData();
    TreeMap<Integer, Boolean> timestamps = new TreeMap<Integer, Boolean>();

    for (int i = 0; i < mData.size(); i++) {
      timestamps.put(new Integer(mData.get(i).mTsStart), true);
    }
    for (int i = 0; i < ts.mData.size(); i++) {
      timestamps.put(new Integer(ts.mData.get(i).mTsStart), true);
    }

    Iterator<Integer> iterator = timestamps.keySet().iterator();
    while (iterator.hasNext()) {
      int seconds = iterator.next();

      res1 = interpolateValue(seconds);
      res2 = ts.interpolateValue(seconds);

      // We handle invalid interpolations slightly differing depending on
      // opcode. For example, for + and -, it could be justified that
      // adding
      // or subtracting to/from a Datum that doesn't exist means that
      // the
      // missing value should be 0 (this assumption can certainly be
      // challenged,
      // but for most of the use cases for the application, I believe this
      // is
      // correct.) However, for * and /, should we attempt to return the
      // identity, or 0? It's unclear, so we bail on the calculation.
      if (op == AST.Opcode.PLUS || op == AST.Opcode.MINUS) {
        if (res1 == null)
          res1 = new Datum(seconds, seconds, 0.0f);
        if (res2 == null)
          res2 = new Datum(seconds, seconds, 0.0f);
      }
      if (res1 == null || Double.isNaN(res1.mValue) || Double.isInfinite(res1.mValue))
        continue;
      if (res2 == null || Double.isNaN(res2.mValue) || Double.isInfinite(res2.mValue))
        continue;

      if (op == AST.Opcode.PLUS)
        res1.mValue += res2.mValue;
      else if (op == AST.Opcode.MINUS)
        res1.mValue -= res2.mValue;
      else if (op == AST.Opcode.MULTIPLY)
        res1.mValue *= res2.mValue;
      else if (op == AST.Opcode.DIVIDE) {
        if (res2.mValue == 0)
          continue;
        else
          res1.mValue /= res2.mValue;
      }

      newSeries.mData.add(res1);
    }
    
    return newSeries;
  }

  public SeriesData seriesPlus(SeriesData ts) {
    return seriesOp(ts, AST.Opcode.PLUS);
  }

  public SeriesData seriesMinus(SeriesData ts) {
    return seriesOp(ts, AST.Opcode.MINUS);
  }

  public SeriesData seriesMultiply(SeriesData ts) {
    return seriesOp(ts, AST.Opcode.MULTIPLY);
  }

  public SeriesData seriesDivide(SeriesData ts) {
    return seriesOp(ts, AST.Opcode.DIVIDE);
  }
}