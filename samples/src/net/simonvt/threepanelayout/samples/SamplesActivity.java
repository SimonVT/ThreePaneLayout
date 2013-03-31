package net.simonvt.threepanelayout.samples;

import net.simonvt.threepanelayout.ThreePaneLayout;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

public class SamplesActivity extends Activity implements LeftPaneFragment.OnLeftPaneListListener,
        MiddlePaneFragment.OnMiddlePaneListListener {

    private ThreePaneLayout mThreePaneLayout;

    int i = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mThreePaneLayout = (ThreePaneLayout) findViewById(R.id.threePaneLayout);
    }

    @Override
    public void onBackPressed() {
        if (mThreePaneLayout.isRightPaneVisible()) {
            mThreePaneLayout.showLeftPane();
            getActionBar().setDisplayHomeAsUpEnabled(false);
            return;
        }

        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mThreePaneLayout.showLeftPane();
                getActionBar().setDisplayHomeAsUpEnabled(false);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onLeftItemClicked(View v, int position) {
        mThreePaneLayout.setLeftActiveView(v, position);
    }

    @Override
    public void onMiddleItemClicked(View v, int position) {
        mThreePaneLayout.setMiddleActiveView(v, position);
        mThreePaneLayout.showRightPane();
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }
}
