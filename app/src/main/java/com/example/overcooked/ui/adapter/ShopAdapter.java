package com.example.overcooked.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.overcooked.R;
import com.example.overcooked.data.model.ShopItem;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class ShopAdapter extends RecyclerView.Adapter<ShopAdapter.ViewHolder> {

    private List<ShopItem> items = new ArrayList<>();
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onBuyClick(ShopItem item);
    }

    public ShopAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<ShopItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shop_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView itemImage;
        TextView itemName;
        MaterialButton btnBuy;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemImage = itemView.findViewById(R.id.itemImage);
            itemName = itemView.findViewById(R.id.itemName);
            btnBuy = itemView.findViewById(R.id.btnBuy);
        }

        void bind(ShopItem item) {
            itemName.setText(item.getName());
            itemImage.setImageResource(item.getImageResId());
            
            if (item.isPurchased()) {
                btnBuy.setText("Owned");
                btnBuy.setEnabled(false);
                btnBuy.setBackgroundTintList(itemView.getContext().getColorStateList(R.color.textSecondary));
            } else {
                btnBuy.setText(String.valueOf(item.getPrice()));
                btnBuy.setEnabled(true);
                btnBuy.setBackgroundTintList(itemView.getContext().getColorStateList(R.color.burntOrange));
                btnBuy.setOnClickListener(v -> listener.onBuyClick(item));
            }
        }
    }
}
