package com.SIGMA.USCO.notifications.repository;

import com.SIGMA.USCO.notifications.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @EntityGraph(attributePaths = {"studentModality"})
    List<Notification> findByRecipient_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByRecipient_IdAndReadFalse(Long recipientId);

    Optional<Notification> findByIdAndRecipient_Id(Long id, Long recipientId);

    // ponytail: EntityGraph para que el retry use asociaciones inicializadas (recipient/studentModality
    // son LAZY y la entidad llega detached al scheduler -> sin esto, LazyInitializationException)
    @EntityGraph(attributePaths = {"recipient", "studentModality"})
    List<Notification> findByEmailSentFalseAndDeliveryAttemptsLessThan(int maxAttempts);

}
