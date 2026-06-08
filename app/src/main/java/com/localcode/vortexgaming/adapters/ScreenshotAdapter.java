package com.localcode.vortexgaming.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.localcode.vortexgaming.R;
import com.localcode.vortexgaming.models.Screenshot;
import java.util.List;

public class ScreenshotAdapter extends RecyclerView.Adapter<ScreenshotAdapter.VH> {

    private final List<Screenshot> items;

    public ScreenshotAdapter(List<Screenshot> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_screenshot, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Glide.with(holder.image.getContext())
                .load(items.get(position).image)
                .into(holder.image);
    }

    @Override
    public int getItemCount() { return items != null ? items.size() : 0; }

    static class VH extends RecyclerView.ViewHolder {
        ImageView image;
        VH(View v) {
            super(v);
            image = v.findViewById(R.id.img_screenshot);
        }
    }
}
