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

import static com.nineoldandroids.view.ViewHelper.setTranslationX;
import static com.nineoldandroids.view.ViewHelper.setTranslationY;
import static com.nineoldandroids.view.ViewHelper.setAlpha;
import java.util.HashMap;
import android.animation.Animator.AnimatorListener;
import android.annotation.SuppressLint;
import android.os.Build;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ListView;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.view.ViewPropertyAnimator;

/**
 * @author J Withey
 * ListViewAnimator allows support for animating the removal and addition of items in a ListView.
 * Undo is also supported via callbacks. Based on open source code by the DevBytes team: 
 * http://graphics-geek.blogspot.co.uk/2013/06/devbytes-animating-listview-deletion.html
 */
public class ListViewAnimator {
	
	/** To decide between translating Y or X axes in animation */
	private enum Axis {
		X, Y;
	}
	
	private ListView mListView;
	private static final int MOVE_DURATION = 250;
	private AccelerateDecelerateInterpolator mInterpolator = new AccelerateDecelerateInterpolator();
	private ListViewAnimatorCallbacks mCallbacks;
	
	private boolean belowApi12 = false;
	private boolean belowApi16 = false;
	private boolean currentlyAnimating = false;
	
	/* This stores the coordinates of the top of each View, so that when we animate removal of
	 * items, we can animate them into the correct position based on where the other views are. */	
	private HashMap<Long, Integer> mItemIdTopMap = new HashMap<Long, Integer>();
	
