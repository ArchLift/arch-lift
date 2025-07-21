package com.phodal.remodern.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phodal.remodern.core.McpTool;
import com.phodal.remodern.core.McpToolRegistry;
import com.phodal.remodern.core.McpToolResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple MCP Server implementation that works without external MCP SDK.
 * This is a basic implementation for testing purposes.
 */
public class SimpleMcpServer {
    
    private final McpToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;
    private final BufferedReader reader;
    private final PrintWriter writer;
    
    public SimpleMcpServer() {
        this.toolRegistry = McpToolRegistry.getInstance();
        this.objectMapper = new ObjectMapper();
        this.reader = new BufferedReader(new InputStreamReader(System.in));
        this.writer = new PrintWriter(System.out, true);
        registerBasicTools();
    }
    
    public static void main(String[] args) {
        try {
            SimpleMcpServer server = new SimpleMcpServer();
            server.start();
        } catch (Exception e) {
            System.err.println("Failed to start MCP server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    public void start() throws IOException {
        System.err.println("Starting Simple MCP Server...");
        System.err.println("Registered tools: " + toolRegistry.getToolNames());
        
        String line;
        while ((line = reader.readLine()) != null) {
            try {
                handleRequest(line);
            } catch (Exception e) {
                System.err.println("Error handling request: " + e.getMessage());
                try {
                    sendErrorResponse("Internal server error: " + e.getMessage());
                } catch (Exception ex) {
                    System.err.println("Failed to send error response: " + ex.getMessage());
                }
            }
        }
    }
    
    private void handleRequest(String requestLine) throws Exception {
        if (requestLine.trim().isEmpty()) {
            return;
        }
        
        Map<String, Object> request = objectMapper.readValue(requestLine, Map.class);
        String method = (String) request.get("method");
        Object id = request.get("id");
        
        switch (method) {
            case "initialize":
                handleInitialize(id);
                break;
            case "tools/list":
                handleToolsList(id);
                break;
            case "tools/call":
                handleToolCall(id, request);
                break;
            default:
                sendErrorResponse(id, "Method not found: " + method);
        }
    }
    
    private void handleInitialize(Object id) throws Exception {
        Map<String, Object> response = new HashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        
        Map<String, Object> result = new HashMap<>();
        result.put("protocolVersion", "2024-11-05");
        
        Map<String, Object> capabilities = new HashMap<>();
        capabilities.put("tools", Map.of("listChanged", true));
        result.put("capabilities", capabilities);
        
        Map<String, Object> serverInfo = new HashMap<>();
        serverInfo.put("name", "ReModern Java MCP Server");
        serverInfo.put("version", "1.0.0");
        result.put("serverInfo", serverInfo);
        
        response.put("result", result);
        
        writer.println(objectMapper.writeValueAsString(response));
    }
    
    private void handleToolsList(Object id) throws Exception {
        Map<String, Object> response = new HashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        
        Map<String, Object> result = new HashMap<>();
        result.put("tools", toolRegistry.getAllTools().stream()
            .map(tool -> {
                Map<String, Object> toolInfo = new HashMap<>();
                toolInfo.put("name", tool.getName());
                toolInfo.put("description", tool.getDescription());
                toolInfo.put("inputSchema", tool.getInputSchema());
                return toolInfo;
            })
            .toArray());
        
        response.put("result", result);
        
        writer.println(objectMapper.writeValueAsString(response));
    }
    
    @SuppressWarnings("unchecked")
    private void handleToolCall(Object id, Map<String, Object> request) throws Exception {
        Map<String, Object> params = (Map<String, Object>) request.get("params");
        String toolName = (String) params.get("name");
        Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");
        
        try {
            McpToolResult result = toolRegistry.executeTool(toolName, arguments);
            
            Map<String, Object> response = new HashMap<>();
            response.put("jsonrpc", "2.0");
            response.put("id", id);
            
            Map<String, Object> resultData = new HashMap<>();
            if (result.isSuccess()) {
                resultData.put("content", java.util.List.of(
                    Map.of("type", "text", "text", result.getContent())
                ));
                if (result.getMetadata() != null) {
                    resultData.put("meta", result.getMetadata());
                }
            } else {
                resultData.put("content", java.util.List.of(
                    Map.of("type", "text", "text", "Error: " + result.getErrorMessage())
                ));
                resultData.put("isError", true);
            }
            
            response.put("result", resultData);
            writer.println(objectMapper.writeValueAsString(response));
            
        } catch (Exception e) {
            sendErrorResponse(id, "Tool execution failed: " + e.getMessage());
        }
    }
    
    private void sendErrorResponse(String message) throws Exception {
        sendErrorResponse(null, message);
    }
    
    private void sendErrorResponse(Object id, String message) throws Exception {
        Map<String, Object> response = new HashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        
        Map<String, Object> error = new HashMap<>();
        error.put("code", -32603);
        error.put("message", message);
        response.put("error", error);
        
        writer.println(objectMapper.writeValueAsString(response));
    }
    
    private void registerBasicTools() {
        try {
            // Register all available tools
            toolRegistry.registerTool(new com.phodal.remodern.tools.codegen.AstCodeGenTool());
            toolRegistry.registerTool(new com.phodal.remodern.tools.codegen.TemplateCodeGenTool());
            toolRegistry.registerTool(new com.phodal.remodern.tools.parsing.JavaParseTool());
            toolRegistry.registerTool(new com.phodal.remodern.tools.parsing.JSPParseTool());
            toolRegistry.registerTool(new com.phodal.remodern.tools.bytecode.ByteCodeTool());

            // Also register a simple test tool for testing
            toolRegistry.registerTool(new TestTool());

            System.err.println("Registered " + toolRegistry.size() + " tools");
        } catch (Exception e) {
            System.err.println("Failed to register tools: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Simple test tool for demonstration.
     */
    private static class TestTool implements McpTool {
        
        @Override
        public String getName() {
            return "test";
        }
        
        @Override
        public String getDescription() {
            return "A simple test tool";
        }
        
        @Override
        public Map<String, Object> getInputSchema() {
            Map<String, Object> schema = new HashMap<>();
            schema.put("type", "object");
            
            Map<String, Object> properties = new HashMap<>();
            properties.put("message", Map.of(
                "type", "string",
                "description", "Test message"
            ));
            
            schema.put("properties", properties);
            return schema;
        }
        
        @Override
        public McpToolResult execute(Map<String, Object> args) {
            String message = (String) args.getOrDefault("message", "Hello from ReModern Java!");
            return McpToolResult.success("Test tool response: " + message);
        }
    }
}
