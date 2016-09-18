package com.catchingnow.tinyclipboardmanager;

/**
 * Exception thrown in the event that a clip-object that is requested does not exist in the database
 * Created by 401 on 2016-04-10.
 */
public class ClipDoesNotExistException extends Exception {
    public ClipDoesNotExistException(String s) {
        super("There exists no clip object with contents: " + s);
    }
}
