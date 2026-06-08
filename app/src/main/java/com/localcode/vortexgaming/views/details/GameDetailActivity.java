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

        int gameId = getIntent().getIntExtra("game_id", 0);
        String title = getIntent().getStringExtra("game_title");
        String imageUrl = getIntent().getStringExtra("game_image");

        TextView tvTitle = findViewById(R.id.tv_detail_title);
        TextView tvDescription = findViewById(R.id.tv_detail_description);
        ImageView ivCover = findViewById(R.id.img_detail_cover);

        tvTitle.setText(title);
        Glide.with(this).load(imageUrl).into(ivCover);

        // Llamada a API para descripción
        com.localcode.vortexgaming.api.RawgApiService service = com.localcode.vortexgaming.api.RetrofitClient.getClient().create(com.localcode.vortexgaming.api.RawgApiService.class);
        service.getGameDetail(gameId, getString(R.string.rawg_api_key)).enqueue(new retrofit2.Callback<com.localcode.vortexgaming.models.Game>() {
            @Override
            public void onResponse(retrofit2.Call<com.localcode.vortexgaming.models.Game> call, retrofit2.Response<com.localcode.vortexgaming.models.Game> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tvDescription.setText(response.body().description);
                }
            }
            @Override
            public void onFailure(retrofit2.Call<com.localcode.vortexgaming.models.Game> call, Throwable t) {}
        });
        // Lógica de favoritos
        com.localcode.vortexgaming.utils.FavoritesManager favManager = new com.localcode.vortexgaming.utils.FavoritesManager(this);
        com.google.android.material.floatingactionbutton.FloatingActionButton fab = findViewById(R.id.fab_favorite);
        
        if (favManager.isFavorite(String.valueOf(gameId))) {
            fab.setImageResource(android.R.drawable.btn_star_big_on);
        }

        fab.setOnClickListener(v -> {
            favManager.toggleFavorite(String.valueOf(gameId));
            fab.setImageResource(favManager.isFavorite(String.valueOf(gameId)) ? 
                android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
        });
    }
}
