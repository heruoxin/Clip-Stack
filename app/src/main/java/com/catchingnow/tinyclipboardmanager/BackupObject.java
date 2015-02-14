package com.catchingnow.tinyclipboardmanager;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by heruoxin on 15/2/14.
 */
public class BackupObject {
    private Date backupDate;
    private int backupSize;
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
            backupSize = Integer.parseInt(String.valueOf(backupFile.length() / 1024));

            return true;
        } catch (Error error) {
            return false;
        }
    }

    public int getBackupSize() {
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
                    dateOne =  new SimpleDateFormat(localTimeFormat).parse(line);
                    if (clipString.endsWith("\n")) {
                        clipString = clipString.substring(0, clipString.length()-1);
                    }
                    if (!"".equals(clipString)) {
                        if (dateTwo != null) {
                            clipObjects.add(new ClipObject(clipString, dateTwo));
                            clipString = "";
                        }
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
}
