package com.SIGMA.USCO.Modalities.service;

import com.SIGMA.USCO.Modalities.entity.ModalityProcessStatusHistory;
import com.SIGMA.USCO.Modalities.entity.StudentModality;
import com.SIGMA.USCO.Modalities.entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.repository.ModalityProcessStatusHistoryRepository;
import com.SIGMA.USCO.Modalities.repository.StudentModalityRepository;
import com.SIGMA.USCO.Users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ModalityStatusTransition {

    private final ModalityProcessStatusHistoryRepository historyRepository;
    private final StudentModalityRepository modalityRepository;

    @Transactional
    public StudentModality transition(StudentModality modality, ModalityProcessStatus newStatus, User responsible, String observations) {
        LocalDateTime now = LocalDateTime.now();
        modality.setStatus(newStatus);
        modality.setUpdatedAt(now);
        historyRepository.save(ModalityProcessStatusHistory.builder()
                .studentModality(modality)
                .status(newStatus)
                .changeDate(now)
                .responsible(responsible)
                .observations(observations)
                .build());
        return modalityRepository.save(modality);
    }

    @Transactional
    public void recordHistory(StudentModality modality, ModalityProcessStatus status, User responsible, String observations) {
        historyRepository.save(ModalityProcessStatusHistory.builder()
                .studentModality(modality)
                .status(status)
                .changeDate(LocalDateTime.now())
                .responsible(responsible)
                .observations(observations)
                .build());
    }
}