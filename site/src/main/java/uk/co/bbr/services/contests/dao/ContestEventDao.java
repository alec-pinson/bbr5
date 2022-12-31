package uk.co.bbr.services.contests.dao;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.co.bbr.services.contests.types.ContestEventDateResolution;
import uk.co.bbr.services.framework.AbstractDao;
import uk.co.bbr.services.framework.mixins.NameTools;
import uk.co.bbr.services.venues.dao.VenueDao;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name="contest_event")
public class ContestEventDao extends AbstractDao implements NameTools {

    @Column(name="name", nullable=false)
    private String name;

    @Column(name="old_id")
    private String oldId;

    @Column(name="date_of_event", nullable=false)
    private LocalDate eventDate;

    @Column(name="date_resolution", nullable=false)
    private ContestEventDateResolution dateResolution;

    @ManyToOne(fetch= FetchType.EAGER, optional=false)
    @JoinColumn(name="contest_id")
    private ContestDao contest;

    @Column(name="notes")
    private String notes;

    @ManyToOne(fetch= FetchType.EAGER)
    @JoinColumn(name="venue_id")
    private VenueDao venue;

    @Column(name="complete", nullable=false)
    private boolean complete;

    @Column(name="no_contest", nullable=false)
    private boolean noContest;

    @Column(name="original_owner", nullable=false)
    private String originalOwner;

    @ManyToOne(fetch= FetchType.EAGER, optional=false)
    @JoinColumn(name="contest_type_id")
    private ContestTypeDao contestType;

    public void setName(String name){
        String nameToSet = simplifyName(name);
        this.name = nameToSet;
    }
}