	public ListViewAnimator(ListView listView, ListViewAnimatorCallbacks callbacks) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1) {
			belowApi12 = true;
		} else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
			belowApi16 = true;
		}
		this.mCallbacks = callbacks;
		this.mListView = listView;
	}
	
	/** 
	 * Callbacks for the Activity to implement to support functionality around the animations,
	 * such as adding or removing items from adapter, supporting undo and enabling/disabling 
	 * buttons.
	 */
	public interface ListViewAnimatorCallbacks {
		/** 
		 * Here the remove(), notifyDatasetChanged() etc. are carried out after the view has been
		 * swiped away. After this has been called, the views are subsequently animated to fill 
		 * the gap. The present implementation includes undo functionality.
		 */
		void deleteAdapterItemCallback(int position);
		
		/** 
		 * This is called when the removal animation ends. You can choose to fill this method or not
		 * with code that will show a PopupWindow with option to undo the last deleted item.
		 */
		void undoCallback();
		
		/** 
		 * In order not to cause glitchy animations, the button used to add items is disabled and enabled
		 * here. Once the animation is finished, the add button(s) is enabled again.
		 */
		void addButtonSetEnabledCallback(boolean enabled);
		
		/** 
		 * Called when an item needs to be added. This is after the views below the item have moved and
		 * given way for the new view to appear. 
		 */
		void addAdapterItemCallback(int pos, Object itemToAdd);
		
		/** 
		 * This could potentially simply be the result of getItemId() in the Adapter,
		 * but for my implementation this is used, hence the option to get id
		 * another way.
		 */
		long getItemIdForAnimation(int position);
	}
	
	/** In this implementation, this is to disable swiping which may cause animation glitches */
	public boolean listViewIsAnimating() {
		return currentlyAnimating;
	}
	
	/** Used to set the pre-animation parameters as appropriate for API level and axis of
	 * animation.  
	 */
	@SuppressLint("NewApi")
	private void setPreAnimationParameters(View view, int delta, Axis axis) {
		if (belowApi12) {
			if (axis == Axis.X) {
				setTranslationX(view, delta);
				setAlpha(view, 0);
			} else {
				setTranslationY(view, delta);
			}
		} else {
			if (axis == Axis.X) {
				view.setTranslationX(delta);
				view.setAlpha(0);
			} else {
				view.setTranslationY(delta);
			}
		}
	}
	
	/** Animate the view appropriately for the chosen axis
	 * @param view The view to animate
	 * @param to The value to animate to
	 * @param axis The axis of which to animate the translation 
	 */
	@SuppressLint("NewApi")
	private void animateAxis(View view, float to, Axis axis) {
		if (belowApi12) {
			ViewPropertyAnimator animator = ViewPropertyAnimator.animate(view)
    		.setDuration(MOVE_DURATION)
    		// axis == axis.X ? .translationX(to) : .translationY(to)
        	.setInterpolator(mInterpolator);
			if (axis == Axis.X) {
				animator.translationX(to)
				.alpha(1);
			} else {
				animator.translationY(to);
			}
        } else {
        	android.view.ViewPropertyAnimator animator = view.animate()
        	.setDuration(MOVE_DURATION)
        	.setInterpolator(mInterpolator);
        	if (axis == Axis.X) {
				animator.translationX(to)
				.alpha(1);
			} else {
				animator.translationY(to);
			}
        }
	}
	
	/** This code attaches onto an existing animation (so we need no duration etc. here)
	 * and is useful for re-enabling the ListView etc. after all animations have finished.
	 * @param view The animated view that we are attaching an end action onto
	 */
	@SuppressLint("NewApi")
	private void afterFirstRemoval(View view) {
		if (belowApi12) {
    		ViewPropertyAnimator.animate(view).setListener(new AnimatorListenerAdapter() {
    			@Override
                public void onAnimationEnd(Animator animation) {
    				afterAllRemovalAnimations();
                }
    		});
    	} else if (belowApi16) {
    		view.animate().setListener(new AnimatorListener() {
    			@Override public void onAnimationStart(android.animation.Animator animation) { }
				@Override
				public void onAnimationEnd(android.animation.Animator animation) { 
					afterAllRemovalAnimations();
				}
				@Override public void onAnimationCancel(android.animation.Animator animation) { }
				@Override public void onAnimationRepeat(android.animation.Animator animation) { }
			});
    	} else {
    		view.animate().withEndAction(new Runnable() {	// position = current animating view pos in adapter
                public void run() {
                	afterAllRemovalAnimations();
                }
            });
    	}
	}
	
	/**
     * This method animates all other views in the ListView container (not including ignoreView)
     * into their final positions. It is called after ignoreView has been removed from the
     * adapter, but before layout has been run. The approach here is to figure out where
     * everything is now, then allow layout to run, then figure out where everything is after
     * layout, and then to run animations between all of those start/end positions.
     */
	@SuppressLint("NewApi")
	public void animateRemoval(View viewToRemove, int adapterPos) {
		currentlyAnimating = true;
		mCallbacks.addButtonSetEnabledCallback(false);
		int firstVisiblePosition = mListView.getFirstVisiblePosition();
        final int preDeleteChildCount = mListView.getChildCount();
        for (int i = 0; i < preDeleteChildCount; ++i) {
            View child = mListView.getChildAt(i);
            if (child != viewToRemove) {
                int position = firstVisiblePosition + i;
                long itemId = mCallbacks.getItemIdForAnimation(position); 
                mItemIdTopMap.put(itemId, child.getTop());
            }
        }
        mCallbacks.deleteAdapterItemCallback(adapterPos);		
        final ViewTreeObserver observer = mListView.getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                observer.removeOnPreDrawListener(this);
                boolean firstAnimation = true;
                int firstVisiblePosition = mListView.getFirstVisiblePosition();	// get adapter's pos of view, as 0 in listView may != 0 in adapter
                boolean didAnimate = false;
                for (int i = 0; i < mListView.getChildCount(); ++i) {	// for each visible listView child
                	final View child = mListView.getChildAt(i);
                    int position = firstVisiblePosition + i;
                    long itemId = mCallbacks.getItemIdForAnimation(position);	// position in adapter
                    Integer startTop = mItemIdTopMap.get(itemId);	// the start point of the top of current child. We need this to decide where to animate from
                    int top = child.getTop();
                    if (startTop != null) {		// startTop not null means child was already visible when other view swiped. 					
                    	if (startTop != top) {		// check if the starting and end positions aren't the same. Only animate if so.
	                		didAnimate = true;			// didAnimate flag used for re-enabling elements disabled to prevent animation glitches
	                		int delta = startTop - top;	// delta = startPosition for child to animate from (where it was!)
	                		setPreAnimationParameters(child, delta, Axis.Y); 
	                    	animateAxis(child, 0, Axis.Y);
	                        if (firstAnimation) {
	                        	afterFirstRemoval(child);
	                            firstAnimation = false;
	                        }
                    	}
                    } else { // this view was not visible before the view was removed!
                        /*Animate new views along with the others. The catch is that they did not
                        exist in the start state, so we must calculate their starting position
                        based on neighbouring views.*/
                        int childHeight = child.getHeight() + mListView.getDividerHeight();
                        startTop = top + (i > 0 ? childHeight : -childHeight);
                        int delta = startTop - top;
                        setPreAnimationParameters(child, delta, Axis.Y);
	                    animateAxis(child, 0, Axis.Y);
                        if (firstAnimation) {
                        	afterFirstRemoval(child);
                            firstAnimation = false;
                        }
                    }
                }
                if (!didAnimate) { // reenable listView if no views were animated
                	afterAllRemovalAnimations();
                }
                mItemIdTopMap.clear();
                return true;
            }
        });
    }
	
	/** Called when all removal animations are complete. ListView and 'add button' are re-enabled.
	 * Undo option given as possibility.
	 */
	private void afterAllRemovalAnimations() {
		mListView.setEnabled(true);
        mCallbacks.addButtonSetEnabledCallback(true);
        mCallbacks.undoCallback();
        currentlyAnimating = false;
	}
	
	/** Called when all addition animations are complete. ListView and 'add button' are re-enabled.
	 */
	private void afterAllAdditionAnimations() {
		mCallbacks.addButtonSetEnabledCallback(true);
		mListView.setEnabled(true);
	}
	
    /** This code supplements the existing animation that may be running on the view.
     * @param view The animated view that we are attaching an end action onto
     */
    @SuppressLint("NewApi")
	private void afterAddition(View view) {
    	if (belowApi12) {
    		ViewPropertyAnimator.animate(view).setListener(new AnimatorListenerAdapter() {
    			@Override
                public void onAnimationEnd(Animator animation) {
    				afterAllAdditionAnimations();
                }
    		});
    	} else if (belowApi16) {
    		view.animate().setListener(new AnimatorListener() {
    			@Override public void onAnimationStart(android.animation.Animator animation) { }
				@Override
				public void onAnimationEnd(android.animation.Animator animation) { 
					afterAllAdditionAnimations();
				}
				@Override public void onAnimationCancel(android.animation.Animator animation) { }
				@Override public void onAnimationRepeat(android.animation.Animator animation) { }
			});
    	} else {
    		view.animate().withEndAction(new Runnable() {
                public void run() {
                	afterAllAdditionAnimations();
                }
            });
    	}
	}
    
    /** For additions, views are first animated to make room for the new view, then the new view is added
     * and animated in. This code attaches onto existing animation (thus no duration etc.) to add when
     * the animation is complete. 
     * @param view The view that is already animating that we attach this onAnimationEnd onto
     * @param pos The position within the adapter that we wish to add an item
     * @param itemToAdd The object that we wish to add to the adapter
     * @param firstVisiblePosition The position within the adapter of the first visible position in the 
     * ListView
     */
    @SuppressLint("NewApi")
	private void afterViewsMadeWayForAddition(View view, final int pos, final Object itemToAdd, final int firstVisiblePosition) {
		if (belowApi12) {
    		ViewPropertyAnimator.animate(view).setListener(new AnimatorListenerAdapter() {
    			@Override
                public void onAnimationEnd(Animator animation) {
    				addThenAnimateAddedView(pos, itemToAdd, firstVisiblePosition);
                }
    		});
    	} else if (belowApi16) {
    		view.animate().setListener(new AnimatorListener() {
    			@Override public void onAnimationStart(android.animation.Animator animation) { }
				@Override
				public void onAnimationEnd(android.animation.Animator animation) { 
					addThenAnimateAddedView(pos, itemToAdd, firstVisiblePosition);
				}
				@Override public void onAnimationCancel(android.animation.Animator animation) { }
				@Override public void onAnimationRepeat(android.animation.Animator animation) { }
			});
    	} else {
    		view.animate().withEndAction(new Runnable() {	// position = current animating view pos in adapter
                public void run() {
                	addThenAnimateAddedView(pos, itemToAdd, firstVisiblePosition);
                }
            });
    	}
	}
    
    
    /** Convenience method to add a new view at first position of ListView 
     */
    public void animateAddition(Object itemToAdd) {
    	animateAddition(itemToAdd, 0);
    }
    
    /** When animating addition, we first animate all views which are after the adapter position
     * where the new view will be inserted down to make room. The new view is then added and animated
     * in. If the view added is not visible but above the visible items, all will animate down. If 
     * the view is below all visible views, nothing will animate unless the items do not fill the 
     * screen.
     * @param itemToAdd the object to add to the adapter
     * @param pos the position within the adapter to add the object
     */
    @SuppressLint("NewApi")
	public void animateAddition(final Object itemToAdd, final int pos) {
    	mListView.setEnabled(false);
    	mCallbacks.addButtonSetEnabledCallback(false);
    	final int firstVisiblePosition = mListView.getFirstVisiblePosition();
    	int lastVisiblePosition = mListView.getLastVisiblePosition();
    	int childCount = mListView.getChildCount();
    	if (pos > lastVisiblePosition) { // Do not animate unless once added, the new view is the new lastVisiblePos, in which case only animate inserted view
    		mCallbacks.addAdapterItemCallback(pos, itemToAdd);
			final ViewTreeObserver observer = mListView.getViewTreeObserver();
	        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
	            public boolean onPreDraw() {
	                observer.removeOnPreDrawListener(this);
	                if (pos == mListView.getLastVisiblePosition()) { // the added view is the new lastVisiblePos
		                final View child = mListView.getChildAt(mListView.getChildCount() - 1); // inserted view
		                setPreAnimationParameters(child, child.getWidth(), Axis.X);
		                animateAxis(child, 0, Axis.X);
                        afterAddition(child);
	                } else {
	    				// No animating as item is not currently visible and below last item
	                	afterAllAdditionAnimations();
	    			}
					return true;
	            }
	        });
    	} else { // Animate the already visible children to make way for new view, then animate insertion of new view
    		boolean firstAnimation = true;     		
    		mListView.setEnabled(false); // disable listView scrolling during animation
    		for (int i = 0; i < childCount; i++) { // each visible view
    			int position = firstVisiblePosition + i;
    			final View child = mListView.getChildAt(i);
    			// Animate down each visible view including and after the insertion position
    			if (position >= pos) {
    				animateAxis(child, child.getHeight() + mListView.getDividerHeight(), Axis.Y);
    				if (firstAnimation) { // animate in the new child if visible
    					afterViewsMadeWayForAddition(child, pos, itemToAdd, firstVisiblePosition);
    					firstAnimation = false;
                    }
    			}
    		}
    	}
    }
    
    /**
     * This is called when the necessary views have made way for the new view, all that is
     * left to do now is add and animate the new view into the list (if visible), but we 
     * also need to reset the translationY values as necessary to keep the final positions 
     * correct.
     * @param pos the position within the adapter to add the object
     * @param itemToAdd the object to add to the adapter
     * @param firstVisiblePosition the position within the adapter of the first visible 
     * position in the ListView
     */
    @SuppressLint("NewApi")
	private void addThenAnimateAddedView(final int pos, Object itemToAdd, int firstVisiblePosition) {
    	mCallbacks.addAdapterItemCallback(pos, itemToAdd);
    	/* reset Y values after they have been animated to make room for new view.
    	 * need to do all views indiscriminately because in older versions of Android 
    	 * view recycling (after notifyDataSetChanged()) is unpredictable */
    	for (int i = 0; i < mListView.getChildCount(); i++) {
			View v = mListView.getChildAt(i);
			setPreAnimationParameters(v, 0, Axis.Y);
		}
		final int firstPos = mListView.getFirstVisiblePosition();
		final int lastPos = mListView.getLastVisiblePosition();
		if (firstPos <= pos && pos <= lastPos) { // if added item is visible
			final ViewTreeObserver observer = mListView.getViewTreeObserver();
	        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
	            public boolean onPreDraw() {
	                observer.removeOnPreDrawListener(this);
					final int loc = pos - firstPos;
					final View child = mListView.getChildAt(loc); // inserted view
					setPreAnimationParameters(child, 0, Axis.Y);
					setPreAnimationParameters(child, child.getWidth(), Axis.X);
					animateAxis(child, 0, Axis.X);
					afterAddition(child);
					return true;
	            }
	        });
		} else if (pos < firstVisiblePosition) { // if added item is not visible and is above all visible items
			/* animate in child 0 from above after others have moved down. This is not the newly added item child.
			 * This is a special case because unlike with removal, addition first animates, then adds and as a
			 * result, views moved down do not include the first visible view, which must appear later after
			 * adding.
			 */
			final ViewTreeObserver observer = mListView.getViewTreeObserver();
	        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
	            public boolean onPreDraw() {
	                observer.removeOnPreDrawListener(this);
        			final View child0 = mListView.getChildAt(0);
        			setPreAnimationParameters(child0, -(child0.getHeight() + mListView.getDividerHeight()), Axis.Y);
        			animateAxis(child0, 0, Axis.Y);
        			afterAddition(child0);
					return true;
	            }
	        });
		} else { // the added item is below all visible items. No need to animate.
			afterAllAdditionAnimations();
		}
    }
	
}















