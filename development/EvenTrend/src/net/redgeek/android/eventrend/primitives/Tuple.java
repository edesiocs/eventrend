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

package net.redgeek.android.eventrend.primitives;

/** Basic tuple representation used throughout.  Provide basic constructors and
 * arithmetic operations on tuples.
 * 
 * @author barclay
 */
public class Tuple implements Comparable<Tuple> {
	public float x;
	public float y;

	/** Creates a Tuple initialized to (0.0f, 0.0f).
	 */
	public Tuple() {
		x = 0.0f;
		y = 0.0f;
	}

	/** Creates a new Tuple initialized with the values specified.
	 * 
	 * @param x X-value.
	 * @param y Y-value.
	 */
	public Tuple(float x, float y) {
		this.x = x;
		this.y = y;
	}	

	/** Copy constructor.  Creates a new tuple with the values of <code>t</code>
	 * 
	 * @param t The Tuple with which to copy values from.
	 */
	public Tuple(Tuple t) {
		this.x = t.x;
		this.y = t.y;
	}
	
	/** Set a Tuple to the value of another Tuple without creating a new Tuple.
	 * Essentially the assignment operator.
	 * 
	 * @param t The Tuple with which to copy values from.
	 * @return The original Tuple.
	 */
	public Tuple set(Tuple t) {
		x = t.x;
		y = t.y;
		return this;
	}

	/** Set a Tuple to the values of x and y creating a new Tuple.
	 * Essentially the assignment operator.
	 * 
	 * @param x The x value to assign.
	 * @param y The y value to assign.
	 * @return The original Tuple.
	 */
	public Tuple set(float x, float y) {
		this.x = x;
		this.y = y;
		return this;
	}

	/** Equality test.  Returns true iff both tuples have the same x and y values.
	 *  @return boolean
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Tuple))
			return false;
		Tuple other = (Tuple) obj;
		return x == other.x && y == other.y;
	}
	
	/** X-axis Comparison test for Comparable.  Returns < 0, 0, or > 0 if 
	 * <code>this</code>'s X-value is less than, equal to, or greater than
	 * <code>other</code>'s X-value, respectively.
	 * @return int
	 */
	public int compareTo(Tuple other) {
    	if (this.x < other.x)
        	return -1;
        else if (this.x > other.x)
        	return 1;
        return 0;
    }

	/** Display the Tuple as a string.
	 * @return The String representation.
	 */
	@Override
	public String toString() {
		return String.format("(%f, %f)", x, y);
	}
  
	/** Generate the standard hash code for the Tuple by bit packing into an int.
	 * @return The hashcode as an int.
	 */
	@Override
	public int hashCode() {
		return (Float.floatToRawIntBits(x) >> 16) + Float.floatToRawIntBits(y);
	}
	  
	/** Adds <code>other</code> to <code>this</code>, modifying <code>this</code> and
	 * returning it.
	 * @param other The Tuple to add.
	 * @return <code>this</code>
	 */
	public Tuple plus(Tuple other) {
		x += other.x;
		y += other.y;
		return this;
	}

	/** Adds <code>other</code> to both <code>this.x</code> and <code>this.y</code>,
	 * modifying <code>this</code> and returning it.
	 * @param other The value to add to both <code>x</code> and <code>y</code>.
	 * @return <code>this</code>
	 */
	public Tuple plus(float other) {
		x += other;
		y += other;
		return this;
	}

	/** Adds Tuples <code>a</code> and <code>b</code>, generating a new Tuple and
	 * returning it.  Does not modify <code>a</code> or <code>b</code>.
	 * @param a A Tuple
	 * @param b A Tuple
	 * @return A new Tuple that is the sum of <code>a</code> and <code>b</code>.
	 */
	public static Tuple plus(Tuple a, Tuple b) {
		return new Tuple(a.x + b.x, a.y + b.y);
	}
	
	/** Subtracts Tuple <code>other</code> from <code>this</code>, modifying 
	 * <code>this</code> and returning it.
	 * @param other The Tuple to subtract.
	 * @return <code>this</code>
	 */
	public Tuple minus(Tuple other) {
		x -= other.x;
		y -= other.y;
		return this;
	}

	/** Subtracts <code>other</code> from both <code>this.x</code> and <code>this.y</code>,
	 * modifying <code>this</code> and returning it.
	 * @param other The value to subtract from both <code>x</code> and <code>y</code>.
	 * @return <code>this</code>
	 */
	public Tuple minus(float other) {
		x -= other;
		y -= other;
		return this;
	}

