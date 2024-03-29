package b.nana.technology.gingester.core.configuration;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializerFactory;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Iffy deserializer that allows for some rule based normalization before
 * calling through to the "normal" deserializer. Meant to be used for
 * Gingester transformer parameters deserialization which does not need to be
 * very performant.
 */
public abstract class NormalizingDeserializer<T> extends StdDeserializer<T> {

    private final List<Rule> rules = new ArrayList<>();
    private final Class<T> target;
    private final JavaType javaType;

    public NormalizingDeserializer(Class<T> target) {
        super(target);
        this.target = target;
        this.javaType = TypeFactory.defaultInstance().constructType(target);
    }

    protected void rule(Predicate<JsonNode> predicate, Function<JsonNode, JsonNode> normalizer) {
        rules.add(new Rule(predicate, normalizer));
    }

    @Override
    public final T deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException {

        ObjectCodec objectCodec = jsonParser.getCodec();
        JsonNode json = objectCodec.readTree(jsonParser);

        for (Rule rule : rules) {
            if (rule.predicate.test(json)) {
                json = rule.normalizer.apply(json);
            }
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

    public ObjectNode flags(JsonNode source, int from, ObjectNode destination) {

        Set<String> booleanFields = Arrays.stream(this.target.getFields())
                .filter(f -> f.getType().equals(Boolean.TYPE))
                .map(Field::getName)
                .collect(Collectors.toSet());

        for (int i = from; i < source.size(); i++) {

            boolean value = true;
            String field = source.get(i).asText();
            if (field.startsWith("!")) {
                value = false;
                field = field.substring(1);
            }

            if (!booleanFields.contains(field))
                throw new IllegalArgumentException("Unexpected flag: " + source.get(i));

            destination.put(field, value);
        }

        return destination;
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

    public static ArrayNode a(Object... entries) {
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        for (Object entry : entries) {
            arrayNode.add(v(entry));
        }
        return arrayNode;
    }

    public static ObjectNode am(ArrayNode array, String... keys) {
        ObjectNode objectNode = o();
        for (int i = 0; i < array.size(); i++) {
            objectNode.set(keys[i], array.get(i));
        }
        return objectNode;
    }

    public static ArrayNode as(ArrayNode arrayNode, int start) {
        return as(arrayNode, start, arrayNode.size());
    }

    public static ArrayNode as(ArrayNode arrayNode, int start, int end) {
        ArrayNode slice = JsonNodeFactory.instance.arrayNode(end - start);
        for (int i = start; i < end; i++) slice.add(arrayNode.get(i));
        return slice;
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

    private static class Rule {

        private final Predicate<JsonNode> predicate;
        private final Function<JsonNode, JsonNode> normalizer;

        private Rule(Predicate<JsonNode> predicate, Function<JsonNode, JsonNode> normalizer) {
            this.predicate = predicate;
            this.normalizer = normalizer;
        }
    }
}
