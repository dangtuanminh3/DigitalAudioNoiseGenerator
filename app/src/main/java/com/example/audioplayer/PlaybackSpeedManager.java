package com.example.audioplayer;

import androidx.media3.common.PlaybackParameters;
import androidx.media3.exoplayer.ExoPlayer;

public class PlaybackSpeedManager {
    private float speed = 1.0f;
    private float pitch = 1.0f;

    private final ExoPlayer player;

    public PlaybackSpeedManager(ExoPlayer player) {
        this.player = player;
    }

    public void setPlaybackSpeed(float speedIn) {
        speed = speedIn;
        PlaybackParameters playbackParameters = new PlaybackParameters(speed, pitch);
        player.setPlaybackParameters(playbackParameters);
    }

    public void setPlaybackPitch(float pitchIn) {
        pitch = pitchIn;
        PlaybackParameters playbackParameters = new PlaybackParameters(speed, pitch);
        player.setPlaybackParameters(playbackParameters);
    }

    public int getPlaybackSpeed() {
        return (int) (speed * 10f);
    }

    public int getPlaybackPitch() {
        return (int) (pitch * 10f);
    }

}
