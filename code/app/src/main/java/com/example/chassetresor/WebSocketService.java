package com.example.chassetresor;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.JsonObject;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class WebSocketService extends Service {

    private OkHttpClient client;
    private WebSocket ws;

    JSONParser parser;

    private ArrayList<String> playerNames;
    private ArrayList<Integer> playerProgression;

    private String playerName;

    private final Binder binder = new LocalBinder();

    @Override
    public void onCreate() {
        client = new OkHttpClient();

        Request request = new Request.Builder().url("wss://chase-ar.herokuapp.com/").build();
        WSListener listener = new WSListener();
        ws = client.newWebSocket(request, listener);

        playerNames = new ArrayList<String>();
        playerProgression = new ArrayList<Integer>();

        parser = new JSONParser();

        Thread pinger = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        Log.i("ping", "ping");
                        ping();
                        Thread.sleep(10000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        pinger.start();
    }

    public class LocalBinder extends Binder {
        public WebSocketService getService() {
            return (WebSocketService.this);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "WebSocketService done", Toast.LENGTH_SHORT).show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    public void addPlayer(String name){
        JsonObject postData = new JsonObject();
        postData.addProperty("id", "newPlayer");
        postData.addProperty("data", name);

        playerName = name;

        ws.send(postData.toString());
    }

    public void addHint(){
        JsonObject postData = new JsonObject();
        postData.addProperty("id", "updatePlayer");
        postData.addProperty("data", playerName);

        ws.send(postData.toString());
    }

    public void ping(){
        JsonObject postData = new JsonObject();
        postData.addProperty("id", "ping");

        ws.send(postData.toString());
    }

    public ArrayList<String> getPlayerNames(){
        return playerNames;
    }

    public ArrayList<Integer> getPlayerProgression() {
        return playerProgression;
    }

    private final class WSListener extends WebSocketListener {
        private static final int NORMAL_CLOSURE_STATUS = 1000;

        public void onOpen(WebSocket webSocket, Response response) {
        }

        public void onMessage(WebSocket webSocket, String text) {
            JSONObject json;
            try {
                json = (JSONObject) parser.parse(text);

                JSONObject leaderboard = (JSONObject) json.get("data");

                leaderboard.forEach((playerName, playerProg) -> {
                    if (!playerName.equals("null")) {
                        if (!playerNames.contains(playerName)) {
                            playerNames.add((String) playerName);
                            playerProgression.add(((Long) playerProg).intValue());
                        } else {
                            playerProgression.set(playerNames.indexOf(playerName), ((Long) playerProg).intValue());
                        }
                    }
                });

            } catch (ParseException e) {
                e.printStackTrace();
            }

            output("Receiving : " + text);
        }

        public void onMessage(WebSocket webSocket, ByteString bytes) {
            output("Receiving bytes : " + bytes.hex());
        }

        public void onClosing(WebSocket webSocket, int code, String reason) {
            webSocket.close(NORMAL_CLOSURE_STATUS, null);
            output("Closing : " + code + " / " + reason);
        }

        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            output("Error : " + t.getMessage());
        }
    }



    private void output(final String txt) {
        Log.i("WebSocketResponse", txt);
    }
}

