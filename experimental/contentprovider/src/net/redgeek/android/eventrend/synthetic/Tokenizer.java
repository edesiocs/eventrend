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

import net.redgeek.android.timeseries.CategoryDbTable;

public class Tokenizer {
  public enum Opcode {
    GROUPING, PLUS, MINUS, MULTIPLY, DIVIDE, DELTA,
  }

  public enum TokenID {
    GROUP_START, GROUP_END, PLUS, MINUS, MULTIPLY, DIVIDE, DELTA, DELTA_TIMESTAMP, DELTA_VALUE, SERIES, LONG_VALUE, FLOAT_VALUE, STRING_VALUE, PERIOD_CONSTANT, COMPOSITE, UNKNOWN, EOF
  }

  public static final String GROUP_START = "(";
  public static final String GROUP_END = ")";
  public static final String PLUS = "+";
  public static final String MINUS = "-";
  public static final String MULTIPLY = "*";
  public static final String DIVIDE = "/";
  public static final String DELTA = "previous";
  public static final String TIMESTAMP = "timestamp";
  public static final String VALUE = "value";
  public static final String SERIES = "series";
  public static final String SERIES_DELIM = "\"";
  public static final String ESCAPE = "\\";

  public static class Token {
    public TokenID mTokenID;
    public String mValue;
    public int mStart;
    public int mConsumed;

    public Token() {
    }

    public Token(TokenID t, String v, int start, int consumed) {
      mTokenID = t;
      mValue = v;
      mStart = start;
      mConsumed = consumed;
    }

    public void set(Token in) {
      mTokenID = in.mTokenID;
      mValue = in.mValue;
      mStart = in.mStart;
      mConsumed = in.mConsumed;
    }

    public String toString() {
      return mTokenID + ": " + mValue + " @ " + mStart;
    }
  }

  private String mInput;
  private int mLength;
  private int mConsumed;

  public Tokenizer() {
    mInput = "";
    mConsumed = 0;
    mLength = 0;
  }

  public void setInput(String input) {
    mInput = input;
    mLength = input.length();
    mConsumed = 0;
  }

  public Token getNextToken() {
    Token t = nextToken();
    mConsumed += t.mConsumed;
    return t;
  }

