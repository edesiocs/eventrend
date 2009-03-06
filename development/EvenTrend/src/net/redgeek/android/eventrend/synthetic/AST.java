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

import java.util.ArrayList;

import net.redgeek.android.eventrend.synthetic.Tokenizer.Token;
import net.redgeek.android.eventrend.synthetic.Tokenizer.TokenID;
import net.redgeek.android.eventrend.util.DateUtil;

public class AST {
  public enum Opcode {
    GROUPING, PLUS, MINUS, MULTIPLY, DIVIDE, DELTA,
  }

  private boolean mValid;
  private Operation mRoot;
  private ArrayList<String> mDependents;
  private Tokenizer mTokenizer;
  private Tokenizer.Token mError;

  public AST() {
    setup();
  }

  public AST(String input) {
    setup();
    generate(input);
  }

  private void setup() {
    mValid = false;
    mRoot = null;
    mError = new Tokenizer.Token();
    mTokenizer = new Tokenizer();
    mDependents = new ArrayList<String>();
  }

  public Operation getRoot() {
    return mRoot;
  }

  public boolean isValid() {
    return mValid;
  }

  public Tokenizer.Token getErrorToken() {
    return mError;
  }

  public ArrayList<String> getDependentNames() {
    return mDependents;
  }

  public boolean generate(String input) {
    mTokenizer.setInput(input);
    mRoot = parseString();
    if (mRoot != null)
      mValid = true;
    return mValid;
  }

  public String toString() {
    if (mRoot == null || mValid == false)
      return "<parse error>";
    return collapseSpaces(mRoot.toString());
  }

  public static String opcodeToString(Opcode opcode) {
    switch (opcode) {
      case PLUS:
        return Tokenizer.PLUS;
      case MINUS:
        return Tokenizer.MINUS;
      case MULTIPLY:
        return Tokenizer.MULTIPLY;
      case DIVIDE:
        return Tokenizer.DIVIDE;
      case DELTA:
        return Tokenizer.DELTA;
      default:
        return "";
    }
  }

  public static Opcode tokenToOpcode(TokenID t) {
    if (t == TokenID.PLUS)
      return Opcode.PLUS;
    else if (t == TokenID.MINUS)
      return Opcode.MINUS;
    else if (t == TokenID.MULTIPLY)
      return Opcode.MULTIPLY;
    else if (t == TokenID.DIVIDE)
      return Opcode.DIVIDE;
    else
      // if (t == TokenID.DELTA)
      return Opcode.DELTA;
  }

  public static interface Operand<T> {
    void setValue(T value);

    T getValue();

    String toString();
  }

  public static class TimeSeriesOperand implements Operand<String> {
    private String mCategoryName = null;

    public TimeSeriesOperand() {
    }

    public TimeSeriesOperand(String seriesId) {
      mCategoryName = seriesId;
    }

    public void setValue(String seriesId) {
      mCategoryName = seriesId;
    }

    public String getValue() {
      return mCategoryName;
    }

    public String toString() {
      return Tokenizer.SERIES + " " + Tokenizer.SERIES_DELIM
          + Tokenizer.escape(mCategoryName) + Tokenizer.SERIES_DELIM;
    }
  }

  public static class FloatOperand implements Operand<Float> {
    private Float mValue = null;

    public FloatOperand() {
    }

    public FloatOperand(Float value) {
      mValue = value;
    }

    public void setValue(Float value) {
      mValue = value;
    }

    public Float getValue() {
      return mValue;
    }

    public String toString() {
      return mValue.toString();
    }
  }

  public static class LongOperand implements Operand<Long> {
    private Long mValue = null;

    public LongOperand() {
    }

    public LongOperand(Long value) {
      mValue = value;
    }

    public void setValue(Long value) {
      mValue = value;
    }

    public Long getValue() {
      return mValue;
    }

    public String toString() {
      return mValue.toString();
    }
  }

  public static class UnitsOperand implements Operand<DateUtil.Period> {
    private DateUtil.Period mPeriod = null;

    public UnitsOperand() {
    }

    public UnitsOperand(DateUtil.Period period) {
      mPeriod = period;
    }

