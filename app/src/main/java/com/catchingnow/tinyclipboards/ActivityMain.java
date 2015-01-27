package com.catchingnow.tinyclipboards;

import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.content.Intent;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.SearchView;

import java.util.List;


public class ActivityMain extends ActionBarActivity {
    private final static String PACKAGE_NAME = "com.catchingnow.tinyclipboards";
    private String queryText;
    private  RecyclerView recList;
    private Storage db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        queryText = handleIntent(getIntent());
        setView(queryText);
        Intent i = new Intent(this, CBWatcherService.class);
        this.startService(i);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        queryText = handleIntent(getIntent());
        setView(queryText);
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
                queryText = null;
                setView(null);
                searchView.clearFocus();
                return true;
            }
        });
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                searchItem.collapseActionView();
                queryText = null;
                setView(queryText);
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
            case R.id.action_settings:
                startActivity(new Intent(this, ActivitySetting.class));
                return super.onOptionsItemSelected(item);
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private String handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            queryText = intent.getStringExtra(SearchManager.QUERY);
            setView(queryText);
            return queryText;
        }
        return null;
    }

    public void setView(String query) {
//    TextView textView = new TextView(this);
//    textView.setTextSize(40);
//    textView.setText("your query is: " + query);
//    setContentView(textView);

        //get clips
        db = new Storage(this);
        final List<ClipObject> clips = db.getClipHistory(query);

        setContentView(R.layout.activity_main);
        recList = (RecyclerView) findViewById(R.id.cardList);
        recList.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        recList.setLayoutManager(llm);
        final ClipCardAdapter ca = new ClipCardAdapter(clips, this);
        recList.setAdapter(ca);

        SwipeDismissRecyclerViewTouchListener touchListener =
                new SwipeDismissRecyclerViewTouchListener(
                        recList,
                        new SwipeDismissRecyclerViewTouchListener.DismissCallbacks() {
                            @Override
                            public boolean canDismiss(int position) {
                                return true;
                            }

                            @Override
                            public void onDismiss(RecyclerView recyclerView, int[] reverseSortedPositions) {
                                for (int position : reverseSortedPositions) {

                                    db.deleteClipHistory(clips.get(position).text);
                                    clips.remove(position);
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

}
