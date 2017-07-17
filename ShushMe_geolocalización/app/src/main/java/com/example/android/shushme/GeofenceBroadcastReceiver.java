package com.example.android.shushme;

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

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import timber.log.Timber;

public class GeofenceBroadcastReceiver  extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {
        Timber.d("onReceive Triggered");

        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            Timber.e("Error en " + geofencingEvent.getErrorCode());
            return;
        }
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER){
            setRingerMode(context,AudioManager.RINGER_MODE_SILENT);
        }else if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT){
            setRingerMode(context,AudioManager.RINGER_MODE_NORMAL);
        }else {
            Timber.e("transition desconocido: " + geofenceTransition);
            return;
        }
        sendNotification(context,geofenceTransition);

    }

    private void sendNotification(Context context, int geofenceTransition) {
        Intent notificationIntent = new Intent(context,MainActivity.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MainActivity.class);

        stackBuilder.addNextIntent(notificationIntent);

        PendingIntent pendingIntent = stackBuilder.getPendingIntent(0,PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER){

            builder.setSmallIcon(R.drawable.ic_volume_off_white_24dp)
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),R.drawable.ic_volume_off_white_24dp))
                    .setContentTitle(context.getString(R.string.silent_mode_activated));

        }else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT){

            builder.setSmallIcon(R.drawable.ic_volume_up_white_24dp)
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),R.drawable.ic_volume_up_white_24dp))
                    .setContentTitle(context.getString(R.string.back_to_normal));
        }

        builder.setContentText(context.getString(R.string.touch_to_relaunch));
        builder.setContentIntent(pendingIntent);

        builder.setAutoCancel(true);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        manager.notify(0,builder.build());


    }

    private void setRingerMode(Context context, int mode){
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !manager.isNotificationPolicyAccessGranted())){
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            audioManager.setRingerMode(mode);

        }


    }
}//fin clase
