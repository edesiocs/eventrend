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

package net.redgeek.android.eventrend.test.primitives;

import junit.framework.TestCase;
import net.redgeek.android.timeseries.Datapoint;
import net.redgeek.android.timeseries.Tuple;

// Note that several tests use equality comparison on float, which could be 
// dangerous in general, but should be safe for such small predefined values.
public class DatapointTest extends TestCase {
  public void testConstructors() {
    Datapoint d;

    d = new Datapoint();
    assertEquals(0L, d.mMillis);
    assertEquals(0L, d.mCatId);
    assertEquals(0L, d.mEntryId);
    assertEquals(0.0f, d.mStdDev);
    assertEquals(0, d.mNEntries);
    assertEquals(false, d.mSynthetic);
    assertEquals(0.0f, d.mValue.x);
    assertEquals(0.0f, d.mValue.y);
    assertEquals(0.0f, d.mValueScreen.x);
    assertEquals(0.0f, d.mValueScreen.y);
    assertEquals(0.0f, d.mTrend.x);
    assertEquals(0.0f, d.mTrend.y);
    assertEquals(0.0f, d.mTrendScreen.x);
    assertEquals(0.0f, d.mTrendScreen.y);

    d = new Datapoint(1L, 2.0f, 3, 4, 5);
    assertEquals(1L, d.mMillis);
    assertEquals(3L, d.mCatId);
    assertEquals(4L, d.mEntryId);
    assertEquals(0.0f, d.mStdDev);
    assertEquals(5, d.mNEntries);
    assertEquals(false, d.mSynthetic);
    assertEquals((float) 1L, d.mValue.x);
    assertEquals(2.0f, d.mValue.y);
    assertEquals(0.0f, d.mValueScreen.x);
    assertEquals(0.0f, d.mValueScreen.y);
    assertEquals(0.0f, d.mTrend.x);
    assertEquals(0.0f, d.mTrend.y);
    assertEquals(0.0f, d.mTrendScreen.x);
    assertEquals(0.0f, d.mTrendScreen.y);

    d.mStdDev = 10.0f;
    d.mValueScreen = new Tuple(11.0f, 12.0f);
    d.mTrend = new Tuple(13.0f, 14.0f);
    d.mTrendScreen = new Tuple(15.0f, 16.0f);
    d.mSynthetic = true;
    Datapoint copy = new Datapoint(d);

    assertEquals(1L, copy.mMillis);
    assertEquals(3L, copy.mCatId);
    assertEquals(4L, copy.mEntryId);
    assertEquals(10.0f, copy.mStdDev);
    assertEquals(5, copy.mNEntries);
    assertEquals(true, copy.mSynthetic);
    assertEquals((float) 1L, copy.mValue.x);
    assertEquals(2.0f, copy.mValue.y);
    assertEquals(11.0f, copy.mValueScreen.x);
    assertEquals(12.0f, copy.mValueScreen.y);
    assertEquals(13.0f, copy.mTrend.x);
    assertEquals(14.0f, copy.mTrend.y);
    assertEquals(15.0f, copy.mTrendScreen.x);
    assertEquals(16.0f, copy.mTrendScreen.y);
    assertNotSame(d.mValue, copy.mValue);
    assertNotSame(d.mValueScreen, copy.mValueScreen);
    assertNotSame(d.mTrend, copy.mTrend);
    assertNotSame(d.mTrendScreen, copy.mTrendScreen);
  }

  public void testEquals() {
    Datapoint d1 = new Datapoint(1L, 2.0f, 3, 4, 5);
    Datapoint d2 = new Datapoint(d1);

    assertFalse(d1.equals(new Long(10)));
    assertTrue(d1.equals(d2));
    assertTrue(d2.equals(d1));

    d2.mValueScreen.x = 100.0f;
    d2.mValueScreen.y = 100.0f;
    d2.mTrendScreen.x = 100.0f;
    d2.mTrendScreen.y = 100.0f;
    assertTrue(d1.equals(d2));
    assertTrue(d2.equals(d1));

    d2 = new Datapoint(d1);
    d2.mCatId = 10;
    assertFalse(d1.equals(d2));
    assertFalse(d2.equals(d1));

    d2 = new Datapoint(d1);
    d2.mMillis = 10;
    assertFalse(d1.equals(d2));
    assertFalse(d2.equals(d1));

    d2 = new Datapoint(d1);
    d2.mEntryId = 10;
    assertFalse(d1.equals(d2));
    assertFalse(d2.equals(d1));

    d2 = new Datapoint(d1);
    d2.mValue.x = 10;
    assertFalse(d1.equals(d2));
    assertFalse(d2.equals(d1));

    d2 = new Datapoint(d1);
    d2.mTrend.x = 10;
    assertFalse(d1.equals(d2));
    assertFalse(d2.equals(d1));

    d2 = new Datapoint(d1);
    d2.mSynthetic = true;
    assertFalse(d1.equals(d2));
    assertFalse(d2.equals(d1));
  }

  public void testCompare() {
    Datapoint d1 = new Datapoint();
    Datapoint d2 = new Datapoint(d1);

    d1.mMillis = 100L;
    d2.mMillis = 100L;
    assertTrue(d2.timestampEqual(d1));
    d2.mMillis = 101L;
    assertFalse(d2.timestampEqual(d1));

    d1.mMillis = 100L;
    d2.mMillis = 100L;
    assertEquals(0, d1.compare(d1, d2));

    d1.mMillis = 101L;
    d2.mMillis = 100L;
    assertEquals(1, d1.compare(d1, d2));

    d1.mMillis = 100L;
    d2.mMillis = 101L;
    assertEquals(-1, d1.compare(d1, d2));
  }
}
