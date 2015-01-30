package com.catchingnow.tinyclipboards;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;


public class ActivityEditor extends ActionBarActivity {

    private String oldText;
    private EditText editText;
    private InputMethodManager inputMethodManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);
        Intent intent = getIntent();
        oldText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (oldText == null) {
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

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        editText.requestFocus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }

    @Override
    public void onBackPressed() {
        finishAndRemoveTaskWithToast();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
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
            case (R.id.action_save):
                saveText();
                break;
            case (R.id.action_cancel):
            case (android.R.id.home):
                finishAndRemoveTaskWithToast();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    private void shareText() {
        String text = editText.getText().toString();
        Intent openIntent = new Intent(this, StringActionIntentService.class);
        openIntent.putExtra(StringActionIntentService.CLIPBOARD_STRING, text);
        openIntent.putExtra(StringActionIntentService.CLIPBOARD_ACTION, StringActionIntentService.ACTION_SHARE);
        startService(openIntent);
    }

    private void saveText() {
        String newText = editText.getText().toString();
        if (oldText.equals(newText)) {
            finishAndRemoveTaskWithToast();
            return;
        }
        Storage db = new Storage(this);
        db.deleteClipHistory(oldText);
        db.close();
        if (newText != null && !"".equals(newText)) {
            ClipboardManager cb = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            cb.setText(newText);
        } else {
            Toast.makeText(this,
                    getString(R.string.toast_deleted),
                    Toast.LENGTH_SHORT).show();
        }
        finishAndRemoveTask();
    }

    private void finishAndRemoveTaskWithToast() {
        Toast.makeText(this,
                "Not saved.",
                Toast.LENGTH_SHORT).show();
        finishAndRemoveTask();
    }

}
