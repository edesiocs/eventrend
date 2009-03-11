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

package net.redgeek.android.eventrend.importing;

import java.util.ArrayList;

import net.redgeek.android.eventrend.db.CategoryDbTable;
import net.redgeek.android.eventrend.db.EntryDbTable;

/**
 * Probably the worst CSV parser ever created. Supports minimal CSV I/O, and
 * understands local DB row/column formats for importing into local DB records.
 * Note resilient to mal-formed CSV.
 * 
 * <strong>This should really be made into a proper parser, not this ad-hoc
 * beast.</strong>
 * 
 * @author barclay
 */
public class CSV {
  /**
   * Utility routine. Joins the array of strings with commas (','). Does no
   * quoting of strings. Result is only newline terminated if the last entry in
   * <code>s</code> is newline terminated.
   * 
   * @param s
   *          The array of strings to join.
   * @return The result of the concatenations.
   */
  public static String joinCSV(String[] s) {
    StringBuffer buffer = new StringBuffer();
    for (int i = 0; i < s.length; i++) {
      buffer.append(s[i]);
      if (i < s.length - 1)
        buffer.append(",");
    }
    return buffer.toString();
  }

  /**
   * Utility routine. Calls {@link #joinCSV(String[])} and append a newline
   * ('\n') character.
   * 
   * @param s
   * @return The result of the concatenation of {@link #joinCSV(String[])} and a
   *         newline.
   * @see #joinCSV(String[])
   */
  public static String joinCSVTerminated(String[] s) {
    return joinCSV(s) + "\n";
  }

  /**
   * Takes a CategoryDbTable.Row and joins it's fields into a comma-separated
   * string. Only fields that are listed in the Row class's EXPORTABLE array
   * will be exported, and special provisions are made to convert boolean fields
   * to either 1 or 0. All other fields are ignored.
   * 
   * @param row
   *          The Row to join the fields of.
   * @return The comma-separated string.
   * @see net.redgeek.android.eventrend.db.CategoryDbTable.Row
   */
  public static String joinCSV(CategoryDbTable.Row row) {
    StringBuffer buffer = new StringBuffer();
    for (int i = 0; i < CategoryDbTable.EXPORTABLE.length; i++) {
      if (CategoryDbTable.EXPORTABLE[i].equals(CategoryDbTable.KEY_ROWID))
        buffer.append(row.getId());
      else if (CategoryDbTable.EXPORTABLE[i]
          .equals(CategoryDbTable.KEY_GROUP_NAME))
        buffer.append(row.getGroupName());
      else if (CategoryDbTable.EXPORTABLE[i]
          .equals(CategoryDbTable.KEY_CATEGORY_NAME))
        buffer.append(row.getCategoryName());
      else if (CategoryDbTable.EXPORTABLE[i]
          .equals(CategoryDbTable.KEY_DEFAULT_VALUE))
        buffer.append(row.getDefaultValue());
      else if (CategoryDbTable.EXPORTABLE[i]
          .equals(CategoryDbTable.KEY_LAST_TREND))
        buffer.append(row.getLastTrend());
      else if (CategoryDbTable.EXPORTABLE[i]
          .equals(CategoryDbTable.KEY_INCREMENT))
        buffer.append(row.getIncrement());
      else if (CategoryDbTable.EXPORTABLE[i].equals(CategoryDbTable.KEY_GOAL))
        buffer.append(row.getGoal());
      else if (CategoryDbTable.EXPORTABLE[i].equals(CategoryDbTable.KEY_COLOR))
        buffer.append(row.getColor());
      else if (CategoryDbTable.EXPORTABLE[i].equals(CategoryDbTable.KEY_TYPE))
        buffer.append(row.getType());
      else if (CategoryDbTable.EXPORTABLE[i]
          .equals(CategoryDbTable.KEY_PERIOD_MS))
        buffer.append(row.getPeriodMs());
      else if (CategoryDbTable.EXPORTABLE[i].equals(CategoryDbTable.KEY_RANK))
        buffer.append(row.getRank());
      else if (CategoryDbTable.EXPORTABLE[i]
          .equals(CategoryDbTable.KEY_TREND_STATE))
        buffer.append(row.getTrendState());
      else if (CategoryDbTable.EXPORTABLE[i]
          .equals(CategoryDbTable.KEY_INTERPOLATION))
        buffer.append(row.getInterpolation());
      else if (CategoryDbTable.EXPORTABLE[i]
          .equals(CategoryDbTable.KEY_ZEROFILL)) {
        if (row.getZeroFill() == false)
          buffer.append("0");
        else
          buffer.append("1");
      } else if (CategoryDbTable.EXPORTABLE[i]
          .equals(CategoryDbTable.KEY_SYNTHETIC)) {
        if (row.getSynthetic() == false)
          buffer.append("0");
        else
          buffer.append("1");
      } else if (CategoryDbTable.EXPORTABLE[i]
          .equals(CategoryDbTable.KEY_FORMULA))
        buffer.append(row.getFormula());

      if (i < CategoryDbTable.EXPORTABLE.length - 1)
        buffer.append(",");
    }
    return buffer.toString();
  }

