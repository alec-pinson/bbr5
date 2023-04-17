package uk.co.bbr.services.contests.sql.dto;

import lombok.Getter;

import java.math.BigInteger;

@Getter
public class ResultPieceSqlDto {


    private BigInteger contestResultId;
    private String pieceSlug;
    private String pieceName;
    private String pieceYear;



    public ResultPieceSqlDto(Object[] eachRowData) {
        this.contestResultId = (BigInteger)eachRowData[0];
        this.pieceSlug = (String)eachRowData[1];
        this.pieceName = (String)eachRowData[2];
        this.pieceYear = (String)eachRowData[3];
    }
}