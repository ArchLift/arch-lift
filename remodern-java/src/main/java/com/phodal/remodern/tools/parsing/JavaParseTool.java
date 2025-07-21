package com.phodal.remodern.tools.parsing;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.phodal.remodern.core.McpTool;
import com.phodal.remodern.core.McpToolException;
import com.phodal.remodern.core.McpToolResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Java source code parsing and analysis tool using JavaParser.
 */
public class JavaParseTool implements McpTool {
    
    private static final String TOOL_NAME = "java-parse";
    private static final String TOOL_DESCRIPTION = "Parse and analyze Java source code";
    
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
            "description", "Java file or directory path"
        ));
        
        // Optional properties
        properties.put("analysisType", Map.of(
            "type", "string",
            "enum", Arrays.asList("full", "structure", "methods", "fields", "imports", "dependencies"),
            "description", "Type of analysis to perform (default: full)"
        ));
        
        properties.put("includeMethodBodies", Map.of(
            "type", "boolean",
            "description", "Include method body analysis (default: false)"
        ));
        
        properties.put("includeComments", Map.of(
            "type", "boolean",
            "description", "Include comments in analysis (default: true)"
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
            boolean includeMethodBodies = (Boolean) args.getOrDefault("includeMethodBodies", false);
            boolean includeComments = (Boolean) args.getOrDefault("includeComments", true);
            
            Path sourcePath = Paths.get(source);
            
            if (!Files.exists(sourcePath)) {
                throw new McpToolException(TOOL_NAME, "Source path does not exist: " + source);
            }
            
            Map<String, Object> analysisResult = new HashMap<>();
            List<String> processedFiles = new ArrayList<>();
            
            if (Files.isDirectory(sourcePath)) {
                analysisResult = analyzeDirectory(sourcePath, analysisType, includeMethodBodies, includeComments, processedFiles);
            } else if (source.endsWith(".java")) {
                analysisResult = analyzeFile(sourcePath, analysisType, includeMethodBodies, includeComments);
                processedFiles.add(source);
            } else {
                throw new McpToolException(TOOL_NAME, "Source must be a Java file or directory");
            }
            
            // Format result as JSON-like string
            String resultContent = formatAnalysisResult(analysisResult);
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("analysisType", analysisType);
            metadata.put("processedFiles", processedFiles);
            metadata.put("includeMethodBodies", includeMethodBodies);
            metadata.put("includeComments", includeComments);
            
            return McpToolResult.success(resultContent, metadata);
            
        } catch (Exception e) {
            throw new McpToolException(TOOL_NAME, "Java parsing failed", e);
        }
    }
    
    private Map<String, Object> analyzeDirectory(Path directory, String analysisType, 
            boolean includeMethodBodies, boolean includeComments, List<String> processedFiles) throws IOException {
        
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> files = new ArrayList<>();
        
        Files.walk(directory)
            .filter(path -> path.toString().endsWith(".java"))
            .forEach(javaFile -> {
                try {
                    Map<String, Object> fileAnalysis = analyzeFile(javaFile, analysisType, includeMethodBodies, includeComments);
                    fileAnalysis.put("filePath", javaFile.toString());
                    files.add(fileAnalysis);
                    processedFiles.add(javaFile.toString());
                } catch (Exception e) {
                    // Log error but continue processing other files
                    Map<String, Object> errorFile = new HashMap<>();
                    errorFile.put("filePath", javaFile.toString());
                    errorFile.put("error", e.getMessage());
                    files.add(errorFile);
                }
            });
        
        result.put("files", files);
        result.put("totalFiles", files.size());
        
        return result;
    }
    
    private Map<String, Object> analyzeFile(Path javaFile, String analysisType, 
            boolean includeMethodBodies, boolean includeComments) throws IOException {
        
        JavaParser parser = new JavaParser();
        ParseResult<CompilationUnit> parseResult = parser.parse(javaFile);
        
        if (!parseResult.isSuccessful()) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "Parse failed");
            errorResult.put("problems", parseResult.getProblems().toString());
            return errorResult;
        }
        
        CompilationUnit cu = parseResult.getResult().get();
        Map<String, Object> analysis = new HashMap<>();
        
        switch (analysisType.toLowerCase()) {
            case "full":
                analysis.putAll(analyzeStructure(cu));
                analysis.putAll(analyzeMethods(cu, includeMethodBodies));
                analysis.putAll(analyzeFields(cu));
                analysis.putAll(analyzeImports(cu));
                if (includeComments) {
                    analysis.putAll(analyzeComments(cu));
                }
                break;
            case "structure":
                analysis.putAll(analyzeStructure(cu));
                break;
            case "methods":
                analysis.putAll(analyzeMethods(cu, includeMethodBodies));
                break;
            case "fields":
                analysis.putAll(analyzeFields(cu));
                break;
            case "imports":
                analysis.putAll(analyzeImports(cu));
                break;
            case "dependencies":
                analysis.putAll(analyzeDependencies(cu));
                break;
        }
        
        return analysis;
    }
    
    private Map<String, Object> analyzeStructure(CompilationUnit cu) {
        Map<String, Object> structure = new HashMap<>();
        
        // Package info
        cu.getPackageDeclaration().ifPresent(pkg -> 
            structure.put("package", pkg.getNameAsString()));
        
        // Classes, interfaces, enums
        List<String> classes = new ArrayList<>();
        List<String> interfaces = new ArrayList<>();
        List<String> enums = new ArrayList<>();
        
        cu.getTypes().forEach(type -> {
            if (type instanceof ClassOrInterfaceDeclaration) {
                ClassOrInterfaceDeclaration classOrInterface = (ClassOrInterfaceDeclaration) type;
                if (classOrInterface.isInterface()) {
                    interfaces.add(classOrInterface.getNameAsString());
                } else {
                    classes.add(classOrInterface.getNameAsString());
                }
            } else if (type instanceof EnumDeclaration) {
                enums.add(type.getNameAsString());
            }
        });
        
        structure.put("classes", classes);
        structure.put("interfaces", interfaces);
        structure.put("enums", enums);
        
        return structure;
    }
    
    private Map<String, Object> analyzeMethods(CompilationUnit cu, boolean includeMethodBodies) {
        Map<String, Object> methodsInfo = new HashMap<>();
        List<Map<String, Object>> methods = new ArrayList<>();
        
        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(MethodDeclaration method, Void arg) {
                Map<String, Object> methodInfo = new HashMap<>();
                methodInfo.put("name", method.getNameAsString());
                methodInfo.put("returnType", method.getType().asString());
                methodInfo.put("modifiers", method.getModifiers().stream()
                    .map(Object::toString).collect(Collectors.toList()));
                methodInfo.put("parameters", method.getParameters().stream()
                    .map(param -> param.getType().asString() + " " + param.getNameAsString())
                    .collect(Collectors.toList()));
                
                if (includeMethodBodies && method.getBody().isPresent()) {
                    methodInfo.put("body", method.getBody().get().toString());
                }
                
                methods.add(methodInfo);
                super.visit(method, arg);
            }
        }, null);
        
        methodsInfo.put("methods", methods);
        methodsInfo.put("methodCount", methods.size());
        
        return methodsInfo;
    }
    
    private Map<String, Object> analyzeFields(CompilationUnit cu) {
        Map<String, Object> fieldsInfo = new HashMap<>();
        List<Map<String, Object>> fields = new ArrayList<>();
        
        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(FieldDeclaration field, Void arg) {
                field.getVariables().forEach(variable -> {
                    Map<String, Object> fieldInfo = new HashMap<>();
                    fieldInfo.put("name", variable.getNameAsString());
                    fieldInfo.put("type", field.getElementType().asString());
                    fieldInfo.put("modifiers", field.getModifiers().stream()
                        .map(Object::toString).collect(Collectors.toList()));
                    
                    variable.getInitializer().ifPresent(init -> 
                        fieldInfo.put("initializer", init.toString()));
                    
                    fields.add(fieldInfo);
                });
                super.visit(field, arg);
            }
        }, null);
        
        fieldsInfo.put("fields", fields);
        fieldsInfo.put("fieldCount", fields.size());
        
        return fieldsInfo;
    }
    
    private Map<String, Object> analyzeImports(CompilationUnit cu) {
        Map<String, Object> importsInfo = new HashMap<>();
        
        List<String> imports = cu.getImports().stream()
            .map(imp -> imp.getNameAsString())
            .collect(Collectors.toList());
        
        importsInfo.put("imports", imports);
        importsInfo.put("importCount", imports.size());
        
        return importsInfo;
    }
    
    private Map<String, Object> analyzeComments(CompilationUnit cu) {
        Map<String, Object> commentsInfo = new HashMap<>();
        
        List<String> comments = cu.getAllComments().stream()
            .map(comment -> comment.getContent())
            .collect(Collectors.toList());
        
        commentsInfo.put("comments", comments);
        commentsInfo.put("commentCount", comments.size());
        
        return commentsInfo;
    }
    
    private Map<String, Object> analyzeDependencies(CompilationUnit cu) {
        Map<String, Object> depsInfo = new HashMap<>();
        
        Set<String> dependencies = new HashSet<>();
        
        // Add imports as dependencies
        cu.getImports().forEach(imp -> dependencies.add(imp.getNameAsString()));
        
        // Add superclasses and interfaces
        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(ClassOrInterfaceDeclaration classDecl, Void arg) {
                classDecl.getExtendedTypes().forEach(ext -> 
                    dependencies.add(ext.getNameAsString()));
                classDecl.getImplementedTypes().forEach(impl -> 
                    dependencies.add(impl.getNameAsString()));
                super.visit(classDecl, arg);
            }
        }, null);
        
        depsInfo.put("dependencies", new ArrayList<>(dependencies));
        depsInfo.put("dependencyCount", dependencies.size());
        
        return depsInfo;
    }
    
    private String formatAnalysisResult(Map<String, Object> result) {
        // Simple JSON-like formatting
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        
        result.forEach((key, value) -> {
            sb.append("  \"").append(key).append("\": ");
            if (value instanceof String) {
                sb.append("\"").append(value).append("\"");
            } else if (value instanceof List) {
                sb.append(value.toString());
            } else if (value instanceof Map) {
                sb.append(value.toString());
            } else {
                sb.append(value);
            }
            sb.append(",\n");
        });
        
        if (sb.length() > 2) {
            sb.setLength(sb.length() - 2); // Remove last comma
        }
        
        sb.append("\n}");
        return sb.toString();
    }
}
