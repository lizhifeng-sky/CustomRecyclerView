package lzf.custom.recyclerview.proxy;

import android.view.View;
import android.view.animation.DecelerateInterpolator;

import lzf.custom.recyclerview.CustomRefreshLayout;

/**
 * Created by Administrator on 2017/5/27 0027.
 */
public class AnimationProxy {
    //动画的变化率
    private DecelerateInterpolator decelerateInterpolator;
    private CustomRefreshLayout.CustomRefreshLayoutProxy customRefreshLayoutProxy;
    public AnimationProxy(CustomRefreshLayout.CustomRefreshLayoutProxy customRefreshLayoutProxy) {
        this.customRefreshLayoutProxy=customRefreshLayoutProxy;
        decelerateInterpolator=new DecelerateInterpolator();
    }
    public void scrollHeaderByMove(float moveY){
        if (!customRefreshLayoutProxy.enableRefresh()){
            //如果不开启刷新
            customRefreshLayoutProxy.getHeader().setVisibility(View.GONE);
        }
        float offsetY=decelerateInterpolator
                .getInterpolation(moveY/ customRefreshLayoutProxy.getMaxHeadHeight()/2)*moveY/2;
        if (customRefreshLayoutProxy.getHeader().getVisibility()!= View.VISIBLE){
            customRefreshLayoutProxy.getHeader().setVisibility(View.VISIBLE);
        }
        customRefreshLayoutProxy.getHeader().getLayoutParams().height= (int) Math.abs(offsetY);
        customRefreshLayoutProxy.getHeader().requestLayout();
        customRefreshLayoutProxy.onPullingDown(offsetY);
    }
}
