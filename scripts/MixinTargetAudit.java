import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import java.util.regex.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Checks compiled mixin selectors against the actual runtime jars without launching Minecraft. */
public class MixinTargetAudit {
    static List<JarFile> jars = new ArrayList<>();
    static Map<String, ClassNode> cache = new HashMap<>();
    static int issues;
    static Object value(AnnotationNode a, String key) {
        if (a.values != null) for (int i = 0; i < a.values.size(); i += 2)
            if (a.values.get(i).equals(key)) return a.values.get(i + 1);
        return null;
    }
    static List<?> list(Object o) { return o == null ? List.of() : o instanceof List<?> l ? l : List.of(o); }
    static List<AnnotationNode> annotations(List<AnnotationNode> a, List<AnnotationNode> b) {
        List<AnnotationNode> all = new ArrayList<>();
        if (a != null) all.addAll(a);
        if (b != null) all.addAll(b);
        return all;
    }
    static ClassNode read(byte[] bytes) {
        ClassNode n = new ClassNode();
        new ClassReader(bytes).accept(n, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return n;
    }
    static ClassNode target(String name) throws Exception {
        if (cache.containsKey(name)) return cache.get(name);
        for (JarFile jar : jars) {
            var e = jar.getJarEntry(name + ".class");
            if (e != null) {
                ClassNode n = read(jar.getInputStream(e).readAllBytes());
                cache.put(name, n);
                return n;
            }
        }
        return null;
    }
    static void issue(String mixin, String detail) { issues++; System.out.println(mixin + ": " + detail); }
    static String inferred(String name) {
        name = name.substring(name.lastIndexOf('$') + 1).replaceFirst("^(get|set|is|call|invoke)", "");
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }
    static boolean matches(MethodNode m, String selector) {
        int p = selector.indexOf('(');
        return p < 0 ? m.name.equals(selector) : (m.name + m.desc).equals(selector);
    }
    static void checkHandler(String mixin, MethodNode handler, AnnotationNode annotation, List<MethodNode> methods) {
        if (!annotation.desc.endsWith("/Inject;") && !annotation.desc.endsWith("/ModifyReturnValue;")) return;
        Type[] args = Type.getArgumentTypes(handler.desc);
        int first = annotation.desc.endsWith("/ModifyReturnValue;") ? 1 : 0;
        int end = args.length;
        for (int i = first; i < args.length; i++) {
            if (args[i].getDescriptor().contains("/callback/CallbackInfo")) { end = i; break; }
        }
        if (end == first) return;
        for (MethodNode method : methods) {
            Type[] targetArgs = Type.getArgumentTypes(method.desc);
            boolean match = end - first <= targetArgs.length;
            for (int i = first; match && i < end; i++) match = args[i].equals(targetArgs[i - first]);
            if (!match) issue(mixin, "handler arguments " + handler.name + handler.desc + " do not match " + method.name + method.desc);
        }
    }
    static void checkAt(String mixin, AnnotationNode at, List<MethodNode> methods) {
        String kind = (String)value(at, "value");
        String selector = (String)value(at, "target");
        if (selector == null || methods.isEmpty() || !Set.of("INVOKE", "FIELD", "NEW").contains(kind)) return;
        boolean found = false;
        for (MethodNode m : methods) for (AbstractInsnNode ins : m.instructions) {
            if (ins instanceof MethodInsnNode i && ("L" + i.owner + ";" + i.name + i.desc).equals(selector)) found = true;
            if (ins instanceof FieldInsnNode i && ("L" + i.owner + ";" + i.name + ":" + i.desc).equals(selector)) found = true;
            if (ins instanceof TypeInsnNode i && kind.equals("NEW") && i.getOpcode() == Opcodes.NEW && i.desc.equals(selector)) found = true;
        }
        if (!found) issue(mixin, "missing " + kind + " " + selector + " in " + methods.stream().map(m -> m.name).distinct().toList());
    }
    public static void main(String[] args) throws Exception {
        for (int i = 2; i < args.length; i++) jars.add(new JarFile(args[i]));
        String config = Files.readString(Path.of(args[1]));
        Matcher packageMatcher = Pattern.compile("\"package\"\\s*:\\s*\"([^\"]+)\"").matcher(config);
        if (!packageMatcher.find()) throw new IllegalArgumentException("Missing mixin package");
        String basePackage = packageMatcher.group(1).replace('.', '/') + "/";
        Matcher names = Pattern.compile("\"([A-Za-z][A-Za-z0-9_.]*)\"\\s*[,\\]]").matcher(config);
        int checked = 0;
        while (names.find()) {
            String mixin = names.group(1);
            Path file = Path.of(args[0], basePackage + mixin.replace('.', '/') + ".class");
            if (!Files.exists(file)) continue;
            ClassNode n = read(Files.readAllBytes(file));
            for (AnnotationNode a : annotations(n.visibleAnnotations, n.invisibleAnnotations)) {
                if (!a.desc.endsWith("/Mixin;")) continue;
                List<String> targets = new ArrayList<>();
                for (Object t : list(value(a, "value"))) targets.add(((Type)t).getInternalName());
                for (Object t : list(value(a, "targets"))) targets.add(t.toString().replace('.', '/'));
                for (String name : targets) {
                    ClassNode t = target(name);
                    if (t == null) { System.out.println("SKIP " + mixin + " (target jar not supplied: " + name + ")"); continue; }
                    checked++;
                    for (FieldNode f : n.fields) for (AnnotationNode fa : annotations(f.visibleAnnotations, f.invisibleAnnotations)) {
                        if (fa.desc.endsWith("/Shadow;") && t.fields.stream().noneMatch(tf -> tf.name.equals(f.name) && tf.desc.equals(f.desc)))
                            issue(mixin, "missing shadow field " + f.name + ":" + f.desc);
                    }
                    for (MethodNode m : n.methods) for (AnnotationNode ma : annotations(m.visibleAnnotations, m.invisibleAnnotations)) {
                        if (ma.desc.endsWith("/Accessor;")) {
                            String field = (String)value(ma, "value");
                            if (field == null || field.isEmpty()) field = inferred(m.name);
                            Type mt = Type.getMethodType(m.desc);
                            String desc = mt.getReturnType().equals(Type.VOID_TYPE) ? mt.getArgumentTypes()[0].getDescriptor() : mt.getReturnType().getDescriptor();
                            String f = field;
                            if (t.fields.stream().noneMatch(tf -> tf.name.equals(f) && tf.desc.equals(desc))) issue(mixin, "missing accessor " + field + ":" + desc);
                        } else if (ma.desc.endsWith("/Invoker;")) {
                            String method = (String)value(ma, "value");
                            if (method == null || method.isEmpty()) method = inferred(m.name);
                            String desc = m.desc;
                            if (method.equals("<init>")) desc = desc.substring(0, desc.indexOf(')') + 1) + "V";
                            String selector = method + desc;
                            if (t.methods.stream().noneMatch(tm -> matches(tm, selector))) issue(mixin, "missing invoker " + selector);
                        } else if (value(ma, "method") != null) {
                            List<MethodNode> selected = new ArrayList<>();
                            for (Object s : list(value(ma, "method"))) {
                                var found = t.methods.stream().filter(tm -> matches(tm, s.toString())).toList();
                                if (found.isEmpty()) issue(mixin, "missing method " + s);
                                selected.addAll(found);
                            }
                            for (Object at : list(value(ma, "at"))) if (at instanceof AnnotationNode an) checkAt(mixin, an, selected);
                            checkHandler(mixin, m, ma, selected);
                        }
                    }
                }
            }
        }
        System.out.println("Checked " + checked + " mixin targets; " + issues + " selector issues. Injection locals and runtime behavior require separate validation.");
        for (JarFile j : jars) j.close();
    }
}
