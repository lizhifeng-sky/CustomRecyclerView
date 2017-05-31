package lzf.custom.recyclerview.base;

import lzf.custom.recyclerview.CustomRefreshLayout;

/**
 * Created by Administrator on 2017/5/27 0027.
 */
/*
* 布局操作接口
* */
public interface LayoutOperationListener {
    /**
     * 下拉中
     *
     * @param refreshLayout
     * @param distance
     */
    void onPullingDown(CustomRefreshLayout refreshLayout, float distance);

    /**
     * 上拉
     */
    void onPullingUp(CustomRefreshLayout refreshLayout, float distance);

    /**
     * 下拉松开
     *
     * @param refreshLayout
     * @param distance
     */
    void onPullDownReleasing(CustomRefreshLayout refreshLayout, float distance);

    /**
     * 上拉松开
     */
    void onPullUpReleasing(CustomRefreshLayout refreshLayout, float distance);

    /**
     * 刷新中。。。
     */
    void onRefresh(CustomRefreshLayout refreshLayout);

    /**
     * 加载更多中
     */
    void onLoadMore(CustomRefreshLayout refreshLayout);

    /**
     * 手动调用finishRefresh或者finishLoadMore之后的回调
     */
    void onFinishRefresh();

    void onFinishLoadMore();
}
