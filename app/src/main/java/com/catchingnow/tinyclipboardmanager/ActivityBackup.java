package com.catchingnow.tinyclipboardmanager;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;


public class ActivityBackup extends ActionBarActivity {
    private Context context;
    private boolean isReverseSort = false;
    private Calendar dateFrom = Calendar.getInstance();
    private Calendar dateTo = Calendar.getInstance();
    private DatePicker datePickerFrom;
    private DatePicker datePickerTo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.activity_backup);
        initExportView();
        initImportView();
    }

    private void initImportView() {

        LinearLayout backupView = (LinearLayout) findViewById(R.id.backup_list);
        LayoutInflater layoutInflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        File[] backupFiles = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String filename) {
                        return (filename.startsWith(getString(R.string.backup_file_name)));
                    }
                });
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
            backupListView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final AlertDialog alertDialog = new AlertDialog.Builder(context)
                            .setTitle(R.string.action_import)
                            .setMessage(R.string.dialog_description_are_you_sure)
                            .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ProgressDialog progressDialog = ProgressDialog.show(context, "",
                                            "Loading. Please wait...", true);
                                    backupObject.makeImport();
                                    progressDialog.dismiss();
                                    finish();
                                }
                            })
                            .setNegativeButton(R.string.dialog_cancel, null)
                            .create();
                            alertDialog.show();
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
                                    initImportView();
                                }
                            })
                            .setNegativeButton(R.string.dialog_cancel, null)
                            .create()
                            .show();
                }
            });
            dateView.setText(backupObject.getBackupDate().toString());
            sizeView.setText(backupObject.getBackupSize() + "");
            backupView.addView(backupListView);
        }
    }

    private void initExportView() {
        //get installed date
        dateFrom.set(2015, 2, 1);
        try {
            long installedDate = this.getPackageManager()
                    .getPackageInfo(ActivityMain.PACKAGE_NAME, 0)
                    .firstInstallTime;
            dateFrom.setTimeInMillis(installedDate);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        Button buttonExport = (Button) findViewById(R.id.export_button);
        Switch switchReverseSort = (Switch) findViewById(R.id.switch_reverse_sort);
        datePickerFrom = (DatePicker) findViewById(R.id.date_picker_from);
        datePickerTo = (DatePicker) findViewById(R.id.date_picker_to);
        buttonExport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                export();
            }
        });
        switchReverseSort.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isReverseSort = isChecked;
            }
        });
        datePickerFrom.init(dateFrom.get(Calendar.YEAR), dateFrom.get(Calendar.MONTH), dateFrom.get(Calendar.DAY_OF_MONTH), null);
        datePickerTo.init(dateTo.get(Calendar.YEAR), dateTo.get(Calendar.MONTH), dateTo.get(Calendar.DAY_OF_MONTH), null);
        datePickerFrom.setMinDate(dateFrom.getTimeInMillis());
        datePickerTo.setMinDate(dateFrom.getTimeInMillis());
        datePickerTo.setMaxDate(dateTo.getTimeInMillis());
        datePickerFrom.setMaxDate(dateTo.getTimeInMillis());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_export, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, ActivitySetting.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void export() {
        if (BackupAction.makeExport(
                this,
                new Date(datePickerFrom.getYear() - 1900, datePickerFrom.getMonth(), datePickerFrom.getDayOfMonth()),
                new Date(datePickerTo.getYear() - 1900, datePickerTo.getMonth(), datePickerTo.getDayOfMonth() + 1),
                isReverseSort
        )) {
            finish();
        }
    }
}
