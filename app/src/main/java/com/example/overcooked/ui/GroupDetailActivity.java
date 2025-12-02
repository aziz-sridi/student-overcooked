package com.example.overcooked.ui;

import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.overcooked.R;
import com.example.overcooked.ui.fragments.GroupDetailFragment;

/**
 * Thin host activity that launches {@link GroupDetailFragment} with the provided group id.
 */
public class GroupDetailActivity extends AppCompatActivity {

    public static final String EXTRA_GROUP_ID = "group_id";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_detail_host);

        String groupId = getIntent().getStringExtra(EXTRA_GROUP_ID);
        if (TextUtils.isEmpty(groupId)) {
            finish();
            return;
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.groupDetailContainer, GroupDetailFragment.newInstance(groupId))
                    .commit();
        }
    }
}
