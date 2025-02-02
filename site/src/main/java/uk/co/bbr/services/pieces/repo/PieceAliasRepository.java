package uk.co.bbr.services.pieces.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uk.co.bbr.services.pieces.dao.PieceAliasDao;

import java.util.List;

public interface PieceAliasRepository extends JpaRepository<PieceAliasDao, Long> {

    @Query("SELECT a FROM PieceAliasDao a WHERE a.piece.id = ?1")
    List<PieceAliasDao> findForPieceId(Long personId);
}
