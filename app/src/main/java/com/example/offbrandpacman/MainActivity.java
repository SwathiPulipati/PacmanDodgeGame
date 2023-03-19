package com.example.offbrandpacman;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.widget.FrameLayout;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    GameSurface gameSurface;
    FrameLayout game;
    volatile boolean gameStarted = true;
    volatile int gameTime;
    MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //setContentView(R.layout.activity_main);

        gameSurface = new GameSurface(this);
        game = new FrameLayout(this);

        game.addView(gameSurface);
        setContentView(game);


        mediaPlayer = MediaPlayer.create(this, R.raw.siren);
        mediaPlayer.start();

        gameTime = 60;
        gameSurface.startTimer();

    }

    @Override
    protected void onResume() {
        super.onResume();
        gameSurface.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameSurface.pause();
    }

    public class GameSurface extends SurfaceView implements Runnable, SensorEventListener{

        Thread gameThread;
        SurfaceHolder holder;
        volatile boolean running = false, ghostOnScreen = false, pacmanHit = false, ghostEaten = false, murderTime = false;
        Bitmap pacman, clyde, inky, pinky, blinky, weakened_ghost, chosen_ghost, gain_powers, dead_ghost_points;
        Bitmap d1, d2, d3, d4, d5, d6, d7, d8, d9, d10, d11;
        Bitmap[] ghosts, pacman_on_his_death_arc;
        RectF powersHitbox;
        float pacmanX = 0;
        Paint rectProperty, textProperty, powerProperty;
        float pacmanAccel = 0.0f;
        float ghostX = 0, ghostY = 0;
        float hitPacmanLeft = 0, hitPacmanTop = 0;
        int points = -10, pacman_death_id_sound, pacman_death_sequence, ghost_death_id_sound;
        long startMurderTime;

        int screenWidth, screenHeight;

        public GameSurface(Context context){
            super(context);
            holder = getHolder();
            pacman = BitmapFactory.decodeResource(getResources(), R.drawable.pacman);
            gain_powers = BitmapFactory.decodeResource(getResources(), R.drawable.pacman_gain_powers);


            d1 = BitmapFactory.decodeResource(getResources(), R.drawable.pacman_death_1);
            d2 = BitmapFactory.decodeResource(getResources(), R.drawable.pacman_death_2);
            d3 = BitmapFactory.decodeResource(getResources(), R.drawable.pacman_death_3);
            d4 = BitmapFactory.decodeResource(getResources(), R.drawable.pacman_death_4);
            d5 = BitmapFactory.decodeResource(getResources(), R.drawable.pacman_death_5);
            d6 = BitmapFactory.decodeResource(getResources(), R.drawable.pacman_death_6);
            d7 = BitmapFactory.decodeResource(getResources(), R.drawable.pacman_death_7);
            d8 = BitmapFactory.decodeResource(getResources(), R.drawable.pacman_death_8);
            d9 = BitmapFactory.decodeResource(getResources(), R.drawable.pacman_death_9);
            d10 = BitmapFactory.decodeResource(getResources(), R.drawable.pacman_death_10);
            d11 = BitmapFactory.decodeResource(getResources(), R.drawable.pacman_death_11);
            pacman_on_his_death_arc = new Bitmap[]{d1, d2, d3, d4, d5, d6, d7, d8, d9, d10, d11};

            clyde = BitmapFactory.decodeResource(getResources(), R.drawable.orange_ghost);
            inky = BitmapFactory.decodeResource(getResources(), R.drawable.cyan_ghost);
            pinky = BitmapFactory.decodeResource(getResources(), R.drawable.pink_ghost);
            blinky = BitmapFactory.decodeResource(getResources(), R.drawable.red_ghost);
            weakened_ghost = BitmapFactory.decodeResource(getResources(), R.drawable.weakened_ghost);
            ghosts = new Bitmap[]{clyde, inky, pinky, blinky, weakened_ghost};
            dead_ghost_points = BitmapFactory.decodeResource(getResources(), R.drawable.dead_ghost_points);


            Display screenDisplay = getWindowManager().getDefaultDisplay();
            Point sizeOfScreen = new Point();
            screenDisplay.getSize(sizeOfScreen);

            screenWidth = sizeOfScreen.x;
            screenHeight = sizeOfScreen.y;

            SensorManager sensorManager =  (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            sensorManager.registerListener(this, accelerometerSensor, sensorManager.SENSOR_DELAY_NORMAL);

            rectProperty = new Paint();
            rectProperty.setColor(Color.TRANSPARENT);

            textProperty = new Paint();
            textProperty.setColor(Color.YELLOW);
            textProperty.setTextSize(48f);
            textProperty.setTypeface(ResourcesCompat.getFont(MainActivity.this, R.font.emulogic));

            powerProperty = new Paint();
            powerProperty.setAlpha(255);

            powersHitbox = new RectF(screenWidth-gain_powers.getWidth()-40f, screenHeight-gain_powers.getHeight()-115f, screenWidth-40f, screenHeight-115f);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (System.currentTimeMillis() - startMurderTime >= 25000) {
                if (powersHitbox.contains(event.getX(), event.getY()) && !murderTime) {
                    murderTime = true;
                    startMurderTime = System.currentTimeMillis();
                    powerProperty.setAlpha(100);
                }
                System.out.println("murder time starting now");
            }
            return super.onTouchEvent(event);
        }

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            pacmanAccel = sensorEvent.values[1]*15;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }

        @Override
        public void run() {

            SoundPool soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
            pacman_death_id_sound = soundPool.load(MainActivity.this, R.raw.pacman_death, 1);
            ghost_death_id_sound = soundPool.load(MainActivity.this, R.raw.pacman_eatghost, 1);

            while(running){
                if(!holder.getSurface().isValid())
                    continue;
                Canvas canvas = holder.lockCanvas();
                canvas.drawColor(Color.BLACK);

                if (gameStarted) {
                    if (System.currentTimeMillis() - startMurderTime >= 25000)
                        powerProperty.setAlpha(255);

            // -------- draw ghosts ---------
                    if (!ghostOnScreen) {
                        if (murderTime && System.currentTimeMillis() - startMurderTime > 5000) {
                            murderTime = false;
                            System.out.println("murder time over");
                            mediaPlayer.stop();
                            mediaPlayer = MediaPlayer.create(MainActivity.this, R.raw.siren);
                            mediaPlayer.start();
                        }
                        if (murderTime) {
                            if (!chosen_ghost.equals(weakened_ghost) && !chosen_ghost.equals(dead_ghost_points)){
                                mediaPlayer.stop();
                                mediaPlayer = MediaPlayer.create(MainActivity.this, R.raw.pacman_murder_mode_audio);
                                mediaPlayer.start();
                            }
                            chosen_ghost = ghosts[4];
                        }
                        else
                            chosen_ghost = ghosts[(int) (Math.random() * 4)];
                        ghostX = (float) (Math.random() * (screenWidth - chosen_ghost.getWidth()));

                        if (ghostEaten)
                            points += 50;
                        else if (!pacmanHit)
                            points += 10;

                        pacman_death_sequence = 0;
                        pacmanHit = false;
                        ghostEaten = false;
                        ghostOnScreen = true;
                    }
                    RectF ghostHitbox = new RectF(ghostX, -chosen_ghost.getHeight() + ghostY, ghostX + chosen_ghost.getWidth(), -chosen_ghost.getHeight() + ghostY + chosen_ghost.getHeight());

                    canvas.drawRect(ghostHitbox, rectProperty);
                    canvas.drawBitmap(chosen_ghost, ghostX, -chosen_ghost.getHeight() + ghostY, null);
                    ghostY += 20;

                    if (ghostY >= screenHeight) {
                        ghostY = 0;
                        ghostOnScreen = false;
                    }

            // ------- assets --------
                    canvas.drawText("Points: " + points, 10f, 64f, textProperty);
                    canvas.drawText("Time: " +gameTime, screenWidth-425f, 64f, textProperty);
                    canvas.drawBitmap(gain_powers, screenWidth-gain_powers.getWidth()-40f, screenHeight-gain_powers.getHeight()-115f, powerProperty);
                    canvas.drawRect(powersHitbox, rectProperty);

            // ------- draw pacman -----------
                    float pacmanLeft = (screenWidth / 2f) - pacman.getWidth() / 2f + pacmanX;
                    float pacmanTop = (screenHeight / 2f) + pacman.getHeight();

            // ------- draw pacman hitbox -------
                    RectF pacmanHitbox = new RectF(pacmanLeft, pacmanTop, pacmanLeft + pacman.getWidth(), pacmanTop + pacman.getHeight());
                    canvas.drawRoundRect(pacmanHitbox, 150f, 150f, rectProperty);
            // ----------------------------------
                    if (pacmanHit) {
                        pacmanDeathSequence(canvas);
                        holder.unlockCanvasAndPost(canvas);
                        continue;
                    }

                    canvas.drawBitmap(pacman, pacmanLeft, pacmanTop, null);

                    if (Math.abs(pacmanX) <= screenWidth / 2f - pacman.getWidth() / 2f)
                        pacmanX += pacmanAccel;
                    if (Math.abs(pacmanX) >= screenWidth / 2f - pacman.getWidth() / 2f) {
                        if (pacmanX > 0f)
                            pacmanX = screenWidth / 2f - pacman.getWidth() / 2f;
                        else if (pacmanX < 0f)
                            pacmanX = -(screenWidth / 2f - pacman.getWidth() / 2f);
                    }

            //  ------- check if pacman hit -----------
                    if (ghostHitbox.intersect(pacmanHitbox)) {
                        if (chosen_ghost.equals(weakened_ghost)) {
                            ghostEaten = true;
                            chosen_ghost = dead_ghost_points;
                            soundPool.play(ghost_death_id_sound, 1, 1, 1, 0, 1);
                        }
                        else if (!chosen_ghost.equals(dead_ghost_points)){
                            pacmanHit = true;
                            hitPacmanLeft = pacmanLeft;
                            hitPacmanTop = pacmanTop;
                            soundPool.play(pacman_death_id_sound, 1, 1, 1, 0, 1);
                        }
                    }
                }
                holder.unlockCanvasAndPost(canvas);
            } // ------- while running loop --------
        }



        private void pacmanDeathSequence(Canvas canvas) {
            int quirky = pacman_death_sequence/3;
            if (quirky > 10)
                quirky = 10;
            canvas.drawBitmap(pacman_on_his_death_arc[quirky], hitPacmanLeft, hitPacmanTop, null);
            pacman_death_sequence++;
        }

        public void resume(){
            running = true;
            gameThread = new Thread(this);
            gameThread.start();
            if (gameStarted)
                mediaPlayer.start();

        }

        public void pause(){
            running = false;
            while(running){
                try{
                    gameThread.join();
                }catch (InterruptedException e){}
            }
            if(mediaPlayer.isPlaying())
                mediaPlayer.pause();
        }

        public void startTimer(){
            Timer timer = new Timer();
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    if (gameTime > 0){
                        gameTime--;
                    }

                    if (gameTime < 1){
                        cancel();
                        gameStarted = false;
                        endScreen(points);
                        try {
                            mediaPlayer.prepare();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                }// end of run method of timer task
            }; // end of timer task
            timer.schedule(timerTask, 0, 1000);
        }
    }


    public void endScreen(int points){
        Intent intentToLoad = new Intent(MainActivity.this,gameOverScreen.class);
        intentToLoad.putExtra("points", points);
        startActivity(intentToLoad);
    }
}