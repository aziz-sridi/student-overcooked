package com.student.overcooked.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.student.overcooked.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for Quick Stats horizontal list in Home Fragment
 */
public class QuickStatsAdapter extends RecyclerView.Adapter<QuickStatsAdapter.QuickStatViewHolder> {

    private List<QuickStatItem> statItems = new ArrayList<>();

    public void updateStats(List<QuickStatItem> stats) {
        this.statItems = stats;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public QuickStatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_quick_stat_card, parent, false);
        return new QuickStatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QuickStatViewHolder holder, int position) {
        holder.bind(statItems.get(position));
    }

    @Override
    public int getItemCount() {
        return statItems.size();
    }

    static class QuickStatViewHolder extends RecyclerView.ViewHolder {
        private final ImageView statIcon;
        private final TextView statTitle;
        private final TextView statValue;

        QuickStatViewHolder(@NonNull View itemView) {
            super(itemView);
            statIcon = itemView.findViewById(R.id.statIcon);
            statTitle = itemView.findViewById(R.id.statTitle);
            statValue = itemView.findViewById(R.id.statValue);
        }

        void bind(QuickStatItem item) {
            statTitle.setText(item.getLabel());
            statValue.setText(item.getValue());
            statIcon.setImageResource(item.getIconRes());
            
            int color = ContextCompat.getColor(itemView.getContext(), item.getColorRes());
            statIcon.setColorFilter(color);
            statValue.setTextColor(color);
        }
    }
}
