package com.maxxenergy.rates;

import com.maxxenergy.rates.PriceRate.DayType;
import com.maxxenergy.rates.PriceRate.Plan;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

record PriceRateDto(String periodName, String start, String end, BigDecimal rate) {
    static PriceRateDto from(PriceRate r) {
        return new PriceRateDto(
                r.getPeriodName(),
                r.getStartTime() == null ? null : r.getStartTime().toString(),
                r.getEndTime()   == null ? null : r.getEndTime().toString(),
                r.getRatePerKwh()
        );
    }
}

@RestController
@RequestMapping("/api/rates")
public class PriceRateController {

    private final PriceRateRepository repo;
    public PriceRateController(PriceRateRepository repo) { this.repo = repo; }

    // e.g. /api/rates?plan=TOU&dayType=WEEKDAY
    @GetMapping
    public List<PriceRateDto> byPlanAndDay(
            @RequestParam Plan plan,
            @RequestParam DayType dayType) {
        return repo.findByPlanAndDayTypeOrderByStartTimeAsc(plan, dayType)
                .stream().map(PriceRateDto::from).toList();
    }

    // e.g. /api/rates/plan/TOU  -> { "WEEKDAY": [...], "WEEKEND":[...], "ALL":[...] }
    @GetMapping("/plan/{plan}")
    public Map<DayType, List<PriceRateDto>> byPlan(@PathVariable Plan plan) {
        return repo.findByPlanOrderByDayTypeAscStartTimeAsc(plan)
                .stream()
                .collect(Collectors.groupingBy(
                        PriceRate::getDayType,
                        LinkedHashMap::new,
                        Collectors.mapping(PriceRateDto::from, Collectors.toList())));
    }
}
