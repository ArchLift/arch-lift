package com.phodal.remodern.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phodal.remodern.core.McpTool;
import com.phodal.remodern.core.McpToolRegistry;
import com.phodal.remodern.core.McpToolResult;
import com.phodal.remodern.tools.bytecode.ByteCodeTool;
import com.phodal.remodern.tools.codegen.AstCodeGenTool;
import com.phodal.remodern.tools.codegen.TemplateCodeGenTool;
import com.phodal.remodern.tools.parsing.JSPParseTool;
import com.phodal.remodern.tools.parsing.JavaParseTool;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Command-line interface for ReModern Java tools.
 */
@Command(
    name = "remodern-java",
    description = "ReModern Java - MCP Tools for Java development",
    version = "1.0.0",
    mixinStandardHelpOptions = true,
    subcommands = {
        ReModernCli.AstCodeGenCommand.class,
        ReModernCli.TemplateCodeGenCommand.class,
        ReModernCli.JavaParseCommand.class,
        ReModernCli.JSPParseCommand.class,
        ReModernCli.ByteCodeCommand.class,
        // ReModernCli.OpenRewriteCommand.class, // Temporarily disabled
        ReModernCli.ListToolsCommand.class
    }
)
public class ReModernCli implements Callable<Integer> {
    
    private static final McpToolRegistry toolRegistry = McpToolRegistry.getInstance();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    static {
        // Register all tools
        try {
            toolRegistry.registerTool(new AstCodeGenTool());
            toolRegistry.registerTool(new TemplateCodeGenTool());
            toolRegistry.registerTool(new JavaParseTool());
            toolRegistry.registerTool(new JSPParseTool());
            toolRegistry.registerTool(new ByteCodeTool());
            // toolRegistry.registerTool(new OpenRewriteTool()); // Temporarily disabled
        } catch (Exception e) {
            System.err.println("Failed to register tools: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        int exitCode = new CommandLine(new ReModernCli()).execute(args);
        System.exit(exitCode);
    }
    
    @Override
    public Integer call() {
        System.out.println("ReModern Java - MCP Tools for Java development");
        System.out.println("Use --help to see available commands");
        return 0;
    }
    
    @Command(name = "ast-codegen", description = "Generate Java code using JavaPoet AST manipulation")
    static class AstCodeGenCommand implements Callable<Integer> {
        
        @Option(names = {"-t", "--type"}, required = true, description = "Type of code element (class, interface, enum)")
        String type;
        
        @Option(names = {"-n", "--name"}, required = true, description = "Name of the code element")
        String name;
        
        @Option(names = {"-p", "--package"}, description = "Package name")
        String packageName = "com.example";
        
        @Option(names = {"-o", "--output"}, description = "Output directory")
        String outputDir = "src/main/java";
        
        @Option(names = {"--modifiers"}, description = "Access modifiers (comma-separated)")
        String modifiers;
        
        @Override
        public Integer call() {
            try {
                Map<String, Object> args = new HashMap<>();
                args.put("type", type);
                args.put("name", name);
                args.put("packageName", packageName);
                args.put("outputDir", outputDir);
                
                if (modifiers != null) {
                    args.put("modifiers", java.util.Arrays.asList(modifiers.split(",")));
                }
                
                McpToolResult result = toolRegistry.executeTool("ast-codegen", args);
                printResult(result);
                
                return result.isSuccess() ? 0 : 1;
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                return 1;
            }
        }
    }
    
    @Command(name = "template-codegen", description = "Generate code using FreeMarker templates")
    static class TemplateCodeGenCommand implements Callable<Integer> {
        
        @Option(names = {"-t", "--template"}, required = true, description = "Template content or file path")
        String template;
        
        @Option(names = {"--template-type"}, description = "Template type (content or file)")
        String templateType = "content";
        
        @Option(names = {"-d", "--data"}, required = true, description = "Data model JSON")
        String dataModelJson;
        
        @Option(names = {"-o", "--output"}, description = "Output file path")
        String outputFile;
        
        @Override
        public Integer call() {
            try {
                Map<String, Object> args = new HashMap<>();
                args.put("template", template);
                args.put("templateType", templateType);
                args.put("dataModel", objectMapper.readValue(dataModelJson, Map.class));
                
                if (outputFile != null) {
                    args.put("outputFile", outputFile);
                }
                
                McpToolResult result = toolRegistry.executeTool("template-codegen", args);
                printResult(result);
                
                return result.isSuccess() ? 0 : 1;
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                return 1;
            }
        }
    }
    
    @Command(name = "java-parse", description = "Parse and analyze Java source code")
    static class JavaParseCommand implements Callable<Integer> {
        
        @Parameters(index = "0", description = "Java file or directory path")
        String source;
        
        @Option(names = {"-a", "--analysis"}, description = "Analysis type (full, structure, methods, fields, imports, dependencies)")
        String analysisType = "full";
        
        @Option(names = {"--include-method-bodies"}, description = "Include method body analysis")
        boolean includeMethodBodies = false;
        
        @Option(names = {"--include-comments"}, description = "Include comments in analysis")
        boolean includeComments = true;
        
        @Override
        public Integer call() {
            try {
                Map<String, Object> args = new HashMap<>();
                args.put("source", source);
                args.put("analysisType", analysisType);
                args.put("includeMethodBodies", includeMethodBodies);
                args.put("includeComments", includeComments);
                
                McpToolResult result = toolRegistry.executeTool("java-parse", args);
                printResult(result);
                
                return result.isSuccess() ? 0 : 1;
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                return 1;
            }
        }
    }
    
    @Command(name = "jsp-parse", description = "Parse and analyze JSP files")
    static class JSPParseCommand implements Callable<Integer> {
        
        @Parameters(index = "0", description = "JSP file or directory path")
        String source;
        
        @Option(names = {"-a", "--analysis"}, description = "Analysis type (full, structure, dependencies, tags, expressions)")
        String analysisType = "full";
        
        @Option(names = {"--extract-content"}, description = "Extract actual content of elements")
        boolean extractContent = true;
        
        @Option(names = {"--validate-syntax"}, description = "Validate JSP syntax")
        boolean validateSyntax = false;
        
        @Override
        public Integer call() {
            try {
                Map<String, Object> args = new HashMap<>();
                args.put("source", source);
                args.put("analysisType", analysisType);
                args.put("extractContent", extractContent);
                args.put("validateSyntax", validateSyntax);
                
                McpToolResult result = toolRegistry.executeTool("jsp-parse", args);
                printResult(result);
                
                return result.isSuccess() ? 0 : 1;
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                return 1;
            }
        }
    }
    
    @Command(name = "bytecode", description = "Analyze Java bytecode using ASM")
    static class ByteCodeCommand implements Callable<Integer> {
        
        @Parameters(index = "0", description = ".class file, .jar file, or directory path")
        String source;
        
        @Option(names = {"-a", "--analysis"}, description = "Analysis type (full, structure, methods, fields, dependencies, instructions)")
        String analysisType = "full";
        
        @Option(names = {"--include-method-bytecode"}, description = "Include detailed method bytecode")
        boolean includeMethodBytecode = false;
        
        @Option(names = {"--include-debug"}, description = "Include debug information")
        boolean includeDebug = true;
        
        @Option(names = {"--analyze-dependencies"}, description = "Analyze class dependencies")
        boolean analyzeDependencies = true;
        
        @Override
        public Integer call() {
            try {
                Map<String, Object> args = new HashMap<>();
                args.put("source", source);
                args.put("analysisType", analysisType);
                args.put("includeMethodBytecode", includeMethodBytecode);
                args.put("includeDebug", includeDebug);
                args.put("analyzeDependencies", analyzeDependencies);
                
                McpToolResult result = toolRegistry.executeTool("bytecode-analysis", args);
                printResult(result);
                
                return result.isSuccess() ? 0 : 1;
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                return 1;
            }
        }
    }
    
    /*
    @Command(name = "openrewrite", description = "Perform AST migration and refactoring using OpenRewrite")
    static class OpenRewriteCommand implements Callable<Integer> {

        @Option(names = {"-o", "--operation"}, required = true, description = "Operation type (recipe, visitor, refactor, migrate)")
        String operation;

        @Parameters(index = "0", description = "Source file or directory path")
        String source;

        @Option(names = {"-r", "--recipe"}, description = "Recipe name (for recipe operation)")
        String recipe;

        @Option(names = {"-m", "--migration-target"}, description = "Migration target (for migrate operation)")
        String migrationTarget;

        @Option(names = {"--dry-run"}, description = "Perform dry run without changes")
        boolean dryRun = false;

        @Override
        public Integer call() {
            try {
                Map<String, Object> args = new HashMap<>();
                args.put("operation", operation);
                args.put("source", source);
                args.put("dryRun", dryRun);

                if (recipe != null) {
                    args.put("recipe", recipe);
                }
                if (migrationTarget != null) {
                    args.put("migrationTarget", migrationTarget);
                }

                McpToolResult result = toolRegistry.executeTool("openrewrite", args);
                printResult(result);

                return result.isSuccess() ? 0 : 1;
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                return 1;
            }
        }
    }
    */
    
    @Command(name = "list-tools", description = "List all available tools")
    static class ListToolsCommand implements Callable<Integer> {
        
        @Override
        public Integer call() {
            System.out.println("Available tools:");
            for (McpTool tool : toolRegistry.getAllTools()) {
                System.out.printf("  %-20s - %s%n", tool.getName(), tool.getDescription());
            }
            return 0;
        }
    }
    
    private static void printResult(McpToolResult result) {
        if (result.isSuccess()) {
            System.out.println("Success:");
            System.out.println(result.getContent());
            
            if (result.getMetadata() != null && !result.getMetadata().isEmpty()) {
                System.out.println("\nMetadata:");
                result.getMetadata().forEach((key, value) -> 
                    System.out.printf("  %s: %s%n", key, value));
            }
            
            if (result.getArtifacts() != null && !result.getArtifacts().isEmpty()) {
                System.out.println("\nGenerated files:");
                result.getArtifacts().forEach(artifact -> 
                    System.out.printf("  %s%n", artifact));
            }
        } else {
            System.err.println("Error: " + result.getErrorMessage());
        }
    }
}
