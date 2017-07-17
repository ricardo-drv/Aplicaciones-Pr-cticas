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

import android.Manifest;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.example.android.shushme.provider.PlaceContract;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    // Constants
    public static final String TAG = MainActivity.class.getSimpleName();
    private static final int PERMISSIONS_REQUEST_FINE_LOCATION = 111;
    private static final int PLACE_PICKER_REQUEST = 1;

    // Member variables
    private PlaceListAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private boolean mIsEnabled;
    private GoogleApiClient mClient;
    private Geofencing mGeofencing;

    /**
     * Called when the activity is starting
     *
     * @param savedInstanceState The Bundle that contains the data supplied in onSaveInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Timber.plant(new Timber.DebugTree());

        // Set up the recycler view
        mRecyclerView = (RecyclerView) findViewById(R.id.places_list_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new PlaceListAdapter(this, null);
        mRecyclerView.setAdapter(mAdapter);

        Switch onOffSwitch =  (Switch) findViewById(R.id.enable_switch);
        mIsEnabled = getPreferences(MODE_PRIVATE).getBoolean(getString(R.string.setting_enabled),false);
        onOffSwitch.setChecked(mIsEnabled);
        onOffSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){

            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
                editor.putBoolean(getString(R.string.setting_enabled),b);
                mIsEnabled = b;
                editor.commit();
                Timber.d("Switch: " + mIsEnabled);
                if (b) mGeofencing.registerAllGeofences();
                else mGeofencing.unRegisterAllGeofences();

            }
        });




         mClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .enableAutoManage(this,this)
                .build();

        mGeofencing = new Geofencing(this,mClient);



    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Timber.d("Conectado...");

        refreshPlacesData();

    }

    @Override
    public void onConnectionSuspended(int i) {
        Timber.d("Conexión suspendida!");

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Timber.d("Conexión fallida!!!");

    }
    public void refreshPlacesData(){
        Uri uri = PlaceContract.PlaceEntry.CONTENT_URI;
        Cursor data = getContentResolver().query(uri,null,null,null,null);

        if (data == null || data.getCount() == 0)
            return;

        List<String> guids = new ArrayList<String>();
        while (data.moveToNext()){
            guids.add(data.getString(data.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_ID)));
        }
        PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi.getPlaceById(mClient,guids.toArray(new String[guids.size()]));

        placeResult.setResultCallback(new ResultCallback<PlaceBuffer>() {
            @Override
            public void onResult(@NonNull PlaceBuffer places) {
                mAdapter.swapPlaces(places);

                mGeofencing.updateGeofencesList(places);
                if (mIsEnabled) mGeofencing.registerAllGeofences();
                Timber.d("sitios Actualizados");
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        CheckBox checkBox = (CheckBox) findViewById(R.id.location_permissions_checkbox);

        //comprobación de permisos
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            checkBox.setChecked(false);

        }else {
            checkBox.setChecked(true);
            checkBox.setEnabled(false);
        }

        //ringer
        CheckBox ringerBox = (CheckBox) findViewById(R.id.ringer_permissions_checkbox);
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N && !manager.isNotificationPolicyAccessGranted()){//version del dispositivo
            ringerBox.setChecked(false);
        }else {
            ringerBox.setChecked(true);
            ringerBox.setEnabled(false);
        }
    }

    public void onLocationPermissionClicked(View view) {
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},PERMISSIONS_REQUEST_FINE_LOCATION);
    }

    /**
     * Button Add new Location
     * @param view
     */
    public void onAddPlaceButtonClicked(View view) {
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            Toast.makeText(this,getString(R.string.need_location_permission_message),Toast.LENGTH_SHORT).show();

        }else {
            Toast.makeText(this,getString(R.string.location_permissions_granted_message),Toast.LENGTH_SHORT).show();

            try {

                PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
                Intent intent = null;
                intent = builder.build(this);
                startActivityForResult(intent,PLACE_PICKER_REQUEST);

            } catch (GooglePlayServicesRepairableException e) {
                Timber.d("Error con google play Service",e);
                e.printStackTrace();
            } catch (GooglePlayServicesNotAvailableException e) {
                Timber.d("No disponible google play Service",e);
                e.printStackTrace();

            }




        }
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_PICKER_REQUEST && resultCode == RESULT_OK){
            Place place = PlacePicker.getPlace(this,data);
            if (place == null){
                Timber.d("Sitio no selecionado");
                return;
            }

            String placeName = place.getName().toString();
            Timber.d("location Name: " + placeName);
            String placeAddress = place.getAddress().toString();
            Timber.d("Dirección : " + placeAddress);
            String placeId = place.getId();
            Timber.d("ID : " + placeId);

            //añadimos a DB
            //Solo se nos permite añadir a base de datos la Id, direcciones NO!!!
            //cita: in the Terms of Service, you are not allowed to cache any data from those Places API for more than 30 days
            ContentValues contentValues = new ContentValues();

            contentValues.put(PlaceContract.PlaceEntry.COLUMN_PLACE_ID,placeId);
            getContentResolver().insert(PlaceContract.PlaceEntry.CONTENT_URI,contentValues);

            refreshPlacesData();

        }


    }

    public void onRingerPermissionClicked(View view) {
        Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
        startActivity(intent);
    }
}//fin clase
