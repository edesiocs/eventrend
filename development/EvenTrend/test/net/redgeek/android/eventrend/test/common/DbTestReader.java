/*
 * Copyright (C) 2007 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package net.redgeek.android.eventrend.test.common;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

// Very, very simple xml handler, intended only for testing.
public class DbTestReader extends DefaultHandler {
  private MockEvenTrendDbAdapter mDbh;
  private XMLReader mXmlReader;

  private HashMap<String, String> mRow;
  private String mTable;
  private String mField;
  private String mValue;

  public DbTestReader(MockEvenTrendDbAdapter dbh) throws SAXException {
    super();
    mDbh = dbh;

    XMLReader mXmlReader = XMLReaderFactory.createXMLReader();
    mXmlReader.setContentHandler(this);
    mXmlReader.setErrorHandler(this);
  }

  public void populateFromFile(String filename) throws IOException,
      SAXException {
    FileReader fr = new FileReader(filename);
    mXmlReader.parse(new InputSource(fr));
  }

  @Override
  public void startDocument() {
    mDbh.close();
    mDbh.open();
  }

  @Override
  public void endDocument() {
  }

  @Override
  public void startElement(String uri, String name, String qName,
      Attributes atts) {
    if (qName.equals("table")) {
      mTable = atts.getValue("name");
    } else if (qName.equals("row")) {
      mRow = new HashMap<String, String>();
    } else {
      mField = new String(qName);
    }
  }

  @Override
  public void endElement(String uri, String name, String qName) {
    if (qName.equals("table")) {
      // nothing
    } else if (qName.equals("row")) {
      mDbh.addContent(mTable, mRow);
    } else {
      mRow.put(mField, mValue);
    }
  }

  @Override
  public void characters(char ch[], int start, int length) {
    mValue = new String(ch);
  }
}
