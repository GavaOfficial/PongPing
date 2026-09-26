package java2js;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.declarations.*;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFactory;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.resolution.model.SymbolReference;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Translates the PongPing Java sources into a single JavaScript file that runs on the
 * web runtime (web/js/runtime.js). The translation is type-aware so that Java semantics
 * that affect what is drawn on screen (integer division, (int) casts, float rounding,
 * number-to-string conversion, method overloading, static/instance resolution) are kept.
 *
 * Usage: java -jar java2js.jar <output.js> <source root>... [--exclude <relative path>]...
 */
public class Java2Js {

    // ---------------------------------------------------------------- setup

    private final CombinedTypeSolver typeSolver = new CombinedTypeSolver();
    private final List<CompilationUnit> units = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final Set<String> libraryTypesUsed = new TreeSet<>();
    private final Set<String> libraryMembersUsed = new TreeSet<>();

    /** Every user type (class/enum, not local/anonymous) by fully qualified name. */
    private final Map<String, TypeDeclaration<?>> userTypes = new LinkedHashMap<>();
    /** Method names that are overloaded somewhere in the program (their JS names carry the signature). */
    private final Set<String> overloadedNames = new HashSet<>();

    private static final Set<String> JS_RESERVED = new HashSet<>(Arrays.asList(
            "arguments", "await", "delete", "eval", "export", "function", "in", "let", "typeof",
            "var", "with", "yield", "debugger", "undefined", "NaN", "Infinity", "of", "J",
            "$this", "window", "document", "self", "top", "parent", "name", "status", "event"));

    /** Library callbacks the runtime invokes by name: never mangled. */
    private static final Set<String> CALLBACKS = new HashSet<>(Arrays.asList(
            "paintComponent", "paint", "actionPerformed", "keyPressed", "keyReleased", "keyTyped",
            "mouseClicked", "mousePressed", "mouseReleased", "mouseEntered", "mouseExited",
            "mouseMoved", "mouseDragged", "mouseWheelMoved", "componentResized", "componentMoved",
            "componentShown", "componentHidden", "windowStateChanged", "windowClosing",
            "windowActivated", "windowDeactivated", "windowIconified", "windowDeiconified",
            "windowOpened", "windowClosed", "windowGainedFocus", "windowLostFocus",
            "focusGained", "focusLost", "run", "compare", "toString", "equals", "hashCode", "main"));

