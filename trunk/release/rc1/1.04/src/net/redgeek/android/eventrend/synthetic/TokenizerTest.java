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

package net.redgeek.android.eventrend.synthetic;

import junit.framework.TestCase;

// This needs more negative case testing and complex tests
public class TokenizerTest extends TestCase {
	public void testLiteralTokens() {
		Tokenizer tz = new Tokenizer();
		Tokenizer.Token token;
		
		tz.setInput(" " + Tokenizer.GROUP_START + " ");
		token = tz.getNextToken();
		assertEquals(Tokenizer.TokenID.GROUP_START, token.mTokenID);
		assertEquals(Tokenizer.GROUP_START, token.mValue);
		assertEquals(1, token.mStart);

		tz.setInput(" " + Tokenizer.GROUP_END + " ");
		token = tz.getNextToken();
		assertEquals(Tokenizer.TokenID.GROUP_END, token.mTokenID);
		assertEquals(Tokenizer.GROUP_END, token.mValue);
		assertEquals(1, token.mStart);

		tz.setInput(" " + Tokenizer.PLUS + " ");
		token = tz.getNextToken();
		assertEquals(Tokenizer.TokenID.PLUS, token.mTokenID);
		assertEquals(Tokenizer.PLUS, token.mValue);
		assertEquals(1, token.mStart);

		tz.setInput(" " + Tokenizer.MINUS + " ");
		token = tz.getNextToken();
		assertEquals(Tokenizer.TokenID.MINUS, token.mTokenID);
		assertEquals(Tokenizer.MINUS, token.mValue);
		assertEquals(1, token.mStart);

		tz.setInput(" " + Tokenizer.MULTIPLY + " ");
		token = tz.getNextToken();
		assertEquals(Tokenizer.TokenID.MULTIPLY, token.mTokenID);
		assertEquals(Tokenizer.MULTIPLY, token.mValue);
		assertEquals(1, token.mStart);

		tz.setInput(" " + Tokenizer.DIVIDE + " ");
		token = tz.getNextToken();
		assertEquals(Tokenizer.TokenID.DIVIDE, token.mTokenID);
		assertEquals(Tokenizer.DIVIDE, token.mValue);
		assertEquals(1, token.mStart);

		tz.setInput(" " + Tokenizer.DELTA + " ");
		token = tz.getNextToken();
		assertEquals(Tokenizer.TokenID.DELTA, token.mTokenID);
		assertEquals(Tokenizer.DELTA, token.mValue);
		assertEquals(1, token.mStart);

		tz.setInput(" " + Tokenizer.TIMESTAMP + " ");
		token = tz.getNextToken();
		assertEquals(Tokenizer.TokenID.DELTA_TIMESTAMP, token.mTokenID);
		assertEquals(Tokenizer.TIMESTAMP, token.mValue);
		assertEquals(1, token.mStart);

		tz.setInput(" " + Tokenizer.VALUE + " ");
		token = tz.getNextToken();
		assertEquals(Tokenizer.TokenID.DELTA_VALUE, token.mTokenID);
		assertEquals(Tokenizer.VALUE, token.mValue);
		assertEquals(1, token.mStart);

		tz.setInput(" " + Tokenizer.SERIES + " ");
		token = tz.getNextToken();
		assertEquals(Tokenizer.TokenID.SERIES, token.mTokenID);
		assertEquals(Tokenizer.SERIES, token.mValue);
		assertEquals(1, token.mStart);
	}
	
	public void testSeriesNames() {
		Tokenizer tz = new Tokenizer();
		Tokenizer.Token token;
		
		tz.setInput(" \"series_name\" ");
		token = tz.getNextToken();
		assertEquals(Tokenizer.TokenID.STRING_VALUE, token.mTokenID);
		assertEquals("series_name", token.mValue);
		assertEquals(2, token.mStart);

		tz.setInput("\"series_name\"");
		token = tz.getNextToken();
		assertEquals(Tokenizer.TokenID.STRING_VALUE, token.mTokenID);
		assertEquals("series_name", token.mValue);
		assertEquals(1, token.mStart);

		tz.setInput(" \"series name\" ");
		token = tz.getNextToken();
		assertEquals(Tokenizer.TokenID.STRING_VALUE, token.mTokenID);
		assertEquals("series name", token.mValue);
		assertEquals(2, token.mStart);

		tz.setInput(" \"series\\\"name\" ");
		token = tz.getNextToken();
		assertEquals(Tokenizer.TokenID.STRING_VALUE, token.mTokenID);
		assertEquals("series\"name", token.mValue);
		assertEquals(2, token.mStart);		
	}