  /**
   * Takes a n EntryDbTable.Row and joins it's fields into a comma-separated
   * string. Only fields that are listed in the Row class's EXPORTABLE array
   * will be exported. All other fields are ignored.
   * 
   * @param row
   *          The Row to join the fields of.
   * @return The comma-separated string.
   * @see net.redgeek.android.eventrend.db.EntryDbTable.Row
   */
  public static String joinCSV(EntryDbTable.Row row) {
    StringBuffer buffer = new StringBuffer();
    for (int i = 0; i < EntryDbTable.EXPORTABLE.length; i++) {
      if (EntryDbTable.EXPORTABLE[i].equals(EntryDbTable.KEY_ROWID))
        buffer.append(row.getId());
      else if (EntryDbTable.EXPORTABLE[i].equals(EntryDbTable.KEY_CATEGORY_ID))
        buffer.append(row.getCategoryId());
      else if (EntryDbTable.EXPORTABLE[i].equals(EntryDbTable.KEY_TIMESTAMP))
        buffer.append(row.getTimestamp());
      else if (EntryDbTable.EXPORTABLE[i].equals(EntryDbTable.KEY_VALUE))
        buffer.append(row.getValue());
      else if (EntryDbTable.EXPORTABLE[i].equals(EntryDbTable.KEY_N_ENTRIES))
        buffer.append(row.getNEntries());

      if (i < EntryDbTable.EXPORTABLE.length - 1)
        buffer.append(",");
    }
    return buffer.toString();
  }

  /**
   * Parses a String assumed to be the complete header of a CSV file, that is, a
   * listing of the field names, and returns the values as an ArrayList. Field
   * names may not contain commas, else they will be treated as separate fields.
   * Quoted field names (in order to support field names with commas or other
   * exceptional characters, like newlines) are not supported.
   * 
   * @param line
   *          A String containing the CSV file header.
   * @return An ArrayList of Strings, ordered in the same order as CSV field
   *         headers occurred in <code>line</code>.
   */
  public static ArrayList<String> parseHeader(String line) {
    ArrayList<String> columns = new ArrayList<String>();

    int i = 0;
    int startIndex = 0;
    int nextComma = 0;
    for (startIndex = 0, i = 0; startIndex < line.length(); startIndex++, i++) {
      nextComma = line.indexOf(',', startIndex);
      if (nextComma == -1)
        nextComma = line.length();
      String column = line.substring(startIndex, nextComma);
      columns.add(i, column);
      startIndex = nextComma;
    }

    return columns;
  }

  /**
   * Given a String representing a (non-header) line of CSV, and a character
   * index to begin processing at, returns the next data field of the line as a
   * String. Supports double-quote ('"') delimited fields. If there is no such
   * entry, either due to repeated commas (e.g., ",," indicating an empty field)
   * or end-of-line, an empty String is returned (""). This is not resilient to
   * malformed CSV.
   * 
   * @param line
   *          The line of text to process.
   * @param startIndex
   *          The index to begin process at. This should be the first valid
   *          character of the next field, not the (possible) comma before the
   *          field.
   * 
   */
  public static String getNextLineEntry(String line, int startIndex) {
    boolean inQuotes = false;
    int i, entryStart, entryEnd;
    char c;

    entryStart = startIndex;
    entryEnd = startIndex;
    if (startIndex >= line.length()) {
      entryStart = line.length();
      entryEnd = entryStart;
    }

    for (i = 0; startIndex < line.length(); startIndex++, i++) {
      c = line.charAt(startIndex);
      if (inQuotes == false && c == ',') {
        entryEnd = startIndex;
        break;
      }
      if (i == 0 && c == '"') {
        inQuotes = true;
      }
      if (i > 0 && inQuotes == true && c == '"') {
        inQuotes = false;
      }
      entryEnd = startIndex;
    }

    if (startIndex == line.length())
      entryEnd++;

    return line.substring(entryStart, entryEnd);
  }

  /**
   * Parses a data line of a CSV file and returns the results as an ArrayList
   * ordered in the original order of the line.
   * 
   * @param line
   *          The line to process.
   * @return An ArrayList of Strings representing the field values.
   */
  public static ArrayList<String> getNextLine(String line) {
    ArrayList<String> entries = new ArrayList<String>();

    String item;
    int i = 0;
    int startIndex = 0;

    for (startIndex = 0, i = 0; startIndex < line.length(); i++) {
      item = CSV.getNextLineEntry(line, startIndex);
      startIndex += item.length() + 1;

      // Remove quotes around quoted strings
      if (item.length() > 1 && item.charAt(0) == '"'
          && item.charAt(item.length() - 1) == '"') {
        int newStart = 1;
        int newEnd = item.length() - 1;
        if (newEnd < newStart)
          newEnd = newStart;
        item = item.substring(newStart, newEnd);
      }

      entries.add(i, item);
    }

    return entries;
  }
}