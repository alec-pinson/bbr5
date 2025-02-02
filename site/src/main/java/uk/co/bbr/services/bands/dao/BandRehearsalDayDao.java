package uk.co.bbr.services.bands.dao;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.co.bbr.services.bands.types.RehearsalDay;
import uk.co.bbr.services.framework.AbstractDao;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Getter
@Entity
@NoArgsConstructor
@Table(name="band_rehearsal_day")
public class BandRehearsalDayDao extends AbstractDao {

    @Column(name="day_number", nullable=false)
    @Setter
    private RehearsalDay day;

    @ManyToOne(fetch=FetchType.EAGER, optional=false)
    @JoinColumn(name="band_id")
    @Setter
    private BandDao band;

    @Column(name="details")
    private String details;

    public void setDetails(String value) {
        if (value == null) {
            this.details = null;
            return;
        }

        this.details = value.trim();
    }

    public String getDayTranslationKey() {
        return this.day.getTranslationKey();
    }
}
