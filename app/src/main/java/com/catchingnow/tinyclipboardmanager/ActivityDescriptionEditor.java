package com.catchingnow.tinyclipboardmanager;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by mehrunestenets on 2016-03-22.
 */
public class ActivityDescriptionEditor extends MyActionBarActivity {

    private String oldText;
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
        oldText = intent.getStringExtra(Intent.EXTRA_TEXT);
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

        editComment.setText(oldComment);
        editLabel.setText(oldLabel);
        editTags.setText(oldTag);

    }

    public void saveThreeTextsOnClick(View view) {
        saveText();
    }

    private void saveText(){
        String newComment = editComment.getText().toString();
        String newLabel = editLabel.getText().toString();
        String newTag = editTags.getText().toString();

        ArrayList<String> newTagArray = new ArrayList<String>(Arrays.asList(newTag.split(" , ")));

        String toastMessage = "Temporary Toast";

        editTags.setText(newTag);
        editLabel.setText(newLabel);
        editComment.setText(newComment);

        db.modifyClipTagsCommentLabel(oldText, newComment, newLabel, newTagArray, (isStarred ? 1 : -1));

        if (newComment!=oldComment || newLabel!=oldLabel || newTag!=oldTag) {
            toastMessage = getString(R.string.toast_updated);
        } else {
            toastMessage = getString(R.string.toast_no_change);
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