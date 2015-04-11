package com.catchingnow.tinyclipboardmanager;

import android.animation.Animator;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
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
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class ActivityMain extends MyActionBarActivity {
    public final static String EXTRA_IS_FROM_NOTIFICATION = "com.catchingnow.tinyclipboard.EXTRA.isFromNotification";
    public final static String FIRST_LAUNCH = "pref_is_first_launch";
    public final static String SECOND_LAUNCH = "pref_is_second_launch";
    private static int TRANSLATION_FAST = 400;
    private static int TRANSLATION_SLOW = 1000;

    private RecyclerView mRecList;
    private LinearLayout mRecLayout;
    private ClipCardAdapter clipCardAdapter;
    private LinearLayoutManager linearLayoutManager;
    protected Toolbar mToolbar;
    private ImageButton mFAB;
    private SearchView searchView;
    private MenuItem searchItem;
    private Menu menu;
    protected MenuItem starItem;

    protected SharedPreferences preference;
    protected Context context;
    private Storage db;
    private List<ClipObject> clips;
    private ArrayList<ClipObject> deleteQueue = new ArrayList<>();
    private BroadcastReceiver mMessageReceiver;

    //FAB
    private int isYHidden = -1;
    private int isXHidden = -1;
    private boolean isRotating = false;

    protected boolean isStarred = false;
    private boolean clickToCopy = true;
    private Date lastStorageUpdate = null;
    private String queryText = "";

    private int tooYoungTooSimple = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_main);
        super.onCreate(savedInstanceState);
        context = this.getBaseContext();
        db = Storage.getInstance(context);
        queryText = "";

        mFAB = (ImageButton) findViewById(R.id.main_fab);
        mRecLayout = (LinearLayout) findViewById(R.id.recycler_layout);
        mToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        if (getString(R.string.screen_type).contains("phone")) {
            mToolbar.setNavigationIcon(R.drawable.icon_shadow);
        } else {
            mToolbar.setNavigationIcon(R.drawable.ic_stat_icon);
        }
        initView();

        //tablet layout
        if (getString(R.string.screen_type).contains("tablet")) {
            RelativeLayout tabletMain = (RelativeLayout) findViewById(R.id.tablet_main);
            if (tabletMain != null) {
                ViewGroup.LayoutParams tabletMainLayoutParams = tabletMain.getLayoutParams();
                tabletMainLayoutParams.width =
                        (getScreenWidthPixels() * 2 / 3);
                tabletMain.setLayoutParams(tabletMainLayoutParams);
            }
        }

        easterEgg();

        attachKeyboardListeners();

        mMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getBooleanExtra(Storage.UPDATE_DB_ADD, false)) {
                    setView();
                } else {
                    clipCardAdapter.remove(intent.getStringExtra(Storage.UPDATE_DB_DELETE));
                }

            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(Storage.UPDATE_DB));

    }

    @Override
    protected void onStop() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            db.cleanUpAndRequestBackup();
        }
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (getString(R.string.screen_type).contains("phone")) {
            //phone
            mFAB.setTranslationX(0);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mFAB.animate().translationX(MyUtil.dip2px(context, 90));
                    mFabRotation(false, 600);
                }
            }, 600);
        } else {
            //tablet
            mFAB.setScaleX(1);
            mFAB.setScaleY(1);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mFAB.animate().scaleX(0).scaleY(0);
                    mFabRotation(false, 600);
                }
            }, 600);
        }

        clearDeleteQueue();
        db.updateSystemClipboard();
    }

    @Override
    protected void onResume() {
        super.onResume();

        preference = PreferenceManager.getDefaultSharedPreferences(this);
        clickToCopy = (preference.getString(ActivitySetting.PREF_LONG_CLICK_BEHAVIOR, "0").equals("1"));
        final boolean tmpStarred = preference.getBoolean(AppWidget.WIDGET_IS_STARRED, false);
        if (isStarred != tmpStarred) {
            final TransitionDrawable mFabBackground = (TransitionDrawable) mFAB.getBackground();
            mFabBackground.resetTransition();
            if (tmpStarred) mFabBackground.startTransition(10);
            mFAB.setImageResource(tmpStarred ?
                            R.drawable.ic_action_star_white
                            :
                            R.drawable.ic_action_add
            );
            lastStorageUpdate = null;
        }
        isStarred = tmpStarred;
        setStarredIcon();
        setView();
        //check if first launch
        if (preference.getBoolean(FIRST_LAUNCH, true)) {
            try {
                firstLaunch();
                preference.edit()
                        .putBoolean(FIRST_LAUNCH, false)
                        .apply();
            } catch (InterruptedException e) {
                Log.e(MyUtil.PACKAGE_NAME, "first launch error:");
                e.printStackTrace();
            }
        }

        if (getString(R.string.screen_type).contains("phone")) {
            //phone
            mFAB.setTranslationX(MyUtil.dip2px(context, 90));
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mFAB.animate().translationX(0);
                    mFabRotation(false, 600);
                }
            }, 600);
        } else {
            //tablet
            mFAB.setScaleX(0);
            mFAB.setScaleY(0);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mFAB.animate().scaleX(1).scaleY(1);
                    mFabRotation(false, 600);
                }
            }, 600);
        }
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        this.menu = menu;
        getMenuInflater().inflate(R.menu.menu_main, menu);
        starItem = menu.findItem(R.id.action_star);
        setStarredIcon();
        searchItem = menu.findItem(R.id.action_search);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) searchItem.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                if (getString(R.string.screen_type).contains("tablet")) {
                    MyUtil.ResizeWidthAnimation resizeWidthAnimation =
                            new MyUtil.ResizeWidthAnimation(mToolbar, (getScreenWidthPixels() * 2 / 3));
                    resizeWidthAnimation.setDuration(TRANSLATION_FAST);
                    mToolbar.startAnimation(resizeWidthAnimation);
                }
                searchView.setIconified(false);
                searchView.requestFocus();
                queryText = searchView.getQuery().toString();
                lastStorageUpdate = null;
                setView();
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                if (getString(R.string.screen_type).contains("tablet")) {
                    MyUtil.ResizeWidthAnimation resizeWidthAnimation =
                            new MyUtil.ResizeWidthAnimation(mToolbar, getScreenWidthPixels());
                    resizeWidthAnimation.setDuration(TRANSLATION_FAST);
                    mToolbar.startAnimation(resizeWidthAnimation);
                }
                searchView.clearFocus();
                queryText = null;
                lastStorageUpdate = null;
                setView();
                return true;
            }
        });
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                searchItem.collapseActionView();
                queryText = null;
                initView();
                lastStorageUpdate = null;
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
                lastStorageUpdate = null;
                setView();
                return true;
            }
        });

        initSecondLaunch();

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
                onStarredMenuClicked();
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
                cancelSecondLaunch();
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
        mFabRotation(true, TRANSLATION_FAST);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (getString(R.string.screen_type).contains("phone")) {
                    mFAB.animate().translationX(MyUtil.dip2px(context, 90));
                } else {
                    mFAB.animate().scaleX(0).scaleY(0);
                }
                mFAB.animate().setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        isXHidden = 1;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        isXHidden = 1;
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
                if (getString(R.string.screen_type).contains("phone")) {
                    mFAB.animate().translationX(0);
                } else {
                    mFAB.animate().scaleX(1).scaleY(1);
                }
                mFAB.animate().setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        isXHidden = -1;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        isXHidden = -1;
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
                mFabRotation(false, TRANSLATION_SLOW);
            }
        }, 200);
    }

    private void easterEgg() {
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tooYoungTooSimple += 1;
                switch (tooYoungTooSimple) {
                    case 3:
                        db.modifyClip(
                                null,
                                "　 ∧_∧\n" +
                                        "　(  ❜ω❜ )\n" +
                                        "　｜つ／(＿＿＿\n" +
                                        "／└-(＿＿＿_／\n" +
                                        "￣￣￣￣￣￣\n" +
                                        "Are you clicking me ?"
                        );
                        break;
                    case 4:
                        db.modifyClip(
                                null,
                                "　＜⌒／ヽ-_＿\n" +
                                        "／＜_/＿＿＿_／\n" +
                                        "￣￣￣￣￣￣\n" +
                                        "I want to sleep..."
                        );
                        break;
                    case 5:
                        db.modifyClip(
                                null,
                                "╮(╯_╰)╭\n" +
                                        "Well..."
                        );
                        break;
                    case 6:
                        Intent browserIntent = new Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=com.catchingnow.tinyclipboardmanager")
                        );
                        startActivity(browserIntent);
                        Toast.makeText(
                                context,
                                getString(R.string.pref_rate_title) +
                                        " ★★★★★\n" +
                                        "ヽ(́◕◞౪◟◕‵)ﾉ\n" +
                                        getString(R.string.pref_rate_summary),
                                Toast.LENGTH_LONG
                        ).show();
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                db.modifyClip(
                                        null,
                                        "(́^ _ ^)~♥\n" +
                                                "Thank you!"
                                );
                            }
                        }, 500);
                        tooYoungTooSimple = 0;
                        break;
                }
            }
        });
    }

    private void onStarredMenuClicked() {
        isStarred = !isStarred;
        mFabRotation(isStarred, TRANSLATION_SLOW);
        setStarredIcon();
        lastStorageUpdate = null;
        setView();
        final TransitionDrawable mFabBackground = (TransitionDrawable) mFAB.getBackground();
        if (isStarred) {
            mFabBackground.startTransition((int) mFAB.animate().getDuration());
        } else {
            mFabBackground.reverseTransition((int) mFAB.animate().getDuration());
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mFAB.setImageResource(isStarred ?
                                R.drawable.ic_action_star_white
                                :
                                R.drawable.ic_action_add
                );
            }
        }, TRANSLATION_SLOW / 3 * 2);
    }

    private void mFabRotation(boolean clockwise, long time) {
        if (isRotating) return;
        mFAB.setRotation(0);
        float rotateDegree = (clockwise ? 360 : -360);
        isRotating = true;
        mFAB.animate()
                .rotation(rotateDegree)
                .setDuration(time);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                isRotating = false;
            }
        }, time - 400);
    }

    public void mFabOnClick(View view) {
        mFabRotation(true, TRANSLATION_FAST);
        final Intent intent = new Intent(this, ActivityEditor.class)
                .putExtra(ClipObjectActionBridge.STATUE_IS_STARRED, isStarred);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //ActivityOptions options = ActivityOptions
            //        .makeSceneTransitionAnimation(this, mFAB, getString(R.string.action_star));
            //startActivity(intent, options.toBundle());
            startActivity(intent);
        } else {
            startActivity(intent);
        }
    }

    private void clearDeleteQueue() {
        for (ClipObject clipObject : deleteQueue) {
            db.modifyClip(clipObject.getText(), null);
            clipCardAdapter.remove(clipObject);
        }
        clipCardAdapter.notifyDataSetChanged();
        deleteQueue.clear();
    }

    protected void setStarredIcon() {
        if (starItem == null) return;
        if (isStarred) {
            starItem.setIcon(R.drawable.ic_switch_star_on);
        } else {
            starItem.setIcon(R.drawable.ic_switch_star_off);
        }
        preference.edit()
                .putBoolean(AppWidget.WIDGET_IS_STARRED, isStarred)
                .apply();
        AppWidget.updateAllAppWidget(context);
    }

    private void setItemsVisibility() {
        if (clipCardAdapter.getItemCount() == 0) {
            mRecLayout.setVisibility(View.INVISIBLE);
            //Animation animation = AnimationUtils.loadAnimation(context, android.R.anim.fade_out);
            //mRecLayout.setAnimation(animation);
        } else {
            mRecLayout.setVisibility(View.VISIBLE);
            //Animation animation = AnimationUtils.loadAnimation(context, android.R.anim.fade_in);
            //mRecLayout.setAnimation(animation);
        }
    }

    private void initView() {
        //init View

        mRecLayout.removeAllViewsInLayout();
        LayoutInflater layoutInflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.view_main_recycler, mRecLayout, true);
        mRecList = (RecyclerView) findViewById(R.id.cardList);
        mRecList.setHasFixedSize(true);
        linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecList.setLayoutManager(linearLayoutManager);

        SwipeableRecyclerViewTouchListener swipeDeleteTouchListener =
                new SwipeableRecyclerViewTouchListener(
                        context,
                        mRecList,
                        R.id.main_view,
                        R.id.main_background_view,
                        new SwipeableRecyclerViewTouchListener.SwipeListener() {
                            @Override
                            public boolean canSwipe(int position) {
                                return !clips.get(position).isStarred();
                            }

                            @Override
                            public void onDismissedBySwipe(RecyclerView recyclerView, int[] reverseSortedPositions) {
                                for (int position : reverseSortedPositions) {
                                    deleteQueue.add(clips.get(position));
                                }
                                clearDeleteQueue();
                            }

                        });
        mRecList.addOnItemTouchListener(swipeDeleteTouchListener);
        if (getString(R.string.screen_type).contains("phone")) {
            // hide FAB when list scroll on phone
            RecyclerView.OnScrollListener scrollFabAnimateListener = new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    if (dy > 20 && isYHidden == -1) {
                        //hide FAB on Y
                        if (isXHidden != -1) return;
                        isYHidden = 0;
                        mFabRotation(true, TRANSLATION_FAST);
                        mFabRotation(true, TRANSLATION_FAST);
                        mFAB.animate()
                                .translationY(MyUtil.dip2px(context, 90));
                        mFAB.animate()
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
                                        isYHidden = 1;
                                    }

                                    @Override
                                    public void onAnimationRepeat(Animator animation) {

                                    }
                                });
                    } else if (dy < -20 && isYHidden == 1) {
                        //show FAB on Y
                        if (isXHidden != -1) return;
                        isYHidden = 0;
                        mFabRotation(false, TRANSLATION_FAST);
                        mFabRotation(false, TRANSLATION_FAST);
                        mFAB.animate()
                                .translationY(0);
                        mFAB.animate()
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
                                        isYHidden = -1;
                                    }

                                    @Override
                                    public void onAnimationRepeat(Animator animation) {

                                    }
                                });
                    }
                }

                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        switch (newState) {
                            case RecyclerView.SCROLL_STATE_IDLE:
                                if (getString(R.string.screen_type).contains("phone")) {
                                    mToolbar.animate().translationZ(0);
                                }
                                mFAB.animate().translationZ(0);
                                break;
                            default:
                                if (getString(R.string.screen_type).contains("phone")) {
                                    mToolbar.animate().translationZ(MyUtil.dip2px(context, 4));
                                }
                                mFAB.animate().translationZ(MyUtil.dip2px(context, 4));
                                break;
                        }
                    }
                }

            };
            mRecList.setOnScrollListener(scrollFabAnimateListener);
        }
    }

    protected void setView() {

        if (db.getLatsUpdateDate() == lastStorageUpdate) return;
        lastStorageUpdate = db.getLatsUpdateDate();

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

    private void firstLaunch() throws InterruptedException {
        //db.modifyClip(null, getString(R.string.first_launch_clips_3, "👈", "😇"));
        db.modifyClip(null, getString(R.string.first_launch_clipboards_3, "", "👉"));
        Thread.sleep(50);
        db.modifyClip(null, getString(R.string.first_launch_clipboards_2, "🙋"));
        Thread.sleep(50);
        db.modifyClip(null, getString(R.string.first_launch_clipboards_1, "😄"), 1);
        Thread.sleep(50);
        db.modifyClip(null, getString(R.string.first_launch_clipboards_0, "😄"), 1);
//        BackupManager backupManager = new BackupManager(this);
//        backupManager.requestRestore(new RestoreObserver() {
//            @Override
//            public void restoreFinished(int error) {
//                super.restoreFinished(error);
//            }
//        });
    }

    private void initSecondLaunch() {
        //show red circle
        if (preference.getBoolean(SECOND_LAUNCH, true)) {
            MenuItem settingItem = menu.findItem(R.id.action_settings);
            settingItem.setTitle(settingItem.getTitle()+" 🙋");
            setOverflowButtonColor(this, R.drawable.ic_action_more_vert_white_with_star);
        }
    }

    private void cancelSecondLaunch() {
        if (preference.getBoolean(SECOND_LAUNCH, true)) {
            preference.edit()
                    .putBoolean(SECOND_LAUNCH, false)
                    .apply();
            MenuItem settingItem = menu.findItem(R.id.action_settings);
            settingItem.setTitle(String.valueOf(settingItem.getTitle()).replace(" 🙋", ""));
            setOverflowButtonColor(this, R.drawable.ic_action_more_vert_white);
        }
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

    protected void addClickStringAction(final Context context, final ClipObject clipObject, final int actionCode, View button) {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent openIntent = new Intent(context, ClipObjectActionBridge.class)
                        .putExtra(Intent.EXTRA_TEXT, clipObject.getText())
                        .putExtra(ClipObjectActionBridge.STATUE_IS_STARRED, clipObject.isStarred())
                        .putExtra(ClipObjectActionBridge.ACTION_CODE, actionCode);
                context.startService(openIntent);
            }
        });
    }

    protected void addLongClickStringAction(final Context context, final ClipObject clipObject, final int actionCode, View button) {
        button.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                v.playSoundEffect(0);
                Intent openIntent = new Intent(context, ClipObjectActionBridge.class)
                        .putExtra(Intent.EXTRA_TEXT, clipObject.getText())
                        .putExtra(ClipObjectActionBridge.STATUE_IS_STARRED, clipObject.isStarred())
                        .putExtra(ClipObjectActionBridge.ACTION_CODE, actionCode);
                context.startService(openIntent);
//                if (isFromNotification) {
//                    moveTaskToBack(true);
//                }
                return true;
            }
        });
    }

    protected void setActionIcon(ImageButton view) {
        //for dialog layout.
    }

    public class ClipCardAdapter extends RecyclerView.Adapter<ClipCardAdapter.ClipCardViewHolder> {
        private Context context;
        private List<ClipObject> clipObjectList;
        private boolean allowAnimate = true;

        public ClipCardAdapter(List<ClipObject> clipObjectList, Context context) {
            this.context = context;
            this.clipObjectList = clipObjectList;
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
            clipCardViewHolder.vDate.setText(MyUtil.getFormatDate(context, clipObject.getDate()));
            clipCardViewHolder.vTime.setText(MyUtil.getFormatTime(context, clipObject.getDate()));
            clipCardViewHolder.vText.setText(MyUtil.stringLengthCut(clipObject.getText()));
            if (clipObject.isStarred()) {
                clipCardViewHolder.vStarred.setImageResource(R.drawable.ic_action_star_yellow);
                clipCardViewHolder.vBackground.removeAllViews();
                clipCardViewHolder.vBackground.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            } else {
                clipCardViewHolder.vStarred.setImageResource(R.drawable.ic_action_star_outline_grey600);
            }

            if (clickToCopy) {
                addClickStringAction(context, clipObject, ClipObjectActionBridge.ACTION_COPY, clipCardViewHolder.vText);
                addLongClickStringAction(context, clipObject, ClipObjectActionBridge.ACTION_EDIT, clipCardViewHolder.vText);
            } else {
                addClickStringAction(context, clipObject, ClipObjectActionBridge.ACTION_EDIT, clipCardViewHolder.vText);
                addLongClickStringAction(context, clipObject, ClipObjectActionBridge.ACTION_COPY, clipCardViewHolder.vText);

            }
            addClickStringAction(context, clipObject, ClipObjectActionBridge.ACTION_SHARE, clipCardViewHolder.vShare);

            setActionIcon(clipCardViewHolder.vShare);

            clipCardViewHolder.vStarred.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    db.changeClipStarStatus(clipObject);
                    //clipObject.setStarred(!clipObject.isStarred());
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
            if (position == -1) return;
            remove(position);
        }

        public void remove(String clipString) {
            for (ClipObject clipObject : clipObjectList) {
                if (clipObject.getText().equals(clipString)) {
                    remove(clipObject);
                    return;
                }
            }
        }

        public void remove(int position) {
            clipObjectList.remove(position);
            notifyItemRemoved(position);
            setItemsVisibility();
        }

        private void setAnimation(final View viewToAnimate, int position) {
            //animate for list fade in
            if (!allowAnimate) {
                return;
            }
            viewToAnimate.setVisibility(View.INVISIBLE);
            final Animation animation = AnimationUtils.loadAnimation(context, android.R.anim.fade_in);
            animation.setDuration(TRANSLATION_FAST);
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
            }, (position + 2) * 60);
        }

        public class ClipCardViewHolder extends RecyclerView.ViewHolder {
            protected TextView vTime;
            protected TextView vDate;
            protected TextView vText;
            protected ImageButton vStarred;
            protected ImageButton vShare;
            protected LinearLayout vBackground;
            protected View vMain;

            public ClipCardViewHolder(View v) {
                super(v);
                vTime = (TextView) v.findViewById(R.id.activity_main_card_time);
                vDate = (TextView) v.findViewById(R.id.activity_main_card_date);
                vText = (TextView) v.findViewById(R.id.activity_main_card_text);
                vStarred = (ImageButton) v.findViewById(R.id.activity_main_card_star_button);
                vShare = (ImageButton) v.findViewById(R.id.activity_main_card_share_button);
                vBackground = (LinearLayout) v.findViewById(R.id.main_background_view);
                vMain = v;
            }
        }

    }
}
