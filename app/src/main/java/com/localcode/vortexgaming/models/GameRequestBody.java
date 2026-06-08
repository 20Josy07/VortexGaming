package com.localcode.vortexgaming.models;

public class GameRequestBody {
    public String game_name;
    public int game_id;
    public String expansion_name;
    public int expansion_id;

    public GameRequestBody(String game_name, int game_id, String expansion_name, int expansion_id) {
        this.game_name      = game_name;
        this.game_id        = game_id;
        this.expansion_name = expansion_name;
        this.expansion_id   = expansion_id;
    }
}
