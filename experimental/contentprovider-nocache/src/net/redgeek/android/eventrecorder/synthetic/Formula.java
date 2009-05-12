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

package net.redgeek.android.eventrecorder.synthetic;

import java.util.ArrayList;

import net.redgeek.android.eventrecorder.synthetic.AST.BinaryOperation;
import net.redgeek.android.eventrecorder.synthetic.AST.GroupOperand;
import net.redgeek.android.eventrecorder.synthetic.AST.Operand;
import net.redgeek.android.eventrecorder.synthetic.AST.Operation;
import net.redgeek.android.eventrecorder.synthetic.AST.UnaryOperation;
import net.redgeek.android.eventrend.util.DateUtil;

/**
 * A representation of formulas for generating synthetic time series. Note that
 * the operations are pretty basic, so I'm not using whatever the java
 * equivalent to [f]lex/{bison|yacc} is.
 * 
 * @author barclay
 * 
 */
public class Formula {
  private AST mAST;

  public Formula() {
    mAST = new AST();
  }

  public Formula(String input) {
    mAST = new AST(input);
  }

  public void setFormula(String input) {
    mAST.generate(input);
  }

  public boolean isValid() {
    return mAST.isValid();
  }

  public String getErrorString() {
    Tokenizer.Token err = mAST.getErrorToken();
    return "Unexpecting value " + err.mValue + " at character " + err.mStart;
  }

  public String toString() {
    return mAST.toString();
  }

  public ArrayList<String> getDependentNames() {
    return mAST.getDependentNames();
  }
  
  public SeriesData apply(ArrayList<SeriesData> sources) {
    OperandInstance result;
    Operation root = mAST.getRoot();
    if (root == null || mAST.isValid() == false)
      return null;

    result = applyOp(sources, mAST.getRoot());
    if (result.mType == Tokenizer.TokenID.SERIES) {
      return result.mSeries;
    }
    return null;
  }

  public static class OperandInstance {
    public Tokenizer.TokenID mType;
    public Double mDouble;
    public Long mLong;
    public SeriesData mSeries;
    public DateUtil.Period mPeriod;
    public boolean mTimestamp;

    public OperandInstance() {
    }
  }

  private OperandInstance makeInstance(ArrayList<SeriesData> sources,
      AST.Operand operand) {
    OperandInstance instance = new OperandInstance();

    if (operand.getClass().equals(AST.FloatOperand.class)) {
      instance.mType = Tokenizer.TokenID.FLOAT_VALUE;
      instance.mDouble = (Double) operand.getValue();
    } else if (operand.getClass().equals(AST.LongOperand.class)) {
      instance.mType = Tokenizer.TokenID.LONG_VALUE;
      instance.mDouble = (Double) operand.getValue();
    } else if (operand.getClass().equals(AST.UnitsOperand.class)) {
      instance.mType = Tokenizer.TokenID.PERIOD_CONSTANT;
      instance.mPeriod = (DateUtil.Period) operand.getValue();
    } else if (operand.getClass().equals(AST.DeltaOperand.class)) {
      instance.mType = Tokenizer.TokenID.DELTA;
      if (((String) operand.getValue()).equals(Tokenizer.TIMESTAMP)) {
        instance.mTimestamp = true;
      } else {
        instance.mTimestamp = false;
      }
    } else if (operand.getClass().equals(AST.TimeSeriesOperand.class)) {
      SeriesData ts = null;
      instance.mType = Tokenizer.TokenID.SERIES;
      String tsName = (String) operand.getValue();
      for (int i = 0; i < sources.size(); i++) {
        ts = sources.get(i);
        if (tsName.equals(ts.mName))
          break;
      }
      instance.mSeries = ts;
    }

    return instance;
  }

  private OperandInstance executeTimeseriesOp(OperandInstance left,
      AST.Opcode opcode, OperandInstance right) {
    if (left.mType == Tokenizer.TokenID.SERIES
        && right.mType == Tokenizer.TokenID.SERIES) {
      left.mSeries = left.mSeries.seriesOp(right.mSeries, opcode);
      return left;
    }

    if (left.mType == Tokenizer.TokenID.SERIES) {
      if (right.mType == Tokenizer.TokenID.FLOAT_VALUE)
        left.mSeries.floatOp(right.mDouble, opcode, false);
      else if (right.mType == Tokenizer.TokenID.LONG_VALUE)
        left.mSeries.longOp(right.mLong, opcode, false);
      else if (right.mType == Tokenizer.TokenID.DELTA) {
        if (right.mTimestamp == true)
          left.mSeries.previousTimestamp();
        else
          left.mSeries.previousValue();
      } else if (right.mType == Tokenizer.TokenID.PERIOD_CONSTANT) {
        Long l = new Long(DateUtil.mapPeriodToLong(right.mPeriod));
        left.mSeries.longOp(l, opcode, false);
      }
      return left;
    }

    if (right.mType == Tokenizer.TokenID.SERIES) {
      if (left.mType == Tokenizer.TokenID.FLOAT_VALUE)
        right.mSeries.floatOp(right.mDouble, opcode, true);
      else if (left.mType == Tokenizer.TokenID.LONG_VALUE)
        right.mSeries.longOp(right.mLong, opcode, true);
      else if (left.mType == Tokenizer.TokenID.PERIOD_CONSTANT) {
        Long l = new Long(DateUtil.mapPeriodToLong(left.mPeriod));
        right.mSeries.longOp(l, opcode, true);
      }
      return right;
    }

    return null;
  }

