package com.phodal.remodern.core;

import java.util.Map;

/**
 * Base interface for all MCP tools in the ReModern Java toolkit.
 * Each tool provides specific functionality for Java development tasks.
 */
public interface McpTool {
    
    /**
     * Get the name of this tool.
     * @return the tool name
     */
    String getName();
    
    /**
     * Get the description of this tool.
     * @return the tool description
     */
    String getDescription();
    
    /**
     * Get the input schema for this tool.
     * This defines the expected parameters and their types.
     * @return the input schema as a Map
     */
    Map<String, Object> getInputSchema();
    
    /**
     * Execute the tool with the given arguments.
     * @param args the input arguments
     * @return the execution result
     * @throws McpToolException if execution fails
     */
    McpToolResult execute(Map<String, Object> args) throws McpToolException;
    
    /**
     * Validate the input arguments against the tool's schema.
     * @param args the input arguments to validate
     * @throws McpToolException if validation fails
     */
    default void validateArgs(Map<String, Object> args) throws McpToolException {
        // Default implementation - can be overridden by specific tools
        if (args == null) {
            throw new McpToolException("Arguments cannot be null");
        }
    }
}
