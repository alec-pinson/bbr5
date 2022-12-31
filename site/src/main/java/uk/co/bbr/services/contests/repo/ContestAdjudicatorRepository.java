package uk.co.bbr.services.contests.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uk.co.bbr.services.contests.dao.ContestAdjudicatorDao;
import uk.co.bbr.services.contests.dao.ContestAliasDao;

import java.util.List;

public interface ContestAdjudicatorRepository extends JpaRepository<ContestAdjudicatorDao, Long> {
    @Query("SELECT a FROM ContestAdjudicatorDao a WHERE a.contestEvent.id = ?1")
    List<ContestAdjudicatorDao> fetchForEvent(Long contestEventId);
}