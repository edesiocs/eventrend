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

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

// Very, very simple xml handler, intended only for testing.
public class DbTestReader extends DefaultHandler {
  private MockEvenTrendDbAdapter mDbh;
  private XMLReader mXmlReader;
  private HashMap<Integer, String> mColumnMap;

  private HashMap<String, String> mRow;
  private String  mTable;
  private String  mField;
  private String  mValue;
  private Integer mIndex;

  public DbTestReader(MockEvenTrendDbAdapter dbh) {
    super();
    mDbh = dbh;
    mColumnMap = new HashMap<Integer, String>();
    mIndex = new Integer(0);

    try {
      mXmlReader = XMLReaderFactory.createXMLReader();
    } catch (SAXException e) {
      e.printStackTrace();
    }
    mXmlReader.setContentHandler(this);
    mXmlReader.setErrorHandler(this);
  }
  
  public void populateFromFile(String filename) {
    FileReader fr;
    try {
      fr = new FileReader(filename);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return;
    }
    try {
      mXmlReader.parse(new InputSource(fr));
    } catch (IOException e) {
      e.printStackTrace();
    } catch (SAXException e) {
      e.printStackTrace();
    }
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
    } else if (qName.equals("column")) {
      mField = atts.getValue("name");
    }
  }

  @Override
  public void endElement(String uri, String name, String qName) {
    if (qName.equals("table")) {
      mDbh.setColumnMap(mTable, mColumnMap);
    } else if (qName.equals("row")) {
      mDbh.addContent(mTable, mRow);
    } else if (qName.equals("column")) {
      Integer i = null;
      Iterator<Map.Entry<Integer, String>> iterator = mColumnMap.entrySet().iterator();
      while(iterator.hasNext()) {
        Map.Entry<Integer, String> entry = iterator.next();
        String value = entry.getValue();
        if (value.equals(mField))
          i = entry.getKey();
      }
      if (i == null) {
        mIndex++;
        mColumnMap.put(new Integer(mIndex), mField);        
      }
      mRow.put(mField, mValue);
    }
  }

  @Override
  public void characters(char ch[], int start, int length) {
    mValue = new String(ch, start, length);
  }
}