	public void testLongs() {
		Tokenizer tz = new Tokenizer();
		Tokenizer.Token token;
		
		tz.setInput(" 1 ");
		token = tz.getNextToken();
		assertEquals(Tokenizer.TokenID.LONG_VALUE, token.mTokenID);
		assertEquals("1", token.mValue);
		assertEquals(1, token.mStart);

		tz.setInput("1");
		token = tz.getNextToken();
		assertEquals(Tokenizer.TokenID.LONG_VALUE, token.mTokenID);
		assertEquals("1", token.mValue);
		assertEquals(0, token.mStart);

		tz.setInput(" -1 ");
		token = tz.getNextToken();
		assertEquals(Tokenizer.TokenID.LONG_VALUE, token.mTokenID);
		assertEquals("-1", token.mValue);
		assertEquals(1, token.mStart);

		tz.setInput(" 123456789 ");
		token = tz.getNextToken();
		assertEquals(Tokenizer.TokenID.LONG_VALUE, token.mTokenID);
		assertEquals("123456789", token.mValue);
		assertEquals(1, token.mStart);

		tz.setInput("1)");
		token = tz.getNextToken();
		assertEquals(Tokenizer.TokenID.LONG_VALUE, token.mTokenID);
		assertEquals("1", token.mValue);
		assertEquals(0, token.mStart);
	}
	
	public void testFloats() {
		Tokenizer tz = new Tokenizer();
		Tokenizer.Token token;
		
		tz.setInput(" 1.0 ");
		token = tz.getNextToken();
		assertEquals(Tokenizer.TokenID.FLOAT_VALUE, token.mTokenID);
		assertEquals("1.0", token.mValue);
		assertEquals(1, token.mStart);

		tz.setInput("1.0");
		token = tz.getNextToken();
		assertEquals(Tokenizer.TokenID.FLOAT_VALUE, token.mTokenID);
		assertEquals("1.0", token.mValue);
		assertEquals(0, token.mStart);

		tz.setInput(" 1. ");
		token = tz.getNextToken();
		assertEquals(Tokenizer.TokenID.FLOAT_VALUE, token.mTokenID);
		assertEquals("1.", token.mValue);
		assertEquals(1, token.mStart);

		tz.setInput(" -1.5 ");
		token = tz.getNextToken();
		assertEquals(Tokenizer.TokenID.FLOAT_VALUE, token.mTokenID);
		assertEquals("-1.5", token.mValue);
		assertEquals(1, token.mStart);

		tz.setInput(" -.5 ");
		token = tz.getNextToken();
		assertEquals(Tokenizer.TokenID.FLOAT_VALUE, token.mTokenID);
		assertEquals("-.5", token.mValue);
		assertEquals(1, token.mStart);

		tz.setInput(" 1e-4 ");
		token = tz.getNextToken();
		assertEquals(Tokenizer.TokenID.FLOAT_VALUE, token.mTokenID);
		assertEquals("1e-4", token.mValue);
		assertEquals(1, token.mStart);

		tz.setInput(" 1E4 ");
		token = tz.getNextToken();
		assertEquals(Tokenizer.TokenID.FLOAT_VALUE, token.mTokenID);
		assertEquals("1E4", token.mValue);
		assertEquals(1, token.mStart);

		tz.setInput("1.0)");
		token = tz.getNextToken();
		assertEquals(Tokenizer.TokenID.FLOAT_VALUE, token.mTokenID);
		assertEquals("1.0", token.mValue);
		assertEquals(0, token.mStart);

		tz.setInput("1.0\"");
		token = tz.getNextToken();
		assertEquals(Tokenizer.TokenID.FLOAT_VALUE, token.mTokenID);
		assertEquals("1.0", token.mValue);
		assertEquals(0, token.mStart);
	}
	
