package uk.co.bbr.services.bands;

import uk.co.bbr.services.bands.dao.BandDao;
import uk.co.bbr.services.bands.dao.BandPreviousNameDao;
import uk.co.bbr.services.bands.dao.BandRelationshipDao;
import uk.co.bbr.services.bands.dao.BandRelationshipTypeDao;
import uk.co.bbr.services.bands.dto.BandListDto;
import uk.co.bbr.services.bands.sql.dto.BandWinnersSqlDto;
import uk.co.bbr.services.bands.types.RehearsalDay;
import uk.co.bbr.services.regions.dao.RegionDao;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BandService {

    BandDao create(BandDao band);
    BandDao migrate(BandDao band);

    BandDao create(String bandName);

    BandDao create(String bandName, RegionDao region);

    BandListDto listBandsStartingWith(String prefix);

    void createRehearsalDay(BandDao band, RehearsalDay day);
    void migrateRehearsalNight(BandDao band, RehearsalDay day);

    List<RehearsalDay> findRehearsalNights(BandDao band);

    Optional<BandDao> fetchBySlug(String bandSlug);

    Optional<BandDao> fetchBandByOldId(String bandOldId);

    BandPreviousNameDao createPreviousName(BandDao band, BandPreviousNameDao previousName);
    BandPreviousNameDao migratePreviousName(BandDao band, BandPreviousNameDao previousName);

    BandRelationshipTypeDao fetchIsParentOfRelationship();

    BandRelationshipDao saveRelationship(BandRelationshipDao relationship);
    BandRelationshipDao migrateRelationship(BandRelationshipDao relationship);

    BandDao update(BandDao band);

    Optional<BandPreviousNameDao> aliasExists(BandDao band, String aliasName);

    List<BandPreviousNameDao> findVisiblePreviousNames(BandDao band);
    List<BandPreviousNameDao> findAllPreviousNames(BandDao band);

    BandDao findMatchingBandByName(String bandName, LocalDate dateContext);

    List<BandWinnersSqlDto> fetchContestWinningBands();

    void showPreviousBandName(BandDao band, Long aliasId);

    void hidePreviousBandName(BandDao band, Long aliasId);

    void deletePreviousBandName(BandDao band, Long aliasId);
}
