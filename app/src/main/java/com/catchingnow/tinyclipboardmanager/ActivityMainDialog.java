package com.catchingnow.tinyclipboardmanager;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

/**
 * Created by heruoxin on 15/3/27.
 */

public class ActivityMainDialog extends ActivityMain {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        findViewById(R.id.main_fab).setVisibility(View.GONE);
        mToolbar.setNavigationIcon(R.drawable.ic_stat_icon);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(context, ActivityMain.class)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                );
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean returnInt = super.onCreateOptionsMenu(menu);
        menu.removeGroup(R.id.menu_group);
        return returnInt;
    }

    @Override
    protected void onPause() {
        super.onPause();
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                    finish();
//            }
//        }, 200);
    }

    @Override
    protected void onResume() {
        super.onResume();
        isStarred = false;
        setView();
    }

    @Override
    protected void setStarredIcon() {
        if (isStarred) {
            starItem.setIcon(R.drawable.ic_action_star_white);
        } else {
            starItem.setIcon(R.drawable.ic_action_star_outline_white);
        }
    }
}