	/** Subtracts Tuple <code>b</code> from <code>a</code>, generating a new Tuple and
	 * returning it.  Does not modify <code>a</code> or <code>b</code>.
	 * @param a A Tuple
	 * @param b A Tuple
	 * @return A new Tuple that is the different of <code>a</code> and <code>b</code>.
	 */
	public static Tuple minus(Tuple a, Tuple b) {
		return new Tuple(a.x - b.x, a.y - b.y);
	}

	/** Multiplies <code>this</code> by <code>other</code>, modifying <code>this</code> and
	 * returning it.
	 * @param other The Tuple to multiply by.
	 * @return <code>this</code>
	 */
	public Tuple multiply(Tuple other) {
		x *= other.x;
		y *= other.y;
		return this;
	}

	/** Multiplies both <code>this.x</code> and <code>this.y</code> by <code>other</code>,
	 * modifying <code>this</code> and returning it.
	 * @param other The value to multiply both <code>x</code> and <code>y</code> by.
	 * @return <code>this</code>
	 */
	public Tuple multiply(float other) {
		x *= other;
		y *= other;
		return this;
	}

	/** Multiplies Tuples <code>a</code> and <code>b</code>, generating a new Tuple and
	 * returning it.  Does not modify <code>a</code> or <code>b</code>.
	 * @param a A Tuple
	 * @param b A Tuple
	 * @return A new Tuple that is the product of <code>a</code> and <code>b</code>.
	 */
	public static Tuple multiply(Tuple a, Tuple b) {
		return new Tuple(a.x * b.x, a.y * b.y);
	}

	/** Divides <code>this</code> by <code>other</code>, modifying <code>this</code> and
	 * returning it.  Does not check for division by zero.
	 * @param other The Tuple to divide by.
	 * @return <code>this</code>
	 */
	public Tuple divide(Tuple other) {
		x /= other.x;
		y /= other.y;
		return this;
	}

	/** Divides both <code>this.x</code> and <code>this.y</code> by <code>other</code>,
	 * modifying <code>this</code> and returning it.  Does not check for division by zero.
	 * @param other The value to divide both <code>x</code> and <code>y</code> by.
	 * @return <code>this</code>
	 */
	public Tuple divide(float other) {
		x /= other;
		y /= other;
		return this;
	}

	/** Divides Tuple <code>a</code> by <code>b</code>, generating a new Tuple and
	 * returning it.  Does not modify <code>a</code> or <code>b</code>.  Does not check for 
	 * division by zero.
	 * @param a A Tuple
	 * @param b A Tuple
	 * @return A new Tuple that is the quotient of <code>a</code> and <code>b</code>.
	 */
	public static Tuple divide(Tuple a, Tuple b) {
		return new Tuple(a.x / b.x, a.y / b.y);
	}

	/** Sets <code>this.x</code> to the minimum of <code>this.x</code> and 
	 * <code>other.x</code>.  Does the same for <code>other.y</code>.  Returns
	 * this.
	 * @param other The Tuple to compare to.
	 * @return <code>this</code>
	 */
	public Tuple min(Tuple other) {
		x = other.x < x ? other.x : x;
		y = other.y < y ? other.y : y;
		return this;
	}
	
	/** Compares both <code>x</code> and <code>y</code> components of <code>a</code>
	 * and <code>b</code>, and returns a new Tuple with the minimum values of the
	 * respective components.
	 * @param a A Tuple.
	 * @param b A Tuple.
	 * @return A new Tuple containing the minimum values of the source Tuples components.
	 */
	public static Tuple min(Tuple a, Tuple b) {
		return new Tuple(a.x < b.x ? a.x : b.x, a.y < b.y ? a.y : b.y);
	}

	/** Sets <code>this.x</code> to the maximum of <code>this.x</code> and 
	 * <code>other.x</code>.  Does the same for <code>other.y</code>.  Returns
	 * this.
	 * @param other The Tuple to compare to.
	 * @return <code>this</code>
	 */
	public Tuple max(Tuple other) {
		x = other.x > x ? other.x : x;
		y = other.y > y ? other.y : y;
		return this;
	}
	
	/** Compares both <code>x</code> and <code>y</code> components of <code>a</code>
	 * and <code>b</code>, and returns a new Tuple with the maximum values of the
	 * respective components.
	 * @param a A Tuple.
	 * @param b A Tuple.
	 * @return A new Tuple containing the maximum values of the source Tuples components.
	 */
	public static Tuple max(Tuple a, Tuple b) {
		return new Tuple(a.x > b.x ? a.x : b.x, a.y > b.y ? a.y : b.y);
	}
}

