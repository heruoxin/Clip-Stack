package com.catchingnow.tinyclipboardmanager;

import android.animation.Animator;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.app.backup.BackupManager;
import android.app.backup.RestoreObserver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.view.MenuItemCompat;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.TextView;

import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.listeners.ActionClickListener;
import com.nispok.snackbar.listeners.EventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;


public class ActivityMain extends MyActionBarActivity {
    public final static String PACKAGE_NAME = "com.catchingnow.tinyclipboardmanager";
    public final static String EXTRA_QUERY_TEXT = "com.catchingnow.tinyclipboard.EXTRA.queryText";
    public final static String EXTRA_IS_FROM_NOTIFICATION = "com.catchingnow.tinyclipboard.EXTRA.isFromNotification";
    public final static String FIRST_LAUNCH = "pref_is_first_launch";
    private RecyclerView mRecList;
    private View mRecLayout;
    private ClipCardAdapter clipCardAdapter;
    private ImageButton mFAB;
    private SearchView searchView;
    private MenuItem searchItem;
    private MenuItem starItem;
    private Storage db;
    private List<ClipObject> clips;
    private Context context;
    private ArrayList<ClipObject> deleteQueue = new ArrayList<>();

    //FAB
    private int isYHidden = -1;
    private int isXHidden = -1;
    private boolean isRotating = false;
    private float defaultX;
    private float defaultY;

    private int isSnackbarShow = 0;
    private boolean isFromNotification = false;
    private boolean isStarred = false;
    private String queryText = "";
    private static int TRANSLATION_MOVE_TIME = 300;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.drawable.icon_shadow);
        getSupportActionBar().setTitle(" " + getString(R.string.title_activity_main));
        context = this.getBaseContext();
        db = Storage.getInstance(context);
        queryText = "";

        //init View
        setContentView(R.layout.activity_main);
        mFAB = (ImageButton) findViewById(R.id.main_fab);
        mRecList = (RecyclerView) findViewById(R.id.cardList);
        mRecLayout = findViewById(R.id.recycler_layout);
        mRecList.setHasFixedSize(true);
        defaultX = mFAB.getX();
        defaultY = mFAB.getY();
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecList.setLayoutManager(linearLayoutManager);

        SwipeableRecyclerViewTouchListener touchListener =
                new SwipeableRecyclerViewTouchListener(
                        mRecList,
                        new SwipeableRecyclerViewTouchListener.SwipeListener() {
                            @Override
                            public boolean canSwipe(int position) {
                                return !clips.get(position).isStarred();
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
        RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 20 && isYHidden == -1) {
                    //hide FAB on Y
                    if (isXHidden == 1) return;
                    isYHidden = 0;
                    mFabRotation(true);
                    mFAB.animate()
                            .translationY(DisplayUtil.dip2px(context, 90))
                            .setDuration(TRANSLATION_MOVE_TIME)
                            .setListener(new Animator.AnimatorListener() {
                                @Override
                                public void onAnimationStart(Animator animation) {

                                }

                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    isYHidden = 1;
                                }

                                @Override
                                public void onAnimationCancel(Animator animation) {

                                }

                                @Override
                                public void onAnimationRepeat(Animator animation) {

                                }
                            });
                } else if (dy < -20 && isYHidden == 1) {
                    //show FAB on Y
                    if (isXHidden == 1) return;
                    isYHidden = 0;
                    mFabRotation(false);
                    mFAB.animate()
                            .translationY(0)
                            .setDuration(TRANSLATION_MOVE_TIME)
                            .setListener(new Animator.AnimatorListener() {
                                @Override
                                public void onAnimationStart(Animator animation) {

                                }

                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    isYHidden = -1;
                                }

                                @Override
                                public void onAnimationCancel(Animator animation) {

                                }

                                @Override
                                public void onAnimationRepeat(Animator animation) {

                                }
                            });
                }
            }
        };
        mRecList.setOnScrollListener(scrollListener);
        mRecList.addOnItemTouchListener(touchListener);

        attachKeyboardListeners();
        onNewIntent(getIntent());

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
        isFromNotification = false;
        clearDeleteQueue();
        CBWatcherService.startCBService(context, false, -1);
    }

    @Override
    protected void onResume() {
        CBWatcherService.startCBService(context, true, Storage.NOTIFICATION_VIEW);
        super.onResume();
        setView();
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

        mFAB.setX(mFAB.getX() + DisplayUtil.px2dip(context, 90));
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mFAB.animate().translationX(0).setDuration(TRANSLATION_MOVE_TIME);
                mFabRotation(false);
            }
        }, 600);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (intent.hasExtra(EXTRA_QUERY_TEXT)) {
            String s = intent.getStringExtra(EXTRA_QUERY_TEXT);
            if (s != null) {
                if (!s.isEmpty()) {
                    queryText = s;
                }
            }
            setView();
        }
        if (intent.getBooleanExtra(EXTRA_IS_FROM_NOTIFICATION, false)) {
            isFromNotification = true;
        }
        super.onNewIntent(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        starItem = menu.findItem(R.id.action_star);
        searchItem = menu.findItem(R.id.action_search);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) searchItem.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                searchView.setIconified(false);
                searchView.requestFocus();
                queryText = searchView.getQuery().toString();
                setView();
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                searchView.clearFocus();
                queryText = null;
                setView();
                return true;
            }
        });
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                searchItem.collapseActionView();
                queryText = null;
                setView();
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
                setView();
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
            case R.id.action_star:
                isStarred = !isStarred;
                mFabRotation(isStarred);
                setStarredIcon();
                setView();
                break;
