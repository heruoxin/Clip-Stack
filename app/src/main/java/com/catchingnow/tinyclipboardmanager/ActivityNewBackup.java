package com.catchingnow.tinyclipboardmanager;

import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import android.widget.Switch;

import java.util.Calendar;
import java.util.Date;


public class ActivityNewBackup extends MyActionBarActivity {
    private boolean isReverseSort = false;
    private Calendar dateFrom = Calendar.getInstance();
    private Calendar dateTo = Calendar.getInstance();
    private DatePicker datePickerFrom;
    private DatePicker datePickerTo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_backup);
        initExportView();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_new_backup, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        initExportView();

        //noinspection SimplifiableIfStatement

        return super.onOptionsItemSelected(item);
    }

    private void initExportView() {
        //get installed date
        dateFrom.set(2015, 2, 1);
        try {
            long installedDate = this.getPackageManager()
                    .getPackageInfo(MyUtil.PACKAGE_NAME, 0)
                    .firstInstallTime;
            dateFrom.setTimeInMillis(installedDate);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        while (dateFrom.after(dateTo)) {
            dateFrom.setTimeInMillis(dateFrom.getTimeInMillis() - 70000000);
        }

        //set the start date to the minimum
        dateFrom.set(Calendar.HOUR_OF_DAY, dateFrom.getMinimum(Calendar.HOUR_OF_DAY));
        dateFrom.set(Calendar.MINUTE, dateFrom.getMinimum(Calendar.MINUTE));
        dateFrom.set(Calendar.SECOND, dateFrom.getMinimum(Calendar.SECOND));
        dateFrom.set(Calendar.MILLISECOND, dateFrom.getMinimum(Calendar.MILLISECOND));
        //set the end date to the maximum
        dateTo.set(Calendar.HOUR_OF_DAY, dateTo.getMaximum(Calendar.HOUR_OF_DAY));
        dateTo.set(Calendar.MINUTE, dateTo.getMaximum(Calendar.MINUTE));
        dateTo.set(Calendar.SECOND, dateTo.getMaximum(Calendar.SECOND));
        dateTo.set(Calendar.MILLISECOND, dateTo.getMaximum(Calendar.MILLISECOND));

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

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            datePickerFrom.setCalendarViewShown(false);
            datePickerTo.setCalendarViewShown(false);
        }

        long dateDiff = dateTo.getTimeInMillis() - dateFrom.getTimeInMillis();
        if (dateDiff > 80000000) {
            datePickerFrom.setMinDate(dateFrom.getTimeInMillis());
            datePickerTo.setMinDate(dateFrom.getTimeInMillis());
        }
        datePickerFrom.setMaxDate(dateTo.getTimeInMillis());
        datePickerTo.setMaxDate(dateTo.getTimeInMillis());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            HorizontalScrollView datePickersScrollView = (HorizontalScrollView) findViewById(R.id.date_pickers_scroll_view);
            datePickersScrollView.smoothScrollTo(0, 0);
        } else {
            ScrollView datePickersScrollView = (ScrollView) findViewById(R.id.date_pickers_scroll_view);
            datePickersScrollView.smoothScrollTo(0, 0);
        }

    }


    private void export() {
        if (BackupExport.makeExport(
                this,
                new Date(datePickerFrom.getYear() - 1900, datePickerFrom.getMonth(), datePickerFrom.getDayOfMonth()),
                new Date(datePickerTo.getYear() - 1900, datePickerTo.getMonth(), datePickerTo.getDayOfMonth() + 1),
                isReverseSort
        )) {
            finish();
        }
    }
}
