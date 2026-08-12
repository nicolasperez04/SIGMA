package com.SIGMA.USCO.Modalities.Repository;

import com.SIGMA.USCO.Modalities.Entity.ExaminerCertificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExaminerCertificateRepository extends JpaRepository<ExaminerCertificate, Long> {

    /**
     * Busca certificados de un jurado en una modalidad específica
     */
    @Query("SELECT ec FROM ExaminerCertificate ec " +
           "WHERE ec.studentModality.id = :modalityId AND ec.examiner.id = :examinerId")
    Optional<ExaminerCertificate> findByModalityAndExaminer(
            @Param("modalityId") Long modalityId,
            @Param("examinerId") Long examinerId
    );

    /**
     * Busca todos los certificados de jurados para una modalidad
     */
    @Query("SELECT ec FROM ExaminerCertificate ec WHERE ec.studentModality.id = :modalityId")
    List<ExaminerCertificate> findByStudentModalityId(@Param("modalityId") Long modalityId);

    /**
     * Busca todos los certificados de un jurado
     */
    @Query("SELECT ec FROM ExaminerCertificate ec WHERE ec.examiner.id = :examinerId")
    List<ExaminerCertificate> findByExaminerId(@Param("examinerId") Long examinerId);

    Optional<String> findTopByCertificateNumberStartingWithOrderByCertificateNumberDesc(String prefix);
}

