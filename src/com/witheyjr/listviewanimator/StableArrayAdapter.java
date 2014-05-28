/*
 * Copyright (C) 2013 The Android Open Source Project
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class StableArrayAdapter extends ArrayAdapter<String> {

    HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();
    
    List<String> objects = new ArrayList<String>();
    MainActivity mainActivity;
    Context context;
    int layoutResourceId;
    
    public StableArrayAdapter(Context context, int layoutResourceId, int textViewResourceId,
            List<String> origObjects) {
        // super(context, textViewResourceId, objects);
        super(context, layoutResourceId, textViewResourceId, origObjects);
        this.objects = origObjects;
        for (int i = 0; i < objects.size(); ++i) {
            // objects.add(origObjects.get(i));
        	mIdMap.put(objects.get(i), i);
        }
        this.context = context;
        this.layoutResourceId = layoutResourceId;
    }
    
    /*public void addItem(String toAdd, int pos) {
    	objects.add(pos, toAdd);
    }*/
    
    public long getItemIdForAnimation(int position) {
        // String item = getItem(position);
        return mIdMap.get(objects.get(position));
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
    
    /* this is necessary for correct animations after new views/items have been added
     * to the ListView.
     */    
    public void reIdMapObjects() {
		mIdMap.clear();
		for (int i = 0; i < objects.size(); i++) {
			mIdMap.put(objects.get(i), i);
		}
	}
    
    /*@Override
	public int getCount() {
		return objects.size();
	}

	@Override
	public String getItem(int position) {
		return objects.get(position);
	}*/
	
	@Override
	public long getItemId(int position) {
		// return position;
		return mIdMap.get(position);
	}
	
	/*@Override
	public int getPosition(String item) {
		return super.getPosition(item);
	}*/
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        
    	if (null == convertView)
			convertView = LayoutInflater.from(context).inflate(layoutResourceId, null);
    	TextView title = (TextView) convertView.findViewById(R.id.title);
    	title.setText(objects.get(position));
    	// View view = super.getView(position, convertView, parent);
        /*if (view != convertView) {
            // Add touch listener to every new view to track swipe motion
        }*/
        return convertView;
    }

}
