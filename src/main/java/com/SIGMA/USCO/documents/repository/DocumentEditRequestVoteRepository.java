package com.SIGMA.USCO.documents.repository;

import com.SIGMA.USCO.documents.entity.DocumentEditRequestVote;
import com.SIGMA.USCO.documents.entity.enums.EditRequestVoteDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentEditRequestVoteRepository extends JpaRepository<DocumentEditRequestVote, Long> {

    List<DocumentEditRequestVote> findByEditRequestId(Long editRequestId);

    /** Batch de votos por solicitudes (T5.5): ORDER BY id preserva el orden del query por-solicitud */
    @Query("SELECT v FROM DocumentEditRequestVote v WHERE v.editRequest.id IN :ids ORDER BY v.id ASC")
    List<DocumentEditRequestVote> findByEditRequestIdIn(@Param("ids") Collection<Long> ids);

    Optional<DocumentEditRequestVote> findByEditRequestIdAndExaminerId(Long editRequestId, Long examinerId);

    boolean existsByEditRequestIdAndExaminerId(Long editRequestId, Long examinerId);

    long countByEditRequestIdAndDecision(Long editRequestId, EditRequestVoteDecision decision);
}

