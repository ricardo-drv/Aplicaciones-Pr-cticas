package com.example.android.android_me.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.android.android_me.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Gato on 28/06/2017.
 */

public class BodyPartFragment extends Fragment {

    public static String TAG = "BodyPartFragment";
    public static final String IMAGE_ID_LIST = "image_ids";
    public static final String LIST_INDEX = "list_index";
    private List<Integer> mImagesIds;
    private int mListIndex;

    public BodyPartFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {


        if (savedInstanceState != null){//el rotate
            mImagesIds = savedInstanceState.getIntegerArrayList(IMAGE_ID_LIST);
            mListIndex = savedInstanceState.getInt(LIST_INDEX);
        }
        View view = inflater.inflate(R.layout.fragment_body_part, container, false);
        final ImageView imageView = (ImageView) view.findViewById(R.id.body_part_image_view);

        if (mImagesIds != null){
            imageView.setImageResource(mImagesIds.get(mListIndex));
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mListIndex < mImagesIds.size()-1){
                        mListIndex++;
                    }else {
                     mListIndex = 0;
                    }
                    imageView.setImageResource(mImagesIds.get(mListIndex));
                }
            });
        }
        else {
            Log.d(TAG,"Fragmento con lista nula");
        }


        return view;

    }


    //el on rotate
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putIntegerArrayList(IMAGE_ID_LIST,(ArrayList<Integer>) mImagesIds);
        outState.putInt(LIST_INDEX,mListIndex);

    }

    public void setmImagesIds(List<Integer> mImagesIds) {
        this.mImagesIds = mImagesIds;
    }

    public void setmListIndex(int mListIndex) {
        this.mListIndex = mListIndex;
    }
}
