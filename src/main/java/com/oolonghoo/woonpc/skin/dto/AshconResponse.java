package com.oolonghoo.woonpc.skin.dto;

import com.google.gson.annotations.SerializedName;

public class AshconResponse {
    
    @SerializedName("uuid")
    private String uuid;
    
    @SerializedName("username")
    private String username;
    
    @SerializedName("textures")
    private Textures textures;
    
    public String getUuid() {
        return uuid;
    }
    
    public String getUsername() {
        return username;
    }
    
    public Textures getTextures() {
        return textures;
    }
    
    public static class Textures {
        @SerializedName("raw")
        private RawTexture raw;
        
        public RawTexture getRaw() {
            return raw;
        }
    }
    
    public static class RawTexture {
        @SerializedName("value")
        private String value;
        
        @SerializedName("signature")
        private String signature;
        
        public String getValue() {
            return value;
        }
        
        public String getSignature() {
            return signature;
        }
    }
}
