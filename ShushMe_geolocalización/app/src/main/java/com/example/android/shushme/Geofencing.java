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

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class Geofencing implements ResultCallback<Status> {

    private static final long GEOFENCE_EXPIRAR = 1000 * 60 * 60 * 24;
    private static final float GEOFENCE_RADIUS = 50;
    private Context context;
    private GoogleApiClient googleApiClient;

    private List<Geofence> geofenceList;
    private PendingIntent pendingIntent;


    //constructor
    public Geofencing(Context context, GoogleApiClient googleApiClient) {
        this.context = context;
        this.googleApiClient = googleApiClient;
        pendingIntent = null;
        geofenceList = new ArrayList<>();
    }

    public void registerAllGeofences(){
        if (googleApiClient == null || !googleApiClient.isConnected()
                || geofenceList == null || geofenceList.size() == 0){
            return;
        }

        try {
            LocationServices.GeofencingApi.addGeofences(
                    googleApiClient,getGeofencingRequest(),getGeofencePendingIntent()
            ).setResultCallback(this);
            Timber.d("registerAllGeofences");
        }catch (SecurityException e){
            Timber.e(e);
        }
    }
    public void unRegisterAllGeofences(){
        if (googleApiClient == null || !googleApiClient.isConnected()){
            return;
        }

        try {
            LocationServices.GeofencingApi.removeGeofences(
                    googleApiClient,getGeofencePendingIntent()
            ).setResultCallback(this);
            Timber.d("UNregisterAllGeofences");
        }catch (SecurityException e){
            Timber.e(e);
        }
    }

    public void updateGeofencesList (PlaceBuffer placeBuffer){
        geofenceList = new ArrayList<>();

        if (placeBuffer == null || placeBuffer.getCount() == 0) return;

        Timber.d("updateGeofencesList: " + placeBuffer.getCount());
        for (Place place: placeBuffer){
            String placeUID = place.getId();
            double placeLat = place.getLatLng().latitude;
            double placeLon = place.getLatLng().longitude;

            Geofence geofence = new Geofence.Builder()
                    .setRequestId(placeUID)
                    .setExpirationDuration(GEOFENCE_EXPIRAR)
                    .setCircularRegion(placeLat,placeLon,GEOFENCE_RADIUS)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build();

            geofenceList.add(geofence);
        }//fin for
    }


    private GeofencingRequest getGeofencingRequest(){
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(geofenceList);
        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent(){
        if (pendingIntent != null){
            return pendingIntent;
        }
        Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(context,0,intent, PendingIntent.FLAG_UPDATE_CURRENT);

        return pendingIntent;
    }


    @Override
    public void onResult(@NonNull Status status) {

        Timber.d("on result: " + status.getStatusMessage() + status.getStatus().toString());

    }
}
