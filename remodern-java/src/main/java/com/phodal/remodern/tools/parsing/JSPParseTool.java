package com.phodal.remodern.tools.parsing;

import com.phodal.remodern.core.McpTool;
import com.phodal.remodern.core.McpToolException;
import com.phodal.remodern.core.McpToolResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JSP file parsing and analysis tool.
 * Provides basic JSP parsing capabilities without requiring a full JSP engine.
 */
public class JSPParseTool implements McpTool {
    
    private static final String TOOL_NAME = "jsp-parse";
    private static final String TOOL_DESCRIPTION = "Parse and analyze JSP files";
    
    // JSP tag patterns
    private static final Pattern JSP_DIRECTIVE_PATTERN = Pattern.compile("<%@\\s*(\\w+)\\s+([^%>]*)%>");
    private static final Pattern JSP_SCRIPTLET_PATTERN = Pattern.compile("<%([^@!][^%>]*)%>");
    private static final Pattern JSP_EXPRESSION_PATTERN = Pattern.compile("<%=([^%>]*)%>");
    private static final Pattern JSP_DECLARATION_PATTERN = Pattern.compile("<%!([^%>]*)%>");
    private static final Pattern JSP_COMMENT_PATTERN = Pattern.compile("<%--([^-]*)(--%>|$)");
    private static final Pattern JSTL_TAG_PATTERN = Pattern.compile("<(\\w+:[^>]+)>");
    private static final Pattern JSP_INCLUDE_PATTERN = Pattern.compile("<jsp:include\\s+page\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>");
    private static final Pattern JSP_FORWARD_PATTERN = Pattern.compile("<jsp:forward\\s+page\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>");
    
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
        properties.put("source", Map.of(
            "type", "string",
            "description", "JSP file or directory path"
        ));
        
        // Optional properties
        properties.put("analysisType", Map.of(
            "type", "string",
            "enum", Arrays.asList("full", "structure", "dependencies", "tags", "expressions"),
            "description", "Type of analysis to perform (default: full)"
        ));
        
        properties.put("extractContent", Map.of(
            "type", "boolean",
            "description", "Extract actual content of elements (default: true)"
        ));
        
        properties.put("validateSyntax", Map.of(
            "type", "boolean",
            "description", "Validate JSP syntax (default: false)"
        ));
        
        schema.put("properties", properties);
        schema.put("required", Arrays.asList("source"));
        
