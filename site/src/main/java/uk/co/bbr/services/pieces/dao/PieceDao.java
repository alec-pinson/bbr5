package uk.co.bbr.services.pieces.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.co.bbr.services.framework.AbstractDao;
import uk.co.bbr.services.framework.mixins.NameTools;
import uk.co.bbr.services.people.dao.PersonDao;
import uk.co.bbr.services.pieces.types.PieceCategory;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

@Getter
@Entity
@NoArgsConstructor
@Table(name="piece")
public class PieceDao extends AbstractDao implements NameTools {
    @Column(name="old_id")
    private String oldId;

    @Column(name="name")
    private String name;

    @Column(name="slug", nullable=false)
    private String slug;

    @Column(name="notes")
    private String notes;

    @Column(name="piece_year")
    private String year;

    @Column(name="category")
    @Setter
    private PieceCategory category;

    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name="composer_id")
    @Setter
    private PersonDao composer;

    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name="arranger_id")
    @Setter
    private PersonDao arranger;

    @Transient
    @Setter
    private int setTestCount;

    @Transient
    @Setter
    private int ownChoiceCount;

    public void setNotes(String notes) {
        if (notes != null) {
            notes = notes.trim();
        }
        this.notes = notes;
    }

    public void setSlug(String value) {
        if (value != null) {
            value = value.trim();
        }
        this.slug = value;
    }

    public void setOldId(String oldId) {
        if (oldId != null) {
            oldId = oldId.trim();
        }
        this.oldId = oldId;
    }

    public void setName(String name) {
        if (name == null) {
            this.name = null;
            return;
        }

        this.name = simplifyPieceName(name);
    }

    public void setYear(String year) {
        if (year != null) {
            year = year.trim();
        }
        this.year = year;
    }

    public ObjectNode asLookup(ObjectMapper objectMapper) {
        ObjectNode piece = objectMapper.createObjectNode();
        piece.put("slug", this.getSlug());
        piece.put("name", this.escapeJson(this.name));
        piece.put("context", "");
        return piece;
    }
}
