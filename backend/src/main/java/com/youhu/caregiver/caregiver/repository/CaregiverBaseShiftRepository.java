package com.youhu.caregiver.caregiver.repository;

import com.youhu.caregiver.caregiver.domain.CaregiverBaseShift;
import com.youhu.caregiver.common.domain.RecordStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaregiverBaseShiftRepository extends JpaRepository<CaregiverBaseShift, Long> {

    List<CaregiverBaseShift> findByCaregiverIdOrderByStartTimeAsc(Long caregiverId);

    List<CaregiverBaseShift> findByCaregiverIdAndStatusOrderByStartTimeAsc(
            Long caregiverId, RecordStatus status);
}
