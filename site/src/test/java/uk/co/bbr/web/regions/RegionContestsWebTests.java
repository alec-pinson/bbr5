package uk.co.bbr.web.regions;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;
import uk.co.bbr.services.bands.BandService;
import uk.co.bbr.services.bands.dao.BandDao;
import uk.co.bbr.services.bands.types.BandStatus;
import uk.co.bbr.services.events.ContestEventService;
import uk.co.bbr.services.contests.ContestService;
import uk.co.bbr.services.contests.dao.ContestDao;
import uk.co.bbr.services.regions.RegionService;
import uk.co.bbr.services.regions.dao.RegionDao;
import uk.co.bbr.services.security.JwtService;
import uk.co.bbr.services.security.SecurityService;
import uk.co.bbr.services.security.ex.AuthenticationFailedException;
import uk.co.bbr.web.LoginMixin;
import uk.co.bbr.web.security.support.TestUser;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@SpringBootTest(properties = {  "spring.config.location=classpath:test-application.yml",
        "spring.datasource.url=jdbc:h2:mem:region-contests-web-tests-admin-h2;DB_CLOSE_DELAY=-1;MODE=MSSQLServer;DATABASE_TO_LOWER=TRUE"},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RegionContestsWebTests implements LoginMixin {

    @Autowired private SecurityService securityService;
    @Autowired private JwtService jwtService;
    @Autowired private RegionService regionService;
    @Autowired private BandService bandService;
    @Autowired private ContestService contestService;
    @Autowired private ContestEventService contestEventService;
    @Autowired private RestTemplate restTemplate;
    @LocalServerPort private int port;

    @BeforeAll
    void setupBands() throws AuthenticationFailedException {
        loginTestUser(this.securityService, this.jwtService, TestUser.TEST_MEMBER);

        RegionDao midlands = this.regionService.fetchBySlug("midlands").get();
        RegionDao yorkshire = this.regionService.fetchBySlug("yorkshire").get();
        RegionDao northWest = this.regionService.fetchBySlug("north-west").get();
        RegionDao wales = this.regionService.fetchBySlug("wales").get();
        RegionDao norway = this.regionService.fetchBySlug("norway").get();
        RegionDao denmark = this.regionService.fetchBySlug("denmark").get();

        this.bandService.create("Abercrombie Primary School Community", midlands);
        this.bandService.create("Black Dyke Band", yorkshire);
        this.bandService.create("Accrington Borough", northWest);
        this.bandService.create("Rothwell Temperance", yorkshire);
        this.bandService.create("48th Div. R.E. T.A", midlands);
        this.bandService.create("Aalesunds Ungdomsmusikkorps", norway);
        this.bandService.create("Abb Kettleby", midlands);
        this.bandService.create("Acceler8", northWest);
        this.bandService.create("Aberllefenni", wales);
        this.bandService.create("Aalborg Brass Band", denmark);
        this.bandService.create("102 (Cheshire) Transport Column R.A.S.C. (T.A.)", northWest);

        BandDao extinct = this.bandService.create("Extinct Yorkshire", yorkshire);
        extinct.setStatus(BandStatus.EXTINCT);
        extinct.setLatitude("0.00");
        extinct.setLongitude("0.00");
        this.bandService.update(extinct);

        ContestDao yorkshireArea = this.contestService.create("Yorkshire Area");
        yorkshireArea.setRegion(yorkshire);
        yorkshireArea = this.contestService.update(yorkshireArea);
        this.contestEventService.create(yorkshireArea, LocalDate.of(2001, 3, 3));

        logoutTestUser();
    }

    @Test
    void testGetYorkshireRegionContestsPageWorksSuccessfully() {
        String response = this.restTemplate.getForObject("http://localhost:" + this.port + "/regions/yorkshire/contests", String.class);
        assertNotNull(response);
        assertTrue(response.contains("<title>Yorkshire - Region - Brass Band Results</title>"));

        assertTrue(response.contains("<h2>Yorkshire</h2>"));
        assertTrue(response.contains(">Links<"));
        assertTrue(response.contains(">Bands<"));

        assertTrue(response.contains(">Yorkshire Area<"));
        assertTrue(response.contains(">1<"));
    }
}
