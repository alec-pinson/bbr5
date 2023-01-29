package uk.co.bbr.services.contests.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uk.co.bbr.services.contests.dao.ContestGroupDao;

import java.util.List;
import java.util.Optional;

public interface ContestGroupRepository extends JpaRepository<ContestGroupDao, Long> {
    @Query("SELECT g FROM ContestGroupDao g WHERE g.slug = ?1")
    Optional<ContestGroupDao> fetchBySlug(String slug);
    @Query("SELECT g FROM ContestGroupDao g WHERE g.name = ?1")
    Optional<ContestGroupDao> fetchByName(String name);

    @Query("SELECT g FROM ContestGroupDao g " +
            "ORDER BY g.name")
    List<ContestGroupDao> findAllOrderByName();

    @Query("SELECT g FROM ContestGroupDao g " +
            "WHERE UPPER(g.name) LIKE UPPER(CONCAT(:prefix, '%')) " +
            "ORDER BY g.name")
    List<ContestGroupDao> findByPrefixOrderByName(String prefix);
}
