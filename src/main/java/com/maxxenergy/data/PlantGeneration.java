package com.maxxenergy.data;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "plant_generation",
        indexes = {
                @Index(name = "idx_pg_time", columnList = "time_stamped_clean"),
                @Index(name = "idx_pg_plant_time", columnList = "plant_id,time_stamped_clean")
        }
)
public class PlantGeneration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ac_power")
    private Double acPower;

    @Column(name = "dc_power")
    private Double dcPower;

    @Column(name = "daily_yield")
    private Double dailyYield;

    @Column(name = "total_yield")
    private Double totalYield;

    @Column(name = "plant_id")
    private String plantId;

    @Column(name = "source_key")
    private String sourceKey;

    // DB column name is snake_case
    @Column(name = "time_stamped_clean")
    private LocalDateTime timeStampedClean;

    // --- getters/setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Double getAcPower() { return acPower; }
    public void setAcPower(Double acPower) { this.acPower = acPower; }

    public Double getDcPower() { return dcPower; }
    public void setDcPower(Double dcPower) { this.dcPower = dcPower; }

    public Double getDailyYield() { return dailyYield; }
    public void setDailyYield(Double dailyYield) { this.dailyYield = dailyYield; }

    public Double getTotalYield() { return totalYield; }
    public void setTotalYield(Double totalYield) { this.totalYield = totalYield; }

    public String getPlantId() { return plantId; }
    public void setPlantId(String plantId) { this.plantId = plantId; }

    public String getSourceKey() { return sourceKey; }
    public void setSourceKey(String sourceKey) { this.sourceKey = sourceKey; }

    public LocalDateTime getTimeStampedClean() { return timeStampedClean; }
    public void setTimeStampedClean(LocalDateTime timeStampedClean) { this.timeStampedClean = timeStampedClean; }
}
