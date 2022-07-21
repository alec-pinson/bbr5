package uk.co.bbr.services.band.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.co.bbr.services.band.dao.BandDao;
import uk.co.bbr.services.band.dao.BandRelationshipDao;

public interface BandRelationshipRepository extends JpaRepository<BandRelationshipDao, Long> {
}
