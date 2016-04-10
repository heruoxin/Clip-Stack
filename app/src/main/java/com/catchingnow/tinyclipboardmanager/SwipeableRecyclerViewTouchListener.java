/*
 * Copyright 2013 Google Inc.
 * Copyright 2015 Bruno Romeu Nunes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.catchingnow.tinyclipboardmanager;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * A {@link View.OnTouchListener} that makes the list items in a {@link android.support.v7.widget.RecyclerView}
 * dismissable by swiping.
 * <p/>
 * <p>Example usage:</p>
 * <p/>
 * <pre>
 * SwipeDismissRecyclerViewTouchListener touchListener =
 *         new SwipeDismissRecyclerViewTouchListener(
 *                 listView,
 *                 new SwipeDismissRecyclerViewTouchListener.OnDismissCallback() {
 *                     public void onDismiss(ListView listView, int[] reverseSortedPositions) {
 *                         for (int position : reverseSortedPositions) {
 *                             adapter.remove(adapter.getItem(position));
 *                         }
 *                         adapter.notifyDataSetChanged();
 *                     }
 *                 });
 * listView.setOnTouchListener(touchListener);
 * listView.setOnScrollListener(touchListener.makeScrollListener());
 * </pre>
 * <p/>
 * <p>This class Requires API level 12 or later due to use of {@link
 * android.view.ViewPropertyAnimator}.</p>
 */
public class SwipeableRecyclerViewTouchListener implements RecyclerView.OnItemTouchListener {
    // Cached ViewConfiguration and system-wide constant values
    private int mSlop;
    private int mMinFlingVelocity;
    private int mMaxFlingVelocity;
    private long ANIMATION_FAST = 300;
    private long ANIMATION_WAIT = 2200;

    // Fixed properties
    private RecyclerView mRecyclerView;
    private SwipeListener mSwipeListener;
    private int mViewWidth = 1; // 1 and not 0 to prevent dividing by zero

    // Transient properties
    private List<PendingDismissData> mPendingDismisses = new ArrayList<>();
    private int mDismissAnimationRefCount = 0;
    private float mDownX;
    private float mDownY;
    private boolean mSwiping;
    private int mSwipingSlop;
    private VelocityTracker mVelocityTracker;
    private int mDownPosition;
    private View mDownView;
    private boolean mPaused;
    private float mFinalDelta;

    // Foreground view (to be swiped)
    // background view (to show)
    private View mFgView;
    private View mBgView;

    //view ID
    private int mFgID;
    private int mBgID;

    // Added by MehrunesTenets
    Context context;
    String clipText;

