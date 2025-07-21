package com.phodal.remodern.tools.codegen;

import com.phodal.remodern.core.McpTool;
import com.phodal.remodern.core.McpToolException;
import com.phodal.remodern.core.McpToolResult;
import com.squareup.javapoet.*;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * AST-based code generation tool using JavaPoet.
 * Supports generating classes, interfaces, enums, methods, and fields.
 */
public class AstCodeGenTool implements McpTool {
    
    private static final String TOOL_NAME = "ast-codegen";
    private static final String TOOL_DESCRIPTION = "Generate Java code using JavaPoet AST manipulation";
    
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
        properties.put("type", Map.of(
            "type", "string",
            "enum", Arrays.asList("class", "interface", "enum", "method", "field"),
            "description", "Type of code element to generate"
        ));
        
        properties.put("name", Map.of(
            "type", "string",
            "description", "Name of the code element"
        ));
        
        // Optional properties
        properties.put("packageName", Map.of(
            "type", "string",
            "description", "Package name for generated code"
        ));
        
        properties.put("outputDir", Map.of(
            "type", "string",
            "description", "Output directory (default: src/main/java)"
        ));
        
        properties.put("modifiers", Map.of(
            "type", "array",
            "items", Map.of("type", "string"),
            "description", "Access modifiers (public, private, protected, static, final, etc.)"
        ));
        
        properties.put("superclass", Map.of(
            "type", "string",
            "description", "Superclass name (for classes)"
        ));
        
        properties.put("interfaces", Map.of(
            "type", "array",
            "items", Map.of("type", "string"),
            "description", "List of interface names to implement"
        ));
        
        properties.put("fields", Map.of(
            "type", "array",
            "items", Map.of("type", "object"),
            "description", "List of field definitions"
        ));
        
        properties.put("methods", Map.of(
            "type", "array",
            "items", Map.of("type", "object"),
            "description", "List of method definitions"
        ));
        
        schema.put("properties", properties);
        schema.put("required", Arrays.asList("type", "name"));
        