  private Token nextToken() {
    char c;
    int start, end, consumed;

    consumed = 0;
    start = mConsumed;
    while (start < mLength) {
      c = mInput.charAt(start);

      // skip over spaces and escaped characters
      if (c == ' ' || c == '\t' || c == '\n' || c == ESCAPE.charAt(0)) {
        if (start == mConsumed)
          start++;
        consumed++;
        continue;
      }

      // single-char tokens
      if (c == GROUP_START.charAt(0))
        return new Token(TokenID.GROUP_START, GROUP_START, start, consumed
            + GROUP_START.length());
      if (c == GROUP_END.charAt(0))
        return new Token(TokenID.GROUP_END, GROUP_END, start, consumed
            + GROUP_END.length());
      if (c == PLUS.charAt(0)) {
        // lookahead to check for the start of positive prefixed numbers
        if (start + 1 >= mLength)
          return new Token(TokenID.PLUS, PLUS, start, consumed + PLUS.length());
        else if (possibleFloatStart(mInput.charAt(start + 1)) == false)
          return new Token(TokenID.PLUS, PLUS, start, consumed + PLUS.length());
      }
      if (c == MINUS.charAt(0)) {
        // lookahead to check for the start of negative numbers
        if (start + 1 >= mLength)
          return new Token(TokenID.MINUS, MINUS, start, consumed
              + MINUS.length());
        else if (possibleFloatStart(mInput.charAt(start + 1)) == false)
          return new Token(TokenID.MINUS, MINUS, start, consumed
              + MINUS.length());
      }
      if (c == MULTIPLY.charAt(0))
        return new Token(TokenID.MULTIPLY, MULTIPLY, start, consumed
            + MULTIPLY.length());
      if (c == DIVIDE.charAt(0))
        return new Token(TokenID.DIVIDE, DIVIDE, start, consumed
            + DIVIDE.length());

      // static word tokens
      if (mInput.startsWith(SERIES, start))
        return new Token(TokenID.SERIES, SERIES, start, consumed
            + SERIES.length());
      if (mInput.startsWith(DELTA, start))
        return new Token(TokenID.DELTA, DELTA, start, consumed + DELTA.length());
      if (mInput.startsWith(TIMESTAMP, start))
        return new Token(TokenID.DELTA_TIMESTAMP, TIMESTAMP, start, consumed
            + TIMESTAMP.length());
      if (mInput.startsWith(VALUE, start))
        return new Token(TokenID.DELTA_VALUE, VALUE, start, consumed
            + VALUE.length());

      // series must be in quotes
      if (c == SERIES_DELIM.charAt(0)) {
        // make sure we won't terminate on an escaped quote
        end = start + 1;
        while (end < mLength) {
          end = mInput.indexOf(SERIES_DELIM.charAt(0), end);
          c = mInput.charAt(end - 1);
          if (c != ESCAPE.charAt(0))
            break;
          end++;
        }
        if (end >= mLength || start + 1 >= end) {
          String s = mInput.substring(start, mLength);
          consumed += s.length();
          return new Token(TokenID.UNKNOWN, unescape(s), start, consumed);
        } else {
          String s = mInput.substring(start + 1, end);
          consumed += s.length() + 2; // for the quotes
          return new Token(TokenID.STRING_VALUE, unescape(s), start + 1,
              consumed);
        }
      }

      // rest of the processing below
      break;
    }

    Token t = new Token(TokenID.UNKNOWN, "", start, consumed);
    if (start + 1 <= mLength) {
      String s;
      end = start + 1;
      while (end < mLength) {
        c = mInput.charAt(end);
        if (isDelimiter(c) == true)
          break;
        end++;
      }

      if (end < mLength)
        s = mInput.substring(start, end);
      else
        s = mInput.substring(start, mLength);
      t.mValue = s;
      t.mConsumed = consumed + t.mValue.length();
    }

    // try checking for numbers and period constants
    try {
      Long.valueOf(t.mValue);
      t.mTokenID = TokenID.LONG_VALUE;
      return t;
    } catch (NumberFormatException e) {
    }

    try {
      Float.valueOf(t.mValue);
      t.mTokenID = TokenID.FLOAT_VALUE;
      return t;
    } catch (NumberFormatException e) {
    }

    for (int i = 0; i < CategoryDbTable.KEY_PERIODS.length; i++) {
      if (CategoryDbTable.KEY_PERIODS[i].toLowerCase().equals("none"))
        continue;
      if (t.mValue.toLowerCase().equals(
          CategoryDbTable.KEY_PERIODS[i].toLowerCase())) {
        t.mTokenID = TokenID.PERIOD_CONSTANT;
        return t;
      }
    }

    if (start >= mLength)
      return new Token(TokenID.EOF, "", mConsumed, 0);

    return t;
  }

  private boolean isDelimiter(char c) {
    switch (c) {
      case ')':
      case '(':
      case ' ':
      case '\t':
      case '\n':
      case '"':
        return true;
    }
    return false;
  }

  private boolean possibleFloatStart(char c) {
    switch (c) {
      case '0':
      case '1':
      case '2':
      case '3':
      case '4':
      case '6':
      case '7':
      case '8':
      case '9':
      case '.':
      case '+':
      case '-':
        return true;
    }
    return false;
  }

  public static String unescape(String in) {
    char c;
    String out = "";
    for (int i = 0; i < in.length(); i++) {
      c = in.charAt(i);
      if (c != '\\')
        out += c;
    }
    return out;
  }

  public static String escape(String in) {
    char c;
    String out = "";
    for (int i = 0; i < in.length(); i++) {
      c = in.charAt(i);
      if (c == '\"')
        out += '\\';
      out += c;
    }
    return out;
  }

}
