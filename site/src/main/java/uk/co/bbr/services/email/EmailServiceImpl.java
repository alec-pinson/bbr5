package uk.co.bbr.services.email;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import uk.co.bbr.services.security.dao.SiteUserDao;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final String EMAIL_FROM = "notification@brassbandresults.co.uk";
    private static final String FEEDBACK_NOTIFY_ADDRESS = "notification@brassbandresults.co.uk";

    private final MessageSource messageSource;
    private final JavaMailSender javaMailSender;

    private String getSiteUrl() {
        return "https://" + System.getenv("BBR_WEB_SITE_PREFIX") + ".brassbandresults.co.uk";
    }

    @Override
    public void sendFeedbackEmail(SiteUserDao user, String feedbackComment, String feedbackOffset) {
        String destinationEmail = user.getEmail();
        if (user.isFeedbackEmailOptOut()) {
            destinationEmail = FEEDBACK_NOTIFY_ADDRESS;
        }

        StringBuilder messageText = new StringBuilder();
        messageText.append(this.messageSource.getMessage("email.hello", null, LocaleContextHolder.getLocale()));
        messageText.append(" ");
        messageText.append(user.getUsercode());
        messageText.append("\n\n");
        messageText.append(this.messageSource.getMessage("email.feedback.prompt", null, LocaleContextHolder.getLocale()));
        messageText.append("\n\n");
        messageText.append(feedbackComment);
        messageText.append("\n\n");
        messageText.append(this.getSiteUrl()).append(feedbackOffset);
        messageText.append("\n\n");
        messageText.append(this.messageSource.getMessage("email.feedback.opt-out", null, LocaleContextHolder.getLocale()));
        messageText.append(this.getSiteUrl() + "/acc/feedback/opt-out/").append(user.getUuid());
        messageText.append("\n\n");
        messageText.append(this.messageSource.getMessage("email.sign-off", null, LocaleContextHolder.getLocale()));

        String subject = this.messageSource.getMessage("email.feedback.subject", null, LocaleContextHolder.getLocale()) + " " + feedbackOffset;
        this.sendEmail(EmailServiceImpl.EMAIL_FROM, destinationEmail, subject, messageText.toString());
    }

    @Override
    public void sendResetPasswordEmail(SiteUserDao user) {
        StringBuilder messageText = new StringBuilder();
        messageText.append(this.messageSource.getMessage("email.hello", null, LocaleContextHolder.getLocale()));
        messageText.append(" ");
        messageText.append(user.getUsercode());
        messageText.append("\n\n");
        messageText.append(this.messageSource.getMessage("email.reset-password.prompt1", null, LocaleContextHolder.getLocale()));
        messageText.append("\n\n");
        messageText.append(this.getSiteUrl() + "/acc/forgotten-password/reset/").append(user.getResetPasswordKey());
        messageText.append("\n\n");
        messageText.append(this.messageSource.getMessage("email.reset-password.username-prompt", null, LocaleContextHolder.getLocale())).append(" ").append(user.getUsercode());
        messageText.append("\n\n");
        messageText.append(this.messageSource.getMessage("email.sign-off", null, LocaleContextHolder.getLocale()));

        String subject = this.messageSource.getMessage("email.reset-password.subject", null, LocaleContextHolder.getLocale());
        this.sendEmail(EmailServiceImpl.EMAIL_FROM, user.getEmail(), subject, messageText.toString());
    }

    @Override
    public void sendActivationEmail(String destinationEmail, String activationKey) {
        StringBuilder messageText = new StringBuilder();
        messageText.append(this.messageSource.getMessage("email.account-activation.line1", null, LocaleContextHolder.getLocale())).append("\n\n");
        messageText.append(this.messageSource.getMessage("email.account-activation.line2", null, LocaleContextHolder.getLocale())).append("\n\n");
        messageText.append(this.getSiteUrl() + "/acc/activate/").append(activationKey).append("\n\n");
        messageText.append(this.messageSource.getMessage("email.sign-off", null, LocaleContextHolder.getLocale()));

        String subject = this.messageSource.getMessage("email.account-activation.subject", null, LocaleContextHolder.getLocale());
        this.sendEmail(EmailServiceImpl.EMAIL_FROM, destinationEmail, subject, messageText.toString());
    }

    private void sendEmail(String fromEmail, String toEmail, String subject, String contents) {
        List<String> toEmails = new ArrayList<>();
        toEmails.add(toEmail);

        this.sendEmail(fromEmail, toEmails, subject, contents);
    }

    private void sendEmail(String fromEmail, List<String> to, String subject, String contents) {
        MimeMessage message = javaMailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            helper.setTo(to.parallelStream().toArray(String[]::new));
            helper.setBcc("bcc@brassbandresults.co.uk");
            helper.setText(contents);
            helper.setSubject(subject);
            helper.setFrom(new InternetAddress(fromEmail, "BrassBandResults"));
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        javaMailSender.send(message);
    }
}
