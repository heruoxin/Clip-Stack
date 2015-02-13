package com.catchingnow.tinyclipboardmanager;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.Switch;

import java.util.Calendar;
import java.util.Date;


public class ActivityBackup extends ActionBarActivity {
    private boolean isReverseSort = false;
    private Calendar dateFrom = Calendar.getInstance();
    private Calendar dateTo = Calendar.getInstance();
    private DatePicker datePickerFrom;
    private DatePicker datePickerTo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup);
        initExportView();
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
        if(Export.makeExport(
                this,
                new Date(datePickerFrom.getYear()-1900, datePickerFrom.getMonth(), datePickerFrom.getDayOfMonth()),
                new Date(datePickerTo.getYear()-1900, datePickerTo.getMonth(), datePickerTo.getDayOfMonth()+1),
                isReverseSort
        )) {
            finish();
        }
    }
}
