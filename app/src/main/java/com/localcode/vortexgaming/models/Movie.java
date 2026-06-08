package com.localcode.vortexgaming.models;

import com.google.gson.annotations.SerializedName;

public class Movie {
    public int id;
    public String name;
    public String preview;
    public MovieData data;

    public static class MovieData {
        @SerializedName("480")
        public String quality480;
        public String max;

        public String bestUrl() {
            return max != null ? max : quality480;
        }
    }
}
