package com.SIGMA.USCO.Modalities.Repository;

import com.SIGMA.USCO.Modalities.Entity.AcademicCertificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AcademicCertificateRepository extends JpaRepository<AcademicCertificate, Long> {

    Optional<AcademicCertificate> findByStudentModalityId(Long studentModalityId);

    Optional<AcademicCertificate> findByCertificateNumber(String certificateNumber);

    boolean existsByStudentModalityId(Long studentModalityId);

    Optional<String> findTopByCertificateNumberStartingWithOrderByCertificateNumberDesc(String prefix);
}

