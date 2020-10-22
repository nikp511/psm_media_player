package video.player.qrplayer.fragment;


import android.content.Context;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.clockbyte.admobadapter.bannerads.AdmobBannerRecyclerAdapterWrapper;
import com.clockbyte.admobadapter.bannerads.BannerAdViewWrappingStrategyBase;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

import java.io.File;
import java.util.ArrayList;

import video.player.qrplayer.R;
import video.player.qrplayer.utils.Utils;

public class SavedStatusFragment extends Fragment {

    RecyclerView recyclerView;
    SavedStatusAdapter adapter;
    TextView txtNoVideo;
    ArrayList<String> statuList = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_saved_status, container, false);
        /*recyclerView = view.findViewById(R.id.recycleSaved);
        txtNoVideo=view.findViewById(R.id.txtNoVideo);
        GridLayoutManager manager = new GridLayoutManager(getActivity(), 3, GridLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(manager);
        adapter = new SavedStatusAdapter(getActivity());
        recyclerView.setAdapter(adapter);
        statuList=FetchVideos();*/


        recyclerView = view.findViewById(R.id.recycleSaved);
        txtNoVideo = view.findViewById(R.id.txtNoVideo);
        GridLayoutManager manager = new GridLayoutManager(getActivity(), 2, GridLayoutManager.VERTICAL, false);
        recyclerView.addItemDecoration(new GridSpacingItemDecoration(2, 20, true));
        recyclerView.setLayoutManager(manager);
        statuList = FetchVideos();
        adapter = new SavedStatusAdapter(getActivity(), statuList);
        AdmobBannerRecyclerAdapterWrapper adapterWrapper = AdmobBannerRecyclerAdapterWrapper.builder(getActivity())
                .setLimitOfAds(20)
                .setFirstAdIndex(2)
                .setSingleAdSize(AdSize.LARGE_BANNER)
                .setNoOfDataBetweenAds(6)
                .setAdapter(adapter)
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
            recyclerView.setAdapter(adapter);
        }
        if (statuList.size() == 0) {
            txtNoVideo.setVisibility(View.VISIBLE);
        } else {
            txtNoVideo.setVisibility(View.INVISIBLE);
        }
        Utils.isWhatsapp = 1;
        return view;
    }

    public boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setUserVisibleHint(true);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            View view = getView();
            recyclerView = view.findViewById(R.id.recycleSaved);
            txtNoVideo = view.findViewById(R.id.txtNoVideo);
            GridLayoutManager manager = new GridLayoutManager(getActivity(), 2, GridLayoutManager.VERTICAL, false);
//            recyclerView.addItemDecoration(new GridSpacingItemDecoration(2, 20, true));
            recyclerView.setLayoutManager(manager);
            statuList = FetchVideos();
            adapter = new SavedStatusAdapter(getActivity(), statuList);
            AdmobBannerRecyclerAdapterWrapper adapterWrapper = AdmobBannerRecyclerAdapterWrapper.builder(getActivity())
                    .setLimitOfAds(20)
                    .setFirstAdIndex(4)
                    .setSingleAdSize(AdSize.LARGE_BANNER)
                    .setNoOfDataBetweenAds(4)
                    .setAdapter(adapter)
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
                recyclerView.setAdapter(adapter);
            }
            if (statuList.size() == 0) {
                txtNoVideo.setVisibility(View.VISIBLE);
            } else {
                txtNoVideo.setVisibility(View.INVISIBLE);
            }
            Utils.isWhatsapp = 1;
        }
    }

    private ArrayList<String> FetchVideos() {

        ArrayList<String> filenames = new ArrayList<String>();
        String path = Environment.getExternalStorageDirectory()
                + File.separator + Utils.FOLDER_NAME;

        File directory = new File(path);

        if (!directory.exists()) {
            directory.mkdirs();
        }
        File[] files = directory.listFiles();

        for (int i = 0; i < files.length; i++) {

            String file_name = files[i].toString();
            // you can store name to arraylist and use it later
            filenames.add(file_name);
        }
        return filenames;
    }

    @Override
    public void onResume() {
        super.onResume();
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
