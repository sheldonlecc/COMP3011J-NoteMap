package com.amap.apis.cluster;

import com.amap.api.maps.model.BitmapDescriptor;

/**
 * Created by yiyi.qi on 16/10/10.
 *
 * <p>自定义聚合渲染接口，返回每个聚合点对应的 {@link BitmapDescriptor}。</p>
 */
public interface ClusterRender {
    /**
     * 根据聚合点内容返回渲染所需的 {@link BitmapDescriptor}。
     *
     * @param cluster 聚合数据
     * @return 渲染用的位图描述对象；返回 {@code null} 时将使用默认样式。
     */
    BitmapDescriptor getBitmapDescriptor(Cluster cluster);
}
