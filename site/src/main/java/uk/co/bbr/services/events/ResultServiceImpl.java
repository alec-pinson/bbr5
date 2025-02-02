package uk.co.bbr.services.events;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.co.bbr.services.bands.BandService;
import uk.co.bbr.services.bands.dao.BandDao;
import uk.co.bbr.services.contests.dao.ContestDao;
import uk.co.bbr.services.contests.dto.ContestStreakContainerDto;
import uk.co.bbr.services.contests.dto.ContestStreakDto;
import uk.co.bbr.services.contests.sql.ContestResultSql;
import uk.co.bbr.services.contests.sql.dto.ContestResultPieceSqlDto;
import uk.co.bbr.services.contests.sql.dto.ContestWinsSqlDto;
import uk.co.bbr.services.events.dao.ContestEventDao;
import uk.co.bbr.services.events.dao.ContestResultDao;
import uk.co.bbr.services.events.dao.ContestResultPieceDao;
import uk.co.bbr.services.events.repo.ContestResultPieceRepository;
import uk.co.bbr.services.events.repo.ContestResultRepository;
import uk.co.bbr.services.events.sql.ResultFilterSql;
import uk.co.bbr.services.events.sql.dto.ContestResultDrawPositionSqlDto;
import uk.co.bbr.services.events.types.ContestEventDateResolution;
import uk.co.bbr.services.events.types.ResultPositionType;
import uk.co.bbr.services.people.dao.PersonDao;
import uk.co.bbr.services.pieces.dao.PieceDao;
import uk.co.bbr.services.regions.dao.RegionDao;
import uk.co.bbr.services.security.SecurityService;
import uk.co.bbr.web.security.annotations.IsBbrMember;

