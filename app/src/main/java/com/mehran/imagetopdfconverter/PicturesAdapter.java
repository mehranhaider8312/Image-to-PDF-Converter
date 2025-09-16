package com.mehran.imagetopdfconverter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class PicturesAdapter extends RecyclerView.Adapter<PicturesAdapter.ViewHolder> {

    Context context;
    ArrayList<Picture> pictures;

    public PicturesAdapter(Context context,ArrayList<Picture> pictures){
        this.context = context;
        this.pictures = pictures;
    }

    @NonNull
    @Override
    public PicturesAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.single_image_layout,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        Picture picture = pictures.get(position);
        holder.ivImage.setImageURI(picture.getImageUri());

    }
    @Override
    public int getItemCount() {return pictures.size();}

    public static class ViewHolder extends RecyclerView.ViewHolder{

        ImageView ivImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            ivImage = itemView.findViewById(R.id.imageViewItem);

        }
    }
}
