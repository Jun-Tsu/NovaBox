package com.example.novaflix;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

public class VideoPlayerActivity extends Activity {

    private VideoView videoView;
    private ImageButton playPauseButton, rewindButton, previousButton, forwardButton, nextButton,backButton,settingButton;
    private TextView movieTitle, startTime, endTime;
    private SeekBar videoSeekbar;
    private String videoUriString;
    private boolean isPlaying = false;
    private LinearLayout playerControls;
    private RelativeLayout topbar;
    private Handler hideHandler = new Handler();
    private Runnable hideRunnable = new Runnable() {
        @Override
        public void run() {
            hideControls();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        // Initialize views
        videoView = findViewById(R.id.videoView);
        playPauseButton = findViewById(R.id.playPauseButton);
        rewindButton = findViewById(R.id.rewindButton);
        previousButton = findViewById(R.id.previousButton);
        forwardButton = findViewById(R.id.forwardButton);
        nextButton = findViewById(R.id.nextButton);
        movieTitle = findViewById(R.id.movieTitle);
        videoSeekbar = findViewById(R.id.videoSeekbar);
        playerControls = findViewById(R.id.playerControls);
        topbar = findViewById(R.id.topBar);
        startTime = findViewById(R.id.startTimeText);
        endTime = findViewById(R.id.endTimeText);
        backButton=findViewById(R.id.backButton) ;
                settingButton=findViewById(R.id.settingsButton);

        // Get the URI passed from the Intent
        videoUriString = getIntent().getStringExtra("video_uri");

        if (videoUriString != null) {
            Uri videoUri = Uri.parse(videoUriString);
            videoView.setVideoURI(videoUri);

            // Extract the file name from the URI
            String fileName = videoUri.getLastPathSegment();

            // Set the file name as the movie title (you can customize it further)
            movieTitle.setText("Playing: " + fileName);

            videoView.start();
            playPauseButton.setImageResource(R.drawable.ic_pause); // Set to pause icon
            isPlaying = true;
        }
        else {
            Toast.makeText(this, "Error: No video file found.", Toast.LENGTH_SHORT).show();
        }

        // Hide the status and navigation bar for full-screen mode
        hideSystemUI();

        // Play/Pause Button Listener
        playPauseButton.setOnClickListener(v -> togglePlayPause());

        // Rewind Button Listener
        rewindButton.setOnClickListener(v -> rewindVideo());

        // Forward Button Listener
        forwardButton.setOnClickListener(v -> forwardVideo());

        // Previous Button Listener
        previousButton.setOnClickListener(v -> playPreviousVideo());

        // Next Button Listener
        nextButton.setOnClickListener(v -> playNextVideo());
        backButton.setOnClickListener(V->finish());
        settingButton.setOnClickListener(v -> showSettingDialog());
        // Video Seekbar Listener
        videoSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    int newPosition = (int) (progress / 100.0 * videoView.getDuration());
                    videoView.seekTo(newPosition);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // No action needed
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // No action needed
            }
        });

        // Update the SeekBar position during video playback
        videoView.setOnPreparedListener(mp -> {
            videoSeekbar.setMax(videoView.getDuration());
            updateSeekBar();
        });

        videoView.setOnCompletionListener(mp -> {
            // When the video finishes, move to next video
            playNextVideo();
        });

        // Show/Hide controls when user taps on the screen
        videoView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                toggleControlsVisibility();
                hideHandler.removeCallbacks(hideRunnable); // Reset timer
                hideHandler.postDelayed(hideRunnable, 3000); // Hide controls after 3 seconds
            }
            return false;
        });
    }
    private void showSettingDialog() {
        final String[] subtitleOptions = {"Change Subtitle Language", "Add Custom Subtitle"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Subtitle Settings")
                .setItems(subtitleOptions, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            // User chose "Change Subtitle Language"
                            showSubtitleLanguageDialog();
                        } else if (which == 1) {
                            // User chose "Add Custom Subtitle"
                            openFileChooserForSubtitle();
                        }
                    }
                });
        builder.create().show();
    }

    // Method to show subtitle language options
    private void showSubtitleLanguageDialog() {
        // Example subtitle languages (replace with actual available languages)
        final String[] languages = {"English", "Spanish", "French", "German"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Subtitle Language")
                .setItems(languages, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Handle subtitle language change
                        String selectedLanguage = languages[which];
                        Toast.makeText(VideoPlayerActivity.this, "Subtitle language: " + selectedLanguage, Toast.LENGTH_SHORT).show();
                        // Implement logic to change subtitle language here
                    }
                });
        builder.create().show();
    }

    // Method to open a file chooser for custom subtitle file
    private void openFileChooserForSubtitle() {
        // Open a file picker to allow user to choose a subtitle file (e.g., .srt, .vtt)
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/*"); // Set to text file type (e.g., .srt, .vtt)
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, 1001);  // Request code 1001
    }

    // Handle the result from the file picker (custom subtitle file selection)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            Uri subtitleUri = data.getData();
            if (subtitleUri != null) {
                // Add subtitle to the video
                addCustomSubtitle(subtitleUri);
            }
        }
    }

    // Method to add custom subtitle to video
    private void addCustomSubtitle(Uri subtitleUri) {
        // Logic to add the custom subtitle to the video
        // For example, you could use MediaPlayer to set the subtitle file
        Toast.makeText(this, "Custom subtitle added: " + subtitleUri.getPath(), Toast.LENGTH_SHORT).show();
        // You can now set the subtitle to your VideoView (if using MediaPlayer)
        // videoView.setSubtitleTrack(subtitleUri);  // Replace with your actual subtitle handling logic
    }

    // Hide the status and navigation bar for full-screen mode
    private void hideSystemUI() {
        videoView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }

    // Toggle play/pause
    private void togglePlayPause() {
        if (isPlaying) {
            videoView.pause();
            playPauseButton.setImageResource(R.drawable.ic_play);
        } else {
            videoView.start();
            playPauseButton.setImageResource(R.drawable.ic_pause);
        }
        isPlaying = !isPlaying;
    }

    private void rewindVideo() {
        int currentPosition = videoView.getCurrentPosition();
        videoView.seekTo(currentPosition - 10000); // Rewind by 10 seconds
    }

    private void forwardVideo() {
        int currentPosition = videoView.getCurrentPosition();
        videoView.seekTo(currentPosition + 10000); // Forward by 10 seconds
    }

    private void playPreviousVideo() {
        // Logic to play the previous video
    }

    private void playNextVideo() {
        // Logic to play the next video
    }

    // Helper method to format time in hh:mm:ss
    private String formatTime(int timeInMillis) {
        int hours = timeInMillis / 3600000;
        int minutes = (timeInMillis % 3600000) / 60000;
        int seconds = (timeInMillis % 60000) / 1000;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    // Update SeekBar and time display
    private void updateSeekBar() {
        final int currentPosition = videoView.getCurrentPosition();
        int duration = videoView.getDuration();

        // Check if the video is playing and hasn't reached the end
        if (isPlaying && currentPosition < duration) {
            // Update the SeekBar's progress
            int progress = (int) ((currentPosition / (float) duration) * 100);
            videoSeekbar.setProgress(progress);

            // Update start time (current position)
            startTime.setText(formatTime(currentPosition));

            // Update end time (total duration)
            endTime.setText(formatTime(duration));

            // Post next update every second
            videoSeekbar.postDelayed(this::updateSeekBar, 1000); // Update every second
        }
    }


    private void toggleControlsVisibility() {
        // Check if any of the views are currently visible
        boolean areControlsVisible = playerControls.getVisibility() == View.VISIBLE ||
                videoSeekbar.getVisibility() == View.VISIBLE ||
                topbar.getVisibility() == View.VISIBLE ||
                startTime.getVisibility() == View.VISIBLE ||
                endTime.getVisibility() == View.VISIBLE;

        // If any controls are visible, hide all, else show all
        if (areControlsVisible) {
            playerControls.setVisibility(View.INVISIBLE);
            videoSeekbar.setVisibility(View.INVISIBLE);
            topbar.setVisibility(View.INVISIBLE);
            startTime.setVisibility(View.INVISIBLE);
            endTime.setVisibility(View.INVISIBLE);
        } else {
            playerControls.setVisibility(View.VISIBLE);
            videoSeekbar.setVisibility(View.VISIBLE);
            topbar.setVisibility(View.VISIBLE);
            startTime.setVisibility(View.VISIBLE);
            endTime.setVisibility(View.VISIBLE);
        }
    }

    private void hideControls() {
        playerControls.setVisibility(View.INVISIBLE);
        videoSeekbar.setVisibility(View.INVISIBLE);
                topbar.setVisibility(View.INVISIBLE);
        startTime.setVisibility(View.INVISIBLE);
                endTime.setVisibility(View.INVISIBLE);
    }


}
