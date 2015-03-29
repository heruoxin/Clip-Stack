package com.catchingnow.tinyclipboardmanager;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Build;
import android.util.Log;

/**
 * Created by heruoxin on 15/1/19.
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class SyncJobService extends JobService {

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log.v(MyUtil.PACKAGE_NAME, "Start Clean up...");
        return Storage.getInstance(this).cleanUpAndRequestBackup();
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
