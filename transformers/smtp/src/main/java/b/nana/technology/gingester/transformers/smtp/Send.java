package b.nana.technology.gingester.transformers.smtp;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import jakarta.mail.*;
import jakarta.mail.internet.*;

import java.io.ByteArrayInputStream;
import java.util.*;

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
            message.setContent(body.getValue().render(context, in), body.getKey() + "; charset=UTF-8");
        } else if (attachments.isEmpty()) {
            message.setContent(createAlternativeMultipart(context, in));
        } else {
            MimeBodyPart inlinePart = new MimeBodyPart();
            if (inline.size() == 1) {
                Map.Entry<String, Template> body = inline.entrySet().stream().findFirst().orElseThrow();
                inlinePart.setContent(body.getValue().render(context, in), body.getKey() + "; charset=UTF-8");
            } else {
                inlinePart.setContent(createAlternativeMultipart(context, in));
            }
            Multipart mixedMultipart = new MimeMultipart("mixed");
            mixedMultipart.addBodyPart(inlinePart);
            addAttachments(context, mixedMultipart);
            message.setContent(mixedMultipart);
        }

        // TODO include replyTo if present?

        Transport.send(message);

        // TODO output raw message, align with SmtpServer output
    }

    private Multipart createAlternativeMultipart(Context context, Object in) throws MessagingException {
        Multipart alternativeMultipart = new MimeMultipart("alternative");
        for (Map.Entry<String, Template> entry : inline.entrySet()) {
            MimeBodyPart mimeBodyPart = new MimeBodyPart();
            mimeBodyPart.setContent(entry.getValue().render(context, in), entry.getKey() + "; charset=UTF-8");
            alternativeMultipart.addBodyPart(mimeBodyPart);
        }
        return alternativeMultipart;
    }

    private void addAttachments(Context context, Multipart multipart) throws MessagingException {
        for (Parameters.Attachment attachment : attachments) {
            Optional<Object> optionalBytes = context.fetch(attachment.bytes);
            if (optionalBytes.isPresent()) {
                InternetHeaders headers = new InternetHeaders();
                headers.addHeader("Content-Type", attachment.type + "; name=\"" + attachment.name + "\"");
                headers.addHeader("Content-Disposition", "attachment; filename=\"" + attachment.name + "\"");
                headers.addHeader("Content-Transfer-Encoding", "base64");
                byte[] bytes = Base64.getMimeEncoder().encode((byte[]) optionalBytes.get());
                multipart.addBodyPart(new MimeBodyPart(headers, bytes));
            } else if (!attachment.optional) {
                throw new IllegalStateException("Missing non-optional attachment " + attachment.name);
            }
        }
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

            // TODO support String syntax, e.g. '^3 text/plain > name.txt?'

            public String name;
            public String type;
            public FetchKey bytes;
            public boolean optional;
        }
    }
}
