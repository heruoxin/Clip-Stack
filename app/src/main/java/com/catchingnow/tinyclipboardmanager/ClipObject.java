package com.catchingnow.tinyclipboardmanager;

import java.util.Date;

/**
 * Created by heruoxin on 15/1/24.
 */
public class ClipObject {
    protected String text;
    protected Date date;
    public ClipObject(String text, Date date) {
        this.text = text;
        this.date = date;
    }
}