    public void setValue(DateUtil.Period period) {
      mPeriod = period;
    }

    public DateUtil.Period getValue() {
      return mPeriod;
    }

    public String toString() {
      return DateUtil.mapPeriodToString(mPeriod);
    }
  }

  public static class DeltaOperand implements Operand<String> {
    private String mAxis = null;

    public DeltaOperand() {
    }

    public DeltaOperand(String axis) {
      mAxis = axis;
    }

    public void setValue(String axis) {
      mAxis = axis;
    }

    public String getValue() {
      return mAxis;
    }

    public String toString() {
      return mAxis;
    }
  }

  public static class GroupOperand implements Operand<Operation> {
    private Operation mOp = null;

    public GroupOperand() {
    }

    public GroupOperand(Operation op) {
      mOp = op;
    }

    public void setValue(Operation op) {
      mOp = op;
    }

    public Operation getValue() {
      return mOp;
    }

    public String toString() {
      return Tokenizer.GROUP_START + mOp.toString() + Tokenizer.GROUP_END;
    }
  }

  // ***** Operations ***** //
  public static interface Operation {
    Opcode getOpcode();

    void setOpcode(Opcode opcode);

    String toString();
  }

  public static class UnaryOperation<U> implements Operation {
    private Opcode mOpcode;
    private U mOperand = null;

    public UnaryOperation() {
    }

    public UnaryOperation(U operand) {
      mOperand = operand;
    }

    public Opcode getOpcode() {
      return mOpcode;
    }

    public U getOperand() {
      return mOperand;
    }

    public void setOpcode(Opcode opcode) {
      mOpcode = opcode;
    }

    public void setOperand(U operand) {
      mOperand = operand;
    }

    public String toString() {
      return opcodeToString(mOpcode) + " " + mOperand.toString();
    }
  }

  public static class BinaryOperation<L, R> implements Operation {
    private Opcode mOpcode;
    private L mOperandLeft = null;
    private R mOperandRight = null;

    public BinaryOperation() {
    }

    public BinaryOperation(L left, R right) {
      mOperandLeft = left;
      mOperandRight = right;
    }

    public Opcode getOpcode() {
      return mOpcode;
    }

    public L getOperandLeft() {
      return mOperandLeft;
    }

    public R getOperandRight() {
      return mOperandRight;
    }

    public void setOpcode(Opcode opcode) {
      mOpcode = opcode;
    }

    public void setOperandLeft(L operand) {
      mOperandLeft = operand;
    }

    public void setOperandRight(R operand) {
      mOperandRight = operand;
    }

    public String toString() {
      return mOperandLeft.toString() + " " + opcodeToString(mOpcode) + " "
          + mOperandRight.toString();
    }
  }

  // ***** Tree Building ***** //
  private Operation parseString() {
    Operation expr;
    Token t = new Tokenizer.Token();
    expr = getExpression(t);
    if (expr == null || t.mTokenID == Tokenizer.TokenID.UNKNOWN) {
      mError.set(t);
      return null;
    }

    return expr;
  }

  private Operation getExpression(Tokenizer.Token out) {
    Tokenizer.Token op;
    Operand left, right;

    left = getOperand(out);
    if (left == null)
      return null;

    op = getOperator(out.mTokenID);
    out.set(op);
    if (op.mTokenID == Tokenizer.TokenID.EOF) {
      UnaryOperation group = new UnaryOperation();
      group.setOpcode(Opcode.GROUPING);
      group.setOperand(left);
      return group;
    }

    if (op.mTokenID == Tokenizer.TokenID.UNKNOWN)
      return null;

    right = getOperand(out);
    if (right == null || out.mTokenID == Tokenizer.TokenID.EOF)
      return null;

    out.mTokenID = Tokenizer.TokenID.COMPOSITE;

    BinaryOperation binop = new BinaryOperation();
    binop.setOperandLeft(left);
    binop.setOpcode(tokenToOpcode(op.mTokenID));
    binop.setOperandRight(right);

    return binop;
  }

