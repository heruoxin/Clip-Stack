package com.catchingnow.tinyclipboardmanager;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * Created by heruoxin on 15/2/14.
 */
public class BackupObject {
    private Date backupDate;
    private String backupSize;
    private File backupFile;
    private Context context;

    public BackupObject (Context context) {
        this.context = context;
    }
    public boolean init (File backupFile) {
        try {
            String fileName = backupFile.getName();
            this.backupFile = backupFile;
            backupDate = new Date(fileName.replace(context.getString(R.string.backup_file_name), "").replace(".txt", ""));
            backupSize = humanReadableByteCount(backupFile.length(), false);

            return true;
        } catch (Error error) {
            return false;
        }
    }

    public String getBackupSize() {
        return backupSize;
    }

    public Date getBackupDate() {
        return backupDate;
    }

    public void delete() {
        backupFile.delete();
    }

    public boolean makeImport() {
        try {
            ArrayList<ClipObject> clipObjects = new ArrayList<>();

            BufferedReader br = new BufferedReader(new FileReader(backupFile));
            String line;
            String clipString = "";
            Date dateOne;
            Date dateTwo = null;
            while ((line = br.readLine()) != null) {
                try {
                    String localTimeFormat = "E MMM d H:m:s ZZZZ y";
                    dateOne =  new SimpleDateFormat(localTimeFormat, Locale.ENGLISH)
                            .parse(line);
                    if (clipString.endsWith("\n")) {
                        clipString = clipString.substring(0, clipString.length()-1);
                    }
                    if (!"".equals(clipString)) {
                        if (dateTwo != null) {
                            clipObjects.add(new ClipObject(clipString, dateTwo));
                        }
                        clipString = "";
                        dateTwo = dateOne;
                    }
                } catch (ParseException error) {
                    clipString += (line+"\n");
                }
            }
            br.close();
            Storage db = Storage.getInstance(context);
            db.importClips(clipObjects);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void openInEditor() {
        context.startActivity(new Intent()
                .setAction(android.content.Intent.ACTION_VIEW)
                .setDataAndType(Uri.fromFile(backupFile), MimeTypeMap.getSingleton().getMimeTypeFromExtension("txt"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        );
    }

    private static String humanReadableByteCount(long bytes, boolean si) {
        //stackOverflow 3758606
        int unit = si ? 1000: 1024;
        if (bytes < unit) {
            return bytes + "B";
        }
        int exp = (int) (Math.log(bytes)/Math.log(unit));
        String pre = (si?"kMGTPE":"KMGTPE").charAt(exp-1) + (si?"":"i");
        return String.format("%1f%sB",bytes/Math.pow(unit, exp), pre);
    }
}
