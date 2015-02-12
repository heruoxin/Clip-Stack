package com.catchingnow.tinyclipboardmanager;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.TextView;

import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.listeners.ActionClickListener;
import com.nispok.snackbar.listeners.EventListener;

import java.text.SimpleDateFormat;
import java.util.List;


public class ActivityMain extends ActionBarActivity {
    public final static String PACKAGE_NAME = "com.catchingnow.tinyclipboardmanager";
    public final static String EXTRA_QUERY_TEXT = "com.catchingnow.tinyclipboard.EXTRA.queryText";
    public final static String EXTRA_IS_FROM_NOTIFICATION = "com.catchingnow.tinyclipboard.EXTRA.isFromNotification";
    public final static String FIRST_LAUNCH = "pref_is_first_launch";
    private String queryText;
    private RecyclerView recList;
    private ImageButton mFAB;
    private Storage db;
    private Context context;
    private int isSnackbarShow = 0;
    private boolean isFromNotification = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.drawable.icon_shadow);
        context = this.getBaseContext();
        queryText = "";
    }

    @Override
    protected void onStop() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CBWatcherService.startCBService(context, false, true);
        }
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
        Log.v(PACKAGE_NAME, "ActivityMain onResume");
        CBWatcherService.startCBService(context, true, Storage.NOTIFICATION_VIEW);
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
        isFromNotification = intent.getBooleanExtra(EXTRA_IS_FROM_NOTIFICATION, false);
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
//            case R.id.action_refresh:
//                setView(queryText);
//                return super.onOptionsItemSelected(item);
            case R.id.action_export:
                startActivity(new Intent(context, ActivityExport.class));
                return super.onOptionsItemSelected(item);
            case R.id.action_delete_all:
                clearAll();
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

        //set view
        setContentView(R.layout.activity_main);
        mFAB = (ImageButton) findViewById(R.id.main_fab);
        recList = (RecyclerView) findViewById(R.id.cardList);
        recList.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recList.setLayoutManager(linearLayoutManager);
        final ClipCardAdapter clipCardAdapter = new ClipCardAdapter(clips, this);
        recList.setAdapter(clipCardAdapter);

        SwipeableRecyclerViewTouchListener touchListener =
                new SwipeableRecyclerViewTouchListener(
                        recList,
                        new SwipeableRecyclerViewTouchListener.SwipeListener() {
                            @Override
                            public boolean canSwipe(int position) {
                                return true;
                            }

                            @Override
                            public void onDismissedBySwipeLeft(RecyclerView recyclerView, int[] reverseSortedPositions) {
                                for (int position : reverseSortedPositions) {
                                    showSnackbar(position, clips.get(position), clipCardAdapter);
                                    clipCardAdapter.remove(position);
                                }
                                clipCardAdapter.notifyDataSetChanged();
                            }

                            @Override
                            public void onDismissedBySwipeRight(RecyclerView recyclerView, int[] reverseSortedPositions) {
                                onDismissedBySwipeLeft(recyclerView, reverseSortedPositions);
                            }
                        });
        recList.addOnItemTouchListener(touchListener);

    }

    private void showSnackbar(final int position, final ClipObject clipObject, final ClipCardAdapter clipCardAdapter) {
        final boolean[] isUndo = new boolean[1];
        SnackbarManager.show(
                Snackbar.with(getApplicationContext())
                        .text(getString(R.string.toast_deleted))
                        .actionLabel(getString(R.string.toast_undo))
                        .actionColor(getResources().getColor(R.color.accent))
                        .duration(Snackbar.SnackbarDuration.LENGTH_SHORT)
                        .eventListener(new EventListener() {
                            @Override
                            public void onShow(Snackbar snackbar) {
                                mFAB.animate().translationY(-snackbar.getHeight());
                            }

                            @Override
                            public void onShown(Snackbar snackbar) {
                                isSnackbarShow += 1;
                            }

                            @Override
                            public void onDismiss(Snackbar snackbar) {
                                isSnackbarShow -= 1;
                            }

                            @Override
                            public void onDismissed(Snackbar snackbar) {
                                if (isSnackbarShow <= 0) {
                                    isSnackbarShow = 0;
                                    mFAB.animate().translationY(0);
                                }
                                if (isUndo[0]) {
                                    return;
                                }
                                db.modifyClip(clipObject.getText(), null, Storage.MAIN_ACTIVITY_VIEW);
                            }
                        })
                        .actionListener(new ActionClickListener() {
                            @Override
                            public void onActionClicked(Snackbar snackbar) {
                                isUndo[0] = true;
                                clipCardAdapter.add(position, clipObject);
                            }
                        })
                , this);
    }

    private void firstLaunch() throws InterruptedException {
        db = Storage.getInstance(this);
        //db.modifyClip(null, getString(R.string.first_launch_clips_3, "👈", "😇"));
        db.modifyClip(null, getString(R.string.first_launch_clips_3, "", "👉"), Storage.SYSTEM_CLIPBOARD);
        Thread.sleep(50);
        db.modifyClip(null, getString(R.string.first_launch_clips_2, "🙋"), Storage.SYSTEM_CLIPBOARD);
        Thread.sleep(50);
        db.modifyClip(null, getString(R.string.first_launch_clips_1, "😄"), Storage.SYSTEM_CLIPBOARD);
        Thread.sleep(50);
        db.modifyClip(null, getString(R.string.first_launch_clips_0, "😄"), Storage.SYSTEM_CLIPBOARD);
    }

    private void clearAll() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.action_delete_all)
                .setMessage(getString(R.string.dialog_delete_all))
                .setPositiveButton(getString(R.string.dialog_ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                db.deleteAllClipHistory();
                            }
                        }
                )
                .setNegativeButton(getString(R.string.dialog_cancel), null)
                .create()
                .show();
    }

    public void actionAdd(View view) {
        Intent i = new Intent(this, StringActionIntentService.class)
                .putExtra(StringActionIntentService.CLIPBOARD_ACTION, StringActionIntentService.ACTION_EDIT);
        startService(i);
    }

    public class ClipCardAdapter extends RecyclerView.Adapter<ClipCardAdapter.ClipCardViewHolder> {
        private Context context;
        private List<ClipObject> clipObjectList;
        private View mView;
        public SimpleDateFormat sdfDate;
        public SimpleDateFormat sdfTime;

        public ClipCardAdapter(List<ClipObject> clipObjectList, Context context) {
            this.context = context;
            this.clipObjectList = clipObjectList;
            sdfDate = new SimpleDateFormat(context.getString(R.string.date_format));
            sdfTime = new SimpleDateFormat(context.getString(R.string.time_format));
        }

        @Override
        public int getItemCount() {
            return clipObjectList.size();
        }

        @Override
        public void onBindViewHolder(ClipCardViewHolder clipCardViewHolder, int i) {
            final ClipObject clipObject = clipObjectList.get(i);
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

        public void add(int position, ClipObject clipObject) {
            clipObjectList.add(position, clipObject);
            notifyItemInserted(position);
        }

        public void remove(ClipObject clipObject) {
            int position = clipObjectList.indexOf(clipObject);
            remove(position);
        }

        public void remove(int position) {
            clipObjectList.remove(position);
            notifyItemRemoved(position);
        }

        public void addClickStringAction(final Context context, final String string, final int actionCode, View button) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent openIntent = new Intent(context, StringActionIntentService.class)
                            .putExtra(StringActionIntentService.CLIPBOARD_STRING, string)
                            .putExtra(StringActionIntentService.CLIPBOARD_ACTION, actionCode);
                    context.startService(openIntent);
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
                    if (isFromNotification) {
                        finish();
                    }
                    return false;
                }
            });
        }

        public class ClipCardViewHolder extends RecyclerView.ViewHolder {
            protected TextView vTime;
            protected TextView vDate;
            protected TextView vText;
            protected View vShare;

            public ClipCardViewHolder(View v) {
                super(v);
                mView = v;
                vTime = (TextView) v.findViewById(R.id.activity_main_card_time);
                vDate = (TextView) v.findViewById(R.id.activity_main_card_date);
                vText = (TextView) v.findViewById(R.id.activity_main_card_text);
                vShare = (View) v.findViewById(R.id.activity_main_card_share_button);
            }
        }

    }
}
