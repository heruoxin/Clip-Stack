package com.catchingnow.tinyclipboardmanager;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

/**
 * Created by heruoxin on 15/3/27.
 */

public class ActivityMainDialog extends ActivityMain {

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
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    @Override
    protected void setStarredIcon() {
        super.setStarredIcon();
        if (starItem == null) return;
        if (isStarred) {
            starItem.setIcon(R.drawable.ic_action_star_white);
        } else {
            starItem.setIcon(R.drawable.ic_action_star_outline_white);
        }
    }

    @Override
    protected void addClickStringAction(final Context context, final ClipObject clipObject, final int actionCode, View button) {
        if (button instanceof TextView) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent openIntent = new Intent(context, ClipObjectActionBridge.class)
                            .putExtra(Intent.EXTRA_TEXT, clipObject.getText())
                            .putExtra(ClipObjectActionBridge.STATUE_IS_STARRED, clipObject.isStarred())
                            .putExtra(ClipObjectActionBridge.ACTION_CODE, ClipObjectActionBridge.ACTION_EDIT);
                    context.startService(openIntent);
                }
            });
        } else {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent openIntent = new Intent(context, ClipObjectActionBridge.class)
                            .putExtra(Intent.EXTRA_TEXT, clipObject.getText())
                            .putExtra(ClipObjectActionBridge.STATUE_IS_STARRED, clipObject.isStarred())
                            .putExtra(ClipObjectActionBridge.ACTION_CODE, ClipObjectActionBridge.ACTION_COPY);
                    context.startService(openIntent);
                    moveTaskToBack(true);
                }
            });
        }
    }

    @Override
    protected void addLongClickStringAction(Context context, ClipObject clipObject, int actionCode, View button) {
        //super.addLongClickStringAction(context, clipObject, actionCode, button);
    }

    @Override
    protected void setActionIcon(ImageButton view) {
        view.setImageResource(R.drawable.ic_content_copy_grey);
    }

}
