package b.nana.technology.gingester.transformers.smtp;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

public final class SendText implements Transformer<String, String> {

    private final Template toTemplate;
    private final Template fromTemplate;
    private final Template subjectTemplate;
    private final Template mimeTypeTemplate;
    private final Parameters.Smtp smtp;

    public SendText(Parameters parameters) {
        toTemplate = Context.newTemplate(parameters.to);
        fromTemplate = Context.newTemplate(parameters.from);
        subjectTemplate = Context.newTemplate(parameters.subject);
        mimeTypeTemplate = Context.newTemplate(parameters.mimeType);
        smtp = parameters.smtp;
    }

    @Override
    public void transform(Context context, String in, Receiver<String> out) throws MessagingException {

        Properties properties = System.getProperties();
        properties.put("mail.smtp.host", smtp.host);
        properties.put("mail.smtp.port", smtp.port);
        properties.put("mail.smtp.starttls.enable", smtp.startTls);

        Session session;
        if (smtp.username != null && smtp.password != null) {
            properties.put("mail.smtp.auth", true);
            session = Session.getDefaultInstance(properties, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(smtp.username, smtp.password);
                }
            });
        } else {
            session = Session.getDefaultInstance(properties);
        }

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(fromTemplate.render(context)));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(toTemplate.render(context)));
        message.setSubject(subjectTemplate.render(context));
        message.setContent(in, mimeTypeTemplate.render(context));

        Transport.send(message);
    }

    public static class Parameters {

        public TemplateParameters from = new TemplateParameters("binokkio@b.nana.technology", true);
        public TemplateParameters to;
        public TemplateParameters subject;
        public TemplateParameters mimeType = new TemplateParameters("text/plain", true);
        public Smtp smtp = new Smtp();

        public static class Smtp {
            public String host = "localhost";
            public int port = 25;
            public boolean startTls;
            public String username;
            public String password;
        }
    }
}
