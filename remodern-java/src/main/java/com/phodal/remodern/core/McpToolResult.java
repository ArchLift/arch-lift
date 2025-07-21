package com.phodal.remodern.core;

import java.util.List;
import java.util.Map;

/**
 * Represents the result of executing an MCP tool.
 * Contains the output content and metadata about the execution.
 */
public class McpToolResult {
    
    private final String content;
    private final boolean isText;
    private final Map<String, Object> metadata;
    private final List<String> artifacts;
    private final boolean success;
    private final String errorMessage;
    
    private McpToolResult(Builder builder) {
        this.content = builder.content;
        this.isText = builder.isText;
        this.metadata = builder.metadata;
        this.artifacts = builder.artifacts;
        this.success = builder.success;
        this.errorMessage = builder.errorMessage;
    }
    
    /**
     * Create a successful result with text content.
     */
    public static McpToolResult success(String content) {
        return new Builder()
                .content(content)
                .isText(true)
                .success(true)
                .build();
    }
    
    /**
     * Create a successful result with content and metadata.
     */
    public static McpToolResult success(String content, Map<String, Object> metadata) {
        return new Builder()
                .content(content)
                .isText(true)
                .metadata(metadata)
                .success(true)
                .build();
    }
    
    /**
     * Create a successful result with artifacts.
     */
    public static McpToolResult successWithArtifacts(String content, List<String> artifacts) {
        return new Builder()
                .content(content)
                .isText(true)
                .artifacts(artifacts)
                .success(true)
                .build();
    }
    
    /**
     * Create an error result.
     */
    public static McpToolResult error(String errorMessage) {
        return new Builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
    
    // Getters
    public String getContent() { return content; }
    public boolean isText() { return isText; }
    public Map<String, Object> getMetadata() { return metadata; }
    public List<String> getArtifacts() { return artifacts; }
    public boolean isSuccess() { return success; }
    public String getErrorMessage() { return errorMessage; }
    
    /**
     * Builder for McpToolResult.
     */
    public static class Builder {
        private String content;
        private boolean isText = true;
        private Map<String, Object> metadata;
        private List<String> artifacts;
        private boolean success = true;
        private String errorMessage;
        
        public Builder content(String content) {
            this.content = content;
            return this;
        }
        
        public Builder isText(boolean isText) {
            this.isText = isText;
            return this;
        }
        
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }
        
        public Builder artifacts(List<String> artifacts) {
            this.artifacts = artifacts;
            return this;
        }
        
        public Builder success(boolean success) {
            this.success = success;
            return this;
        }
        
        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }
        
        public McpToolResult build() {
            return new McpToolResult(this);
        }
    }
}
