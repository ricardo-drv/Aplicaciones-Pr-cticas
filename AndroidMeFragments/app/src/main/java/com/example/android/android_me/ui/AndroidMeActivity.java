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

package com.example.android.android_me.ui;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import com.example.android.android_me.R;
import com.example.android.android_me.data.AndroidImageAssets;

// This activity will display a custom Android image composed of three body parts: head, body, and legs
public class AndroidMeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_android_me);
        if (savedInstanceState == null) {//el rotate

            int headIndex = getIntent().getIntExtra("headIndex",0);
            int bodyIndex = getIntent().getIntExtra("bodyIndex",0);
            int legIndex = getIntent().getIntExtra("legIndex",0);
            BodyPartFragment headBodyPartFragment = new BodyPartFragment();
            headBodyPartFragment.setmImagesIds(AndroidImageAssets.getHeads());
            headBodyPartFragment.setmListIndex(headIndex);

            FragmentManager fm = getSupportFragmentManager();
            fm.beginTransaction()
                    .add(R.id.head_container, headBodyPartFragment)
                    .commit();

            BodyPartFragment bodyPartFragment = new BodyPartFragment();
            bodyPartFragment.setmImagesIds(AndroidImageAssets.getBodies());
            bodyPartFragment.setmListIndex(bodyIndex);
            fm.beginTransaction()
                    .add(R.id.body_container, bodyPartFragment)
                    .commit();

            BodyPartFragment legsPartFragment = new BodyPartFragment();
            legsPartFragment.setmImagesIds(AndroidImageAssets.getLegs());
            legsPartFragment.setmListIndex(legIndex);
            fm.beginTransaction()
                    .add(R.id.leg_container, legsPartFragment)
                    .commit();
        }



    }
}
