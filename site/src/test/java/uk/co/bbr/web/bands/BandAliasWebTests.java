package uk.co.bbr.web.bands;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.co.bbr.services.bands.BandService;
import uk.co.bbr.services.bands.dao.BandDao;
import uk.co.bbr.services.bands.dao.BandPreviousNameDao;
import uk.co.bbr.services.contests.ContestEventService;
import uk.co.bbr.services.contests.ContestResultService;
import uk.co.bbr.services.contests.ContestService;
import uk.co.bbr.services.contests.dao.ContestDao;
import uk.co.bbr.services.contests.dao.ContestEventDao;
import uk.co.bbr.services.contests.types.ContestEventDateResolution;
import uk.co.bbr.services.people.PersonService;
import uk.co.bbr.services.people.dao.PersonDao;
import uk.co.bbr.services.regions.RegionService;
import uk.co.bbr.services.regions.dao.RegionDao;
import uk.co.bbr.services.security.JwtService;
import uk.co.bbr.services.security.SecurityService;
import uk.co.bbr.services.security.ex.AuthenticationFailedException;
import uk.co.bbr.web.LoginMixin;
import uk.co.bbr.web.security.support.TestUser;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@SpringBootTest(properties = { "spring.config.name=band-alias-web-tests-admin-h2", "spring.datasource.url=jdbc:h2:mem:band-alias-web-tests-admin-h2;DB_CLOSE_DELAY=-1;MODE=MSSQLServer;DATABASE_TO_LOWER=TRUE", "spring.jpa.database-platform=org.hibernate.dialect.SQLServerDialect"},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BandAliasWebTests implements LoginMixin {

    @Autowired private SecurityService securityService;
    @Autowired private JwtService jwtService;
    @Autowired private RegionService regionService;
    @Autowired private BandService bandService;
    @Autowired private ContestService contestService;
    @Autowired private ContestEventService contestEventService;
    @Autowired private ContestResultService contestResultService;
    @Autowired private PersonService personService;
    @Autowired private RestTemplate restTemplate;
    @LocalServerPort private int port;

    @BeforeAll
    void setupBands() throws AuthenticationFailedException {
        loginTestUser(this.securityService, this.jwtService, TestUser.TEST_MEMBER);

        RegionDao yorkshire = this.regionService.fetchBySlug("yorkshire").get();

        // band with two aliases
        BandDao rtb = this.bandService.create("Rothwell Temperance Band", yorkshire);
        BandPreviousNameDao bandPreviousName1 = new BandPreviousNameDao();
        bandPreviousName1.setOldName("Visible");
        bandPreviousName1.setHidden(false);
        this.bandService.createPreviousName(rtb, bandPreviousName1);

        BandPreviousNameDao bandPreviousName2 = new BandPreviousNameDao();
        bandPreviousName2.setOldName("Hidden");
        bandPreviousName2.setHidden(true);
        this.bandService.createPreviousName(rtb, bandPreviousName2);

        // band with no aliases
        this.bandService.create("Black Dyke", yorkshire);

        logoutTestUser();
    }

    @Test
    void testListAliasesWorksSuccessfully() {
        String response = this.restTemplate.getForObject("http://localhost:" + this.port + "/bands/rothwell-temperance-band/edit-aliases", String.class);
        assertNotNull(response);
        assertTrue(response.contains("<title>Rothwell Temperance Band - Band Aliases - Brass Band Results</title>"));
        assertTrue(response.contains(">Rothwell Temperance Band</a>"));
        assertTrue(response.contains("> Aliases<"));

        assertTrue(response.contains(">Visible<"));
        assertTrue(response.contains(">Hidden<"));
    }

    @Test
    void testListAliasesWithInvalidBandSlugFailsAsExpected() {
        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> this.restTemplate.getForObject("http://localhost:" + this.port + "/bands/not-a-real-band/edit-aliases", String.class));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void testHideAliasWorksSuccessfully() {
        Optional<BandDao> rtb = this.bandService.fetchBySlug("rothwell-temperance-band");

        long visibleAliasId = 0;
        List<BandPreviousNameDao> previousNamesBefore = this.bandService.findAllPreviousNames(rtb.get());
        for (BandPreviousNameDao previousName : previousNamesBefore) {
            if (previousName.getOldName().equals("Visible")) {
                assertFalse(previousName.isHidden());
                visibleAliasId = previousName.getId();
                break;
            }
        }

        String response = this.restTemplate.getForObject("http://localhost:" + this.port + "/bands/rothwell-temperance-band/edit-aliases/" + visibleAliasId + "/hide", String.class);
        assertNotNull(response);

        List<BandPreviousNameDao> previousNamesAfter = this.bandService.findAllPreviousNames(rtb.get());
        for (BandPreviousNameDao previousName : previousNamesAfter) {
            if (previousName.getOldName().equals("Visible")) {
                assertTrue(previousName.isHidden());
                break;
            }
        }

        this.bandService.showPreviousBandName(rtb.get(), visibleAliasId);
    }

    @Test
    void testHideAliasesWithInvalidBandSlugFailsAsExpected() {
        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> this.restTemplate.getForObject("http://localhost:" + this.port + "/bands/not-a-real-band/edit-aliases/1/hide", String.class));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void testHideAliasesWithInvalidAliasIdFailsAsExpected() {
        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> this.restTemplate.getForObject("http://localhost:" + this.port + "/bands/rothwell-temperance-band/edit-aliases/999/hide", String.class));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void testShowAliasWorksSuccessfully() {
        Optional<BandDao> rtb = this.bandService.fetchBySlug("rothwell-temperance-band");

        long hiddenAliasId = 0;
        List<BandPreviousNameDao> previousNamesBefore = this.bandService.findAllPreviousNames(rtb.get());
        for (BandPreviousNameDao previousName : previousNamesBefore) {
            if (previousName.getOldName().equals("Hidden")) {
                assertTrue(previousName.isHidden());
                hiddenAliasId = previousName.getId();
                break;
            }
        }

        String response = this.restTemplate.getForObject("http://localhost:" + this.port + "/bands/rothwell-temperance-band/edit-aliases/" + hiddenAliasId + "/show", String.class);
        assertNotNull(response);

        List<BandPreviousNameDao> previousNamesAfter = this.bandService.findAllPreviousNames(rtb.get());
        for (BandPreviousNameDao previousName : previousNamesAfter) {
            if (previousName.getOldName().equals("Hidden")) {
                assertFalse(previousName.isHidden());
                break;
            }
        }

        this.bandService.hidePreviousBandName(rtb.get(), hiddenAliasId);
    }

    @Test
    void testShowAliasesWithInvalidBandSlugFailsAsExpected() {
        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> this.restTemplate.getForObject("http://localhost:" + this.port + "/bands/not-a-real-band/edit-aliases/1/show", String.class));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void testShowAliasesWithInvalidAliasIdFailsAsExpected() {
        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> this.restTemplate.getForObject("http://localhost:" + this.port + "/bands/rothwell-temperance-band/edit-aliases/999/show", String.class));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void testDeleteAliasWorksSuccessfully() throws AuthenticationFailedException {
        loginTestUser(this.securityService, this.jwtService, TestUser.TEST_MEMBER);

        Optional<BandDao> noAliasBand = this.bandService.fetchBySlug("black-dyke");
        assertTrue(noAliasBand.isPresent());

        List<BandPreviousNameDao> fetchedAliases1 = this.bandService.findAllPreviousNames(noAliasBand.get());
        assertEquals(0, fetchedAliases1.size());

        BandPreviousNameDao previousName = new BandPreviousNameDao();
        previousName.setOldName("Old Name To Delete");
        BandPreviousNameDao newAlias = this.bandService.createPreviousName(noAliasBand.get(), previousName);

        List<BandPreviousNameDao> fetchedAliases2 = this.bandService.findAllPreviousNames(noAliasBand.get());
        assertEquals(1, fetchedAliases2.size());

        logoutTestUser();

        String response = this.restTemplate.getForObject("http://localhost:" + this.port + "/bands/black-dyke/edit-aliases/" + newAlias.getId() + "/delete", String.class);
        assertNotNull(response);

        List<BandPreviousNameDao> fetchedAliases3 = this.bandService.findAllPreviousNames(noAliasBand.get());
        assertEquals(0, fetchedAliases3.size());
    }

    @Test
    void testDeleteAliasWithInvalidBandSlugFailsAsExpected() {
        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> this.restTemplate.getForObject("http://localhost:" + this.port + "/bands/not-a-real-band/edit-aliases/1/delete", String.class));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void testDeleteAliasWithInvalidAliasIdFailsAsExpected() {
        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> this.restTemplate.getForObject("http://localhost:" + this.port + "/bands/rothwell-temperance-band/edit-aliases/999/delete", String.class));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}

