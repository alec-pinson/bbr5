package uk.co.bbr.web.results;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import uk.co.bbr.services.bands.BandService;
import uk.co.bbr.services.contests.ContestService;
import uk.co.bbr.services.contests.ContestTypeService;
import uk.co.bbr.services.contests.dao.ContestDao;
import uk.co.bbr.services.contests.dao.ContestTypeDao;
import uk.co.bbr.services.events.ContestEventService;
import uk.co.bbr.services.events.dao.ContestEventDao;
import uk.co.bbr.services.events.types.ContestEventDateResolution;
import uk.co.bbr.services.people.PersonService;
import uk.co.bbr.services.regions.RegionService;
import uk.co.bbr.services.security.JwtService;
import uk.co.bbr.services.security.SecurityService;
import uk.co.bbr.services.security.ex.AuthenticationFailedException;
import uk.co.bbr.web.LoginMixin;
import uk.co.bbr.web.security.filter.SecurityFilter;
import uk.co.bbr.web.security.support.TestUser;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(properties = {  "spring.config.location=classpath:test-application.yml",
        "spring.datasource.url=jdbc:h2:mem:results-add-results-contest-type-tests-admin-h2;DB_CLOSE_DELAY=-1;MODE=MSSQLServer;DATABASE_TO_LOWER=TRUE"},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AddResults3ContestTypeWebTests implements LoginMixin {

    @Autowired private SecurityService securityService;
    @Autowired private JwtService jwtService;
    @Autowired private RegionService regionService;
    @Autowired private BandService bandService;
    @Autowired private ContestService contestService;
    @Autowired private PersonService personService;
    @Autowired private ContestEventService contestEventService;
    @Autowired private ContestTypeService contestTypeService;
    @Autowired private RestTemplate restTemplate;
    @Autowired private CsrfTokenRepository csrfTokenRepository;
    @LocalServerPort private int port;

    @BeforeAll
    void setupData() throws AuthenticationFailedException {
        this.securityService.createUser(TestUser.TEST_MEMBER.getUsername(), TestUser.TEST_MEMBER.getPassword(), TestUser.TEST_MEMBER.getEmail());
        loginTestUserByWeb(TestUser.TEST_MEMBER, this.restTemplate, this.csrfTokenRepository, this.port);

        Optional<ContestTypeDao> ownChoiceType = this.contestTypeService.fetchBySlug("own-choice-test-piece-contest");
        assertTrue(ownChoiceType.isPresent());

        loginTestUser(this.securityService, this.jwtService, TestUser.TEST_MEMBER);

        ContestDao yorkshireArea = this.contestService.create("Yorkshire Area");
        yorkshireArea.setDefaultContestType(ownChoiceType.get());
        yorkshireArea = this.contestService.update(yorkshireArea);
        this.contestEventService.create(yorkshireArea, LocalDate.of(2000, 3, 15));

        logoutTestUser();
    }

    @Test
    void testEventContestTypeStageGetWorksSuccessfully() {
        String response = this.restTemplate.getForObject("http://localhost:" + this.port + "/add-results/3/yorkshire-area/2000-03-15", String.class);
        assertNotNull(response);
        assertTrue(response.contains("<title>Add Contest Results - Brass Band Results</title>"));

        assertTrue(response.contains("<h2>Add Contest Results</h2>"));
        assertTrue(response.contains(">Contest:<"));
        assertTrue(response.contains(">Event Date:<"));
        assertTrue(response.contains(">Contest Type:<"));
        assertTrue(response.contains("<option value=\"1\">Entertainments Contest</option>"));
        assertTrue(response.contains("<option value=\"8\" selected=\"selected\">Own Choice Test Piece Contest</option>"));
    }

    @Test
    void testUpdateContestTypeWorksSuccessfully() {
        Optional<ContestTypeDao> contestType = contestTypeService.fetchBySlug("set-test-and-entertainments-contest");
        assertTrue(contestType.isPresent());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        CsrfToken csrfToken = csrfTokenRepository.generateToken(null);
        headers.add(csrfToken.getHeaderName(), csrfToken.getToken());
        headers.add("Cookie", SecurityFilter.CSRF_HEADER_NAME + "=" + csrfToken.getToken());

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("contestType", String.valueOf(contestType.get().getId()));
        map.add("_csrf", csrfToken.getToken());
        map.add("_csrf_header", SecurityFilter.CSRF_HEADER_NAME);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        // act
        ResponseEntity<String> response = this.restTemplate.postForEntity("http://localhost:" + port + "/add-results/3/yorkshire-area/2000-03-15", request, String.class);

        // assert
        assertEquals(HttpStatus.FOUND, response.getStatusCode());

        assertTrue(Objects.requireNonNull(response.getHeaders().get("Location")).get(0).endsWith("/add-results/4/yorkshire-area/2000-03-15"));

        Optional<ContestEventDao> fetchedContestEvent =  this.contestEventService.fetchEvent("yorkshire-area", LocalDate.of(2000,3,15));
        assertTrue(fetchedContestEvent.isPresent());
        assertEquals("Yorkshire Area", fetchedContestEvent.get().getName());
        assertEquals("Yorkshire Area", fetchedContestEvent.get().getContest().getName());
        assertEquals(LocalDate.of(2000,3,15), fetchedContestEvent.get().getEventDate());
        assertEquals(contestType.get().getSlug(), fetchedContestEvent.get().getContestType().getSlug());
        assertEquals(ContestEventDateResolution.EXACT_DATE, fetchedContestEvent.get().getEventDateResolution());
    }
}
