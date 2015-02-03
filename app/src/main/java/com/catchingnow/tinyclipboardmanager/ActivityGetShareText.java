package com.catchingnow.tinyclipboardmanager;

import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Build;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;


public class ActivityGetShareText extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //receive system's text share
        Intent intent = getIntent();
        if (Intent.ACTION_SEND.equals(intent.getAction()) && "text/plain".equals(intent.getType())) {
            copyText(intent.getStringExtra(Intent.EXTRA_TEXT));
            return;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask();
        } else {
            finish();
        }
    }

    protected void copyText(String clips) {
        if (clips == null) {
            return;
        }
        if ("".equals(clips)) {
            return;
        }
        //copy clips to clipboard
        ClipboardManager cb = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        cb.setText(clips);

        if (clips.length() > 15) {
            clips = clips.substring(0, 15) + "…";
        }
        Toast.makeText(this,
                getString(R.string.toast_front_string) + clips + "\n" + getString(R.string.toast_end_string),
                Toast.LENGTH_LONG
        ).show();

    }

}
