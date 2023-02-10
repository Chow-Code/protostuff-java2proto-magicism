package org.alan.gen;

import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.WireFormat;
import com.dyuproject.protostuff.runtime.EnumIO;
import com.dyuproject.protostuff.runtime.HasSchema;
import com.dyuproject.protostuff.runtime.MappedSchema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
import net.webby.protostuff.runtime.Pair;
import net.webby.protostuff.runtime.ReflectionUtil;
import net.webby.protostuff.runtime.RuntimeFieldType;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

/**
 * Created on 2017/6/14.
 *
 * @author Alan
 * @since 1.0
 */
public class Java2Pb {
    /* 协议*/
    public final Schema<?> schema;
    /* 本类message*/
    public StringBuilder message = new StringBuilder();

    public Java2Pb(Schema<?> schema, String pkg) {
        if (!(schema instanceof RuntimeSchema)) {
            throw new IllegalArgumentException("schema instance must be a RuntimeSchema");
        }

        this.schema = schema;
    }

    public Java2Pb gen() {
        generateInternal();
        return this;
    }

    public String toMesage() {
        return message.toString();
    }

    protected void generateInternal() {
        if (schema.typeClass().isEnum()) {
            doGenerateEnum(schema.typeClass());
        } else {
            doGenerateMessage(schema);
        }
    }

    protected void doGenerateEnum(Class<?> enumClass) {

        message.append("enum ").append(enumClass.getSimpleName()).append(" {").append("\n");

        for (Object val : enumClass.getEnumConstants()) {
            Enum<?> v = (Enum<?>) val;
            String desc = "";
            message.append("  ").append(val).append(" = ").append(v.ordinal()).append(";");
            try {
              Field field =  enumClass.getDeclaredField((v.name()));
              ProtoDesc protoDesc = field.getAnnotation(ProtoDesc.class);
                if (protoDesc != null) {
                    desc = protoDesc.value();
                }
            } catch (Exception e) {
                throw new RuntimeException("generate enum fail", e);
            }
            if (!StringUtils.isEmpty(desc)) {
                message.append("  //").append(desc);
            }
            message.append("\n");
        }

        message.append("}").append("\n\n");

    }

    protected void doGenerateMessage(Schema<?> schema) {

        if (!(schema instanceof RuntimeSchema)) {
            throw new IllegalStateException("invalid schema type " + schema.getClass());
        }

        RuntimeSchema<?> runtimeSchema = (RuntimeSchema<?>) schema;

        message.append("message ").append(runtimeSchema.messageName()).append(" {").append("\n");

        try {
            Field fieldsField = MappedSchema.class.getDeclaredField("fields");
            fieldsField.setAccessible(true);
            com.dyuproject.protostuff.runtime.MappedSchema.Field<?>[] fields = (com.dyuproject.protostuff.runtime.MappedSchema.Field<?>[]) fieldsField
                    .get(runtimeSchema);

            for (int i = 0; i != fields.length; ++i) {
                String desc = "";
                com.dyuproject.protostuff.runtime.MappedSchema.Field<?> field = fields[i];
                String fieldType = null;
                if (field.type == WireFormat.FieldType.ENUM) {

                    Field reflectionField = field.getClass().getDeclaredField("val$eio");
                    reflectionField.setAccessible(true);
                    EnumIO enumIO = (EnumIO) reflectionField.get(field);
                    fieldType = enumIO.enumClass.getSimpleName();
                    Field fieldTmp = schema.typeClass().getDeclaredField(field.name);
                    ProtoDesc protoDesc = fieldTmp.getAnnotation(ProtoDesc.class);
                    if (protoDesc != null) {
                        desc = protoDesc.value();
                    }
                } else if (field.type == WireFormat.FieldType.MESSAGE) {
                    if (field.repeated) {
                        Field typeClassField = field.getClass().getField("typeClass");
                        typeClassField.setAccessible(true);
                        Class<?> tmpClass = (Class<?>) typeClassField.get(field);
                        fieldType = tmpClass.getSimpleName();
                    } else {
                        Pair<RuntimeFieldType, Class<?>> normField = ReflectionUtil.normalizeFieldClass(field);
                        if (normField == null) {
                            throw new IllegalStateException(
                                    "unknown fieldClass " + field.getClass());
                        }

                        Class<?> fieldClass = normField.getSecond();
                        if (normField.getFirst() == RuntimeFieldType.RuntimeRepeatedField) {

                        } else if (normField.getFirst() == RuntimeFieldType.RuntimeMessageField) {

                            Field typeClassField = fieldClass.getDeclaredField("typeClass");
                            typeClassField.setAccessible(true);
                            Class<?> typeClass = (Class<?>) typeClassField.get(field);

                            Field hasSchemaField = fieldClass.getDeclaredField("hasSchema");
                            hasSchemaField.setAccessible(true);

                            HasSchema<?> hasSchema = (HasSchema<?>) hasSchemaField.get(field);
                            Schema<?> fieldSchema = hasSchema.getSchema();
                            fieldType = fieldSchema.messageName();
                        } else {
                            throw new IllegalStateException("field type not support, typeclass=" + schema.typeClass() + ",fieldName=" + field.name);
                        }
                    }

                    Field fieldTmp = schema.typeClass().getDeclaredField(field.name);
                    ProtoDesc protoDesc = fieldTmp.getAnnotation(ProtoDesc.class);
                    if (protoDesc != null) {
                        desc = protoDesc.value();
                    }
                } else {
                    fieldType = field.type.toString().toLowerCase();
                    Class clazz = schema.typeClass();
                    Field fieldTmp = null;
                    try {
                        fieldTmp = clazz.getDeclaredField(field.name);
                    } catch (NoSuchFieldException e) {
                        fieldTmp = clazz.getSuperclass().getDeclaredField(field.name);
                    } catch (SecurityException e) {
                        System.out.println(e);
                    }
                    ProtoDesc protoDesc = fieldTmp.getAnnotation(ProtoDesc.class);
                    if (protoDesc != null) {
                        desc = protoDesc.value();
                    }
                }

                message.append("  ");

//                if (field.type == WireFormat.FieldType.ENUM) {
//                    message.append("required ");
//                } else {
                    if (field.repeated) {
                        message.append("repeated ");
                    } else {
                        message.append("optional ");
                    }
//                }
                message.append(fieldType).append(" ").append(field.name).append(" = ").append(field.number).append(";");
                if (!StringUtils.isEmpty(desc)) {
                    message.append("  //").append(desc);
                }
                message.append("\n");
            }

        } catch (Exception e) {
            throw new RuntimeException("generate proto fail", e);
        }

        message.append("}").append("\n\n");

    }

    private static String getMapFieldType(Pair<Type, Type> keyValue) {
        if (keyValue.getFirst() == String.class) {
            if (keyValue.getSecond() == String.class) {
                return "MapStringString";
            } else {
                return "MapStringObject";
            }
        }
        return "MapObjectObject";
    }
}

