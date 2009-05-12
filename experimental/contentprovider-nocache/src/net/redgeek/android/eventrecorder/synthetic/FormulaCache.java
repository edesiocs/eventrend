///*
// * Copyright (C) 2007 The Android Open Source Project
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package net.redgeek.android.eventrecorder.synthetic;
//
//import java.util.HashMap;
//
//public class FormulaCache {
//  private HashMap<Long, Formula> mCache;
//
//  public FormulaCache() {
//    mCache = new HashMap<Long, Formula>();
//  }
//
//  public void setFormula(long catId, Formula f) {
//    mCache.put(new Long(catId), f);
//  }
//
//  public Formula getFormula(long catId) {
//    return mCache.get(Long.valueOf(catId));
//  }
//
//  public boolean updateFormula(long catId, String input) {
//    Formula f = mCache.get(Long.valueOf(catId));
//    if (f == null)
//      return false;
//
//    f.setFormula(input);
//    return true;
//  }
//}
