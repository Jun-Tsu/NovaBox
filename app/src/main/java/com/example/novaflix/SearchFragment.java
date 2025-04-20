package com.example.novaflix;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
public class SearchFragment extends Fragment implements MoviesAdapter.OnMovieClickListener {
    private RecyclerView searchResultsRecyclerView;
    private MoviesAdapter searchResultsAdapter;
    private List<Movies> allMovies;
    private List<Movies> filteredMovies = new ArrayList<>();
    private EditText searchField;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        // Retrieve the movie list from arguments
        if (getArguments() != null) {
            allMovies = (List<Movies>) getArguments().getSerializable("movies_list");
        }

        searchField = view.findViewById(R.id.searchField);
        searchResultsRecyclerView = view.findViewById(R.id.searchResultsRecyclerView);

        // Set up RecyclerView
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        searchResultsAdapter = new MoviesAdapter(
                filteredMovies,
                getContext(),
                null, // No download listener
                this  // Pass the fragment itself as the movie click listener
        );
        searchResultsRecyclerView.setAdapter(searchResultsAdapter);

        // Add a listener for search input
        searchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterMovies(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    private void filterMovies(String query) {
        filteredMovies.clear();
        if (query.isEmpty()) {
            filteredMovies.addAll(allMovies);
        } else {
            for (Movies movie : allMovies) {
                if (movie.getTitle().toLowerCase().contains(query.toLowerCase())) {
                    filteredMovies.add(movie);
                }
            }
        }
        searchResultsAdapter.notifyDataSetChanged();
    }

    @Override
    public void onMovieClick(Movies movie) {
        Log.d("SearchFragment", "Selected movie: " + movie.getTitle());
        // Handle the movie selection
    }
}
