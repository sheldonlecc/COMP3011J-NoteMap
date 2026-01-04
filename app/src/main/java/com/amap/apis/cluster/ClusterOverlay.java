package com.amap.apis.cluster;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.LruCache;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Typeface;

import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.animation.AlphaAnimation;
import com.amap.api.maps.model.animation.Animation;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by yiyi.qi on 16/10/10.
 * Design uses two threads: one calculates clusters, the other handles marker operations.
 */
public class ClusterOverlay implements AMap.OnCameraChangeListener,
        AMap.OnMarkerClickListener {
    private AMap mAMap;
    private Context mContext;
    private List<ClusterItem> mClusterItems;
    private List<Cluster> mClusters;
    private int mClusterSize;
    private ClusterClickListener mClusterClickListener;
    private ClusterRender mClusterRender;
    private List<Marker> mAddMarkers = new ArrayList<Marker>();
    private double mClusterDistance;
    private LruCache<String, BitmapDescriptor> mLruCache;
    private HandlerThread mMarkerHandlerThread = new HandlerThread("addMarker");
    private HandlerThread mSignClusterThread = new HandlerThread("calculateCluster");
    private Handler mMarkerhandler;
    private Handler mSignClusterHandler;
    private float mPXInMeters;
    private boolean mIsCanceled = false;

    /**
     * Constructor.
     *
     * @param amap map instance
     * @param clusterSize cluster radius in screen pixels
     * @param context context
     */
    public ClusterOverlay(AMap amap, int clusterSize, Context context) {
        this(amap, null, clusterSize, context);


    }

    /**
     * Constructor used when adding cluster items in batch.
     *
     * @param amap map instance
     * @param clusterItems cluster items
     * @param clusterSize cluster radius in screen pixels
     * @param context context
     */
    public ClusterOverlay(AMap amap, List<ClusterItem> clusterItems,
                          int clusterSize, Context context) {
        // Cache up to 120 marker bitmaps by default; adjust if memory allows.
        mLruCache = new LruCache<String, BitmapDescriptor>(120) {
            protected void entryRemoved(boolean evicted, String key, BitmapDescriptor oldValue, BitmapDescriptor newValue) {
                reycleBitmap(oldValue.getBitmap());
            }
        };
        if (clusterItems != null) {
            mClusterItems = clusterItems;
        } else {
            mClusterItems = new ArrayList<ClusterItem>();
        }
        mContext = context;
        mClusters = new ArrayList<Cluster>();
        this.mAMap = amap;
        mClusterSize = clusterSize;
        mPXInMeters = mAMap.getScalePerPixel();
        mClusterDistance = mPXInMeters * mClusterSize;
        amap.setOnCameraChangeListener(this);
        amap.setOnMarkerClickListener(this);
        initThreadHandler();
        assignClusters();
    }

    private void reycleBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }
        // Do not recycle on newer Android versions
        if (Build.VERSION.SDK_INT <= 10) {
            if (!bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
    }

    /**
     * Set cluster click listener.
     *
     * @param clusterClickListener listener
     */
    public void setOnClusterClickListener(
            ClusterClickListener clusterClickListener) {
        mClusterClickListener = clusterClickListener;
    }

    /**
     * Add a single cluster item.
     *
     * @param item cluster item
     */
    public void addClusterItem(ClusterItem item) {
        Message message = Message.obtain();
        message.what = SignClusterHandler.CALCULATE_SINGLE_CLUSTER;
        message.obj = item;
        mSignClusterHandler.sendMessage(message);
    }

    /**
     * Set custom cluster render; defaults to bubble with count if not provided.
     *
     * @param render renderer
     */
    public void setClusterRenderer(ClusterRender render) {
        mClusterRender = render;
    }

    public void onDestroy() {
        mIsCanceled = true;
        mSignClusterHandler.removeCallbacksAndMessages(null);
        mMarkerhandler.removeCallbacksAndMessages(null);
        mSignClusterThread.quit();
        mMarkerHandlerThread.quit();
        for (Marker marker : mAddMarkers) {
            marker.remove();

        }
        mAddMarkers.clear();
        mLruCache.evictAll();
    }

    // Initialize handlers
    private void initThreadHandler() {
        mMarkerHandlerThread.start();
        mSignClusterThread.start();
        mMarkerhandler = new MarkerHandler(mMarkerHandlerThread.getLooper());
        mSignClusterHandler = new SignClusterHandler(mSignClusterThread.getLooper());
    }

    @Override
    public void onCameraChange(CameraPosition arg0) {


    }

    @Override
    public void onCameraChangeFinish(CameraPosition arg0) {
        mPXInMeters = mAMap.getScalePerPixel();
        mClusterDistance = mPXInMeters * mClusterSize;
        assignClusters();
    }

    // Click event
    @Override
    public boolean onMarkerClick(Marker arg0) {
        if (mClusterClickListener == null) {
            return true;
        }
       Cluster cluster= (Cluster) arg0.getObject();
        if(cluster!=null){
            mClusterClickListener.onClick(arg0,cluster.getClusterItems());
            return true;
        }
        return false;
    }


    /**
     * Add cluster groups onto the map.
     */
    private void addClusterToMap(List<Cluster> clusters) {

        ArrayList<Marker> removeMarkers = new ArrayList<>();
        removeMarkers.addAll(mAddMarkers);
        AlphaAnimation alphaAnimation=new AlphaAnimation(1, 0);
        MyAnimationListener myAnimationListener=new MyAnimationListener(removeMarkers);
        for (Marker marker : removeMarkers) {
            marker.setAnimation(alphaAnimation);
            marker.setAnimationListener(myAnimationListener);
            marker.startAnimation();
        }

        for (Cluster cluster : clusters) {
            addSingleClusterToMap(cluster);
        }
    }

    private AlphaAnimation mADDAnimation=new AlphaAnimation(0, 1);
    /**
     * Add a single cluster onto the map.
     *
     * @param cluster cluster to draw
     */
    private void addSingleClusterToMap(Cluster cluster) {
        LatLng latlng = cluster.getCenterLatLng();
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.anchor(0.5f, 0.5f)
                .icon(getBitmapDes(cluster)).position(latlng);
        Marker marker = mAMap.addMarker(markerOptions);
        marker.setAnimation(mADDAnimation);
        marker.setObject(cluster);

        marker.startAnimation();
        cluster.setMarker(marker);
        mAddMarkers.add(marker);

    }



    private void calculateClusters() {
        mIsCanceled = false;
        mClusters.clear();
        LatLngBounds visibleBounds = mAMap.getProjection().getVisibleRegion().latLngBounds;
        for (ClusterItem clusterItem : mClusterItems) {
            if (mIsCanceled) {
                return;
            }
            LatLng latlng = clusterItem.getPosition();
            if (visibleBounds.contains(latlng)) {
                Cluster cluster = getCluster(latlng,mClusters);
                if (cluster != null) {
                    cluster.addClusterItem(clusterItem);
                } else {
                    cluster = new Cluster(latlng);
                    mClusters.add(cluster);
                    cluster.addClusterItem(clusterItem);
                }

            }
        }

        // Copy to avoid sync issues
        List<Cluster> clusters = new ArrayList<Cluster>();
        clusters.addAll(mClusters);
        Message message = Message.obtain();
        message.what = MarkerHandler.ADD_CLUSTER_LIST;
        message.obj = clusters;
        if (mIsCanceled) {
            return;
        }
        mMarkerhandler.sendMessage(message);
    }

    /**
     * Aggregate points into clusters.
     */
    private void assignClusters() {
        mIsCanceled = true;
        mSignClusterHandler.removeMessages(SignClusterHandler.CALCULATE_CLUSTER);
        mSignClusterHandler.sendEmptyMessage(SignClusterHandler.CALCULATE_CLUSTER);
    }

    /**
     * Aggregate a newly added item into existing clusters.
     *
     * @param clusterItem item to add
     */
    private void calculateSingleCluster(ClusterItem clusterItem) {
        LatLngBounds visibleBounds = mAMap.getProjection().getVisibleRegion().latLngBounds;
        LatLng latlng = clusterItem.getPosition();
        if (!visibleBounds.contains(latlng)) {
            return;
        }
        Cluster cluster = getCluster(latlng,mClusters);
        if (cluster != null) {
            cluster.addClusterItem(clusterItem);
            Message message = Message.obtain();
            message.what = MarkerHandler.UPDATE_SINGLE_CLUSTER;

            message.obj = cluster;
            mMarkerhandler.removeMessages(MarkerHandler.UPDATE_SINGLE_CLUSTER);
            mMarkerhandler.sendMessageDelayed(message, 5);


        } else {

            cluster = new Cluster(latlng);
            mClusters.add(cluster);
            cluster.addClusterItem(clusterItem);
            Message message = Message.obtain();
            message.what = MarkerHandler.ADD_SINGLE_CLUSTER;
            message.obj = cluster;
            mMarkerhandler.sendMessage(message);

        }
    }

    /**
     * Find an existing cluster a point can attach to; return null if none.
     *
     * @param latLng point position
     * @return cluster or null
     */
    private Cluster getCluster(LatLng latLng,List<Cluster>clusters) {
        for (Cluster cluster : clusters) {
            LatLng clusterCenterPoint = cluster.getCenterLatLng();
            double distance = AMapUtils.calculateLineDistance(latLng, clusterCenterPoint);
            if (distance < mClusterDistance && mAMap.getCameraPosition().zoom < 19) {
                return cluster;
            }
        }

        return null;
    }


    /**
     * Build the drawable for a cluster.
     */
    private BitmapDescriptor getBitmapDes(Cluster cluster) {
        int clusterCount = cluster.getClusterCount();
        String cacheKey = buildCacheKey(cluster, clusterCount);
        BitmapDescriptor bitmapDescriptor = mLruCache.get(cacheKey);
        if (bitmapDescriptor != null) {
            return bitmapDescriptor;
        }

        if (mClusterRender != null) {
            BitmapDescriptor customDescriptor = mClusterRender.getBitmapDescriptor(cluster);
            if (customDescriptor != null) {
                mLruCache.put(cacheKey, customDescriptor);
                return customDescriptor;
            }
        }

        // Default style: orange circle with white count text
        TextView textView = new TextView(mContext);
        textView.setGravity(Gravity.CENTER);
        textView.setTextColor(Color.WHITE);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        textView.setText(String.valueOf(clusterCount));

        int sizeDp;
        if (clusterCount < 5) {
            sizeDp = 48;
        } else if (clusterCount < 20) {
            sizeDp = 56;
        } else if (clusterCount < 50) {
            sizeDp = 64;
        } else {
            sizeDp = 72;
        }
        int sizePx = dp2px(mContext, sizeDp);
        textView.setWidth(sizePx);
        textView.setHeight(sizePx);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        // Orange palette close to original
        int color;
        if (clusterCount < 5) {
            color = Color.parseColor("#FF8A33"); // light orange
        } else if (clusterCount < 20) {
            color = Color.parseColor("#F36E21"); // medium orange
        } else if (clusterCount < 50) {
            color = Color.parseColor("#E65A0F"); // deep orange
        } else {
            color = Color.parseColor("#CC4A00"); // darker orange
        }
        bg.setColor(color);
        bg.setStroke(dp2px(mContext, 2), Color.WHITE);
        textView.setBackground(bg);

        BitmapDescriptor defaultDescriptor = BitmapDescriptorFactory.fromView(textView);
        mLruCache.put(cacheKey, defaultDescriptor);
        return defaultDescriptor;
    }

    private String buildCacheKey(Cluster cluster, int clusterCount) {
        if (clusterCount <= 1 && !cluster.getClusterItems().isEmpty()) {
            LatLng latLng = cluster.getClusterItems().get(0).getPosition();
            return "single_" + latLng.latitude + "_" + latLng.longitude;
        }
        return "cluster_" + clusterCount;
    }

    private int dp2px(Context ctx, float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, ctx.getResources().getDisplayMetrics());
    }

    /**
     * Update styles for clusters already on the map.
     */
    private void updateCluster(Cluster cluster) {

        Marker marker = cluster.getMarker();
            marker.setIcon(getBitmapDes(cluster));


    }


