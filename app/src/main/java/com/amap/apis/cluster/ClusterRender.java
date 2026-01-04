package com.amap.apis.cluster;

import com.amap.api.maps.model.BitmapDescriptor;

/**
 * Created by yiyi.qi on 16/10/10.
 *
 * <p>Render interface for clusters, returning a {@link BitmapDescriptor} for each cluster.</p>
 */
public interface ClusterRender {
    /**
     * Return the {@link BitmapDescriptor} for a given cluster.
     *
     * @param cluster cluster data
     * @return descriptor for rendering; {@code null} uses default styling.
     */
    BitmapDescriptor getBitmapDescriptor(Cluster cluster);
}
