package com.bytedance.androidcamp.network.dou.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class GetResponse {
    @SerializedName("feeds") List<Video>videos;
    @SerializedName("success") Boolean success;

    public List<Video> getVideos() { return videos; }

    public Boolean getSuccess() { return success; }
}
