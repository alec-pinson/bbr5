package uk.co.bbr.services.framework;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.time.LocalDateTime;

@Getter
@Setter
@MappedSuperclass
public class AbstractDao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="created", nullable=false)
    private LocalDateTime created = LocalDateTime.now();

    @Column(name="updated", nullable=false)
    private LocalDateTime updated = LocalDateTime.now();

    @Column(name="created_by")
    private String createdBy;

    @Column(name="updated_by")
    private String updatedBy;

    protected String escapeJson(String text) {
        return text.replace("'", "`");
    }
}


