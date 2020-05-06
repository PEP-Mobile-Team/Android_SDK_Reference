/**
 * Copyright (C) 2003-2019, Foxit Software Inc..
 * All Rights Reserved.
 * <p>
 * http://www.foxitsoftware.com
 * <p>
 * The following code is copyrighted and is the proprietary of Foxit Software Inc.. It is not allowed to
 * distribute any parts of Foxit PDF SDK to third party or public without permission unless an agreement
 * is signed between Foxit Software Inc. and customers to explicitly grant customers permissions.
 * Review legal.txt for additional license and legal information.
 */
package com.foxit.uiextensions.modules.thumbnail.createpage.colorpicker;

import android.graphics.Color;

import java.util.ArrayList;
import java.util.List;

public class ObservableColor {

	private final float[] hsv = {0, 0, 0};
	private int alpha;
	private final List<ColorObserver> observers = new ArrayList<ColorObserver>();

	public ObservableColor(int color) {
		Color.colorToHSV(color, hsv);
		alpha = Color.alpha(color);
	}

	public void getHsv(float hsvOut[]) {
		hsvOut[0] = hsv[0];
		hsvOut[1] = hsv[1];
		hsvOut[2] = hsv[2];
	}

	public int getColor() {
		return Color.HSVToColor(alpha, hsv);
	}

	public float getHue() {
		return hsv[0];
	}

	public float getSat() {
		return hsv[1];
	}

	public float getValue() {
		return hsv[2];
	}

	public int getAlpha() {
		return alpha;
	}

	public float getLightness() {
		return getLightnessWithValue(hsv[2]);
	}

	public float getLightnessWithValue(float value) {
		float[] hsV = {hsv[0], hsv[1], value};
		final int color = Color.HSVToColor(hsV);
		return (Color.red(color) * 0.2126f + Color.green(color) * 0.7152f + Color.blue(color) * 0.0722f)/0xff;
	}

	public void addObserver(ColorObserver observer) {
		observers.add(observer);
	}

	public void updateHueSat(float hue, float sat, ColorObserver sender) {
		hsv[0] = hue;
		hsv[1] = sat;
		notifyOtherObservers(sender);
	}

	public void updateValue(float value, ColorObserver sender) {
		hsv[2] = value;
		notifyOtherObservers(sender);
	}

	public void updateColor(int color, ColorObserver sender) {
		Color.colorToHSV(color, hsv);
		alpha = Color.alpha(color);
		notifyOtherObservers(sender);
	}

	private void notifyOtherObservers(ColorObserver sender) {
		for (ColorObserver observer : observers) {
			if (observer != sender) {
				observer.updateColor(this);
			}
		}
	}
}
