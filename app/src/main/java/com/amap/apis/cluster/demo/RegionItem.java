package com.amap.apis.cluster.demo;

import com.amap.api.maps.model.LatLng;
import com.amap.apis.cluster.ClusterItem;

import java.io.Serializable;

public class RegionItem implements ClusterItem, Serializable {

    private static final long serialVersionUID = 20251107L; // 保持版本号

    // 【1. 关键修改】将 LatLng 对象标记为 transient (瞬态)
    // 这告诉序列化器“跳过”这个字段，不要尝试序列化它。
    private transient LatLng mLatLng;

    // 【2. 新增】我们改为存储原始的经纬度，因为 double 是可序列化的
    private double mLatitude;
    private double mLongitude;

    private String mTitle;
    private String photoUrl;

    // 【3. 修改】构造函数，同时保存 LatLng 和 原始 double
    public RegionItem(LatLng latLng, String title, String photoUrl) {
        this.mLatLng = latLng; // 运行时使用
        this.mTitle = title;
        this.photoUrl = photoUrl;

        // 立即保存可序列化的值
        this.mLatitude = latLng.latitude;
        this.mLongitude = latLng.longitude;
    }

    // 兼容旧的构造函数
    public RegionItem(LatLng latLng, String title) {
        this(latLng, title, null);
    }

    // 【4. 关键修改】重写 getPosition()
    @Override
    public LatLng getPosition() {
        // 当对象被反序列化（在新 Activity 中）时，mLatLng 会是 null
        // 我们需要用保存的 double 值将其重新创建出来
        if (mLatLng == null) {
            mLatLng = new LatLng(mLatitude, mLongitude);
        }
        return mLatLng;
    }

    // (Getters 保持不变)
    public String getTitle() {
        return mTitle;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }
}