package com.noworld.notemap.overlay;

import android.content.Context;

import com.amap.api.maps.AMap;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.route.WalkPath;
import com.amap.api.services.route.WalkStep;

import java.util.List;

/**
 * Walking route overlay for AMap; use it to render walking navigation lines or extend as needed.
 *
 * @since V2.1.0
 */
public class WalkRouteOverlay extends RouteOverlay {

    private PolylineOptions mPolylineOptions;

    private BitmapDescriptor walkStationDescriptor = null;

    private WalkPath walkPath;

    /**
     * Create a walking route overlay.
     *
     * @param context current activity
     * @param amap    map instance
     * @param path    walking path result (see WalkStep)
     * @param start   start point (LatLonPoint)
     * @param end     end point (LatLonPoint)
     * @since V2.1.0
     */
    public WalkRouteOverlay(Context context, AMap amap, WalkPath path,
                            LatLonPoint start, LatLonPoint end) {
        super(context);
        this.mAMap = amap;
        this.walkPath = path;
        startPoint = AMapServicesUtil.convertToLatLng(start);
        endPoint = AMapServicesUtil.convertToLatLng(end);
    }

    /**
     * Add walking route to the map.
     *
     * @since V2.1.0
     */
    public void addToMap() {

        initPolylineOptions();
        try {
            List<WalkStep> walkPaths = walkPath.getSteps();
            for (int i = 0; i < walkPaths.size(); i++) {
                WalkStep walkStep = walkPaths.get(i);
                LatLng latLng = AMapServicesUtil.convertToLatLng(walkStep
                        .getPolyline().get(0));

                addWalkStationMarkers(walkStep, latLng);
                addWalkPolyLines(walkStep);

            }
            addStartAndEndMarker();

            showPolyline();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * Check gap between end of this step and start of next.
     */
    private void checkDistanceToNextStep(WalkStep walkStep,
                                         WalkStep walkStep1) {
        LatLonPoint lastPoint = getLastWalkPoint(walkStep);
        LatLonPoint nextFirstPoint = getFirstWalkPoint(walkStep1);
        if (!(lastPoint.equals(nextFirstPoint))) {
            addWalkPolyLine(lastPoint, nextFirstPoint);
        }
    }

    /**
     * @param walkStep
     * @return
     */
    private LatLonPoint getLastWalkPoint(WalkStep walkStep) {
        return walkStep.getPolyline().get(walkStep.getPolyline().size() - 1);
    }

    /**
     * @param walkStep
     * @return
     */
    private LatLonPoint getFirstWalkPoint(WalkStep walkStep) {
        return walkStep.getPolyline().get(0);
    }


    private void addWalkPolyLine(LatLonPoint pointFrom, LatLonPoint pointTo) {
        addWalkPolyLine(AMapServicesUtil.convertToLatLng(pointFrom), AMapServicesUtil.convertToLatLng(pointTo));
    }

    private void addWalkPolyLine(LatLng latLngFrom, LatLng latLngTo) {
        mPolylineOptions.add(latLngFrom, latLngTo);
    }

    /**
     * @param walkStep
     */
    private void addWalkPolyLines(WalkStep walkStep) {
        mPolylineOptions.addAll(AMapServicesUtil.convertArrList(walkStep.getPolyline()));
    }

    /**
     * @param walkStep
     * @param position
     */
    private void addWalkStationMarkers(WalkStep walkStep, LatLng position) {
        addStationMarker(new MarkerOptions()
                .position(position)
                .title("Direction:" + walkStep.getAction()
                        + "\nRoad:" + walkStep.getRoad())
                .snippet(walkStep.getInstruction()).visible(nodeIconVisible)
                .anchor(0.5f, 0.5f).icon(walkStationDescriptor));
    }

    /**
     * Initialize polyline options
     */
    private void initPolylineOptions() {

        if (walkStationDescriptor == null) {
            walkStationDescriptor = getWalkBitmapDescriptor();
        }

        mPolylineOptions = null;

        mPolylineOptions = new PolylineOptions();
        mPolylineOptions.color(getWalkColor()).width(getRouteWidth());
    }


    private void showPolyline() {
        addPolyLine(mPolylineOptions);
    }
}