	public void testPeriodConstants() {
		Tokenizer tz = new Tokenizer();
		Tokenizer.Token token;
		
		tz.setInput(" hour ");
		token = tz.getNextToken();
		assertEquals(Tokenizer.TokenID.PERIOD_CONSTANT, token.mTokenID);
		assertEquals("hour", token.mValue);
		assertEquals(1, token.mStart);

		tz.setInput(" am/pm ");
		token = tz.getNextToken();
		assertEquals(Tokenizer.TokenID.PERIOD_CONSTANT, token.mTokenID);
		assertEquals("am/pm", token.mValue);
		assertEquals(1, token.mStart);

		tz.setInput(" day ");
		token = tz.getNextToken();
		assertEquals(Tokenizer.TokenID.PERIOD_CONSTANT, token.mTokenID);
		assertEquals("day", token.mValue);
		assertEquals(1, token.mStart);

		tz.setInput(" week ");
		token = tz.getNextToken();
		assertEquals(Tokenizer.TokenID.PERIOD_CONSTANT, token.mTokenID);
		assertEquals("week", token.mValue);
		assertEquals(1, token.mStart);

		tz.setInput(" month ");
		token = tz.getNextToken();
		assertEquals(Tokenizer.TokenID.PERIOD_CONSTANT, token.mTokenID);
		assertEquals("month", token.mValue);
		assertEquals(1, token.mStart);

		tz.setInput(" quarter ");
		token = tz.getNextToken();
		assertEquals(Tokenizer.TokenID.PERIOD_CONSTANT, token.mTokenID);
		assertEquals("quarter", token.mValue);
		assertEquals(1, token.mStart);

		tz.setInput(" year ");
		token = tz.getNextToken();
		assertEquals(Tokenizer.TokenID.PERIOD_CONSTANT, token.mTokenID);
		assertEquals("year", token.mValue);
		assertEquals(1, token.mStart);
	}

	public void testGarbage() {
		Tokenizer tz = new Tokenizer();
		Tokenizer.Token token;
		
		tz.setInput(" none ");
		token = tz.getNextToken();
		assertEquals(Tokenizer.TokenID.UNKNOWN, token.mTokenID);
		assertEquals("none", token.mValue);
		assertEquals(1, token.mStart);

		tz.setInput(" asdf asdf ");
		token = tz.getNextToken();
		assertEquals(Tokenizer.TokenID.UNKNOWN, token.mTokenID);
		assertEquals("asdf", token.mValue);
		assertEquals(1, token.mStart);
	}
	
	public void testComposite() {
		Tokenizer tz = new Tokenizer();
		Tokenizer.Token token;
		
		// radix:     12345678 9 12 3456789 123 4567 89 
		tz.setInput("( series \"one\" + series \"two\" ) ");

		token = tz.getNextToken();
		assertEquals(Tokenizer.TokenID.GROUP_START, token.mTokenID);
		assertEquals(Tokenizer.GROUP_START, token.mValue);
		assertEquals(0, token.mStart);

		token = tz.getNextToken();
		assertEquals(Tokenizer.TokenID.SERIES, token.mTokenID);
		assertEquals("series", token.mValue);
		assertEquals(2, token.mStart);

		token = tz.getNextToken();
		assertEquals(Tokenizer.TokenID.STRING_VALUE, token.mTokenID);
		assertEquals("one", token.mValue);
		assertEquals(10, token.mStart);

		token = tz.getNextToken();
		assertEquals(Tokenizer.TokenID.PLUS, token.mTokenID);
		assertEquals("+", token.mValue);
		assertEquals(15, token.mStart);

		token = tz.getNextToken();
		assertEquals(Tokenizer.TokenID.SERIES, token.mTokenID);
		assertEquals("series", token.mValue);
		assertEquals(17, token.mStart);

		token = tz.getNextToken();
		assertEquals(Tokenizer.TokenID.STRING_VALUE, token.mTokenID);
		assertEquals("two", token.mValue);
		assertEquals(25, token.mStart);

		token = tz.getNextToken();
		assertEquals(Tokenizer.TokenID.GROUP_END, token.mTokenID);
		assertEquals(Tokenizer.GROUP_END, token.mValue);
		assertEquals(30, token.mStart);

		token = tz.getNextToken();
		assertEquals(Tokenizer.TokenID.EOF, token.mTokenID);
		assertEquals("", token.mValue);
		assertEquals(31, token.mStart);
	}
}

