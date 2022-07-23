package uk.co.bbr.services.people.dao;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.co.bbr.services.framework.AbstractDao;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name="PERSON_PREVIOUS_NAME")
public class PersonPreviousNameDao extends AbstractDao {

    @Column(name="OLD_NAME", nullable=false)
    private String oldName;

    @Column(name="HIDDEN")
    private boolean hidden;
}
