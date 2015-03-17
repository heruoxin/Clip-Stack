package com.catchingnow.tinyclipboardmanager;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


public class ActivityBackup extends MyActionBarActivity {
    private Context context;
    private Activity activity;
    private View newBackupView;
    private LinearLayout backupView;

    private AlertDialog alertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_backup);
        super.onCreate(savedInstanceState);
        context = this;
        activity = this;
        newBackupView = findViewById(R.id.new_backup);
        backupView = (LinearLayout) findViewById(R.id.backup_list);
        Button buttonNewBackup = (Button) newBackupView;
        buttonNewBackup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ActivityOptions options = ActivityOptions
                            .makeSceneTransitionAnimation(activity,
                                    newBackupView,
                                    getString(R.string.action_export));
                    startActivity(new Intent(context, ActivityBackupNew.class), options.toBundle());
                } else {
                    startActivity(new Intent(context, ActivityBackupNew.class));
                }
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        initImportView(false);
    }

    private void initImportView(boolean showToast) {

        backupView.removeAllViewsInLayout();
        LayoutInflater layoutInflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        File[] backupFiles = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String filename) {
                        return (
                                filename.startsWith(getString(R.string.backup_file_name_old).trim())
                                        || filename.startsWith(getString(R.string.backup_file_name).trim())
                        );
                    }
                });

        if (showToast) {
            Toast.makeText(context,
                    getString(
                            R.string.toast_find_backup,
                            backupFiles.length+"",
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()+""
                    ),
                    Toast.LENGTH_LONG
            ).show();
        }

        ArrayList<BackupObject> backupObjects = new ArrayList<>();
        for (File backupFile : backupFiles) {
            BackupObject backupObject = new BackupObject(this);
            if (backupObject.init(backupFile)) {
                backupObjects.add(backupObject);
            }
        }
        for (final BackupObject backupObject : backupObjects) {
            View backupListView = layoutInflater.inflate(R.layout.activity_backup_card, null);
            TextView dateView = (TextView) backupListView.findViewById(R.id.date);
            TextView sizeView = (TextView) backupListView.findViewById(R.id.size);
            ImageButton deleteButton = (ImageButton) backupListView.findViewById(R.id.action_delete);
            View backupContent = backupListView.findViewById(R.id.backup_content);
            backupContent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    alertDialog = new AlertDialog.Builder(context)
                            .setTitle(R.string.action_import)
                            .setMessage(R.string.dialog_description_are_you_sure)
                            .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    alertDialog.dismiss();
                                    final ProgressDialog progressDialog = ProgressDialog.show(context, "",
                                            getString(R.string.progress_importing), true);
                                    new Timer().schedule(new TimerTask() {
                                        @Override
                                        public void run() {
                                            backupObject.makeImport();
                                            progressDialog.dismiss();
                                            finish();
                                        }
                                    }, 100);
                                }
                            })
                            .setNegativeButton(R.string.dialog_cancel, null)
                            .create();
                    alertDialog.show();
                }
            });
            backupContent.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    v.playSoundEffect(0);
                    backupObject.openInEditor();
                    return true;
                }
            });
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(context)
                            .setTitle(R.string.action_delete)
                            .setMessage(R.string.dialog_description_are_you_sure)
                            .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    backupObject.delete();
                                    initImportView(false);
                                }
                            })
                            .setNegativeButton(R.string.dialog_cancel, null)
                            .create()
                            .show();
                }
            });
            dateView.setText(backupObject.getBackupDateString());
            sizeView.setText(backupObject.getBackupSize());
            backupView.addView(backupListView);
        }

        if (backupView.getChildCount() == 0) {
            backupView.addView(layoutInflater.inflate(R.layout.activity_backup_card_empty, null));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_backup, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.action_refresh:
                initImportView(true);
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
