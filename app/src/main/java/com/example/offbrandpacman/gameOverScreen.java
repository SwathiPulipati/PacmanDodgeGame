package com.example.offbrandpacman;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class gameOverScreen extends AppCompatActivity {
    TextView pointsDisplay;
    MediaPlayer mediaPlayer;
    Button playAgain;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_over_screen);

        pointsDisplay = findViewById(R.id.pointsDisplay);
        playAgain = findViewById(R.id.button);

        Bundle extras = getIntent().getExtras();
        int points = extras.getInt("points");

        pointsDisplay.setText("Points: "+points);

        mediaPlayer = MediaPlayer.create(this, R.raw.pacman_intermission);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();

        playAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(gameOverScreen.this, StartScreen.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onPause() {
        mediaPlayer.pause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        mediaPlayer.start();
        super.onResume();
    }
}