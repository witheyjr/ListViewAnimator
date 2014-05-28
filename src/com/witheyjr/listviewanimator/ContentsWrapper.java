package com.witheyjr.listviewanimator;

/**
 * Simple a wrapper class to hold text and an image resource id
 */
public class ContentsWrapper {
	
	private String text;
	private int imageResource;
	
	public ContentsWrapper(String text) {
		this.text = text;
	}
	
	public ContentsWrapper(String text, int imageResource) {
		this(text);
		this.imageResource = imageResource;
	}
	
	public String getContents() {
		return text;
	}
	
	public int getImageResource() {
		return imageResource;
	}
}