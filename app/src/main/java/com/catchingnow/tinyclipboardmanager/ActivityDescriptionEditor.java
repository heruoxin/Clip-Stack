package com.catchingnow.tinyclipboardmanager;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

/**
 * Created by mehrunestenets on 2016-03-22.
 */
public class ActivityDescriptionEditor extends MyActionBarActivity {

    private String oldComment;
    private String oldLabel;
    private String oldTag;
    private EditText editLabel;
    private EditText editTags;
    private EditText editComment;
    private ImageButton mFAB;
    private Storage db;
    private boolean isStarred;


    @Override
    protected void onCreate(Bundle savedInstanceState)   {
        setContentView(R.layout.activity_comment);
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        oldComment = intent.getStringExtra(Intent.EXTRA_TEXT);
        oldLabel = intent.getStringExtra(Intent.EXTRA_TEXT);
        oldTag = intent.getStringExtra(Intent.EXTRA_TEXT);
        isStarred = intent.getBooleanExtra(ClipObjectActionBridge.STATUE_IS_STARRED, false);

        if (oldComment == null || oldComment.equals(getString(R.string.clip_notification_single_text))) {
            oldComment = "";
        }

        editComment = (EditText) findViewById(R.id.edit_comment);
        editLabel = (EditText) findViewById(R.id.edit_label);
        editTags = (EditText) findViewById(R.id.edit_tags);
        mFAB = (ImageButton) findViewById(R.id.main_fab);
        db = Storage.getInstance(this);

    }

    public void saveThreeTextsOnClick(View view) {
        saveText();
    }

    private void saveText(){
        String newComment = editComment.getText().toString();
        String newLabel = editLabel.getText().toString();
        String newTag = editTags.getText().toString();

        String toastMessage;
        db.modifyClip(oldComment, newComment, (isStarred ? 1 : -1));
        if (newComment != null && !newComment.isEmpty()) {
            toastMessage = getString(R.string.toast_copied, newComment + "\n");
        } else {
            toastMessage = getString(R.string.toast_deleted);
        }

        db.modifyClip(oldLabel, newLabel, (isStarred ? 1 : -1));
        if (newLabel != null && !newLabel.isEmpty()) {
            toastMessage = getString(R.string.toast_copied, newLabel + "\n");
        } else {
            toastMessage = getString(R.string.toast_deleted);
        }

        db.modifyClip(oldTag, newTag, (isStarred ? 1 : -1));
        if (newTag != null && !newTag.isEmpty()) {
            toastMessage = getString(R.string.toast_copied, newTag + "\n");
        } else {
            toastMessage = getString(R.string.toast_deleted);
        }
        finishAndRemoveTaskWithToast(toastMessage);
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