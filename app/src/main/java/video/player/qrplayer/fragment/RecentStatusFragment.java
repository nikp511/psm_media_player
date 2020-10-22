package video.player.qrplayer.fragment;


import android.content.Context;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.clockbyte.admobadapter.bannerads.AdmobBannerRecyclerAdapterWrapper;
import com.clockbyte.admobadapter.bannerads.BannerAdViewWrappingStrategyBase;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

import java.io.File;
import java.net.URLConnection;
import java.util.ArrayList;

import video.player.qrplayer.R;
import video.player.qrplayer.adapter.RecentStatusAdapter;
import video.player.qrplayer.utils.Utils;

public class RecentStatusFragment extends Fragment {

    public static ArrayList<String> whatsappStatusList;
    public static String folder;
    RecyclerView recyclerView;
    RecentStatusAdapter recentStatusAdapter;
    ArrayList<String> statusList = new ArrayList<>();
    ImageView imgRefresh;
    TextView txtNoVideo;

    public static boolean isVideoFile(String path) {
        String mimeType = URLConnection.guessContentTypeFromName(path);
        return mimeType != null && mimeType.startsWith("video");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_recent_status, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        txtNoVideo = view.findViewById(R.id.txtNoVideo);
        folder = Environment.getExternalStorageDirectory()
                + File.separator + "WhatsApp/Media/.Statuses";
        whatsappStatusList = new ArrayList<>();
        whatsappStatusList = FetchVideos();
        if (whatsappStatusList.size() == 0) {
            txtNoVideo.setVisibility(View.VISIBLE);
        } else {
            txtNoVideo.setVisibility(View.INVISIBLE);
        }
        GridLayoutManager manager = new GridLayoutManager(getActivity(), 2, GridLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(manager);
        recentStatusAdapter = new RecentStatusAdapter(getActivity(), whatsappStatusList);
        recyclerView.addItemDecoration(new GridSpacingItemDecoration(2, 20, true));

        AdmobBannerRecyclerAdapterWrapper adapterWrapper = AdmobBannerRecyclerAdapterWrapper.builder(getActivity())
                .setLimitOfAds(20)
                .setFirstAdIndex(4)
                .setSingleAdSize(AdSize.LARGE_BANNER)
                .setNoOfDataBetweenAds(4)
                .setAdapter(recentStatusAdapter)
                .setSingleAdUnitId(getString(R.string.adUnitID))
                .setAdViewWrappingStrategy(new BannerAdViewWrappingStrategyBase() {
                    @NonNull
                    @Override
                    protected ViewGroup getAdViewWrapper(ViewGroup parent) {
                        return (ViewGroup) LayoutInflater.from(parent.getContext()).inflate(R.layout.item_banner_ad,
                                parent, false);
                    }

                    @Override
                    protected void recycleAdViewWrapper(@NonNull ViewGroup wrapper, @NonNull AdView ad) {
                        ViewGroup container = (ViewGroup) wrapper.findViewById(R.id.ad_container);
                        for (int i = 0; i < container.getChildCount(); i++) {
                            View v = container.getChildAt(i);
                            if (v instanceof AdView) {
                                container.removeViewAt(i);
                                break;
                            }
                        }
                    }

                    @Override
                    protected void addAdViewToWrapper(@NonNull ViewGroup wrapper, @NonNull AdView ad) {
                        ViewGroup container = (ViewGroup) wrapper.findViewById(R.id.ad_container);
                        container.addView(ad);
                    }
                })
                .build();

        //rviewVideoList.getRecycledViewPool().clear();
        //adapterWrapper.notifyDataSetChanged();

        if (isNetworkConnected()) {
            recyclerView.setAdapter(adapterWrapper);
            manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    if (adapterWrapper.getItemViewType(position) == adapterWrapper.getViewTypeAdBanner()) {
                        return 2;
                    } else {
                        return 1;
                    }
                }
            });
            recyclerView.setLayoutManager(manager);
        } else {
            recyclerView.setAdapter(recentStatusAdapter);
        }
        return view;
    }

    public boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            if (recentStatusAdapter != null) {
                recentStatusAdapter.notifyDataSetChanged();
            }
            Utils.isWhatsapp = 0;
        }
    }

    private ArrayList<String> FetchVideos() {

        ArrayList<String> filenames = new ArrayList<String>();
        String path = Environment.getExternalStorageDirectory()
                + File.separator + "WhatsApp/Media/.Statuses";

        File directory = new File(path);
        if (directory.exists()) {
            File[] files = directory.listFiles();

            for (int i = 0; i < files.length; i++) {
                String file_name = files[i].toString();
                if (isVideoFile(file_name)) {
                    filenames.add(file_name);
                }
            }
        }
        return filenames;
    }

    public class GetDuration extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            return null;
        }
    }

    public class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {

        private int spanCount;
        private int spacing;
        private boolean includeEdge;

        public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount;
            this.spacing = spacing;
            this.includeEdge = includeEdge;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view); // item position
            int column = position % spanCount; // item column

            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount; // spacing - column * ((1f / spanCount) * spacing)
                outRect.right = (column + 1) * spacing / spanCount; // (column + 1) * ((1f / spanCount) * spacing)

                if (position < spanCount) { // top edge
                    outRect.top = spacing;
                }
                outRect.bottom = spacing; // item bottom
            } else {
                outRect.left = column * spacing / spanCount; // column * ((1f / spanCount) * spacing)
                outRect.right = spacing - (column + 1) * spacing / spanCount; // spacing - (column + 1) * ((1f /    spanCount) * spacing)
                if (position >= spanCount) {
                    outRect.top = spacing; // item top
                }
            }
        }
    }

}
