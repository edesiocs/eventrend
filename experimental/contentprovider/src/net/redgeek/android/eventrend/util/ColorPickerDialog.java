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
//package net.redgeek.android.eventrend.util;
//
//import android.app.Dialog;
//import android.content.Context;
//import android.graphics.Canvas;
//import android.graphics.Color;
//import android.graphics.ColorMatrix;
//import android.graphics.LinearGradient;
//import android.graphics.Paint;
//import android.graphics.RectF;
//import android.graphics.Shader;
//import android.graphics.SweepGradient;
//import android.os.Bundle;
//import android.view.MotionEvent;
//import android.view.View;
//
///**
// * Slightly modified verions of the ColorPickerDialog class from the DemoApi
// * examples.
// * 
// * @author Google DemoApis
// * @author barclay (modifications)
// * 
// */
//public class ColorPickerDialog extends Dialog {
//
//  public interface OnColorChangedListener {
//    void colorChanged(int color);
//  }
//
//  private OnColorChangedListener mListener;
//  private int mInitialColor;
//
//  private static class ColorPickerView extends View {
//    private Paint mPaint;
//    private Paint mSaturationPaint;
//    private Paint mValuePaint;
//    private Paint mCenterPaint;
//    private int[] mColors;
//    private int[] mSaturations;
//    private int[] mValues;
//    private OnColorChangedListener mListener;
//
//    ColorPickerView(Context c, OnColorChangedListener l, int color) {
//      super(c);
//      mListener = l;
//      mColors = new int[] { 0xFFFF0000, 0xFFFF00FF, 0xFF0000FF, 0xFF00FFFF,
//          0xFF00FF00, 0xFFFFFF00, 0xFFFF0000 };
//      mSaturations = new int[] { 0xFFFFFFFF, 0xFF000000 };
//      mValues = new int[] { 0xFFFFFFFF, 0xFF000000 };
//
//      Shader h = new SweepGradient(0, 0, mColors, null);
//      mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
//      mPaint.setShader(h);
//      mPaint.setStyle(Paint.Style.STROKE);
//      mPaint.setStrokeWidth(32);
//
//      Shader s = new LinearGradient(0, -CENTER_Y + 5, 0, CENTER_Y - 5,
//          mSaturations, null, Shader.TileMode.CLAMP);
//      mSaturationPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
//      mSaturationPaint.setShader(s);
//      mSaturationPaint.setStyle(Paint.Style.STROKE);
//      mSaturationPaint.setStrokeWidth(SATURATION_STRIPE);
//
//      Shader v = new LinearGradient(-CENTER_X + 5, 0, CENTER_X - 5, 0, mValues,
//          null, Shader.TileMode.CLAMP);
//      mValuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
//      mValuePaint.setShader(v);
//      mValuePaint.setStyle(Paint.Style.STROKE);
//      mValuePaint.setStrokeWidth(SATURATION_STRIPE);
//
//      mCenterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
//      mCenterPaint.setColor(color);
//      mCenterPaint.setStrokeWidth(5);
//    }
//
//    private boolean mTrackingCenter;
//    private boolean mHighlightCenter;
//
//    @Override
//    protected void onDraw(Canvas canvas) {
//      float r = CENTER_X - mPaint.getStrokeWidth() * 0.5f;
//
//      canvas.translate(CENTER_X, CENTER_X);
//
//      Shader s = new SweepGradient(0, 0, mColors, null);
//      mPaint.setShader(s);
//      canvas.drawOval(new RectF(-r, -r, r, r), mPaint);
//      canvas.drawCircle(0, 0, CENTER_RADIUS, mCenterPaint);
//      canvas.drawRect(new RectF(r + mPaint.getStrokeWidth() + DIVIDER_PAD, -r,
//          r + mPaint.getStrokeWidth() + DIVIDER_PAD + SATURATION_STRIPE, r),
//          mSaturationPaint);
//      canvas.drawRect(new RectF(-r, r + mPaint.getStrokeWidth() + DIVIDER_PAD,
//          r, r + mPaint.getStrokeWidth() + DIVIDER_PAD + SATURATION_STRIPE),
//          mValuePaint);
//
//      if (mTrackingCenter) {
//        int c = mCenterPaint.getColor();
//        mCenterPaint.setStyle(Paint.Style.STROKE);
//
//        if (mHighlightCenter) {
//          mCenterPaint.setAlpha(0xFF);
//        } else {
//          mCenterPaint.setAlpha(0x80);
//        }
//        canvas.drawCircle(0, 0, CENTER_RADIUS + mCenterPaint.getStrokeWidth(),
//            mCenterPaint);
//
//        mCenterPaint.setStyle(Paint.Style.FILL);
//        mCenterPaint.setColor(c);
//      }
//    }
//
//    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//      setMeasuredDimension(CENTER_X * 2 + EXTRA_RIGHT, CENTER_Y * 2
//          + EXTRA_BOTTOM);
//    }
//
//    private static final int CENTER_X = 100;
//    private static final int CENTER_Y = 100;
//    private static final int DIVIDER_PAD = 10;
//    private static final int SATURATION_STRIPE = 30;
//    private static final int EXTRA_RIGHT = DIVIDER_PAD + SATURATION_STRIPE;
//    private static final int EXTRA_BOTTOM = DIVIDER_PAD + SATURATION_STRIPE;
//    private static final int CENTER_RADIUS = 32;
//
//    private int floatToByte(float x) {
//      int n = java.lang.Math.round(x);
//      return n;
//    }
//
//    private int pinToByte(int n) {
//      if (n < 0) {
//        n = 0;
//      } else if (n > 255) {
//        n = 255;
//      }
//      return n;
//    }
//
//    private int ave(int s, int d, float p) {
//      return s + java.lang.Math.round(p * (d - s));
//    }
//
//    private int interpColor(int colors[], float unit) {
//      if (unit <= 0) {
//        return colors[0];
//      }
//      if (unit >= 1) {
//        return colors[colors.length - 1];
//      }
//
//      float p = unit * (colors.length - 1);
//      int i = (int) p;
//      p -= i;
//
//      // now p is just the fractional part [0...1) and i is the index
//      int c0 = colors[i];
//      int c1 = colors[i + 1];
//      int a = ave(Color.alpha(c0), Color.alpha(c1), p);
//      int r = ave(Color.red(c0), Color.red(c1), p);
//      int g = ave(Color.green(c0), Color.green(c1), p);
//      int b = ave(Color.blue(c0), Color.blue(c1), p);
//
//      return Color.argb(a, r, g, b);
//    }
//
//    private int shadeColorSaturation(int colors[], Paint p, float unit) {
//      float[] hsv = new float[3];
//
//      for (int i = 0; i < mColors.length; i++) {
//        Color.colorToHSV(mColors[i], hsv);
//        hsv[1] = unit;
//        mColors[i] = Color.HSVToColor(hsv);
//      }
//
//      Color.colorToHSV(p.getColor(), hsv);
//      hsv[1] = unit;
//      return Color.HSVToColor(hsv);
//    }
//
//    private int shadeColorValue(int colors[], Paint p, float unit) {
//      float[] hsv = new float[3];
//
//      for (int i = 0; i < mColors.length; i++) {
//        Color.colorToHSV(mColors[i], hsv);
//        hsv[2] = unit;
//        mColors[i] = Color.HSVToColor(hsv);
//      }
//
//      Color.colorToHSV(p.getColor(), hsv);
//      hsv[2] = unit;
//      return Color.HSVToColor(hsv);
//    }
//
//    private int rotateColor(int color, float rad) {
//      float deg = rad * 180 / 3.1415927f;
//      int r = Color.red(color);
//      int g = Color.green(color);
//      int b = Color.blue(color);
//
//      ColorMatrix cm = new ColorMatrix();
//      ColorMatrix tmp = new ColorMatrix();
//
//      cm.setRGB2YUV();
//      tmp.setRotate(0, deg);
//      cm.postConcat(tmp);
//      tmp.setYUV2RGB();
//      cm.postConcat(tmp);
//
//      final float[] a = cm.getArray();
//
//      int ir = floatToByte(a[0] * r + a[1] * g + a[2] * b);
//      int ig = floatToByte(a[5] * r + a[6] * g + a[7] * b);
//      int ib = floatToByte(a[10] * r + a[11] * g + a[12] * b);
//
//      return Color.argb(Color.alpha(color), pinToByte(ir), pinToByte(ig),
//          pinToByte(ib));
//    }
//
//    private static final float PI = 3.1415926f;
//
//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//      float x = event.getX() - CENTER_X;
//      float y = event.getY() - CENTER_Y;
//      boolean inCenter = java.lang.Math.sqrt(x * x + y * y) <= CENTER_RADIUS;
//
//      switch (event.getAction()) {
//        case MotionEvent.ACTION_DOWN:
//          mTrackingCenter = inCenter;
//          if (inCenter) {
//            mHighlightCenter = true;
//            invalidate();
//            break;
//          }
//        case MotionEvent.ACTION_MOVE:
//          if (mTrackingCenter) {
//            if (mHighlightCenter != inCenter) {
//              mHighlightCenter = inCenter;
//              invalidate();
//            }
//          } else {
//            if (x < CENTER_X && y < CENTER_Y) {
//              float angle = (float) java.lang.Math.atan2(y, x);
//              // need to turn angle [-PI ... PI] into unit [0....1]
//              float unit = angle / (2 * PI);
//              if (unit < 0) {
//                unit += 1;
//              }
//              mCenterPaint.setColor(interpColor(mColors, unit));
//              invalidate();
//            } else if (x > CENTER_X) {
//              float unit = ((y + CENTER_Y) / (CENTER_Y * 2));
//              unit = Number.Clamp(unit, 0.0f, 1.0f);
//              mCenterPaint.setColor(shadeColorSaturation(mColors, mCenterPaint,
//                  unit));
//              invalidate();
//            } else if (y > CENTER_Y) {
//              float unit = 1 - ((x + CENTER_X) / (CENTER_X * 2));
//              unit = Number.Clamp(unit, 0.0f, 1.0f);
//              mCenterPaint
//                  .setColor(shadeColorValue(mColors, mCenterPaint, unit));
//              invalidate();
//            }
//          }
//          break;
//        case MotionEvent.ACTION_UP:
//          if (mTrackingCenter) {
//            if (inCenter) {
//              mListener.colorChanged(mCenterPaint.getColor());
//            }
//            mTrackingCenter = false; // so we draw w/o halo
//            invalidate();
//          }
//          break;
//      }
//      return true;
//    }
//  }
//
//  public ColorPickerDialog(Context context, OnColorChangedListener listener,
//      int initialColor) {
//    super(context);
//
//    mListener = listener;
//    mInitialColor = initialColor;
//  }
//
//  protected void onCreate(Bundle savedInstanceState) {
//    super.onCreate(savedInstanceState);
//    OnColorChangedListener l = new OnColorChangedListener() {
//      public void colorChanged(int color) {
//        mListener.colorChanged(color);
//        dismiss();
//      }
//    };
//
//    setContentView(new ColorPickerView(getContext(), l, mInitialColor));
//    setTitle("Pick a Color");
//  }
//}