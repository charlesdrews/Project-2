package com.charlesdrews.neighborhoodguide;

import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.Spinner;

import com.charlesdrews.neighborhoodguide.places.PlaceDbOpenHelper;

import java.util.ArrayList;

public class FavoritesActivity extends AppCompatActivity {
    private Menu mMenu;
    private PlaceDbOpenHelper mHelper;
    private RecyclerCursorAdapter mAdapter;
    private String mCategoryFilterValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_favs);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.action_favorites);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        setStatusBarColor(R.color.favsStatusBar);

        mHelper = PlaceDbOpenHelper.getInstance(FavoritesActivity.this);
        final Cursor cursor = mHelper.getFavoritePlaces();

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view_favs);
        recyclerView.setHasFixedSize(false);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(FavoritesActivity.this);
        recyclerView.setLayoutManager(linearLayoutManager);

        mAdapter = new RecyclerCursorAdapter(FavoritesActivity.this, cursor);
        recyclerView.setAdapter(mAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mMenu = menu;
        getMenuInflater().inflate(R.menu.menu_favs, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView mSearchView = (SearchView) menu.findItem(R.id.action_search_favs).getActionView();
        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                updateCursorWithSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                updateCursorWithSearch(newText);
                return true;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_filter_favs:
                launchFilterDialog();
                return true;

            case R.id.action_refresh_favs:
                updateCursorWithFavorites();
                Snackbar.make(
                        findViewById(R.id.coordinator_layout_favs),
                        "Favorites refreshed",
                        Snackbar.LENGTH_SHORT
                ).show();
                return true;

            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCursorWithFavorites();
    }

    @Override
    protected void onDestroy() {
        Cursor cursor = mAdapter.getCursor();
        cursor.close();
        super.onDestroy();
    }

    private void updateCursorWithSearch(String query) {
        mAdapter.changeCursor(mHelper.searchFavoritePlaces(query));
    }

    private void updateCursorWithFavorites() {
        mAdapter.changeCursor(mHelper.getFavoritePlaces());
    }

    private void setStatusBarColor(int colorResource) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = FavoritesActivity.this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(ContextCompat.getColor(FavoritesActivity.this, colorResource));
        }
    }

    private void launchFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(FavoritesActivity.this);
        builder.setTitle("Filter by category");

        final Spinner spinner = new Spinner(FavoritesActivity.this);
        ArrayList<String> categories = mHelper.getCategories();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                FavoritesActivity.this,
                android.R.layout.simple_spinner_item,
                categories
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        if (mCategoryFilterValue != null) {
            spinner.setSelection(categories.indexOf(mCategoryFilterValue));
        }

        RelativeLayout relativeLayout = new RelativeLayout(FavoritesActivity.this);
        relativeLayout.setPadding(0, 40, 0, 0); // left, top, right, bottom
        relativeLayout.setGravity(Gravity.CENTER);
        relativeLayout.addView(spinner);
        builder.setView(relativeLayout);

        builder.setPositiveButton("Set Filter", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mCategoryFilterValue = spinner.getSelectedItem().toString();
                setFilter();
                dialog.dismiss();
            }
        });

        builder.setNegativeButton("Clear Filter", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mCategoryFilterValue = null;
                clearFilter();
                dialog.dismiss();
            }
        });

        builder.show();
    }

    private void setFilter() {
        mAdapter.changeCursor(mHelper.getFavoritePlacesByCategory(mCategoryFilterValue));

        if (mCategoryFilterValue.equals("All")) {
            mMenu.findItem(R.id.action_filter_favs).setIcon(R.drawable.ic_filter_list_white_18dp);
        } else {
            mMenu.findItem(R.id.action_filter_favs).setIcon(R.drawable.ic_filter_list_cyan_a200_18dp);
        }
    }

    private void clearFilter() {
        mAdapter.changeCursor(mHelper.getFavoritePlaces());
        mMenu.findItem(R.id.action_filter_favs).setIcon(R.drawable.ic_filter_list_white_18dp);
    }
}
