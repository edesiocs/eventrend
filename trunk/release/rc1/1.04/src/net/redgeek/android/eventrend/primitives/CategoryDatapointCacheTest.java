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

import java.util.ArrayList;

import junit.framework.TestCase;

// Note that several tests use equality comparison on float, which could be 
// dangerous in general, but should be safe for such small predefined values.
public class CategoryDatapointCacheTest extends TestCase {
	public void testConstructors() {
		CategoryDatapointCache cache = new CategoryDatapointCache(1, 10);
		assertEquals(1, cache.getCategoryId());
		assertFalse(cache.isValid());
		assertEquals(Long.MAX_VALUE, cache.getStart());
		assertEquals(Long.MIN_VALUE, cache.getEnd());
		assertEquals(10, cache.getHistory());
	}

	public void testAdd() {
		Datapoint d = new Datapoint(100L, 2.0f, 3, 4, 5);

		CategoryDatapointCache cache = new CategoryDatapointCache(3, 10);
		cache.addDatapoint(d);

		assertTrue(cache.isValid());
		assertEquals(100L, cache.getStart());
		assertEquals(100L, cache.getEnd());

		d = new Datapoint(200L, 2.0f, 3, 4, 5);
		cache.addDatapoint(d);

		assertTrue(cache.isValid());
		assertEquals(100L, cache.getStart());
		assertEquals(200L, cache.getEnd());

		d = new Datapoint(150, 2.0f, 3, 4, 5);
		cache.addDatapoint(d);

		assertTrue(cache.isValid());
		assertEquals(100L, cache.getStart());
		assertEquals(200L, cache.getEnd());
	}
	
	public void testRetreiveInRange() {
		Datapoint d = new Datapoint(100L, 2.0f, 3, 4, 5);
		ArrayList<Datapoint> out;

		CategoryDatapointCache cache = new CategoryDatapointCache(3, 10);

		out = cache.getDataInRange(99, 101);
		assertNotNull(out);
		assertEquals(0, out.size());
		
		cache.addDatapoint(d);

		out = cache.getDataInRange(99, 101);
		assertEquals(1, out.size());
		assertSame(d, out.get(0));

		out = cache.getDataInRange(100, 150);
		assertEquals(1, out.size());
		assertSame(d, out.get(0));

		out = cache.getDataInRange(50, 100);
		assertEquals(1, out.size());
		assertSame(d, out.get(0));

		out = cache.getDataInRange(100, 100);
		assertEquals(1, out.size());
		assertSame(d, out.get(0));

		Datapoint d2 = new Datapoint(50L, 2.0f, 3, 4, 5);
		cache.addDatapoint(d2);
		d2 = new Datapoint(150L, 2.0f, 3, 4, 5);
		cache.addDatapoint(d2);

		out = cache.getDataInRange(51, 149);
		assertEquals(1, out.size());
		assertSame(d, out.get(0));		

		out = cache.getDataInRange(50, 150);
		assertEquals(3, out.size());
		assertSame(d, out.get(1));		
		assertNotSame(d, out.get(0));		
		assertNotSame(d, out.get(2));		
		assertEquals(50L, out.get(0).mMillis);
		assertEquals(100L, out.get(1).mMillis);
		assertEquals(150L, out.get(2).mMillis);
	}