  private OperandInstance executeFloatOp(OperandInstance left,
      AST.Opcode opcode, OperandInstance right) {
    if (right.mType == Tokenizer.TokenID.LONG_VALUE
        || right.mType == Tokenizer.TokenID.PERIOD_CONSTANT) {
      Long l;
      if (right.mType == Tokenizer.TokenID.PERIOD_CONSTANT)
        l = new Long(DateUtil.mapPeriodToLong(right.mPeriod));
      else
        l = new Long(right.mLong);

      if (opcode == AST.Opcode.PLUS)
        left.mDouble += l;
      else if (opcode == AST.Opcode.MINUS)
        left.mDouble -= l;
      else if (opcode == AST.Opcode.MULTIPLY)
        left.mDouble *= l;
      else if (opcode == AST.Opcode.DIVIDE) {
        if (l != 0)
          left.mDouble /= l;
      }
    }

    return left;
  }

  private OperandInstance executeLongOp(OperandInstance left,
      AST.Opcode opcode, OperandInstance right) {
    if (right.mType == Tokenizer.TokenID.LONG_VALUE
        || right.mType == Tokenizer.TokenID.PERIOD_CONSTANT) {
      Long l;
      if (right.mType == Tokenizer.TokenID.PERIOD_CONSTANT)
        l = new Long(DateUtil.mapPeriodToLong(right.mPeriod));
      else
        l = new Long(right.mLong);

      if (opcode == AST.Opcode.PLUS)
        left.mLong += l;
      else if (opcode == AST.Opcode.MINUS)
        left.mLong -= l;
      else if (opcode == AST.Opcode.MULTIPLY)
        left.mLong *= l;
      else if (opcode == AST.Opcode.DIVIDE) {
        if (l != 0)
          left.mLong /= l;
      }
    } else if (right.mType == Tokenizer.TokenID.FLOAT_VALUE) {
      if (opcode == AST.Opcode.PLUS)
        left.mDouble = left.mLong + right.mDouble;
      else if (opcode == AST.Opcode.MINUS)
        left.mDouble = left.mLong - right.mDouble;
      else if (opcode == AST.Opcode.MULTIPLY)
        left.mDouble = left.mLong * right.mDouble;
      else if (opcode == AST.Opcode.DIVIDE) {
        if (right.mDouble != 0)
          left.mDouble = left.mLong / right.mDouble;
      }
    }

    return left;
  }

  private OperandInstance executeOp(OperandInstance left, AST.Opcode opcode,
      OperandInstance right) {
    OperandInstance result = null;
    if (left.mType == Tokenizer.TokenID.SERIES
        || right.mType == Tokenizer.TokenID.SERIES) {
      result = executeTimeseriesOp(left, opcode, right);
    } else if (left.mType == Tokenizer.TokenID.FLOAT_VALUE) {
      result = executeFloatOp(left, opcode, right);
    } else if (left.mType == Tokenizer.TokenID.LONG_VALUE) {
      result = executeLongOp(left, opcode, right);
    }

    return result;
  }

  private OperandInstance applyOp(ArrayList<SeriesData> sources, Operation op) {
    GroupOperand group;
    AST.Opcode oc = op.getOpcode();
    OperandInstance l, r, result = null;

    if (oc == AST.Opcode.GROUPING) {
      group = ((UnaryOperation<GroupOperand>) op).getOperand();
      result = applyOp(sources, group.getValue());
    } else if (oc == AST.Opcode.PLUS || oc == AST.Opcode.MINUS
        || oc == AST.Opcode.MULTIPLY || oc == AST.Opcode.DIVIDE
        || oc == AST.Opcode.DELTA) {
      Operand left = (Operand) ((BinaryOperation) op).getOperandLeft();
      Operand right = (Operand) ((BinaryOperation) op).getOperandRight();

      if (left.getClass().equals(GroupOperand.class)) {
        l = applyOp(sources, (Operation) left.getValue());
      } else
        l = makeInstance(sources, left);

      if (right.getClass().equals(GroupOperand.class)) {
        r = applyOp(sources, (Operation) right.getValue());
      } else
        r = makeInstance(sources, right);

      result = executeOp(l, oc, r);
    }

    return result;
  }
}
