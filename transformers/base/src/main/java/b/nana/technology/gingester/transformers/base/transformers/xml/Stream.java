package b.nana.technology.gingester.transformers.base.transformers.xml;

import b.nana.technology.gingester.core.configuration.FlagOrderDeserializer;
import b.nana.technology.gingester.core.configuration.Order;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.TemplateMapper;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.codehaus.stax2.XMLInputFactory2;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Stream implements Transformer<InputStream, String> {

    private final XMLInputFactory xmlInputFactory = XMLInputFactory2.newDefaultFactory();
    private final TransformerFactory transformerFactory = TransformerFactory.newDefaultInstance();

    private final TemplateMapper<PathMatcher> pathMatcherTemplate;

    public Stream(Parameters parameters) {
        pathMatcherTemplate = Context.newTemplateMapper(parameters.path, PathMatcher::new);
    }

    @Override
    public void transform(Context context, InputStream in, Receiver<String> out) throws Exception {

        PathMatcher pathMatcher = pathMatcherTemplate.render(context);
        int pointer = 0;

        XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(in);
        while (xmlStreamReader.hasNext()) {
            switch (xmlStreamReader.next()) {

                case XMLStreamReader.START_ELEMENT -> {

                    if (pathMatcher.pathElements.get(pointer).matches(xmlStreamReader)) {

                        if (pointer == pathMatcher.pathElements.size() - 1) {
                            out.accept(context, readNode(xmlStreamReader));
                        } else {
                            pointer++;
                        }

                    } else {
                        discard(xmlStreamReader);
                    }
                }

                case XMLStreamConstants.END_ELEMENT -> pointer--;
            }
        }
    }

    private String readNode(XMLStreamReader xmlStreamReader) throws TransformerException {
        StringWriter stringWriter = new StringWriter();
        var t = transformerFactory.newTransformer();
        t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        t.transform(new StAXSource(xmlStreamReader), new StreamResult(stringWriter));
        return stringWriter.toString();
    }

    private void discard(XMLStreamReader xmlStreamReader) throws TransformerException {
        transformerFactory
                .newTransformer()
                .transform(new StAXSource(xmlStreamReader), new StreamResult(OutputStream.nullOutputStream()));
    }



    private static class PathMatcher {

        final List<PathElement> pathElements = new ArrayList<>();

        PathMatcher(String source) {

            String[] parts = source.split("/");

            if (!parts[0].isEmpty())
                throw new IllegalArgumentException("Unsupported XPath, must start with /");

            for (int i = 1; i < parts.length; i++) {

                if (parts[i].isEmpty())
                    throw new IllegalArgumentException("Unsupported XPath, contains //");

                pathElements.add(new PathElement(parts[i]));
            }
        }
    }

    private static class PathElement {

        final String elementName;
        final Map<String, String> attributePredicates = new HashMap<>();

        PathElement(String source) {

            if (source.startsWith("@") || source.startsWith("["))
                throw new IllegalArgumentException("Unsupported XPath, path elements must start with element name or wildcard, source: " + source);

            String[] sourceParts = source.split("\\[", 2);
            elementName = sourceParts[0];

            if (sourceParts.length == 2) {

                if (!sourceParts[1].endsWith("]"))
                    throw new IllegalArgumentException("Unsupported XPath, path element predicates closing bracket is missing, source: " + source);

                for (String predicate : sourceParts[1].substring(0, sourceParts[1].length() - 1).split(" and ")) {

                    predicate = predicate.trim();

                    if (!predicate.startsWith("@"))
                        throw new IllegalArgumentException("Unsupported XPath, only attributes are supported in predicates, source: " + source);

                    String[] predicateParts = predicate.split("=", 2);
                    String attributeName = predicateParts[0].substring(1).trim();

                    if (predicateParts.length == 1) {
                        attributePredicates.put(attributeName, "*");
                    } else {
                        String attributeValue = predicateParts[1].trim();
                        if (attributeValue.equals("*")) {
                            attributePredicates.put(attributeName, "*");
                        } else if (!attributeValue.startsWith("'") || !attributeValue.endsWith("'")) {
                            throw new IllegalArgumentException("Unsupported XPath, predicate values must be surrounded by single quotes, predicate: " + predicate);
                        } else {
                            attributePredicates.put(attributeName, attributeValue.substring(1, attributeValue.length() - 1));
                        }
                    }
                }
            }
        }

        boolean matches(XMLStreamReader xmlStreamReader) {

            if (!elementName.equals("*") && !elementName.equals(xmlStreamReader.getLocalName()))
                return false;

            if (!attributePredicates.isEmpty()) {

                int predicatesMet = 0;

                for (int i = 0; i < xmlStreamReader.getAttributeCount(); i++) {

                    String attributeName = xmlStreamReader.getAttributeLocalName(i);
                    String predicateValue = attributePredicates.get(attributeName);

                    if (predicateValue == null)
                        continue;

                    if (predicateValue.equals("*") || predicateValue.equals(xmlStreamReader.getAttributeValue(i)))
                        predicatesMet++;
                    else
                        return false;
                }

                return predicatesMet == attributePredicates.size();
            }

            return true;
        }
    }



    @JsonDeserialize(using = FlagOrderDeserializer.class)
    @Order({"path"})
    public static class Parameters {
        public TemplateParameters path;
    }
}
