package lzf.custom.recyclerview.proxy;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.RelativeLayout;

import lzf.custom.recyclerview.CustomRefreshLayout;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * Created by Administrator on 2017/5/27 0027.
 */
public class AnimationProxy {
    private DecelerateInterpolator decelerateInterpolator;//动画的变化率
    private static final float animFraction = 1f;
    private CustomRefreshLayout.CustomRefreshLayoutProxy customRefreshLayoutProxy;

    public AnimationProxy(CustomRefreshLayout.CustomRefreshLayoutProxy customRefreshLayoutProxy) {
        this.customRefreshLayoutProxy = customRefreshLayoutProxy;
        decelerateInterpolator = new DecelerateInterpolator();
    }

    //下拉刷新
    public void scrollHeaderByMove(float moveY) {
        if (!customRefreshLayoutProxy.enableRefresh()) {
            //如果不开启刷新
            customRefreshLayoutProxy.getHeader().setVisibility(View.GONE);
        }
        float offsetY = decelerateInterpolator
                .getInterpolation(moveY / customRefreshLayoutProxy.getMaxHeadHeight() / 2) * moveY / 2;
        if (customRefreshLayoutProxy.getHeader().getVisibility() != View.VISIBLE) {
            customRefreshLayoutProxy.getHeader().setVisibility(View.VISIBLE);
        }
        customRefreshLayoutProxy.getHeader().getLayoutParams().height = (int) Math.abs(offsetY);
        customRefreshLayoutProxy.getHeader().requestLayout();

        customRefreshLayoutProxy.getContent().setTranslationY(offsetY);
        customRefreshLayoutProxy.onPullingDown(offsetY);
    }

    //上拉加载
    public void scrollBottomByMove(float moveY) {
        float offsetY = decelerateInterpolator
                .getInterpolation(moveY / customRefreshLayoutProxy.getBottomHeight() / 2)
                * moveY / 2;
        if (!customRefreshLayoutProxy.enableLoadMore()) {
            //如果不开启加载
            customRefreshLayoutProxy.getBottom().setVisibility(View.GONE);
            return;
        }
        if (customRefreshLayoutProxy.getBottom().getVisibility() != VISIBLE)
            customRefreshLayoutProxy.getBottom().setVisibility(VISIBLE);

        customRefreshLayoutProxy.getBottom().getLayoutParams().height = (int) Math.abs(offsetY);
        customRefreshLayoutProxy.getBottom().requestLayout();

        customRefreshLayoutProxy.getContent().setTranslationY(-offsetY);
        customRefreshLayoutProxy.onPullingUp(-offsetY);
    }

    public void dealPullDownRelease() {
        if (getVisibleHeadHeight() >= customRefreshLayoutProxy.getHeadHeight() -
                customRefreshLayoutProxy.getMinSwipeDistance()) {
            animHeadToRefresh();
        } else {
            animHeadBack();
        }
    }

    public void dealPullUpRelease() {
        if (getVisibleBottomHeight() >= customRefreshLayoutProxy.getBottomHeight() -
                customRefreshLayoutProxy.getMinSwipeDistance()) {
            animBottomToLoad();
        } else {
            animBottomBack();
        }
    }

    private boolean isAnimHeadToRefresh = false;

