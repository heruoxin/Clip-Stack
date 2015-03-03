package com.catchingnow.tinyclipboardmanager;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;


public class ActivityEditor extends MyActionBarActivity {

    private String oldText;
    private EditText editText;
    private boolean isStarred;
    private boolean textStatueHasChanged = false;
    private MenuItem starItem;
    private InputMethodManager inputMethodManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);
        Intent intent = getIntent();
        oldText = intent.getStringExtra(Intent.EXTRA_TEXT);
        isStarred = intent.getBooleanExtra(ClipObjectActionBridge.STATUE_IS_STARRED, false);
        if (oldText == null || oldText.equals(getString(R.string.clip_notification_single_text))) {
            oldText = "";
        }

        editText = (EditText) findViewById(R.id.edit_text);
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

        // if is copied form other application.
        if (Intent.ACTION_SEND.equals(intent.getAction()) && "text/plain".equals(intent.getType())) {
            oldText = "";
        }

        //set activity title and icon.
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        String titleText = getString(R.string.title_activity_activity_editor);
        if (oldText.isEmpty()) {
            titleText = getString(R.string.title_activity_editor);
        }
        getSupportActionBar().setTitle(titleText);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setTaskDescription(new ActivityManager.TaskDescription(
                    titleText,
                    BitmapFactory.decodeResource(getResources(), R.drawable.icon),
                    getResources().getColor(R.color.primary)
            ));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        editText.requestFocus();
        CBWatcherService.startCBService(this, false, 1);
    }

    @Override
    protected void onPause() {
        super.onPause();
        inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(editText.getWindowToken(), 0);
        CBWatcherService.startCBService(this, false, -1);
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
                isStarred = !isStarred;
                //Once click will change it. Twice won't change it.
                textStatueHasChanged = !textStatueHasChanged;
                setStarredIcon();
                break;
//            case (R.id.action_save):
//                saveText();
//                break;
            case (R.id.action_delete):
                deleteText();
                break;
            case (R.id.action_cancel):
            case (android.R.id.home):
                finishAndRemoveTaskWithToast(getString(R.string.toast_no_saved));
        }
        return super.onOptionsItemSelected(item);
    }

    private void setStarredIcon() {
        if (isStarred) {
            starItem.setIcon(R.drawable.ic_action_star_white);
        } else {
            starItem.setIcon(R.drawable.ic_action_star_outline_white);
        }
    }

    private void deleteText() {
        Storage db = Storage.getInstance(this);
        db.modifyClip(oldText, null, Storage.MAIN_ACTIVITY_VIEW);
        finishAndRemoveTaskWithToast(getString(R.string.toast_deleted));
    }

    private void shareText() {
        String text = editText.getText().toString();
        Intent openIntent = new Intent(this, ClipObjectActionBridge.class);
        openIntent.putExtra(ClipObjectActionBridge.CLIPBOARD_STRING, text);
        openIntent.putExtra(ClipObjectActionBridge.CLIPBOARD_ACTION, ClipObjectActionBridge.ACTION_SHARE);
        startService(openIntent);
    }

    private void saveText() {
        String newText = editText.getText().toString();
        String toastMessage;
        if (!oldText.equals(newText)) {
            textStatueHasChanged = true;
        }
        if (!textStatueHasChanged) {
            finishAndRemoveTaskWithToast(getString(R.string.toast_no_saved));
            return;
        }
        Storage db = Storage.getInstance(this);
        db.modifyClip(oldText, newText, Storage.MAIN_ACTIVITY_VIEW, (isStarred? 1:-1));
        if (newText != null && !newText.isEmpty()) {
            toastMessage = getString(R.string.toast_saved);
        } else {
            toastMessage = getString(R.string.toast_deleted);
        }
        finishAndRemoveTaskWithToast(toastMessage);
    }

    public void saveTextOnClick(View view) {
        saveText();
    }

    private void finishAndRemoveTaskWithToast(String toastMessage) {
        Toast.makeText(this,
                toastMessage,
                Toast.LENGTH_SHORT).show();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            finish();
        } else {
            finishAndRemoveTask();
        }
    }

}
