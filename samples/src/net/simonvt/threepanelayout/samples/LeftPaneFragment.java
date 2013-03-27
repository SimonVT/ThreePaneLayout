package net.simonvt.threepanelayout.samples;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class LeftPaneFragment extends ListFragment {

    public interface OnLeftPaneListListener {

        void onLeftItemClicked(View v, int position);
    }

    private static final String[] ENTRIES;

    static {
        final int count = 10;
        ENTRIES = new String[count];

        for (int i = 0; i < count; i++) {
            ENTRIES[i] = "Item " + i;
        }
    }

    private OnLeftPaneListListener mListener;

    private ArrayAdapter<String> mAdapter;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mListener = (OnLeftPaneListListener) activity;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mAdapter == null) {
            mAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, ENTRIES) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View v = super.getView(position, convertView, parent);
                    v.setTag(R.id.tplActiveViewPosition, position);
                    return v;
                }
            };
        }

        setListAdapter(mAdapter);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        mListener.onLeftItemClicked(v, position);
    }
}
