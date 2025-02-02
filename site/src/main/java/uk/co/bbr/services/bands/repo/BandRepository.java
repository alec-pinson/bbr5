package uk.co.bbr.services.bands.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uk.co.bbr.services.bands.dao.BandDao;

import java.util.List;
import java.util.Optional;

public interface BandRepository extends JpaRepository<BandDao, Long> {

    @Query("SELECT b FROM BandDao b ORDER BY b.name")
    List<BandDao> findAll();
    @Query("SELECT b FROM BandDao b WHERE UPPER(b.name) LIKE UPPER(CONCAT(:prefix, '%')) ORDER BY b.name")
    List<BandDao> findByPrefixOrderByName(String prefix);

    @Query("SELECT b FROM BandDao b " +
            "WHERE b.name LIKE UPPER('0%')" +
            "OR b.name LIKE UPPER('1%') " +
            "OR b.name LIKE UPPER('2%') " +
            "OR b.name LIKE UPPER('3%') " +
            "OR b.name LIKE UPPER('4%') " +
            "OR b.name LIKE UPPER('5%') " +
            "OR b.name LIKE UPPER('6%') " +
            "OR b.name LIKE UPPER('7%') " +
            "OR b.name LIKE UPPER('8%') " +
            "OR b.name LIKE UPPER('9%') ORDER BY b.name")
    List<BandDao> findWithNumberPrefixOrderByName();

    @Query("SELECT b FROM BandDao b WHERE b.slug = ?1")
    Optional<BandDao> fetchBySlug(String bandSlug);

    @Query("SELECT b FROM BandDao b WHERE b.oldId = ?1")
    Optional<BandDao> fetchByOldId(String bandOldId);

    @Query("SELECT b FROM BandDao b WHERE UPPER(b.name) = :bandNameUpper")
    List<BandDao> findExactNameMatch(String bandNameUpper);

    @Query("SELECT b FROM BandDao b WHERE UPPER(b.name) LIKE :bandNameUpper")
    List<BandDao> findContainsNameMatch(String bandNameUpper);

    @Query("SELECT b FROM BandDao b WHERE UPPER(b.name) LIKE :searchStringUpper")
    List<BandDao> lookupByPrefix(String searchStringUpper);

    @Query("SELECT COUNT(b) FROM BandDao b")
    int countBands();

    @Query("SELECT COUNT(b) FROM BandDao b WHERE LENGTH(b.website) > 2")
    int countBandsWithWebsite();

    @Query("SELECT COUNT(b) FROM BandDao b WHERE LENGTH(b.longitude) > 0 AND LENGTH(b.latitude) > 0")
    int countBandsOnMap();

    @Query("SELECT COUNT(b) FROM BandDao b WHERE LENGTH(b.longitude) > 0 AND LENGTH(b.latitude) > 0 AND b.status = uk.co.bbr.services.bands.types.BandStatus.EXTINCT")
    int countExtinctBandsOnMap();

    @Query("SELECT b FROM BandDao b WHERE b.id = (SELECT MAX(b1.id) FROM BandDao b1)")
    BandDao fetchLatestBand();
}
