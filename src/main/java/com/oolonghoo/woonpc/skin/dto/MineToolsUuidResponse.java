package com.oolonghoo.woonpc.skin.dto;

import com.google.gson.annotations.SerializedName;

public class MineToolsUuidResponse {
    
    @SerializedName("id")
    private String id;
    
    @SerializedName("name")
    private String name;
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
}
