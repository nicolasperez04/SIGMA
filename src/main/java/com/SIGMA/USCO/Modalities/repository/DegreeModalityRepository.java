package com.SIGMA.USCO.Modalities.repository;

import com.SIGMA.USCO.Modalities.entity.DegreeModality;
import com.SIGMA.USCO.Modalities.entity.enums.ModalityStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DegreeModalityRepository extends JpaRepository<DegreeModality, Long> {


    List<DegreeModality> findByStatus(ModalityStatus status);

    boolean existsByNameIgnoreCaseAndFacultyId(String name, Long id);
}
