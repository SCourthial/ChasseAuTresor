package com.example.chassetresor;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DashBoardActivity extends AppCompatActivity {

    int numberOfIndices;

    RecyclerView recyclerView;

    List<String> s1 = new ArrayList<>();

    List<Integer> images = new ArrayList<>();

    List<Integer> level = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        numberOfIndices =10;

        images.add(R.drawable.player_image);
        images.add(R.drawable.player_image);
        images.add(R.drawable.player_image);
        images.add(R.drawable.player_image);
        images.add(R.drawable.player_image);


        s1.add("Aleck");
        s1.add("Otba");
        s1.add("Sebastian");
        s1.add("Samuel");
        s1.add("Inconnu");

        level.add(8);
        level.add(7);
        level.add(7);
        level.add(6);
        level.add(0);

        createDashboard(s1,level,numberOfIndices);
    }

    public void createDashboard(List<String> playersName, List<Integer> levelProgression, int numberOfIndices){

        setContentView(R.layout.activity_dash_board);

        recyclerView = findViewById(R.id.recyclerView);

         MyAdapter myAdapter = new MyAdapter(this,s1,level,images, numberOfIndices);

        recyclerView.setAdapter(myAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

    }

    public void clickButtonMap(View view) {
        Intent myIntent = new Intent(DashBoardActivity.this, MapActivity.class);
        DashBoardActivity.this.startActivity(myIntent);
    }
}