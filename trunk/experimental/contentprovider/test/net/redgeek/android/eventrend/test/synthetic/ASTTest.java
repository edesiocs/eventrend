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

package net.redgeek.android.eventrend.test.synthetic;

import net.redgeek.android.eventrecorder.synthetic.AST;
import junit.framework.TestCase;

public class ASTTest extends TestCase {
  String parseAndPrint(String input) {
    AST t = new AST(input);
    return t.toString();
  }

  boolean parseAndReturnValid(String input) {
    AST t = new AST();
    return t.generate(input);
  }

  public void testSimpleVectorOps() {
    String input;
    input = "series \"one\" + series \"two\"";
    assertEquals(input, parseAndPrint(input));
    input = "(series \"one\" + series \"two\")";
    assertEquals(input, parseAndPrint(input));
    input = "(series \"one\" - series \"two\")";
    assertEquals(input, parseAndPrint(input));
    input = "(series \"one\" * series \"two\")";
    assertEquals(input, parseAndPrint(input));
    input = "(series \"one\" / series \"two\")";
    assertEquals(input, parseAndPrint(input));
  }

  public void testSimpleVectorPrevious() {
    String input;
    input = "(series \"one\" previous value)";
    assertEquals(input, parseAndPrint(input));
    input = "(series \"one\" previous timestamp)";
    assertEquals(input, parseAndPrint(input));
  }

  public void testVectorPreviousPeriod() {
    String input;
    input = "((series \"one\" previous timestamp) / hour)";
    assertEquals(input, parseAndPrint(input));
    input = "((series \"one\" previous timestamp) / am/pm)";
    assertEquals(input, parseAndPrint(input));
    input = "((series \"one\" previous timestamp) / day)";
    assertEquals(input, parseAndPrint(input));
    input = "((series \"one\" previous timestamp) / week)";
    assertEquals(input, parseAndPrint(input));
    input = "((series \"one\" previous timestamp) / month)";
    assertEquals(input, parseAndPrint(input));
    input = "((series \"one\" previous timestamp) / quarter)";
    assertEquals(input, parseAndPrint(input));
    input = "((series \"one\" previous timestamp) / year)";
    assertEquals(input, parseAndPrint(input));
  }

  public void testScalarPeriod() {
    String input;
    input = "(4 * hour)";
    assertEquals(input, parseAndPrint(input));
    input = "(4 * am/pm)";
    assertEquals(input, parseAndPrint(input));
    input = "(4 * day)";
    assertEquals(input, parseAndPrint(input));
    input = "(4 * week)";
    assertEquals(input, parseAndPrint(input));
    input = "(4 * month)";
    assertEquals(input, parseAndPrint(input));
    input = "(4 * quarter)";
    assertEquals(input, parseAndPrint(input));
    input = "(4 * year)";
    assertEquals(input, parseAndPrint(input));
  }

  public void testSimpleScalarOps() {
    String input;
    input = "(series \"one\" + 2.0)";
    assertEquals(input, parseAndPrint(input));
    input = "(series \"one\" - 2.0)";
    assertEquals(input, parseAndPrint(input));
    input = "(series \"one\" * 2.0)";
    assertEquals(input, parseAndPrint(input));
    input = "(series \"one\" / 2.0)";
    assertEquals(input, parseAndPrint(input));
  }

  public void testCompositeVectorOps() {
    String input;
    input = "((series \"one\" + series \"two\") + series \"three\")";
    assertEquals(input, parseAndPrint(input));
    input = "(series \"one\" + (series \"two\" + series \"three\"))";
    assertEquals(input, parseAndPrint(input));
    input = "((series \"one\" + series \"two\") + (series \"three\" + series \"four\"))";
    assertEquals(input, parseAndPrint(input));
  }

  public void testBadParse() {
    String input;
    input = "series \"one\" + series 2";
    assertEquals(false, parseAndReturnValid(input));
    input = "(series \"one\")";
    assertEquals(false, parseAndReturnValid(input));
    input = "(series \"one\" + series \"two\" + series \"three\")";
    assertEquals(false, parseAndReturnValid(input));
    input = "(series \"one\" - series \"two\" * series \"three\" + series \"four\")";
    assertEquals(false, parseAndReturnValid(input));
    input = "(series \"one\" + series \"two\" + series \"three\"))";
    assertEquals(false, parseAndReturnValid(input));
    input = "((series \"one\" + series \"two\") + series \"three\" + series \"four\"))";
    assertEquals(false, parseAndReturnValid(input));
  }

  public void testCompositePrevious() {
    String input;
    input = "(((series \"one\" + series \"two\") previous value) * 2)";
    assertEquals(false, parseAndReturnValid(input));
    input = "(series \"one\" - (series \"one\" previous timestamp))";
    assertEquals(input, parseAndPrint(input));
  }

  public void testOrdered() {
    String input;
    input = "(((series \"one\" + series \"two\") previous value) * 2)";
    assertEquals(false, parseAndReturnValid(input));
    input = "(series \"one\" - (series \"one\" previous timestamp))";
    assertEquals(input, parseAndPrint(input));
  }
}
