package com.example.chassetresor;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DashBoardActivity extends AppCompatActivity {

    WebSocketService wsService;
    ServiceConnection onService;

    private boolean[] hintTouched;

    int numberOfIndices;
    boolean firstTime = true;

    RecyclerView recyclerView;

    List<Integer> images = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        numberOfIndices=10;

        onService = new ServiceConnection(){
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                wsService = ((WebSocketService.LocalBinder) service).getService();
                Log.i("on connection", "service connected");

                List<String> players = wsService.getPlayerNames();
                images.clear();
                for (int i = 0; i<players.size(); i++){
                    images.add(R.drawable.player_image);
                }

                if (firstTime) {
                    createDashboard(wsService.getPlayerNames(), wsService.getPlayerProgression(), numberOfIndices);
                    firstTime = false;
                }
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

    }

    @Override
    public void onResume() {
        super.onResume();

        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra("HINT_TOUCHED")){ // vérifie qu'une valeur est associée à la clé “edittext”
                hintTouched = intent.getBooleanArrayExtra("HINT_TOUCHED"); // on récupère la valeur associée à la clé
            }
        }

        bindService(new Intent(this, WebSocketService.class), onService, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();

        unbindService(onService);
    }

    public void createDashboard(List<String> playerNames, List<Integer> playerProgressions, int numberOfIndices){

        setContentView(R.layout.activity_dash_board);

        recyclerView = findViewById(R.id.recyclerView);

        MyAdapter myAdapter = new MyAdapter(this,playerNames, playerProgressions, images, numberOfIndices);

        recyclerView.setAdapter(myAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    public void clickButtonMap(View view) {
        Intent myIntent = new Intent(DashBoardActivity.this, MapActivity.class);
        myIntent.putExtra("HINT_TOUCHED", hintTouched);
        DashBoardActivity.this.startActivity(myIntent);
    }
}