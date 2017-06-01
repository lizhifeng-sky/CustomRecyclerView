package lzf.custom.recyclerview;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import lzf.custom.recyclerview.base.RefreshLayoutAdapter;
import lzf.custom.recyclerview.test.MyAdapter;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private MyAdapter adapter;
    private List<String> list;
    private CustomRefreshLayout refreshLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recyclerView= (RecyclerView) findViewById(R.id.recycler);
        refreshLayout= (CustomRefreshLayout) findViewById(R.id.refreshLayout);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        initData();
        adapter=new MyAdapter(this,list);
        setUpRefreshLayout();
        recyclerView.setAdapter(adapter);

    }

    private void setUpRefreshLayout() {
        refreshLayout.setRefreshLayoutAdapter(new RefreshLayoutAdapter(){
            @Override
            public void onRefresh(final CustomRefreshLayout refreshLayout) {
                super.onRefresh(refreshLayout);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.e("lzf_pull", "onRefresh成功");
                        refreshLayout.invalidate();
                        refreshLayout.finishRefreshing();
                    }
                }, 3000);
            }

            @Override
            public void onLoadMore(CustomRefreshLayout refreshLayout) {
                super.onLoadMore(refreshLayout);
            }
        });
    }

    private void initData() {
        list=new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            list.add("第"+i+"项");
        }
    }
}