  private Operand getOperand(Tokenizer.Token out) {
    Tokenizer.Token t, id, end, paren;

    t = mTokenizer.getNextToken();
    out.set(t);
    if (out.mTokenID == Tokenizer.TokenID.EOF)
      return null;

    if (out.mTokenID == Tokenizer.TokenID.GROUP_START) {
      Operation subop = getExpression(out);
      if (subop == null)
        return null;

      paren = mTokenizer.getNextToken();
      out.set(paren);
      if (paren.mTokenID != Tokenizer.TokenID.GROUP_END)
        return null;

      GroupOperand group = new GroupOperand();
      group.setValue(subop);
      out.mTokenID = Tokenizer.TokenID.COMPOSITE;

      return group;
    } else if (out.mTokenID == Tokenizer.TokenID.LONG_VALUE) {
      return new LongOperand(Long.valueOf(out.mValue));
    } else if (out.mTokenID == Tokenizer.TokenID.FLOAT_VALUE) {
      return new FloatOperand(Float.valueOf(out.mValue));
    } else if (out.mTokenID == Tokenizer.TokenID.SERIES) {
      id = mTokenizer.getNextToken();
      out.set(id);

      if (id.mTokenID != Tokenizer.TokenID.STRING_VALUE)
        return null;

      out.mTokenID = Tokenizer.TokenID.SERIES;
      TimeSeriesOperand ts = new TimeSeriesOperand(id.mValue);
      mDependents.add(id.mValue);
      return ts;
    } else if (out.mTokenID == Tokenizer.TokenID.DELTA_TIMESTAMP
        || out.mTokenID == Tokenizer.TokenID.DELTA_VALUE) {
      DeltaOperand prev = new DeltaOperand(out.mValue);
      return prev;
    } else if (out.mTokenID == Tokenizer.TokenID.PERIOD_CONSTANT) {
      DateUtil.Period p = DateUtil.mapStringToPeriod(out.mValue);
      UnitsOperand units = new UnitsOperand(p);
      return units;
    }

    return null;
  }

  private Tokenizer.Token getOperator(TokenID bindingType) {
    Tokenizer.Token op = mTokenizer.getNextToken();

    if (op.mTokenID == Tokenizer.TokenID.EOF)
      return op;

    if (bindingType == Tokenizer.TokenID.SERIES) {
      // series can use the DELTA operator, but not scalars
      if (op.mTokenID != Tokenizer.TokenID.PLUS
          && op.mTokenID != Tokenizer.TokenID.MINUS
          && op.mTokenID != Tokenizer.TokenID.MULTIPLY
          && op.mTokenID != Tokenizer.TokenID.DIVIDE
          && op.mTokenID != Tokenizer.TokenID.DELTA) {
        op.mTokenID = Tokenizer.TokenID.UNKNOWN;
      }
    } else if (bindingType == Tokenizer.TokenID.DELTA) {
      // DELTA takes one of two arguments:
      if (op.mTokenID != Tokenizer.TokenID.DELTA_TIMESTAMP
          && op.mTokenID != Tokenizer.TokenID.DELTA_VALUE) {
        op.mTokenID = Tokenizer.TokenID.UNKNOWN;
      }
    } else if (op.mTokenID != Tokenizer.TokenID.PLUS
        && op.mTokenID != Tokenizer.TokenID.MINUS
        && op.mTokenID != Tokenizer.TokenID.MULTIPLY
        && op.mTokenID != Tokenizer.TokenID.DIVIDE) {
      op.mTokenID = Tokenizer.TokenID.UNKNOWN;
    }
    return op;
  }

  private String collapseSpaces(String in) {
    boolean addSpace;
    int j;
    char c;
    String out = "";
    for (int inIdx = 0; inIdx < in.length(); inIdx++) {
      j = inIdx;
      addSpace = false;
      c = in.charAt(inIdx);
      if (c == ' ') {
        for (j = inIdx; j < in.length(); j++) {
          c = in.charAt(j);
          if (c != ' ') {
            addSpace = true;
            break;
          }
        }
      }
      if (j >= in.length())
        break;
      if (inIdx > 0 && addSpace == true)
        out += ' ';
      inIdx = j;
      out += c;
    }

    return out;
  }
}
