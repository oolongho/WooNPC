package com.oolonghoo.woonpc.skin.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MojangSessionResponse {
    
    @SerializedName("id")
    private String id;
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("properties")
    private List<Property> properties;
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public List<Property> getProperties() {
        return properties;
    }
    
    public static class Property {
        @SerializedName("name")
        private String name;
        
        @SerializedName("value")
        private String value;
        
        @SerializedName("signature")
        private String signature;
        
        public String getName() {
            return name;
        }
        
        public String getValue() {
            return value;
        }
        
        public String getSignature() {
            return signature;
        }
    }
}
