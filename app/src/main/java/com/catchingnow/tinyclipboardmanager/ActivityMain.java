package com.catchingnow.tinyclipboardmanager;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.List;


public class ActivityMain extends ActionBarActivity {
    public final static String PACKAGE_NAME = "com.catchingnow.tinyclipboardmanager";
    public final static String EXTRA_QUERY_TEXT = "com.catchingnow.tinyclipboard.EXTRA.queryText";
    public final static String FIRST_LAUNCH = "pref_is_first_launch";
    private String queryText;
    private RecyclerView recList;
    private Storage db;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.drawable.icon_shadow);
        context = this.getBaseContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CBWatcherService.startCBService(context, false);
        } else {
            CBWatcherService.startCBService(context, false, true);
        }
        queryText = "";
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        CBWatcherService.startCBService(context, false, -1);
    }

    @Override
    protected void onResume() {
        setView(queryText);
        CBWatcherService.startCBService(context, true, 1);
        super.onResume();
        //check if first launch
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(this);
        if (preference.getBoolean(FIRST_LAUNCH, true)) {
            try {
                firstLaunch();
                preference.edit()
                        .putBoolean(FIRST_LAUNCH, false)
                        .apply();
            } catch (InterruptedException e) {
                Log.e(PACKAGE_NAME, "first launch error:");
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (intent.hasExtra(EXTRA_QUERY_TEXT)) {
            String s = intent.getStringExtra(EXTRA_QUERY_TEXT);
            if (s != null) {
                if (!"".equals(s)) {
                    queryText = s;
                }
            }
            setView(queryText);
        }
        super.onNewIntent(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        final MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                searchView.setIconified(false);
                searchView.requestFocus();
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                searchView.clearFocus();
                return true;
            }
        });
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                searchItem.collapseActionView();
                queryText = null;
                setView(null);
                return false;
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                queryText = newText;
                setView(queryText);
                return true;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_search:
                return super.onOptionsItemSelected(item);
            case R.id.action_refresh:
                setView(queryText);
                return super.onOptionsItemSelected(item);
            case R.id.action_settings:
                startActivity(new Intent(this, ActivitySetting.class));
                return super.onOptionsItemSelected(item);
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    public static void refreshMainView(Context context, String query) {
        Intent intent = new Intent(context, ActivityMain.class)
                .putExtra(EXTRA_QUERY_TEXT, query)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private void setView(String query) {
        //get clips
        db = Storage.getInstance(this);
        final List<ClipObject> clips = db.getClipHistory(query);

        setContentView(R.layout.activity_main);
        recList = (RecyclerView) findViewById(R.id.cardList);
        recList.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        recList.setLayoutManager(llm);
        final ClipCardAdapter ca = new ClipCardAdapter(clips, this);
        recList.setAdapter(ca);

        SwipeDismissRecycleViewTouchListener touchListener =
                new SwipeDismissRecycleViewTouchListener(
                        recList,
                        new SwipeDismissRecycleViewTouchListener.DismissCallbacks() {
                            @Override
                            public boolean canDismiss(int position) {
                                return true;
                            }

                            @Override
                            public void onDismiss(RecyclerView recyclerView, int[] reverseSortedPositions) {
                                for (int position : reverseSortedPositions) {

                                    db.modifyClip(clips.get(position).getText(), null, Storage.MAIN_ACTIVITY_VIEW);
                                    clips.remove(position);
                                    Toast.makeText(context,
                                            getString(R.string.toast_deleted),
                                            Toast.LENGTH_SHORT).show();
                                }
                                // do not call notifyItemRemoved for every item, it will cause gaps on deleting items
                                ca.notifyDataSetChanged();
                            }
                        });
        recList.setOnTouchListener(touchListener);
        // Setting this scroll listener is required to ensure that during ListView scrolling,
        // we don't look for swipes.
        recList.setOnScrollListener(touchListener.makeScrollListener());
    }

    private void firstLaunch() throws InterruptedException {
        db = Storage.getInstance(this);
        //db.modifyClip(null, getString(R.string.first_launch_clips_3, "👈", "😇"));
        db.modifyClip(null, getString(R.string.first_launch_clips_3, "👈", ""), Storage.SYSTEM_CLIPBOARD);
        Thread.sleep(50);
        db.modifyClip(null, getString(R.string.first_launch_clips_2, "🙋"), Storage.SYSTEM_CLIPBOARD);
        Thread.sleep(50);
        db.modifyClip(null, getString(R.string.first_launch_clips_1, "😄"), Storage.SYSTEM_CLIPBOARD);
        Thread.sleep(50);
        db.modifyClip(null, getString(R.string.first_launch_clips_0, "😄"), Storage.SYSTEM_CLIPBOARD);
    }

    public void actionAdd(View view) {
        Intent i = new Intent(this, StringActionIntentService.class)
                .putExtra(StringActionIntentService.CLIPBOARD_ACTION, StringActionIntentService.ACTION_EDIT);
        startService(i);
    }

    public class ClipCardAdapter extends RecyclerView.Adapter<ClipCardAdapter.ClipCardViewHolder> {
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
            clipCardViewHolder.vDate.setText(sdfDate.format(clipObject.getDate()));
            clipCardViewHolder.vTime.setText(sdfTime.format(clipObject.getDate()));
            clipCardViewHolder.vText.setText(clipObject.getText().trim());
            addClickStringAction(context, clipObject.getText(), StringActionIntentService.ACTION_EDIT, clipCardViewHolder.vText);
            addLongClickStringAction(context, clipObject.getText(), StringActionIntentService.ACTION_COPY, clipCardViewHolder.vText);
            addClickStringAction(context, clipObject.getText(), StringActionIntentService.ACTION_SHARE, clipCardViewHolder.vShare);
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
                    Intent openIntent = new Intent(context, StringActionIntentService.class)
                            .putExtra(StringActionIntentService.CLIPBOARD_STRING, string)
                            .putExtra(StringActionIntentService.CLIPBOARD_ACTION, actionCode);
                    context.startService(openIntent);
                    refreshMainView();
                }
            });
        }

        public void addLongClickStringAction(final Context context, final String string, final int actionCode, View button) {
            button.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Intent openIntent = new Intent(context, StringActionIntentService.class)
                            .putExtra(StringActionIntentService.CLIPBOARD_STRING, string)
                            .putExtra(StringActionIntentService.CLIPBOARD_ACTION, actionCode);
                    context.startService(openIntent);
                    refreshMainView();
                    return false;
                }
            });
        }

        private void refreshMainView() {
            Intent i = new Intent(context, ActivityMain.class);
            context.startActivity(i);
        }

        public class ClipCardViewHolder extends RecyclerView.ViewHolder {
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
}
