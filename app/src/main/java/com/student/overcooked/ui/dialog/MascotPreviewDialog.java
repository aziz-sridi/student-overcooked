package com.student.overcooked.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;

import com.student.overcooked.R;
import com.student.overcooked.data.model.Mascot;
import com.student.overcooked.ui.adapter.MascotStageAdapter;
import com.google.android.material.button.MaterialButton;

public class MascotPreviewDialog extends Dialog {

    private Mascot mascot;
    private OnBuyClickListener buyListener;

    public interface OnBuyClickListener {
        void onBuyClick(Mascot mascot);
    }

    public MascotPreviewDialog(@NonNull Context context, Mascot mascot, OnBuyClickListener listener) {
        super(context, android.R.style.Theme_Material_Dialog_NoActionBar_MinWidth);
        this.mascot = mascot;
        this.buyListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_mascot_preview);

        getWindow().setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
        );

        // Setup views
        ImageButton btnClose = findViewById(R.id.btnClose);
        TextView mascotName = findViewById(R.id.mascotName);
        TextView mascotPrice = findViewById(R.id.mascotPrice);
        ViewPager2 viewPager = findViewById(R.id.stagesViewPager);
        LinearLayout indicatorsContainer = findViewById(R.id.indicatorsContainer);
        MaterialButton btnBuy = findViewById(R.id.btnBuyMascot);

        // Set data
        mascotName.setText(mascot.getName());
        mascotPrice.setText(String.valueOf(mascot.getPrice()));

        // Setup ViewPager
        MascotStageAdapter adapter = new MascotStageAdapter();
        adapter.setStages(mascot.getStageDrawables(), mascot.getStageNames());
        viewPager.setAdapter(adapter);

        // Setup page indicators
        setupPageIndicators(indicatorsContainer, mascot.getStageDrawables().size(), viewPager);

        // Close button
        btnClose.setOnClickListener(v -> dismiss());

        // Buy button
        btnBuy.setOnClickListener(v -> {
            if (buyListener != null) {
                buyListener.onBuyClick(mascot);
            }
            dismiss();
        });
    }

    private void setupPageIndicators(LinearLayout container, int count, ViewPager2 viewPager) {
        container.removeAllViews();
        for (int i = 0; i < count; i++) {
            View dot = new View(getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(12, 12);
            params.setMargins(6, 0, 6, 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(R.drawable.ic_coin);
            container.addView(dot);
        }

        int finalCount = count;
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                for (int i = 0; i < finalCount; i++) {
                    View dot = container.getChildAt(i);
                    if (dot != null) {
                        dot.setAlpha(i == position ? 1f : 0.4f);
                    }
                }
            }
        });

        // Set initial state
        if (container.getChildCount() > 0) {
            container.getChildAt(0).setAlpha(1f);
        }
    }
}
