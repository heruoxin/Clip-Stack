package com.catchingnow.tinyclipboards;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Created by heruoxin on 15/1/24.
 */
public class ClipCardAdapter extends RecyclerView.Adapter<ClipCardAdapter.ClipCardViewHolder>{
    private final static String PACKAGE_NAME = "com.catchingnow.tinyclipboards";
    public final static String CLIPBOARD_STRING = "com.catchingnow.tinyclipboards.clipboardString";
    public final static String CLIPBOARD_ACTION = "com.catchingnow.tinyclipboards.clipboarAction";


    private Context context;
    private List<ClipObject> clipObjectList;
    public SimpleDateFormat sdfDate;
    public SimpleDateFormat sdfTime;

    public ClipCardAdapter(List<ClipObject> clipObjectList, Context context) {
        this.clipObjectList = clipObjectList;
        this.context = context;
        sdfDate = new SimpleDateFormat(context.getString(R.string.date_formart));
        sdfTime = new SimpleDateFormat(context.getString(R.string.time_formart));
    }

    @Override
    public int getItemCount() {
        return clipObjectList.size();
    }

    @Override
    public void onBindViewHolder(ClipCardViewHolder clipCardViewHolder, int i) {
        ClipObject clipObject = clipObjectList.get(i);
        clipCardViewHolder.vDate.setText(sdfDate.format(clipObject.date));
        clipCardViewHolder.vTime.setText(sdfTime.format(clipObject.date));
        clipCardViewHolder.vText.setText(clipObject.text.trim());
        addClickStringAction(context, clipObject.text, StringActionIntentService.ACTION_EDIT, clipCardViewHolder.vText);
        addLongClickStringAction(context, clipObject.text, StringActionIntentService.ACTION_COPY, clipCardViewHolder.vText);
        addClickStringAction(context, clipObject.text, StringActionIntentService.ACTION_SHARE, clipCardViewHolder.vShare);
    }

    @Override
    public ClipCardViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.
                from(viewGroup.getContext()).
                inflate(R.layout.activity_main_card_view, viewGroup, false);

        return new ClipCardViewHolder(itemView);
    }

    public void addClickStringAction(final Context context, final String string, final int actionCode, View button) {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent openIntent = new Intent(context, StringActionIntentService.class);
                openIntent.putExtra(CLIPBOARD_STRING, string);
                openIntent.putExtra(CLIPBOARD_ACTION, actionCode);
                context.startService(openIntent);
                refreshMainView();
            }
        });
    }

    public void addLongClickStringAction(final Context context, final String string, final int actionCode, View button) {
        button.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Intent openIntent = new Intent(context, StringActionIntentService.class);
                openIntent.putExtra(CLIPBOARD_STRING, string);
                openIntent.putExtra(CLIPBOARD_ACTION, actionCode);
                context.startService(openIntent);
                refreshMainView();
                return true;
            }
        });
    }

    private void refreshMainView() {
        Intent i = new Intent(context, ActivityMain.class);
        context.startActivity(i);
    }

    public static class ClipCardViewHolder extends RecyclerView.ViewHolder{
        protected TextView vTime;
        protected TextView vDate;
        protected TextView vText;
        protected View vShare;

        public ClipCardViewHolder(View v) {
            super(v);
            vTime = (TextView) v.findViewById(R.id.activity_main_card_time);
            vDate = (TextView) v.findViewById(R.id.activity_main_card_date);
            vText = (TextView) v.findViewById(R.id.activity_main_card_text);
            vShare = (View) v.findViewById(R.id.activity_main_card_share_button);
        }
    }


}
