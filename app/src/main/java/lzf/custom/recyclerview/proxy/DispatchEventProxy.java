package lzf.custom.recyclerview.proxy;

import android.view.MotionEvent;

import lzf.custom.recyclerview.CustomRefreshLayout;

/**
 * Created by Administrator on 2017/5/27 0027.
 */
public class DispatchEventProxy {
    private float touchX;
    private float touchY;
    private CustomRefreshLayout.CustomRefreshLayoutProxy customRefreshLayoutProxy;
    public DispatchEventProxy(CustomRefreshLayout.CustomRefreshLayoutProxy customRefreshLayoutProxy) {
        this.customRefreshLayoutProxy=customRefreshLayoutProxy;
    }
    /*
    *
        *  false，则表示将事件放行，
        *  当前 View 上的事件会被传递到子 View 上，
        *  再由子 View 的 dispatchTouchEvent 来开始这个事件的分发
        *
        *  true，则表示将事件进行拦截，
        *  并将拦截到的事件交由当前 View 的 onTouchEvent 进行处理
        *
    *
    * */
    public boolean interceptTouchEvent(MotionEvent event){
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                touchX=event.getX();
                touchY=event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                float dx=event.getX()-touchX;
                float dy=event.getY()-touchY;
                //触发上拉、下拉的最大角度为45度
                if (Math.abs(dx)<=Math.abs(dy)){
                    if (dy>0&&customRefreshLayoutProxy.enableRefresh()){
                        //dy >0  说明手势下拉
                        customRefreshLayoutProxy.setState(CustomRefreshLayout.CustomRefreshLayoutProxy.FROM_TOP_TO_BOTTOM);
                        return true;
                    }else if (dy<0&&customRefreshLayoutProxy.enableLoadMore()){
                        customRefreshLayoutProxy.setState(CustomRefreshLayout.CustomRefreshLayoutProxy.FROM_BOTTOM_TO_TOP);
                        //dy <0  说明手势上拉
                        return true;
                    }
                }
                break;
        }
        return false;
    }
    public boolean onTouchEvent(MotionEvent event){
        switch (event.getAction()){
            case MotionEvent.ACTION_MOVE:
                float dy=event.getY()-touchY;
                if (customRefreshLayoutProxy.getState()== CustomRefreshLayout.CustomRefreshLayoutProxy.FROM_TOP_TO_BOTTOM){
                    dy=Math.min(customRefreshLayoutProxy.getMaxHeadHeight(),dy);
                    dy=Math.max(0,dy);
                    customRefreshLayoutProxy.getAnimationProxy().scrollHeaderByMove(dy);
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                break;
        }
        return false;
    }
}
