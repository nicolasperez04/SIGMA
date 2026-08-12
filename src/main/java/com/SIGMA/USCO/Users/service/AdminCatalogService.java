package com.SIGMA.USCO.Users.service;

import com.SIGMA.USCO.Modalities.Entity.DegreeModality;
import com.SIGMA.USCO.Modalities.Entity.enums.ModalityStatus;
import com.SIGMA.USCO.Modalities.Repository.DegreeModalityRepository;
import com.SIGMA.USCO.Modalities.Repository.ModalityRequirementsRepository;
import com.SIGMA.USCO.Modalities.dto.ModalityDTO;
import com.SIGMA.USCO.Modalities.dto.RequirementDTO;
import com.SIGMA.USCO.documents.dto.RequiredDocumentDTO;
import com.SIGMA.USCO.documents.repository.RequiredDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminCatalogService {

    private final DegreeModalityRepository degreeModalityRepository;
    private final ModalityRequirementsRepository modalityRequirementsRepository;
    private final RequiredDocumentRepository requiredDocumentRepository;

    @Transactional(readOnly = true)
    public List<ModalityDTO> getModalities(ModalityStatus status) {

        List<DegreeModality> modalities;

        if (status != null) {
            modalities = degreeModalityRepository.findByStatus(status);
        } else {
            modalities = degreeModalityRepository.findAll();
        }

        List<ModalityDTO> response = modalities.stream()
                .map(mod -> ModalityDTO.builder()
                        .id(mod.getId())
                        .name(mod.getName())
                        .description(mod.getDescription())
                        .status(mod.getStatus())


                        .facultyId(mod.getFaculty().getId())
                        .facultyName(mod.getFaculty().getName())


                        .requirements(
                                modalityRequirementsRepository.findByModalityId(mod.getId())
                                        .stream()
                                        .map(req -> RequirementDTO.builder()
                                                .id(req.getId())
                                                .requirementName(req.getRequirementName())
                                                .description(req.getDescription())
                                                .ruleType(req.getRuleType())
                                                .expectedValue(req.getExpectedValue())
                                                .active(req.isActive())
                                                .build())
                                        .toList()
                        )


                        .documents(
                                requiredDocumentRepository.findByModalityId(mod.getId())
                                        .stream()
                                        .map(doc -> RequiredDocumentDTO.builder()
                                                .id(doc.getId())
                                                .modalityId( doc.getModality().getId())
                                                .documentName(doc.getDocumentName())
                                                .description(doc.getDescription())
                                                .allowedFormat(doc.getAllowedFormat())
                                                .maxFileSizeMB(doc.getMaxFileSizeMB())
                                                .documentType(doc.getDocumentType())
                                                .active(doc.isActive())
                                                .requiresProposalEvaluation(doc.isRequiresProposalEvaluation())
                                                .build())
                                        .toList()
                        )

                        .build()
                )
                .toList();

        return response;
    }

}