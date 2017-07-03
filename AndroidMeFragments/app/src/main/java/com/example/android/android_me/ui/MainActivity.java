package com.example.android.android_me.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.Toast;

import com.example.android.android_me.R;
import com.example.android.android_me.data.AndroidImageAssets;

import static com.example.android.android_me.data.AndroidImageAssets.getHeads;

/**
 * Created by Gato on 28/06/2017.
 *
 * Cosas aprendidas:
 * comunicación fragmento - actividad con interfaces
 * funcionamineto del Gridview
 * savedInstanceState para la rotación de la pantalla
 * soporte para tablets y como utilizarlo
 */

public class MainActivity extends AppCompatActivity implements MasterListFragment.OnImageClickListener {//interface 3

    private int headIndex;
    private int bodyIndex;
    private int legIndex;

    private boolean twoPane;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //comprobación de pantalla
        if (findViewById(R.id.android_me_linear_layout) != null){
            twoPane = true;

            if (savedInstanceState == null) {

                Button nextButton = (Button) findViewById(R.id.next_button);
                nextButton.setVisibility(View.GONE);

                GridView gridView = (GridView) findViewById(R.id.images_grid_view);
                gridView.setNumColumns(2);

                BodyPartFragment headfragment = new BodyPartFragment();
                headfragment.setmImagesIds(getHeads());
                //headfragment.setmListIndex(headIndex);

                FragmentManager fm = getSupportFragmentManager();
                fm.beginTransaction()
                        .add(R.id.head_container, headfragment)
                        .commit();

                BodyPartFragment bodyPartFragment = new BodyPartFragment();
                bodyPartFragment.setmImagesIds(AndroidImageAssets.getBodies());
                //bodyPartFragment.setmListIndex(bodyIndex);
                fm.beginTransaction()
                        .add(R.id.body_container, bodyPartFragment)
                        .commit();

                BodyPartFragment legsPartFragment = new BodyPartFragment();
                legsPartFragment.setmImagesIds(AndroidImageAssets.getLegs());
                //legsPartFragment.setmListIndex(legIndex);
                fm.beginTransaction()
                        .add(R.id.leg_container, legsPartFragment)
                        .commit();
            }


        }else {
            twoPane = false;
        }

    }

    @Override
    public void onImageSelected(int position) {
        Toast.makeText(this,"click position: " + position, Toast.LENGTH_LONG).show();

        int bodyPartNumber = position / 12;

        int listIndex = position - 12 * bodyPartNumber;


        if (twoPane){//tablet

            BodyPartFragment newFragment = new BodyPartFragment();

            switch (bodyPartNumber){
                case 0:
                    newFragment.setmImagesIds(getHeads());
                    newFragment.setmListIndex(listIndex);

                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.head_container, newFragment).commit();
                    break;
                case 1:
                    newFragment.setmImagesIds(AndroidImageAssets.getBodies());
                    newFragment.setmListIndex(listIndex);

                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.body_container, newFragment).commit();
                    break;
                case 2:
                    newFragment.setmImagesIds(AndroidImageAssets.getLegs());
                    newFragment.setmListIndex(listIndex);

                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.leg_container, newFragment).commit();
                    break;
            }


        }else {

            switch (bodyPartNumber) {
                case 0:
                    headIndex = listIndex;
                    break;
                case 1:
                    bodyIndex = listIndex;
                    break;
                case 2:
                    legIndex = listIndex;
                    break;
                default:
                    break;
            }

            //pasamos las variables a androidmeActiity
            Bundle bundle = new Bundle();

            bundle.putInt("headIndex", headIndex);
            bundle.putInt("bodyIndex", bodyIndex);
            bundle.putInt("legIndex", legIndex);

            final Intent intent = new Intent(this, AndroidMeActivity.class);

            intent.putExtras(bundle);

            Button nextButton = (Button) findViewById(R.id.next_button);
            nextButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(intent);
                }
            });
        }


    }
}