    /**
     * 1.满足进入刷新的条件或者主动刷新时，把Head位移到刷新位置（当前位置 ~ HeadHeight）
     */
    public void animHeadToRefresh() {
        isAnimHeadToRefresh = true;
        animLayoutByTime(getVisibleHeadHeight(),
                customRefreshLayoutProxy.getHeadHeight(),
                animHeadUpListener, new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        isAnimHeadToRefresh = false;
                        customRefreshLayoutProxy.setRefreshing(true);
                        customRefreshLayoutProxy.onRefresh();
                    }
                });
    }

    private boolean isAnimHeadBack = false;

    /**
     * 2.动画结束或不满足进入刷新状态的条件，收起头部（当前位置 ~ 0）
     */
    public void animHeadBack() {
        isAnimHeadBack = true;
        animLayoutByTime(getVisibleHeadHeight(), 0, animHeadUpListener, new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isAnimHeadBack = false;
            }
        });
    }

    private boolean isAnimBottomToLoad = false;

    /**
     * 3.满足进入加载更多的条件或者主动加载更多时，把Footer移到加载更多位置（当前位置 ~ BottomHeight）
     */
    public void animBottomToLoad() {
        isAnimBottomToLoad = true;
        animLayoutByTime(getVisibleBottomHeight(),
                customRefreshLayoutProxy.getBottomHeight(),
                animBottomUpListener,
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        isAnimBottomToLoad = false;
                        customRefreshLayoutProxy.setLoadingMore(true);
                        customRefreshLayoutProxy.onLoadMore();
                    }
                });
    }

    private boolean isAnimBottomBack = false;

    /**
     * 4.加载更多完成或者不满足进入加载更多模式的条件时，收起尾部（当前位置 ~ 0）
     */
    public void animBottomBack() {
        isAnimBottomBack = true;
        Log.e("lzf_anim", customRefreshLayoutProxy.getChildHeight() + "  " + getVisibleBottomHeight());
        animLayoutByTime(getVisibleBottomHeight(), 0, 0, animBottomUpListener, new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isAnimBottomBack = false;
            }
        });
    }


    private boolean isAnimHeadHide = false;

    /**
     * 5.当刷新处于可见状态，向上滑动屏幕时，隐藏刷新控件
     *
     * @param vy 手指向上滑动速度
     */
    public void animHeadHideByVy(int vy) {
        isAnimHeadHide = true;
        vy = Math.abs(vy);
        if (vy < 5000) vy = 8000;
        animLayoutByTime(getVisibleHeadHeight(), 0, 5 * Math.abs(getVisibleHeadHeight() * 1000 / vy), animHeadUpListener, new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isAnimHeadHide = false;
                customRefreshLayoutProxy.resetHeaderView();
            }
        });
    }

    private boolean isAnimBottomHide = false;

    /**
     * 6.当加载更多处于可见状态时，向下滑动屏幕，隐藏加载更多控件
     *
     * @param vy 手指向下滑动的速度
     */
    public void animBottomHideByVy(int vy) {
        isAnimBottomHide = true;
        vy = Math.abs(vy);
        if (vy < 5000) vy = 8000;
        animLayoutByTime(getVisibleBottomHeight(), 0, 5 * getVisibleBottomHeight() * 1000 / vy, animBottomUpListener, new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isAnimBottomHide = false;
                customRefreshLayoutProxy.resetBottomView();
            }
        });
    }

    private boolean isAnimOsTop = false;

    /**
     * 7.执行顶部越界  To executive cross-border springback at the top.
     * 越界高度height ∝ vy/computeTimes，此处采用的模型是 height=A*(vy + B)/computeTimes
     *
     * @param vy           满足越界条件的手指滑动速度  the finger sliding speed on the screen.
     * @param computeTimes 从满足条件到滚动到顶部总共计算的次数 Calculation times from sliding to top.
     */
    public void animOverScrollTop(float vy, int computeTimes) {
        if (!isAnimOsTop) return;
        isAnimOsTop = true;
        customRefreshLayoutProxy.setState(CustomRefreshLayout.CustomRefreshLayoutProxy.FROM_TOP_TO_BOTTOM);
        int oh = (int) Math.abs(vy / computeTimes / 2);
        final int overHeight = oh > customRefreshLayoutProxy.getMaxHeadHeight() ? (int) customRefreshLayoutProxy.getMaxHeadHeight() : oh;
        final int time = overHeight <= 50 ? 115 : (int) (0.3 * overHeight + 100);
        animLayoutByTime(0, overHeight, time, overScrollTopUpListener, new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                animLayoutByTime(overHeight, 0, 2 * time, overScrollTopUpListener, new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        isAnimOsTop = false;
                    }
                });
            }
        });
    }

    private boolean isAnimOsBottom = false;

    /**
     * 8.执行底部越界
     *
     * @param vy           满足越界条件的手指滑动速度
     * @param computeTimes 从满足条件到滚动到顶部总共计算的次数
     */
    public void animOverScrollBottom(float vy, int computeTimes) {
        if (!isAnimOsBottom) return;
        isAnimOsBottom = true;
        customRefreshLayoutProxy.setState(CustomRefreshLayout.CustomRefreshLayoutProxy.FROM_BOTTOM_TO_TOP);
        int oh = (int) Math.abs(vy / computeTimes / 2);
        final int overHeight = oh > customRefreshLayoutProxy.getMaxHeadHeight() ? (int) customRefreshLayoutProxy.getMaxHeadHeight() : oh;
        final int time = overHeight <= 50 ? 115 : (int) (0.3 * overHeight + 100);
        animLayoutByTime(0, overHeight, time, overScrollBottomUpListener, new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                animLayoutByTime(overHeight, 0, 2 * time, overScrollBottomUpListener, new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        isAnimOsBottom = false;
                    }
                });
            }
        });
    }

    private ValueAnimator.AnimatorUpdateListener overScrollTopUpListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            int height = (int) animation.getAnimatedValue();
            customRefreshLayoutProxy.getHeader().getLayoutParams().height = height;
            customRefreshLayoutProxy.getHeader().requestLayout();
            customRefreshLayoutProxy.getContent().setTranslationY(height);
            customRefreshLayoutProxy.onPullDownReleasing(height);
        }
    };

    private ValueAnimator.AnimatorUpdateListener overScrollBottomUpListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            int height = (int) animation.getAnimatedValue();
            customRefreshLayoutProxy.getBottom().getLayoutParams().height = height;
            customRefreshLayoutProxy.getBottom().requestLayout();
            customRefreshLayoutProxy.getContent().setTranslationY(-height);
            customRefreshLayoutProxy.onPullUpReleasing(height);
        }
    };


    private ValueAnimator.AnimatorUpdateListener animHeadUpListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            int height = (int) animation.getAnimatedValue();
            customRefreshLayoutProxy.getHeader().getLayoutParams().height = height;
            customRefreshLayoutProxy.getHeader().requestLayout();
            customRefreshLayoutProxy.getContent().setTranslationY(height);
            customRefreshLayoutProxy.onPullDownReleasing(height);
        }
    };

    private ValueAnimator.AnimatorUpdateListener animBottomUpListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            Log.e("lzf_anim_update", customRefreshLayoutProxy.getChildHeight() + "  " + getVisibleBottomHeight());
            int height = (int) animation.getAnimatedValue();
            if (getVisibleBottomHeight() == 0) {
                RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                layoutParams.setMargins(0, 0, 0, 0);
                customRefreshLayoutProxy.getChild().setLayoutParams(layoutParams);
                customRefreshLayoutProxy.getChild().requestLayout();
                customRefreshLayoutProxy.getBottom().setVisibility(GONE);
                customRefreshLayoutProxy.getContent().requestLayout();
            } else {
                customRefreshLayoutProxy.getBottom().getLayoutParams().height = height;
                customRefreshLayoutProxy.getBottom().requestLayout();
                customRefreshLayoutProxy.onPullUpReleasing(height);
            }
        }
    };

    private int getVisibleHeadHeight() {
        return customRefreshLayoutProxy.getHeader().getLayoutParams().height;
    }

    private int getVisibleBottomHeight() {
        return customRefreshLayoutProxy.getBottom().getLayoutParams().height;
    }

    public void animLayoutByTime(int start, int end, long time, ValueAnimator.AnimatorUpdateListener listener, Animator.AnimatorListener animatorListener) {
        ValueAnimator va = ValueAnimator.ofInt(start, end);
        va.setInterpolator(new DecelerateInterpolator());
        va.addUpdateListener(listener);
        va.addListener(animatorListener);
        va.setDuration(time);
        va.start();
    }

    public void animLayoutByTime(int start, int end, ValueAnimator.AnimatorUpdateListener listener, Animator.AnimatorListener animatorListener) {
        ValueAnimator va = ValueAnimator.ofInt(start, end);
        va.setInterpolator(new DecelerateInterpolator());
        va.addUpdateListener(listener);
        va.addListener(animatorListener);
        va.setDuration((int) (Math.abs(start - end) * animFraction));
        va.start();
    }
}
