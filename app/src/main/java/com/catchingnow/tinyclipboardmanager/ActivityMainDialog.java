package com.catchingnow.tinyclipboardmanager;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.View;

/**
 * Created by heruoxin on 15/3/27.
 */

public class ActivityMainDialog extends ActivityMain {
    public static final String DIALOG_CLOSED = "dialog_closed";
    public static final String DIALOG_OPENED = "dialog_opened";

    @Override
    public void setContentView(int layoutResID) {
        if (layoutResID == R.layout.activity_main) {
            super.setContentView(R.layout.activity_main_dialog);
        } else {
            super.setContentView(layoutResID);
        }
    }

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
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(DIALOG_CLOSED));
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
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(DIALOG_OPENED));
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
