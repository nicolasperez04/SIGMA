package com.SIGMA.USCO.Modalities.dto.response;

public record AcceptInvitationResponse(boolean success, Long studentModalityId, String message, String modalityName,
                                       long pendingInvitations) {
}