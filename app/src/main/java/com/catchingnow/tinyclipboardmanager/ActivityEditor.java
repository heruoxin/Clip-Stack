package com.catchingnow.tinyclipboardmanager;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;


public class ActivityEditor extends MyActionBarActivity {

    private String oldText;
    private EditText editText;
    private boolean isStarred;
    private MenuItem starItem;
    private ImageButton mFAB;
    private Toolbar mToolbar;
    private InputMethodManager inputMethodManager;
    private Storage db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_editor);
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        oldText = intent.getStringExtra(Intent.EXTRA_TEXT);
        isStarred = intent.getBooleanExtra(ClipObjectActionBridge.STATUE_IS_STARRED, false);
        if (oldText == null || oldText.equals(getString(R.string.clip_notification_single_text))) {
            oldText = "";
        }

        editText = (EditText) findViewById(R.id.edit_text);
        mFAB = (ImageButton) findViewById(R.id.main_fab);
        mToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        editText.setText(oldText);
        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                }
            }
        });

        db = Storage.getInstance(this);
        // if is copied form other application.
        if (Intent.ACTION_SEND.equals(intent.getAction()) && "text/plain".equals(intent.getType())) {
            oldText = "";
        }

        String titleText = getString(R.string.title_activity_activity_editor);
        if (oldText.isEmpty()) {
            titleText = getString(R.string.title_activity_editor);
        }
        mToolbar.setLogo(R.drawable.ic_action_edit);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setTitle(titleText);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setTaskDescription(new ActivityManager.TaskDescription(
                    titleText + ": " + MyUtil.stringLengthCut(oldText, 4),
                    BitmapFactory.decodeResource(getResources(), R.drawable.ic_action_edit),
                    getResources().getColor(R.color.primary)
            ));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        editText.requestFocus();
        setStarredIcon();
    }

    @Override
    protected void onPause() {
        super.onPause();
        inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }

    @Override
    public void onBackPressed() {
        finishAndRemoveTaskWithToast(getString(R.string.toast_no_saved));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        starItem = menu.findItem(R.id.action_star);
        setStarredIcon();
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case (R.id.action_share):
                shareText();
                break;
            case (R.id.action_star):
                //Once click will change it. Twice won't change it.
                isStarred = !isStarred;
                setStarredIcon();
                break;
            case (R.id.action_delete):
                deleteText();
                break;
            case (R.id.action_cancel):
            case (android.R.id.home):
                finishAndRemoveTaskWithToast(getString(R.string.toast_no_saved));
                break;
            case (R.id.action_save):
                saveText();
        }
        return super.onOptionsItemSelected(item);
    }

    private void setStarredIcon() {
        if (starItem == null) return;
        final TransitionDrawable mFabBackground = (TransitionDrawable) mFAB.getBackground();
        if (isStarred) {
            starItem.setIcon(R.drawable.ic_action_star_white);
        } else {
            starItem.setIcon(R.drawable.ic_action_star_outline_white);
        }
        mFAB.animate().scaleX(0).setDuration(160);
        mFAB.animate().scaleY(0);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isStarred) {
                    mFAB.setImageResource(R.drawable.ic_action_star_white);
                    mFabBackground.startTransition((int) mFAB.animate().getDuration());
                } else {
                    mFAB.setImageResource(R.drawable.ic_action_copy);
                    mFabBackground.resetTransition();
                }
                mFAB.animate().scaleX(1);
                mFAB.animate().scaleY(1);
            }
        }, 220);
    }

    private void deleteText() {
        db.modifyClip(oldText, null);
        finishAndRemoveTaskWithToast(getString(R.string.toast_deleted));
    }

    private void shareText() {
        String text = editText.getText().toString();
        Intent openIntent = new Intent(this, ClipObjectActionBridge.class);
        openIntent.putExtra(Intent.EXTRA_TEXT, text);
        openIntent.putExtra(ClipObjectActionBridge.ACTION_CODE, ClipObjectActionBridge.ACTION_SHARE);
        startService(openIntent);
    }

    private void saveText() {
        String newText = editText.getText().toString();
        String toastMessage;
        db.modifyClip(oldText, newText, (isStarred ? 1 : -1));
        if (newText != null && !newText.isEmpty()) {
            toastMessage = getString(R.string.toast_copied, newText + "\n");
        } else {
            toastMessage = getString(R.string.toast_deleted);
        }
        finishAndRemoveTaskWithToast(toastMessage);
    }

    public void saveTextOnClick(View view) {
        saveText();
    }

    private void finishAndRemoveTaskWithToast(String toastMessage) {
        Toast
                .makeText(
                        this,
                        toastMessage,
                        Toast.LENGTH_SHORT
                )
                .show();
        db.updateSystemClipboard();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            finish();
        } else {
            finishAndRemoveTask();
        }
    }

}
