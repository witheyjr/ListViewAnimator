package com.witheyjr.listviewanimator;

import static com.nineoldandroids.view.ViewHelper.getTranslationY;
import static com.nineoldandroids.view.ViewHelper.setTranslationX;
import static com.nineoldandroids.view.ViewHelper.setTranslationY;
import static com.nineoldandroids.view.ViewHelper.setAlpha;
import java.util.HashMap;
import android.animation.Animator.AnimatorListener;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.annotation.SuppressLint;
import android.os.Build;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Adapter;
import android.widget.ListView;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.view.ViewPropertyAnimator;

/**
 * @author J Withey
 * ListViewAnimator allows support for animating the removal and addition of items in a ListView.
 * Undo is also available. Based on open source code by the DevBytes team.
 */
public class ListViewAnimator_before_clean {
	
	private enum Axis {
		X, Y;
		
		/*public int getValue() {
			return value;
		}*/
		
		/*@Override
		public String toString() {
			switch(this.ordinal()) {
				case 0: return "1 minute";
				case 1: return "2 minutes";
				case 2: return "3 minutes";
				case 3: return "4 minutes";
				case 4: return "5 minutes";
				case 5: return "6 minutes";
				case 6: return "7 minutes";
				case 7: return "8 minutes";
				case 8: return "9 minutes";
				case 9: return "10 minutes";
				case 10: return "12 minutes";
				case 11: return "15 minutes";
				case 12: return "20 minutes";
				case 13: return "30 minutes";
				case 14: return "45 minutes";
				case 15: return "60 minutes";
				case 16: return "90 minutes";
			}
			return super.toString();
		}*/
	}
	
	private ListView mListView;
	private Adapter mAdapter;
	
	private int swipedPos = -1;
    // private float swipedTranslationY = 0f;
	
	private static final int MOVE_DURATION = 300;
	
	// We need this because the swipe interpolator may be different, so we need to reset it as necessary before animating
	private AccelerateDecelerateInterpolator interpolator = new AccelerateDecelerateInterpolator();
	
	private boolean belowApi12 = false;
	private boolean belowApi16 = false;
	
	private boolean currentlyAnimating = false;
	
	private Callbacks callbacks;
	
	/* This stores the coordinates of the top of each View, so that when we animate removal of
	 * items, we can animate them into the correct position based on where the other views are. */	
	HashMap<Long, Integer> mItemIdTopMap = new HashMap<Long, Integer>();
	
