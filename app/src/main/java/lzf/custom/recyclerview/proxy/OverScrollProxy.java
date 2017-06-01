package lzf.custom.recyclerview.proxy;

import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.AbsListView;

import lzf.custom.recyclerview.CustomRefreshLayout;
import lzf.custom.recyclerview.utils.ScrollingUtil;

/**
 * Created by Administrator on 2017/5/27 0027.
 */
public class OverScrollProxy {
    private CustomRefreshLayout.CustomRefreshLayoutProxy customRefreshLayoutProxy;
    protected int OVER_SCROLL_MIN_VX = 3000;//满足越界的手势的最低速度(默认3000)
    private VelocityTracker moveTracker;
    private int mPointerId;
    private float vy;
    private float mVelocityY; //主要为了监测Fling的动作,实现越界回弹
    //针对部分没有OnScrollListener的View的延时策略
    private static final int MSG_START_COMPUTE_SCROLL = 0; //开始计算
    private static final int MSG_CONTINUE_COMPUTE_SCROLL = 1;//继续计算
    private static final int MSG_STOP_COMPUTE_SCROLL = 2; //停止计算

    private int cur_delay_times = 0; //当前计算次数
    private static final int ALL_DELAY_TIMES = 60;  //10ms计算一次,总共计算20次
    private int minSwipeDistance;//最小可识别手势距离

    public OverScrollProxy(CustomRefreshLayout.CustomRefreshLayoutProxy customRefreshLayoutProxy) {
        this.customRefreshLayoutProxy=customRefreshLayoutProxy;
        minSwipeDistance=customRefreshLayoutProxy.getMinSwipeDistance();
    }
    public void init() {
        final View mChildView = customRefreshLayoutProxy.getContent();

        final GestureDetector gestureDetector = new GestureDetector(customRefreshLayoutProxy.getContext(), new GestureDetector.SimpleOnGestureListener() {

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (customRefreshLayoutProxy.isRefreshVisible()
                        && distanceY >= minSwipeDistance
                        ) {
                    customRefreshLayoutProxy.setRefreshing(false);
                    customRefreshLayoutProxy.getAnimationProxy().animHeadHideByVy((int) vy);
                }
                if (customRefreshLayoutProxy.isLoadingVisible()
                        && distanceY <= -minSwipeDistance) {
                    customRefreshLayoutProxy.setLoadingMore(false);
                    customRefreshLayoutProxy.getAnimationProxy().animBottomHideByVy((int) vy);
                }

                return super.onScroll(e1, e2, distanceX, distanceY);
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                mVelocityY = velocityY;
//            if (!(mChildView instanceof AbsListView || mChildView instanceof RecyclerView)) {
                //既不是AbsListView也不是RecyclerView,由于这些没有实现OnScrollListener接口,无法回调状态,只能采用延时策略
//                if (Math.abs(mVelocityY) >= OVER_SCROLL_MIN_VX) {
//                    mHandler.sendEmptyMessage(MSG_START_COMPUTE_SCROLL);
//                } else {
//                    mVelocityY = 0;
//                    cur_delay_times = ALL_DELAY_TIMES;
//                }
//            }
                return false;
            }
        });

        mChildView.setOnTouchListener(new View.OnTouchListener() {
            int mMaxVelocity = ViewConfiguration.get(customRefreshLayoutProxy.getContext()).getScaledMaximumFlingVelocity();
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //手势监听的两个任务：1.监听fling动作，获取速度  2.监听滚动状态变化
                obtainTracker(event);
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mPointerId = event.getPointerId(0);
                        break;
                    case MotionEvent.ACTION_UP:
                        moveTracker.computeCurrentVelocity(1000, mMaxVelocity);
                        vy = moveTracker.getYVelocity(mPointerId);
                        releaseTracker();
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        releaseTracker();
                        break;
                }
                return gestureDetector.onTouchEvent(event);
            }
        });

       if (mChildView instanceof RecyclerView) {
            ((RecyclerView) mChildView).addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        if (mVelocityY >= OVER_SCROLL_MIN_VX && ScrollingUtil.isRecyclerViewToTop((RecyclerView) mChildView)) {
                            customRefreshLayoutProxy.getAnimationProxy().animOverScrollTop(mVelocityY, cur_delay_times);
                            mVelocityY = 0;
                            cur_delay_times = ALL_DELAY_TIMES;
                        }
                        if (mVelocityY <= -OVER_SCROLL_MIN_VX && ScrollingUtil.isRecyclerViewToBottom((RecyclerView) mChildView)) {
                            customRefreshLayoutProxy.getAnimationProxy().animOverScrollBottom(mVelocityY, cur_delay_times);
                            mVelocityY = 0;
                            cur_delay_times = ALL_DELAY_TIMES;
                        }
                    }
                    super.onScrollStateChanged(recyclerView, newState);
                }
            });
        }
    }
    private void obtainTracker(MotionEvent event) {
        if (null == moveTracker) {
            moveTracker = VelocityTracker.obtain();
        }
        moveTracker.addMovement(event);
    }

    private void releaseTracker() {
        if (null != moveTracker) {
            moveTracker.clear();
            moveTracker.recycle();
            moveTracker = null;
        }
    }
}
