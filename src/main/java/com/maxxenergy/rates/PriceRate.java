package com.maxxenergy.rates;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(name = "price_rates")
public class PriceRate {

    public enum Plan { TOU, STABLE }
    public enum DayType { WEEKDAY, WEEKEND, ALL }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Plan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayType dayType;

    @Column(nullable = false, length = 32)
    private String periodName; // Off-Peak, Mid-Peak, Peak, Flat

    @Column(columnDefinition = "TIME")
    private LocalTime startTime; // can be null for STABLE

    @Column(columnDefinition = "TIME")
    private LocalTime endTime;   // can be null for STABLE

    @Column(nullable = false, precision = 6, scale = 4)
    private BigDecimal ratePerKwh;

    // getters & setters
    public Long getId() { return id; }
    public Plan getPlan() { return plan; }
    public void setPlan(Plan plan) { this.plan = plan; }
    public DayType getDayType() { return dayType; }
    public void setDayType(DayType dayType) { this.dayType = dayType; }
    public String getPeriodName() { return periodName; }
    public void setPeriodName(String periodName) { this.periodName = periodName; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public BigDecimal getRatePerKwh() { return ratePerKwh; }
    public void setRatePerKwh(BigDecimal ratePerKwh) { this.ratePerKwh = ratePerKwh; }
}
