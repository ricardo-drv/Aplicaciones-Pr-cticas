/*
* Copyright (C) 2017 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*  	http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.example.android.classicalmusicquiz;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.LoopingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.util.ArrayList;

import timber.log.Timber;

import static com.example.android.classicalmusicquiz.R.string.play;

public class QuizActivity extends AppCompatActivity implements View.OnClickListener,ExoPlayer.EventListener {

    private static final int CORRECT_ANSWER_DELAY_MILLIS = 1000;
    private static final String REMAINING_SONGS_KEY = "remaining_songs";
    private int[] mButtonIDs = {R.id.buttonA, R.id.buttonB, R.id.buttonC, R.id.buttonD};
    private ArrayList<Integer> mRemainingSampleIDs;
    private ArrayList<Integer> mQuestionSampleIDs;
    private int mAnswerSampleID;
    private int mCurrentScore;
    private int mHighScore;
    private Button[] mButtons;

    private SimpleExoPlayerView  playerView;
    private static SimpleExoPlayer exoPlayer;

    private static MediaSessionCompat sessionCompat;
    private PlaybackStateCompat.Builder playbackBuilder;

    private NotificationManager notificationManager;
    private HeadsetReceiver headsetReceiver;

    private static boolean isKilled = false;//controla si hemos ordenado destruir la actividad, porque se esta llamando onCreate antes que onDestroy

    private static boolean playState;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);
      //  Timber.plant(new Timber.DebugTree());

        Timber.d("onCreate");

        playerView = (SimpleExoPlayerView) findViewById(R.id.playerView);

        boolean isNewGame = !getIntent().hasExtra(REMAINING_SONGS_KEY);

        // If it's a new game, set the current score to 0 and load all samples.
        if (isNewGame) {
            QuizUtils.setCurrentScore(this, 0);
            mRemainingSampleIDs = Sample.getAllSampleIDs(this);
            // Otherwise, get the remaining songs from the Intent.
        } else {
            mRemainingSampleIDs = getIntent().getIntegerArrayListExtra(REMAINING_SONGS_KEY);
        }

        if (mRemainingSampleIDs.size() > 0) {

            // Get current and high scores.
            mCurrentScore = QuizUtils.getCurrentScore(this);
            mHighScore = QuizUtils.getHighScore(this);

            // Generate a question and get the correct answer.
            mQuestionSampleIDs = QuizUtils.generateQuestion(mRemainingSampleIDs);
            mAnswerSampleID = QuizUtils.getCorrectAnswerID(mQuestionSampleIDs);

            // Load the image of the composer for the answer into the ImageView.

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4;
            playerView.setDefaultArtwork(BitmapFactory.decodeResource(getResources(), R.drawable.cover, options));

            // If there is only one answer left, end the game.
            if (mQuestionSampleIDs.size() < 2) {
                QuizUtils.endGame(this);
                isKilled = true;
                finish();
                return;
            }


            // Initialize the buttons with the composers names.
            mButtons = initializeButtons(mQuestionSampleIDs);

            arrancarMediaSesion();

            Sample answersample = Sample.getSampleByID(this, mAnswerSampleID);
            if (answersample == null) {
                Toast.makeText(this, R.string.sample_list_load_error, Toast.LENGTH_SHORT).show();
                return;
            }
            arrancarExoplayer(Uri.parse(answersample.getUri()));


            //plug auriculares

            IntentFilter reciveFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
            headsetReceiver = new HeadsetReceiver();
            registerReceiver(headsetReceiver, reciveFilter);
        }

    }

    private void arrancarMediaSesion() {
        sessionCompat = new MediaSessionCompat(this,getPackageName().getClass().getName());

        sessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);//callbacks de agentes externos

        sessionCompat.setMediaButtonReceiver(null);

        playbackBuilder = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY |
                PlaybackStateCompat.ACTION_PAUSE |
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                PlaybackStateCompat.ACTION_PLAY_PAUSE);

        sessionCompat.setPlaybackState(playbackBuilder.build());

        sessionCompat.setCallback(new MySessionCallback());

        sessionCompat.setActive(true);

    }

    private void arrancarExoplayer(Uri uri){
        if (exoPlayer == null){
            TrackSelector trackSelector = new DefaultTrackSelector();
            LoadControl loadControl = new DefaultLoadControl();
             exoPlayer = ExoPlayerFactory.newSimpleInstance(this,trackSelector,loadControl);
            playerView.setPlayer(exoPlayer);

            exoPlayer.addListener(this);

            String userAgent = Util.getUserAgent(this,"ClassicalMusicalQuiz");
            MediaSource mediaSource = new ExtractorMediaSource(uri, new DefaultDataSourceFactory(this,userAgent),new DefaultExtractorsFactory(),null,null);
            LoopingMediaSource loopingMediaSource = new LoopingMediaSource(mediaSource);//opcion para crear un looping infinito del audio
            exoPlayer.prepare(loopingMediaSource);
            exoPlayer.setPlayWhenReady(true);
            Timber.d("Exoplayer Arrancado");

            setVolumeControlStream(AudioManager.STREAM_MUSIC);

        }
    }


    /**
     * Initializes the button to the correct views, and sets the text to the composers names,
     * and set's the OnClick listener to the buttons.
     *
     * @param answerSampleIDs The IDs of the possible answers to the question.
     * @return The Array of initialized buttons.
     */
    private Button[] initializeButtons(ArrayList<Integer> answerSampleIDs) {
        Button[] buttons = new Button[mButtonIDs.length];
        for (int i = 0; i < answerSampleIDs.size(); i++) {
            Button currentButton = (Button) findViewById(mButtonIDs[i]);
            Sample currentSample = Sample.getSampleByID(this, answerSampleIDs.get(i));
            buttons[i] = currentButton;
            currentButton.setOnClickListener(this);
            if (currentSample != null) {
                currentButton.setText(currentSample.getComposer());
            }
        }
        return buttons;
    }


    /**
     * The OnClick method for all of the answer buttons. The method uses the index of the button
     * in button array to to get the ID of the sample from the array of question IDs. It also
     * toggles the UI to show the correct answer.
     *
     * @param v The button that was clicked.
     */
    @Override
    public void onClick(View v) {

        // Show the correct answer.
        showCorrectAnswer();

        // Get the button that was pressed.
        Button pressedButton = (Button) v;

        // Get the index of the pressed button
        int userAnswerIndex = -1;
        for (int i = 0; i < mButtons.length; i++) {
            if (pressedButton.getId() == mButtonIDs[i]) {
                userAnswerIndex = i;
            }
        }

        // Get the ID of the sample that the user selected.
        int userAnswerSampleID = mQuestionSampleIDs.get(userAnswerIndex);

        // If the user is correct, increase there score and update high score.
        if (QuizUtils.userCorrect(mAnswerSampleID, userAnswerSampleID)) {
            mCurrentScore++;
            QuizUtils.setCurrentScore(this, mCurrentScore);
            if (mCurrentScore > mHighScore) {
                mHighScore = mCurrentScore;
                QuizUtils.setHighScore(this, mHighScore);
            }
        }

        // Remove the answer sample from the list of all samples, so it doesn't get asked again.
        mRemainingSampleIDs.remove(Integer.valueOf(mAnswerSampleID));

        // Wait some time so the user can see the correct answer, then go to the next question.
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent nextQuestionIntent = new Intent(QuizActivity.this, QuizActivity.class);
                nextQuestionIntent.putExtra(REMAINING_SONGS_KEY, mRemainingSampleIDs);
                isKilled = true;
                finish();
                nextQuestionIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(nextQuestionIntent);
            }
        }, CORRECT_ANSWER_DELAY_MILLIS);

    }

    /**
     * Disables the buttons and changes the background colors to show the correct answer.
     */
    private void showCorrectAnswer() {
        for (int i = 0; i < mQuestionSampleIDs.size(); i++) {
            int buttonSampleID = mQuestionSampleIDs.get(i);

            mButtons[i].setEnabled(false);
            if (buttonSampleID == mAnswerSampleID) {
                mButtons[i].getBackground().setColorFilter(ContextCompat.getColor
                                (this, android.R.color.holo_green_light),
                        PorterDuff.Mode.MULTIPLY);
                mButtons[i].setTextColor(Color.WHITE);
            } else {
                mButtons[i].getBackground().setColorFilter(ContextCompat.getColor
                                (this, android.R.color.holo_red_light),
                        PorterDuff.Mode.MULTIPLY);
                mButtons[i].setTextColor(Color.WHITE);

            }
        }
    }

    private void showNotification(PlaybackStateCompat state) {
        NotificationCompat.Builder builder =  new NotificationCompat.Builder(this);

        int icon;
        String play_pause;

        if (state.getState() == PlaybackStateCompat.STATE_PLAYING){
            icon = R.drawable.exo_controls_pause;
            play_pause = getString(R.string.pause);
        } else {
            icon = R.drawable.exo_controls_play;
            play_pause = getString(play);
        }
        NotificationCompat.Action playPauseAction = new NotificationCompat.Action(icon,play_pause,
                MediaButtonReceiver.buildMediaButtonPendingIntent(this,PlaybackStateCompat.ACTION_PLAY_PAUSE));

        NotificationCompat.Action restartAction = new NotificationCompat.Action(R.drawable.exo_controls_previous,"Restart",
                MediaButtonReceiver.buildMediaButtonPendingIntent(this,PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS));

        PendingIntent pendingIntent = PendingIntent.getActivity(this,0,new Intent(this,QuizActivity.class),0);

        builder.setContentTitle(getString(R.string.guess))
                .setContentText(getString(R.string.notification_text))
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.music_note)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(restartAction)
                .addAction(playPauseAction)
                .setStyle(new NotificationCompat.MediaStyle()
                        .setMediaSession(sessionCompat.getSessionToken())
                        .setShowActionsInCompactView(0,1)
                        );

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(0,builder.build());
    }



    private void releasePlayer(){
        notificationManager.cancelAll();
        exoPlayer.stop();
        exoPlayer.release();
        exoPlayer = null;
    }

    @Override
    protected void onPause() {
        super.onPause();
        Timber.d("onPause");
        if (isKilled){
            releasePlayer();
            unregisterReceiver(headsetReceiver);//necesario

        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Timber.d("onDestroy");


    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {

    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

    }

    @Override
    public void onLoadingChanged(boolean isLoading) {

    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        if ((playbackState == ExoPlayer.STATE_READY) && playWhenReady){
            Timber.d("Reproduciendo...");
            playbackBuilder.setState(PlaybackStateCompat.STATE_PLAYING,exoPlayer.getCurrentPosition(),1f);

        }else if (playbackState == ExoPlayer.STATE_READY){
            Timber.d("Pausa!");
            playbackBuilder.setState(PlaybackStateCompat.STATE_PAUSED,exoPlayer.getCurrentPosition(),1f);
        }

        sessionCompat.setPlaybackState(playbackBuilder.build());

        showNotification(playbackBuilder.build());

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {

    }

    @Override
    public void onPositionDiscontinuity() {

    }


    private class MySessionCallback extends MediaSessionCompat.Callback {


        @Override
        public void onPlay() {
            Timber.d("PLAY!!");
            exoPlayer.setPlayWhenReady(true);
        }

        @Override
        public void onPause() {
            Timber.d("onPause");
            exoPlayer.setPlayWhenReady(false);
        }

        @Override
        public void onSkipToPrevious() {
            Timber.d("vamos para atrás");
            exoPlayer.seekTo(0);
        }
    }



    public static class MediaReceiver extends BroadcastReceiver{
        public MediaReceiver() {

        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Timber.d("onRecive MediaReceiver");

            //cuando quitamos los auriculares del dispositivo, también se añade en el manifest
            if (intent.getAction() == AudioManager.ACTION_AUDIO_BECOMING_NOISY){//parte replazada por headsetReceiver contempla el plug y unplugged de auricular
                Timber.d("unplugged");
                exoPlayer.setPlayWhenReady(false);
            }else if (intent.getAction() == AudioManager.ACTION_HEADSET_PLUG){
                Timber.d("plugged");
                exoPlayer.setPlayWhenReady(true);
            }else if (intent.getAction() == Intent.ACTION_MEDIA_BUTTON){
                Timber.d("mediaButton Triggered");
                MediaButtonReceiver.handleIntent(sessionCompat,intent);
            }

        }
    }

    //receiver que controla la el plug de los auriculares
    //no necesita declarar en Manifest, tiene que ser en el oncreate
    public static class HeadsetReceiver extends BroadcastReceiver {

        public HeadsetReceiver() {

        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Timber.d("onRecive Headset");
            playState = true;
                if (intent.getAction() == Intent.ACTION_HEADSET_PLUG){
                    if (intent.hasExtra("state")){
                        if (intent.getIntExtra("state",0) == 0){
                            Timber.d("unplugged");
                            if (exoPlayer.getCurrentPosition() == 0){
                                exoPlayer.setPlayWhenReady(true);
                                return;
                            }
                            exoPlayer.setPlayWhenReady(false);

                        }else if (intent.getIntExtra("state",0) == 1){
                            Timber.d("plug headset");
                            exoPlayer.setPlayWhenReady(true);
                        }
                    }

            }
        }
    }
}//fin clase
