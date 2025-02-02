package uk.co.bbr.web.security;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.co.bbr.services.security.SecurityService;
import uk.co.bbr.services.security.UserService;
import uk.co.bbr.services.security.dao.SiteUserDao;
import uk.co.bbr.web.LoginMixin;
import uk.co.bbr.web.security.filter.SecurityFilter;

import javax.mail.internet.MimeMessage;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@SpringBootTest(properties = {  "spring.config.location=classpath:test-application.yml",
                                "spring.datasource.url=jdbc:h2:mem:security-reset-password-web-tests-admin-h2;DB_CLOSE_DELAY=-1;MODE=MSSQLServer;DATABASE_TO_LOWER=TRUE"},
                 webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ResetPasswordTests implements LoginMixin {

    @Autowired private CsrfTokenRepository csrfTokenRepository;
    @Autowired private UserService userService;
    @Autowired private SecurityService securityService;
    @Autowired private RestTemplate restTemplate;
    @LocalServerPort private int port;

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withUser("user", "admin"))
            .withPerMethodLifecycle(false);

    @Test
    void testGetResetPasswordPageWorksSuccessfully() {
        String response = this.restTemplate.getForObject("http://localhost:" + this.port + "/acc/forgotten-password", String.class);
        assertNotNull(response);
        assertTrue(response.contains("<title>Forgotten Password - Brass Band Results</title>"));
        assertTrue(response.contains("<h2>Forgotten Password</h2>"));
        assertTrue(response.contains("Please enter your username or the email address of your account below."));
    }

    @Test
    void testGetResetPasswordSentPageWorksSuccessfully() {
        String response = this.restTemplate.getForObject("http://localhost:" + this.port + "/acc/forgotten-password/sent", String.class);
        assertNotNull(response);
        assertTrue(response.contains("<title>Forgotten Password - Brass Band Results</title>"));
        assertTrue(response.contains("<h2>Forgotten Password</h2>"));
        assertTrue(response.contains("The email associated with your account has been sent a link. Click on this link and follow the instructions to reset your password."));
    }

    @Test
    void testSubmitResetPasswordPageWithUsernameWorksSuccessfully() throws FolderException {
        // arrange
        greenMail.purgeEmailFromAllMailboxes();

        this.securityService.createUser("test-user1", "password1", "test-reset1@brassbandresults.co.uk");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        CsrfToken csrfToken = csrfTokenRepository.generateToken(null);
        headers.add(csrfToken.getHeaderName(), csrfToken.getToken());
        headers.add("Cookie", SecurityFilter.CSRF_HEADER_NAME + "=" + csrfToken.getToken());

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("usercode", "  test-user1  ");
        map.add("_csrf", csrfToken.getToken());
        map.add("_csrf_header", SecurityFilter.CSRF_HEADER_NAME);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        // act
        ResponseEntity<String> response = this.restTemplate.postForEntity("http://localhost:" + port + "/acc/forgotten-password", request, String.class);

        // assert
        assertEquals(HttpStatus.FOUND, response.getStatusCode());

        assertTrue(Objects.requireNonNull(response.getHeaders().get("Location")).get(0).endsWith("/acc/forgotten-password/sent"));

        Awaitility.await().atMost(2, TimeUnit.SECONDS).untilAsserted(()-> {
            MimeMessage receivedMessage = greenMail.getReceivedMessages()[0];

            assertEquals(1, receivedMessage.getAllRecipients().length);
            assertEquals("test-reset1@brassbandresults.co.uk", receivedMessage.getAllRecipients()[0].toString());
            assertEquals("BrassBandResults <notification@brassbandresults.co.uk>", receivedMessage.getFrom()[0].toString());
            assertEquals("Password Reset Request", receivedMessage.getSubject());

            String emailContents = GreenMailUtil.getBody(receivedMessage);

            assertTrue(emailContents.contains("Someone has put your username or email into the forgotten password box"));
            assertTrue(emailContents.contains("/acc/forgotten-password/reset/"));
            assertTrue(emailContents.contains("test-user1"));
            assertTrue(emailContents.contains("The Brass Band Results Team"));
        });
    }

    @Test
    void testSubmitResetPasswordPageWithEmailWorksSuccessfully() throws FolderException {
        // arrange
        greenMail.purgeEmailFromAllMailboxes();

        this.securityService.createUser("test-user2", "password1", "test-reset2@brassbandresults.co.uk");
        this.securityService.createUser("test-user3", "password1", "test-reset2@brassbandresults.co.uk");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        CsrfToken csrfToken = csrfTokenRepository.generateToken(null);
        headers.add(csrfToken.getHeaderName(), csrfToken.getToken());
        headers.add("Cookie", SecurityFilter.CSRF_HEADER_NAME + "=" + csrfToken.getToken());

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("usercode", "  test-reset2@brassbandresults.co.uk  ");
        map.add("_csrf", csrfToken.getToken());
        map.add("_csrf_header", SecurityFilter.CSRF_HEADER_NAME);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        // act
        ResponseEntity<String> response = this.restTemplate.postForEntity("http://localhost:" + port + "/acc/forgotten-password", request, String.class);

        // assert
        assertEquals(HttpStatus.FOUND, response.getStatusCode());

        assertTrue(Objects.requireNonNull(response.getHeaders().get("Location")).get(0).endsWith("/acc/forgotten-password/sent"));

        Awaitility.await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            MimeMessage receivedMessage = greenMail.getReceivedMessages()[0];

            assertEquals(1, receivedMessage.getAllRecipients().length);
            assertEquals("test-reset2@brassbandresults.co.uk", receivedMessage.getAllRecipients()[0].toString());
            assertEquals("BrassBandResults <notification@brassbandresults.co.uk>", receivedMessage.getFrom()[0].toString());
            assertEquals("Password Reset Request", receivedMessage.getSubject());

            String emailContents = GreenMailUtil.getBody(receivedMessage);

            assertTrue(emailContents.contains("Someone has put your username or email into the forgotten password box"));
            assertTrue(emailContents.contains("/acc/forgotten-password/reset/"));
            assertTrue(emailContents.contains("The Brass Band Results Team"));
        });
    }

    @Test
    void testSubmitResetPasswordPageWithInvalidUsernameWorksSuccessfullyWithoutEmail() {
        // arrange
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        CsrfToken csrfToken = csrfTokenRepository.generateToken(null);
        headers.add(csrfToken.getHeaderName(), csrfToken.getToken());
        headers.add("Cookie", SecurityFilter.CSRF_HEADER_NAME + "=" + csrfToken.getToken());

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("usercode", "  not-a-user  ");
        map.add("_csrf", csrfToken.getToken());
        map.add("_csrf_header", SecurityFilter.CSRF_HEADER_NAME);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        // act
        ResponseEntity<String> response = this.restTemplate.postForEntity("http://localhost:" + port + "/acc/forgotten-password", request, String.class);

        // assert
        assertEquals(HttpStatus.FOUND, response.getStatusCode());

        assertTrue(Objects.requireNonNull(response.getHeaders().get("Location")).get(0).endsWith("/acc/forgotten-password/sent"));

        Awaitility.await().atMost(2, TimeUnit.SECONDS).untilAsserted(()-> {
            assertEquals(0, greenMail.getReceivedMessages().length);
        });
    }

    @Test
    void testEnterPasswordPageWorksSuccessfully() {
        Optional<SiteUserDao> user = this.userService.fetchUserByUsercode("test-user2");
        assertTrue(user.isPresent());

        this.userService.sendResetPasswordEmail(user.get());
        String resetKey = user.get().getResetPasswordKey();

        String response = this.restTemplate.getForObject("http://localhost:" + this.port + "/acc/forgotten-password/reset/" + resetKey, String.class);
        assertNotNull(response);
        assertTrue(response.contains("<title>Forgotten Password - Brass Band Results</title>"));
        assertTrue(response.contains("<h2>Forgotten Password</h2>"));
        assertTrue(response.contains("password1"));
        assertTrue(response.contains("password2"));
    }

    @Test
    void testEnterPasswordPageFailsWithInvalidResetKey() {
        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> this.restTemplate.getForObject("http://localhost:" + this.port + "/acc/forgotten-password/reset/not-a-real-reset-key--------------------", String.class));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void testSubmitNewPasswordPageWorksSuccessfully() throws FolderException {
        // arrange
        greenMail.purgeEmailFromAllMailboxes();

        this.securityService.createUser("test-user4", "password1", "test-reset4@brassbandresults.co.uk");
        Optional<SiteUserDao> user = this.userService.fetchUserByUsercode("test-user4");
        assertTrue(user.isPresent());

        this.userService.sendResetPasswordEmail(user.get());
        String resetKey = user.get().getResetPasswordKey();

        String oldPassword = user.get().getPassword();
        String oldSalt = user.get().getSalt();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        CsrfToken csrfToken = csrfTokenRepository.generateToken(null);
        headers.add(csrfToken.getHeaderName(), csrfToken.getToken());
        headers.add("Cookie", SecurityFilter.CSRF_HEADER_NAME + "=" + csrfToken.getToken());

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("password1", "new-password");
        map.add("password2", "new-password");
        map.add("_csrf", csrfToken.getToken());
        map.add("_csrf_header", SecurityFilter.CSRF_HEADER_NAME);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        // act
        ResponseEntity<String> response = this.restTemplate.postForEntity("http://localhost:" + port + "/acc/forgotten-password/reset/" + resetKey, request, String.class);

        // assert
        assertEquals(HttpStatus.FOUND, response.getStatusCode());

        assertTrue(Objects.requireNonNull(response.getHeaders().get("Location")).get(0).endsWith("/acc/forgotten-password/changed"));

        Optional<SiteUserDao> userAfter = this.userService.fetchUserByUsercode("test-user4");
        assertTrue(userAfter.isPresent());

        assertNotEquals(oldPassword, userAfter.get().getPassword());
        assertNotEquals(oldSalt, userAfter.get().getSalt());
    }

    @Test
    void testSubmitNewPasswordPageFailsWithInvalidResetKey() {
        // arrange
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        CsrfToken csrfToken = csrfTokenRepository.generateToken(null);
        headers.add(csrfToken.getHeaderName(), csrfToken.getToken());
        headers.add("Cookie", SecurityFilter.CSRF_HEADER_NAME + "=" + csrfToken.getToken());

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("password1", "new-password");
        map.add("password2", "new-password");
        map.add("_csrf", csrfToken.getToken());
        map.add("_csrf_header", SecurityFilter.CSRF_HEADER_NAME);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        // act
        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> this.restTemplate.postForEntity("http://localhost:" + port + "/acc/forgotten-password/reset/not-a-valid-reset-key-------------------", request, String.class));

        // assert
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void testSubmitNewPasswordPageFailsWithMismatchedPasswords() throws FolderException {
        // arrange
        greenMail.purgeEmailFromAllMailboxes();

        this.securityService.createUser("test-user5", "password1", "test-reset5@brassbandresults.co.uk");
        Optional<SiteUserDao> user = this.userService.fetchUserByUsercode("test-user5");
        assertTrue(user.isPresent());
        this.userService.sendResetPasswordEmail(user.get());
        String resetKey = user.get().getResetPasswordKey();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        CsrfToken csrfToken = csrfTokenRepository.generateToken(null);
        headers.add(csrfToken.getHeaderName(), csrfToken.getToken());
        headers.add("Cookie", SecurityFilter.CSRF_HEADER_NAME + "=" + csrfToken.getToken());

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("password1", "new-password1");
        map.add("password2", "new-password2");
        map.add("_csrf", csrfToken.getToken());
        map.add("_csrf_header", SecurityFilter.CSRF_HEADER_NAME);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        // act
        ResponseEntity<String> response = this.restTemplate.postForEntity("http://localhost:" + port + "/acc/forgotten-password/reset/" + resetKey, request, String.class);

        // assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(Objects.requireNonNull(response.getBody()).contains("The two passwords you&#39;ve entered don&#39;t match, please try again."));

    }

    @Test
    void testSubmitNewPasswordPageFailsWithTooShortPassword() throws FolderException {
        // arrange
        greenMail.purgeEmailFromAllMailboxes();

        this.securityService.createUser("test-user6", "password1", "test-reset6@brassbandresults.co.uk");
        Optional<SiteUserDao> user = this.userService.fetchUserByUsercode("test-user6");
        assertTrue(user.isPresent());
        this.userService.sendResetPasswordEmail(user.get());
        String resetKey = user.get().getResetPasswordKey();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        CsrfToken csrfToken = csrfTokenRepository.generateToken(null);
        headers.add(csrfToken.getHeaderName(), csrfToken.getToken());
        headers.add("Cookie", SecurityFilter.CSRF_HEADER_NAME + "=" + csrfToken.getToken());

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("password1", "new1");
        map.add("password2", "new1");
        map.add("_csrf", csrfToken.getToken());
        map.add("_csrf_header", SecurityFilter.CSRF_HEADER_NAME);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        // act
        ResponseEntity<String> response =  this.restTemplate.postForEntity("http://localhost:" + port + "/acc/forgotten-password/reset/" + resetKey, request, String.class);

        // assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        String responseBody = response.getBody();
        assertTrue(Objects.requireNonNull(response.getBody()).contains("Your password is too short, please use one that&#39;s more than eight characters or more."));
    }

}
