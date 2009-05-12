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

package net.redgeek.android.eventgrapher.primitives;

/**
 * Basic tuple representation used throughout. Provide basic constructors and
 * arithmetic operations on tuples.
 * 
 * @author barclay
 */
public class GraphTuple implements Comparable<GraphTuple> {
  public long  mX1;
  public long  mX2;
  public float mY;
  public FloatTuple mScreen1;
  public FloatTuple mScreen2;

  public GraphTuple() {
    mX1 = 0;
    mX2 = 0;
    mY = 0.0f;
    mScreen1 = new FloatTuple(0.0f, 0.0f);
    mScreen2 = new FloatTuple(0.0f, 0.0f);
  }

  public GraphTuple(long start, long end, float value) {
    mX1 = start;
    mX2 = end;
    mY = value;
    mScreen1 = new FloatTuple(0.0f, 0.0f);
    mScreen2 = new FloatTuple(0.0f, 0.0f);
  }

  public GraphTuple(GraphTuple t) {
    mX1 = t.mX1;
    mX2 = t.mX2;
    mY = t.mY;
    mScreen1 = new FloatTuple(t.mScreen1);
    mScreen2 = new FloatTuple(t.mScreen2);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof GraphTuple))
      return false;
    GraphTuple other = (GraphTuple) obj;
    return mX1 == other.mX1 && mX2 == other.mX2 && mY == other.mY;
  }

  public int compareTo(GraphTuple other) {
    if (this.mX1 < other.mX1)
      return -1;
    else if (this.mX1 > other.mX1)
      return 1;
    return 0;
  }

  @Override
  public String toString() {
    return String.format("([%d, %d], %f)", mX1, mX2, mY);
  }
}