	public void testRetreiveBeforeAfter() {
		Datapoint d = new Datapoint(100L, 2.0f, 3, 4, 5);
		ArrayList<Datapoint> out;

		CategoryDatapointCache cache = new CategoryDatapointCache(3, 10);

		out = cache.getDataBefore(1, 101);
		assertNotNull(out);
		assertEquals(0, out.size());
		
		cache.addDatapoint(d);

		out = cache.getDataBefore(1, 100);
		assertEquals(0, out.size());
		out = cache.getDataBefore(1, 101);
		assertEquals(1, out.size());
		assertSame(d, out.get(0));

		out = cache.getDataAfter(1, 100);
		assertEquals(0, out.size());
		out = cache.getDataAfter(1, 99);
		assertEquals(1, out.size());
		assertSame(d, out.get(0));
		
		Datapoint d2 = new Datapoint(50L, 2.0f, 3, 4, 5);
		cache.addDatapoint(d2);
		d2 = new Datapoint(150L, 2.0f, 3, 4, 5);
		cache.addDatapoint(d2);

		out = cache.getDataBefore(1, 150);
		assertEquals(1, out.size());
		out = cache.getDataBefore(2, 150);
		assertEquals(2, out.size());

		out = cache.getDataAfter(1, 50);
		assertEquals(1, out.size());
		out = cache.getDataAfter(2, 50);
		assertEquals(2, out.size());

		out = cache.getDataBefore(10, 151);
		assertEquals(3, out.size());
		assertSame(d, out.get(1));		
		assertNotSame(d, out.get(0));		
		assertNotSame(d, out.get(2));		
		assertEquals(50L, out.get(0).mMillis);
		assertEquals(100L, out.get(1).mMillis);
		assertEquals(150L, out.get(2).mMillis);

		out = cache.getDataAfter(10, 49);
		assertEquals(3, out.size());
		assertSame(d, out.get(1));		
		assertNotSame(d, out.get(0));		
		assertNotSame(d, out.get(2));		
		assertEquals(50L, out.get(0).mMillis);
		assertEquals(100L, out.get(1).mMillis);
		assertEquals(150L, out.get(2).mMillis);
	}
	
	public void testRetreiveLast() {
		Datapoint d = new Datapoint(100L, 2.0f, 3, 4, 5);
		ArrayList<Datapoint> out;

		CategoryDatapointCache cache = new CategoryDatapointCache(3, 10);

		out = cache.getLast(1);
		assertNotNull(out);
		assertEquals(0, out.size());
		
		cache.addDatapoint(d);

		out = cache.getLast(1);
		assertEquals(1, out.size());
		assertSame(d, out.get(0));
		
		Datapoint d2 = new Datapoint(50L, 2.0f, 3, 4, 5);
		cache.addDatapoint(d2);
		d2 = new Datapoint(150L, 2.0f, 3, 4, 5);
		cache.addDatapoint(d2);

		out = cache.getLast(1);
		assertEquals(1, out.size());
		assertNotSame(d, out.get(0));

		out = cache.getLast(2);
		assertEquals(2, out.size());
		assertSame(d, out.get(0));
		assertNotSame(d, out.get(1));

		out = cache.getLast(3);
		assertEquals(3, out.size());
		assertNotSame(d, out.get(0));
		assertSame(d, out.get(1));
		assertNotSame(d, out.get(2));

		out = cache.getLast(4);
		assertEquals(3, out.size());
		assertNotSame(d, out.get(0));
		assertSame(d, out.get(1));
		assertNotSame(d, out.get(2));
		assertEquals(50L, out.get(0).mMillis);
		assertEquals(100L, out.get(1).mMillis);
		assertEquals(150L, out.get(2).mMillis);
	}

	public void testUpdate() {
		Datapoint in = new Datapoint(100L, 2.0f, 3, 4, 5);
		Datapoint update = new Datapoint(101L, 2.0f, 3, 4, 5);
		Datapoint out;
		
		CategoryDatapointCache cache = new CategoryDatapointCache(3, 10);
		cache.addDatapoint(in);

		out = cache.updateDatapoint(update);
		assertNull(out);

		update.mMillis = 100L;
		update.mEntryId = 200;
		out = cache.updateDatapoint(update);
		assertNotNull(out);
		assertEquals(4, out.mEntryId);
	}

	public void testClear() {
		Datapoint d1 = new Datapoint(100L, 2.0f, 3, 4, 5);
		Datapoint d2 = new Datapoint(101L, 2.0f, 3, 4, 5);
		
		CategoryDatapointCache cache = new CategoryDatapointCache(3, 10);
		cache.addDatapoint(d1);
		cache.addDatapoint(d2);

		assertTrue(cache.isValid());
		assertEquals(100L, cache.getStart());
		assertEquals(101L, cache.getEnd());

		cache.clear();
		
		assertFalse(cache.isValid());
		assertEquals(Long.MAX_VALUE, cache.getStart());
		assertEquals(Long.MIN_VALUE, cache.getEnd());
	}
}

