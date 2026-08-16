package com.SIGMA.USCO.Modalities.dto.response;

import com.SIGMA.USCO.Modalities.dto.DefenseProposalDTO;

import java.util.List;

public record PendingDefenseProposalsResponse(boolean success, int totalProposals,
                                              List<DefenseProposalDTO> proposals) {
}