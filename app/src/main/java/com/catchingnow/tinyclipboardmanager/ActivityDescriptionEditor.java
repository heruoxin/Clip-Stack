package com.catchingnow.tinyclipboardmanager;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;

/**
 * Created by mehrunestenets on 2016-03-22.
 */
public class ActivityDescriptionEditor extends MyActionBarActivity {

    private String oldComment;
    private EditText editLabel;
    private EditText editTags;
    private EditText editComment;
    private ImageButton mFAB;


    @Override
    protected void onCreate(Bundle savedInstanceState)   {
        setContentView(R.layout.activity_comment);
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        oldComment = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (oldComment == null || oldComment.equals(getString(R.string.clip_notification_single_text))) {
            oldComment = "";
        }

        editComment = (EditText) findViewById(R.id.edit_comment);
        editLabel = (EditText) findViewById(R.id.edit_label);
        editTags = (EditText) findViewById(R.id.edit_tags);
        mFAB = (ImageButton) findViewById(R.id.main_fab);
    }
}