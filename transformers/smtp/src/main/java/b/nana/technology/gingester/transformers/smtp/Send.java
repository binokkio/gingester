package b.nana.technology.gingester.transformers.smtp;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.ByteArrayInputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public final class Send implements Transformer<Object, Object> {

    private final Parameters.Smtp smtp;
    private final Template fromTemplate;
    private final Template toTemplate;
    private final Template subjectTemplate;
    private final FetchKey fetchReplyTo;
    private final Map<String, Template> inline;
    private final List<Parameters.Attachment> attachments;

    public Send(Parameters parameters) {

        smtp = parameters.smtp;
        fromTemplate = Context.newTemplate(parameters.from);
        toTemplate = parameters.to != null ? Context.newTemplate(parameters.to) : null;
        subjectTemplate = parameters.subject != null ? Context.newTemplate(parameters.subject) : null;
        fetchReplyTo = parameters.replyTo;

        inline = new LinkedHashMap<>();
        parameters.inline.forEach((type, templateParameters) ->
                inline.put(type, Context.newTemplate(templateParameters)));

        attachments = parameters.attachments;
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws MessagingException {

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

        Message replyTo = fetchReplyTo != null ?
                new MimeMessage(session, new ByteArrayInputStream((byte[]) context.require(fetchReplyTo))) :
                null;

        Message message = replyTo != null ?
                replyTo.reply(false) :  // TODO parameterize
                new MimeMessage(session);

        message.setFrom(new InternetAddress(fromTemplate.render(context)));

        if (toTemplate != null)
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(toTemplate.render(context)));

        if (subjectTemplate != null)
            message.setSubject(subjectTemplate.render(context));

        if (inline.size() == 1 && attachments.isEmpty()) {
            Map.Entry<String, Template> body = inline.entrySet().stream().findFirst().orElseThrow();
            message.setContent(body.getValue().render(context, in), body.getKey());
        } else {
            // TODO Multipart multipart = new MimeMultipart();
            // TODO inline
            // TODO attachments
            // TODO include replyTo if present?
        }

        Transport.send(message);

        // TODO output raw message, align with SmtpServer output
    }

    public static class Parameters {

        public Smtp smtp = new Smtp();
        public TemplateParameters from;
        public TemplateParameters to;
        public TemplateParameters subject;
        public FetchKey replyTo;
        public Map<String, TemplateParameters> inline = Map.of("text/plain", new TemplateParameters("${__in__}", false));
        public List<Attachment> attachments = List.of();

        public static class Smtp {
            public String host = "localhost";
            public int port = 25;
            public boolean startTls;
            public String username;
            public String password;
        }

        public static class Attachment {
            public String name;
            public String type;
            public FetchKey bytes;
        }
    }
}
