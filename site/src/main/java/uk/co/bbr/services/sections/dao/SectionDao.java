package uk.co.bbr.services.sections.dao;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.co.bbr.services.framework.AbstractDao;
import uk.co.bbr.services.framework.mixins.NameTools;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name="section")
public class SectionDao extends AbstractDao implements NameTools {

    @Column(name="name", nullable=false)
    private String name;

    @Column(name="slug", nullable=false)
    private String slug;

    @Column(name="position", nullable=false)
    private int position;

    @Column(name="map_short_code")
    private String mapShortCode;

    @Column(name="translation_key")
    private String translationKey;

    public void setName(String name){
        String nameToSet = simplifyName(name);
        this.name = nameToSet;
    }
}