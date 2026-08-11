package com.SIGMA.USCO.academic.service;


import com.SIGMA.USCO.academic.dto.ProgramDTO;
import com.SIGMA.USCO.academic.entity.AcademicProgram;
import com.SIGMA.USCO.academic.entity.Faculty;
import com.SIGMA.USCO.academic.repository.AcademicProgramRepository;
import com.SIGMA.USCO.academic.repository.FacultyRepository;
import com.SIGMA.USCO.common.exception.ConflictException;
import com.SIGMA.USCO.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AcademicProgramService {

    private final AcademicProgramRepository academicProgramRepository;
    private final FacultyRepository facultyRepository;

    @Transactional
    public ProgramDTO createProgram(ProgramDTO request) {

        Faculty faculty = facultyRepository.findById(request.getFacultyId())
                .orElseThrow(() -> new NotFoundException("La facultad no existe."));

        if (academicProgramRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ConflictException("El nombre del programa ya existe.");
        }

        if (academicProgramRepository.existsByCodeIgnoreCase(request.getCode())) {
            throw new ConflictException("El código del programa ya existe.");
        }

        AcademicProgram program = AcademicProgram.builder()
                .name(request.getName())
                .code(request.getCode().toUpperCase())
                .description(request.getDescription())
                .faculty(faculty)
                .totalCredits(request.getTotalCredits() != null ? request.getTotalCredits() : 0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .active(true)
                .build();

         academicProgramRepository.save(program);

        return toProgramDTO(program);
    }

    @Transactional(readOnly = true)
    public ProgramDTO getProgramById(Long programId) {

        AcademicProgram program = academicProgramRepository.findById(programId)
                .orElseThrow(() -> new NotFoundException("Programa académico no encontrado."));

        return toProgramDTO(program);

    }

    @Transactional(readOnly = true)
    public List<ProgramDTO> getAllPrograms() {
        List<AcademicProgram> programs = academicProgramRepository.findAll();

        return programs.stream().map(this::toProgramDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<ProgramDTO> getActivePrograms() {

        return facultyRepository.findByActiveTrue()
                .stream()
                .flatMap(faculty -> academicProgramRepository.findByFaculty_IdAndActiveTrue(faculty.getId()).stream())
                .map(this::toProgramDTO)
                .toList();

    }
    @Transactional
    public ProgramDTO updateProgram(Long programId, ProgramDTO request) {

        AcademicProgram program = academicProgramRepository.findById(programId)
                .orElseThrow(() -> new NotFoundException("Programa académico no encontrado."));

        if (request.getName() != null && !request.getName().isBlank()) {

            boolean exists =
                    academicProgramRepository.existsByNameIgnoreCaseAndIdNot(
                            request.getName(),
                            programId
                    );

            if (exists) {
                throw new ConflictException("Ya existe un programa con ese nombre.");
            }

            program.setName(request.getName().toUpperCase());
        }

        if (request.getCode() != null && !request.getCode().isBlank()) {

            boolean exists =
                    academicProgramRepository.existsByCodeIgnoreCaseAndIdNot(
                            request.getCode(),
                            programId
                    );

            if (exists) {
                throw new ConflictException("Ya existe un programa con ese código.");
            }

            program.setCode(request.getCode().toUpperCase());
        }

        if (request.getDescription() != null) {
            program.setDescription(request.getDescription());
        }

        if (request.getTotalCredits() != null) {
            program.setTotalCredits(request.getTotalCredits());
        }

        if (request.getFacultyId() != null &&
                !request.getFacultyId().equals(program.getFaculty().getId())) {

            Faculty faculty = facultyRepository.findById(request.getFacultyId())
                    .orElseThrow(() -> new NotFoundException("La facultad no existe."));

            program.setFaculty(faculty);
        }

        program.setUpdatedAt(LocalDateTime.now());

        academicProgramRepository.save(program);

        return toProgramDTO(program);
    }

    private ProgramDTO toProgramDTO(AcademicProgram program) {
        return ProgramDTO.builder()
                .id(program.getId())
                .name(program.getName())
                .code(program.getCode())
                .description(program.getDescription())
                .facultyId(program.getFaculty() != null ? program.getFaculty().getId() : null)
                .totalCredits(program.getTotalCredits())
                .active(program.isActive())
                .build();
    }

}
