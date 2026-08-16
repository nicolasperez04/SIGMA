package com.SIGMA.USCO.Modalities.repository;

import com.SIGMA.USCO.Modalities.entity.ModalityRequirements;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ModalityRequirementsRepository extends JpaRepository<ModalityRequirements, Long> {

    List<ModalityRequirements> findByModalityId(Long modalityId);

    List<ModalityRequirements> findByModalityIdIn(List<Long> modalityIds);

    List<ModalityRequirements> findByModalityIdAndActiveTrue(Long modalityId);

    List<ModalityRequirements> findByModalityIdAndActive(Long modalityId, boolean active);
}
