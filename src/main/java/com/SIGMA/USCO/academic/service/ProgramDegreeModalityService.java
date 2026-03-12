package com.SIGMA.USCO.academic.service;

import com.SIGMA.USCO.Modalities.Entity.DegreeModality;
import com.SIGMA.USCO.Modalities.Repository.DegreeModalityRepository;
import com.SIGMA.USCO.academic.dto.ProgramDegreeModalityDTO;
import com.SIGMA.USCO.academic.dto.ProgramDegreeModalityRequest;
import com.SIGMA.USCO.academic.entity.AcademicProgram;
import com.SIGMA.USCO.academic.entity.ProgramDegreeModality;
import com.SIGMA.USCO.academic.repository.AcademicProgramRepository;
import com.SIGMA.USCO.academic.repository.ProgramDegreeModalityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProgramDegreeModalityService {

    private final ProgramDegreeModalityRepository programDegreeModalityRepository;
    private final AcademicProgramRepository academicProgramRepository;
    private final DegreeModalityRepository degreeModalityRepository;


    @Transactional
    public ProgramDegreeModalityDTO createProgramModality(ProgramDegreeModalityRequest request) {

        if (request.getAcademicProgramId() == null ||
                request.getDegreeModalityId() == null ||
                request.getCreditsRequired() == null) {
            throw new IllegalArgumentException("Programa, modalidad y créditos son obligatorios.");
        }

        if (request.getCreditsRequired() <= 0) {
            throw new IllegalArgumentException(
                    "Los créditos requeridos deben ser mayores a cero."
            );
        }

        AcademicProgram program = academicProgramRepository.findById(request.getAcademicProgramId()).orElseThrow(() ->
                        new IllegalArgumentException("El programa académico no existe."));

        DegreeModality modality = degreeModalityRepository.findById(request.getDegreeModalityId()).orElseThrow(() ->
                        new IllegalArgumentException("La modalidad no existe."));


        if (!program.getFaculty().getId().equals(modality.getFaculty().getId())) {
            throw new IllegalArgumentException("La modalidad no pertenece a la facultad del programa.");
        }


        if (programDegreeModalityRepository.existsByAcademicProgramIdAndDegreeModalityId(program.getId(), modality.getId())) {
            throw new IllegalArgumentException("La modalidad ya está configurada para este programa.");
        }

        ProgramDegreeModality programModality = ProgramDegreeModality.builder()
                        .academicProgram(program)
                        .degreeModality(modality)
                        .creditsRequired(request.getCreditsRequired())
                        .active(true)
                        .requiresDefenseProcess(request.isRequiresDefenseProcess())
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

        ProgramDegreeModality saved = programDegreeModalityRepository.save(programModality);
        return mapToDTO(saved);
    }


    @Transactional(readOnly = true)
    public List<ProgramDegreeModalityDTO> getAllProgramModalities(Boolean active, Long degreeModalityId, Long facultyId, Long academicProgramId) {
        return programDegreeModalityRepository.findByFilters(active, degreeModalityId, facultyId, academicProgramId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProgramDegreeModalityDTO getProgramModalityById(Long id) {
        ProgramDegreeModality programModality = programDegreeModalityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("La configuración de modalidad no existe."));
        return mapToDTO(programModality);
    }





    @Transactional
    public ProgramDegreeModalityDTO updateProgramModality(Long id, ProgramDegreeModalityRequest request) {
        ProgramDegreeModality programModality = programDegreeModalityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("La configuración de modalidad no existe."));


        if (request.getCreditsRequired() != null) {
            if (request.getCreditsRequired() <= 0) {
                throw new IllegalArgumentException("Los créditos requeridos deben ser mayores a cero.");
            }
            programModality.setCreditsRequired(request.getCreditsRequired());
        }

        // Actualizar requiresDefenseProcess siempre que venga en el request
        programModality.setRequiresDefenseProcess(request.isRequiresDefenseProcess());


        if (request.getAcademicProgramId() != null || request.getDegreeModalityId() != null) {
            Long newProgramId = request.getAcademicProgramId() != null
                    ? request.getAcademicProgramId()
                    : programModality.getAcademicProgram().getId();

            Long newModalityId = request.getDegreeModalityId() != null
                    ? request.getDegreeModalityId()
                    : programModality.getDegreeModality().getId();


            if (!newProgramId.equals(programModality.getAcademicProgram().getId()) ||
                !newModalityId.equals(programModality.getDegreeModality().getId())) {

                if (programDegreeModalityRepository.existsByAcademicProgramIdAndDegreeModalityId(newProgramId, newModalityId)) {
                    throw new IllegalArgumentException("Ya existe una configuración con este programa y modalidad.");
                }
            }

            if (request.getAcademicProgramId() != null) {
                AcademicProgram newProgram = academicProgramRepository.findById(request.getAcademicProgramId())
                        .orElseThrow(() -> new IllegalArgumentException("El programa académico no existe."));
                programModality.setAcademicProgram(newProgram);
            }

            if (request.getDegreeModalityId() != null) {
                DegreeModality newModality = degreeModalityRepository.findById(request.getDegreeModalityId())
                        .orElseThrow(() -> new IllegalArgumentException("La modalidad no existe."));


                if (!programModality.getAcademicProgram().getFaculty().getId().equals(newModality.getFaculty().getId())) {
                    throw new IllegalArgumentException("La modalidad no pertenece a la facultad del programa.");
                }

                programModality.setDegreeModality(newModality);
            }
        }

        programModality.setUpdatedAt(LocalDateTime.now());
        ProgramDegreeModality updated = programDegreeModalityRepository.save(programModality);
        return mapToDTO(updated);
    }


    @Transactional
    public void deactivateProgramModality(Long id) {
        ProgramDegreeModality programModality = programDegreeModalityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("La configuración de modalidad no existe."));

        programModality.setActive(false);
        programModality.setUpdatedAt(LocalDateTime.now());
        programDegreeModalityRepository.save(programModality);
    }


    @Transactional
    public void activateProgramModality(Long id) {
        ProgramDegreeModality programModality = programDegreeModalityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("La configuración de modalidad no existe."));

        programModality.setActive(true);
        programModality.setUpdatedAt(LocalDateTime.now());
        programDegreeModalityRepository.save(programModality);
    }




    private ProgramDegreeModalityDTO mapToDTO(ProgramDegreeModality entity) {
        return ProgramDegreeModalityDTO.builder()
                .id(entity.getId())
                .academicProgramId(entity.getAcademicProgram().getId())
                .academicProgramName(entity.getAcademicProgram().getName())
                .facultyName(entity.getAcademicProgram().getFaculty().getName())
                .degreeModalityId(entity.getDegreeModality().getId())
                .degreeModalityName(entity.getDegreeModality().getName())
                .degreeModalityDescription(entity.getDegreeModality().getDescription())
                .creditsRequired(entity.getCreditsRequired())
                .active(entity.isActive())
                .requiresDefenseProcess(entity.isRequiresDefenseProcess())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

}
