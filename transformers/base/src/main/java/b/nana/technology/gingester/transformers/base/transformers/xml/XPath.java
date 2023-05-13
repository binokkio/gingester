package b.nana.technology.gingester.transformers.base.transformers.xml;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.configuration.FlagOrderDeserializer;
import b.nana.technology.gingester.core.configuration.Order;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.TemplateMapper;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

@Names(1)
public final class XPath implements Transformer<Node, Node> {

    private final ThreadLocal<XPathExpressionFactory> xPathExpressionFactories =
            ThreadLocal.withInitial(XPathExpressionFactory::new);

    private final TemplateMapper<XPathExpression> xPathTemplate;

    public XPath(Parameters parameters) {
        parameters.xPath.invariant = false;  // force invariant to be false, to ensure we get an XPathExpression per thread
        xPathTemplate = Context.newTemplateMapper(parameters.xPath, s -> xPathExpressionFactories.get().compile(s));
    }

    @Override
    public void transform(Context context, Node in, Receiver<Node> out) throws XPathExpressionException {
        XPathExpression xPath = xPathTemplate.render(context, in);
        NodeList nodeList = (NodeList) xPath.evaluate(in, XPathConstants.NODESET);
        for (int i = 0; i < nodeList.getLength(); i++) {
            out.accept(context, nodeList.item(i));
        }
    }

    private static class XPathExpressionFactory {

        final javax.xml.xpath.XPath xPath;

        XPathExpressionFactory() {
            xPath = XPathFactory.newDefaultInstance().newXPath();
        }

        XPathExpression compile(String expression) throws XPathExpressionException {
            return xPath.compile(expression);
        }
    }

    @JsonDeserialize(using = FlagOrderDeserializer.class)
    @Order({ "xPath" })
    public static class Parameters {
        public TemplateParameters xPath;
    }
}
