package org.alan.gen;

import cn.hutool.core.io.file.FileWriter;
import cn.hutool.core.util.ClassUtil;
import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created on 2017/8/31.
 *
 * @author Alan
 * @since 1.0
 */
public class ToOneFile {
    public static void main(String[] args) throws ClassNotFoundException {
        String searchPackage = args[0];
        String genToDir = args[1];
        String clazzName = args[2];
        String importFiles = null;
        if (args.length > 3) {
            importFiles = args[3];
        }
        java2PbMessage(searchPackage, importFiles, null, genToDir, Class.forName(clazzName).asSubclass(Annotation.class));
    }

    public static void java2PbMessage(String searchPackage, String importFiles, String pkg, String genToDir, Class<? extends Annotation> protoClazz) {
        String[] sps = searchPackage.split(";");
        List<Class<?>> classList = new ArrayList<>();
        for (String pk : sps) {
            Set<Class<?>> classes = ClassUtil.scanPackage(pk);
            classes = classes.stream().filter(c -> c.getAnnotation(protoClazz) != null).collect(Collectors.toSet());
            classList.addAll(classes);
        }
        Class<?>[] classes1 = classList.toArray(new Class[0]);
        List<Class<?>> classListTmp = Arrays.stream(classes1).sorted(Comparator.comparingInt(clazz -> {
            int i = 0;
            try {
                Annotation annotation = clazz.getAnnotation(protoClazz);
                Class<?> c = annotation.getClass();
                Object cmd = c.getMethod("cmd").invoke(annotation);
                Object messageType = c.getMethod("messageType").invoke(annotation);
                if (messageType instanceof Integer && cmd instanceof Integer) {
                    int messageType1 = (Integer) messageType;
                    int cmd1 = (Integer) cmd;
                    i += messageType1 * 10000;
                    i += cmd1;
                    if (messageType1 == 0 && cmd1 == 0) {
                        String simpleName = clazz.getSimpleName();
                        char[] charArray = simpleName.toCharArray();
                        for (int j = 0; j < charArray.length; j++) {
                            char c1 = charArray[j];
                            i += c1 * (charArray.length - j);
                        }
                    }
                }
                return i;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 0;
        })).collect(Collectors.toList());
        StringBuilder sb = new StringBuilder();
        if (importFiles != null) {
            String[] imports = importFiles.split(";");
            for (String imp : imports) {
                sb.append("import \"").append(imp).append("\";\n");
            }
            sb.append("\n\n");
        }
        //String fileName = dir + "/Protocol.proto";
        classListTmp.forEach(clazz -> {
            System.out.print(clazz);
            Schema<?> schema = RuntimeSchema.getSchema(clazz);
            Annotation annotation = clazz.getAnnotation(protoClazz);
            try {
                Class<?> c = annotation.getClass();
                Object obj2 = c.getMethod("messageType").invoke(annotation);
                Object obj3 = c.getMethod("cmd").invoke(annotation);
                Object obj4 = c.getMethod("desc").invoke(annotation);
                Object obj5 = c.getMethod("privately").invoke(annotation);
                if (((boolean) obj5)) {
                    System.out.println("  内部消息, 跳过..");
                    return;
                }
                if ("0".equals(obj2.toString())) {
                    String note = String.format("//%s | -- | %s", obj4, clazz.getSimpleName());
                    sb.append(note).append("\n");
                } else {
                    String note = String.format("//%s | %s-%s | %s", obj4, obj2, obj3, clazz.getSimpleName());
                    sb.append(note).append("\n");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }


            Java2Pb pbGen = new Java2Pb(schema, pkg).gen();
            String content = pbGen.toMesage();
            sb.append(content);
            System.out.println("  完成.");
        });
        FileWriter writer = new FileWriter(genToDir);
        writer.write(sb.toString(), false);
    }
}
