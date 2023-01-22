package uk.co.bbr.services.migrate;

import lombok.RequiredArgsConstructor;
import org.jdom2.Element;
import org.springframework.stereotype.Service;
import uk.co.bbr.services.bands.BandService;
import uk.co.bbr.services.bands.dao.BandDao;
import uk.co.bbr.services.contests.ContestEventService;
import uk.co.bbr.services.contests.ContestResultService;
import uk.co.bbr.services.contests.ContestService;
import uk.co.bbr.services.contests.ContestTypeService;
import uk.co.bbr.services.contests.dao.ContestDao;
import uk.co.bbr.services.contests.dao.ContestEventDao;
import uk.co.bbr.services.contests.dao.ContestEventTestPieceDao;
import uk.co.bbr.services.contests.dao.ContestResultDao;
import uk.co.bbr.services.contests.dao.ContestResultPieceDao;
import uk.co.bbr.services.contests.dao.ContestTypeDao;
import uk.co.bbr.services.contests.types.ContestEventDateResolution;
import uk.co.bbr.services.contests.types.TestPieceAndOr;
import uk.co.bbr.services.framework.mixins.SlugTools;
import uk.co.bbr.services.people.PersonService;
import uk.co.bbr.services.people.dao.PersonDao;
import uk.co.bbr.services.pieces.PieceService;
import uk.co.bbr.services.pieces.dao.PieceDao;
import uk.co.bbr.services.security.SecurityService;
import uk.co.bbr.services.venues.VenueService;
import uk.co.bbr.services.venues.dao.VenueDao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EventMigrationServiceImpl extends AbstractMigrationServiceImpl implements EventMigrationService, SlugTools {

    private final SecurityService securityService;
    private final ContestEventService contestEventService;
    private final ContestResultService contestResultService;
    private final ContestTypeService contestTypeService;
    private final BandService bandService;
    private final PersonService personService;
    private final VenueService venueService;
    private final ContestService contestService;
    private final PieceService pieceService;

    @Override
    public void migrate(Element rootNode) {
        System.out.println(rootNode.getChildText("name"));

        ContestEventDao contestEvent = new ContestEventDao();
        contestEvent.setOldId(rootNode.getAttributeValue("id"));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-M-d");
        LocalDate eventDate = LocalDate.parse(rootNode.getChild("event-date").getAttributeValue("date"), formatter);
        contestEvent.setEventDate(eventDate);

        contestEvent.setEventDateResolution(ContestEventDateResolution.fromCode(rootNode.getChildText("event-date-resolution")));
        contestEvent.setName(this.notBlank(rootNode, "name"));

        Optional<ContestDao> contestOptional = this.contestService.fetchBySlug(rootNode.getChild("contest").getAttributeValue("slug"));
        if (contestOptional.isEmpty()) {
            throw new UnsupportedOperationException("Contest not found");
        }
        contestEvent.setContest(contestOptional.get());

        contestEvent.setContestType(contestEvent.getContest().getDefaultContestType());
        if (rootNode.getChild("contest-type-override") != null) {
            String contestTypeName = rootNode.getChildText("contest-type-override").replace("Chuch Concert, Test Piece", "Church Concert, Test Piece");
            Optional<ContestTypeDao> contestType = this.contestTypeService.fetchByName(contestTypeName);
            if (contestType.isEmpty()) {
                throw new UnsupportedOperationException("Failed to find contest type override");
            }
            contestEvent.setContestType(contestType.get());
        }

        if (rootNode.getChild("venue") != null) {
            Optional<VenueDao> venueOptional = this.venueService.fetchBySlug(rootNode.getChild("venue").getAttributeValue("slug"));
            if (venueOptional.isPresent()) {
                contestEvent.setVenue(venueOptional.get());
            }
        }

        Element noContest = rootNode.getChild("no-contest");
        if (noContest != null) {
            contestEvent.setNotes(this.notBlank(rootNode, "no-contest"));
            contestEvent.setNoContest(true);
        } else {
            contestEvent.setNotes(this.notBlank(rootNode, "notes"));
        }

        contestEvent.setCreatedBy(this.createUser(this.notBlank(rootNode, "owner"), this.securityService));
        contestEvent.setUpdatedBy(this.createUser(this.notBlank(rootNode, "lastChangedBy"), this.securityService));
        contestEvent.setCreated(this.notBlankDateTime(rootNode, "created"));
        contestEvent.setUpdated(this.notBlankDateTime(rootNode, "lastModified"));

        contestEvent = this.contestEventService.migrate(contestOptional.get(), contestEvent);

        this.migrateEventTestPieces(contestEvent, rootNode.getChild("test-piece"), rootNode.getChild("extra-test-pieces"));

        for (Element eachResultNode : rootNode.getChild("results").getChildren()) {
            this.migrateResult(contestEvent, eachResultNode);
        }
    }


    private void migrateResult(ContestEventDao contestEvent, Element eachResultNode) {

        ContestResultDao contestResult = new ContestResultDao();
        contestResult.setContestEvent(contestEvent);
        contestResult.setOldId(eachResultNode.getAttributeValue("id"));
        contestResult.setPosition(eachResultNode.getChild("result").getAttributeValue("code"));

        Optional<BandDao> band = this.bandService.fetchBySlug(eachResultNode.getChild("band").getAttributeValue("slug"));
        if (band.isEmpty()) {
            throw new UnsupportedOperationException("Band not found [" + eachResultNode.getChild("band").getAttributeValue("slug")  + "]");
        }
        contestResult.setBand(band.get());
        contestResult.setBandName(this.notBlank(eachResultNode, "band_name"));

        Optional<PersonDao> conductor1 = this.personService.fetchBySlug(eachResultNode.getChild("conductor").getAttributeValue("slug"));
        if (conductor1.isPresent()) {
            contestResult.setConductor(conductor1.get());
        }

        if (eachResultNode.getChild("conductor2") != null) {
            Optional<PersonDao> conductor2 = this.personService.fetchBySlug(eachResultNode.getChild("conductor2").getAttributeValue("slug"));
            if (conductor2.isPresent()) {
                contestResult.setConductorSecond(conductor2.get());
            }
        }

        String originalConductorName = this.notBlank(eachResultNode, "conductor_name");
        if (originalConductorName != null) {
            contestResult.setOriginalConductorName(originalConductorName);
        }

        contestResult.setDraw(this.notBlankInteger(eachResultNode, "draw"));
        contestResult.setDrawSecond(this.notBlankInteger(eachResultNode, "draw_second_part"));

        contestResult.setPointsTotal(this.notBlank(eachResultNode, "points"));
        contestResult.setPointsFirst(this.notBlank(eachResultNode, "points_first_part"));
        contestResult.setPointsSecond(this.notBlank(eachResultNode, "points_second_part"));
        contestResult.setPointsThird(this.notBlank(eachResultNode, "points_third_part"));
        contestResult.setPointsFourth(this.notBlank(eachResultNode, "points_fourth_part"));
        contestResult.setPointsPenalty(this.notBlank(eachResultNode, "penalty_points"));

        contestResult.setCreatedBy(this.createUser(this.notBlank(eachResultNode, "owner"), this.securityService));
        contestResult.setUpdatedBy(this.createUser(this.notBlank(eachResultNode, "lastChangedBy"), this.securityService));
        contestResult.setCreated(this.notBlankDateTime(eachResultNode, "created"));
        contestResult.setUpdated(this.notBlankDateTime(eachResultNode, "lastModified"));

        if (contestResult.getCreatedBy() == null) {
            contestResult.setCreatedBy(this.securityService.getCurrentUser());
        }
        if (contestResult.getUpdatedBy() == null) {
            contestResult.setUpdatedBy(this.securityService.getCurrentUser());
        }

        if (contestResult.getCreated() == null) {
            contestResult.setCreated(LocalDateTime.now());
        }
        if (contestResult.getUpdated() == null) {
            contestResult.setUpdated(LocalDateTime.now());
        }

        this.contestResultService.migrate(contestEvent, contestResult);

        this.migrateResultTestPieces(contestResult, eachResultNode.getChild("test-piece"), eachResultNode.getChild("extra-test-pieces"));
    }

    private void migrateEventTestPieces(ContestEventDao contestEvent, Element testPieceNode, Element extraPiecesNode) {
        if (testPieceNode != null && testPieceNode.getAttributeValue("slug") != null) {
            Optional<PieceDao> testPiece = this.pieceService.fetchBySlug(testPieceNode.getAttributeValue("slug"));
            if (testPiece.isEmpty()) {
                throw new UnsupportedOperationException("Test piece not found [" + testPieceNode.getAttributeValue("slug") + "]");
            }
            ContestEventTestPieceDao contestEventTestPiece = new ContestEventTestPieceDao();
            contestEventTestPiece.setContestEvent(contestEvent);
            contestEventTestPiece.setPiece(testPiece.get());
            contestEventTestPiece.setAndOr(null);

            contestEventTestPiece.setCreated(contestEvent.getCreated());
            contestEventTestPiece.setUpdated(contestEvent.getUpdated());
            contestEventTestPiece.setCreatedBy(contestEvent.getCreatedBy());
            contestEventTestPiece.setUpdatedBy(contestEvent.getUpdatedBy());
            this.contestEventService.addTestPieceToContest(contestEvent, contestEventTestPiece);
        }

        if (extraPiecesNode != null) {
            for (Element eachExtraPieceNode : extraPiecesNode.getChildren()) {
                Optional<PieceDao> testPiece = this.pieceService.fetchBySlug(eachExtraPieceNode.getChild("test-piece").getAttributeValue("slug"));
                if (testPiece.isEmpty()) {
                    throw new UnsupportedOperationException("Test piece not found [" + eachExtraPieceNode.getChild("test-piece").getAttributeValue("slug") + "]");
                }
                String andOr = eachExtraPieceNode.getChildText("and_or");

                ContestEventTestPieceDao contestEventTestPiece = new ContestEventTestPieceDao();
                contestEventTestPiece.setContestEvent(contestEvent);
                contestEventTestPiece.setPiece(testPiece.get());

                if ("and".equals(andOr)) {
                    contestEventTestPiece.setAndOr(TestPieceAndOr.AND);
                }
                if ("or".equals(andOr)) {
                    contestEventTestPiece.setAndOr(TestPieceAndOr.OR);
                }

                contestEventTestPiece.setCreated(contestEvent.getCreated());
                contestEventTestPiece.setUpdated(contestEvent.getUpdated());
                contestEventTestPiece.setCreatedBy(contestEvent.getCreatedBy());
                contestEventTestPiece.setUpdatedBy(contestEvent.getUpdatedBy());
                this.contestEventService.addTestPieceToContest(contestEvent, contestEventTestPiece);
            }
        }
    }

    private void migrateResultTestPieces(ContestResultDao contestResult, Element testPieceNode, Element extraPiecesNode) {
        if (testPieceNode != null && testPieceNode.getAttributeValue("slug") != null) {
            Optional<PieceDao> testPiece = this.pieceService.fetchBySlug(testPieceNode.getAttributeValue("slug"));
            if (testPiece.isEmpty()) {
                throw new UnsupportedOperationException("Test piece not found [" + testPieceNode.getAttributeValue("slug") + "]");
            }
            ContestResultPieceDao contestResultTestPiece = new ContestResultPieceDao();
            contestResultTestPiece.setContestResult(contestResult);
            contestResultTestPiece.setPiece(testPiece.get());
            contestResultTestPiece.setOrdering(0);

            contestResultTestPiece.setCreated(contestResult.getCreated());
            contestResultTestPiece.setUpdated(contestResult.getUpdated());
            contestResultTestPiece.setCreatedBy(contestResult.getCreatedBy());
            contestResultTestPiece.setUpdatedBy(contestResult.getUpdatedBy());
            this.contestResultService.addPieceToResult(contestResult, contestResultTestPiece);
        }

        if (extraPiecesNode != null) {
            for (Element eachExtraPieceNode : extraPiecesNode.getChildren()) {
                Optional<PieceDao> testPiece = this.pieceService.fetchBySlug(eachExtraPieceNode.getChild("test-piece").getAttributeValue("slug"));
                if (testPiece.isEmpty()) {
                    throw new UnsupportedOperationException("Test piece not found [" + eachExtraPieceNode.getChild("test-piece").getAttributeValue("slug") + "]");
                }
                String ordering = eachExtraPieceNode.getChildText("ordering");

                ContestResultPieceDao contestResultTestPiece = new ContestResultPieceDao();
                contestResultTestPiece.setContestResult(contestResult);
                contestResultTestPiece.setPiece(testPiece.get());
                contestResultTestPiece.setOrdering(Integer.parseInt(ordering));

                contestResultTestPiece.setCreated(contestResult.getCreated());
                contestResultTestPiece.setUpdated(contestResult.getUpdated());
                contestResultTestPiece.setCreatedBy(contestResult.getCreatedBy());
                contestResultTestPiece.setUpdatedBy(contestResult.getUpdatedBy());
                this.contestResultService.addPieceToResult(contestResult, contestResultTestPiece);
            }
        }
    }
}
