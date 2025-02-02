package uk.co.bbr.services.people.sql;

import lombok.experimental.UtilityClass;
import uk.co.bbr.services.framework.sql.SqlExec;
import uk.co.bbr.services.people.sql.dto.PeopleWinnersSqlDto;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@UtilityClass
public class PeopleCountSql {

    private static final String PERSON_CONDUCTOR_COUNT_SQL = "SELECT conductor_id, count(*) FROM contest_result WHERE conductor_id IS NOT NULL AND result_position_type <> 'D' AND result_position_type <> 'W' GROUP BY conductor_id";
    private static final String PERSON_CONDUCTOR_2_COUNT_SQL = "SELECT conductor_two_id, count(*) FROM contest_result WHERE conductor_two_id IS NOT NULL AND result_position_type <> 'D' AND result_position_type <> 'W' GROUP BY conductor_two_id";
    private static final String PERSON_CONDUCTOR_3_COUNT_SQL = "SELECT conductor_three_id, count(*) FROM contest_result WHERE conductor_three_id IS NOT NULL AND result_position_type <> 'D' AND result_position_type <> 'W' GROUP BY conductor_three_id";
    private static final String PERSON_ADJUDICATOR_COUNT_SQL = "SELECT person_id, count(*) FROM contest_event_adjudicator WHERE person_id IS NOT NULL GROUP BY person_id";
    private static final String PERSON_COMPOSER_COUNT_SQL = "SELECT composer_id, count(*) FROM piece WHERE composer_id IS NOT NULL GROUP BY composer_id";
    private static final String PERSON_ARRANGER_COUNT_SQL = "SELECT arranger_id, count(*) FROM piece WHERE arranger_id IS NOT NULL GROUP BY arranger_id";

    public static Map<Long, Integer> selectConductorCounts(EntityManager entityManager) {
        return PeopleCountSql.selectCounts(entityManager, PERSON_CONDUCTOR_COUNT_SQL);
    }
    public static Map<Long, Integer> selectConductorTwoCounts(EntityManager entityManager) {
        return PeopleCountSql.selectCounts(entityManager, PERSON_CONDUCTOR_2_COUNT_SQL);
    }
    public static Map<Long, Integer> selectConductorThreeCounts(EntityManager entityManager) {
        return PeopleCountSql.selectCounts(entityManager, PERSON_CONDUCTOR_3_COUNT_SQL);
    }

    public static Map<Long, Integer> selectAdjudicatorCounts(EntityManager entityManager) {
        return PeopleCountSql.selectCounts(entityManager, PERSON_ADJUDICATOR_COUNT_SQL);
    }
    public static Map<Long, Integer> selectComposerCounts(EntityManager entityManager) {
        return PeopleCountSql.selectCounts(entityManager, PERSON_COMPOSER_COUNT_SQL);
    }
    public static Map<Long, Integer> selectArrangerCounts(EntityManager entityManager) {
        return PeopleCountSql.selectCounts(entityManager, PERSON_ARRANGER_COUNT_SQL);
    }

    private static Map<Long, Integer> selectCounts(EntityManager entityManager, String sql) {
        Map<Long, Integer>  returnData = new HashMap<>();
        try {
            Query query = entityManager.createNativeQuery(sql);
            List<Object[]> queryResults = query.getResultList();

            for (Object[] columnList : queryResults) {
                Long id = columnList[0] instanceof BigInteger ? ((BigInteger)columnList[0]).longValue() : (Integer)columnList[0];
                int count = columnList[1] instanceof BigInteger ? ((BigInteger)columnList[1]).intValue() : (Integer)columnList[1];
                returnData.put(id, count);
            }

            return returnData;
        } catch (Exception e) {
            throw new RuntimeException("SQL Failure, " + e.getMessage());
        }
    }
}

