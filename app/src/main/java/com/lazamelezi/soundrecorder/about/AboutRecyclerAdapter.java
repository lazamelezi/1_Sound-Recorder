package com.lazamelezi.soundrecorder.about;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.lazamelezi.soundrecorder.R;

import java.util.List;

public class AboutRecyclerAdapter extends RecyclerView.Adapter<AboutRecyclerAdapter.ViewHolder> {

    private final List<AboutModel> mData;
    private final Context context;
    private LayoutInflater inflater;
    private ItemClickListener itemClickListener;

    public AboutRecyclerAdapter(Context context, List<AboutModel> mData) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.mData = mData;
    }

    public void setItemClickListener(ItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View mView = inflater.inflate(R.layout.item_for_about_recycler_view, parent, false);
        return new ViewHolder(mView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        AboutModel aboutModel = mData.get(position);
        holder.myTitleText_id.setText(aboutModel.getTitle());

        if (aboutModel.getIcon() != 0)
            holder.myImageView.setImageResource(aboutModel.getIcon());
        else
            holder.myImageView.setVisibility(View.GONE);

        if (aboutModel.getTintColor() != 0)
            holder.myImageView.setColorFilter(aboutModel.getTintColor());

    }

    @Override
    public int getItemCount() {
        return mData.size();
    }


    public interface ItemClickListener {
        void onItemClick(View view, int position, Intent intent);
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView myTitleText_id;
        ImageView myImageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            myTitleText_id = itemView.findViewById(R.id.myTitleText_id);
            myImageView = itemView.findViewById(R.id.myImageView);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (itemClickListener != null)
                itemClickListener.onItemClick(v, getLayoutPosition(), mData.get(getLayoutPosition()).getIntent());
        }
    }
}
