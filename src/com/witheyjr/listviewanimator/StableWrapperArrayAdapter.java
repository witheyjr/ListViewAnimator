/*
 * Copyright (C) 2014 J Withey
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

package com.witheyjr.listviewanimator;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.nineoldandroids.view.ViewPropertyAnimator.animate;
import static com.nineoldandroids.view.ViewHelper.setAlpha;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * @author J Withey
 * StableWrapperArrayAdapter makes some tweaks to a bog-standard adapter to make it compatible with
 * the {@link ListViewAnimator}. Specifically, the {@link getItemIdForAnimation()} and related {@link mIdMap} are necessary
 * to keep track of the correct items for animating, and just prior to notifyDataSetChanged(), the
 * mIdMap should be updated with the method {@link reIdMapObjects}.
 * 
 * Based on open source code by the DevBytes team: 
 * http://graphics-geek.blogspot.co.uk/2013/06/devbytes-animating-listview-deletion.html
 */
public class StableWrapperArrayAdapter extends ArrayAdapter<ContentsWrapper> {

    private HashMap<ContentsWrapper, Integer> mIdMap = new HashMap<ContentsWrapper, Integer>();
    private List<ContentsWrapper> mObjects = new ArrayList<ContentsWrapper>();
    private Context mContext;
    private int mLayoutResourceId;
    private boolean withImage = false;
    private final int mDuration = 100;
    private LruCache<String, Bitmap> mMemoryCache;

    public StableWrapperArrayAdapter(Context context, int layoutResourceId,
    		List<ContentsWrapper> origObjects) {
    	super(context, layoutResourceId, origObjects);
        this.mObjects = origObjects;
        for (int i = 0; i < mObjects.size(); ++i) {
        	mIdMap.put(mObjects.get(i), i);
        }
        this.mContext = context;
        this.mLayoutResourceId = layoutResourceId;
    	withImage = true;
    	/* Get max available VM memory, exceeding this amount will throw an OutOfMemory exception. 
    	 * Stored in kilobytes as LruCache takes an int in its constructor.*/
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        // Use 1/4 of the available memory for this memory cache.
        final int cacheSize = maxMemory / 4;
        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @SuppressLint("NewApi")
			@Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than number of items. Also, I love fragmentation.
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1) {
                	return (bitmap.getRowBytes() * bitmap.getHeight()) / 1024;
                } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                	return bitmap.getByteCount() / 1024;
                } else {
                	return bitmap.getAllocationByteCount() / 1024;
                }
            }
        };
    }
    
    /** This is necessary for the correct items to be animated by the ListViewAnimator.
     * Cannot simply override getItemId() as this is used elsewhere.
     */
    public long getItemIdForAnimation(int position) {
        return mIdMap.get(mObjects.get(position));
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
    
    /** This is necessary for correct animations after new views/items have been added
     * to the ListView. To be called just prior to notifyDataSetChanged().
     */    
    public void reIdMapObjects() {
		mIdMap.clear();
		for (int i = 0; i < mObjects.size(); i++) {
			mIdMap.put(mObjects.get(i), i);
		}
	}
    
    @Override
	public long getItemId(int position) {
		return position;
	}
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
    	if (null == convertView)
			convertView = LayoutInflater.from(mContext).inflate(mLayoutResourceId, null);
    	TextView title = (TextView) convertView.findViewById(R.id.title);
    	title.setText(mObjects.get(position).getContents());
    	if (withImage) {
    		ImageView mImageView = (ImageView) convertView.findViewById(R.id.image);
    		int resId = mObjects.get(position).getImageResource();
    		final String imageKey = String.valueOf(resId);
    		final Bitmap bitmap = getBitmapFromMemCache(imageKey);
    		if (bitmap != null) {
    			mImageView.setImageBitmap(bitmap);
    		} else if (cancelPotentialLoad(mObjects.get(position).getImageResource(), mImageView)) {
    			ImageLoaderTask task = new ImageLoaderTask(mImageView);
	    		LoadedDrawable loadedDrawable = new LoadedDrawable(task);
	    		mImageView.setImageDrawable(loadedDrawable);
	    		task.execute(mObjects.get(position).getImageResource());
    		}
    	}
        return convertView;
    }
    
    /* ########################################################################################
     * # The following are all methods to efficiently load and cache images, and not relevant #
     * # to the workings of the ListViewAnimator.											  #
     * ########################################################################################
     */
    
    private void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }
    
    private Bitmap getBitmapFromMemCache(String key) {
        return mMemoryCache.get(key);
    }
    
    private static boolean cancelPotentialLoad(int id, ImageView imageView) {
        ImageLoaderTask imageLoaderTask = getImageLoaderTask(imageView);
        if (imageLoaderTask != null) {
            int bitmapId = imageLoaderTask.id;
            if ((bitmapId == 0) || (bitmapId != id)) {
                imageLoaderTask.cancel(true);
            } else {
                // The same id is already being loaded.
                return false;
            }
        }
        return true;
    }
    
    private static ImageLoaderTask getImageLoaderTask(ImageView imageView) {
        if (imageView != null) {
            Drawable drawable = imageView.getDrawable();
            if (drawable instanceof LoadedDrawable) {
                LoadedDrawable downloadedDrawable = (LoadedDrawable)drawable;
                return downloadedDrawable.getImageLoaderTask();
            }
        }
        return null;
    }
    
    private class ImageLoaderTask extends AsyncTask<Integer, Void, Bitmap> {
    	private int id;
    	private final WeakReference<ImageView> imageViewReference;        
        private ImageLoaderTask(ImageView image) {
        	this.imageViewReference = new WeakReference<ImageView>(image);
        }
        
        @Override
        protected Bitmap doInBackground(Integer... params) {
            Bitmap bitmap = null;
            bitmap = BitmapFactory.decodeResource(mContext.getResources(), params[0]);
            addBitmapToMemoryCache(String.valueOf(params[0]), bitmap);
            return bitmap;
        }
        
        @SuppressLint("NewApi")
		@SuppressWarnings("deprecation")
		@Override
        protected void onPostExecute(Bitmap result) {
        	if (isCancelled()) {
        		result = null;
        	}
        	if (imageViewReference != null) {
        		ImageView imageView = imageViewReference.get();
        		ImageLoaderTask imageLoaderTask = getImageLoaderTask(imageView);
        		if (this == imageLoaderTask) {
                    imageView.setImageBitmap(result);
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1) {
                    	setAlpha(imageView, 0);
                    	animate(imageView)
                    	.alpha(1)
                    	.setDuration(mDuration);
            		} else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            			imageView.setAlpha(0);
            			imageView.animate()
            			.alpha(1)
            			.setDuration(mDuration);
            		} else {
            			// Have to cast ImageView to View for animating for some reason
            			View dummy = (View) imageView;
            			dummy.setAlpha(0);
            			dummy.animate()
            			.alpha(1)
            			.setDuration(mDuration);
            		}
                }
        	}
        }
    }
    
    private static class LoadedDrawable extends ColorDrawable {
        private final WeakReference<ImageLoaderTask> imageLoaderTaskReference;
        public LoadedDrawable(ImageLoaderTask imageLoaderTask) {
            super(Color.WHITE);
            imageLoaderTaskReference = new WeakReference<ImageLoaderTask>(imageLoaderTask);
        }
        public ImageLoaderTask getImageLoaderTask() {
            return imageLoaderTaskReference.get();
        }
    }
}















