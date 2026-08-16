package com.SIGMA.USCO.Modalities.repository;

import com.SIGMA.USCO.Modalities.entity.ModalityProcessStatusHistory;
import com.SIGMA.USCO.Modalities.entity.StudentModality;
import com.SIGMA.USCO.Modalities.entity.enums.ModalityProcessStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ModalityProcessStatusHistoryRepository extends JpaRepository<ModalityProcessStatusHistory, Long> {

    List<ModalityProcessStatusHistory> findByStudentModalityIdOrderByChangeDateAsc(Long studentModalityId);


    List<ModalityProcessStatusHistory> findByStudentModalityIdOrderByChangeDateDesc(Long id);

    Optional<ModalityProcessStatusHistory> findTopByStudentModalityAndStatusOrderByChangeDateDesc(StudentModality studentModality, ModalityProcessStatus status);
}
