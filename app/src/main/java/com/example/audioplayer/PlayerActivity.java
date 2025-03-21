package com.example.audioplayer;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

@UnstableApi
public class PlayerActivity extends AppCompatActivity {
    private static ArrayList<String> queue;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int repeatState;
    private int shuffleState;
    private ExoPlayer player;
    private TextView songTitle, artistName, currentTime, totalTime;
    private SeekBar seekBar;
    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            if (player != null && player.isPlaying()) {
                long currentPosition = player.getCurrentPosition();
                seekBar.setProgress((int) currentPosition);
                currentTime.setText(formatTime(currentPosition));
                handler.postDelayed(this, 1000);
            }
        }
    };
    private ImageButton btnNext, btnPrev, btnShuffle, btnRepeat;
    private GifImageView btnPlayPause;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        repeatState = 0;
        shuffleState = 0;
        findViewById(R.id.player_view);
        PlayerView playerView;
        songTitle = findViewById(R.id.song_title);
        artistName = findViewById(R.id.artist_name);
        seekBar = findViewById(R.id.seek_bar);
        currentTime = findViewById(R.id.current_time);
        totalTime = findViewById(R.id.total_time);
        btnPlayPause = findViewById(R.id.btn_play_pause);
        btnNext = findViewById(R.id.btn_next);
        btnPrev = findViewById(R.id.btn_prev);
        btnShuffle = findViewById(R.id.btn_shuffle);
        btnRepeat = findViewById(R.id.btn_repeat);

        GifDrawable playGif;
        GifDrawable pauseGif;
        try {
            playGif = new GifDrawable(getResources(), R.drawable.play);
            pauseGif = new GifDrawable(getResources(), R.drawable.pause);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        GifDrawable finalPlayGif = playGif;
        GifDrawable finalPauseGif = pauseGif;


        player = PlayerManager.getPlayer(this);
        playerView = findViewById(R.id.player_view);
        playerView.setPlayer(player);

        ImageButton backButton = findViewById(R.id.back_button);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("SONGS")) {
            queue = intent.getStringArrayListExtra("SONGS");
            PlayerManager.playSong(queue);
        }

        backButton.setOnClickListener(v -> {
            finish(); //exits
        });

        btnPlayPause.setOnClickListener(v -> {
            if (player.isPlaying()) {
                player.pause();
                btnPlayPause.setImageDrawable(finalPauseGif);
                handler.removeCallbacks(updateRunnable);
                finalPauseGif.start();

            } else {
                player.play();
                startUpdatingSeekBar();
                btnPlayPause.setImageDrawable(finalPlayGif);
                finalPlayGif.start();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (player.getCurrentMediaItemIndex() == queue.size() - 1) {
                player.seekTo(0, 0);
                player.play();
            } else {
                player.seekToNextMediaItem();
            }
            updateSongHighlight();
        });

        btnPrev.setOnClickListener(v -> {
            if ((player.getCurrentMediaItemIndex() == 0) && player.getCurrentPosition() < 7000) {
                int lastIndex = queue.size() - 1;
                player.seekTo(lastIndex, 0);
                player.play();
            } else if (player.getCurrentPosition() < 7000) {
                player.seekToPreviousMediaItem();
            } else {
                player.seekTo(0);
            }
            updateSongHighlight();
        });


        btnRepeat.setOnClickListener(v -> {
            repeatState += 1;
            if (repeatState == 1) {
                player.setRepeatMode(Player.REPEAT_MODE_ALL);
                btnRepeat.setImageResource(R.drawable.repeat);
                Toast.makeText(this, "Repeat queue", Toast.LENGTH_SHORT).show();
            } else if (repeatState == 2) {
                player.setRepeatMode(Player.REPEAT_MODE_ONE);
                btnRepeat.setImageResource(R.drawable.repeat_one);
                Toast.makeText(this, "Repeat song", Toast.LENGTH_SHORT).show();
            } else {
                player.setRepeatMode(Player.REPEAT_MODE_OFF);
                repeatState = 0;
                btnRepeat.setImageResource(R.drawable.order);
                Toast.makeText(this, "Repeat off", Toast.LENGTH_SHORT).show();

            }
        });

        btnShuffle.setOnClickListener(v -> {
            shuffleState += 1;
            if (shuffleState == 1) {
                player.setShuffleModeEnabled(true);
                btnShuffle.setImageResource(R.drawable.shuffle);
                btnShuffle.setColorFilter(Color.argb(255, 255, 255, 255));
                Toast.makeText(this, "Shuffle ON", Toast.LENGTH_SHORT).show();
            } else {
                player.setShuffleModeEnabled(false);
                btnRepeat.setImageResource(R.drawable.order);
                btnShuffle.setColorFilter(Color.argb(255, 186, 186, 186));
                shuffleState = 0;
                Toast.makeText(this, "Shuffle OFF", Toast.LENGTH_SHORT).show();
            }
        });

        player.addListener(new androidx.media3.common.Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == androidx.media3.common.Player.STATE_READY) {
                    String songPath = queue.get(player.getCurrentMediaItemIndex());
                    File file = new File(songPath);
                    String songName = file.getName();
                    songTitle.setText(songName.replaceFirst("[.][^.]+$", ""));
                    artistName.setText("Unknown Artist");
                    long duration = player.getDuration();
                    seekBar.setMax((int) duration);
                    totalTime.setText(formatTime(duration));
                    startUpdatingSeekBar();
                    updateSongHighlight();
                }
                if (state == androidx.media3.common.Player.STATE_ENDED) {
                    handler.removeCallbacks(updateRunnable);
                }
            }

            public void onMediaItemTransition(MediaItem mediaItem, int reason) {
                String songPath = queue.get(player.getCurrentMediaItemIndex());
                File file = new File(songPath);
                String songName = file.getName();
                songTitle.setText(songName.replaceFirst("[.][^.]+$", ""));
                artistName.setText("Unknown Artist");
                long duration = player.getDuration();
                seekBar.setMax((int) duration);
                totalTime.setText(formatTime(duration));
                startUpdatingSeekBar();
                updateSongHighlight();
            }

        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    player.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

    }

    private void updateSongHighlight() {
        if (player != null) {
            String currentSongPath = queue.get(player.getCurrentMediaItemIndex());
            Log.d("DEBUG", "Updating highlight for: " + currentSongPath);

            // Send the new song path back to MainActivity
            Intent intent = new Intent();
            intent.putExtra("CURRENT_SONG", currentSongPath);
            setResult(RESULT_OK, intent);
        }
    }

    private void startUpdatingSeekBar() {
        handler.post(updateRunnable);
    }

    private String formatTime(long millis) {
        return String.format("%d:%02d", TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) % 60);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //not released because we want it to keep playing!!
    }
}