    public static void main(String[] args) throws Exception {
        String out = args[0];
        List<Path> roots = new ArrayList<>();
        Set<String> excludes = new HashSet<>();
        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("--exclude")) excludes.add(Paths.get(args[++i]).normalize().toString());
            else roots.add(Paths.get(args[i]));
        }
        Java2Js t = new Java2Js();
        t.load(roots, excludes);
        String js = t.translate();
        Files.createDirectories(Paths.get(out).toAbsolutePath().getParent());
        Files.writeString(Paths.get(out), js, StandardCharsets.UTF_8);
        for (String w : t.warnings) System.err.println("WARN " + w);
        System.err.println("Library types used: " + t.libraryTypesUsed);
        System.err.println("Library members used: " + t.libraryMembersUsed);
        System.err.println(t.warnings.size() + " warnings. Wrote " + out);
        if (!t.warnings.isEmpty() && System.getenv("JAVA2JS_STRICT") != null) System.exit(1);
    }

    private void load(List<Path> roots, Set<String> excludes) throws IOException {
        typeSolver.add(new ReflectionTypeSolver());
        for (Path r : roots) typeSolver.add(new JavaParserTypeSolver(r));
        ParserConfiguration cfg = new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17)
                .setSymbolResolver(new JavaSymbolSolver(typeSolver));
        JavaParser parser = new JavaParser(cfg);
        for (Path r : roots) {
            List<Path> files;
            try (var s = Files.walk(r)) {
                files = s.filter(p -> p.toString().endsWith(".java")).sorted().collect(Collectors.toList());
            }
            for (Path f : files) {
                if (excludes.contains(f.normalize().toString())) continue;
                ParseResult<CompilationUnit> res = parser.parse(f);
                if (!res.isSuccessful()) throw new IllegalStateException("Parse error in " + f + ": " + res.getProblems());
                CompilationUnit cu = res.getResult().get();
                cu.setStorage(f);
                units.add(cu);
            }
        }
        for (CompilationUnit cu : units) {
            for (TypeDeclaration<?> td : cu.findAll(TypeDeclaration.class)) {
                if (isLocalOrAnonymous(td)) continue;
                td.getFullyQualifiedName().ifPresent(q -> userTypes.put(q, td));
            }
        }
        computeMethodNames();
    }

    private static boolean isLocalOrAnonymous(Node td) {
        return td.getParentNode().map(p -> p instanceof LocalClassDeclarationStmt || p instanceof ObjectCreationExpr).orElse(false)
                || td.findAncestor(CallableDeclaration.class).isPresent()
                || td.findAncestor(InitializerDeclaration.class).isPresent();
    }

    // ---------------------------------------------------------------- naming

    /** Overloaded method names get a signature suffix so JS can tell them apart. */
    private void computeMethodNames() {
        Map<String, Set<String>> sigsByName = new HashMap<>();
        List<MethodDeclaration> all = new ArrayList<>();
        for (CompilationUnit cu : units) {
            for (MethodDeclaration md : cu.findAll(MethodDeclaration.class)) {
                if (md.findAncestor(ObjectCreationExpr.class).isPresent()) continue; // anonymous classes keep names
                all.add(md);
                sigsByName.computeIfAbsent(md.getNameAsString(), k -> new TreeSet<>()).add(signature(md));
            }
        }
        for (Map.Entry<String, Set<String>> e : sigsByName.entrySet()) {
            if (e.getValue().size() > 1) {
                if (CALLBACKS.contains(e.getKey())) System.err.println("WARN overloaded callback method " + e.getKey() + " not mangled");
                else overloadedNames.add(e.getKey());
            }
        }
    }

    /** JS name of a user method (works for AST copies made by the symbol solver too). */
    private String methodJsName(MethodDeclaration md) {
        String name = md.getNameAsString();
        if (overloadedNames.contains(name) && !md.findAncestor(ObjectCreationExpr.class).isPresent()) return name + "$" + signature(md);
        return safeMember(name);
    }

    private static String signature(MethodDeclaration md) {
        return md.getParameters().stream().map(p -> simpleTypeName(p.getType()) + (p.isVarArgs() ? "Arr" : ""))
                .collect(Collectors.joining("$"));
    }

    private static String simpleTypeName(Type t) {
        String s = t.asString().replaceAll("<.*>", "");
        s = s.replace("[]", "Arr");
        int dot = s.lastIndexOf('.');
        return dot >= 0 ? s.substring(dot + 1) : s;
    }

    private static String safeMember(String name) {
        // Members are always accessed with a receiver, so only a few names need escaping.
        if (name.equals("constructor") || name.equals("prototype") || name.equals("__proto__")) return name + "$";
        return name;
    }

    private static String safeLocal(String name) {
        return JS_RESERVED.contains(name) ? name + "$" : name;
    }

    /** JS expression that names a type (user class or runtime class). */
    private String jsTypeName(String qname) {
        TypeDeclaration<?> td = userTypes.get(qname);
        if (td != null) return userJsName(td);
        // Library type: keep the class chain starting at the first capitalised segment.
        String[] parts = qname.split("\\.");
        int i = 0;
        while (i < parts.length - 1 && !Character.isUpperCase(parts[i].charAt(0))) i++;
        String chain = String.join(".", Arrays.copyOfRange(parts, i, parts.length));
        libraryTypesUsed.add(chain);
        return "J." + chain;
    }

    private String userJsName(TypeDeclaration<?> td) {
        if (isLocalOrAnonymous(td)) return td.getNameAsString();
        StringBuilder sb = new StringBuilder(td.getNameAsString());
        Node p = td.getParentNode().orElse(null);
        while (p instanceof TypeDeclaration) {
            sb.insert(0, ((TypeDeclaration<?>) p).getNameAsString() + "$");
            p = p.getParentNode().orElse(null);
        }
        return sb.toString();
    }

    private void warn(Node n, String msg) {
        String where = n.findCompilationUnit().flatMap(CompilationUnit::getStorage)
                .map(s -> s.getPath().getFileName().toString()).orElse("?")
                + ":" + n.getBegin().map(b -> b.line).orElse(-1);
        warnings.add(where + " " + msg);
    }

    // ---------------------------------------------------------------- types

    enum K { INT, LONG, CHAR, FLOAT, DOUBLE, BOOLEAN, STRING, REF, NULL, VOID, UNKNOWN }

    private final Map<Expression, ResolvedType> typeCache = new IdentityHashMap<>();

    private ResolvedType typeOf(Expression e) {
        if (typeCache.containsKey(e)) return typeCache.get(e);
        ResolvedType t = null;
        try {
            t = e.calculateResolvedType();
        } catch (Throwable ex) {
            if (e instanceof NameExpr && localDeclType((NameExpr) e) != null) { typeCache.put(e, null); return null; }
            warn(e, "cannot resolve type of `" + abbreviate(e) + "`: " + ex.getClass().getSimpleName() + " " + firstLine(ex.getMessage()));
        }
        typeCache.put(e, t);
        return t;
    }

    private static String firstLine(String s) {
        if (s == null) return "";
        int i = s.indexOf('\n');
        return i >= 0 ? s.substring(0, i) : s;
    }

    private static String abbreviate(Node n) {
        String s = n.toString().replaceAll("\\s+", " ");
        return s.length() > 80 ? s.substring(0, 80) + "…" : s;
    }

    private K kindOf(Expression e) {
        // Fast paths that do not need the solver.
        if (e instanceof StringLiteralExpr || e instanceof TextBlockLiteralExpr) return K.STRING;
        if (e instanceof IntegerLiteralExpr) return K.INT;
        if (e instanceof LongLiteralExpr) return K.LONG;
        if (e instanceof CharLiteralExpr) return K.CHAR;
        if (e instanceof BooleanLiteralExpr) return K.BOOLEAN;
        if (e instanceof NullLiteralExpr) return K.NULL;
        if (e instanceof DoubleLiteralExpr) {
            String v = ((DoubleLiteralExpr) e).getValue();
            return (v.endsWith("f") || v.endsWith("F")) ? K.FLOAT : K.DOUBLE;
        }
        if (e instanceof EnclosedExpr) return kindOf(((EnclosedExpr) e).getInner());
        if (e instanceof LambdaExpr || e instanceof MethodReferenceExpr) return K.REF;
        if (e instanceof CastExpr) return kindOfType(((CastExpr) e).getType());
        if (e instanceof NameExpr) {
            Type lt = localDeclType((NameExpr) e);
            if (lt != null) return lt.isArrayType() ? K.REF : kindOfType(lt);
        }
        if (e instanceof ArrayAccessExpr && ((ArrayAccessExpr) e).getName() instanceof NameExpr) {
            Type lt = localDeclType((NameExpr) ((ArrayAccessExpr) e).getName());
            if (lt != null && lt.isArrayType()) return kindOfType(lt.asArrayType().getComponentType());
        }
        return kindOf(typeOf(e));
    }

    /**
     * Finds the declared type of a local variable or parameter by walking the AST
     * (the symbol solver fails on some locals, e.g. in for-loop updates).
     */
    private static Type localDeclType(NameExpr n) {
        String name = n.getNameAsString();
        Node child = n;
        Node p = n.getParentNode().orElse(null);
        while (p != null) {
            if (p instanceof ForStmt) {
                for (Expression init : ((ForStmt) p).getInitialization()) {
                    if (init instanceof VariableDeclarationExpr) {
                        for (VariableDeclarator v : ((VariableDeclarationExpr) init).getVariables())
                            if (v.getNameAsString().equals(name)) return v.getType();
                    }
                }
            } else if (p instanceof ForEachStmt) {
                for (VariableDeclarator v : ((ForEachStmt) p).getVariable().getVariables())
                    if (v.getNameAsString().equals(name)) return v.getType();
            } else if (p instanceof BlockStmt || p instanceof SwitchEntry) {
                List<Statement> stmts = p instanceof BlockStmt ? ((BlockStmt) p).getStatements() : ((SwitchEntry) p).getStatements();
                for (Statement st : stmts) {
                    if (st == child) break;
                    if (st instanceof ExpressionStmt && ((ExpressionStmt) st).getExpression() instanceof VariableDeclarationExpr) {
                        for (VariableDeclarator v : ((VariableDeclarationExpr) ((ExpressionStmt) st).getExpression()).getVariables())
                            if (v.getNameAsString().equals(name)) return v.getType();
                    }
                }
            } else if (p instanceof CallableDeclaration) {
                for (Parameter prm : ((CallableDeclaration<?>) p).getParameters())
                    if (prm.getNameAsString().equals(name)) return prm.getType();
                return null;
            } else if (p instanceof LambdaExpr) {
                for (Parameter prm : ((LambdaExpr) p).getParameters())
                    if (prm.getNameAsString().equals(name)) return prm.getType().isUnknownType() ? null : prm.getType();
            } else if (p instanceof CatchClause) {
                if (((CatchClause) p).getParameter().getNameAsString().equals(name)) return ((CatchClause) p).getParameter().getType();
            } else if (p instanceof TypeDeclaration) {
                return null;
            }
            child = p;
            p = p.getParentNode().orElse(null);
        }
        return null;
    }

    private static K kindOfType(Type t) {
        String s = t.asString();
        switch (s) {
            case "int": case "short": case "byte": return K.INT;
            case "long": return K.LONG;
            case "char": return K.CHAR;
            case "float": return K.FLOAT;
            case "double": return K.DOUBLE;
            case "boolean": return K.BOOLEAN;
            case "String": case "java.lang.String": return K.STRING;
            case "Integer": case "Short": case "Byte": return K.INT;
            case "Long": return K.LONG;
            case "Character": return K.CHAR;
            case "Float": return K.FLOAT;
            case "Double": return K.DOUBLE;
            case "Boolean": return K.BOOLEAN;
            default: return K.REF;
        }
    }

    private static K kindOf(ResolvedType t) {
        if (t == null) return K.UNKNOWN;
        if (t.isVoid()) return K.VOID;
        if (t.isNull()) return K.NULL;
        if (t.isPrimitive()) {
            switch (t.asPrimitive()) {
                case INT: case SHORT: case BYTE: return K.INT;
                case LONG: return K.LONG;
                case CHAR: return K.CHAR;
                case FLOAT: return K.FLOAT;
                case DOUBLE: return K.DOUBLE;
                case BOOLEAN: return K.BOOLEAN;
            }
        }
        if (t.isReferenceType()) {
            switch (t.asReferenceType().getQualifiedName()) {
                case "java.lang.String": return K.STRING;
                case "java.lang.Integer": case "java.lang.Short": case "java.lang.Byte": return K.INT;
                case "java.lang.Long": return K.LONG;
                case "java.lang.Character": return K.CHAR;
                case "java.lang.Float": return K.FLOAT;
                case "java.lang.Double": return K.DOUBLE;
                case "java.lang.Boolean": return K.BOOLEAN;
            }
        }
        return K.REF;
    }

    private static boolean integral(K k) { return k == K.INT || k == K.LONG || k == K.CHAR; }
    private static boolean numeric(K k) { return integral(k) || k == K.FLOAT || k == K.DOUBLE; }

    /** Converts a translated value of kind `from` so it can be stored in kind `to` (Java assignment conversion). */
    private static String convert(String js, K from, K to) {
        if (from == K.CHAR && (to == K.INT || to == K.LONG || to == K.FLOAT || to == K.DOUBLE)) return "J.$c(" + js + ")";
        if (to == K.CHAR && (from == K.INT || from == K.LONG)) return "String.fromCharCode(" + js + ")";
        return js;
    }

    /** Expression as a number (chars become their code). */
    private String num(Expression e) {
        String js = expr(e);
        return kindOf(e) == K.CHAR ? "J.$c(" + js + ")" : js;
    }

    private String qnameOf(ResolvedType t) {
        if (t != null && t.isReferenceType()) return t.asReferenceType().getQualifiedName();
        return null;
    }

    // ---------------------------------------------------------------- output

    private StringBuilder out;
    private int indent;

    private void line(String s) {
        for (int i = 0; i < indent; i++) out.append("    ");
        out.append(s).append('\n');
    }

    private void comment(Node n) {
        n.getComment().ifPresent(c -> emitComment(c));
    }

    private void emitComment(Comment c) {
        String content = c.getContent();
        if (c.isLineComment()) {
            line("//" + content);
        } else {
            for (String l : content.split("\n")) {
                String t = l.trim();
                if (t.startsWith("*")) t = t.substring(1).trim();
                if (!t.isEmpty()) line("// " + t);
            }
        }
    }

    // ---------------------------------------------------------------- translation driver

    private String translate() {
        out = new StringBuilder();
        line("// GENERATED FILE - do not edit by hand.");
        line("// Translated from the Java sources in src/ by tools/java2js.");
        line("// Regenerate with: tools/build-web.sh");
        line("'use strict';");
        line("const PongPing = (function (J) {");
        indent++;

        List<TypeDeclaration<?>> ordered = orderBySuperclass(new ArrayList<>(userTypes.values()));
        for (TypeDeclaration<?> td : ordered) {
            emitType(td);
            line("");
        }
        // Static initialisers, in dependency order.
        line("const $classes = [" + ordered.stream().map(this::userJsName).collect(Collectors.joining(", ")) + "];");
        line("function $clinit() {");
        indent++;
        for (TypeDeclaration<?> td : orderByStaticDeps(ordered)) {
            line(userJsName(td) + ".$clinit();");
        }
        indent--;
        line("}");
        line("return { $clinit, $classes, " + ordered.stream().filter(t -> t.getParentNode().get() instanceof CompilationUnit)
                .map(this::userJsName).collect(Collectors.joining(", ")) + " };");
        indent--;
        line("})(J);");
        return out.toString();
    }

    private List<TypeDeclaration<?>> orderBySuperclass(List<TypeDeclaration<?>> types) {
        List<TypeDeclaration<?>> res = new ArrayList<>();
        Set<TypeDeclaration<?>> done = new HashSet<>();
        for (TypeDeclaration<?> t : types) addWithSupers(t, res, done);
        return res;
    }

    private void addWithSupers(TypeDeclaration<?> t, List<TypeDeclaration<?>> res, Set<TypeDeclaration<?>> done) {
        if (done.contains(t)) return;
        done.add(t);
        String sup = userSuperclass(t);
        if (sup != null) addWithSupers(userTypes.get(sup), res, done);
        res.add(t);
    }

    /** Qualified name of the user superclass, or null. */
    private String userSuperclass(TypeDeclaration<?> t) {
        if (!(t instanceof ClassOrInterfaceDeclaration)) return null;
        ClassOrInterfaceDeclaration c = (ClassOrInterfaceDeclaration) t;
        if (c.getExtendedTypes().isEmpty()) return null;
        String q = resolveTypeQName(c.getExtendedTypes(0), c);
        return userTypes.containsKey(q) ? q : null;
    }

    private String resolveTypeQName(ClassOrInterfaceType t, Node ctx) {
        try {
            return t.resolve().asReferenceType().getQualifiedName();
        } catch (Throwable ex) {
            SymbolReference<ResolvedTypeDeclaration> r = solveType(t.getNameWithScope(), ctx);
            if (r != null && r.isSolved()) return r.getCorrespondingDeclaration().getQualifiedName();
            warn(t, "cannot resolve type " + t);
            return t.getNameWithScope();
        }
    }

    private SymbolReference<ResolvedTypeDeclaration> solveType(String name, Node ctx) {
        try {
            return JavaParserFactory.getContext(ctx, typeSolver).solveType(name);
        } catch (Throwable ex) {
            return null;
        }
    }

    private List<TypeDeclaration<?>> orderByStaticDeps(List<TypeDeclaration<?>> types) {
        Map<TypeDeclaration<?>, Set<TypeDeclaration<?>>> deps = new LinkedHashMap<>();
        for (TypeDeclaration<?> t : types) {
            Set<TypeDeclaration<?>> d = new LinkedHashSet<>();
            List<Node> staticParts = new ArrayList<>();
            for (BodyDeclaration<?> m : t.getMembers()) {
                if (m instanceof FieldDeclaration && ((FieldDeclaration) m).isStatic()) staticParts.add(m);
                if (m instanceof InitializerDeclaration && ((InitializerDeclaration) m).isStatic()) staticParts.add(m);
            }
            if (t instanceof EnumDeclaration) staticParts.addAll(((EnumDeclaration) t).getEntries());
            for (Node n : staticParts) {
                for (Node x : n.findAll(Node.class)) {
                    String q = null;
                    if (x instanceof NameExpr || x instanceof FieldAccessExpr) {
                        try {
                            ResolvedValueDeclaration v = x instanceof NameExpr ? ((NameExpr) x).resolve() : ((FieldAccessExpr) x).resolve();
                            if (v.isField() && v.asField().isStatic()) q = v.asField().declaringType().getQualifiedName();
                            if (v.isEnumConstant()) q = v.getType().asReferenceType().getQualifiedName();
                        } catch (Throwable ignored) { }
                    } else if (x instanceof ClassOrInterfaceType) {
                        try { q = ((ClassOrInterfaceType) x).resolve().asReferenceType().getQualifiedName(); } catch (Throwable ignored) { }
                    } else if (x instanceof MethodCallExpr) {
                        try {
                            ResolvedMethodDeclaration m = ((MethodCallExpr) x).resolve();
                            if (m.isStatic()) q = m.declaringType().getQualifiedName();
                        } catch (Throwable ignored) { }
                    }
                    if (q != null && userTypes.containsKey(q) && userTypes.get(q) != t) d.add(userTypes.get(q));
                }
            }
            deps.put(t, d);
        }
        List<TypeDeclaration<?>> res = new ArrayList<>();
        Set<TypeDeclaration<?>> done = new HashSet<>();
        Set<TypeDeclaration<?>> visiting = new HashSet<>();
        for (TypeDeclaration<?> t : types) visitDeps(t, deps, res, done, visiting);
        return res;
    }

    private void visitDeps(TypeDeclaration<?> t, Map<TypeDeclaration<?>, Set<TypeDeclaration<?>>> deps,
                           List<TypeDeclaration<?>> res, Set<TypeDeclaration<?>> done, Set<TypeDeclaration<?>> visiting) {
        if (done.contains(t)) return;
        if (visiting.contains(t)) { warn(t, "static initialisation cycle through " + t.getNameAsString()); return; }
        visiting.add(t);
        for (TypeDeclaration<?> d : deps.get(t)) visitDeps(d, deps, res, done, visiting);
        visiting.remove(t);
        done.add(t);
        res.add(t);
    }

    // ---------------------------------------------------------------- classes

    private static String defaultValue(Type t) {
        if (t.isArrayType()) return "null";
        switch (t.asString()) {
            case "int": case "short": case "byte": case "long": case "float": case "double": return "0";
            case "char": return "'\\0'";
            case "boolean": return "false";
            default: return "null";
        }
    }

    private boolean isInnerMember(TypeDeclaration<?> td) {
        return !td.isStatic() && td instanceof ClassOrInterfaceDeclaration
                && td.getParentNode().map(p -> p instanceof TypeDeclaration).orElse(false)
                && !((ClassOrInterfaceDeclaration) td).isInterface();
    }

    private void emitType(TypeDeclaration<?> td) {
        comment(td);
        String js = userJsName(td);
        boolean isEnum = td instanceof EnumDeclaration;
        String ext;
        if (isEnum) ext = " extends J.$Enum";
        else {
            ext = "";
            ClassOrInterfaceDeclaration c = (ClassOrInterfaceDeclaration) td;
            if (c.isInterface()) { warn(td, "interfaces are not supported: " + js); }
            if (!c.getExtendedTypes().isEmpty()) {
                String q = resolveTypeQName(c.getExtendedTypes(0), c);
                ext = " extends " + jsTypeName(q);
            }
        }
        boolean local = isLocalOrAnonymous(td);
        line((local ? "" : "") + "class " + js + ext + " {");
        indent++;

        // JS constructor: default values for this class's instance fields.
        List<FieldDeclaration> instFields = new ArrayList<>();
        List<Node> instInit = new ArrayList<>();  // fields with initialisers and instance init blocks, in order
        List<Node> staticInit = new ArrayList<>();
        for (BodyDeclaration<?> m : td.getMembers()) {
            if (m instanceof FieldDeclaration) {
                FieldDeclaration f = (FieldDeclaration) m;
                if (f.isStatic()) staticInit.add(f);
                else { instFields.add(f); instInit.add(f); }
            } else if (m instanceof InitializerDeclaration) {
                if (((InitializerDeclaration) m).isStatic()) staticInit.add(m);
                else instInit.add(m);
            }
        }
        boolean inner = isInnerMember(td);
        String ctorParams = isEnum ? "$name, $ordinal" : (inner ? "$outer" : "");
        line("constructor(" + ctorParams + ") {");
        indent++;
        if (isEnum) line("super($name, $ordinal);");
        else if (!ext.isEmpty()) line("super();");
        if (inner) line("this.$outer = $outer;");
        for (FieldDeclaration f : instFields) {
            for (VariableDeclarator v : f.getVariables()) {
                line("this." + safeMember(v.getNameAsString()) + " = " + defaultValue(v.getType()) + ";");
            }
        }
        indent--;
        line("}");

        // Field initialisers and instance initialiser blocks.
        String fiName = "$fi$" + js;
        line(fiName + "() {");
        indent++;
        for (Node n : instInit) {
            if (n instanceof FieldDeclaration) {
                for (VariableDeclarator v : ((FieldDeclaration) n).getVariables()) {
                    if (v.getInitializer().isPresent()) {
                        line("this." + safeMember(v.getNameAsString()) + " = " + initializer(v) + ";");
                    }
                }
            } else {
                emitBlockBody(((InitializerDeclaration) n).getBody());
            }
        }
        indent--;
        line("}");

        // Java constructors.
        List<ConstructorDeclaration> ctors = new ArrayList<>(td.getConstructors());
        String superQ = isEnum ? null : userSuperclass(td);
        if (ctors.isEmpty()) {
            line("$ctor$" + js + "$0() {");
            indent++;
            if (superQ != null) line("super." + defaultCtorName(superQ) + "();");
            line("this." + fiName + "();");
            line("return this;");
            indent--;
            line("}");
        }
        for (int i = 0; i < ctors.size(); i++) {
            ConstructorDeclaration cd = ctors.get(i);
            comment(cd);
            line("$ctor$" + js + "$" + i + "(" + params(cd.getParameters()) + ") {");
            indent++;
            BlockStmt body = cd.getBody();
            List<Statement> stmts = new ArrayList<>(body.getStatements());
            boolean delegatesToThis = false;
            if (!stmts.isEmpty() && stmts.get(0) instanceof ExplicitConstructorInvocationStmt) {
                ExplicitConstructorInvocationStmt ec = (ExplicitConstructorInvocationStmt) stmts.remove(0);
                String ctorJs = ctorNameFor(ec);
                String args = args(ec.getArguments(), resolvedParamsOf(ec));
                if (ec.isThis()) {
                    delegatesToThis = true;
                    line("this." + ctorJs + "(" + args + ");");
                } else if (superQ != null) {
                    line("super." + ctorJs + "(" + args + ");");
                } else if (!ec.getArguments().isEmpty()) {
                    line("this.$superInit(" + args + ");");
                }
            } else if (superQ != null) {
                line("super." + defaultCtorName(superQ) + "();");
            }
            if (!delegatesToThis) line("this." + fiName + "();");
            boolean needsThis = body.findFirst(LocalClassDeclarationStmt.class).isPresent();
            if (needsThis) line("const $this = this;");
            for (Statement s : stmts) stmt(s);
            line("return this;");
            indent--;
            line("}");
        }

        // Methods.
        for (MethodDeclaration md : td.getMethods()) emitMethod(md);

        // Enum helpers.
        if (isEnum) {
            line("static values() { return " + js + ".$VALUES.slice(); }");
            line("static valueOf(name) { return J.$enumValueOf(" + js + ", name); }");
        }

        // Static initialisation.
        line("static $clinit() {");
        indent++;
        if (isEnum) {
            EnumDeclaration ed = (EnumDeclaration) td;
            List<String> names = new ArrayList<>();
            int ord = 0;
            for (EnumConstantDeclaration ec : ed.getEntries()) {
                if (!ec.getClassBody().isEmpty()) warn(ec, "enum constant bodies are not supported");
                String n = ec.getNameAsString();
                names.add(js + "." + n);
                String ctor;
                if (ctors.isEmpty()) ctor = "$ctor$" + js + "$0()";
                else {
                    int idx = 0;
                    if (ctors.size() > 1) {
                        idx = -1;
                        for (int i = 0; i < ctors.size(); i++) if (ctors.get(i).getParameters().size() == ec.getArguments().size()) idx = i;
                        if (idx < 0) { warn(ec, "cannot pick enum constructor"); idx = 0; }
                    }
                    ctor = "$ctor$" + js + "$" + idx + "(" + args(ec.getArguments(), paramKinds(ctors.get(idx).getParameters())) + ")";
                }
                line(js + "." + n + " = new " + js + "('" + n + "', " + (ord++) + ")." + ctor + ";");
            }
            line(js + ".$VALUES = [" + String.join(", ", names) + "];");
        }
        for (Node n : staticInit) {
            if (n instanceof FieldDeclaration) {
                for (VariableDeclarator v : ((FieldDeclaration) n).getVariables()) {
                    if (v.getInitializer().isPresent()) {
                        comment(n);
                        line(js + "." + safeMember(v.getNameAsString()) + " = " + initializer(v) + ";");
                    }
                }
            } else {
                emitBlockBody(((InitializerDeclaration) n).getBody());
            }
        }
        indent--;
        line("}");

        indent--;
        line("}");
        // Static field defaults (before $clinit runs).
        for (Node n : staticInit) {
            if (n instanceof FieldDeclaration) {
                for (VariableDeclarator v : ((FieldDeclaration) n).getVariables()) {
                    line(js + "." + safeMember(v.getNameAsString()) + " = " + defaultValue(v.getType()) + ";");
                }
            }
        }
        // Nested types are emitted separately (as top-level JS classes); local classes are emitted in place.
    }

    private String defaultCtorName(String classQ) {
        TypeDeclaration<?> td = userTypes.get(classQ);
        String js = userJsName(td);
        List<ConstructorDeclaration> ctors = td.getConstructors();
        if (ctors.isEmpty()) return "$ctor$" + js + "$0";
        for (int i = 0; i < ctors.size(); i++) if (ctors.get(i).getParameters().isEmpty()) return "$ctor$" + js + "$" + i;
        warn(td, "no default constructor in " + js);
        return "$ctor$" + js + "$0";
    }

    private String ctorNameFor(ExplicitConstructorInvocationStmt ec) {
        try {
            ResolvedConstructorDeclaration rc = ec.resolve();
            Optional<Node> ast = rc.toAst();
            if (ast.isPresent() && ast.get() instanceof ConstructorDeclaration) return ctorJsName((ConstructorDeclaration) ast.get());
        } catch (Throwable ex) {
            warn(ec, "cannot resolve constructor call: " + firstLine(ex.getMessage()));
        }
        return "$ctor$?";
    }

    private String ctorJsName(ConstructorDeclaration cd) {
        TypeDeclaration<?> td = (TypeDeclaration<?>) cd.getParentNode().get();
        int idx = td.getConstructors().indexOf(cd);
        return "$ctor$" + userJsName(td) + "$" + idx;
    }

    private List<K> resolvedParamsOf(ExplicitConstructorInvocationStmt ec) {
        try {
            ResolvedConstructorDeclaration rc = ec.resolve();
            List<K> ks = new ArrayList<>();
            for (int i = 0; i < rc.getNumberOfParams(); i++) ks.add(kindOf(rc.getParam(i).getType()));
            return ks;
        } catch (Throwable ex) {
            return null;
        }
    }

    private List<K> paramKinds(List<Parameter> ps) {
        List<K> ks = new ArrayList<>();
        for (Parameter p : ps) ks.add(kindOfType(p.getType()));
        return ks;
    }

    private String params(List<Parameter> ps) {
        return ps.stream().map(p -> (p.isVarArgs() ? "..." : "") + safeLocal(p.getNameAsString())).collect(Collectors.joining(", "));
    }

    private String initializer(VariableDeclarator v) {
        Expression init = v.getInitializer().get();
        if (init instanceof ArrayInitializerExpr) return arrayInit((ArrayInitializerExpr) init, elementKind(v.getType()));
        return convert(expr(init), kindOf(init), kindOfType(v.getType()));
    }

    private static K elementKind(Type t) {
        Type c = t;
        while (c.isArrayType()) c = c.asArrayType().getComponentType();
        return kindOfType(c);
    }

    private final Deque<K> returnKinds = new ArrayDeque<>();

    private void emitMethod(MethodDeclaration md) {
        comment(md);
        if (md.isAbstract() || md.getBody().isEmpty()) {
            warn(md, "abstract/bodiless method " + md.getNameAsString());
            return;
        }
        String name = methodJsName(md);
        line((md.isStatic() ? "static " : "") + name + "(" + params(md.getParameters()) + ") {");
        indent++;
        returnKinds.push(kindOfType(md.getType()));
        BlockStmt body = md.getBody().get();
        if (!md.isStatic() && body.findFirst(LocalClassDeclarationStmt.class).isPresent()) line("const $this = this;");
        emitBlockBody(body);
        returnKinds.pop();
        indent--;
        line("}");
    }

    // ---------------------------------------------------------------- statements

    private void emitBlockBody(BlockStmt b) {
        for (Statement s : b.getStatements()) stmt(s);
    }

    private void block(Statement s) {
        // Emits `{ ... }` contents for a statement used as a body.
        if (s instanceof BlockStmt) emitBlockBody((BlockStmt) s);
        else stmt(s);
    }

    private void stmt(Statement s) {
        comment(s);
        if (s instanceof BlockStmt) {
            line("{");
            indent++;
            emitBlockBody((BlockStmt) s);
            indent--;
            line("}");
        } else if (s instanceof ExpressionStmt) {
            Expression e = ((ExpressionStmt) s).getExpression();
            if (e instanceof VariableDeclarationExpr) line(varDecl((VariableDeclarationExpr) e) + ";");
            else line(expr(e) + ";");
        } else if (s instanceof IfStmt) {
            emitIf((IfStmt) s, false);
        } else if (s instanceof ForStmt) {
            ForStmt f = (ForStmt) s;
            String init = f.getInitialization().stream().map(e -> e instanceof VariableDeclarationExpr ? varDecl((VariableDeclarationExpr) e) : expr(e)).collect(Collectors.joining(", "));
            String cmp = f.getCompare().map(this::expr).orElse("");
            String upd = f.getUpdate().stream().map(this::expr).collect(Collectors.joining(", "));
            line("for (" + init + "; " + cmp + "; " + upd + ") {");
            indent++;
            block(f.getBody());
            indent--;
            line("}");
        } else if (s instanceof ForEachStmt) {
            ForEachStmt f = (ForEachStmt) s;
            String var = safeLocal(f.getVariable().getVariables().get(0).getNameAsString());
            line("for (let " + var + " of " + expr(f.getIterable()) + ") {");
            indent++;
            block(f.getBody());
            indent--;
            line("}");
        } else if (s instanceof WhileStmt) {
            WhileStmt w = (WhileStmt) s;
            line("while (" + expr(w.getCondition()) + ") {");
            indent++;
            block(w.getBody());
            indent--;
            line("}");
        } else if (s instanceof DoStmt) {
            DoStmt d = (DoStmt) s;
            line("do {");
            indent++;
            block(d.getBody());
            indent--;
            line("} while (" + expr(d.getCondition()) + ");");
        } else if (s instanceof ReturnStmt) {
            ReturnStmt r = (ReturnStmt) s;
            if (r.getExpression().isPresent()) {
                Expression e = r.getExpression().get();
                K target = returnKinds.isEmpty() ? K.UNKNOWN : returnKinds.peek();
                line("return " + convert(expr(e), kindOf(e), target) + ";");
            } else line("return;");
        } else if (s instanceof BreakStmt) {
            line("break" + ((BreakStmt) s).getLabel().map(l -> " " + l.asString()).orElse("") + ";");
        } else if (s instanceof ContinueStmt) {
            line("continue" + ((ContinueStmt) s).getLabel().map(l -> " " + l.asString()).orElse("") + ";");
        } else if (s instanceof SwitchStmt) {
            emitSwitch((SwitchStmt) s);
        } else if (s instanceof TryStmt) {
            emitTry((TryStmt) s);
        } else if (s instanceof ThrowStmt) {
            line("throw " + expr(((ThrowStmt) s).getExpression()) + ";");
        } else if (s instanceof LabeledStmt) {
            LabeledStmt l = (LabeledStmt) s;
            line(l.getLabel().asString() + ":");
            stmt(l.getStatement());
        } else if (s instanceof LocalClassDeclarationStmt) {
            emitType(((LocalClassDeclarationStmt) s).getClassDeclaration());
        } else if (s instanceof EmptyStmt) {
            // nothing
        } else if (s instanceof SynchronizedStmt) {
            line("{");
            indent++;
            emitBlockBody(((SynchronizedStmt) s).getBody());
            indent--;
            line("}");
        } else if (s instanceof AssertStmt) {
            // assertions are disabled by default in Java
        } else if (s instanceof ExplicitConstructorInvocationStmt) {
            warn(s, "unexpected constructor invocation");
        } else {
            warn(s, "unsupported statement " + s.getClass().getSimpleName());
            line("/* unsupported: " + abbreviate(s) + " */");
        }
    }

    private void emitIf(IfStmt i, boolean isElse) {
        line((isElse ? "} else " : "") + "if (" + expr(i.getCondition()) + ") {");
        indent++;
        block(i.getThenStmt());
        indent--;
        if (i.getElseStmt().isPresent()) {
            Statement e = i.getElseStmt().get();
            if (e instanceof IfStmt && e.getComment().isEmpty()) {
                emitIf((IfStmt) e, true);
                return;
            }
            line("} else {");
            indent++;
            block(e);
            indent--;
        }
        line("}");
    }

    private void emitSwitch(SwitchStmt s) {
        Expression sel = s.getSelector();
        ResolvedType st = typeOf(sel);
        String enumJs = null;
        if (st != null && st.isReferenceType() && st.asReferenceType().getTypeDeclaration().map(ResolvedReferenceTypeDeclaration::isEnum).orElse(false)) {
            enumJs = jsTypeName(st.asReferenceType().getQualifiedName());
        }
        line("switch (" + expr(sel) + ") {");
        indent++;
        for (SwitchEntry en : s.getEntries()) {
            if (en.getType() != SwitchEntry.Type.STATEMENT_GROUP) warn(en, "arrow switch entries are not supported");
            if (en.getLabels().isEmpty()) line("default:");
            for (Expression lab : en.getLabels()) {
                String l = (enumJs != null && lab instanceof NameExpr) ? enumJs + "." + ((NameExpr) lab).getNameAsString() : expr(lab);
                line("case " + l + ":");
            }
            indent++;
            for (Statement st2 : en.getStatements()) stmt(st2);
            indent--;
        }
        indent--;
        line("}");
    }

    private void emitTry(TryStmt t) {
        boolean hasHandlers = !t.getCatchClauses().isEmpty() || t.getFinallyBlock().isPresent();
        if (hasHandlers) {
            line("try {");
            indent++;
        }
        // try-with-resources: resources are opened inside the handlers (Java catches their exceptions too)
        List<String> resources = new ArrayList<>();
        int opened = 0;
        for (Expression r : t.getResources()) {
            if (r instanceof VariableDeclarationExpr) {
                VariableDeclarationExpr v = (VariableDeclarationExpr) r;
                line(varDecl(v).replaceFirst("^let ", "const ") + ";");
                resources.add(safeLocal(v.getVariables().get(0).getNameAsString()));
            } else {
                String tmp = "$res" + opened;
                line("const " + tmp + " = " + expr(r) + ";");
                resources.add(tmp);
            }
            line("try {");
            indent++;
            opened++;
        }
        emitBlockBody(t.getTryBlock());
        for (int i = opened - 1; i >= 0; i--) {
            indent--;
            line("} finally {");
            line("    if (" + resources.get(i) + " != null) " + resources.get(i) + ".close();");
            line("}");
        }
        if (hasHandlers) {
            indent--;
            if (!t.getCatchClauses().isEmpty()) {
                line("} catch ($e) {");
                indent++;
                boolean first = true;
                boolean catchAll = false;
                for (CatchClause cc : t.getCatchClauses()) {
                    Parameter p = cc.getParameter();
                    List<String> types = new ArrayList<>();
                    if (p.getType().isUnionType()) p.getType().asUnionType().getElements().forEach(e -> types.add(e.asString()));
                    else types.add(p.getType().asString());
                    boolean all = types.stream().anyMatch(x -> x.equals("Exception") || x.equals("Throwable") || x.equals("RuntimeException") || x.equals("Error"));
                    String cond = types.stream().map(x -> "J.$isException($e, '" + x.replaceAll(".*\\.", "") + "')").collect(Collectors.joining(" || "));
                    if (all) line((first ? "" : "} else ") + "{");
                    else line((first ? "" : "} else ") + "if (" + cond + ") {");
                    indent++;
                    line("let " + safeLocal(p.getNameAsString()) + " = J.$wrapException($e);");
                    emitBlockBody(cc.getBody());
                    indent--;
                    first = false;
                    if (all) { catchAll = true; break; }
                }
                if (!catchAll) {
                    line("} else {");
                    line("    throw $e;");
                }
                line("}");
                indent--;
            }
            if (t.getFinallyBlock().isPresent()) {
                line("} finally {");
                indent++;
                emitBlockBody(t.getFinallyBlock().get());
                indent--;
            }
            line("}");
        }
    }

    private String varDecl(VariableDeclarationExpr v) {
        List<String> parts = new ArrayList<>();
        for (VariableDeclarator d : v.getVariables()) {
            String n = safeLocal(d.getNameAsString());
            if (d.getInitializer().isPresent()) parts.add(n + " = " + initializer(d));
            else parts.add(n);
        }
        return "let " + String.join(", ", parts);
    }

    // ---------------------------------------------------------------- expressions

    private String expr(Expression e) {
        if (e instanceof NameExpr) return nameExpr((NameExpr) e);
        if (e instanceof IntegerLiteralExpr) return intLiteral(((IntegerLiteralExpr) e).getValue());
        if (e instanceof LongLiteralExpr) return intLiteral(((LongLiteralExpr) e).getValue().replaceAll("[lL]$", ""));
        if (e instanceof DoubleLiteralExpr) return doubleLiteral(((DoubleLiteralExpr) e).getValue());
        if (e instanceof CharLiteralExpr) return jsString(String.valueOf(((CharLiteralExpr) e).asChar()));
        if (e instanceof StringLiteralExpr) return jsString(((StringLiteralExpr) e).asString());
        if (e instanceof BooleanLiteralExpr) return String.valueOf(((BooleanLiteralExpr) e).getValue());
        if (e instanceof NullLiteralExpr) return "null";
        if (e instanceof EnclosedExpr) return "(" + expr(((EnclosedExpr) e).getInner()) + ")";
        if (e instanceof BinaryExpr) return binary((BinaryExpr) e);
        if (e instanceof UnaryExpr) return unary((UnaryExpr) e);
        if (e instanceof AssignExpr) return assign((AssignExpr) e);
        if (e instanceof CastExpr) return cast((CastExpr) e);
        if (e instanceof ConditionalExpr) {
            ConditionalExpr c = (ConditionalExpr) e;
            K rk = kindOf(c);
            return "(" + expr(c.getCondition()) + " ? " + convert(expr(c.getThenExpr()), kindOf(c.getThenExpr()), rk)
                    + " : " + convert(expr(c.getElseExpr()), kindOf(c.getElseExpr()), rk) + ")";
        }
        if (e instanceof MethodCallExpr) return methodCall((MethodCallExpr) e);
        if (e instanceof FieldAccessExpr) return fieldAccess((FieldAccessExpr) e);
        if (e instanceof ObjectCreationExpr) return objectCreation((ObjectCreationExpr) e);
        if (e instanceof ArrayCreationExpr) return arrayCreation((ArrayCreationExpr) e);
        if (e instanceof ArrayInitializerExpr) return arrayInit((ArrayInitializerExpr) e, K.UNKNOWN);
        if (e instanceof ArrayAccessExpr) {
            ArrayAccessExpr a = (ArrayAccessExpr) e;
            return expr(a.getName()) + "[" + num(a.getIndex()) + "]";
        }
        if (e instanceof ThisExpr) {
            ThisExpr t = (ThisExpr) e;
            if (t.getTypeName().isPresent()) return outerThis(e, t.getTypeName().get().asString());
            return "this";
        }
        if (e instanceof InstanceOfExpr) {
            InstanceOfExpr i = (InstanceOfExpr) e;
            if (i.getPattern().isPresent()) warn(e, "instanceof patterns not supported");
            String tq = typeQName(i.getType(), e);
            if ("java.lang.String".equals(tq)) return "(typeof " + expr(i.getExpression()) + " === 'string')";
            return "(" + expr(i.getExpression()) + " instanceof " + jsTypeName(tq) + ")";
        }
        if (e instanceof LambdaExpr) return lambda((LambdaExpr) e);
        if (e instanceof MethodReferenceExpr) return methodRef((MethodReferenceExpr) e);
        if (e instanceof ClassExpr) return "J.$class(" + jsTypeName(typeQName(((ClassExpr) e).getType(), e)) + ")";
        if (e instanceof VariableDeclarationExpr) return varDecl((VariableDeclarationExpr) e);
        warn(e, "unsupported expression " + e.getClass().getSimpleName());
        return "undefined /* " + abbreviate(e) + " */";
    }

    private String typeQName(Type t, Node ctx) {
        if (t instanceof ClassOrInterfaceType) return resolveTypeQName((ClassOrInterfaceType) t, ctx);
        return t.asString();
    }

    private static String intLiteral(String v) {
        v = v.replace("_", "");
        if (v.startsWith("0x") || v.startsWith("0X")) {
            long l = Long.parseUnsignedLong(v.substring(2), 16);
            if (l > Integer.MAX_VALUE && l <= 0xFFFFFFFFL) return String.valueOf((int) l); // int hex literal wraps
            return String.valueOf(l);
        }
        if (v.length() > 1 && v.startsWith("0") && v.chars().allMatch(Character::isDigit)) return String.valueOf(Long.parseLong(v, 8));
        return v;
    }

    private static String doubleLiteral(String v) {
        v = v.replace("_", "");
        boolean isFloat = v.endsWith("f") || v.endsWith("F");
        String num = v.replaceAll("[fFdD]$", "");
        if (num.endsWith(".")) num = num + "0";
        if (num.startsWith(".")) num = "0" + num;
        if (isFloat) {
            float f = Float.parseFloat(num);
            double d = Double.parseDouble(num);
            if ((double) f != d) return "Math.fround(" + num + ")";
        }
        return num;
    }

    static String jsString(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case 0: sb.append("\\0"); break;
                default:
                    if (c < 0x20 || c > 0x7e) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.append('"').toString();
    }

    // ----- names and members

    private String nameExpr(NameExpr n) {
        String name = n.getNameAsString();
        ResolvedValueDeclaration d;
        try {
            d = n.resolve();
        } catch (Throwable ex) {
            if (localDeclType(n) != null) return safeLocal(name);
            String viaField = fieldByName(n, name);
            if (viaField != null) return viaField;
            // Maybe a type name used as an expression scope.
            SymbolReference<ResolvedTypeDeclaration> r = solveType(name, n);
            if (r != null && r.isSolved()) return jsTypeName(r.getCorrespondingDeclaration().getQualifiedName());
            warn(n, "cannot resolve name " + name + ": " + firstLine(ex.getMessage()));
            return safeLocal(name);
        }
        if (d.isEnumConstant()) {
            return jsTypeName(d.getType().asReferenceType().getQualifiedName()) + "." + name;
        }
        if (d.isField()) {
            ResolvedFieldDeclaration f = d.asField();
            String declQ = f.declaringType().getQualifiedName();
            if (f.isStatic()) return jsTypeName(declQ) + "." + safeMember(name);
            return receiver(n, declQ) + "." + safeMember(name);
        }
        return safeLocal(name);
    }

    /** Fallback field lookup by name (enclosing classes, their user superclasses, static imports). */
    private String fieldByName(NameExpr n, String name) {
        Node p = n.getParentNode().orElse(null);
        while (p != null) {
            if (p instanceof TypeDeclaration) {
                TypeDeclaration<?> t = (TypeDeclaration<?>) p;
                while (t != null) {
                    Optional<FieldDeclaration> f = t.getFieldByName(name);
                    if (f.isPresent()) {
                        String q = t.getFullyQualifiedName().orElse(t.getNameAsString());
                        if (f.get().isStatic()) return jsTypeName(q) + "." + safeMember(name);
                        return receiver(n, q) + "." + safeMember(name);
                    }
                    String sup = userSuperclass(t);
                    t = sup != null ? userTypes.get(sup) : null;
                }
            }
            p = p.getParentNode().orElse(null);
        }
        Optional<CompilationUnit> cu = n.findCompilationUnit();
        if (cu.isPresent()) {
            for (var imp : cu.get().getImports()) {
                if (!imp.isStatic()) continue;
                String q = imp.isAsterisk() ? imp.getNameAsString() : imp.getName().getQualifier().map(Object::toString).orElse("");
                if (!imp.isAsterisk() && !imp.getName().getIdentifier().equals(name)) continue;
                TypeDeclaration<?> t = userTypes.get(q);
                if (t != null && t.getFieldByName(name).isPresent()) return jsTypeName(q) + "." + safeMember(name);
            }
        }
        return null;
    }

    /** JS expression for the object that owns an instance member declared in declQ, seen from node n. */
    private String receiver(Node n, String declQ) {
        String acc = "this";
        Node cur = n;
        while (true) {
            Node encl = enclosingClassLike(cur);
            if (encl == null) { warn(n, "no enclosing class for member of " + declQ); return acc; }
            if (encl instanceof ObjectCreationExpr) {
                // Anonymous classes are emitted as objects with arrow functions: `this` stays the outer instance.
                cur = encl;
                continue;
            }
            TypeDeclaration<?> c = (TypeDeclaration<?>) encl;
            if (isSameOrSubtype(c, declQ)) return acc;
            if (c.getParentNode().orElse(null) instanceof LocalClassDeclarationStmt) {
                if (!acc.equals("this")) warn(n, "nested local class access");
                acc = "$this";
            } else if (isInnerMember(c)) {
                acc = acc + ".$outer";
            } else {
                warn(n, "cannot reach member of " + declQ + " from " + c.getNameAsString());
                return acc;
            }
            cur = c;
        }
    }

    private String outerThis(Node n, String typeName) {
        String acc = "this";
        Node cur = n;
        while (true) {
            Node encl = enclosingClassLike(cur);
            if (encl == null) return acc;
            if (encl instanceof ObjectCreationExpr) { cur = encl; continue; }
            TypeDeclaration<?> c = (TypeDeclaration<?>) encl;
            if (c.getNameAsString().equals(typeName) || c.getFullyQualifiedName().map(q -> q.equals(typeName)).orElse(false)) return acc;
            if (c.getParentNode().orElse(null) instanceof LocalClassDeclarationStmt) acc = "$this";
            else if (isInnerMember(c)) acc = acc + ".$outer";
            cur = c;
        }
    }

    private static Node enclosingClassLike(Node n) {
        Node p = n.getParentNode().orElse(null);
        while (p != null) {
            if (p instanceof TypeDeclaration) return p;
            if (p instanceof ObjectCreationExpr && ((ObjectCreationExpr) p).getAnonymousClassBody().isPresent()) {
                // Only when n is inside the anonymous body (not in the constructor arguments).
                ObjectCreationExpr oce = (ObjectCreationExpr) p;
                Node child = n;
                while (child.getParentNode().orElse(null) != p) child = child.getParentNode().get();
                if (oce.getAnonymousClassBody().get().contains(child)) return p;
            }
            p = p.getParentNode().orElse(null);
        }
        return null;
    }

    private final Map<TypeDeclaration<?>, Set<String>> ancestorCache = new IdentityHashMap<>();

    private boolean isSameOrSubtype(TypeDeclaration<?> c, String q) {
        Set<String> anc = ancestorCache.computeIfAbsent(c, k -> {
            Set<String> s = new HashSet<>();
            try {
                ResolvedReferenceTypeDeclaration r = k.resolve();
                s.add(r.getQualifiedName());
                for (var a : r.getAllAncestors()) s.add(a.getQualifiedName());
            } catch (Throwable ex) {
                warn(k, "cannot resolve ancestors of " + k.getNameAsString() + ": " + firstLine(ex.getMessage()));
                k.getFullyQualifiedName().ifPresent(s::add);
            }
            return s;
        });
        return anc.contains(q);
    }

    private String fieldAccess(FieldAccessExpr fa) {
        String name = fa.getNameAsString();
        Expression scope = fa.getScope();
        // Type or package-qualified names.
        String typeJs = typeReference(scope);
        if (typeJs != null) {
            if (typeJs.startsWith("J.")) libraryMembersUsed.add(typeJs.substring(2) + "." + name);
            // Could be a nested type (e.g. Achievement.Tier) or a static field.
            String nested = typeReference(fa);
            if (nested != null) return nested;
            return typeJs + "." + safeMember(name);
        }
        if (name.equals("length")) {
            ResolvedType t = typeOf(scope);
            if (t != null && t.isArray()) return expr(scope) + ".length";
        }
        if (scope instanceof SuperExpr) return "this." + safeMember(name);
        // static field accessed through an instance
        try {
            ResolvedValueDeclaration v = fa.resolve();
            if (v.isField() && v.asField().isStatic()) return jsTypeName(v.asField().declaringType().getQualifiedName()) + "." + safeMember(name);
        } catch (Throwable ignored) { }
        return expr(scope) + "." + safeMember(name);
    }

    /** If e names a type (simple, nested or package-qualified), its JS name; otherwise null. */
    private String typeReference(Expression e) {
        if (!(e instanceof NameExpr) && !(e instanceof FieldAccessExpr)) return null;
        if (e instanceof NameExpr) {
            try {
                ((NameExpr) e).resolve();
                return null; // a variable or field
            } catch (Throwable ignored) { }
        } else {
            // a.b.c: if a value resolves, it's a field access
            FieldAccessExpr fa = (FieldAccessExpr) e;
            if (isValue(fa)) return null;
        }
        String text = e.toString();
        SymbolReference<ResolvedTypeDeclaration> r = solveType(text, e);
        if (r != null && r.isSolved()) return jsTypeName(r.getCorrespondingDeclaration().getQualifiedName());
        // Fully qualified library names such as java.awt.RenderingHints
        if (Character.isLowerCase(text.charAt(0)) && text.contains(".")) {
            try {
                ResolvedReferenceTypeDeclaration d = typeSolver.solveType(text);
                return jsTypeName(d.getQualifiedName());
            } catch (Throwable ignored) { }
        }
        return null;
    }

    private boolean isValue(FieldAccessExpr fa) {
        try {
            fa.resolve();
            return true;
        } catch (Throwable ex) {
            // Could still be a value whose scope is a value (e.g. obj.field where obj is a variable).
            Expression s = fa.getScope();
            if (s instanceof NameExpr) {
                try { ((NameExpr) s).resolve(); return true; } catch (Throwable ignored) { }
            }
            if (s instanceof FieldAccessExpr) return isValue((FieldAccessExpr) s) && typeReference(s) == null;
            if (!(s instanceof NameExpr) && !(s instanceof FieldAccessExpr)) return true;
            return false;
        }
    }

    // ----- operators

    private String binary(BinaryExpr b) {
        BinaryExpr.Operator op = b.getOperator();
        Expression l = b.getLeft(), r = b.getRight();
        K lk = kindOf(l), rk = kindOf(r);
        switch (op) {
            case PLUS: {
                if (lk == K.STRING || rk == K.STRING) return "(" + strPart(l, lk) + " + " + strPart(r, rk) + ")";
                return arith(b, "+", lk, rk);
            }
            case MINUS: return arith(b, "-", lk, rk);
            case MULTIPLY: return arith(b, "*", lk, rk);
            case DIVIDE: return arith(b, "/", lk, rk);
            case REMAINDER: return arith(b, "%", lk, rk);
            case AND: return "(" + expr(l) + " && " + expr(r) + ")";
            case OR: return "(" + expr(l) + " || " + expr(r) + ")";
            case EQUALS: case NOT_EQUALS: {
                String o = op == BinaryExpr.Operator.EQUALS ? " === " : " !== ";
                boolean charNum = (lk == K.CHAR) != (rk == K.CHAR) && (numeric(lk) && numeric(rk));
                String ls = charNum ? num(l) : expr(l), rs = charNum ? num(r) : expr(r);
                return "(" + ls + o + rs + ")";
            }
            case LESS: case GREATER: case LESS_EQUALS: case GREATER_EQUALS: {
                boolean bothChar = lk == K.CHAR && rk == K.CHAR;
                String ls = bothChar ? expr(l) : num(l), rs = bothChar ? expr(r) : num(r);
                return "(" + ls + " " + op.asString() + " " + rs + ")";
            }
            case BINARY_AND: case BINARY_OR: case XOR: {
                if (lk == K.BOOLEAN || rk == K.BOOLEAN) {
                    String o = op == BinaryExpr.Operator.XOR ? " !== " : (op == BinaryExpr.Operator.BINARY_AND ? " & " : " | ");
                    if (op == BinaryExpr.Operator.XOR) return "(" + expr(l) + o + expr(r) + ")";
                    return "!!(" + expr(l) + o + expr(r) + ")";
                }
                if (lk == K.LONG || rk == K.LONG) warn(b, "bitwise op on long");
                return "(" + num(l) + " " + op.asString() + " " + num(r) + ")";
            }
            case LEFT_SHIFT: case SIGNED_RIGHT_SHIFT: case UNSIGNED_RIGHT_SHIFT:
                if (lk == K.LONG) warn(b, "shift on long");
                return "(" + num(l) + " " + op.asString() + " " + num(r) + ")";
        }
        warn(b, "unsupported operator " + op);
        return "(" + expr(l) + " " + op.asString() + " " + expr(r) + ")";
    }

    private String arith(BinaryExpr b, String op, K lk, K rk) {
        K res = promote(lk, rk);
        if (res == K.UNKNOWN) warn(b, "unknown numeric type in `" + abbreviate(b) + "`");
        String ls = num(b.getLeft()), rs = num(b.getRight());
        if (op.equals("/") && (res == K.INT || res == K.LONG)) return "J.$idiv(" + ls + ", " + rs + ")";
        if (op.equals("%") && (res == K.INT || res == K.LONG)) return "J.$imod(" + ls + ", " + rs + ")";
        if (res == K.INT && (op.equals("*"))) return "Math.imul(" + ls + ", " + rs + ")";
        if (res == K.INT) return "((" + ls + " " + op + " " + rs + ") | 0)";
        if (res == K.FLOAT) return "Math.fround(" + ls + " " + op + " " + rs + ")";
        return "(" + ls + " " + op + " " + rs + ")";
    }

    private static K promote(K a, K b) {
        if (!numeric(a) || !numeric(b)) return K.UNKNOWN;
        if (a == K.DOUBLE || b == K.DOUBLE) return K.DOUBLE;
        if (a == K.FLOAT || b == K.FLOAT) return K.FLOAT;
        if (a == K.LONG || b == K.LONG) return K.LONG;
        return K.INT;
    }

    /** Operand of a string concatenation, converted the way Java's String.valueOf would. */
    private String strPart(Expression e, K k) {
        String js = expr(e);
        switch (k) {
            case STRING: return js;
            case INT: case LONG: case BOOLEAN: return "(" + js + ")";
            case CHAR: return js;
            case DOUBLE: return "J.$d2s(" + js + ")";
            case FLOAT: return "J.$f2s(" + js + ")";
            case NULL: return "\"null\"";
            default: return "J.$str(" + js + ")";
        }
    }

    private String unary(UnaryExpr u) {
        Expression x = u.getExpression();
        K k = kindOf(x);
        switch (u.getOperator()) {
            case LOGICAL_COMPLEMENT: return "!" + expr(x);
            case MINUS: return k == K.INT ? "(-" + num(x) + " | 0)" : "(-" + num(x) + ")";
            case PLUS: return "(+" + num(x) + ")";
            case BITWISE_COMPLEMENT: return "(~" + num(x) + ")";
            case PREFIX_INCREMENT: case PREFIX_DECREMENT: case POSTFIX_INCREMENT: case POSTFIX_DECREMENT: {
                if (k == K.CHAR || k == K.FLOAT) warn(u, "increment on " + k);
                String t = expr(x);
                String o = u.getOperator().asString();
                return u.isPrefix() ? "(" + o + t + ")" : "(" + t + o + ")";
            }
        }
        warn(u, "unsupported unary");
        return expr(x);
    }

    private String assign(AssignExpr a) {
        Expression target = a.getTarget(), value = a.getValue();
        String t = expr(target);
        K tk = kindOf(target), vk = kindOf(value);
        AssignExpr.Operator op = a.getOperator();
        if (op == AssignExpr.Operator.ASSIGN) {
            String v = value instanceof ArrayInitializerExpr ? arrayInit((ArrayInitializerExpr) value, K.UNKNOWN) : convert(expr(value), vk, tk);
            return t + " = " + v;
        }
        String bop;
        switch (op) {
            case PLUS: bop = "+"; break;
            case MINUS: bop = "-"; break;
            case MULTIPLY: bop = "*"; break;
            case DIVIDE: bop = "/"; break;
            case REMAINDER: bop = "%"; break;
            case BINARY_AND: bop = "&"; break;
            case BINARY_OR: bop = "|"; break;
            case XOR: bop = "^"; break;
            case LEFT_SHIFT: bop = "<<"; break;
            case SIGNED_RIGHT_SHIFT: bop = ">>"; break;
            case UNSIGNED_RIGHT_SHIFT: bop = ">>>"; break;
            default: bop = "?";
        }
        if (tk == K.STRING && op == AssignExpr.Operator.PLUS) return t + " += " + strPart(value, vk);
        if (tk == K.BOOLEAN) {
            if (op == AssignExpr.Operator.BINARY_AND) return t + " = !!(" + t + " & " + expr(value) + ")";
            if (op == AssignExpr.Operator.BINARY_OR) return t + " = !!(" + t + " | " + expr(value) + ")";
            if (op == AssignExpr.Operator.XOR) return t + " = (" + t + " !== " + expr(value) + ")";
        }
        if (bop.length() >= 2 || bop.equals("&") || bop.equals("|") || bop.equals("^")) {
            return t + " " + op.asString() + " " + num(value);
        }
        String v = num(value);
        K res = promote(tk, vk);
        if (res == K.UNKNOWN) warn(a, "unknown type in compound assignment `" + abbreviate(a) + "`");
        String combined;
        if ((res == K.INT || res == K.LONG) && bop.equals("/")) combined = "J.$idiv(" + t + ", " + v + ")";
        else if ((res == K.INT || res == K.LONG) && bop.equals("%")) combined = "J.$imod(" + t + ", " + v + ")";
        else combined = "(" + t + " " + bop + " " + v + ")";
        switch (tk) {
            case INT:
                if (res == K.INT && bop.equals("*")) return t + " = Math.imul(" + t + ", " + v + ")";
                if (res == K.INT) return t + " = " + combined + " | 0";
                return t + " = J.$i(" + combined + ")";
            case LONG:
                if (res == K.LONG) return t + " = " + combined;
                return t + " = J.$l(" + combined + ")";
            case FLOAT: return t + " = Math.fround" + (combined.startsWith("(") ? combined : "(" + combined + ")");
            case DOUBLE: return t + " = " + combined;
            case CHAR: warn(a, "compound assignment on char"); return t + " = " + combined;
            default:
                warn(a, "compound assignment on " + tk);
                return t + " " + op.asString() + " " + v;
        }
    }

    private String cast(CastExpr c) {
        Expression x = c.getExpression();
        K from = kindOf(x);
        String js = expr(x);
        String ts = c.getType().asString();
        switch (ts) {
            case "int":
                if (from == K.DOUBLE || from == K.FLOAT) return "J.$i(" + js + ")";
                if (from == K.LONG) return "(" + js + " | 0)";
                if (from == K.CHAR) return "J.$c(" + js + ")";
                if (from != K.INT) warn(c, "(int) cast from " + from);
                return js;
            case "long":
                if (from == K.DOUBLE || from == K.FLOAT) return "J.$l(" + js + ")";
                if (from == K.CHAR) return "J.$c(" + js + ")";
                return js;
            case "short": return "J.$i2s(" + (from == K.DOUBLE || from == K.FLOAT ? "J.$i(" + js + ")" : from == K.CHAR ? "J.$c(" + js + ")" : js) + ")";
            case "byte": return "J.$i2b(" + (from == K.DOUBLE || from == K.FLOAT ? "J.$i(" + js + ")" : from == K.CHAR ? "J.$c(" + js + ")" : js) + ")";
            case "char":
                if (from == K.CHAR) return js;
                if (from == K.DOUBLE || from == K.FLOAT) return "String.fromCharCode(J.$i(" + js + ") & 0xFFFF)";
                return "String.fromCharCode(" + js + " & 0xFFFF)";
            case "float":
                if (from == K.CHAR) return "J.$c(" + js + ")";
                if (from == K.FLOAT || from == K.INT && isSmallIntLiteral(x)) return js;
                return "Math.fround(" + js + ")";
            case "double":
                if (from == K.CHAR) return "J.$c(" + js + ")";
                return js;
            default:
                return js; // reference casts are unchecked in JS
        }
    }

    private static boolean isSmallIntLiteral(Expression x) {
        return x instanceof IntegerLiteralExpr;
    }

    // ----- calls

    private List<K> paramKindsOf(ResolvedMethodLikeDeclaration m) {
        List<K> ks = new ArrayList<>();
        try {
            for (int i = 0; i < m.getNumberOfParams(); i++) {
                ResolvedType t = m.getParam(i).getType();
                ks.add(m.getParam(i).isVariadic() ? K.REF : kindOf(t));
            }
        } catch (Throwable ex) {
            return null;
        }
        return ks;
    }

    private String args(List<Expression> args, List<K> paramKinds) {
        List<String> res = new ArrayList<>();
        for (int i = 0; i < args.size(); i++) {
            Expression a = args.get(i);
            K pk = paramKinds != null && i < paramKinds.size() ? paramKinds.get(i) : K.UNKNOWN;
            String js = a instanceof ArrayInitializerExpr ? arrayInit((ArrayInitializerExpr) a, K.UNKNOWN) : expr(a);
            res.add(convert(js, kindOf(a), pk));
        }
        return String.join(", ", res);
    }

    private String methodCall(MethodCallExpr mc) {
        String name = mc.getNameAsString();
        ResolvedMethodDeclaration rm = null;
        try {
            rm = mc.resolve();
        } catch (Throwable ex) {
            warn(mc, "cannot resolve method " + abbreviate(mc) + ": " + firstLine(ex.getMessage()));
        }
        List<K> pks = rm != null ? paramKindsOf(rm) : null;
        String declQ = rm != null ? rm.declaringType().getQualifiedName() : null;
        MethodDeclaration userDecl = null;
        if (rm != null) {
            try {
                Optional<Node> ast = rm.toAst();
                if (ast.isPresent() && ast.get() instanceof MethodDeclaration) userDecl = (MethodDeclaration) ast.get();
            } catch (Throwable ignored) { }
        }

        Optional<Expression> scopeOpt = mc.getScope();
        String argJs = args(mc.getArguments(), pks);

        if (userDecl != null) {
            String jsName = methodJsName(userDecl);
            if (scopeOpt.isEmpty()) {
                if (rm.isStatic()) return jsTypeName(declQ) + "." + jsName + "(" + argJs + ")";
                return receiver(mc, declQ) + "." + jsName + "(" + argJs + ")";
            }
            Expression scope = scopeOpt.get();
            if (scope instanceof SuperExpr) return "super." + jsName + "(" + argJs + ")";
            String tr = typeReference(scope);
            if (tr != null) return tr + "." + jsName + "(" + argJs + ")";
            // static method called through an instance: JS statics live on the class
            if (rm.isStatic()) return jsTypeName(declQ) + "." + jsName + "(" + argJs + ")";
            return expr(scope) + "." + jsName + "(" + argJs + ")";
        }

        // Library method.
        if (scopeOpt.isEmpty()) {
            if (rm != null && rm.isStatic()) return jsTypeName(declQ) + "." + name + "(" + argJs + ")";
            if (name.equals("getClass")) return "J.$getClass(this)";
            // inherited from a runtime class (e.g. repaint(), getWidth())
            if (declQ != null) libraryMembersUsed.add(simpleOf(declQ) + "#" + name);
            return (declQ != null ? receiver(mc, declQ) : "this") + "." + name + "(" + argJs + ")";
        }
        Expression scope = scopeOpt.get();
        String tr = typeReference(scope);
        if (tr != null) {
            String special = staticLibraryCall(mc, tr, name, argJs);
            if (special != null) return special;
            libraryMembersUsed.add(tr.replaceFirst("^J\\.", "") + "." + name);
            return tr + "." + name + "(" + argJs + ")";
        }
        K sk = kindOf(scope);
        String s = scope instanceof SuperExpr ? "super" : expr(scope);
        if (sk == K.STRING) return stringMethod(mc, s, name);
        if (sk == K.INT || sk == K.LONG || sk == K.DOUBLE || sk == K.FLOAT || sk == K.BOOLEAN || sk == K.CHAR) {
            // methods on boxed values
            switch (name) {
                case "intValue": case "longValue": return "J.$i(" + s + ")";
                case "doubleValue": case "floatValue": return "(" + s + ")";
                case "booleanValue": case "charValue": return s;
                case "equals": return "(" + s + " === " + argJs + ")";
                case "toString": return "J.$str(" + s + ")";
                case "compareTo": return "J.$compare(" + s + ", " + argJs + ")";
            }
            warn(mc, "method on boxed value: " + name);
        }
        if (declQ != null) {
            String simple = simpleOf(declQ);
            libraryMembersUsed.add(simple + "#" + name);
            if (name.equals("remove") && pks != null && pks.size() == 1 && pks.get(0) == K.INT
                    && (declQ.startsWith("java.util.") && (declQ.endsWith("List") || declQ.endsWith("ArrayList")))) {
                return s + ".removeAt(" + argJs + ")";
            }
            if (name.equals("getClass")) return "J.$getClass(" + s + ")";
            // Overloads the runtime cannot tell apart by argument count.
            if (declQ.equals("java.awt.Font") && name.equals("deriveFont") && pks != null && pks.size() == 1 && pks.get(0) == K.INT) {
                return s + ".deriveFontStyle(" + argJs + ")";
            }
        }
        return s + "." + name + "(" + argJs + ")";
    }

    private static String simpleOf(String q) {
        int i = q.lastIndexOf('.');
        return i >= 0 ? q.substring(i + 1) : q;
    }

    private String staticLibraryCall(MethodCallExpr mc, String tr, String name, String argJs) {
        List<Expression> a = mc.getArguments();
        switch (tr) {
            case "J.String":
                if (name.equals("valueOf") && a.size() == 1) {
                    Expression x = a.get(0);
                    K k = kindOf(x);
                    if (k == K.REF) {
                        ResolvedType t = typeOf(x);
                        if (t != null && t.isArray()) return "J.$charArrayToString(" + expr(x) + ")";
                    }
                    return "(\"\" + " + strPart(x, k) + ")";
                }
                if (name.equals("format")) return "J.$format(" + fmtArgs(a) + ")";
                break;
            case "J.System":
                if (name.equals("currentTimeMillis")) return "J.$currentTimeMillis()";
                if (name.equals("nanoTime")) return "J.$nanoTime()";
                break;
            case "J.Math":
                return "J.JMath." + name + "(" + argJs + ")";
        }
        return null;
    }

    /** Arguments of String.format: numbers keep their Java kind so %d/%s/%f render the same. */
    private String fmtArgs(List<Expression> a) {
        List<String> res = new ArrayList<>();
        for (int i = 0; i < a.size(); i++) {
            Expression x = a.get(i);
            K k = kindOf(x);
            String js = expr(x);
            if (i > 0 && (k == K.DOUBLE || k == K.FLOAT)) res.add("J.$dbl(" + js + ")");
            else res.add(js);
        }
        return String.join(", ", res);
    }

    private String stringMethod(MethodCallExpr mc, String s, String name) {
        List<Expression> a = mc.getArguments();
        String a0 = a.size() > 0 ? expr(a.get(0)) : null;
        String a1 = a.size() > 1 ? expr(a.get(1)) : null;
        switch (name) {
            case "length": return s + ".length";
            case "isEmpty": return "(" + s + ".length === 0)";
            case "equals": return "(" + s + " === " + a0 + ")";
            case "equalsIgnoreCase": return "J.$equalsIgnoreCase(" + s + ", " + a0 + ")";
            case "contains": return s + ".includes(" + a0 + ")";
            case "charAt": return s + ".charAt(" + num(a.get(0)) + ")";
            case "substring": case "startsWith": case "endsWith": case "toUpperCase": case "toLowerCase": case "repeat":
                return s + "." + name + "(" + a.stream().map(this::num).collect(Collectors.joining(", ")) + ")";
            case "trim": return "J.$trim(" + s + ")";
            case "indexOf": case "lastIndexOf": {
                String first = kindOf(a.get(0)) == K.INT ? "String.fromCharCode(" + a0 + ")" : a0;
                return s + "." + name + "(" + first + (a1 != null ? ", " + num(a.get(1)) : "") + ")";
            }
            case "replace": {
                return s + ".replaceAll(" + a0 + ", " + a1 + ")";
            }
            case "replaceAll": return "J.$replaceAll(" + s + ", " + a0 + ", " + a1 + ")";
            case "replaceFirst": return "J.$replaceFirst(" + s + ", " + a0 + ", " + a1 + ")";
            case "split": return "J.$split(" + s + ", " + a0 + (a1 != null ? ", " + a1 : "") + ")";
            case "matches": return "J.$matches(" + s + ", " + a0 + ")";
            case "toCharArray": return "Array.from(" + s + ")";
            case "compareTo": return "J.$compareStrings(" + s + ", " + a0 + ")";
            case "compareToIgnoreCase": return "J.$compareStrings(" + s + ".toLowerCase(), " + a0 + ".toLowerCase())";
            case "hashCode": return "J.$stringHash(" + s + ")";
            case "toString": case "intern": return s;
            case "concat": return "(" + s + " + " + a0 + ")";
            case "format": case "formatted": return "J.$format(" + s + ", " + fmtArgs(a) + ")";
        }
        warn(mc, "unsupported String method " + name);
        return s + "." + name + "(" + a.stream().map(this::expr).collect(Collectors.joining(", ")) + ")";
    }

    private String objectCreation(ObjectCreationExpr oc) {
        if (oc.getAnonymousClassBody().isPresent()) return anonymous(oc);
        String q = resolveTypeQName(oc.getType(), oc);
        TypeDeclaration<?> td = userTypes.get(q);
        if (td == null) {
            // local class?
            try {
                ResolvedConstructorDeclaration rc = oc.resolve();
                Optional<Node> ast = rc.toAst();
                if (ast.isPresent() && ast.get() instanceof ConstructorDeclaration) {
                    td = (TypeDeclaration<?>) ast.get().getParentNode().get();
                }
            } catch (Throwable ignored) { }
            if (td == null) {
                // local class without constructors
                SymbolReference<ResolvedTypeDeclaration> r = solveType(oc.getType().getNameAsString(), oc);
                if (r != null && r.isSolved()) {
                    try {
                        Optional<Node> ast = r.getCorrespondingDeclaration().toAst();
                        if (ast.isPresent() && ast.get() instanceof ClassOrInterfaceDeclaration) td = (TypeDeclaration<?>) ast.get();
                    } catch (Throwable ignored) { }
                }
            }
        }
        if (td != null) {
            String js = userJsName(td);
            String ctor;
            List<K> pks = null;
            if (td.getConstructors().isEmpty()) ctor = "$ctor$" + js + "$0";
            else {
                ConstructorDeclaration cd = null;
                try {
                    ResolvedConstructorDeclaration rc = oc.resolve();
                    pks = paramKindsOf(rc);
                    Optional<Node> ast = rc.toAst();
                    if (ast.isPresent()) cd = (ConstructorDeclaration) ast.get();
                } catch (Throwable ex) {
                    warn(oc, "cannot resolve constructor " + abbreviate(oc) + ": " + firstLine(ex.getMessage()));
                }
                if (cd == null) {
                    for (ConstructorDeclaration c : td.getConstructors()) if (c.getParameters().size() == oc.getArguments().size()) cd = c;
                    if (cd == null) cd = td.getConstructors().get(0);
                }
                ctor = ctorJsName(cd);
                if (pks == null) pks = paramKinds(cd.getParameters());
            }
            String outer = isInnerMember(td) ? receiver(oc, ((TypeDeclaration<?>) td.getParentNode().get()).getFullyQualifiedName().orElse("")) : "";
            return "new " + js + "(" + outer + ")." + ctor + "(" + args(oc.getArguments(), pks) + ")";
        }
        List<K> pks = null;
        try { pks = paramKindsOf(oc.resolve()); } catch (Throwable ignored) { }
        String tj = jsTypeName(q);
        if (q.equals("java.awt.Color") && pks != null && !pks.isEmpty() && pks.get(0) == K.FLOAT) {
            return "J.Color.$fromFloats(" + args(oc.getArguments(), pks) + ")";
        }
        libraryMembersUsed.add("new " + tj.substring(2) + "/" + oc.getArguments().size());
        return "new " + tj + "(" + args(oc.getArguments(), pks) + ")";
    }

    private String anonymous(ObjectCreationExpr oc) {
        StringBuilder sb = new StringBuilder();
        String q = resolveTypeQName(oc.getType(), oc);
        sb.append("J.$impl(").append(jsTypeName(q)).append(", {\n");
        int saved = indent;
        StringBuilder savedOut = out;
        out = new StringBuilder();
        indent = saved + 1;
        for (BodyDeclaration<?> m : oc.getAnonymousClassBody().get()) {
            if (m instanceof MethodDeclaration) {
                MethodDeclaration md = (MethodDeclaration) m;
                line(md.getNameAsString() + ": (" + params(md.getParameters()) + ") => {");
                indent++;
                returnKinds.push(kindOfType(md.getType()));
                emitBlockBody(md.getBody().get());
                returnKinds.pop();
                indent--;
                line("},");
            } else {
                warn(m, "anonymous class member not supported: " + m.getClass().getSimpleName());
            }
        }
        String body = out.toString();
        out = savedOut;
        indent = saved;
        sb.append(body);
        for (int i = 0; i < indent; i++) sb.append("    ");
        sb.append("})");
        return sb.toString();
    }

    private String arrayCreation(ArrayCreationExpr ac) {
        if (ac.getInitializer().isPresent()) return arrayInit(ac.getInitializer().get(), kindOfType(ac.getElementType()));
        List<String> dims = new ArrayList<>();
        for (ArrayCreationLevel l : ac.getLevels()) {
            if (l.getDimension().isPresent()) dims.add(num(l.getDimension().get()));
        }
        int totalLevels = ac.getLevels().size();
        String def = totalLevels > dims.size() ? "null" : defaultValue(ac.getElementType());
        return "J.$newArray([" + String.join(", ", dims) + "], " + def + ")";
    }

    private String arrayInit(ArrayInitializerExpr ai, K elem) {
        return "[" + ai.getValues().stream().map(v -> v instanceof ArrayInitializerExpr ? arrayInit((ArrayInitializerExpr) v, elem)
                : convert(expr(v), kindOf(v), elem)).collect(Collectors.joining(", ")) + "]";
    }

    private String lambda(LambdaExpr l) {
        String ps = l.getParameters().stream().map(p -> safeLocal(p.getNameAsString())).collect(Collectors.joining(", "));
        Statement body = l.getBody();
        if (body instanceof ExpressionStmt) return "((" + ps + ") => " + expr(((ExpressionStmt) body).getExpression()) + ")";
        int saved = indent;
        StringBuilder savedOut = out;
        out = new StringBuilder();
        indent = saved + 1;
        returnKinds.push(K.UNKNOWN);
        emitBlockBody((BlockStmt) body);
        returnKinds.pop();
        String b = out.toString();
        out = savedOut;
        indent = saved;
        StringBuilder sb = new StringBuilder("((" + ps + ") => {\n").append(b);
        for (int i = 0; i < indent; i++) sb.append("    ");
        return sb.append("})").toString();
    }

    private String methodRef(MethodReferenceExpr mr) {
        String id = mr.getIdentifier();
        Expression scope = mr.getScope();
        String tr = scope instanceof TypeExpr ? jsTypeName(typeQName(((TypeExpr) scope).getType(), mr)) : typeReference(scope);
        if (tr != null) {
            String simple = tr.replaceFirst("^J\\.", "");
            if (simple.equals("String") && id.equals("compareToIgnoreCase")) return "((a, b) => J.$compareStrings(a.toLowerCase(), b.toLowerCase()))";
            if (simple.equals("String") && id.equals("compareTo")) return "((a, b) => J.$compareStrings(a, b))";
            if ((simple.equals("Integer") && id.equals("intValue")) || (simple.equals("Double") && id.equals("doubleValue"))
                    || (simple.equals("Long") && id.equals("longValue"))) return "((x) => x)";
            // Type::method - static or unbound instance method
            ResolvedMethodDeclaration rm = null;
            try { rm = mr.resolve(); } catch (Throwable ignored) { }
            String jsName = id;
            if (rm != null) {
                try {
                    Optional<Node> ast = rm.toAst();
                    if (ast.isPresent() && ast.get() instanceof MethodDeclaration) jsName = methodJsName((MethodDeclaration) ast.get());
                } catch (Throwable ignored) { }
                if (rm.isStatic()) return "((...a) => " + tr + "." + jsName + "(...a))";
            } else {
                warn(mr, "cannot resolve method reference " + mr + ", assuming instance method");
            }
            return "((o, ...a) => o." + jsName + "(...a))";
        }
        String s = scope instanceof ThisExpr ? "this" : expr(scope);
        String jsName = id;
        try {
            ResolvedMethodDeclaration rm = mr.resolve();
            Optional<Node> ast = rm.toAst();
            if (ast.isPresent() && ast.get() instanceof MethodDeclaration) jsName = methodJsName((MethodDeclaration) ast.get());
        } catch (Throwable ignored) { }
        return "((...a) => " + s + "." + jsName + "(...a))";
    }
}
