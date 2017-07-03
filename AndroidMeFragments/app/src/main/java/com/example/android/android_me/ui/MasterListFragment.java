package com.example.android.android_me.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import com.example.android.android_me.R;
import com.example.android.android_me.data.AndroidImageAssets;

/**
 * Created by Gato on 28/06/2017.
 */

public class MasterListFragment extends Fragment {

    OnImageClickListener mCallback;

    //interface de comunicación 1
    public interface OnImageClickListener {
        void onImageSelected(int position);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mCallback = (OnImageClickListener) context;
        }catch (ClassCastException e){
            throw new ClassCastException(context.toString() + " debes implementar onImageClickListener ");
        }
    }

    //requerida
    public MasterListFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_master_list,container,false);
        GridView gridView = (GridView) rootView.findViewById(R.id.images_grid_view);

        //adaptador
        MasterListAdapter mAdapter = new MasterListAdapter(getContext(), AndroidImageAssets.getAll());
        gridView.setAdapter(mAdapter);


        //interface 2
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //posición
                mCallback.onImageSelected(i);
            }
        });


        return rootView;
    }
}


