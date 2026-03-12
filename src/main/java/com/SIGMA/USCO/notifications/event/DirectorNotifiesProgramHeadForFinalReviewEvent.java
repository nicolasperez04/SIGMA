package com.SIGMA.USCO.notifications.event;

import com.SIGMA.USCO.notifications.entity.enums.NotificationType;

/**
 * Evento publicado cuando el director de proyecto notifica a jefatura de programa
 * que los documentos finales están listos para revisión, como paso previo a la
 * notificación a los jurados evaluadores.
 */
public class DirectorNotifiesProgramHeadForFinalReviewEvent extends DomainEvent {

    public DirectorNotifiesProgramHeadForFinalReviewEvent() {
        super(NotificationType.DIRECTOR_NOTIFIES_PROGRAM_HEAD_FINAL_REVIEW, null, null);
    }

    public DirectorNotifiesProgramHeadForFinalReviewEvent(Long studentModalityId, Long actorUserId) {
        super(NotificationType.DIRECTOR_NOTIFIES_PROGRAM_HEAD_FINAL_REVIEW, studentModalityId, actorUserId);
    }
}

