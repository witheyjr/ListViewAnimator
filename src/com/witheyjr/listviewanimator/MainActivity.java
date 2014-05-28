package com.witheyjr.listviewanimator;

import java.util.ArrayList;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.PopupWindow.OnDismissListener;

public class MainActivity extends ActionBarActivity {
	
	private ListView mListView;
	// private StableArrayAdapter mAdapter;
	private StableWrapperArrayAdapter mAdapter;
	private ListViewAnimator mListViewAnimator;
	private SwipeDismissListViewTouchListener mTouchListener;
	
	private PopupWindow mUndoPopup;
	private TextView mUndoText;
	private Button mUndoButton;
	
	private float mDensity;
	
	// private String toUndoOrRemove;
	private ContentsWrapper toUndoOrRemove;
	
	private Boolean isRunning;
	
	private int adapterRemoveId;
	
	private MenuItem addButton;
	
	private int index;
	
	// private boolean currentlyAnimatingSwipe = false;
	
	private String[] eggs = { "Fried egg", "Poached egg", "Hard-boiled egg", "Soft-boiled egg",
			"Scrambled egg", "Large egg", "Medium egg", "Small egg", "Chicken egg", "Duck egg", 
			"Goose egg", "Quail egg", "Coddled egg", "Shirred egg", "Basted egg", "Baked egg",
			"Eggs benedict", "Pickled egg", "Scotch egg", "Tea egg", "Omelette", "Good egg", 
			"Baaaaad egg", "Caviar", "Ostrich egg", "Pheasant egg", "Emu egg", "Eggcellent", 
			"Eggstraterrestrial", "Eggstravagant" };
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		/*if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}*/
		
		mListView = (ListView) findViewById(R.id.list);
        /*mAdapter = new ArrayAdapter<String>(this,
                R.layout.eggsample_item,
                R.id.title,
                new ArrayList<String>(Arrays.asList(eggs)));*/
		/*final ArrayList<String> eggList = new ArrayList<String>();
        for (int i = 0; i < eggs.length; ++i) {
            eggList.add(eggs[i]);
        }*/
		final ArrayList<ContentsWrapper> eggList = new ArrayList<ContentsWrapper>();
		for (int i = 0; i < eggs.length; ++i) {
            eggList.add(new ContentsWrapper(eggs[i]));
        }
		// mAdapter = new StableArrayAdapter(this, R.layout.eggsample_item, R.id.title, eggList);        
        mAdapter = new StableWrapperArrayAdapter(this, R.layout.eggsample_item, R.id.title, eggList);
        mListView.setAdapter(mAdapter);
		
		mTouchListener = new SwipeDismissListViewTouchListener(
				mListView, 
				new SwipeDismissListViewTouchListener.DismissCallbacks() {
					@Override
					public void onDismiss(ListView listView, View viewToRemove, int position) {
						// listView.setEnabled(false);
						mListViewAnimator.animateRemoval(listView, viewToRemove, position);
					}
					@Override
					public boolean canDismiss(int position) {
						mUndoPopup.dismiss(); // dismiss it if user touches away from popup
						// return mListView.isEnabled(); // prevent bugs from multiple swiping
						// return !listViewAnimator.listViewIsAnimating();
						return true;
					}
					@Override
					public boolean listViewIsAnimating() {
						return mListViewAnimator.listViewIsAnimating();
					}
					@Override
					public void currentlyAnimatingSwipe(boolean isAnimating) {
						// currentlyAnimatingSwipe = isAnimating;
						Log.d("onScrollStateChanged", "Setting addButton enabled(" + !isAnimating + ")");
						addButton.setEnabled(!isAnimating);
					}
					/*@Override
					public void currentlyAnimatingScroll(boolean isAnimating) {
						addButton.setEnabled(!isAnimating);
					}*/
				}, 0);
		mListView.setOnTouchListener(mTouchListener);
		mListView.setOnScrollListener(mTouchListener.makeScrollListener());
		mDensity = mListView.getResources().getDisplayMetrics().density;
		
		LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		View undoPopupView = inflater.inflate(R.layout.undo_popup, null);
		mUndoButton = (Button)undoPopupView.findViewById(R.id.undo);
		
		mUndoText = (TextView)undoPopupView.findViewById(R.id.text);
		mUndoText.setText("");
		
		mUndoPopup = new PopupWindow(undoPopupView);
		mUndoPopup.setAnimationStyle(R.style.fade_animation);
        
		// Get screen width in dp and set width of popups respectively
        int xdensity = (int)(mListView.getContext().getResources().getDisplayMetrics().widthPixels / mDensity);
        if(xdensity < 300) {
    		mUndoPopup.setWidth((int)(mDensity * 280));
        } else if(xdensity < 350) {
            mUndoPopup.setWidth((int)(mDensity * 300));
        } else if(xdensity < 500) {
            mUndoPopup.setWidth((int)(mDensity * 330));
        } else {
            mUndoPopup.setWidth((int)(mDensity * 450));
        }
		mUndoPopup.setHeight((int)(mDensity * 56));
		mUndoPopup.setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss() {
				// toUndoOrRemove = null;
			}
		});
		index = eggs.length - 1;
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
	
	public ListViewAnimator getListViewAnimator() {
		return mListViewAnimator;
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
		
		mListViewAnimator = new ListViewAnimator(new ListViewAnimator.Callbacks() {
			@Override
			public void undoCallback() {
				// String title = toUndoOrRemove;
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
			/*@Override
			public void deleteAdapterItemCallback(View viewToRemove) {
				int position = mListView.getPositionForView(viewToRemove);
				adapterRemoveId = position;
				toUndoOrRemove = mAdapter.getItem(position);
				mAdapter.remove(mAdapter.getItem(position));
				mAdapter.notifyDataSetChanged();
			}*/
			@Override
			public void deleteAdapterItemCallback(int position) {
				// int position = mListView.getPositionForView(viewToRemove);
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
				// String toAdd = (String) itemToAdd;
				ContentsWrapper toAdd = (ContentsWrapper) itemToAdd;
				mAdapter.insert(toAdd, pos);
				// mAdapter.add(toAdd);
				// mAdapter.addItem(toAdd, pos);
				mAdapter.reIdMapObjects();
	    		mAdapter.notifyDataSetChanged();
			}
		}, mListView, mAdapter);
		
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
	
	private void timeDismissUndoMessage() {
		handler.removeCallbacks(runnable);
		handler.postDelayed(runnable, 5000); // 5 second delay
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		addButton = menu.findItem(R.id.add);
		addButton.setEnabled(true);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		switch (item.getItemId()) {
			case R.id.add:
				item.setEnabled(false);
				mListView.scrollBy(0, 0);
				mUndoPopup.dismiss();
				// listViewAnimator.animateAddition(eggs[index]);
				mListViewAnimator.animateAddition(new ContentsWrapper(eggs[index]));
				// index = index > eggs.length - 2 ? 0 : ++index;
				index = index == 0 ? eggs.length - 1 : --index;
				return true;
			/*case R.id.switch_activity:
				// Intent intent = new Intent(this, ImageActivity.class);
				// intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				// intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				// intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                // intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				// startActivity(intent);
				
				OldSwipeDismissListViewTouchListener mOldTouchListener =
                new OldSwipeDismissListViewTouchListener(
                        mListView,
                        new OldSwipeDismissListViewTouchListener.DismissCallbacks() {
                            @Override
                            public boolean canDismiss(int position) {
                                return true;
                            }

                            @Override
                            public void onDismiss(ListView listView, int[] reverseSortedPositions) {
                                for (int position : reverseSortedPositions) {
                                    mAdapter.remove(mAdapter.getItem(position));
                                }
                                mAdapter.notifyDataSetChanged();
                            }
                        });
				mListView.setOnTouchListener(mOldTouchListener);
				mListView.setOnScrollListener(mOldTouchListener.makeScrollListener());
				
				break;*/
			default:
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			return rootView;
		}
	}

}