import javax.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ResultServiceImpl implements ResultService {

    private final ContestResultRepository contestResultRepository;
    private final ContestResultPieceRepository contestResultPieceRepository;
    private final BandService bandService;
    private final SecurityService securityService;
    private final EntityManager entityManager;

    @Override
    @IsBbrMember
    public ContestResultDao addResult(ContestEventDao event, ContestResultDao result) {
        result.setContestEvent(event);

        ContestResultDao returnResult;
        // is there an existing result for the same band?
        Optional<ContestResultDao> existingResult = this.contestResultRepository.fetchForEventAndBand(event.getId(), result.getBand().getId());
        if (existingResult.isPresent()) {
            ContestResultDao existingResultObject = existingResult.get();
            existingResultObject.populateFrom(result);
            existingResultObject.setUpdated(LocalDateTime.now());
            existingResultObject.setUpdatedBy(this.securityService.getCurrentUsername());
            returnResult = this.contestResultRepository.saveAndFlush(existingResultObject);
        } else {
            result.setCreated(LocalDateTime.now());
            result.setCreatedBy(this.securityService.getCurrentUsername());
            result.setUpdated(LocalDateTime.now());
            result.setUpdatedBy(this.securityService.getCurrentUsername());
            returnResult = this.contestResultRepository.saveAndFlush(result);
        }

        return returnResult;
    }

    @Override
    @IsBbrMember
    public ContestResultDao addResult(ContestEventDao event, String position, BandDao band, PersonDao conductor) {
        ContestResultDao newResult = new ContestResultDao();
        newResult.setPosition(position);
        newResult.setBand(band);
        newResult.setBandName(band.getName());
        newResult.setConductor(conductor);

        return this.addResult(event, newResult);
    }

    @Override
    public List<ContestResultDao> fetchForEvent(ContestEventDao event) {
        return this.contestResultRepository.findAllForEvent(event.getId());
    }

    @Override
    @IsBbrMember
    public ContestResultPieceDao addPieceToResult(ContestResultDao contestResult, ContestResultPieceDao contestResultTestPiece) {
        contestResultTestPiece.setContestResult(contestResult);

        contestResultTestPiece.setCreated(LocalDateTime.now());
        contestResultTestPiece.setCreatedBy(this.securityService.getCurrentUsername());
        contestResultTestPiece.setUpdated(LocalDateTime.now());
        contestResultTestPiece.setUpdatedBy(this.securityService.getCurrentUsername());

        return this.contestResultPieceRepository.saveAndFlush(contestResultTestPiece);
    }

    @Override
    @IsBbrMember
    public ContestResultPieceDao addPieceToResult(ContestResultDao contestResult, PieceDao piece) {
        ContestResultPieceDao newPiece = new ContestResultPieceDao();
        newPiece.setPiece(piece);
        return this.addPieceToResult(contestResult, newPiece);
    }

    @Override
    public List<ContestResultPieceDao> fetchResultsWithOwnChoicePieces(ContestDao contest) {
        List<ContestResultPieceDao> returnData = new ArrayList<>();

        List<ContestResultPieceSqlDto> pieceResults = ContestResultSql.selectOwnChoiceUsedForContest(this.entityManager, contest.getId());
        for (ContestResultPieceSqlDto eachResult : pieceResults) {
            ContestResultPieceDao eachReturnPiece = new ContestResultPieceDao();
            eachReturnPiece.setPiece(new PieceDao());
            eachReturnPiece.setContestResult(new ContestResultDao());
            eachReturnPiece.getContestResult().setBand(new BandDao());
            eachReturnPiece.getContestResult().getBand().setRegion(new RegionDao());
            eachReturnPiece.getContestResult().setContestEvent(new ContestEventDao());
            eachReturnPiece.getContestResult().getContestEvent().setContest(new ContestDao());

            eachReturnPiece.getContestResult().getContestEvent().setEventDate(eachResult.getEventDate());
            eachReturnPiece.getContestResult().getContestEvent().setEventDateResolution(ContestEventDateResolution.fromCode(eachResult.getDateResolution()));
            eachReturnPiece.getContestResult().getContestEvent().getContest().setSlug(eachResult.getContestSlug());
            eachReturnPiece.getContestResult().setBandName(eachResult.getBandCompetedAs());
            eachReturnPiece.getContestResult().getBand().setSlug(eachResult.getBandSlug());
            eachReturnPiece.getContestResult().getBand().setName(eachResult.getBandName());
            eachReturnPiece.getPiece().setName(eachResult.getPieceName());
            eachReturnPiece.getPiece().setSlug(eachResult.getPieceSlug());
            eachReturnPiece.getPiece().setYear(eachResult.getPieceYear());
            eachReturnPiece.getContestResult().setPosition(eachResult.getPosition());
            eachReturnPiece.getContestResult().setResultPositionType(ResultPositionType.fromCode(eachResult.getPositionType()));
            eachReturnPiece.getContestResult().getBand().getRegion().setName(eachResult.getRegionName());
            eachReturnPiece.getContestResult().getBand().getRegion().setCountryCode(eachResult.getRegionCountryCode());

            returnData.add(eachReturnPiece);
        }

        return returnData;
    }

    @Override
    public int fetchCountOfOwnChoiceForContest(ContestDao contest) {
        return this.contestResultPieceRepository.fetchCountOfOwnChoiceForContest(contest.getId());
    }

    @Override
    public List<ContestWinsSqlDto> fetchWinsCounts(ContestDao contest) {
        return ContestResultSql.selectWinsForContest(this.entityManager, contest.getId());
    }

    @Override
    public Set<PersonDao> fetchBandConductors(BandDao band) {
        Set<PersonDao> conductors = new HashSet<>();

        if (band != null) {
            List<ContestResultDao> bandResults = this.contestResultRepository.findAllForBand(band.getId());

            for (ContestResultDao eachResult : bandResults) {
                if (eachResult.getConductor() != null) {
                    conductors.add(eachResult.getConductor());
                }
                if (eachResult.getConductorSecond() != null) {
                    conductors.add(eachResult.getConductorSecond());
                }
                if (eachResult.getConductorThird() != null) {
                    conductors.add(eachResult.getConductorThird());
                }
            }
        }

        return conductors;
    }

    @Override
    public List<ContestResultDao> fetchResultsForContestAndPosition(ContestDao contest, String position) {
        List<ContestResultDrawPositionSqlDto> results;
        switch (position) {
            case "W":
                results = ResultFilterSql.selectContestResultsForWithdrawn(this.entityManager, contest.getSlug());
                break;
            case "D":
                results = ResultFilterSql.selectContestResultsForDisqualified(this.entityManager, contest.getSlug());
                break;
            default:
                results = ResultFilterSql.selectContestResultsForPosition(this.entityManager, contest.getSlug(), position);
                break;
        }

        List<ContestResultDao> resultsToReturn = new ArrayList<>();
        for (ContestResultDrawPositionSqlDto eachSqlResult : results) {
            resultsToReturn.add(eachSqlResult.getResult());
        }

        return resultsToReturn;
    }

    @Override
    public List<ContestResultDao> fetchResultsForContestAndDraw(ContestDao contest, int draw) {
        List<ContestResultDrawPositionSqlDto> results = ResultFilterSql.selectContestResultsForDraw(this.entityManager, contest.getSlug(), draw);

        List<ContestResultDao> resultsToReturn = new ArrayList<>();
        for (ContestResultDrawPositionSqlDto eachSqlResult : results) {
            resultsToReturn.add(eachSqlResult.getResult());
        }

        return resultsToReturn;
    }

    @Override
    public void update(ContestResultDao result) {
        result.setUpdatedBy(this.securityService.getCurrentUsername());
        result.setUpdated(LocalDateTime.now());
        this.contestResultRepository.saveAndFlush(result);
    }

    @Override
    public List<ContestStreakDto> fetchStreaksForContest(ContestDao contest) {
        Map<String, List<Integer>> streaksBandSlugToYear = this.fetchStreakData(contest);

        ContestStreakContainerDto streaks = new ContestStreakContainerDto();
        streaks.populate(streaksBandSlugToYear, this.bandService);
        return streaks.getStreaks();
    }

    private Map<String, List<Integer>> fetchStreakData(ContestDao contest) {
        Map<String, List<Integer>> streaksBandSlugToYear = new HashMap<>();

        List<ContestResultDrawPositionSqlDto> wins = ResultFilterSql.selectContestResultsForPosition(this.entityManager, contest.getSlug(), "1");
        for (ContestResultDrawPositionSqlDto win : wins) {
            String currentBandSlug = win.getResult().getBand().getSlug();
            List<Integer> existingRecord = streaksBandSlugToYear.get(currentBandSlug);
            if (existingRecord == null) {
                existingRecord = new ArrayList<>();
            }
            existingRecord.add(win.getResult().getContestEvent().getEventDate().getYear());
            streaksBandSlugToYear.put(currentBandSlug, existingRecord);
        }
        return streaksBandSlugToYear;
    }

    @Override
    @IsBbrMember
    public ContestResultDao migrate(ContestEventDao event, ContestResultDao contestResult) {
        contestResult.setContestEvent(event);
        return this.contestResultRepository.saveAndFlush(contestResult);
    }
}
