package com.charlesdrews.neighborhoodguide;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;

import com.charlesdrews.neighborhoodguide.places.Place;
import com.charlesdrews.neighborhoodguide.places.PlaceDbOpenHelper;

public class DetailActivity extends AppCompatActivity {
    private int mSelectedPlaceId;
    private PlaceDbOpenHelper mHelper;
    private TextView mNoteView;
    private String mNoteDraft = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setStatusBarColor(R.color.detailStatusBar);

        mSelectedPlaceId = getIntent().getExtras().getInt(MainActivity.SELECTED_PLACE_KEY, -1);

        if (mSelectedPlaceId >= 0) {

            mHelper = PlaceDbOpenHelper.getInstance(DetailActivity.this);
            final Place selectedPlace = mHelper.getPlaceById(mSelectedPlaceId);

            getSupportActionBar().setTitle(selectedPlace.getTitle());

            TextView locationView = (TextView) findViewById(R.id.detail_location);
            String locationText = "Location: " + selectedPlace.getLocation();
            locationView.setText(locationText);

            TextView neighborhoodView = (TextView) findViewById(R.id.detail_neighborhood);
            String neighborhoodText = "Neighborhood: " + selectedPlace.getNeighborhood();
            neighborhoodView.setText(neighborhoodText);

            mNoteView = (TextView) findViewById(R.id.detail_note);
            if (selectedPlace.getNote().isEmpty()) {
                mNoteView.setText(getString(R.string.detail_msg_click_to_add_note));
            } else {
                String note = "Your note: " + selectedPlace.getNote();
                mNoteView.setText(note);
            }
            mNoteView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    launchAddOrEditNoteDialog(selectedPlace);
                }
            });

            TextView descriptionView = (TextView) findViewById(R.id.detail_description);
            descriptionView.setText(selectedPlace.getDescription());

            RatingBar ratingBar = (RatingBar) findViewById(R.id.detail_rating_bar);
            ratingBar.setRating(selectedPlace.getRating());

            ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
                @Override
                public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                    selectedPlace.setRating(rating);
                    mHelper.setRatingById(mSelectedPlaceId, rating);
                    Snackbar.make(
                            findViewById(R.id.coordinator_layout_detail),
                            "Your rating of " + rating + " stars was saved for " + selectedPlace.getTitle(),
                            Snackbar.LENGTH_SHORT
                    ).show();
                }
            });

            final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
            setFabFavIcon(fab, selectedPlace.isFavorite());

            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    boolean isFavorite = !selectedPlace.isFavorite(); // toggle to opposite value
                    selectedPlace.setFavoriteStatus(isFavorite);
                    mHelper.setFavoriteStatusById(mSelectedPlaceId, isFavorite);
                    setFabFavIcon(fab, isFavorite);
                    if (isFavorite) {
                        Snackbar.make(view, selectedPlace.getTitle() + " favorited", Snackbar.LENGTH_SHORT).show();
                    } else {
                        Snackbar.make(view, selectedPlace.getTitle() + " unfavorited", Snackbar.LENGTH_SHORT).show();
                    }
                }
            });

        } else {
            TextView locationView = (TextView) findViewById(R.id.detail_location);
            locationView.setText(getString(R.string.err_msg_item_not_found));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setFabFavIcon(FloatingActionButton fab, boolean isFavorite) {
        if (isFavorite) {
            fab.setImageResource(R.drawable.ic_favorite_white_24dp); // filled in heart if favorite
        } else {
            fab.setImageResource(R.drawable.ic_favorite_border_white_24dp); // otherwise just outline
        }
    }

    private void setStatusBarColor(int colorResource) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = DetailActivity.this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(ContextCompat.getColor(DetailActivity.this, colorResource));
        }
    }

    private void  launchAddOrEditNoteDialog(final Place place) {
        AlertDialog.Builder builder = new AlertDialog.Builder(DetailActivity.this);
        final EditText input = new EditText(DetailActivity.this);

        if (place.getNote().isEmpty()) {
            builder.setTitle("Add a note");
        } else {
            builder.setTitle("Edit your note");
        }

        if (mNoteDraft.isEmpty()) {
            input.setText(place.getNote());
        } else {
            input.setText(mNoteDraft);
        }

        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                mNoteDraft = s.toString();
            }
        });

        builder.setView(input);

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mNoteDraft = "";
                dialog.dismiss();
            }
        });

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String note = input.getText().toString();

                place.setNote(note);
                mHelper.setNoteById(place.getId(), note);

                if (note.isEmpty()) {
                    mNoteView.setText(getString(R.string.detail_msg_click_to_add_note));
                } else {
                    mNoteView.setText("Your note: " + note);
                }

                dialog.dismiss();
                Snackbar.make(
                        findViewById(R.id.coordinator_layout_detail),
                        "Your note was saved to " + place.getTitle(),
                        Snackbar.LENGTH_SHORT).show();
            }
        });

        builder.show();
    }
}
