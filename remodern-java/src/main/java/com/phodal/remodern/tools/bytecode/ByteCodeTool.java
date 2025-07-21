package com.phodal.remodern.tools.bytecode;

import com.phodal.remodern.core.McpTool;
import com.phodal.remodern.core.McpToolException;
import com.phodal.remodern.core.McpToolResult;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Bytecode analysis tool using ASM.
 * Analyzes Java class files and JAR files.
 */
public class ByteCodeTool implements McpTool {
    
    private static final String TOOL_NAME = "bytecode-analysis";
    private static final String TOOL_DESCRIPTION = "Analyze Java bytecode using ASM";
    
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
            "description", ".class file, .jar file, or directory path"
        ));
        
        // Optional properties
        properties.put("analysisType", Map.of(
            "type", "string",
            "enum", Arrays.asList("full", "structure", "methods", "fields", "dependencies", "instructions"),
            "description", "Type of analysis to perform (default: full)"
        ));
        
        properties.put("includeMethodBytecode", Map.of(
            "type", "boolean",
            "description", "Include detailed method bytecode (default: false)"
        ));
        
        properties.put("includeDebug", Map.of(
            "type", "boolean",
            "description", "Include debug information (default: true)"
        ));
        
        properties.put("analyzeDependencies", Map.of(
            "type", "boolean",
            "description", "Analyze class dependencies (default: true)"
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
            boolean includeMethodBytecode = (Boolean) args.getOrDefault("includeMethodBytecode", false);
            boolean includeDebug = (Boolean) args.getOrDefault("includeDebug", true);
            boolean analyzeDependencies = (Boolean) args.getOrDefault("analyzeDependencies", true);
            
            Path sourcePath = Paths.get(source);
            
            if (!Files.exists(sourcePath)) {
                throw new McpToolException(TOOL_NAME, "Source path does not exist: " + source);
            }
            
            Map<String, Object> analysisResult = new HashMap<>();
            List<String> processedFiles = new ArrayList<>();
            
            if (Files.isDirectory(sourcePath)) {
                analysisResult = analyzeDirectory(sourcePath, analysisType, includeMethodBytecode, 
                    includeDebug, analyzeDependencies, processedFiles);
            } else if (source.endsWith(".jar")) {
                analysisResult = analyzeJarFile(sourcePath, analysisType, includeMethodBytecode, 
                    includeDebug, analyzeDependencies, processedFiles);
            } else if (source.endsWith(".class")) {
                analysisResult = analyzeClassFile(sourcePath, analysisType, includeMethodBytecode, 
                    includeDebug, analyzeDependencies);
                processedFiles.add(source);
            } else {
                throw new McpToolException(TOOL_NAME, "Source must be a .class file, .jar file, or directory");
            }
            
            String resultContent = formatAnalysisResult(analysisResult);
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("analysisType", analysisType);
            metadata.put("processedFiles", processedFiles);
            metadata.put("includeMethodBytecode", includeMethodBytecode);
            metadata.put("includeDebug", includeDebug);
            metadata.put("analyzeDependencies", analyzeDependencies);
            
            return McpToolResult.success(resultContent, metadata);
            
        } catch (Exception e) {
            throw new McpToolException(TOOL_NAME, "Bytecode analysis failed", e);
        }
    }
    
    private Map<String, Object> analyzeDirectory(Path directory, String analysisType, 
            boolean includeMethodBytecode, boolean includeDebug, boolean analyzeDependencies,
            List<String> processedFiles) throws IOException {
        
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> classes = new ArrayList<>();
        
        Files.walk(directory)
            .filter(path -> path.toString().endsWith(".class"))
            .forEach(classFile -> {
                try {
                    Map<String, Object> classAnalysis = analyzeClassFile(classFile, analysisType, 
                        includeMethodBytecode, includeDebug, analyzeDependencies);
                    classAnalysis.put("filePath", classFile.toString());
                    classes.add(classAnalysis);
                    processedFiles.add(classFile.toString());
                } catch (Exception e) {
                    Map<String, Object> errorClass = new HashMap<>();
                    errorClass.put("filePath", classFile.toString());
                    errorClass.put("error", e.getMessage());
                    classes.add(errorClass);
                }
            });
        
        result.put("classes", classes);
        result.put("totalClasses", classes.size());
        
        return result;
    }
    
    private Map<String, Object> analyzeJarFile(Path jarPath, String analysisType, 
            boolean includeMethodBytecode, boolean includeDebug, boolean analyzeDependencies,
            List<String> processedFiles) throws IOException {
        
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> classes = new ArrayList<>();
        
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                
                if (entry.getName().endsWith(".class") && !entry.isDirectory()) {
                    try (InputStream inputStream = jarFile.getInputStream(entry)) {
                        Map<String, Object> classAnalysis = analyzeClassStream(inputStream, analysisType, 
                            includeMethodBytecode, includeDebug, analyzeDependencies);
                        classAnalysis.put("entryName", entry.getName());
                        classes.add(classAnalysis);
                        processedFiles.add(jarPath + "!" + entry.getName());
                    } catch (Exception e) {
                        Map<String, Object> errorClass = new HashMap<>();
                        errorClass.put("entryName", entry.getName());
                        errorClass.put("error", e.getMessage());
                        classes.add(errorClass);
                    }
                }
            }
        }
        
        result.put("jarFile", jarPath.toString());
        result.put("classes", classes);
        result.put("totalClasses", classes.size());
        
        return result;
    }
    
    private Map<String, Object> analyzeClassFile(Path classFile, String analysisType, 
            boolean includeMethodBytecode, boolean includeDebug, boolean analyzeDependencies) throws IOException {
        
        try (InputStream inputStream = Files.newInputStream(classFile)) {
            return analyzeClassStream(inputStream, analysisType, includeMethodBytecode, includeDebug, analyzeDependencies);
        }
    }
    
    private Map<String, Object> analyzeClassStream(InputStream inputStream, String analysisType, 
            boolean includeMethodBytecode, boolean includeDebug, boolean analyzeDependencies) throws IOException {
        
        ClassReader classReader = new ClassReader(inputStream);
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, includeDebug ? 0 : ClassReader.SKIP_DEBUG);
        
        Map<String, Object> analysis = new HashMap<>();
        
        switch (analysisType.toLowerCase()) {
            case "full":
                analysis.putAll(analyzeClassStructure(classNode));
                analysis.putAll(analyzeClassMethods(classNode, includeMethodBytecode));
                analysis.putAll(analyzeClassFields(classNode));
                if (analyzeDependencies) {
                    analysis.putAll(analyzeClassDependencies(classNode));
                }
                break;
            case "structure":
                analysis.putAll(analyzeClassStructure(classNode));
                break;
            case "methods":
                analysis.putAll(analyzeClassMethods(classNode, includeMethodBytecode));
                break;
            case "fields":
                analysis.putAll(analyzeClassFields(classNode));
                break;
            case "dependencies":
                analysis.putAll(analyzeClassDependencies(classNode));
                break;
            case "instructions":
                analysis.putAll(analyzeInstructions(classNode));
                break;
        }
        
        return analysis;
    }
    
    private Map<String, Object> analyzeClassStructure(ClassNode classNode) {
        Map<String, Object> structure = new HashMap<>();
        
        structure.put("className", classNode.name.replace('/', '.'));
        structure.put("superClass", classNode.superName != null ? classNode.superName.replace('/', '.') : null);
        structure.put("access", getAccessFlags(classNode.access));
        structure.put("version", classNode.version);
        structure.put("signature", classNode.signature);
        
        List<String> interfaces = new ArrayList<>();
        if (classNode.interfaces != null) {
            for (String interfaceName : classNode.interfaces) {
                interfaces.add(interfaceName.replace('/', '.'));
            }
        }
        structure.put("interfaces", interfaces);
        
        List<String> annotations = new ArrayList<>();
        if (classNode.visibleAnnotations != null) {
            for (AnnotationNode annotation : classNode.visibleAnnotations) {
                annotations.add(annotation.desc);
            }
        }
        structure.put("annotations", annotations);
        
        return structure;
    }
    
    private Map<String, Object> analyzeClassMethods(ClassNode classNode, boolean includeMethodBytecode) {
        Map<String, Object> methodsInfo = new HashMap<>();
        List<Map<String, Object>> methods = new ArrayList<>();
        
        for (MethodNode method : classNode.methods) {
            Map<String, Object> methodInfo = new HashMap<>();
            methodInfo.put("name", method.name);
            methodInfo.put("descriptor", method.desc);
            methodInfo.put("access", getAccessFlags(method.access));
            methodInfo.put("signature", method.signature);
            
            List<String> exceptions = new ArrayList<>();
            if (method.exceptions != null) {
                for (String exception : method.exceptions) {
                    exceptions.add(exception.replace('/', '.'));
                }
            }
            methodInfo.put("exceptions", exceptions);
            
            if (includeMethodBytecode && method.instructions != null) {
                List<String> instructions = new ArrayList<>();
                for (AbstractInsnNode instruction : method.instructions) {
                    instructions.add(getInstructionString(instruction));
                }
                methodInfo.put("instructions", instructions);
            }
            
            methods.add(methodInfo);
        }
        
        methodsInfo.put("methods", methods);
        methodsInfo.put("methodCount", methods.size());
        
        return methodsInfo;
    }
    
    private Map<String, Object> analyzeClassFields(ClassNode classNode) {
        Map<String, Object> fieldsInfo = new HashMap<>();
        List<Map<String, Object>> fields = new ArrayList<>();
        
        for (FieldNode field : classNode.fields) {
            Map<String, Object> fieldInfo = new HashMap<>();
            fieldInfo.put("name", field.name);
            fieldInfo.put("descriptor", field.desc);
            fieldInfo.put("access", getAccessFlags(field.access));
            fieldInfo.put("signature", field.signature);
            fieldInfo.put("value", field.value);
            
            fields.add(fieldInfo);
        }
        
        fieldsInfo.put("fields", fields);
        fieldsInfo.put("fieldCount", fields.size());
        
        return fieldsInfo;
    }
    
    private Map<String, Object> analyzeClassDependencies(ClassNode classNode) {
        Map<String, Object> dependencies = new HashMap<>();
        Set<String> referencedClasses = new HashSet<>();
        
        // Add superclass and interfaces
        if (classNode.superName != null) {
            referencedClasses.add(classNode.superName.replace('/', '.'));
        }
        if (classNode.interfaces != null) {
            for (String interfaceName : classNode.interfaces) {
                referencedClasses.add(interfaceName.replace('/', '.'));
            }
        }
        
        // Analyze method instructions for class references
        for (MethodNode method : classNode.methods) {
            if (method.instructions != null) {
                for (AbstractInsnNode instruction : method.instructions) {
                    if (instruction instanceof TypeInsnNode) {
                        TypeInsnNode typeInsn = (TypeInsnNode) instruction;
                        referencedClasses.add(typeInsn.desc.replace('/', '.'));
                    } else if (instruction instanceof FieldInsnNode) {
                        FieldInsnNode fieldInsn = (FieldInsnNode) instruction;
                        referencedClasses.add(fieldInsn.owner.replace('/', '.'));
                    } else if (instruction instanceof MethodInsnNode) {
                        MethodInsnNode methodInsn = (MethodInsnNode) instruction;
                        referencedClasses.add(methodInsn.owner.replace('/', '.'));
                    }
                }
            }
        }
        
        dependencies.put("referencedClasses", new ArrayList<>(referencedClasses));
        dependencies.put("dependencyCount", referencedClasses.size());
        
        return dependencies;
    }
    
    private Map<String, Object> analyzeInstructions(ClassNode classNode) {
        Map<String, Object> instructionsInfo = new HashMap<>();
        Map<String, Integer> instructionCounts = new HashMap<>();
        
        for (MethodNode method : classNode.methods) {
            if (method.instructions != null) {
                for (AbstractInsnNode instruction : method.instructions) {
                    String instructionName = getInstructionName(instruction);
                    instructionCounts.merge(instructionName, 1, Integer::sum);
                }
            }
        }
        
        instructionsInfo.put("instructionCounts", instructionCounts);
        instructionsInfo.put("totalInstructions", instructionCounts.values().stream().mapToInt(Integer::intValue).sum());
        
        return instructionsInfo;
    }
    
    private List<String> getAccessFlags(int access) {
        List<String> flags = new ArrayList<>();
        
        if ((access & Opcodes.ACC_PUBLIC) != 0) flags.add("public");
        if ((access & Opcodes.ACC_PRIVATE) != 0) flags.add("private");
        if ((access & Opcodes.ACC_PROTECTED) != 0) flags.add("protected");
        if ((access & Opcodes.ACC_STATIC) != 0) flags.add("static");
        if ((access & Opcodes.ACC_FINAL) != 0) flags.add("final");
        if ((access & Opcodes.ACC_ABSTRACT) != 0) flags.add("abstract");
        if ((access & Opcodes.ACC_SYNCHRONIZED) != 0) flags.add("synchronized");
        if ((access & Opcodes.ACC_VOLATILE) != 0) flags.add("volatile");
        if ((access & Opcodes.ACC_TRANSIENT) != 0) flags.add("transient");
        if ((access & Opcodes.ACC_NATIVE) != 0) flags.add("native");
        if ((access & Opcodes.ACC_INTERFACE) != 0) flags.add("interface");
        if ((access & Opcodes.ACC_ENUM) != 0) flags.add("enum");
        if ((access & Opcodes.ACC_ANNOTATION) != 0) flags.add("annotation");
        
        return flags;
    }
    
    private String getInstructionString(AbstractInsnNode instruction) {
        return getInstructionName(instruction) + " " + getInstructionDetails(instruction);
    }
    
    private String getInstructionName(AbstractInsnNode instruction) {
        return switch (instruction.getType()) {
            case AbstractInsnNode.INSN -> "INSN";
            case AbstractInsnNode.INT_INSN -> "INT_INSN";
            case AbstractInsnNode.VAR_INSN -> "VAR_INSN";
            case AbstractInsnNode.TYPE_INSN -> "TYPE_INSN";
            case AbstractInsnNode.FIELD_INSN -> "FIELD_INSN";
            case AbstractInsnNode.METHOD_INSN -> "METHOD_INSN";
            case AbstractInsnNode.INVOKE_DYNAMIC_INSN -> "INVOKE_DYNAMIC_INSN";
            case AbstractInsnNode.JUMP_INSN -> "JUMP_INSN";
            case AbstractInsnNode.LABEL -> "LABEL";
            case AbstractInsnNode.LDC_INSN -> "LDC_INSN";
            case AbstractInsnNode.IINC_INSN -> "IINC_INSN";
            case AbstractInsnNode.TABLESWITCH_INSN -> "TABLESWITCH_INSN";
            case AbstractInsnNode.LOOKUPSWITCH_INSN -> "LOOKUPSWITCH_INSN";
            case AbstractInsnNode.MULTIANEWARRAY_INSN -> "MULTIANEWARRAY_INSN";
            case AbstractInsnNode.FRAME -> "FRAME";
            case AbstractInsnNode.LINE -> "LINE";
            default -> "UNKNOWN";
        };
    }
    
    private String getInstructionDetails(AbstractInsnNode instruction) {
        return switch (instruction.getType()) {
            case AbstractInsnNode.TYPE_INSN -> ((TypeInsnNode) instruction).desc;
            case AbstractInsnNode.FIELD_INSN -> {
                FieldInsnNode fieldInsn = (FieldInsnNode) instruction;
                yield fieldInsn.owner + "." + fieldInsn.name + " " + fieldInsn.desc;
            }
            case AbstractInsnNode.METHOD_INSN -> {
                MethodInsnNode methodInsn = (MethodInsnNode) instruction;
                yield methodInsn.owner + "." + methodInsn.name + " " + methodInsn.desc;
            }
            case AbstractInsnNode.LDC_INSN -> String.valueOf(((LdcInsnNode) instruction).cst);
            case AbstractInsnNode.VAR_INSN -> String.valueOf(((VarInsnNode) instruction).var);
            case AbstractInsnNode.INT_INSN -> String.valueOf(((IntInsnNode) instruction).operand);
            default -> "";
        };
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
