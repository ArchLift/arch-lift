package com.phodal.remodern.tools.codegen;

import com.phodal.remodern.core.McpTool;
import com.phodal.remodern.core.McpToolException;
import com.phodal.remodern.core.McpToolResult;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Template-driven code generation tool using FreeMarker.
 * Supports generating code from templates with data models.
 */
public class TemplateCodeGenTool implements McpTool {
    
    private static final String TOOL_NAME = "template-codegen";
    private static final String TOOL_DESCRIPTION = "Generate code using FreeMarker templates";
    
    private final Configuration freemarkerConfig;
    
    public TemplateCodeGenTool() {
        this.freemarkerConfig = createFreemarkerConfiguration();
    }
    
    @Override
    public String getName() {
        return TOOL_NAME;
    }
    
    @Override
    public String getDescription() {
        return TOOL_DESCRIPTION;
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        
        // Required properties
        properties.put("template", Map.of(
            "type", "string",
            "description", "Template content or file path"
        ));
        
        properties.put("dataModel", Map.of(
            "type", "object",
            "description", "Data model for template processing"
        ));
        
        // Optional properties
        properties.put("templateType", Map.of(
            "type", "string",
            "enum", Arrays.asList("content", "file"),
            "description", "Type of template (content or file, default: content)"
        ));
        
        properties.put("outputFile", Map.of(
            "type", "string",
            "description", "Output file path"
        ));
        
        properties.put("templateDir", Map.of(
            "type", "string",
            "description", "Template directory (default: templates)"
        ));
        
        schema.put("properties", properties);
        schema.put("required", Arrays.asList("template", "dataModel"));
        
        return schema;
    }
    
    @Override
    public McpToolResult execute(Map<String, Object> args) throws McpToolException {
        try {
            String templateContent = (String) args.get("template");
            String templateType = (String) args.getOrDefault("templateType", "content");
            Map<String, Object> dataModel = (Map<String, Object>) args.get("dataModel");
            String outputFile = (String) args.get("outputFile");
            String templateDir = (String) args.getOrDefault("templateDir", "templates");
            
            // Process template
            String result;
            if ("file".equals(templateType)) {
                result = processTemplateFile(templateContent, dataModel, templateDir);
            } else {
                result = processTemplateContent(templateContent, dataModel);
            }
            
            // Write to output file if specified
            List<String> artifacts = new ArrayList<>();
            if (outputFile != null && !outputFile.isEmpty()) {
                writeToFile(result, outputFile);
                artifacts.add(outputFile);
            }
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("templateType", templateType);
            metadata.put("dataModelKeys", dataModel.keySet());
            
            if (!artifacts.isEmpty()) {
                return McpToolResult.successWithArtifacts(result, artifacts);
            } else {
                return McpToolResult.success(result, metadata);
            }
            
        } catch (Exception e) {
            throw new McpToolException(TOOL_NAME, "Template processing failed", e);
        }
    }
    
    private String processTemplateContent(String templateContent, Map<String, Object> dataModel) 
            throws IOException, TemplateException {
        
        Template template = new Template("inline", new StringReader(templateContent), freemarkerConfig);
        
        StringWriter writer = new StringWriter();
        template.process(dataModel, writer);
        
        return writer.toString();
    }
    
    private String processTemplateFile(String templateFileName, Map<String, Object> dataModel, String templateDir) 
            throws IOException, TemplateException {
        
        // Set template directory
        freemarkerConfig.setDirectoryForTemplateLoading(new File(templateDir));
        
        Template template = freemarkerConfig.getTemplate(templateFileName);
        
        StringWriter writer = new StringWriter();
        template.process(dataModel, writer);
        
        return writer.toString();
    }
    
    private void writeToFile(String content, String outputFile) throws IOException {
        Path outputPath = Paths.get(outputFile);
        
        // Create parent directories if they don't exist
        Path parentDir = outputPath.getParent();
        if (parentDir != null) {
            Files.createDirectories(parentDir);
        }
        
        Files.write(outputPath, content.getBytes());
    }
    
    private Configuration createFreemarkerConfiguration() {
        Configuration config = new Configuration(Configuration.VERSION_2_3_32);
        
        // Set default settings
        config.setDefaultEncoding("UTF-8");
        config.setLogTemplateExceptions(false);
        config.setWrapUncheckedExceptions(true);
        config.setFallbackOnNullLoopVariable(false);
        
        return config;
    }
    
    @Override
    public void validateArgs(Map<String, Object> args) throws McpToolException {
        McpTool.super.validateArgs(args);
        
        if (!args.containsKey("template")) {
            throw new McpToolException(TOOL_NAME, "Template is required");
        }
        
        if (!args.containsKey("dataModel")) {
            throw new McpToolException(TOOL_NAME, "Data model is required");
        }
        
        String templateType = (String) args.get("templateType");
        if (templateType != null && !"content".equals(templateType) && !"file".equals(templateType)) {
            throw new McpToolException(TOOL_NAME, "Template type must be 'content' or 'file'");
        }
        
        // Validate template file exists if templateType is "file"
        if ("file".equals(templateType)) {
            String templateDir = (String) args.getOrDefault("templateDir", "templates");
            String templateFile = (String) args.get("template");
            Path templatePath = Paths.get(templateDir, templateFile);
            
            if (!Files.exists(templatePath)) {
                throw new McpToolException(TOOL_NAME, "Template file not found: " + templatePath);
            }
        }
    }
}
