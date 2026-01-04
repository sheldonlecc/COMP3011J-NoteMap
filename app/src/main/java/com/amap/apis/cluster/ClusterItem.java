package com.amap.apis.cluster;

import com.amap.api.maps.model.LatLng;

/**
 * Created by yiyi.qi on 16/10/10.
 */

public interface ClusterItem {
    /**
     * Return the position of the clustered element.
     *
     * @return
     */
     LatLng getPosition();
}