    /**
     * Constructs a new swipe touch listener for the given {@link android.support.v7.widget.RecyclerView}
     *
     * @param recyclerView The recycler view whose items should be dismissable by swiping.
     * @param listener     The listener for the swipe events.
     */
    public SwipeableRecyclerViewTouchListener(
            Context context,
            RecyclerView recyclerView,
            int fgID,
            int BgID,
            SwipeListener listener) {
        mFgID = fgID;
        mBgID = BgID;
        ViewConfiguration vc = ViewConfiguration.get(recyclerView.getContext());
        mSlop = vc.getScaledTouchSlop();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity() * 16;
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        mRecyclerView = recyclerView;
        mSwipeListener = listener;
        this.context = context;


        /**
         * This will ensure that this SwipeableRecyclerViewTouchListener is paused during list view scrolling.
         * If a scroll listener is already assigned, the caller should still pass scroll changes through
         * to this listener.
         */
        mRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                setEnabled(newState != RecyclerView.SCROLL_STATE_DRAGGING);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            }
        });
    }

    /*Added by 401*/
    public SwipeableRecyclerViewTouchListener(
            Context context,
            RecyclerView recyclerView,
            int fgID,
            int BgID,
            SwipeListener listener,
            String clipText) {
        mFgID = fgID;
        mBgID = BgID;
        ViewConfiguration vc = ViewConfiguration.get(recyclerView.getContext());
        mSlop = vc.getScaledTouchSlop();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity() * 16;
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        mRecyclerView = recyclerView;
        mSwipeListener = listener;
        this.context = context;
        this.clipText = clipText;


        /**
         * This will ensure that this SwipeableRecyclerViewTouchListener is paused during list view scrolling.
         * If a scroll listener is already assigned, the caller should still pass scroll changes through
         * to this listener.
         */
        mRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                setEnabled(newState != RecyclerView.SCROLL_STATE_DRAGGING);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            }
        });
    }


    /////////////////////////

    /**
     * Enables or disables (pauses or resumes) watching for swipe-to-dismiss gestures.
     *
     * @param enabled Whether or not to watch for gestures.
     */
    public void setEnabled(boolean enabled) {
        mPaused = !enabled;
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent motionEvent) {
        return handleTouchEvent(motionEvent);
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent motionEvent) {
        handleTouchEvent(motionEvent);
    }

    private boolean handleTouchEvent(MotionEvent motionEvent) {
        if (mViewWidth < 2) {
            mViewWidth = mRecyclerView.getWidth();
        }

        switch (motionEvent.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                if (mPaused) {
                    break;
                }

                // Find the child view that was touched (perform a hit test)
                Rect rect = new Rect();
                int childCount = mRecyclerView.getChildCount();
                int[] listViewCoords = new int[2];
                mRecyclerView.getLocationOnScreen(listViewCoords);
                int x = (int) motionEvent.getRawX() - listViewCoords[0];
                int y = (int) motionEvent.getRawY() - listViewCoords[1];
                View child;
                for (int i = 0; i < childCount; i++) {
                    child = mRecyclerView.getChildAt(i);
                    child.getHitRect(rect);
                    if (rect.contains(x, y)) {
                        mDownView = child;
                        break;
                    }
                }

                if (mDownView != null) {
                    mDownX = motionEvent.getRawX();
                    mDownY = motionEvent.getRawY();
                    mDownPosition = mRecyclerView.getChildPosition(mDownView);
                    mVelocityTracker = VelocityTracker.obtain();
                    mVelocityTracker.addMovement(motionEvent);
                    mFgView = mDownView.findViewById(mFgID);
                }
                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                if (mVelocityTracker == null) {
                    break;
                }

                if (mDownView != null && mSwiping) {
                    // cancel
                    mFgView.animate()
                            .translationX(0)
                            .setDuration(ANIMATION_FAST)
                            .setListener(null);
                }
                mVelocityTracker.recycle();
                mVelocityTracker = null;
                mDownX = 0;
                mDownY = 0;
                mDownView = null;
                mDownPosition = ListView.INVALID_POSITION;
                mSwiping = false;
                mBgView = null;
                break;
            }

            case MotionEvent.ACTION_UP: {
                if (mVelocityTracker == null) {
                    break;
                }

                mFinalDelta = motionEvent.getRawX() - mDownX;
                mVelocityTracker.addMovement(motionEvent);
                mVelocityTracker.computeCurrentVelocity(1000);
                float velocityX = mVelocityTracker.getXVelocity();
                float absVelocityX = Math.abs(velocityX);
                float absVelocityY = Math.abs(mVelocityTracker.getYVelocity());
                boolean dismiss = false;
                boolean dismissRight = false;
                if (Math.abs(mFinalDelta) > mViewWidth / 2 && mSwiping) {
                    // Add if statement to run our own code - mehrunestenets
                    if (mFinalDelta > 0)    {
                        Intent openIntent = new Intent(context, ClipObjectActionBridge.class)
                                .putExtra(ClipObjectActionBridge.ACTION_CODE, ClipObjectActionBridge.ACTION_COMMENT)
                                .putExtra(Intent.EXTRA_TEXT, clipText);
                        context.startService(openIntent);
                    } else {
                        dismiss = true;
                        dismissRight = mFinalDelta > 0;
                    }
                } else if (mMinFlingVelocity <= absVelocityX && absVelocityX <= mMaxFlingVelocity
                        && absVelocityY < absVelocityX && mSwiping) {
                    // dismiss only if flinging in the same direction as dragging
                    if (mFinalDelta > 0)    {
                        Intent openIntent = new Intent(context, ClipObjectActionBridge.class)
                                .putExtra(ClipObjectActionBridge.ACTION_CODE, ClipObjectActionBridge.ACTION_COMMENT);
                        context.startService(openIntent);
                    } else {
                        dismiss = (velocityX < 0) == (mFinalDelta < 0);
                        dismissRight = mVelocityTracker.getXVelocity() > 0;
                    }
                }
                if (
                        dismiss &&
                                mDownPosition != ListView.INVALID_POSITION &&
                                mSwipeListener.canSwipe(mDownPosition)
                        ) {
                    // dismiss
                    final View downView = mDownView; // mDownView gets null'd before animation ends
                    final int downPosition = mDownPosition;
                    ++mDismissAnimationRefCount;
                    mBgView.animate()
                            .alpha(1)
                            .setDuration(ANIMATION_FAST);
                    mFgView.animate()
                            .translationX(dismissRight ? mViewWidth : -mViewWidth)
                            .setDuration(ANIMATION_FAST)
                            .setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    performDismiss(downView, downPosition);
                                }
                            });
                } else {
                    // cancel
                    mFgView.animate()
                            .translationX(0)
                            .setDuration(ANIMATION_FAST)
                            .setListener(null);
                }
                mVelocityTracker.recycle();
                mVelocityTracker = null;
                mDownX = 0;
                mDownY = 0;
                mDownView = null;
                mDownPosition = ListView.INVALID_POSITION;
                mSwiping = false;
                mBgView = null;
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (mVelocityTracker == null || mPaused) {
                    break;
                }

                mVelocityTracker.addMovement(motionEvent);
                float deltaX = motionEvent.getRawX() - mDownX;
                float deltaY = motionEvent.getRawY() - mDownY;
                if (!mSwiping && Math.abs(deltaX) > mSlop && Math.abs(deltaY) < Math.abs(deltaX) / 2) {
                    mSwiping = true;
                    mSwipingSlop = (deltaX > 0 ? mSlop : -mSlop);
                }

                if (mSwiping) {
                    if (mBgView == null) {
                        mBgView = mDownView.findViewById(mBgID);
                        mBgView.setVisibility(View.VISIBLE);
                    }
                    mFgView.setTranslationX(deltaX - mSwipingSlop);
                    mBgView.setAlpha(1 - Math.max(0f, Math.min(1f,
                            1f - Math.abs(deltaX) / mViewWidth)));
                    return true;
                }
                break;
            }
        }

        return false;
    }

    private void performDismiss(final View dismissView, final int dismissPosition) {
        // Animate the dismissed list item to zero-height and fire the dismiss callback when
        // all dismissed list item animations have completed. This triggers layout on each animation
        // frame; in the future we may want to do something smarter and more performant.

        final View backgroundView = dismissView.findViewById(mBgID);
        final ViewGroup.LayoutParams lp = dismissView.getLayoutParams();
        final int originalHeight = dismissView.getHeight();
        final boolean[] deleteAble = {true};

        final ValueAnimator animator = ValueAnimator.ofInt(originalHeight, 1).setDuration(ANIMATION_FAST);

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                --mDismissAnimationRefCount;

                if (mDismissAnimationRefCount > 0) return;

                mDismissAnimationRefCount = 0;
                // No active animations, process all pending dismisses.
                // Sort by descending position
                Collections.sort(mPendingDismisses);

                int[] dismissPositions = new int[mPendingDismisses.size()];
                for (int i = mPendingDismisses.size() - 1; i >= 0; i--) {
                    dismissPositions[i] = mPendingDismisses.get(i).position;
                }
                mSwipeListener.onDismissedBySwipe(mRecyclerView, dismissPositions);

                // Reset mDownPosition to avoid MotionEvent.ACTION_UP trying to start a dismiss
                // animation with a stale position
                mDownPosition = ListView.INVALID_POSITION;

                ViewGroup.LayoutParams lp;
                for (PendingDismissData pendingDismiss : mPendingDismisses) {
                    // Reset view presentation
                    pendingDismiss.view.findViewById(mFgID).setTranslationX(0);
                    lp = pendingDismiss.view.getLayoutParams();
                    lp.height = originalHeight;
                    pendingDismiss.view.setLayoutParams(lp);
                }

                // Send a cancel event
                long time = SystemClock.uptimeMillis();
                MotionEvent cancelEvent = MotionEvent.obtain(time, time,
                        MotionEvent.ACTION_CANCEL, 0, 0, 0);
                mRecyclerView.dispatchTouchEvent(cancelEvent);

                mPendingDismisses.clear();
            }
        });

        // Animate the dismissed list item to zero-height
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                lp.height = (Integer) valueAnimator.getAnimatedValue();
                dismissView.setLayoutParams(lp);
            }
        });

        final PendingDismissData pendingDismissData = new PendingDismissData(dismissPosition, dismissView);
        mPendingDismisses.add(pendingDismissData);

        //fade out background view
        backgroundView.animate()
                .alpha(0).setDuration(ANIMATION_WAIT)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (deleteAble[0]) animator.start();
                    }
                });

        //cancel animate when click(actually touch) background view.
        backgroundView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        deleteAble[0] = false;
                        --mDismissAnimationRefCount;
                        mPendingDismisses.remove(pendingDismissData);
                        backgroundView.playSoundEffect(0);
                        backgroundView.setOnTouchListener(null);
                }
                return false;
            }
        });
    }

    /**
     * The callback interface used by {@link SwipeableRecyclerViewTouchListener} to inform its client
     * about a swipe of one or more list item positions.
     */
    public interface SwipeListener {
        /**
         * Called to determine whether the given position can be swiped.
         */
        boolean canSwipe(int position);

        /**
         * Called when the item has been dismissed by swiping to the left.
         *
         * @param recyclerView           The originating {@link android.support.v7.widget.RecyclerView}.
         * @param reverseSortedPositions An array of positions to dismiss, sorted in descending
         *                               order for convenience.
         */
        void onDismissedBySwipe(RecyclerView recyclerView, int[] reverseSortedPositions);

    }

    class PendingDismissData implements Comparable<PendingDismissData> {
        public int position;
        public View view;

        public PendingDismissData(int position, View view) {
            this.position = position;
            this.view = view;
        }

        @Override
        public int compareTo(@NonNull PendingDismissData other) {
            // Sort by descending position
            return other.position - position;
        }
    }
}