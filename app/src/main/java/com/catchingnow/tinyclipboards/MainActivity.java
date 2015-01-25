package com.catchingnow.tinyclipboards;

import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;

import java.util.List;


public class MainActivity extends ActionBarActivity {
    private final static String PACKAGE_NAME = "com.catchingnow.tinyclipboards";
    private Storage db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(PACKAGE_NAME, "onCreate!");
        String query = handleIntent(getIntent());
        setView(query);
        Intent i = new Intent(this, CBWatcherService.class);
        this.startService(i);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        String query = handleIntent(getIntent());
        setView(query);
        super.onNewIntent(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        SearchableInfo searchInfo = searchManager.getSearchableInfo(getComponentName());
        //searchView.setSearchableInfo(searchInfo);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                setView(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        //if (id == R.id.action_settings) {
        //    return true;
        //}
        if (id == R.id.action_search) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private String handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            return query;
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
        List<ClipObject> clips = db.getClipHistory(query);

        setContentView(R.layout.activity_main);
        RecyclerView recList = (RecyclerView) findViewById(R.id.cardList);
        recList.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        recList.setLayoutManager(llm);
        ClipCardAdapter ca = new ClipCardAdapter(clips, this);
        recList.setAdapter(ca);
    }

}
