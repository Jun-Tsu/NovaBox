package com.example.novaflix;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.List;

public class ChannelAdapter extends RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder> {

    private List<Channel> channelList;
    private Context context;

    public ChannelAdapter(List<Channel> channelList, Context context) {
        this.channelList = channelList;
        this.context = context;
    }

    public void updateChannelList(List<Channel> newChannelList) {
        this.channelList = newChannelList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChannelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_channel, parent, false);
        return new ChannelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChannelViewHolder holder, int position) {
        Channel channel = channelList.get(position);
        holder.channelName.setText(channel.getName());

        holder.itemView.setOnClickListener(v -> {
            // Start ExoPlayerActivity with the selected channel's stream URL
            Intent intent = new Intent(context, PlayerActivity.class);
            intent.putExtra("STREAM_URL", channel.getStreamUrl());
            context.startActivity(intent);
        });
        // Load the logo using Picasso, ensuring the URL is not empty
        if (channel.getLogoUrl() != null && !channel.getLogoUrl().isEmpty()) {
            Picasso.get().load(channel.getLogoUrl()).into(holder.channelLogo);
        } else {
            holder.channelLogo.setImageResource(R.drawable.featured); // Set a default image if logo URL is empty
        }
    }

    @Override
    public int getItemCount() {
        return channelList.size();
    }

    static class ChannelViewHolder extends RecyclerView.ViewHolder {
        TextView channelName;
        ImageView channelLogo;

        public ChannelViewHolder(@NonNull View itemView) {
            super(itemView);
            channelName = itemView.findViewById(R.id.channel_name); // Ensure this ID matches your layout
            channelLogo = itemView.findViewById(R.id.channel_logo); // Ensure this ID matches your layout
        }
    }
}
