package com.maxxenergy.data;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/generation")
public class PlantGenerationController {

    private final PlantGenerationRepository repo;

    public PlantGenerationController(PlantGenerationRepository repo) {
        this.repo = repo;
    }

    // ---------- DTOs ----------
    public record SummaryPlant(String plantId, String start, String end, int inverters, int stepMinutes) {}
    public record Summary(List<SummaryPlant> plants, String globalStart, String globalEnd) {}

    public record DailyEnergyPoint(String plantId, String day, double kwh, String method) {}
    public record CumulativePoint(String plantId, String day, double totalKwh) {}
    public record HourlyProfilePoint(int hour, double avgAcKw, double avgDcKw) {}
    public record EfficiencyPoint(String day, Double avgEfficiency) {}
    public record HealthPoint(String day, long intervals, long nonZero, double uptimePct) {}

    // ---------- Helpers ----------
    private static LocalDateTime clamp(LocalDateTime t, LocalDateTime lo, LocalDateTime hi) {
        if (t.isBefore(lo)) return lo;
        if (t.isAfter(hi)) return hi;
        return t;
    }
    private static int defaultStepMinutes() { return 15; }

    private Map<String, PlantGenerationRepository.PlantSummaryRow> summaryMap() {
        return repo.plantSummaries().stream().collect(Collectors.toMap(
                PlantGenerationRepository.PlantSummaryRow::getPlantId, r -> r
        ));
    }

    // ---------- SUMMARY ----------
    @GetMapping("/summary")
    public Summary summary() {
        var rows = repo.plantSummaries();
        LocalDateTime gStart = rows.stream().map(PlantGenerationRepository.PlantSummaryRow::getStartDate).min(LocalDateTime::compareTo).orElse(null);
        LocalDateTime gEnd   = rows.stream().map(PlantGenerationRepository.PlantSummaryRow::getEndDate).max(LocalDateTime::compareTo).orElse(null);

        // Use fixed 15-min unless you want to compute median across diffs (not necessary here)
        int step = defaultStepMinutes();

        var list = rows.stream()
                .map(r -> new SummaryPlant(r.getPlantId(),
                        r.getStartDate().toString(),
                        r.getEndDate().toString(),
                        Optional.ofNullable(r.getInverterCount()).orElse(0),
                        step))
                .toList();

        return new Summary(list, gStart == null ? null : gStart.toString(),
                gEnd == null ? null : gEnd.toString());
    }

    // ---------- DAILY ENERGY ----------
    @GetMapping("/daily-energy")
    public List<DailyEnergyPoint> dailyEnergy(
            @RequestParam(required = false) String plantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        var bounds = summaryMap();
        if (plantId != null && bounds.containsKey(plantId)) {
            var b = bounds.get(plantId);
            from = clamp(from, b.getStartDate(), b.getEndDate());
            to   = clamp(to,   b.getStartDate(), b.getEndDate());
        }
        var rows = repo.dailyEnergy(plantId, from, to);
        return rows.stream()
                .map(r -> new DailyEnergyPoint(r.getPlantId(), r.getDay().toString(),
                        Optional.ofNullable(r.getKwh()).orElse(0d), "daily_yield|max"))
                .toList();
    }

    // ---------- CUMULATIVE ----------
    @GetMapping("/cumulative")
    public List<CumulativePoint> cumulative(
            @RequestParam(required = false) String plantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        var bounds = summaryMap();
        if (plantId != null && bounds.containsKey(plantId)) {
            var b = bounds.get(plantId);
            from = clamp(from, b.getStartDate(), b.getEndDate());
            to   = clamp(to,   b.getStartDate(), b.getEndDate());
        }
        return repo.cumulative(plantId, from, to).stream()
                .map(r -> new CumulativePoint(r.getPlantId(), r.getDay().toString(),
                        Optional.ofNullable(r.getTotalKwh()).orElse(0d)))
                .toList();
    }

    // ---------- HOURLY PROFILE ----------
    @GetMapping("/hourly-profile")
    public List<HourlyProfilePoint> hourlyProfile(
            @RequestParam(required = false) String plantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return repo.hourlyProfile(plantId, from, to).stream()
                .map(r -> new HourlyProfilePoint(
                        Optional.ofNullable(r.getHourOfDay()).orElse(0),
                        Optional.ofNullable(r.getAvgAcKw()).orElse(0d),
                        Optional.ofNullable(r.getAvgDcKw()).orElse(0d)))
                .toList();
    }

