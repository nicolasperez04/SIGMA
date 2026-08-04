package com.SIGMA.USCO.notifications.event;

import com.SIGMA.USCO.notifications.entity.enums.NotificationType;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
public class ModalityEvent {

    public static final String KEY_STUDENT_DOCUMENT_ID = "studentDocumentId";
    public static final String KEY_STUDENT_ID = "studentId";
    public static final String KEY_DOCUMENT_ID = "documentId";
    public static final String KEY_DOCUMENT_NAME = "documentName";
    public static final String KEY_OBSERVATIONS = "observations";
    public static final String KEY_REASON = "reason";
    public static final String KEY_REQUESTED_BY = "requestedBy";
    public static final String KEY_DIRECTOR_ID = "directorId";
    public static final String KEY_PREVIOUS_DIRECTOR_ID = "previousDirectorId";
    public static final String KEY_NEW_DIRECTOR_ID = "newDirectorId";
    public static final String KEY_DEFENSE_DATE = "defenseDate";
    public static final String KEY_DEFENSE_LOCATION = "defenseLocation";
    public static final String KEY_FINAL_STATUS = "finalStatus";
    public static final String KEY_ACADEMIC_DISTINCTION = "academicDistinction";
    public static final String KEY_EXAMINER_ID = "examinerId";
    public static final String KEY_COMMITTEE_MEMBER_ID = "committeeMemberId";
    public static final String KEY_DEADLINE = "deadline";
    public static final String KEY_DAYS_REMAINING = "daysRemaining";
    public static final String KEY_REQUEST_DATE = "requestDate";
    public static final String KEY_APPROVED = "approved";
    public static final String KEY_RESOLUTION_NOTES = "resolutionNotes";
    public static final String KEY_EDIT_REQUEST_ID = "editRequestId";
    public static final String KEY_CERTIFICATE_ID = "certificateId";
    public static final String KEY_PROJECT_DIRECTOR_ID = "projectDirectorId";
    public static final String KEY_RECIPIENT_EMAIL = "recipientEmail";
    public static final String KEY_RECIPIENT_NAME = "recipientName";
    public static final String KEY_SEMINAR_NAME = "seminarName";
    public static final String KEY_START_DATE = "startDate";
    public static final String KEY_TOTAL_HOURS = "totalHours";
    public static final String KEY_PROGRAM_NAME = "programName";
    public static final String KEY_CANCELLED_DATE = "cancelledDate";
    public static final String KEY_INVITATION_ID = "invitationId";
    public static final String KEY_INVITEE_ID = "inviteeId";
    public static final String KEY_INVITER_ID = "inviterId";
    public static final String KEY_INVITER_NAME = "inviterName";
    public static final String KEY_ACCEPTED_BY_ID = "acceptedById";
    public static final String KEY_ACCEPTED_BY_NAME = "acceptedByName";
    public static final String KEY_REJECTED_BY_ID = "rejectedById";
    public static final String KEY_REJECTED_BY_NAME = "rejectedByName";
    public static final String KEY_LEADER_ID = "leaderId";
    public static final String KEY_MODALITY_NAME = "modalityName";

    private final NotificationType type;
    private final Long studentModalityId;
    private final Long actorUserId;
    private final LocalDateTime occurredAt;
    private final Map<String, Object> payload;

    public ModalityEvent(NotificationType type, Long studentModalityId,
                         Long actorUserId, Map<String, Object> payload) {
        this.type = type;
        this.studentModalityId = studentModalityId;
        this.actorUserId = actorUserId;
        this.payload = payload != null ? Map.copyOf(payload) : Map.of();
        this.occurredAt = LocalDateTime.now();
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        return (T) payload.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type, T defaultValue) {
        return payload.containsKey(key) ? (T) payload.get(key) : defaultValue;
    }
}
