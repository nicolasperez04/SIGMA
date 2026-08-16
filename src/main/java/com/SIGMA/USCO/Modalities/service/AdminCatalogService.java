package com.SIGMA.USCO.Modalities.service;

import com.SIGMA.USCO.Modalities.entity.DegreeModality;
import com.SIGMA.USCO.Modalities.entity.ModalityRequirements;
import com.SIGMA.USCO.Modalities.entity.enums.ModalityStatus;
import com.SIGMA.USCO.Modalities.repository.DegreeModalityRepository;
import com.SIGMA.USCO.Modalities.repository.ModalityRequirementsRepository;
import com.SIGMA.USCO.Modalities.dto.ModalityDTO;
import com.SIGMA.USCO.Modalities.dto.RequirementDTO;
import com.SIGMA.USCO.documents.dto.RequiredDocumentDTO;
import com.SIGMA.USCO.documents.entity.RequiredDocument;
import com.SIGMA.USCO.documents.repository.RequiredDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

        List<Long> modalityIds = modalities.stream().map(DegreeModality::getId).toList();

        Map<Long, List<ModalityRequirements>> requirementsByModality =
                modalityRequirementsRepository.findByModalityIdIn(modalityIds).stream()
                        .collect(Collectors.groupingBy(r -> r.getModality().getId()));

        Map<Long, List<RequiredDocument>> documentsByModality =
                requiredDocumentRepository.findByModalityIdIn(modalityIds).stream()
                        .collect(Collectors.groupingBy(d -> d.getModality().getId()));

        List<ModalityDTO> response = modalities.stream()
                .map(mod -> ModalityDTO.builder()
                        .id(mod.getId())
                        .name(mod.getName())
                        .description(mod.getDescription())
                        .status(mod.getStatus())


                        .facultyId(mod.getFaculty().getId())
                        .facultyName(mod.getFaculty().getName())


                        .requirements(
                                requirementsByModality.getOrDefault(mod.getId(), List.of())
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
                                documentsByModality.getOrDefault(mod.getId(), List.of())
                                        .stream()
                                        .map(RequiredDocumentDTO::from)
                                        .toList()
                        )

                        .build()
                )
                .toList();

        return response;
    }

}
