package com.maxxenergy.rates;

import com.maxxenergy.rates.PriceRate.DayType;
import com.maxxenergy.rates.PriceRate.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceRateRepository extends JpaRepository<PriceRate, Long> {

    List<PriceRate> findByPlanAndDayTypeOrderByStartTimeAsc(Plan plan, DayType dayType);

    List<PriceRate> findByPlanOrderByDayTypeAscStartTimeAsc(Plan plan);
}