        return schema;
    }
    
    @Override
    public McpToolResult execute(Map<String, Object> args) throws McpToolException {
        try {
            String type = (String) args.get("type");
            String name = (String) args.get("name");
            String packageName = (String) args.getOrDefault("packageName", "com.example");
            String outputDir = (String) args.getOrDefault("outputDir", "src/main/java");
            
            switch (type.toLowerCase()) {
                case "class":
                    return generateClass(args, name, packageName, outputDir);
                case "interface":
                    return generateInterface(args, name, packageName, outputDir);
                case "enum":
                    return generateEnum(args, name, packageName, outputDir);
                default:
                    throw new McpToolException(TOOL_NAME, "Unsupported type: " + type);
            }
            
        } catch (Exception e) {
            throw new McpToolException(TOOL_NAME, "Code generation failed", e);
        }
    }
    
    @SuppressWarnings("unchecked")
    private McpToolResult generateClass(Map<String, Object> args, String name, String packageName, String outputDir) 
            throws IOException {
        
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(name);
        
        // Add modifiers
        List<String> modifiers = (List<String>) args.get("modifiers");
        if (modifiers != null) {
            for (String modifier : modifiers) {
                classBuilder.addModifiers(parseModifier(modifier));
            }
        } else {
            classBuilder.addModifiers(Modifier.PUBLIC);
        }
        
        // Add superclass
        String superclass = (String) args.get("superclass");
        if (superclass != null && !superclass.isEmpty()) {
            classBuilder.superclass(ClassName.bestGuess(superclass));
        }
        
        // Add interfaces
        List<String> interfaces = (List<String>) args.get("interfaces");
        if (interfaces != null) {
            for (String interfaceName : interfaces) {
                classBuilder.addSuperinterface(ClassName.bestGuess(interfaceName));
            }
        }
        
        // Add fields
        List<Map<String, Object>> fields = (List<Map<String, Object>>) args.get("fields");
        if (fields != null) {
            for (Map<String, Object> field : fields) {
                classBuilder.addField(createField(field));
            }
        }
        
        // Add methods
        List<Map<String, Object>> methods = (List<Map<String, Object>>) args.get("methods");
        if (methods != null) {
            for (Map<String, Object> method : methods) {
                classBuilder.addMethod(createMethod(method));
            }
        }
        
        TypeSpec classSpec = classBuilder.build();
        JavaFile javaFile = JavaFile.builder(packageName, classSpec).build();
        
        // Write to file
        Path outputPath = Paths.get(outputDir);
        javaFile.writeTo(outputPath);
        
        String filePath = outputPath.resolve(packageName.replace('.', '/')).resolve(name + ".java").toString();
        
        return McpToolResult.successWithArtifacts(
            "Generated class: " + packageName + "." + name,
            Arrays.asList(filePath)
        );
    }
    
    @SuppressWarnings("unchecked")
    private McpToolResult generateInterface(Map<String, Object> args, String name, String packageName, String outputDir)
            throws IOException {

        TypeSpec.Builder interfaceBuilder = TypeSpec.interfaceBuilder(name);

        // Add modifiers
        List<String> modifiers = (List<String>) args.get("modifiers");
        if (modifiers != null) {
            for (String modifier : modifiers) {
                interfaceBuilder.addModifiers(parseModifier(modifier));
            }
        } else {
            interfaceBuilder.addModifiers(Modifier.PUBLIC);
        }

        // Add super interfaces
        List<String> interfaces = (List<String>) args.get("interfaces");
        if (interfaces != null) {
            for (String interfaceName : interfaces) {
                interfaceBuilder.addSuperinterface(ClassName.bestGuess(interfaceName));
            }
        }

        // Add methods (abstract by default in interfaces)
        List<Map<String, Object>> methods = (List<Map<String, Object>>) args.get("methods");
        if (methods != null) {
            for (Map<String, Object> method : methods) {
                interfaceBuilder.addMethod(createAbstractMethod(method));
            }
        }

        TypeSpec interfaceSpec = interfaceBuilder.build();
        JavaFile javaFile = JavaFile.builder(packageName, interfaceSpec).build();

        // Write to file
        Path outputPath = Paths.get(outputDir);
        javaFile.writeTo(outputPath);

        String filePath = outputPath.resolve(packageName.replace('.', '/')).resolve(name + ".java").toString();

        return McpToolResult.successWithArtifacts(
            "Generated interface: " + packageName + "." + name,
            Arrays.asList(filePath)
        );
    }

    @SuppressWarnings("unchecked")
    private McpToolResult generateEnum(Map<String, Object> args, String name, String packageName, String outputDir)
            throws IOException {

        TypeSpec.Builder enumBuilder = TypeSpec.enumBuilder(name);

        // Add modifiers
        List<String> modifiers = (List<String>) args.get("modifiers");
        if (modifiers != null) {
            for (String modifier : modifiers) {
                enumBuilder.addModifiers(parseModifier(modifier));
            }
        } else {
            enumBuilder.addModifiers(Modifier.PUBLIC);
        }

        // Add enum constants (from fields if provided)
        List<Map<String, Object>> fields = (List<Map<String, Object>>) args.get("fields");
        if (fields != null) {
            for (Map<String, Object> field : fields) {
                String fieldName = (String) field.get("name");
                if (fieldName != null) {
                    enumBuilder.addEnumConstant(fieldName.toUpperCase());
                }
            }
        }

        TypeSpec enumSpec = enumBuilder.build();
        JavaFile javaFile = JavaFile.builder(packageName, enumSpec).build();

        // Write to file
        Path outputPath = Paths.get(outputDir);
        javaFile.writeTo(outputPath);

        String filePath = outputPath.resolve(packageName.replace('.', '/')).resolve(name + ".java").toString();

        return McpToolResult.successWithArtifacts(
            "Generated enum: " + packageName + "." + name,
            Arrays.asList(filePath)
        );
    }

    @SuppressWarnings("unchecked")
    private FieldSpec createField(Map<String, Object> fieldDef) {
        String fieldName = (String) fieldDef.get("name");
        String fieldType = (String) fieldDef.getOrDefault("type", "String");
        List<String> modifiers = (List<String>) fieldDef.get("modifiers");

        FieldSpec.Builder fieldBuilder = FieldSpec.builder(
            ClassName.bestGuess(fieldType),
            fieldName
        );

        if (modifiers != null) {
            for (String modifier : modifiers) {
                fieldBuilder.addModifiers(parseModifier(modifier));
            }
        } else {
            fieldBuilder.addModifiers(Modifier.PRIVATE);
        }

        return fieldBuilder.build();
    }

    @SuppressWarnings("unchecked")
    private MethodSpec createMethod(Map<String, Object> methodDef) {
        String methodName = (String) methodDef.get("name");
        String returnType = (String) methodDef.getOrDefault("returnType", "void");
        String body = (String) methodDef.get("body");
        List<String> modifiers = (List<String>) methodDef.get("modifiers");

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName);

        // Handle void return type specially
        if ("void".equals(returnType)) {
            methodBuilder.returns(TypeName.VOID);
        } else {
            methodBuilder.returns(ClassName.bestGuess(returnType));
        }

        if (modifiers != null) {
            for (String modifier : modifiers) {
                methodBuilder.addModifiers(parseModifier(modifier));
            }
        } else {
            methodBuilder.addModifiers(Modifier.PUBLIC);
        }

        if (body != null && !body.isEmpty()) {
            methodBuilder.addStatement(body);
        }

        return methodBuilder.build();
    }

    private MethodSpec createAbstractMethod(Map<String, Object> methodDef) {
        String methodName = (String) methodDef.get("name");
        String returnType = (String) methodDef.getOrDefault("returnType", "void");

        return MethodSpec.methodBuilder(methodName)
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .returns(ClassName.bestGuess(returnType))
            .build();
    }

    private Modifier parseModifier(String modifier) {
        switch (modifier.toLowerCase()) {
            case "public": return Modifier.PUBLIC;
            case "private": return Modifier.PRIVATE;
            case "protected": return Modifier.PROTECTED;
            case "static": return Modifier.STATIC;
            case "final": return Modifier.FINAL;
            case "abstract": return Modifier.ABSTRACT;
            case "synchronized": return Modifier.SYNCHRONIZED;
            case "volatile": return Modifier.VOLATILE;
            case "transient": return Modifier.TRANSIENT;
            case "native": return Modifier.NATIVE;
            case "strictfp": return Modifier.STRICTFP;
            default: throw new IllegalArgumentException("Unknown modifier: " + modifier);
        }
    }
}
