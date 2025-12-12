package com.student.overcooked.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.student.overcooked.R;

import java.util.ArrayList;
import java.util.List;

public class MascotStageAdapter extends RecyclerView.Adapter<MascotStageAdapter.StageViewHolder> {

    private List<Integer> stageDrawables = new ArrayList<>();
    private String[] stageNames = new String[]{};

    public void setStages(List<Integer> drawables, String[] names) {
        this.stageDrawables = drawables != null ? drawables : new ArrayList<>();
        this.stageNames = names != null ? names : new String[]{};
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mascot_stage, parent, false);
        return new StageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StageViewHolder holder, int position) {
        if (position < stageDrawables.size()) {
            holder.stageImage.setImageResource(stageDrawables.get(position));
            if (position < stageNames.length) {
                holder.stageName.setText(stageNames[position]);
            }
        }
    }

    @Override
    public int getItemCount() {
        return stageDrawables.size();
    }

    static class StageViewHolder extends RecyclerView.ViewHolder {
        ImageView stageImage;
        TextView stageName;

        StageViewHolder(@NonNull View itemView) {
            super(itemView);
            stageImage = itemView.findViewById(R.id.stageImage);
            stageName = itemView.findViewById(R.id.stageName);
        }
    }
}
