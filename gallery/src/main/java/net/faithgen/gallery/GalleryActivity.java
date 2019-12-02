package net.faithgen.gallery;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.Gravity;
import android.view.View;

import net.faithgen.gallery.adapters.AlbumAdapter;
import net.faithgen.gallery.models.Album;
import net.faithgen.gallery.utils.AlbumsData;
import net.faithgen.gallery.utils.Constants;
import net.faithgen.sdk.FaithGenActivity;
import net.faithgen.sdk.http.API;
import net.faithgen.sdk.http.ErrorResponse;
import net.faithgen.sdk.http.Pagination;
import net.faithgen.sdk.http.types.ServerResponse;
import net.faithgen.sdk.singletons.GSONSingleton;
import net.innoflash.iosview.recyclerview.RecyclerTouchListener;
import net.innoflash.iosview.recyclerview.RecyclerViewClickListener;
import net.innoflash.iosview.swipelib.SwipeRefreshLayout;

import java.util.HashMap;
import java.util.List;

import br.com.liveo.searchliveo.SearchLiveo;

public class GalleryActivity extends FaithGenActivity implements RecyclerViewClickListener, SwipeRefreshLayout.OnRefreshListener {

    private SearchLiveo searchLiveo;
    private RecyclerView albumsView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private GridLayoutManager gridLayoutManager;
    private List<Album> albums;
    private HashMap<String, String> params;
    private Pagination pagination;
    private AlbumsData albumsData;
    private AlbumAdapter albumAdapter;
    private Intent intent;
    private String filterText = "";

    @Override
    public String getPageTitle() {
        return Constants.GALLERY;
    }

    @Override
    public int getPageIcon() {
        return R.drawable.gallery_icon;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        albumsView = findViewById(R.id.albumsView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
            gridLayoutManager = new GridLayoutManager(this, 2);
        else gridLayoutManager = new GridLayoutManager(this, 3);

        swipeRefreshLayout.setPullPosition(Gravity.BOTTOM);
        swipeRefreshLayout.setOnRefreshListener(this);
        albumsView.setLayoutManager(gridLayoutManager);
        albumsView.addOnItemTouchListener(new RecyclerTouchListener(this, albumsView, this));

        params = new HashMap<>();

        searchLiveo = findViewById(R.id.search_liveo);
        searchLiveo.with(this, charSequence -> {
            filterText = (String) charSequence;
            Log.d("tag", "onCreate: " + charSequence);
            loadAlbums(Constants.ALBUMS, true);
        })
                .showVoice()
                .hideKeyboardAfterSearch()
                .hideSearch(() -> {
                    getToolbar().setVisibility(View.VISIBLE);
                    if (!filterText.isEmpty()) {
                        filterText = "";
                        searchLiveo.text(filterText);
                        loadAlbums(Constants.ALBUMS, true);
                    }
                })
                .build();
        setOnOptionsClicked(R.drawable.ic_search, view -> {
            searchLiveo.setVisibility(View.VISIBLE);
            searchLiveo.show();
            getToolbar().setVisibility(View.GONE);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (albums == null || albums.size() == 0)
            loadAlbums(Constants.ALBUMS, true);
    }

    private void loadAlbums(String url, boolean reload) {
        params.put(Constants.FILTER_TEXT, filterText);
        API.get(this, url, params, false, new ServerResponse() {
            @Override
            public void onServerResponse(String serverResponse) {
                swipeRefreshLayout.setRefreshing(false);
                Log.d("tag", "onServerResponse: " + serverResponse);
                pagination = GSONSingleton.getInstance().getGson().fromJson(serverResponse, Pagination.class);
                albumsData = GSONSingleton.getInstance().getGson().fromJson(serverResponse, AlbumsData.class);

                if (reload || albums == null || albums.size() == 0) {
                    albums = albumsData.getAlbums();
                    albumAdapter = new AlbumAdapter(GalleryActivity.this, albums);
                    albumsView.setAdapter(albumAdapter);
                } else {
                    albums.addAll(albumsData.getAlbums());
                    albumAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onError(ErrorResponse errorResponse) {
                super.onError(errorResponse);
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    @Override
    public void onClick(View view, int position) {
        intent = new Intent(this, AlbumActivity.class);
        intent.putExtra(Album.ID, albums.get(position).getId());
        startActivity(intent);
    }

    @Override
    public void onLongClick(View view, int position) {

    }

    @Override
    public void onRefresh() {
        if (pagination == null || pagination.getLinks().getNext() == null)
            swipeRefreshLayout.setRefreshing(false);
        else loadAlbums(pagination.getLinks().getNext(), false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {
            if (requestCode == SearchLiveo.REQUEST_CODE_SPEECH_INPUT) {
                searchLiveo.resultVoice(requestCode, resultCode, data);
            }
        }
    }
}