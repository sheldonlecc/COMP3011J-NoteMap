package com.amap.apis.cluster;

import com.amap.api.maps.model.Marker;

import java.util.List;

/**
 * Created by yiyi.qi on 16/10/10.
 */

public interface ClusterClickListener{
        /**
         * Callback when a cluster marker is clicked.
         *
         * @param marker
         *            the clicked cluster marker
         * @param clusterItems
         *            items contained in the cluster
         */
        public void onClick(Marker marker, List<ClusterItem> clusterItems);
}
