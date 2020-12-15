package com.example.chassetresor;

import android.content.Context;
import android.media.Image;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {

    List<String> data1;
    List <Integer> images;
    Context context;
    List<Integer>levelProgression;
    int numberOfIndices;

    public MyAdapter(Context context, List<String> s1,  List <Integer> levelProgression, List<Integer> images, int numberOfIndices){
        this.context =context;
        data1=s1;
        this.levelProgression=levelProgression;
        this.images=images;
        this.numberOfIndices=numberOfIndices;

    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View view = layoutInflater.inflate(R.layout.my_row,parent,false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.myBar.setMax(numberOfIndices);
        holder.myText1.setText(data1.get(position));
        holder.myText2.setText(levelProgression.get(position)+"/"+holder.myBar.getMax());
        holder.myText3.setText((((double) levelProgression.get(position)/ (double) holder.myBar.getMax())*100) +" %");
        holder.myImage.setImageResource(images.get(position));
        holder.myBar.setProgress(levelProgression.get(position));
        holder.myBar.setScaleY(5f);
    }

    @Override
    public int getItemCount() {
        return data1.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder{

        TextView myText1, myText2;
        TextView myText3;
        ImageView myImage;
        ProgressBar myBar;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            myText1=itemView.findViewById(R.id.players_name_txt);
            myText2=itemView.findViewById(R.id.description_player_txt);
            myText3=itemView.findViewById(R.id.percentage);
            myImage=itemView.findViewById(R.id.player_image);
            myBar = itemView.findViewById(R.id.myProgressBar);

        }
    }
}
