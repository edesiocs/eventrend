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
import net.redgeek.android.timeseries.Tuple;

// Note that several tests use equality comparison on float, which could be 
// dangerous in general, but should be safe for such small predefined values.
public class TupleTest extends TestCase {
  public void testConstructor() {
    Tuple input, output;

    input = new Tuple(0.0f, 1.0f);
    assertEquals(0.0f, input.x);
    assertEquals(1.0f, input.y);

    input = new Tuple(1.5f, 2.5f);
    assertEquals(1.5f, input.x);
    assertEquals(2.5f, input.y);

    input = new Tuple(1.5f, 2.5f);
    output = new Tuple(input);
    assertEquals(1.5f, output.x);
    assertEquals(2.5f, output.y);
    assertNotSame(input, output);
  }

  public void testSet() {
    Tuple input, output;

    input = new Tuple(0.0f, 1.0f);
    output = new Tuple(2.0f, 3.0f);
    output.set(input);
    assertEquals(0.0f, output.x);
    assertEquals(1.0f, output.y);

    output.set(2.5f, 3.5f);
    assertEquals(2.5f, output.x);
    assertEquals(3.5f, output.y);
  }

  public void testComparison() {
    Tuple same1, same2, less, greater;

    same1 = new Tuple(0.0f, 1.0f);
    same2 = new Tuple(0.0f, 1.0f);
    less = new Tuple(-1.0f, 1.0f);
    greater = new Tuple(1.0f, 1.0f);
    assertTrue(same1.equals(same2));
    assertFalse(same1.equals(less));
    assertFalse(same1.equals(greater));

    assertTrue(same1.compareTo(same2) == 0);
    assertTrue(same1.compareTo(greater) < 0);
    assertTrue(same1.compareTo(less) > 0);
  }

  public void testTupleOps() {
    Tuple source1, source2, result;

    source1 = new Tuple(1.0f, 2.0f);
    source2 = new Tuple(2.0f, 4.0f);
    result = new Tuple();

    result.set(source1);
    result.plus(source2);
    assertEquals(3.0f, result.x);
    assertEquals(6.0f, result.y);

    result.set(source1);
    result.minus(source2);
    assertEquals(-1.0f, result.x);
    assertEquals(-2.0f, result.y);

    result.set(source1);
    result.multiply(source2);
    assertEquals(2.0f, result.x);
    assertEquals(8.0f, result.y);

    result.set(source1);
    result.divide(source2);
    assertEquals(0.5f, result.x);
    assertEquals(0.5f, result.y);
  }

  public void testScalarOps() {
    Tuple source1, result;

    source1 = new Tuple(1.0f, 2.0f);
    result = new Tuple();

    result.set(source1);
    result.plus(2.0f);
    assertEquals(3.0f, result.x);
    assertEquals(4.0f, result.y);

    result.set(source1);
    result.minus(2.0f);
    assertEquals(-1.0f, result.x);
    assertEquals(0.0f, result.y);

    result.set(source1);
    result.multiply(2.0f);
    assertEquals(2.0f, result.x);
    assertEquals(4.0f, result.y);

    result.set(source1);
    result.divide(2.0f);
    assertEquals(0.5f, result.x);
    assertEquals(1.0f, result.y);
  }

  public void testStaticOps() {
    Tuple source1, source2, result;

    source1 = new Tuple(1.0f, 2.0f);
    source2 = new Tuple(2.0f, 4.0f);

    result = Tuple.plus(source1, source2);
    assertEquals(3.0f, result.x);
    assertEquals(6.0f, result.y);
    assertNotSame(result, source1);
    assertNotSame(result, source2);

    result = Tuple.minus(source1, source2);
    assertEquals(-1.0f, result.x);
    assertEquals(-2.0f, result.y);
    assertNotSame(result, source1);
    assertNotSame(result, source2);

    result = Tuple.multiply(source1, source2);
    assertEquals(2.0f, result.x);
    assertEquals(8.0f, result.y);
    assertNotSame(result, source1);
    assertNotSame(result, source2);

    result = Tuple.divide(source1, source2);
    assertEquals(0.5f, result.x);
    assertEquals(0.5f, result.y);
    assertNotSame(result, source1);
    assertNotSame(result, source2);
  }

  public void testTupleExtremes() {
    Tuple min, max, result;

    min = new Tuple(1.0f, 1.5f);
    max = new Tuple(2.0f, 2.5f);
    result = new Tuple();

    result.set(min);
    result.min(max);
    assertTrue(result.equals(min));

    result.set(min);
    result.max(max);
    assertTrue(result.equals(max));

    result.set(max);
    result.min(min);
    assertTrue(result.equals(min));

    result.set(max);
    result.max(min);
    assertTrue(result.equals(max));

    Tuple one = new Tuple(2.0f, 4.0f);
    Tuple two = new Tuple(1.0f, 5.0f);

    result.set(one);
    result.min(two);
    assertEquals(1.0f, result.x);
    assertEquals(4.0f, result.y);

    result.set(one);
    result.max(two);
    assertEquals(2.0f, result.x);
    assertEquals(5.0f, result.y);
  }

  public void testStaticExtremes() {
    Tuple min, max, result;

    min = new Tuple(1.0f, 1.5f);
    max = new Tuple(2.0f, 2.5f);

    result = Tuple.min(min, max);
    assertTrue(result.equals(min));
    assertNotSame(result, min);
    assertNotSame(result, max);

    result = Tuple.max(min, max);
    assertTrue(result.equals(max));
    assertNotSame(result, min);
    assertNotSame(result, max);

    Tuple one = new Tuple(2.0f, 4.0f);
    Tuple two = new Tuple(1.0f, 5.0f);

    result = Tuple.min(one, two);
    assertEquals(1.0f, result.x);
    assertEquals(4.0f, result.y);
    assertNotSame(result, min);
    assertNotSame(result, max);

    result = Tuple.max(one, two);
    assertEquals(2.0f, result.x);
    assertEquals(5.0f, result.y);
    assertNotSame(result, min);
    assertNotSame(result, max);
  }
}
