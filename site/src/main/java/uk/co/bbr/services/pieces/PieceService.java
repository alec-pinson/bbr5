package uk.co.bbr.services.pieces;

import uk.co.bbr.services.people.dao.PersonDao;
import uk.co.bbr.services.pieces.dao.PieceAlias;
import uk.co.bbr.services.pieces.dao.PieceDao;
import uk.co.bbr.services.pieces.dto.PieceListDto;
import uk.co.bbr.services.pieces.types.PieceCategory;

import java.util.List;
import java.util.Optional;

public interface PieceService {
    PieceDao create(PieceDao newPiece);
    PieceDao migrate(PieceDao piece);

    PieceDao create(String name, PieceCategory category, PersonDao composer);
    PieceDao create(String name);

    void createAlternativeName(PieceDao piece, PieceAlias alternativeName);

    void migrateAlternativeName(PieceDao piece, PieceAlias previousName);

    Optional<PieceDao> fetchBySlug(String pieceSlug);

    Optional<PieceDao> fetchById(Long pieceId);

    List<PieceAlias> fetchAlternateNames(PieceDao piece);

    PieceListDto listPiecesStartingWith(String letter);

    List<PieceDao> findPiecesForPerson(PersonDao person);
}