//            case R.id.action_refresh:
//                setView(queryText);
//                return super.onOptionsItemSelected(item);
            case R.id.action_export:
                startActivity(new Intent(context, ActivityBackup.class));
                break;
            case R.id.action_delete_all:
                clearAll();
                break;
            case R.id.action_settings:
                startActivity(new Intent(this, ActivitySetting.class));
        }
        clearDeleteQueue();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSearchRequested() {
        MenuItemCompat.expandActionView(searchItem);
        searchView.requestFocus();
        return true;
    }

    @Override
    protected void onShowKeyboard(int keyboardHeight) {
        //hide FAB on X.
        if (isXHidden != -1) return;
        isXHidden = 0;
        mFabRotation(true);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mFAB.animate()
                        .translationX(DisplayUtil.dip2px(context, 90))
                        .setDuration(TRANSLATION_MOVE_TIME)
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        isXHidden = 1;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
            }
        }, 200);
    }

    @Override
    protected void onHideKeyboard() {
        //show FAB on X.
        if (isXHidden != 1) return;
        isXHidden = 0;
        if (isYHidden == 1) {
            mFAB.setTranslationY(0);
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mFAB.animate()
                        .translationX(0)
                        .setDuration(TRANSLATION_MOVE_TIME)
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        isXHidden = -1;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
                mFabRotation(false);
            }
        }, 200);
    }

    private void mFabRotation(boolean clockwise) {
        if (isRotating) return;
        mFAB.setRotation(0);
        float rotateDegree = (clockwise ? 360 : -360);
        isRotating = true;
        mFAB.animate()
                .rotation(rotateDegree)
                .setDuration(1000);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                isRotating = false;
            }
        }, 600);
    }

    public void mFabOnClick(View view) {
        mFabRotation(true);
        final Intent intent = new Intent(this, ActivityEditor.class)
                .putExtra(ClipObjectActionBridge.STATUE_IS_STARRED, isStarred);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ActivityOptions options = ActivityOptions
                    .makeSceneTransitionAnimation(this, mFAB, getString(R.string.action_star));
            //startActivity(intent, options.toBundle());
            startActivity(intent);
        } else {
            startActivity(intent);
        }
    }

    private void clearDeleteQueue() {
        for (ClipObject clipObject : deleteQueue) {
            db.modifyClip(clipObject.getText(), null, Storage.MAIN_ACTIVITY_VIEW);
        }
        deleteQueue.clear();
    }

    public static void refreshMainView(Context context, String query) {
        Intent intent = new Intent(context, ActivityMain.class)
                .putExtra(EXTRA_QUERY_TEXT, query)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private void setStarredIcon() {
        if (isStarred) {
            starItem.setIcon(R.drawable.ic_action_star_white);
        } else {
            starItem.setIcon(R.drawable.ic_action_star_outline_white);
        }
    }

    private void setItemsVisibility() {
        if (clipCardAdapter.getItemCount() == 0) {
            mRecLayout.setVisibility(View.INVISIBLE);
        } else {
            mRecLayout.setVisibility(View.VISIBLE);
        }
    }

    private void setView() {
        //get clips
        if (isStarred) {
            clips = db.getStarredClipHistory(queryText);
        } else {
            clips = db.getClipHistory(queryText);
        }

        //set view
        clipCardAdapter = new ClipCardAdapter(clips, this);
        mRecList.setAdapter(clipCardAdapter);

        setItemsVisibility();
    }

    private void showSnackbar(final int position, final ClipObject clipObject, final ClipCardAdapter clipCardAdapter) {
        deleteQueue.add(clipObject);
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
                                mFabRotation(false);
                                mFAB.animate().translationY(-snackbar.getHeight()).setDuration(TRANSLATION_MOVE_TIME);
                                if (position >= (clipCardAdapter.getItemCount() - 1) && clipCardAdapter.getItemCount() > 6) {
                                    mRecList.animate().translationY(-snackbar.getHeight());
                                }
                            }

                            @Override
                            public void onShown(Snackbar snackbar) {
                                isSnackbarShow += 1;
                            }

                            @Override
                            public void onDismiss(Snackbar snackbar) {
                                isSnackbarShow -= 1;
                                if (!isUndo[0]) {
                                    deleteQueue.remove(clipObject);
                                    db.modifyClip(clipObject.getText(), null, Storage.MAIN_ACTIVITY_VIEW);
                                }
                            }

                            @Override
                            public void onDismissed(Snackbar snackbar) {
                                if (isSnackbarShow <= 0) {
                                    isSnackbarShow = 0;
                                    mFAB.animate().translationY(0).setDuration(TRANSLATION_MOVE_TIME);
                                    mRecList.animate().translationY(0);
                                    mFabRotation(true);
                                }
                                //if (position <= 1 || position >= (clipCardAdapter.getItemCount() - 1)) {
                                //mRecList.smoothScrollToPosition(position);
                                //}
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
        //db.modifyClip(null, getString(R.string.first_launch_clips_3, "👈", "😇"));
        db.modifyClip(null, getString(R.string.first_launch_clipboards_3, "", "👉"), Storage.SYSTEM_CLIPBOARD);
        Thread.sleep(50);
        db.modifyClip(null, getString(R.string.first_launch_clipboards_2, "🙋"), Storage.SYSTEM_CLIPBOARD);
        Thread.sleep(50);
        db.modifyClip(null, getString(R.string.first_launch_clipboards_1, "😄"), Storage.SYSTEM_CLIPBOARD);
        Thread.sleep(50);
        db.modifyClip(null, getString(R.string.first_launch_clipboards_0, "😄"), Storage.SYSTEM_CLIPBOARD);
        BackupManager backupManager = new BackupManager(this);
        backupManager.requestRestore(new RestoreObserver() {
            @Override
            public void restoreFinished(int error) {
                super.restoreFinished(error);
            }
        });
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

    public class ClipCardAdapter extends RecyclerView.Adapter<ClipCardAdapter.ClipCardViewHolder> {
        private Context context;
        private List<ClipObject> clipObjectList;
        public SimpleDateFormat sdfDate;
        public SimpleDateFormat sdfTime;
        private boolean allowAnimate = true;

        public ClipCardAdapter(List<ClipObject> clipObjectList, Context context) {
            this.context = context;
            this.clipObjectList = clipObjectList;
            sdfDate = new SimpleDateFormat(context.getString(R.string.date_format));
            sdfTime = new SimpleDateFormat(context.getString(R.string.time_format));
            DisplayMetrics displaymetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    allowAnimate = false;
                }
            }, 100);
        }

        @Override
        public int getItemCount() {
            return clipObjectList.size();
        }

        @Override
        public void onBindViewHolder(final ClipCardViewHolder clipCardViewHolder, int i) {
            final ClipObject clipObject = clipObjectList.get(i);
            clipCardViewHolder.vDate.setText(sdfDate.format(clipObject.getDate()));
            clipCardViewHolder.vTime.setText(sdfTime.format(clipObject.getDate()));
            clipCardViewHolder.vText.setText(clipObject.getText().trim());
            clipCardViewHolder.vStarred.setImageResource(
                    clipObject.isStarred() ?
                            R.drawable.ic_action_star_yellow : R.drawable.ic_action_star_outline_grey600
            );

            addClickStringAction(context, clipObject, ClipObjectActionBridge.ACTION_EDIT, clipCardViewHolder.vText);
            addLongClickStringAction(context, clipObject, ClipObjectActionBridge.ACTION_COPY, clipCardViewHolder.vText);
            addClickStringAction(context, clipObject, ClipObjectActionBridge.ACTION_SHARE, clipCardViewHolder.vShare);

            clipCardViewHolder.vStarred.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clipObject.setStarred(!clipObject.isStarred());
                    db.starredClip(clipObject);
                    if (clipObject.isStarred()) {
                        clipCardViewHolder.vStarred.setImageResource(R.drawable.ic_action_star_yellow);
                    } else {
                        clipCardViewHolder.vStarred.setImageResource(R.drawable.ic_action_star_outline_grey600);
                        if (isStarred) {
                            //remove form starred list.
                            remove(clipObject);
                        }
                    }
                }
            });

            setAnimation(clipCardViewHolder.vMain, i);

        }

        @Override
        public ClipCardViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View itemView = LayoutInflater.
                    from(viewGroup.getContext()).
                    inflate(R.layout.activity_main_card, viewGroup, false);

            return new ClipCardViewHolder(itemView);
        }

        public void add(int position, ClipObject clipObject) {
            clipObjectList.add(position, clipObject);
            notifyItemInserted(position);
            setItemsVisibility();
        }

        public void remove(ClipObject clipObject) {
            int position = clipObjectList.indexOf(clipObject);
            remove(position);
        }

        public void remove(int position) {
            clipObjectList.remove(position);
            notifyItemRemoved(position);
            setItemsVisibility();
        }

        public void addClickStringAction(final Context context, final ClipObject clipObject, final int actionCode, View button) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent openIntent = new Intent(context, ClipObjectActionBridge.class)
                            .putExtra(ClipObjectActionBridge.CLIPBOARD_STRING, clipObject.getText())
                            .putExtra(ClipObjectActionBridge.STATUE_IS_STARRED, clipObject.isStarred())
                            .putExtra(ClipObjectActionBridge.CLIPBOARD_ACTION, actionCode);
                    context.startService(openIntent);
                }
            });
        }

        public void addLongClickStringAction(final Context context, final ClipObject clipObject, final int actionCode, View button) {
            button.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Intent openIntent = new Intent(context, ClipObjectActionBridge.class)
                            .putExtra(ClipObjectActionBridge.CLIPBOARD_STRING, clipObject.getText())
                            .putExtra(ClipObjectActionBridge.STATUE_IS_STARRED, clipObject.isStarred())
                            .putExtra(ClipObjectActionBridge.CLIPBOARD_ACTION, actionCode);
                    context.startService(openIntent);
                    if (isFromNotification) {
                        moveTaskToBack(true);
                    }
                    return true;
                }
            });
        }

        private void setAnimation(final View viewToAnimate, int position) {
            //animate for list fade in
            if (!allowAnimate) {
                return;
            }
            viewToAnimate.setVisibility(View.INVISIBLE);
            final Animation animation = AnimationUtils.loadAnimation(context, android.R.anim.fade_in);
            animation.setDuration(400);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    viewToAnimate.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    viewToAnimate.startAnimation(animation);
                }
            }, (position+2) * 60);
        }

        public class ClipCardViewHolder extends RecyclerView.ViewHolder {
            protected TextView vTime;
            protected TextView vDate;
            protected TextView vText;
            protected ImageButton vStarred;
            protected View vShare;
            protected View vMain;

            public ClipCardViewHolder(View v) {
                super(v);
                vTime = (TextView) v.findViewById(R.id.activity_main_card_time);
                vDate = (TextView) v.findViewById(R.id.activity_main_card_date);
                vText = (TextView) v.findViewById(R.id.activity_main_card_text);
                vStarred = (ImageButton) v.findViewById(R.id.activity_main_card_star_button);
                vShare = v.findViewById(R.id.activity_main_card_share_button);
                vMain = v;
            }
        }

    }
}
