package com.example.novaflix;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

public class MoviesAdapter extends RecyclerView.Adapter<MoviesAdapter.MovieViewHolder> {
    private List<Movies> moviesList;
    private final Context context;
    private final OnDownloadClickListener downloadClickListener;
    private final OnMovieClickListener movieClickListener;

    // Constructor with click listeners for download and movie selection
    public MoviesAdapter(@NonNull List<Movies> movies, Context context,
                         OnDownloadClickListener downloadClickListener,
                         OnMovieClickListener movieClickListener) {
        this.moviesList = movies;
        this.context = context;
        this.downloadClickListener = downloadClickListener;
        this.movieClickListener = movieClickListener;
    }

    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_movies, parent, false);
        return new MovieViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
        Movies movie = moviesList.get(position);
        holder.titleTextView.setText(movie.getTitle());
        holder.playCountTextView.setText(String.format("%d plays", movie.getPlayCount()));

        // Load the logo into the thumbnail ImageView using Glide with placeholders
        Glide.with(context)
                .load(movie.getLogoUrl())
                .apply(new RequestOptions()
                        .placeholder(R.drawable.movie) // Placeholder image
                        .error(R.drawable.movie))      // Error image
                .into(holder.thumbnailImageView);

        // Download button click event
        holder.downloadButton.setOnClickListener(v -> {
            if (downloadClickListener != null) {
                downloadClickListener.onDownloadClick(movie.getDownloadUrl(), movie.getTitle());
            }
        });

        // Thumbnail click event to open the player with the selected movie
        holder.thumbnailImageView.setOnClickListener(v -> {
            if (movieClickListener != null) {
                movieClickListener.onMovieClick(movie);
            }
        });


        holder.playButton.setOnClickListener(v -> {
            if (movieClickListener != null) {
                movieClickListener.onMovieClick(movie);
            }
        });
    }


    @Override
    public int getItemCount() {
        return moviesList.size();
    }

    // Update the movie list and notify the adapter
    public void updateList(@NonNull List<Movies> newList) {
        moviesList.clear();
        moviesList.addAll(newList);
        notifyDataSetChanged();
    }

    static class MovieViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView playCountTextView;
        ImageButton downloadButton;
        ImageButton rattingButton;
        ImageView thumbnailImageView;
        Button playButton;

        public MovieViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.movieTitle);
            playCountTextView = itemView.findViewById(R.id.playCount);
            downloadButton = itemView.findViewById(R.id.download_Button);
            thumbnailImageView = itemView.findViewById(R.id.movieThumbnail);
            playButton=itemView.findViewById(R.id.play_Button);
            rattingButton=itemView.findViewById(R.id.rating_icon);

        }
    }

    // Listener interface to handle download button clicks
    public interface OnDownloadClickListener {
        void onDownloadClick(String videoUrl, String title);
    }

    // Listener interface to handle movie item clicks
    public interface OnMovieClickListener {
        void onMovieClick(Movies movie);
    }
}
