package com.example.alimentaacao.widget;

import android.content.Intent;
import android.widget.RemoteViewsService;

public class FeedWidgetService extends RemoteViewsService {
    @Override public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new FeedWidgetFactory(getApplicationContext());
    }
}
