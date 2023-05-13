package uk.co.bbr.services.contests.sql.dto;

import lombok.Getter;
import uk.co.bbr.services.framework.sql.AbstractSqlDto;

@Getter
public class ContestWinsSqlDto extends AbstractSqlDto {
    private final String bandSlug;
    private final String bandName;
    private final int winCount;

    public ContestWinsSqlDto(Object[] eachRowData) {
        this.bandSlug = (String)eachRowData[0];
        this.bandName = (String)eachRowData[1];
        this.winCount = (Integer)eachRowData[2];
    }
}
