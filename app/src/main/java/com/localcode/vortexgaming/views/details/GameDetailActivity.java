package com.localcode.vortexgaming.views.details;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.localcode.vortexgaming.R;

public class GameDetailActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_detail);

        String title = getIntent().getStringExtra("game_title");
        String imageUrl = getIntent().getStringExtra("game_image");

        TextView tvTitle = findViewById(R.id.tv_detail_title);
        ImageView ivCover = findViewById(R.id.img_detail_cover);

        tvTitle.setText(title);
        Glide.with(this).load(imageUrl).into(ivCover);
    }
}
