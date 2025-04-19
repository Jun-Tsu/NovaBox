package com.example.novaflix;

import android.app.PictureInPictureParams;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Rational;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import java.util.Collections;

public class PlayerActivity extends AppCompatActivity {

    private ExoPlayer exoPlayer;
    private PlayerView playerView;
    private String videoTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        // Enable immersive full-screen mode and allow dynamic orientation
        enableFullScreenMode();

        // Initialize PlayerView
        playerView = findViewById(R.id.player_view);
        playerView.setKeepScreenOn(true);

        // Get stream URL from the intent
        String streamUrl = getIntent().getStringExtra("STREAM_URL");
        videoTitle = getIntent().getStringExtra("VIDEO_TITLE");

        // Validate stream URL and set up player
        if (isNetworkAvailable() && !TextUtils.isEmpty(streamUrl)) {
            setupPlayer(streamUrl);
        } else {
            showToast("Invalid or missing stream URL or no network connectivity.");
            finish();
        }
    }

    /**
     * Configures and initializes the ExoPlayer with optimized buffering and media setup.
     *
     * @param streamUrl The URL of the video stream.
     */
    @OptIn(markerClass = UnstableApi.class)
    private void setupPlayer(@NonNull String streamUrl) {
        // Setup buffer durations with optimized parameters for smoother playback
        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                        10000, // Min buffer before playback starts
                        30000, // Max buffer duration
                        5000,  // Buffer duration during playback
                        7000   // Buffer during re-buffering
                )
                .setPrioritizeTimeOverSizeThresholds(true) // Prioritize time buffering
                .build();

        // Initialize ExoPlayer
        exoPlayer = new ExoPlayer.Builder(this)
                .setLoadControl(loadControl)
                .build();
        playerView.setPlayer(exoPlayer);

        // Add listener for playback state changes and errors
        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                showToast("Playback error: " + error.getMessage());
                retryPlayback();
            }

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
            public void onIsPlayingChanged(boolean isPlaying) {
                if (isPlaying && videoTitle != null) {
                    showToast("Now playing: " + videoTitle);
                }
            }
        });

        // Create media item with subtitles
        MediaItem mediaItem = new MediaItem.Builder()
                .setUri(streamUrl)
                .setSubtitleConfigurations(Collections.singletonList(
                        new MediaItem.SubtitleConfiguration.Builder(
                                Uri.parse("https://example.com/subtitles.vtt"))
                                .setMimeType(MimeTypes.TEXT_VTT)  // Subtitle MIME type
                                .setLanguage("en")               // Subtitle language
                                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                                .build()
                ))
                .build();

        // Set the media item and prepare the player
        exoPlayer.setMediaItem(mediaItem);
        exoPlayer.prepare();

        // Start playback after a brief delay to ensure pre-buffering
        exoPlayer.pause(); // Initial pause to allow buffer time
        playerView.postDelayed(() -> {
            if (exoPlayer != null && exoPlayer.getPlaybackState() == Player.STATE_READY) {
                exoPlayer.play();
            }
        }, 3000); // Adjust the delay as needed
    }

    /**
     * Checks if the network is available.
     *
     * @return true if the network is available, false otherwise.
     */
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

    /**
     * Retries the playback in case of an error.
     */
    private void retryPlayback() {
        if (exoPlayer != null) {
            exoPlayer.stop();
            exoPlayer.prepare();
            exoPlayer.play();
        }
    }

    /**
     * Releases the ExoPlayer resources safely.
     */
    private void releasePlayer() {
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (exoPlayer != null) {
            exoPlayer.pause();  // Pause player when activity is paused
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        releasePlayer();  // Release player when activity is stopped
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();  // Release player when activity is destroyed
    }

    @Override
    public void onUserLeaveHint() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && exoPlayer != null) {
            Rational aspectRatio = new Rational(playerView.getWidth(), playerView.getHeight());
            PictureInPictureParams.Builder pipBuilder = new PictureInPictureParams.Builder()
                    .setAspectRatio(aspectRatio);
            enterPictureInPictureMode(pipBuilder.build());  // Enable PiP mode
        }
    }

    /**
     * Enables immersive full-screen mode for the activity.
     */
    private void enableFullScreenMode() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        );
    }

    /**
     * Displays a Toast message to the user.
     *
     * @param message The message to display.
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}