package com.catchingnow.tinyclipboards;

import android.widget.LinearLayout;

/**
 * Created by heruoxin on 14/12/18.
 */
public class ClipListViewCreator {
    LinearLayout layout;
    public ClipListViewCreator () {

    }
    public ClipListViewCreator addClips(String s) {
        return this;
    }
    public LinearLayout build() {
        return layout;
    }
}
