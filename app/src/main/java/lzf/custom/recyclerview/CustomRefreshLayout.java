package lzf.custom.recyclerview;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import lzf.custom.recyclerview.base.IBottomView;
import lzf.custom.recyclerview.base.IHeadView;
import lzf.custom.recyclerview.base.LayoutOperationListener;
import lzf.custom.recyclerview.base.OnAnimEndListener;
import lzf.custom.recyclerview.base.RefreshLayoutAdapter;
import lzf.custom.recyclerview.proxy.AnimationProxy;
import lzf.custom.recyclerview.proxy.DispatchEventProxy;
import lzf.custom.recyclerview.proxy.OverScrollProxy;

/**
 * Created by Administrator on 2017/5/27 0027.
 */
public class CustomRefreshLayout extends RelativeLayout {
    //    private static final int MAX_Y_OVER_SCROLL_DISTANCE=200;
    private Context mContext;
    private int mMaxYOverScrollDistance = 250;//y轴最大越界距离 超出了即不再滑动
    protected boolean enableOverScroll = true; //是否允许进入越界回弹模式
    private View childView;//子view 要刷新的view
    private FrameLayout headLayout;//头部的布局
    private FrameLayout bottomLayout;//底部的布局
    private IHeadView headView;
    private IBottomView bottomView;
    private float headHeight = 200;//头部的高度
    private float bottomHeight = 200;//底部的高度
    private boolean enableRefresh = true;//是否允许刷新
    private boolean enableLoadMore = true;//是否允许加载
    private CustomRefreshLayoutProxy customRefreshLayoutProxy;//事件处理 动画、越界、事件分发

    public CustomRefreshLayout(Context context) {
        this(context, null);
    }

    public CustomRefreshLayout(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomRefreshLayout(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        if (isInEditMode()) return;
        setPullListener(new CustomRefreshLayoutListener());
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        //添加头部
        if (headLayout == null) {
            FrameLayout headViewLayout = new FrameLayout(getContext());
            LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, 0);
            layoutParams.addRule(ALIGN_PARENT_TOP);
            layoutParams.addRule(CENTER_VERTICAL);
            this.addView(headViewLayout, layoutParams);
            headLayout = headViewLayout;
            if (headView == null) {
                //添加默认的头部
                setHeaderView(null);
            }
        }
        //添加底部
        if (bottomLayout == null) {
            FrameLayout bottomViewLayout = new FrameLayout(getContext());
            LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0);
            layoutParams.addRule(ALIGN_PARENT_BOTTOM);
            layoutParams.addRule(CENTER_VERTICAL);
            bottomViewLayout.setLayoutParams(layoutParams);
            this.addView(bottomLayout, layoutParams);
            bottomLayout = bottomViewLayout;
            if (bottomView == null) {
                //设置默认底部view
                setBottomView(null);
            }
        }
        //获取子控件
        childView=getChildAt(0);