	public ListViewAnimator_before_clean(Callbacks callbacks, ListView listView, Adapter adapter) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1) {
			belowApi12 = true;
		} else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
			belowApi16 = true;
		}
		this.callbacks = callbacks;
		this.mListView = listView;
		this.mAdapter = adapter;
	}
	
	/** 
	 * Callbacks for the Activity to implement to support functionality around the animations.
	 */
	public interface Callbacks {
		/** 
		 * Here the remove(), notifyDatasetChanged() etc. are carried out after the view has been
		 * swiped away. After this has run, the views are subsequently animated to fill the gap.
		 * See the sample app for my implementation.
		 */
		// void deleteAdapterItemCallback(View viewToRemove);
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
		 * but for my implementation this is overridden, hence the option to get id
		 * another way. See the sample app to see how my Adapter works.
		 */
		long getItemIdForAnimation(int position);
	}
	
	/*private class AttributeBundle {
    	private int top;

    	public AttributeBundle(int top) {
    		this.top = top;
    	}
    	
    	public int getTop() {
    		return top;
    	}
    }*/
	
	public boolean listViewIsAnimating() {
		return currentlyAnimating;
	}
	
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
	
	@SuppressLint("NewApi")
	private void animateAxis(View view, float to, Axis axis) {
		if (belowApi12) {
			ViewPropertyAnimator animator = ViewPropertyAnimator.animate(view)
    		.setDuration(MOVE_DURATION)
    		// axis == axis.X ? .translationX(to) : .translationY(to)
        	.setInterpolator(interpolator);
			if (axis == Axis.X) {
				animator.translationX(to)
				.alpha(1);
			} else {
				animator.translationY(to);
			}
        } else {
        	android.view.ViewPropertyAnimator animator = view.animate()
        	.setDuration(MOVE_DURATION)
        	.setInterpolator(interpolator);
        	if (axis == Axis.X) {
				animator.translationX(to)
				.alpha(1);
			} else {
				animator.translationY(to);
			}
        }
	}
	
	@SuppressLint("NewApi")
	private void afterFirstRemoval(View view) {
		if (belowApi12) {
    		ViewPropertyAnimator.animate(view).setListener(new AnimatorListenerAdapter() {
    			@Override
                public void onAnimationEnd(Animator animation) {
    				afterRemovalAnimation();
                }
    		});
    	} else if (belowApi16) {
    		view.animate().setListener(new AnimatorListener() {
    			@Override public void onAnimationStart(android.animation.Animator animation) { }
				@Override
				public void onAnimationEnd(android.animation.Animator animation) { 
					afterRemovalAnimation();
				}
				@Override public void onAnimationCancel(android.animation.Animator animation) { }
				@Override public void onAnimationRepeat(android.animation.Animator animation) { }
			});
    	} else {
    		view.animate().withEndAction(new Runnable() {	// position = current animating view pos in adapter
                public void run() {
                	afterRemovalAnimation();
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
	public void animateRemoval(final ListView listView, View viewToRemove, int adapterPos) {
		/* Get top (and overlay alpha for view 1) for each non-swiped view.
		 * Save swiped view's values
		 * Remove 
		 */
		currentlyAnimating = true;
		callbacks.addButtonSetEnabledCallback(false);
		int firstVisiblePosition = listView.getFirstVisiblePosition();
        final int preDeleteChildCount = listView.getChildCount();
        // final int preDeleteLastPosition = listView.getLastVisiblePosition();
        // final int preDeleteLastAdapterPosition = mAdapter.getCount() - 1;
        for (int i = 0; i < preDeleteChildCount; ++i) {
            View child = listView.getChildAt(i);
            if (child != viewToRemove) {
                int position = firstVisiblePosition + i;
                long itemId = callbacks.getItemIdForAnimation(position); 
                // mItemIdTopMap.put(itemId, child.getTop());
                // AttributeBundle bundle = new AttributeBundle(child.getTop());
                // Log.d("animateRemoval", "putting itemId == " + itemId);
                // mItemAttributeMap.put(itemId, bundle);
                mItemIdTopMap.put(itemId, child.getTop());
            } /*else {
            	// int position = firstVisiblePosition + i;
            	// Log.d("animateRemoval", "NOT putting itemId == " + callbacks.getItemIdForAnimation(position));
            	swipedPos = i;
            	if (belowApi12) {
            		swipedTranslationY = getTranslationY(viewToRemove);
            	} else {
            		swipedTranslationY = viewToRemove.getTranslationY();
            	}
            }*/
        }
        callbacks.deleteAdapterItemCallback(adapterPos);
        // callbacks.deleteAdapterItemCallback(viewToRemove);
		
        final ViewTreeObserver observer = listView.getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                observer.removeOnPreDrawListener(this);
                boolean firstAnimation = true;
                // int postDeleteChildCount = listView.getChildCount();			// amount of children visible (3 OR 4 on my screen in portrait)
                int firstVisiblePosition = listView.getFirstVisiblePosition(); 	// get adapter's pos of view, so 0 in listView != 0 in adapter
                boolean didAnimate = false; // to reenable listView if no views were animated
                for (int i = 0; i < listView.getChildCount(); ++i) {			// for each visible listView child
                	final View child = listView.getChildAt(i);
                    int position = firstVisiblePosition + i;					// listView childCount may be 3. For first child, 0 + 0 is first adapter item.
                    															// for second item, 0 + 1 = 1 in adapter. Scrolled down, 2 + 1 = 3rd adapter item. 
                    long itemId = callbacks.getItemIdForAnimation(position);	// position in adapter
                    Integer startTop = mItemIdTopMap.get(itemId);			// the start point of the top of current child
                    
                    // AttributeBundle bundle = mItemAttributeMap.get(itemId);
                    // Log.d("animateRemoval", "itemId == " + itemId);
                    // Integer startTop = bundle.getTop();							// the start point of the top of current child
                    
                    int top = child.getTop();
                    if (startTop != null) {					// startTop not null means child was already visible when other view swiped. 					
                    	if (startTop != top) {												// check if the starting and end positions aren't the same. Only animate if so.
	                		didAnimate = true;									// didAnimate flag used for re-enabling elements disabled to prevent animation glitches
	                		int delta = startTop - top;							// delta = startPosition for child to animate from (where it was!)
	                		setPreAnimationParameters(child, delta, Axis.Y);
	                        /*if (i != swipedPos) {			// if child has not taken place of swiped away view, we need to animate to the old view's appearance
	                        	if (i == 1										// We are at the second position
	                        			&& i < swipedPos						// The swiped position is below i
	                        			&& listView.getLastVisiblePosition() == mAdapter.getCount() - 1 	// getLastVisiblePos is last position in adapter. End of list. 
	                        			&& preDeleteChildCount == postDeleteChildCount) {					// amount of visible items is the same before and after delete
	                        		setPreAnimationParameters(child, delta);
	                            } else if (i == 0) { // should only run when at bottom of list, and preDeleteCount > postDeleteCount. This is because: 
	                        				  // a) when a view is swiped with bottom not showing, startTop == top, so we don't get to here.
	                        				  // b) when bottom is showing a new view becomes position 0, so we don't get to here.	                        		
	    	                        setPreAnimationParameters(child, delta); 
	    	                        animateAxis(child, 0, Axis.Y);
	    	                        Log.d("occurrence", "1/2");
	                        	} else {
	                        		animateView(child, 0);
	                        		Log.d("occurrence", "1/2");
	                        	}
	                        } else {*/								// this child is taking the place of the swiped view.
	                        										// Because it was already visible, no need to reset values
	                        	/*if (swipedPos == 1 													// Second position was swiped away
	                        			&& listView.getLastVisiblePosition() == mAdapter.getCount() - 1		// Last visible pos is last in adapter. End of list.
	                        			&& preDeleteChildCount == postDeleteChildCount						// Same amount of visible items before and after
	                        			&& preDeleteLastPosition == preDeleteLastAdapterPosition) {			// 
	                        		// TODO THESE special cases are no longer necessary!!! Was for when parallax effects were existent.
	                        		 this is a rare occurrence but for when we are at the
	                        		 * bottom of the list and swipe the item second from top.
	                        		 * Seems mighty specific but seems to be a special case.
	                        		 
	                        		// setPreAnimationParameters(child, delta);
	    	                        animateView(child, swipedTranslationY);
	    	                        Log.d("occurrence", "4");
	                            } else {*/
	                    	animateAxis(child, 0, Axis.Y);
	                        Log.d("occurrence", "1/2/3/4/5");
	                            // }
	                        //}
	                        if (firstAnimation) {
	                        	afterFirstRemoval(child);
	                            firstAnimation = false;
	                        }
                    	}
                    } else {													// this view was not visible before the view was removed!
                        // Animate new views along with the others. The catch is that they did not
                        // exist in the start state, so we must calculate their starting position
                        // based on neighbouring views.
                        int childHeight = child.getHeight() + listView.getDividerHeight();
                        startTop = top + (i > 0 ? childHeight : -childHeight);
                        int delta = startTop - top;
                        setPreAnimationParameters(child, delta, Axis.Y);
	                    animateAxis(child, 0, Axis.Y);
                        Log.d("occurrence", "6/7");
                        if (firstAnimation) {
                        	afterFirstRemoval(child);
                            firstAnimation = false;
                        }
                    }
                }
                if (!didAnimate) {
                	afterRemovalAnimation();
                }
                mItemIdTopMap.clear();
                return true;
            }
        });
    }
	
	private void afterRemovalAnimation() {
		mListView.setEnabled(true);
        callbacks.addButtonSetEnabledCallback(true);
        callbacks.undoCallback();
        currentlyAnimating = false;
	}
	
	private void afterAdditionAnimation() {
		callbacks.addButtonSetEnabledCallback(true);
		mListView.setEnabled(true);
	}
	
	/**
     * Utility, to avoid having to implement every method in AnimationListener in
     * every implementation class
     */
    static class AnimationListenerAdapter implements AnimationListener {

        @Override
        public void onAnimationEnd(Animation animation) {
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationStart(Animation animation) {
        }
    }
	
    @SuppressLint("NewApi")
	private void afterFirstAddition(View view) {
		if (belowApi12) {
    		ViewPropertyAnimator.animate(view).setListener(new AnimatorListenerAdapter() {
    			@Override
                public void onAnimationEnd(Animator animation) {
    				afterAdditionAnimation();
                }
    		});
    	} else if (belowApi16) {
    		view.animate().setListener(new AnimatorListener() {
    			@Override public void onAnimationStart(android.animation.Animator animation) { }
				@Override
				public void onAnimationEnd(android.animation.Animator animation) { 
					afterAdditionAnimation();
				}
				@Override public void onAnimationCancel(android.animation.Animator animation) { }
				@Override public void onAnimationRepeat(android.animation.Animator animation) { }
			});
    	} else {
    		view.animate().withEndAction(new Runnable() {	// position = current animating view pos in adapter
                public void run() {
                	afterAdditionAnimation();
                }
            });
    	}
	}
    
    @SuppressLint("NewApi")
	private void afterFirstAdditionExtra(View view, final int pos, final Object itemToAdd, final int firstVisiblePosition) {
		if (belowApi12) {
    		ViewPropertyAnimator.animate(view).setListener(new AnimatorListenerAdapter() {
    			@Override
                public void onAnimationEnd(Animator animation) {
    				addThenAnimateAddedView(mListView, pos, itemToAdd, firstVisiblePosition);
                }
    		});
    	} else if (belowApi16) {
    		view.animate().setListener(new AnimatorListener() {
    			@Override public void onAnimationStart(android.animation.Animator animation) { }
				@Override
				public void onAnimationEnd(android.animation.Animator animation) { 
					addThenAnimateAddedView(mListView, pos, itemToAdd, firstVisiblePosition);
				}
				@Override public void onAnimationCancel(android.animation.Animator animation) { }
				@Override public void onAnimationRepeat(android.animation.Animator animation) { }
			});
    	} else {
    		view.animate().withEndAction(new Runnable() {	// position = current animating view pos in adapter
                public void run() {
                	addThenAnimateAddedView(mListView, pos, itemToAdd, firstVisiblePosition);
                }
            });
    	}
	}
    
    public void animateAddition(Object itemToAdd) {
    	animateAddition(itemToAdd, 0);
    }
    
    @SuppressLint("NewApi")
	public void animateAddition(final Object itemToAdd, final int pos) {
    	callbacks.addButtonSetEnabledCallback(false);
    	final int firstVisiblePosition = mListView.getFirstVisiblePosition();
    	int lastVisiblePosition = mListView.getLastVisiblePosition();
    	int childCount = mListView.getChildCount();
    	if (pos > lastVisiblePosition && mListView.getChildCount() != 0) { // Do not animate, simply add, unless the lastVisiblePos is last in adapter!
    		
    		callbacks.addAdapterItemCallback(pos, itemToAdd);
			final ViewTreeObserver observer = mListView.getViewTreeObserver();
	        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
	            public boolean onPreDraw() {
	                observer.removeOnPreDrawListener(this);
	                boolean firstAnimation = true;
	                if (pos == mListView.getLastVisiblePosition()) {
		                final View child = mListView.getChildAt(mListView.getChildCount() - 1); // inserted view
		                
		                setPreAnimationParameters(child, child.getWidth(), Axis.X);
		                animateAxis(child, 0, Axis.X);
		                
		                /*if (belowApi12) {
		                	setTranslationX(child, child.getWidth());
			        		setAlpha(child, 0);
			        		ViewPropertyAnimator.animate(child)
			        		.setDuration(MOVE_DURATION)
			        		.translationX(0)
			        		.alpha(1)
			        		.setInterpolator(interpolator)
			        		.setListener(new AnimatorListenerAdapter() {
                    			@Override
                    			public void onAnimationEnd(Animator animation) {
                    				callbacks.addButtonSetEnabledCallback(true);
                    			}
                    		});
		                } else if (belowApi16) {
		                	child.setTranslationX(child.getWidth());
			        		child.setAlpha(0);
			        		child.animate()
			        		.setDuration(MOVE_DURATION)
			        		.translationX(0)
			        		.alpha(1)
			        		.setInterpolator(interpolator)
			        		.setListener(new AnimatorListener() {
    							@Override
    							public void onAnimationStart(android.animation.Animator animation) { }
    							@Override
    							public void onAnimationEnd(android.animation.Animator animation) { 
    								callbacks.addButtonSetEnabledCallback(true);
    							}
    							@Override
    							public void onAnimationCancel(android.animation.Animator animation) { }
    							@Override
    							public void onAnimationRepeat(android.animation.Animator animation) { }
    	                    });
		                } else {
		                	child.setTranslationX(child.getWidth());
			        		child.setAlpha(0);
			        		child.animate()
			        		.setDuration(MOVE_DURATION)
			        		.translationX(0)
			        		.alpha(1)
			        		.setInterpolator(interpolator)
			        		.withEndAction(new Runnable() {
			        			public void run() {
			        				callbacks.addButtonSetEnabledCallback(true);
			        			}
			        		});
		                }*/
		                if (firstAnimation) {
                        	afterFirstAddition(child);
                            firstAnimation = false;
                        }
	                } else {
	    				// No animating as item is not currently visible and below last item
	                	afterAdditionAnimation();
	    			}
					return true;
	            }
	        });
    	} else if (mListView.getChildCount() == 0) {
    		// only animate the insertion
    		callbacks.addAdapterItemCallback(0, itemToAdd);
			final ViewTreeObserver observer = mListView.getViewTreeObserver();
	        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
	            public boolean onPreDraw() {
	                observer.removeOnPreDrawListener(this);
	                boolean firstAnimation = true;
	                final View child = mListView.getChildAt(0);
	                setPreAnimationParameters(child, child.getWidth(), Axis.X);
	                animateAxis(child, 0, Axis.X);
	        		/*if (belowApi12) {
	        			setTranslationX(child, child.getWidth());
	        			setAlpha(child, 0);
	        			ViewPropertyAnimator.animate(child)
	        			.setDuration(MOVE_DURATION)
		        		.translationX(0)
		        		.alpha(1)
		        		.setInterpolator(interpolator)
		        		.setListener(new AnimatorListenerAdapter() {
	                        @Override
	                        public void onAnimationEnd(Animator animation) {
	                        	mListView.setEnabled(true);
	                        	callbacks.addButtonSetEnabledCallback(true);
	                        }
	                    });
	        		} else if (belowApi16) {
		                child.setTranslationX(child.getWidth());
		        		child.setAlpha(0);
		        		child.animate()
		        		.setDuration(MOVE_DURATION)
		        		.translationX(0)
		        		.alpha(1)
		        		.setInterpolator(interpolator)
		        		.setListener(new AnimatorListener() {
							@Override
							public void onAnimationStart(android.animation.Animator animation) { }
							@Override
							public void onAnimationEnd(android.animation.Animator animation) { 
								mListView.setEnabled(true);
								callbacks.addButtonSetEnabledCallback(true);
							}
							@Override
							public void onAnimationCancel(android.animation.Animator animation) { }
							@Override
							public void onAnimationRepeat(android.animation.Animator animation) { }
	                    });
	        		} else {
	        			child.setTranslationX(child.getWidth());
		        		child.setAlpha(0);
		        		child.animate()
		        		.setDuration(MOVE_DURATION)
		        		.translationX(0)
		        		.alpha(1)
		        		.setInterpolator(interpolator)
		        		.withEndAction(new Runnable() {
		        			public void run() {
		        				mListView.setEnabled(true);
		        				callbacks.addButtonSetEnabledCallback(true);
		        			}
		        		});
	        		}*/
	        		if (firstAnimation) {
                    	afterFirstAddition(child);
                        firstAnimation = false;
                    }
					return true;
	            }
	        });
    	} else { // Do animate the already visible children to make way for new view, then animate new view
    		boolean firstAnimation = true;
    		// View firstChild = mListView.getChildAt(0);     		
    		mListView.setEnabled(false); // disable listView scrolling during animation
    		for (int i = 0; i < childCount; i++) { // each visible view
    			int position = firstVisiblePosition + i;
    			final View child = mListView.getChildAt(i);
    			// Animate down each visible view including and after the insertion position
    			if (position >= pos) {
    				
    				animateAxis(child, child.getHeight() + mListView.getDividerHeight(), Axis.Y);
    				
    				if (belowApi12) {
    					// TODO hmmm do nothing?
    				} else if (belowApi16) {
    					child.animate().setListener(new AnimatorListener() {
							public void onAnimationStart(android.animation.Animator animation) { }
							public void onAnimationEnd(android.animation.Animator animation) { 
								child.setTranslationY(0);
							}
							public void onAnimationCancel(android.animation.Animator animation) { }
							public void onAnimationRepeat(android.animation.Animator animation) { }
    					});
    				} else {
    					child.animate()
    					.withEndAction(new Runnable() {
                			public void run() {
                				child.setTranslationY(0);
                			}
                		});
    				}
    				
    				// TODO
    				// TODO CHECK to see if we need this onPreDrawListener here, and if so use it on older versions of Android.
    				// TODO 
    				
    				/*if (belowApi12) {
    					final ViewTreeObserver observer = mListView.getViewTreeObserver();
    			        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
    			            public boolean onPreDraw() {
    			            	observer.removeOnPreDrawListener(this);
    			            	
    			            	// Hopefully fixes bug on older versions of Android. Presumably, it used to randomly switch views to recycle rather than do it
    			            	// in a way that is more predictable. Thus the view that was at position 'i' may now be elsewhere for some reason, even if it
    			            	// looks different and the underlying values have been changed.
    			            	
    			            	// TODO CHECK to see if we need this onPreDrawListener here, and use it if so on older versions of Android.
    			            	
    			            	final View newChild = child;
    			            	
		    					ViewPropertyAnimator.animate(newChild)
		    					.setDuration(MOVE_DURATION)
		    					.translationY(newChild.getHeight() + mListView.getDividerHeight())
		    					.setInterpolator(interpolator)
		    					.setListener(new AnimatorListenerAdapter() {
			                        @Override
			                        public void onAnimationEnd(Animator animation) { }
			                    });
		    					return true;
    			            }
    			        });
    				} else if (belowApi16) {
    					child.animate() // always animate to normal values as listView pos 0 will never be animated to here
        				.setDuration(MOVE_DURATION)
        				.translationY(child.getHeight() + mListView.getDividerHeight()) // I think the 'snapping to position' glitch is due to skipping frames
        				// or lots of CPU work e.g. Database stuff, therefore:
        				// TODO put CPU heavy adapter/Database work into AsyncTask! Or what if it is getView that is heavy??? test.
        				.setInterpolator(interpolator)
        				.setListener(new AnimatorListener() {
							@Override
							public void onAnimationStart(android.animation.Animator animation) { }
							@Override
							public void onAnimationEnd(android.animation.Animator animation) { 
								child.setTranslationY(0);
							}
							@Override
							public void onAnimationCancel(android.animation.Animator animation) { }
							@Override
							public void onAnimationRepeat(android.animation.Animator animation) { }
	                    });
    				} else {
    					child.animate() // always animate to normal values as listView pos 0 will never be animated to here
        				.setDuration(MOVE_DURATION)
        				.translationY(child.getHeight() + mListView.getDividerHeight()) // I think the 'snapping to position' glitch is due to skipping frames
        				// or lots of CPU work e.g. Database stuff, therefore:
        				// TODO put CPU heavy adapter/Database work into AsyncTask! Or what if it is getView that is heavy??? test.
        				.setInterpolator(interpolator)
        				.withEndAction(new Runnable() {
                			public void run() {
                				child.setTranslationY(0);
                			}
                		});
    				}*/
    				if (firstAnimation) { // animate in the new child if visible
    					// afterFirstAddition(child);
    					afterFirstAdditionExtra(child, pos, itemToAdd, firstVisiblePosition);
    					// TODO CLEAN this up after making sure it all works it is
    					
    					/*if (belowApi12) {
    						final ViewTreeObserver observer = mListView.getViewTreeObserver();
        			        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
        			            public boolean onPreDraw() {
        			            	observer.removeOnPreDrawListener(this);
        			            	final View newChild = child;
		    						ViewPropertyAnimator.animate(newChild)
		    						.setListener(new AnimatorListenerAdapter() {
		    	                        @Override
		    	                        public void onAnimationEnd(Animator animation) {
		    	                        	addThenAnimateAddedView(mListView, pos, itemToAdd, firstVisiblePosition);
		    	                        }
		    	                    });
		    						return true;
        			            }
        			        });
    					} else if (belowApi16) {
    						child.animate()
    						.setListener(new AnimatorListener() {
    							@Override
    							public void onAnimationStart(android.animation.Animator animation) { }
    							@Override
    							public void onAnimationEnd(android.animation.Animator animation) { 
    								addThenAnimateAddedView(mListView, pos, itemToAdd, firstVisiblePosition);
    							}
    							@Override
    							public void onAnimationCancel(android.animation.Animator animation) { }
    							@Override
    							public void onAnimationRepeat(android.animation.Animator animation) { }
    	                    });
    					} else {
	    					child.animate().withEndAction(new Runnable() {
	                            public void run() {
	                            	addThenAnimateAddedView(mListView, pos, itemToAdd, firstVisiblePosition);
	                            }
	                        });
    					}*/
    					firstAnimation = false;
                    }
    			}
    		}
    	}
    }
    
    private void animateAddedViewBelowApi12(final ListView listView, int pos, int firstVisiblePosition) {
    	// new view has already been added. Need to animate.
    	int firstPos = listView.getFirstVisiblePosition();
		int lastPos = listView.getLastVisiblePosition();
		
		for (int i = 0; i < listView.getChildCount(); i++) {
			View v = listView.getChildAt(i);
			setTranslationY(v, 0);
		}
		
		if (firstPos <= pos && pos <= lastPos) {
			final int loc = pos - firstPos;
    		final ViewTreeObserver observer = listView.getViewTreeObserver();
	        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
	            public boolean onPreDraw() {
	                observer.removeOnPreDrawListener(this);
	                final View newChild = listView.getChildAt(loc);
	                setTranslationX(newChild, newChild.getWidth());
	        		setAlpha(newChild, 0);
		    		ViewPropertyAnimator.animate(newChild)
		    		.setDuration(MOVE_DURATION)
		    		.translationX(0)
		    		.alpha(1)
		    		.setInterpolator(interpolator)
		    		.setListener(new AnimatorListenerAdapter() {
	                    @Override
	                    public void onAnimationEnd(Animator animation) {
	                    	listView.setEnabled(true);
		    				callbacks.addButtonSetEnabledCallback(true);
	                    }
	                });
		    		return true;
	            }
	        });
		} else if (pos < firstVisiblePosition) {
			final ViewTreeObserver observer = listView.getViewTreeObserver();
	        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
	            public boolean onPreDraw() {
	                observer.removeOnPreDrawListener(this);
        			final View child0 = listView.getChildAt(0);
					setTranslationY(child0, -(child0.getHeight() + listView.getDividerHeight()));
					ViewPropertyAnimator.animate(child0)
					.setDuration(MOVE_DURATION)
					.translationY(0)
					.setListener(new AnimatorListenerAdapter() {
	                    @Override
	                    public void onAnimationEnd(Animator animation) {
	                    	listView.setEnabled(true);
		    				callbacks.addButtonSetEnabledCallback(true);
	                    }
	                });
					return true;
	            }
	        });
		} else {
			listView.setEnabled(true);
			callbacks.addButtonSetEnabledCallback(true);
		}
    }
    
    @SuppressLint("NewApi")
	private void addThenAnimateAddedView(final ListView listView, int pos, Object itemToAdd, int firstVisiblePosition) {
    	callbacks.addAdapterItemCallback(pos, itemToAdd);
    	boolean firstAnimation = true;
    	if (belowApi12) {
			animateAddedViewBelowApi12(listView, pos, firstVisiblePosition);
			return;
		}
		
		int firstPos = listView.getFirstVisiblePosition();
		int lastPos = listView.getLastVisiblePosition();
		if (firstPos <= pos && pos <= lastPos) { // if added item is visible
    		final int loc = pos - firstPos;
			final View child = listView.getChildAt(loc); // inserted view
			
			setPreAnimationParameters(child, 0, Axis.Y);
			setPreAnimationParameters(child, child.getWidth(), Axis.X);
			animateAxis(child, 0, Axis.X);
			
			if (firstAnimation) {
				afterFirstAddition(child);
				firstAnimation = false;
			}
			
			/*if (belowApi12) {
				setTranslationX(child, child.getWidth());
	    		setAlpha(child, 0);
	    		final ViewTreeObserver observer = listView.getViewTreeObserver();
		        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
		            public boolean onPreDraw() {
		                observer.removeOnPreDrawListener(this);
		                final View newChild = listView.getChildAt(loc);		                
			    		ViewPropertyAnimator.animate(newChild)
			    		.setDuration(MOVE_DURATION)
			    		.translationX(0)
			    		.alpha(1)
			    		.setInterpolator(interpolator)
			    		.setListener(new AnimatorListenerAdapter() {
		                    @Override
		                    public void onAnimationEnd(Animator animation) {
		                    	listView.setEnabled(true);
			    				callbacks.addButtonSetEnabledCallback(true);
		                    }
		                });
			    		return true;
		            }
		        });
			} else if (belowApi16) {
				child.setTranslationX(child.getWidth());
	    		child.setAlpha(0);
	    		child.setTranslationY(0);
	    		child.animate()
	    		.setDuration(MOVE_DURATION)
	    		.translationX(0)
	    		.alpha(1)
	    		.setInterpolator(interpolator)
	    		.setListener(new AnimatorListener() {
					@Override
					public void onAnimationStart(android.animation.Animator animation) { }
					@Override
					public void onAnimationEnd(android.animation.Animator animation) { 
						listView.setEnabled(true);
	    				callbacks.addButtonSetEnabledCallback(true);
					}
					@Override
					public void onAnimationCancel(android.animation.Animator animation) { }
					@Override
					public void onAnimationRepeat(android.animation.Animator animation) { }
                });
			} else {
				child.setTranslationX(child.getWidth());
	    		child.setAlpha(0);
	    		child.setTranslationY(0);
	    		child.animate()
	    		.setDuration(MOVE_DURATION)
	    		.translationX(0)
	    		.alpha(1)
	    		.setInterpolator(interpolator)
	    		.withEndAction(new Runnable() {
	    			public void run() {
	    				listView.setEnabled(true);
	    				callbacks.addButtonSetEnabledCallback(true);
	    			}
	    		});
			}*/
		} else if (pos < firstVisiblePosition) {
			// animate in child 0 from above after others have moved down. This is not the newly added item child.
			final ViewTreeObserver observer = listView.getViewTreeObserver();
	        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
	            public boolean onPreDraw() {
	                observer.removeOnPreDrawListener(this);
        			final View child0 = listView.getChildAt(0);
					
        			setPreAnimationParameters(child0, -(child0.getHeight() + listView.getDividerHeight()), Axis.Y);
        			animateAxis(child0, 0, Axis.Y);
        			
        			// SHOULD this be surrounded by an if statement? Want to avoid running the same method code many times.
        			afterFirstAddition(child0);        			 
        			 
        			/*if (belowApi12) {
						setTranslationY(child0, -(child0.getHeight() + listView.getDividerHeight()));
						ViewPropertyAnimator.animate(child0)
						.setDuration(MOVE_DURATION)
						.translationY(0)
						.setListener(new AnimatorListenerAdapter() {
		                    @Override
		                    public void onAnimationEnd(Animator animation) {
		                    	listView.setEnabled(true);
			    				callbacks.addButtonSetEnabledCallback(true);
		                    }
		                });
					} else if (belowApi16) {
						child0.setTranslationY(-(child0.getHeight() + listView.getDividerHeight()));
						child0.animate()
						.setDuration(MOVE_DURATION)
						// .translationY(translationY)
						.translationY(0)
						.setListener(new AnimatorListener() {
							@Override
							public void onAnimationStart(android.animation.Animator animation) { }
							@Override
							public void onAnimationEnd(android.animation.Animator animation) { 
								listView.setEnabled(true);
			    				callbacks.addButtonSetEnabledCallback(true);
							}
							@Override
							public void onAnimationCancel(android.animation.Animator animation) { }
							@Override
							public void onAnimationRepeat(android.animation.Animator animation) { }
		                });
					} else {
						child0.setTranslationY(-(child0.getHeight() + listView.getDividerHeight()));
						child0.animate()
						.setDuration(MOVE_DURATION)
						// .translationY(translationY)
						.translationY(0)
						.withEndAction(new Runnable() {
							@Override
							public void run() {
								listView.setEnabled(true);
								callbacks.addButtonSetEnabledCallback(true);
							}
						});
					}*/
					return true;
	            }
	        });
		} else {
			afterAdditionAnimation();
		}
    }
	
}















