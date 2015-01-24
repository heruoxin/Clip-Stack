package com.catchingnow.tinyclipboards;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

/**
 * Created by heruoxin on 15/1/24.
 */
public class ClipCardAdapter extends RecyclerView.Adapter<ClipCardAdapter.ClipCardViewHolder>{
    private List<ClipObject> clipObjectList;

    public ClipCardAdapter(List<ClipObject> clipObjectList) {
        this.clipObjectList = clipObjectList;
    }

    @Override
    public int getItemCount() {
        return clipObjectList.size();
    }

    @Override
    public void onBindViewHolder(ClipCardViewHolder clipCardViewHolder, int i) {
        ClipObject clipObject = clipObjectList.get(i);
        clipCardViewHolder.vText.setText(clipObject.text);
        clipCardViewHolder.vTime.setText(clipObject.date.toString());
    }

    @Override
    public ClipCardViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.
                from(viewGroup.getContext()).
                inflate(R.layout.activity_main_card_view, viewGroup, false);

        return new ClipCardViewHolder(itemView);
    }


    public static class ClipCardViewHolder extends RecyclerView.ViewHolder{
        protected TextView vTime;
        protected TextView vText;
        protected ImageButton vShare;
        protected ImageButton vCopy;

        public ClipCardViewHolder(View v) {
            super(v);
            vTime = (TextView) v.findViewById(R.id.activity_main_card_time);
            vText = (TextView) v.findViewById(R.id.activity_main_card_text);
            vShare = (ImageButton) v.findViewById(R.id.activity_main_card_share_button);
            vCopy = (ImageButton) v.findViewById(R.id.activity_main_card_copy_button);
        }
    }


}
