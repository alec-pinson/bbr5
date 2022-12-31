package uk.co.bbr.services.contests.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uk.co.bbr.services.contests.dao.ContestDao;
import uk.co.bbr.services.contests.dao.ContestTypeDao;

public interface ContestTypeRepository extends JpaRepository<ContestTypeDao, Long> {
    @Query("SELECT t FROM ContestTypeDao t WHERE t.slug = ?1")
    ContestTypeDao fetchBySlug(String slug);
}