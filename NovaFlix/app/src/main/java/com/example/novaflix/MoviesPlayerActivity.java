package com.example.novaflix;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.ui.PlayerView;

public class MoviesPlayerActivity extends AppCompatActivity {

    private static final String TAG = "MoviesPlayerActivity";
    private ExoPlayer exoPlayer;
    private PlayerView playerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movies_player);

        // Enable full-screen immersive mode
        enableFullScreenMode();

        // Keep the screen on while playing
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        playerView = findViewById(R.id.player_view);
        playerView.setKeepScreenOn(true);

        // Get the video URL from the Intent
        String videoUrl = getIntent().getStringExtra("videoUrl");
        if (videoUrl == null || videoUrl.isEmpty()) {
            showToast("No video URL provided.");
            finish();
            return;
        }

        // Check network connectivity before initializing the player
        if (isNetworkAvailable()) {
            setupPlayer(videoUrl);
        } else {
            showToast("No network connection available.");
            finish();
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    private void setupPlayer(@NonNull String videoUrl) {
        // Initialize the track selector with adaptive capabilities
        DefaultTrackSelector trackSelector = new DefaultTrackSelector(this);
        trackSelector.setParameters(
                trackSelector.buildUponParameters()
                        .setMaxVideoSizeSd()
                        .setAllowVideoMixedMimeTypeAdaptiveness(true)
        );

        // Configure custom load control for larger buffering
        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                        20000, // Minimum buffer before playback starts
                        120000, // Maximum buffer in background
                        20000, // Minimum buffer before resuming playback
                        10000  // Buffer for rebuffering
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build();

        // Initialize ExoPlayer
        exoPlayer = new ExoPlayer.Builder(this)
                .setTrackSelector(trackSelector)
                .setLoadControl(loadControl)
                .build();

        // Attach player to the PlayerView
        playerView.setPlayer(exoPlayer);

        // Set up MediaItem
        MediaItem mediaItem = new MediaItem.Builder()
                .setUri(Uri.parse(videoUrl))
                .setMimeType(String.valueOf(C.CONTENT_TYPE_MOVIE))
                .build();

        exoPlayer.setMediaItem(mediaItem);
        exoPlayer.prepare();
        exoPlayer.play();

        // Add player listeners
        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                switch (playbackState) {
                    case Player.STATE_BUFFERING:
                        showToast("Buffering...");
                        break;
                    case Player.STATE_READY:
                        showToast("Ready to play!");
                        break;
                    case Player.STATE_ENDED:
                        showToast("Playback finished.");
                        break;
                }
            }

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                Log.e(TAG, "Player error: " + error.getMessage());
                showToast("Playback error occurred.");
                retryPlayback();
            }
        });
    }

    private void retryPlayback() {
        if (exoPlayer != null) {
            exoPlayer.stop();
            exoPlayer.prepare();
            exoPlayer.play();
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            Network network = connectivityManager.getActiveNetwork();
            if (network != null) {
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
                return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            }
        }
        return false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (exoPlayer != null) {
            exoPlayer.pause();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        releasePlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }

    private void releasePlayer() {
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
    }

    private void enableFullScreenMode() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        );
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
