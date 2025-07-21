package com.phodal.remodern.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class McpToolRegistryTest {
    
    private McpToolRegistry registry;
    private TestTool testTool;
    
    @BeforeEach
    void setUp() {
        registry = McpToolRegistry.getInstance();
        registry.clear(); // Clear any existing tools
        testTool = new TestTool();
    }
    
    @Test
    void testRegisterTool() {
        registry.registerTool(testTool);
        
        assertTrue(registry.hasTool("test-tool"));
        assertEquals(1, registry.size());
        assertEquals(testTool, registry.getTool("test-tool"));
    }
    
    @Test
    void testRegisterDuplicateTool() {
        registry.registerTool(testTool);
        
        assertThrows(IllegalArgumentException.class, () -> {
            registry.registerTool(testTool);
        });
    }
    
    @Test
    void testUnregisterTool() {
        registry.registerTool(testTool);
        assertTrue(registry.hasTool("test-tool"));
        
        boolean removed = registry.unregisterTool("test-tool");
        assertTrue(removed);
        assertFalse(registry.hasTool("test-tool"));
        assertEquals(0, registry.size());
    }
    
    @Test
    void testUnregisterNonExistentTool() {
        boolean removed = registry.unregisterTool("non-existent");
        assertFalse(removed);
    }
    
    @Test
    void testExecuteTool() throws McpToolException {
        registry.registerTool(testTool);
        
        Map<String, Object> args = new HashMap<>();
        args.put("message", "Hello World");
        
        McpToolResult result = registry.executeTool("test-tool", args);
        
        assertTrue(result.isSuccess());
        assertEquals("Test result: Hello World", result.getContent());
    }
    
    @Test
    void testExecuteNonExistentTool() {
        Map<String, Object> args = new HashMap<>();
        
        assertThrows(McpToolException.class, () -> {
            registry.executeTool("non-existent", args);
        });
    }
    
    @Test
    void testGetAllTools() {
        registry.registerTool(testTool);
        
        assertEquals(1, registry.getAllTools().size());
        assertTrue(registry.getAllTools().contains(testTool));
    }
    
    @Test
    void testGetToolNames() {
        registry.registerTool(testTool);
        
        assertEquals(1, registry.getToolNames().size());
        assertTrue(registry.getToolNames().contains("test-tool"));
    }
    
    /**
     * Test implementation of McpTool for testing purposes.
     */
    private static class TestTool implements McpTool {
        
        @Override
        public String getName() {
            return "test-tool";
        }
        
        @Override
        public String getDescription() {
            return "A test tool for unit testing";
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
        public McpToolResult execute(Map<String, Object> args) throws McpToolException {
            String message = (String) args.get("message");
            if (message == null) {
                throw new McpToolException("test-tool", "Message is required");
            }
            
            return McpToolResult.success("Test result: " + message);
        }
    }
}
