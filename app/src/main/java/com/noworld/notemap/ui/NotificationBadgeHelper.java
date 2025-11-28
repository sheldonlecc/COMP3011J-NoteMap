package com.noworld.notemap.ui;

import android.view.View;
import android.widget.TextView;

public class NotificationBadgeHelper {
    private final TextView badgeView;

    public NotificationBadgeHelper(TextView badgeView) {
        this.badgeView = badgeView;
    }

    public void setCount(int count) {
        if (badgeView == null) return;
        if (count <= 0) {
            badgeView.setVisibility(View.GONE);
        } else {
            badgeView.setVisibility(View.VISIBLE);
            badgeView.setText(String.valueOf(Math.min(count, 99)));
        }
    }
}
