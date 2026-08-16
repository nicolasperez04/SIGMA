package com.SIGMA.USCO.Modalities.repository;

import com.SIGMA.USCO.Modalities.entity.AcademicCertificate;
import com.SIGMA.USCO.Modalities.entity.enums.CertificateStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicCertificateRepository extends JpaRepository<AcademicCertificate, Long> {

    Optional<AcademicCertificate> findByStudentModalityId(Long studentModalityId);

    List<AcademicCertificate> findByStudentModalityIdAndStatus(Long studentModalityId, CertificateStatus status);

    Optional<AcademicCertificate> findByCertificateNumber(String certificateNumber);

    boolean existsByStudentModalityId(Long studentModalityId);

    Optional<String> findTopByCertificateNumberStartingWithOrderByCertificateNumberDesc(String prefix);
}