//-----------------------Internal helper classes---------------------------------------------

    /**
     * Marker fade-out animation; removes the marker when finished.
     */
    class MyAnimationListener implements Animation.AnimationListener {
        private  List<Marker> mRemoveMarkers ;

        MyAnimationListener(List<Marker> removeMarkers) {
            mRemoveMarkers = removeMarkers;
        }

        @Override
        public void onAnimationStart() {

        }

        @Override
        public void onAnimationEnd() {
            for(Marker marker:mRemoveMarkers){
                marker.remove();
            }
            mRemoveMarkers.clear();
        }
    }

    /**
     * Handle marker add/update operations.
     */
    class MarkerHandler extends Handler {

        static final int ADD_CLUSTER_LIST = 0;

        static final int ADD_SINGLE_CLUSTER = 1;

        static final int UPDATE_SINGLE_CLUSTER = 2;

        MarkerHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message message) {

            switch (message.what) {
                case ADD_CLUSTER_LIST:
                    List<Cluster> clusters = (List<Cluster>) message.obj;
                    addClusterToMap(clusters);
                    break;
                case ADD_SINGLE_CLUSTER:
                    Cluster cluster = (Cluster) message.obj;
                    addSingleClusterToMap(cluster);
                    break;
                case UPDATE_SINGLE_CLUSTER:
                    Cluster updateCluster = (Cluster) message.obj;
                    updateCluster(updateCluster);
                    break;
            }
        }
    }

    /**
     * Handle clustering calculation thread.
     */
    class SignClusterHandler extends Handler {
        static final int CALCULATE_CLUSTER = 0;
        static final int CALCULATE_SINGLE_CLUSTER = 1;

        SignClusterHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message message) {
            switch (message.what) {
                case CALCULATE_CLUSTER:
                    calculateClusters();
                    break;
                case CALCULATE_SINGLE_CLUSTER:
                    ClusterItem item = (ClusterItem) message.obj;
                    mClusterItems.add(item);
                    Log.i("yiyi.qi","calculate single cluster");
                    calculateSingleCluster(item);
                    break;
            }
        }
    }
}
