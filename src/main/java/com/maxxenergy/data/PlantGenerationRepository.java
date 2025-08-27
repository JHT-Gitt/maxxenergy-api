package com.maxxenergy.data;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PlantGenerationRepository extends JpaRepository<PlantGeneration, Long> {

    // existing
    List<PlantGeneration> findByOrderByTimeStampedCleanDesc(Pageable pageable);
    List<PlantGeneration> findByTimeStampedCleanBetweenOrderByTimeStampedCleanAsc(LocalDateTime from, LocalDateTime to);
    List<PlantGeneration> findByPlantIdAndTimeStampedCleanBetweenOrderByTimeStampedCleanAsc(
            String plantId, LocalDateTime from, LocalDateTime to);

    Optional<PlantGeneration> findTopByOrderByTimeStampedCleanDesc();
    Optional<PlantGeneration> findTopByOrderByTimeStampedCleanAsc();

    // ---------- projections ----------
    interface PlantSummaryRow {
        String getPlantId();
        LocalDateTime getStartDate();
        LocalDateTime getEndDate();
        Integer getInverterCount();
    }

    interface DailyEnergyRow {
        String getPlantId();
        java.time.LocalDate getDay();
        Double getKwh();
    }

    interface CumulativeRow {
        String getPlantId();
        java.time.LocalDate getDay();
        Double getTotalKwh();
    }

    interface HourlyProfileRow {
        Integer getHourOfDay();
        Double getAvgAcKw();
        Double getAvgDcKw();
    }

    interface EfficiencyDayRow {
        java.time.LocalDate getDay();
        Double getAvgEfficiency();
    }

    interface HealthDayRow {
        java.time.LocalDate getDay();
        Long getIntervals();
        Long getNonZero();
    }

    // ---------- summary ----------
    @Query(value = """
    SELECT plant_id AS plantId,
           MIN(time_stamped_clean) AS startDate,
           MAX(time_stamped_clean) AS endDate,
           COUNT(DISTINCT source_key) AS inverterCount
    FROM plant_generation
    GROUP BY plant_id
    """, nativeQuery = true)
    List<PlantSummaryRow> plantSummaries();

    // ---------- daily energy from daily_yield ----------
    @Query(value = """
    SELECT plant_id AS plantId,
           DATE(time_stamped_clean) AS day,
           MAX(daily_yield) AS kwh
    FROM plant_generation
    WHERE (:plantId IS NULL OR plant_id = :plantId)
      AND time_stamped_clean BETWEEN :from AND :to
    GROUP BY plant_id, day
    ORDER BY day
    """, nativeQuery = true)
    List<DailyEnergyRow> dailyEnergy(@Param("plantId") String plantId,
                                     @Param("from") LocalDateTime from,
                                     @Param("to") LocalDateTime to);

    // ---------- cumulative from total_yield (last of day) ----------
    @Query(value = """
    SELECT plant_id AS plantId,
           DATE(time_stamped_clean) AS day,
           MAX(total_yield) AS totalKwh
    FROM plant_generation
    WHERE (:plantId IS NULL OR plant_id = :plantId)
      AND time_stamped_clean BETWEEN :from AND :to
    GROUP BY plant_id, day
    ORDER BY day
    """, nativeQuery = true)
    List<CumulativeRow> cumulative(@Param("plantId") String plantId,
                                   @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to);

    // ---------- hour-of-day profile ----------
    @Query(value = """
    SELECT EXTRACT(HOUR FROM time_stamped_clean) AS hourOfDay,
           AVG(ac_power) AS avgAcKw,
           AVG(dc_power) AS avgDcKw
    FROM plant_generation
    WHERE (:plantId IS NULL OR plant_id = :plantId)
      AND time_stamped_clean BETWEEN :from AND :to
    GROUP BY hourOfDay
    ORDER BY hourOfDay
    """, nativeQuery = true)
    List<HourlyProfileRow> hourlyProfile(@Param("plantId") String plantId,
                                         @Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to);

    // ---------- efficiency (AC/DC) by day ----------
    @Query(value = """
    SELECT DATE(time_stamped_clean) AS day,
           AVG(ac_power / NULLIF(dc_power,0)) AS avgEfficiency
    FROM plant_generation
    WHERE (:plantId IS NULL OR plant_id = :plantId)
      AND time_stamped_clean BETWEEN :from AND :to
    GROUP BY day
    ORDER BY day
    """, nativeQuery = true)
    List<EfficiencyDayRow> efficiencyByDay(@Param("plantId") String plantId,
                                           @Param("from") LocalDateTime from,
                                           @Param("to") LocalDateTime to);

    // ---------- health (uptime) ----------
    @Query(value = """
    SELECT DATE(time_stamped_clean) AS day,
           COUNT(*) AS intervals,
           SUM(CASE WHEN ac_power>0 OR dc_power>0 THEN 1 ELSE 0 END) AS nonZero
    FROM plant_generation
    WHERE (:plantId IS NULL OR plant_id = :plantId)
      AND time_stamped_clean BETWEEN :from AND :to
    GROUP BY day
    ORDER BY day
    """, nativeQuery = true)
    List<HealthDayRow> health(@Param("plantId") String plantId,
                              @Param("from") LocalDateTime from,
                              @Param("to") LocalDateTime to);
}
