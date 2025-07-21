package com.phodal.remodern.tools.codegen;

import com.phodal.remodern.core.McpToolException;
import com.phodal.remodern.core.McpToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AstCodeGenToolTest {
    
    private AstCodeGenTool tool;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        tool = new AstCodeGenTool();
    }
    
    @Test
    void testGetName() {
        assertEquals("ast-codegen", tool.getName());
    }
    
    @Test
    void testGetDescription() {
        assertNotNull(tool.getDescription());
        assertFalse(tool.getDescription().isEmpty());
    }
    
    @Test
    void testGetInputSchema() {
        Map<String, Object> schema = tool.getInputSchema();
        
        assertNotNull(schema);
        assertEquals("object", schema.get("type"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        assertNotNull(properties);
        assertTrue(properties.containsKey("type"));
        assertTrue(properties.containsKey("name"));
    }
    
    @Test
    void testGenerateClass() throws McpToolException, java.io.IOException {
        Map<String, Object> args = new HashMap<>();
        args.put("type", "class");
        args.put("name", "TestClass");
        args.put("packageName", "com.example.test");
        args.put("outputDir", tempDir.toString());
        args.put("modifiers", Arrays.asList("public"));
        
        McpToolResult result = tool.execute(args);
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getContent());
        assertTrue(result.getContent().contains("Generated class: com.example.test.TestClass"));
        
        // Verify file was created
        Path expectedFile = tempDir.resolve("com/example/test/TestClass.java");
        assertTrue(Files.exists(expectedFile));
        
        // Verify file content
        String content = Files.readString(expectedFile);
        assertTrue(content.contains("package com.example.test;"));
        assertTrue(content.contains("public class TestClass"));
    }
    
    @Test
    void testGenerateInterface() throws McpToolException, java.io.IOException {
        Map<String, Object> args = new HashMap<>();
        args.put("type", "interface");
        args.put("name", "TestInterface");
        args.put("packageName", "com.example.test");
        args.put("outputDir", tempDir.toString());
        
        McpToolResult result = tool.execute(args);
        
        assertTrue(result.isSuccess());
        assertTrue(result.getContent().contains("Generated interface: com.example.test.TestInterface"));
        
        // Verify file was created
        Path expectedFile = tempDir.resolve("com/example/test/TestInterface.java");
        assertTrue(Files.exists(expectedFile));
        
        // Verify file content
        String content = Files.readString(expectedFile);
        assertTrue(content.contains("public interface TestInterface"));
    }
    
    @Test
    void testGenerateEnum() throws McpToolException, java.io.IOException {
        Map<String, Object> args = new HashMap<>();
        args.put("type", "enum");
        args.put("name", "TestEnum");
        args.put("packageName", "com.example.test");
        args.put("outputDir", tempDir.toString());
        
        // Add enum constants via fields
        Map<String, Object> field1 = new HashMap<>();
        field1.put("name", "VALUE1");
        Map<String, Object> field2 = new HashMap<>();
        field2.put("name", "VALUE2");
        args.put("fields", Arrays.asList(field1, field2));
        
        McpToolResult result = tool.execute(args);
        
        assertTrue(result.isSuccess());
        assertTrue(result.getContent().contains("Generated enum: com.example.test.TestEnum"));
        
        // Verify file was created
        Path expectedFile = tempDir.resolve("com/example/test/TestEnum.java");
        assertTrue(Files.exists(expectedFile));
        
        // Verify file content
        String content = Files.readString(expectedFile);
        assertTrue(content.contains("public enum TestEnum"));
        assertTrue(content.contains("VALUE1"));
        assertTrue(content.contains("VALUE2"));
    }
    
    @Test
    void testGenerateClassWithFields() throws McpToolException, java.io.IOException {
        Map<String, Object> args = new HashMap<>();
        args.put("type", "class");
        args.put("name", "TestClass");
        args.put("packageName", "com.example.test");
        args.put("outputDir", tempDir.toString());
        
        // Add fields
        Map<String, Object> field1 = new HashMap<>();
        field1.put("name", "id");
        field1.put("type", "Long");
        field1.put("modifiers", Arrays.asList("private"));
        
        Map<String, Object> field2 = new HashMap<>();
        field2.put("name", "name");
        field2.put("type", "String");
        field2.put("modifiers", Arrays.asList("private"));
        
        args.put("fields", Arrays.asList(field1, field2));
        
        McpToolResult result = tool.execute(args);
        
        assertTrue(result.isSuccess());
        
        // Verify file content
        Path expectedFile = tempDir.resolve("com/example/test/TestClass.java");
        String content = Files.readString(expectedFile);
        assertTrue(content.contains("private Long id"));
        assertTrue(content.contains("private String name"));
    }
    
    @Test
    void testGenerateClassWithMethods() throws McpToolException, java.io.IOException {
        Map<String, Object> args = new HashMap<>();
        args.put("type", "class");
        args.put("name", "TestClass");
        args.put("packageName", "com.example.test");
        args.put("outputDir", tempDir.toString());
        
        // Add methods
        Map<String, Object> method1 = new HashMap<>();
        method1.put("name", "getId");
        method1.put("returnType", "Long");
        method1.put("modifiers", Arrays.asList("public"));
        method1.put("body", "return this.id");
        
        Map<String, Object> method2 = new HashMap<>();
        method2.put("name", "setId");
        method2.put("returnType", "void");
        method2.put("modifiers", Arrays.asList("public"));
        method2.put("body", "this.id = id");
        
        args.put("methods", Arrays.asList(method1, method2));
        
        McpToolResult result = tool.execute(args);
        
        assertTrue(result.isSuccess());
        
        // Verify file content
        Path expectedFile = tempDir.resolve("com/example/test/TestClass.java");
        String content = Files.readString(expectedFile);
        assertTrue(content.contains("public Long getId()"));
        assertTrue(content.contains("public void setId()"));
        assertTrue(content.contains("return this.id"));
    }
    
    @Test
    void testInvalidType() {
        Map<String, Object> args = new HashMap<>();
        args.put("type", "invalid");
        args.put("name", "TestClass");
        
        assertThrows(McpToolException.class, () -> {
            tool.execute(args);
        });
    }
    
    @Test
    void testMissingRequiredArgs() {
        Map<String, Object> args = new HashMap<>();
        args.put("type", "class");
        // Missing name
        
        assertThrows(Exception.class, () -> {
            tool.execute(args);
        });
    }
}
