/*
 * ******************************************************************************
 *   Copyright (c) 2014 J Withey.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *  *****************************************************************************
 */

package com.witheyjr.listviewanimator;

import java.util.ArrayList;
import android.support.v7.app.ActionBarActivity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

public class EggsampleActivity extends ActionBarActivity {
	
	private ListView mListView;
	private StableWrapperArrayAdapter mAdapter;
	private ListViewAnimator mListViewAnimator;
	private SwipeDismissListViewTouchListener mTouchListener;
	private PopupWindow mUndoPopup;
	private TextView mUndoText;
	private Button mUndoButton;
	private float mDensity;
	private ContentsWrapper toUndoOrRemove;
	private Boolean isRunning;
	private int adapterRemoveId;
	private MenuItem addButton;
	
	private String[] eggs = { "Fried egg", "Poached egg", "Hard-boiled egg", "Soft-boiled egg",
			"Scrambled egg", "Large egg", "Medium egg", "Small egg", "Chicken egg", "Duck egg", 
			"Goose egg", "Quail egg", "Coddled egg", "Shirred egg", "Basted egg", "Baked egg",
			"Eggs benedict", "Pickled egg", "Scotch egg", "Tea egg", "Omelette", "Good egg", 
			"Baaaaad egg", "Caviar", "Ostrich egg", "Pheasant egg", "Emu egg", "Eggcellent", 
			"Eggstraterrestrial", "Eggstravagant" };
	
	private int index = eggs.length - 1;
	
