package com.catchingnow.tinyclipboardmanager;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by heruoxin on 15/1/24.
 */
public class ClipObject {

    public final static String markStar = "☆★☆";
    public static int clipCount = 0;

    protected String text;
    protected Date date;
    protected boolean star;

    // Added by mehrunestenets
    protected String label;
    protected String comment;
    protected ArrayList<String> tags;
    ///////////////////////////////////

    public ClipObject(String text, Date date) {
       clipCount++;
        this.text = text;
        this.date = date;
        this.star = false;
        this.label = "Clip #" + clipCount;
        this.comment = "Test Comment";
        this.tags = new ArrayList<>();
    }
    public ClipObject(String text, Date date, boolean star) {
        clipCount++; /*Added by 401*/
        this.text = text;
        this.date = date;
        this.star = star;
        this.label = "Clip #" + clipCount; /*Added by 401*/
        this.comment = "Test Comment Star";
        this.tags = new ArrayList<>();
    }
    //Added by 401
    public ClipObject(String text, Date date, boolean star, String comment, String label, ArrayList<String> tags ) {
        this.text = text;
        this.date = date;
        this.star = star;
        this.label = label;
        this.comment = comment;
        this.tags = tags;
    }
    //////////////////////////////////////////////////////
    //Added by 401
    public ClipObject(String text, Date date, boolean star, String comment, String label) {
        this.text = text;
        this.date = date;
        this.star = star;
        this.label = label;
        this.comment = comment;
        //this.tags = tags;
    }
    //////////////////////////////////////////////////////

    public String getText() {
        return text;
    }
    public Date getDate() {
        return date;
    }
    public  boolean isStarred() {
        return star;
    }
    public ClipObject setStarred(boolean isStarred) {
        this.star = isStarred;
        return this;
    }

    // Added by 401
    public String getLabel()    { return label; }
    public String getComment()  { return comment; }
    public ArrayList<String> getTags() { return tags; }
    ///////////////////////////////////////////////////////////
}

