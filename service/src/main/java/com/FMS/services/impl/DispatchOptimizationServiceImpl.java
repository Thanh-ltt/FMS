package com.FMS.services.impl;

import com.FMS.dto.VehicleMatchDto;
import com.FMS.entity.Vehicle;
import com.FMS.enums.VehicleStatus;
import com.FMS.repositories.VehicleRepository;
import com.FMS.services.DispatchOptimizationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class DispatchOptimizationServiceImpl implements DispatchOptimizationService {

    VehicleRepository vehicleRepository;

    @Override
    public List<VehicleMatchDto> suggestVehiclesForTrip(Double cargoWeightTon, String startLocation) {
        List<Vehicle> availableVehicles = vehicleRepository.findByStatus(VehicleStatus.AVAILABLE);

        double requiredCapacity = (cargoWeightTon != null && cargoWeightTon > 0) ? cargoWeightTon : 1.0;

        List<VehicleMatchDto> matches = new ArrayList<>();

        for (Vehicle v : availableVehicles) {
            if (v.getCapacity() == null || v.getCapacity() < requiredCapacity) {
                continue; // Capacity too small
            }

            // Calculate capacity utilization percentage
            double utilization = (requiredCapacity / v.getCapacity()) * 100.0;
            double score = 100.0 - Math.abs(100.0 - utilization) * 0.5;
            if (score < 40.0) score = 40.0;

            String reason;
            if (utilization >= 70.0 && utilization <= 100.0) {
                reason = String.format("Tải trọng xe (%.1f tấn) tối ưu nhất cho hàng hóa (%.1f tấn), hiệu suất %.0f%%", v.getCapacity(), requiredCapacity, utilization);
            } else if (utilization < 70.0) {
                reason = String.format("Xe sẵn sàng (%.1f tấn), dư tải trọng so với nhu cầu (%.1f tấn)", v.getCapacity(), requiredCapacity);
            } else {
                reason = String.format("Phù hợp tải trọng (%.1f tấn)", v.getCapacity());
            }

            matches.add(VehicleMatchDto.builder()
                    .vehicleId(v.getId())
                    .licensePlate(v.getLicensePlate())
                    .vehicleType(v.getVehicleType())
                    .capacity(v.getCapacity())
                    .matchScore(Math.round(score * 10.0) / 10.0)
                    .recommendationReason(reason)
                    .build());
        }

        matches.sort(Comparator.comparingDouble(VehicleMatchDto::getMatchScore).reversed());
        return matches;
    }
}