	private int[] eggImages = { R.drawable.fried_egg, R.drawable.poached_egg, R.drawable.hard_boiled_egg, R.drawable.soft_boiled_egg, 
			R.drawable.scrambled_egg, R.drawable.large_egg, R.drawable.medium_egg, R.drawable.small_egg, R.drawable.chicken_egg, 
			R.drawable.duck_egg, R.drawable.goose_egg, R.drawable.quail_egg, R.drawable.coddled_egg, R.drawable.shirred_egg, 
			R.drawable.basted_egg, R.drawable.baked_egg, R.drawable.eggs_benedict, R.drawable.pickled_egg, R.drawable.scotch_egg, 
			R.drawable.tea_egg, R.drawable.omelette, R.drawable.good_egg, R.drawable.bad_egg, R.drawable.caviar, R.drawable.ostrich_egg, 
			R.drawable.pheasant_egg, R.drawable.emu_egg, R.drawable.eggcellent, R.drawable.eggstraterrestrial, R.drawable.eggstravagant };
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mListView = (ListView) findViewById(R.id.list);
		ArrayList<ContentsWrapper> eggList = new ArrayList<ContentsWrapper>(); 
		for (int i = 0; i < eggs.length; ++i) {
            eggList.add(new ContentsWrapper(eggs[i], eggImages[i]));
        }        
        mAdapter = new StableWrapperArrayAdapter(this, R.layout.eggsample_image_item, eggList);
        mListView.setAdapter(mAdapter);
		mTouchListener = new SwipeDismissListViewTouchListener(
				mListView, 
				new SwipeDismissListViewTouchListener.DismissCallbacks() {
					@Override
					public void onDismiss(View viewToRemove, int position) {
						mListViewAnimator.animateRemoval(viewToRemove, position);
					}
					@Override
					public boolean canDismiss(int position) {
						mUndoPopup.dismiss(); // dismiss it if user touches away from popup
						return !mListViewAnimator.listViewIsAnimating();
					}
					@Override
					public void currentlyAnimatingSwipeOrScroll(boolean isAnimating) {
						addButton.setEnabled(!isAnimating);
					}
				});
		mListView.setOnTouchListener(mTouchListener);
		mListView.setOnScrollListener(mTouchListener.makeScrollListener());
		mListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				TextView textView = (TextView) view.findViewById(R.id.title);
				if (textView != null) {
					String text = "Clicked '" + textView.getText() + "'";
					Toast.makeText(EggsampleActivity.this, text, Toast.LENGTH_SHORT).show();
				}
			}
		});
		setUpUndoPopup();
	}
	
	/** Set up the undo popup style and size */
	private void setUpUndoPopup() {
		mDensity = mListView.getResources().getDisplayMetrics().density;
		LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View undoPopupView = inflater.inflate(R.layout.undo_popup, null);
		mUndoButton = (Button)undoPopupView.findViewById(R.id.undo);
		mUndoText = (TextView)undoPopupView.findViewById(R.id.text);
		mUndoText.setText("");
		mUndoPopup = new PopupWindow(undoPopupView);
		mUndoPopup.setAnimationStyle(R.style.fade_animation);
		// Get screen width in dp and set width of popup respectively
        int xdensity = (int)(mListView.getContext().getResources().getDisplayMetrics().widthPixels / mDensity);
        if(xdensity < 300) {
    		mUndoPopup.setWidth((int)(mDensity * 280));
        } else if (xdensity < 350) {
            mUndoPopup.setWidth((int)(mDensity * 300));
        } else if (xdensity < 500) {
            mUndoPopup.setWidth((int)(mDensity * 330));
        } else {
            mUndoPopup.setWidth((int)(mDensity * 450));
        }
		mUndoPopup.setHeight((int)(mDensity * 56));
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		isRunning = true;
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		isRunning = false;
		try {
			mUndoPopup.dismiss();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		mUndoButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mListViewAnimator.animateAddition(toUndoOrRemove, adapterRemoveId);
				mUndoPopup.dismiss();
				addButton.setEnabled(false);
			}
		});
		mListViewAnimator = new ListViewAnimator(mListView, new ListViewAnimator.ListViewAnimatorCallbacks() {
			@Override
			public void undoCallback() {
				String title = toUndoOrRemove.getContents();
				mUndoText.setText("Removed '" + title + "'");
                if (isRunning) { // Check if isRunning in order to prevent rare crash if user swipes and exits app 
                	try {
	                	mUndoPopup.showAtLocation(mListView, 
	         				Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM,
	         				0, (int)(mDensity * 15));
	                    timeDismissUndoMessage();
                	} catch (Exception e) {
                		e.printStackTrace();
                	}
                }
			}
			@Override
			public void deleteAdapterItemCallback(int position) {
				adapterRemoveId = position;
				toUndoOrRemove = mAdapter.getItem(position);
				mAdapter.remove(mAdapter.getItem(position));
				mAdapter.notifyDataSetChanged();
			}
			@Override
			public void addButtonSetEnabledCallback(boolean isEnabled) {
				addButton.setEnabled(isEnabled);
			}
			@Override
			public long getItemIdForAnimation(int position) {
				return mAdapter.getItemIdForAnimation(position);
			}
			@Override
			public void addAdapterItemCallback(int pos, Object itemToAdd) {
				ContentsWrapper toAdd = (ContentsWrapper) itemToAdd;
				mAdapter.insert(toAdd, pos);
				mAdapter.reIdMapObjects();
	    		mAdapter.notifyDataSetChanged();
			}
		});
		
	}
	
	private Handler handler = new Handler();
	Runnable runnable = new Runnable() {
		@Override
		public void run() {
			try {
				mUndoPopup.dismiss();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};
	
	/** Simply removes the undo popup after a 5 second delay once called */
	private void timeDismissUndoMessage() {
		handler.removeCallbacks(runnable);
		handler.postDelayed(runnable, 5000); // 5 second delay
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		addButton = menu.findItem(R.id.add);
		addButton.setEnabled(true);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.add:
				item.setEnabled(false);
				mUndoPopup.dismiss();
				mListViewAnimator.animateAddition(new ContentsWrapper(eggs[index], eggImages[index]));
				index = index == 0 ? eggs.length - 1 : --index;
				return true;
			default:
				break;
		}
		return super.onOptionsItemSelected(item);
	}
}



