        customRefreshLayoutProxy.init();
    }

    private void setPullListener(CustomRefreshLayoutListener customRefreshLayoutListener) {
        this.customRefreshLayoutListener = customRefreshLayoutListener;
    }

    /*
    * 事件分发
    * */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        boolean intercept = customRefreshLayoutProxy.interceptTouchEvent(event);
        return intercept || super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean resume = customRefreshLayoutProxy.onTouchEvent(event);
        return resume || super.onTouchEvent(event);
    }

    /*
    * 外部回调接口
    *
    * */
    private RefreshLayoutAdapter refreshLayoutAdapter;

    public void setRefreshLayoutAdapter(RefreshLayoutAdapter refreshLayoutAdapter) {
        if (refreshLayoutAdapter != null) {
            this.refreshLayoutAdapter = refreshLayoutAdapter;
        }
    }

    /*
    * 操作监听 及回调
    *
    * */
    private CustomRefreshLayoutListener customRefreshLayoutListener;

    public class CustomRefreshLayoutListener implements LayoutOperationListener{

        @Override
        public void onPullingDown(CustomRefreshLayout refreshLayout, float distance) {
            headView.onPullingDown(distance,mMaxYOverScrollDistance,headHeight);
            if (refreshLayoutAdapter!=null)
                refreshLayoutAdapter.onPullingDown(refreshLayout,distance);
        }

        @Override
        public void onPullingUp(CustomRefreshLayout refreshLayout, float distance) {
            bottomView.onPullingUp(distance,mMaxYOverScrollDistance,bottomHeight);
            if (refreshLayoutAdapter!=null)
                refreshLayoutAdapter.onPullingUp(refreshLayout,distance);
        }

        @Override
        public void onPullDownReleasing(CustomRefreshLayout refreshLayout, float distance) {
            headView.onPullReleasing(distance,mMaxYOverScrollDistance,headHeight);
            if (refreshLayoutAdapter!=null)
                refreshLayoutAdapter.onPullDownReleasing(refreshLayout,distance);
        }

        @Override
        public void onPullUpReleasing(CustomRefreshLayout refreshLayout, float distance) {
            bottomView.onPullReleasing(distance,mMaxYOverScrollDistance,bottomHeight);
            if (refreshLayoutAdapter!=null)
                refreshLayoutAdapter.onPullUpReleasing(refreshLayout,distance);
        }

        @Override
        public void onRefresh(CustomRefreshLayout refreshLayout) {
            headView.startAnim(mMaxYOverScrollDistance,headHeight);
            if (refreshLayoutAdapter!=null)
                refreshLayoutAdapter.onRefresh(refreshLayout);
        }

        @Override
        public void onLoadMore(CustomRefreshLayout refreshLayout) {
            bottomView.startAnim(mMaxYOverScrollDistance,bottomHeight);
            if (refreshLayoutAdapter!=null)
                refreshLayoutAdapter.onLoadMore(refreshLayout);
        }

        @Override
        public void onFinishRefresh() {
            if (enableRefresh){
                headView.onFinish(new OnAnimEndListener() {
                    @Override
                    public void onAnimEnd() {

                    }
                });
            }
        }

        @Override
        public void onFinishLoadMore() {
            if (enableLoadMore) {
                bottomView.onFinish();
            }
        }
    }
    /*
    *
    * 事件代理
    *
    * */
    public class CustomRefreshLayoutProxy {
        private OverScrollProxy overScrollProxy;//越界代理
        private AnimationProxy animationProxy;//动画代理
        private DispatchEventProxy dispatchEventProxy;//事件分发代理
        public final static int FROM_TOP_TO_BOTTOM=1;
        public final static int FROM_BOTTOM_TO_TOP=2;
        private int state=FROM_TOP_TO_BOTTOM;

        public CustomRefreshLayoutProxy() {
            animationProxy=new AnimationProxy(this);
            if (enableOverScroll) {
                overScrollProxy=new OverScrollProxy(this);
            }
            dispatchEventProxy=new DispatchEventProxy(this);
        }

        public void init() {
            if (headLayout!=null) headLayout.setVisibility(GONE);
            if (bottomLayout!=null) bottomLayout.setVisibility(GONE);
        }

        public AnimationProxy getAnimationProxy() {
            return animationProxy;
        }

        public void setState(int state) {
            this.state = state;
        }

        public int getState() {
            return state;
        }

        public boolean enableRefresh(){
            return enableRefresh;
        }

        public boolean enableLoadMore(){
            return enableLoadMore;
        }

        public float getMaxHeadHeight() {
            return mMaxYOverScrollDistance;
        }

        public View getHeader() {
            return headLayout;
        }

        public View getBottom() {
            return bottomLayout;
        }

        public View getContent() {
            return childView;
        }

        /*
        * 操作
        *
        * */
        public void onPullingDown(float offsetY) {
            customRefreshLayoutListener.onPullingDown(CustomRefreshLayout.this, offsetY / headHeight);
        }

        public void onPullingUp(float offsetY) {
            customRefreshLayoutListener.onPullingUp(CustomRefreshLayout.this, offsetY / bottomHeight);
        }

        public void onRefresh() {
            customRefreshLayoutListener.onRefresh(CustomRefreshLayout.this);
        }

        public void onLoadMore() {
            customRefreshLayoutListener.onLoadMore(CustomRefreshLayout.this);
        }

        public void onFinishRefresh() {
            customRefreshLayoutListener.onFinishRefresh();
        }

        public void onFinishLoadMore() {
            customRefreshLayoutListener.onFinishLoadMore();
        }

        public void onPullDownReleasing(float offsetY) {
            customRefreshLayoutListener.onPullDownReleasing(CustomRefreshLayout.this, offsetY / headHeight);
        }

        public void onPullUpReleasing(float offsetY) {
            customRefreshLayoutListener.onPullUpReleasing(CustomRefreshLayout.this, offsetY / bottomHeight);
        }
        /*
        * 事件分发
        * */
        public boolean interceptTouchEvent(MotionEvent ev) {
            return dispatchEventProxy.interceptTouchEvent(ev);
        }

        public boolean onTouchEvent(MotionEvent ev) {
            return dispatchEventProxy.onTouchEvent(ev);
        }
    }
    private void setBottomView(final IBottomView iBottomView) {
        if (iBottomView != null) {
            post(new Runnable() {
                @Override
                public void run() {
                    bottomLayout.removeAllViewsInLayout();
                    bottomLayout.addView(iBottomView.getView());
                }
            });
            bottomView=iBottomView;
        }
    }

    private void setHeaderView(final IHeadView iHeadView) {
        if (iHeadView != null) {
            post(new Runnable() {
                @Override
                public void run() {
                    headLayout.removeAllViewsInLayout();
                    headLayout.addView(iHeadView.getView());
                }
            });
        }
        headView = iHeadView;
    }

}
