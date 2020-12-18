package com.example.chassetresor;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

public class WebSocketServiceConnection implements ServiceConnection {

    private WebSocketService wsService;

    public WebSocketServiceConnection(WebSocketService wsService){
        this.wsService = wsService;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        wsService = ((WebSocketService.LocalBinder) service).getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        wsService = null;
    }

    @Override
    public void onBindingDied(ComponentName name) {
        wsService = null;
    }

    @Override
    public void onNullBinding(ComponentName name) {
        wsService = null;
    }
}
