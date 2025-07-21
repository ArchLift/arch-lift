package com.phodal.remodern.core;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing MCP tools.
 * Provides registration, discovery, and execution capabilities.
 */
public class McpToolRegistry {
    
    private final Map<String, McpTool> tools = new ConcurrentHashMap<>();
    private static final McpToolRegistry INSTANCE = new McpToolRegistry();
    
    private McpToolRegistry() {
        // Private constructor for singleton
    }
    
    /**
     * Get the singleton instance of the registry.
     */
    public static McpToolRegistry getInstance() {
        return INSTANCE;
    }
    
    /**
     * Register a tool in the registry.
     * @param tool the tool to register
     * @throws IllegalArgumentException if a tool with the same name already exists
     */
    public void registerTool(McpTool tool) {
        Objects.requireNonNull(tool, "Tool cannot be null");
        Objects.requireNonNull(tool.getName(), "Tool name cannot be null");
        
        if (tools.containsKey(tool.getName())) {
            throw new IllegalArgumentException("Tool with name '" + tool.getName() + "' already exists");
        }
        
        tools.put(tool.getName(), tool);
    }
    
    /**
     * Unregister a tool from the registry.
     * @param toolName the name of the tool to unregister
     * @return true if the tool was removed, false if it didn't exist
     */
    public boolean unregisterTool(String toolName) {
        return tools.remove(toolName) != null;
    }
    
    /**
     * Get a tool by name.
     * @param toolName the name of the tool
     * @return the tool, or null if not found
     */
    public McpTool getTool(String toolName) {
        return tools.get(toolName);
    }
    
    /**
     * Check if a tool is registered.
     * @param toolName the name of the tool
     * @return true if the tool is registered
     */
    public boolean hasTool(String toolName) {
        return tools.containsKey(toolName);
    }
    
    /**
     * Get all registered tool names.
     * @return a set of tool names
     */
    public Set<String> getToolNames() {
        return new HashSet<>(tools.keySet());
    }
    
    /**
     * Get all registered tools.
     * @return a collection of tools
     */
    public Collection<McpTool> getAllTools() {
        return new ArrayList<>(tools.values());
    }
    
    /**
     * Execute a tool with the given arguments.
     * @param toolName the name of the tool to execute
     * @param args the arguments to pass to the tool
     * @return the execution result
     * @throws McpToolException if the tool is not found or execution fails
     */
    public McpToolResult executeTool(String toolName, Map<String, Object> args) throws McpToolException {
        McpTool tool = getTool(toolName);
        if (tool == null) {
            throw new McpToolException("Tool not found: " + toolName);
        }
        
        tool.validateArgs(args);
        return tool.execute(args);
    }
    
    /**
     * Clear all registered tools.
     */
    public void clear() {
        tools.clear();
    }
    
    /**
     * Get the number of registered tools.
     */
    public int size() {
        return tools.size();
    }
}
