package com.example.chassetresor;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonObject;

public class MenuActivity extends AppCompatActivity {

    WebSocketService wsService;
    ServiceConnection onService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        onService = new ServiceConnection(){
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                wsService = ((WebSocketService.LocalBinder) service).getService();
                Log.i("on connection", "service connected");
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                wsService = null;
                Log.i("on disconnection", "service disconnected");
            }

            @Override
            public void onBindingDied(ComponentName name) {
                wsService = null;
                Log.i("on binding death", "service binding dead");
            }

            @Override
            public void onNullBinding(ComponentName name) {
                wsService = null;
                Log.i("on binding null", "service binding null");
            }
        };

        Intent intent = new Intent(this, WebSocketService.class);
        startService(intent);
        
        bindService(intent, onService, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onPause() {
        super.onPause();

        unbindService(onService);
    }

    public void onSignInClick(View view){
        TextInputEditText username = (TextInputEditText) findViewById(R.id.username);
        String playerTag = username.getText().toString();

        wsService.addPlayer(playerTag);

        Intent intent = new Intent(getBaseContext(), MapActivity.class);
        startActivity(intent);
    }
}