        return schema;
    }
    
    @Override
    public McpToolResult execute(Map<String, Object> args) throws McpToolException {
        try {
            String source = (String) args.get("source");
            String analysisType = (String) args.getOrDefault("analysisType", "full");
            boolean extractContent = (Boolean) args.getOrDefault("extractContent", true);
            boolean validateSyntax = (Boolean) args.getOrDefault("validateSyntax", false);
            
            Path sourcePath = Paths.get(source);
            
            if (!Files.exists(sourcePath)) {
                throw new McpToolException(TOOL_NAME, "Source path does not exist: " + source);
            }
            
            Map<String, Object> analysisResult = new HashMap<>();
            List<String> processedFiles = new ArrayList<>();
            
            if (Files.isDirectory(sourcePath)) {
                analysisResult = analyzeDirectory(sourcePath, analysisType, extractContent, validateSyntax, processedFiles);
            } else if (source.endsWith(".jsp") || source.endsWith(".jspx")) {
                analysisResult = analyzeFile(sourcePath, analysisType, extractContent, validateSyntax);
                processedFiles.add(source);
            } else {
                throw new McpToolException(TOOL_NAME, "Source must be a JSP file or directory");
            }
            
            // Format result
            String resultContent = formatAnalysisResult(analysisResult);
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("analysisType", analysisType);
            metadata.put("processedFiles", processedFiles);
            metadata.put("extractContent", extractContent);
            metadata.put("validateSyntax", validateSyntax);
            
            return McpToolResult.success(resultContent, metadata);
            
        } catch (Exception e) {
            throw new McpToolException(TOOL_NAME, "JSP parsing failed", e);
        }
    }
    
    private Map<String, Object> analyzeDirectory(Path directory, String analysisType, 
            boolean extractContent, boolean validateSyntax, List<String> processedFiles) throws IOException {
        
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> files = new ArrayList<>();
        
        Files.walk(directory)
            .filter(path -> path.toString().endsWith(".jsp") || path.toString().endsWith(".jspx"))
            .forEach(jspFile -> {
                try {
                    Map<String, Object> fileAnalysis = analyzeFile(jspFile, analysisType, extractContent, validateSyntax);
                    fileAnalysis.put("filePath", jspFile.toString());
                    files.add(fileAnalysis);
                    processedFiles.add(jspFile.toString());
                } catch (Exception e) {
                    Map<String, Object> errorFile = new HashMap<>();
                    errorFile.put("filePath", jspFile.toString());
                    errorFile.put("error", e.getMessage());
                    files.add(errorFile);
                }
            });
        
        result.put("files", files);
        result.put("totalFiles", files.size());
        
        return result;
    }
    
    private Map<String, Object> analyzeFile(Path jspFile, String analysisType, 
            boolean extractContent, boolean validateSyntax) throws IOException {
        
        String content = Files.readString(jspFile);
        Map<String, Object> analysis = new HashMap<>();
        
        switch (analysisType.toLowerCase()) {
            case "full":
                analysis.putAll(analyzeStructure(content, extractContent));
                analysis.putAll(analyzeDependencies(content));
                analysis.putAll(analyzeTags(content, extractContent));
                analysis.putAll(analyzeExpressions(content, extractContent));
                if (validateSyntax) {
                    analysis.putAll(validateSyntax(content));
                }
                break;
            case "structure":
                analysis.putAll(analyzeStructure(content, extractContent));
                break;
            case "dependencies":
                analysis.putAll(analyzeDependencies(content));
                break;
            case "tags":
                analysis.putAll(analyzeTags(content, extractContent));
                break;
            case "expressions":
                analysis.putAll(analyzeExpressions(content, extractContent));
                break;
        }
        
        return analysis;
    }
    
    private Map<String, Object> analyzeStructure(String content, boolean extractContent) {
        Map<String, Object> structure = new HashMap<>();
        
        // Analyze directives
        List<Map<String, Object>> directives = new ArrayList<>();
        Matcher directiveMatcher = JSP_DIRECTIVE_PATTERN.matcher(content);
        while (directiveMatcher.find()) {
            Map<String, Object> directive = new HashMap<>();
            directive.put("type", directiveMatcher.group(1));
            directive.put("attributes", directiveMatcher.group(2).trim());
            if (extractContent) {
                directive.put("fullMatch", directiveMatcher.group(0));
            }
            directives.add(directive);
        }
        structure.put("directives", directives);
        
        // Analyze comments
        List<Map<String, Object>> comments = new ArrayList<>();
        Matcher commentMatcher = JSP_COMMENT_PATTERN.matcher(content);
        while (commentMatcher.find()) {
            Map<String, Object> comment = new HashMap<>();
            if (extractContent) {
                comment.put("content", commentMatcher.group(1).trim());
                comment.put("fullMatch", commentMatcher.group(0));
            }
            comments.add(comment);
        }
        structure.put("comments", comments);
        
        return structure;
    }
    
    private Map<String, Object> analyzeDependencies(String content) {
        Map<String, Object> dependencies = new HashMap<>();
        
        // Find includes
        List<String> includes = new ArrayList<>();
        Matcher includeMatcher = JSP_INCLUDE_PATTERN.matcher(content);
        while (includeMatcher.find()) {
            includes.add(includeMatcher.group(1));
        }
        dependencies.put("includes", includes);
        
        // Find forwards
        List<String> forwards = new ArrayList<>();
        Matcher forwardMatcher = JSP_FORWARD_PATTERN.matcher(content);
        while (forwardMatcher.find()) {
            forwards.add(forwardMatcher.group(1));
        }
        dependencies.put("forwards", forwards);
        
        // Extract imports from page directives
        List<String> imports = new ArrayList<>();
        Matcher directiveMatcher = JSP_DIRECTIVE_PATTERN.matcher(content);
        while (directiveMatcher.find()) {
            if ("page".equals(directiveMatcher.group(1))) {
                String attributes = directiveMatcher.group(2);
                if (attributes.contains("import=")) {
                    Pattern importPattern = Pattern.compile("import\\s*=\\s*[\"']([^\"']+)[\"']");
                    Matcher importMatcher = importPattern.matcher(attributes);
                    while (importMatcher.find()) {
                        String importList = importMatcher.group(1);
                        Arrays.stream(importList.split(","))
                            .map(String::trim)
                            .forEach(imports::add);
                    }
                }
            }
        }
        dependencies.put("imports", imports);
        
        return dependencies;
    }
    
    private Map<String, Object> analyzeTags(String content, boolean extractContent) {
        Map<String, Object> tags = new HashMap<>();
        
        // JSTL and custom tags
        List<Map<String, Object>> jstlTags = new ArrayList<>();
        Matcher jstlMatcher = JSTL_TAG_PATTERN.matcher(content);
        while (jstlMatcher.find()) {
            Map<String, Object> tag = new HashMap<>();
            String tagContent = jstlMatcher.group(1);
            String[] parts = tagContent.split("\\s+", 2);
            tag.put("name", parts[0]);
            if (parts.length > 1) {
                tag.put("attributes", parts[1]);
            }
            if (extractContent) {
                tag.put("fullMatch", jstlMatcher.group(0));
            }
            jstlTags.add(tag);
        }
        tags.put("jstlTags", jstlTags);
        
        return tags;
    }
    
    private Map<String, Object> analyzeExpressions(String content, boolean extractContent) {
        Map<String, Object> expressions = new HashMap<>();
        
        // Scriptlets
        List<Map<String, Object>> scriptlets = new ArrayList<>();
        Matcher scriptletMatcher = JSP_SCRIPTLET_PATTERN.matcher(content);
        while (scriptletMatcher.find()) {
            Map<String, Object> scriptlet = new HashMap<>();
            if (extractContent) {
                scriptlet.put("code", scriptletMatcher.group(1).trim());
                scriptlet.put("fullMatch", scriptletMatcher.group(0));
            }
            scriptlets.add(scriptlet);
        }
        expressions.put("scriptlets", scriptlets);
        
        // Expressions
        List<Map<String, Object>> jspExpressions = new ArrayList<>();
        Matcher expressionMatcher = JSP_EXPRESSION_PATTERN.matcher(content);
        while (expressionMatcher.find()) {
            Map<String, Object> expression = new HashMap<>();
            if (extractContent) {
                expression.put("expression", expressionMatcher.group(1).trim());
                expression.put("fullMatch", expressionMatcher.group(0));
            }
            jspExpressions.add(expression);
        }
        expressions.put("expressions", jspExpressions);
        
        // Declarations
        List<Map<String, Object>> declarations = new ArrayList<>();
        Matcher declarationMatcher = JSP_DECLARATION_PATTERN.matcher(content);
        while (declarationMatcher.find()) {
            Map<String, Object> declaration = new HashMap<>();
            if (extractContent) {
                declaration.put("declaration", declarationMatcher.group(1).trim());
                declaration.put("fullMatch", declarationMatcher.group(0));
            }
            declarations.add(declaration);
        }
        expressions.put("declarations", declarations);
        
        return expressions;
    }
    
    private Map<String, Object> validateSyntax(String content) {
        Map<String, Object> validation = new HashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Basic syntax validation
        if (!content.contains("<%@ page")) {
            warnings.add("No page directive found");
        }
        
        // Check for unclosed tags
        long openTags = content.chars().filter(ch -> ch == '<').count();
        long closeTags = content.chars().filter(ch -> ch == '>').count();
        if (openTags != closeTags) {
            errors.add("Mismatched angle brackets: " + openTags + " open, " + closeTags + " close");
        }
        
        // Check for unclosed JSP tags
        Pattern unclosedJspPattern = Pattern.compile("<%[^%>]*$");
        if (unclosedJspPattern.matcher(content).find()) {
            errors.add("Unclosed JSP tag found");
        }
        
        validation.put("errors", errors);
        validation.put("warnings", warnings);
        validation.put("isValid", errors.isEmpty());
        
        return validation;
    }
    
    private String formatAnalysisResult(Map<String, Object> result) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        
        result.forEach((key, value) -> {
            sb.append("  \"").append(key).append("\": ");
            if (value instanceof String) {
                sb.append("\"").append(value).append("\"");
            } else {
                sb.append(value.toString());
            }
            sb.append(",\n");
        });
        
        if (sb.length() > 2) {
            sb.setLength(sb.length() - 2);
        }
        
        sb.append("\n}");
        return sb.toString();
    }
}
