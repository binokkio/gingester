package b.nana.technology.gingester.core.configuration;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializerFactory;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.Objects.requireNonNull;

/**
 * Iffy deserializer that adds support for flags and key-less ordered
 * fields during deserialization. Meant to be used for Gingester transformer
 * parameters deserialization which does not need to be very performant.
 */
public final class FlagOrderDeserializer<T> extends StdDeserializer<T> implements ContextualDeserializer {

    private final Class<T> target;
    private final JavaType javaType;
    private final List<String> order;
    private final Set<String> allFields;
    private final Set<String> booleanFields;

    public FlagOrderDeserializer() {
        // dummy constructor so an instance can be created on which `createContextual` can be called
        super(Object.class);
        target = null;
        javaType = null;
        order = null;
        allFields = null;
        booleanFields = null;
    }

    public FlagOrderDeserializer(Class<T> target) {
        super(target);
        this.target = target;
        this.javaType = TypeFactory.defaultInstance().constructType(target);
        this.order = Arrays.asList(requireNonNull(target.getAnnotation(Order.class), target + " is missing @Order annotation").value());
        this.allFields = Arrays.stream(this.target.getFields())
                .map(Field::getName).collect(Collectors.toSet());
        this.booleanFields = Arrays.stream(this.target.getFields())
                .filter(f -> f.getType().equals(Boolean.TYPE))
                .map(Field::getName)
                .collect(Collectors.toSet());
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext context, BeanProperty property) {
        // Thank you https://github.com/FasterXML/jackson-databind/issues/2768 !
        return new FlagOrderDeserializer<>(context.getContextualType().getRawClass());
    }

    @Override
    public T deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException {

        ObjectCodec objectCodec = jsonParser.getCodec();
        JsonNode json = objectCodec.readTree(jsonParser);

        boolean isParametersObject = json.isObject() &&
                StreamSupport.stream(Spliterators.spliteratorUnknownSize(json.fieldNames(), Spliterator.ORDERED), false)
                        .allMatch(allFields::contains);

        if (!isParametersObject) {

            // normalize to always having an array of parameters, even if only 1 was given
            // note that this is not compatible with root-is-array parameters
            ArrayNode array;
            if (!json.isArray()) {
                array = JsonNodeFactory.instance.arrayNode(1);
                array.add(json);
            } else {
                array = (ArrayNode) json;
            }

            ObjectNode result = JsonNodeFactory.instance.objectNode();
            int orderCounter = 0;
            for (int i = 0; i < array.size(); i++) {
                JsonNode entry = array.get(i);

                // handle flags
                if (entry.isTextual()) {
                    String text = entry.textValue();
                    if (text.startsWith("!")) {
                        String possibleBooleanField = text.substring(1);
                        if (booleanFields.contains(possibleBooleanField)) {
                            result.put(possibleBooleanField, false);
                            continue;
                        }
                    } else if (text.endsWith("!")) {
                        String possibleBooleanField = text.substring(0, text.length() - 1);
                        if (booleanFields.contains(possibleBooleanField)) {
                            result.put(possibleBooleanField, true);
                            continue;
                        }
                    }
                }

                // at this point, the entry is not a flag, so assign it to the next ordered field
                result.set(order.get(orderCounter++), entry);
            }

            json = result;
        }

        wrapPolymorphicTypes(json);

        DeserializationConfig config = context.getConfig();
        JsonDeserializer<Object> defaultDeserializer = BeanDeserializerFactory.instance.buildBeanDeserializer(context, javaType, config.introspect(javaType));

        if (defaultDeserializer instanceof ResolvableDeserializer) {
            ((ResolvableDeserializer) defaultDeserializer).resolve(context);
        }

        JsonParser treeParser = objectCodec.treeAsTokens(json);
        config.initialize(treeParser);

        if (treeParser.getCurrentToken() == null) {
            treeParser.nextToken();
        }

        // noinspection unchecked
        return (T) defaultDeserializer.deserialize(treeParser, context);
    }

    private void wrapPolymorphicTypes(JsonNode jsonNode) {
        for (Field field : target.getFields()) {
            JsonTypeInfo jsonTypeInfo = field.getType().getAnnotation(JsonTypeInfo.class);
            if (jsonTypeInfo != null) {
                String fieldName = field.getName();
                if (jsonNode.path(fieldName).isTextual()) {
                    String propertyName = jsonTypeInfo.property().isEmpty() ?
                            jsonTypeInfo.use().getDefaultPropertyName() :
                            jsonTypeInfo.property();
                    ((ObjectNode) jsonNode).set(
                            fieldName,
                            o(propertyName, jsonNode.get(fieldName).textValue())
                    );
                }
            }
        }
    }

    public static ObjectNode o(Object... entries) {
        if (entries.length % 2 != 0) throw new IllegalArgumentException("varargs uneven");
        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
        for (int i = 0; i < entries.length; i += 2) {
            String key = (String) entries[i];
            JsonNode value = v(entries[i + 1]);
            objectNode.set(key, value);
        }
        return objectNode;
    }

    private static JsonNode v(Object o) {
        if (o == null) {
            return JsonNodeFactory.instance.nullNode();
        } else if (o instanceof JsonNode) {
            return (JsonNode) o;
        } else if (o instanceof Boolean) {
            return JsonNodeFactory.instance.booleanNode((Boolean) o);
        } else if (o instanceof Integer) {
            return JsonNodeFactory.instance.numberNode((Integer) o);
        } else if (o instanceof Long) {
            return JsonNodeFactory.instance.numberNode((Long) o);
        } else if (o instanceof String) {
            return JsonNodeFactory.instance.textNode((String) o);
        } else {
            throw new IllegalArgumentException();  // TODO
        }
    }
}