    // ---------- EFFICIENCY ----------
    @GetMapping("/efficiency")
    public List<EfficiencyPoint> efficiency(
            @RequestParam(required = false) String plantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return repo.efficiencyByDay(plantId, from, to).stream()
                .map(r -> new EfficiencyPoint(r.getDay().toString(), r.getAvgEfficiency()))
                .toList();
    }

    // ---------- HEALTH ----------
    @GetMapping("/health")
    public List<HealthPoint> health(
            @RequestParam(required = false) String plantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return repo.health(plantId, from, to).stream()
                .map(r -> {
                    long total = Optional.ofNullable(r.getIntervals()).orElse(0L);
                    long nz    = Optional.ofNullable(r.getNonZero()).orElse(0L);
                    double pct = total == 0 ? 0 : (100.0 * nz / total);
                    return new HealthPoint(r.getDay().toString(), total, nz, pct);
                }).toList();
    }

    // ---------- Your existing endpoints (kept) ----------

    @GetMapping("/latest")
    public List<GenerationPoint> latest(@RequestParam(defaultValue = "200") int limit) {
        var page = PageRequest.of(0, Math.min(limit, 5000), Sort.by(Sort.Direction.DESC, "timeStampedClean"));
        var rowsDesc = repo.findByOrderByTimeStampedCleanDesc(page);
        Collections.reverse(rowsDesc);
        return rowsDesc.stream().map(this::toPoint).toList();
    }

    @GetMapping("/range")
    public List<GenerationPoint> range(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String plantId) {

        var rows = (plantId == null || plantId.isBlank())
                ? repo.findByTimeStampedCleanBetweenOrderByTimeStampedCleanAsc(from, to)
                : repo.findByPlantIdAndTimeStampedCleanBetweenOrderByTimeStampedCleanAsc(plantId, from, to);
        return rows.stream().map(this::toPoint).toList();
    }

    // Zero-filled, clamped to plant bounds
    @GetMapping("/range-sampled")
    public List<GenerationPoint> rangeSampled(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "15") int stepMinutes,
            @RequestParam(required = false) String plantId) {

        if (stepMinutes <= 0) stepMinutes = defaultStepMinutes();

        var bounds = summaryMap();
        if (plantId != null && bounds.containsKey(plantId)) {
            var b = bounds.get(plantId);
            from = clamp(from, b.getStartDate(), b.getEndDate());
            to   = clamp(to,   b.getStartDate(), b.getEndDate());
        }

        var rows = (plantId == null || plantId.isBlank())
                ? repo.findByTimeStampedCleanBetweenOrderByTimeStampedCleanAsc(from, to)
                : repo.findByPlantIdAndTimeStampedCleanBetweenOrderByTimeStampedCleanAsc(plantId, from, to);

        Map<LocalDateTime, PlantGeneration> lastInBucket = new HashMap<>();
        for (var g : rows) {
            var t = g.getTimeStampedClean();
            var bucket = t.withMinute((t.getMinute() / stepMinutes) * stepMinutes).withSecond(0).withNano(0);
            lastInBucket.put(bucket, g);
        }

        List<GenerationPoint> out = new ArrayList<>();
        LocalDateTime t = from.withSecond(0).withNano(0);
        while (!t.isAfter(to)) {
            var g = lastInBucket.get(t);
            if (g == null) {
                out.add(new GenerationPoint(t.toString(), 0d, 0d, 0d, 0d, plantId));
            } else {
                out.add(new GenerationPoint(t.toString(),
                        Optional.ofNullable(g.getAcPower()).orElse(0d),
                        Optional.ofNullable(g.getDcPower()).orElse(0d),
                        Optional.ofNullable(g.getDailyYield()).orElse(0d),
                        Optional.ofNullable(g.getTotalYield()).orElse(0d),
                        g.getPlantId()));
            }
            t = t.plusMinutes(stepMinutes);
        }
        return out;
    }

    private GenerationPoint toPoint(PlantGeneration g) {
        return new GenerationPoint(
                g.getTimeStampedClean() == null ? null : g.getTimeStampedClean().toString(),
                g.getAcPower() == null ? 0 : g.getAcPower(),
                g.getDcPower() == null ? 0 : g.getDcPower(),
                g.getDailyYield() == null ? 0 : g.getDailyYield(),
                g.getTotalYield() == null ? 0 : g.getTotalYield(),
                g.getPlantId()
        );
    }
}
