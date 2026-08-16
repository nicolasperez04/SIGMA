package com.SIGMA.USCO.academic.repository;

import com.SIGMA.USCO.academic.entity.AcademicHistoryPdf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AcademicHistoryPdfRepository extends JpaRepository<AcademicHistoryPdf, Long> {
}



