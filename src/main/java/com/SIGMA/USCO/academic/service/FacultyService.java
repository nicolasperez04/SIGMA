package com.SIGMA.USCO.academic.service;

import com.SIGMA.USCO.academic.dto.FacultyDTO;
import com.SIGMA.USCO.academic.dto.ProgramDTO;
import com.SIGMA.USCO.academic.entity.AcademicProgram;
import com.SIGMA.USCO.academic.entity.Faculty;
import com.SIGMA.USCO.academic.repository.FacultyRepository;
import com.SIGMA.USCO.common.exception.ConflictException;
import com.SIGMA.USCO.common.exception.NotFoundException;
import com.SIGMA.USCO.common.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FacultyService {

    private final FacultyRepository facultyRepository;


    @Transactional
    public FacultyDTO createFaculty(FacultyDTO request){

        // ponytail: @Transactional only narrows the exists-check race; real fix is a unique constraint (deferred)

        if (facultyRepository.existsByCodeIgnoreCase(request.getCode())){
            throw new ConflictException("El código de la facultad ya existe.");
        }

        if (facultyRepository.existsByNameIgnoreCase(request.getName())){
            throw new ConflictException("El nombre de la facultad ya existe.");
        }
        Faculty faculty = Faculty.builder()
                .name(request.getName().toUpperCase())
                .code(request.getCode().toUpperCase())
                .description(request.getDescription())
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        facultyRepository.save(faculty);

        return toFacultyDTO(faculty, null);

    }

    @Transactional(readOnly = true)
    public List<FacultyDTO> getAllFaculties() {
        return facultyRepository.findAll()
                .stream()
                .map(faculty -> toFacultyDTO(faculty, null))
                .toList();

    }

    @Transactional(readOnly = true)
    public List<FacultyDTO> getActiveFaculties() {
        return facultyRepository.findByActiveTrue()
                .stream()
                .map(faculty -> toFacultyDTO(faculty, null))
                .toList();
    }

    @Transactional
    public FacultyDTO updateFaculty(Long id, FacultyDTO request) {

        Faculty faculty = facultyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Facultad no encontrada"));

        if (!faculty.getCode().equalsIgnoreCase(request.getCode())
                && facultyRepository.existsByCodeIgnoreCase(request.getCode())) {
            throw new ConflictException("El código de la facultad ya existe.");
        }

        if (!faculty.getName().equalsIgnoreCase(request.getName())
                && facultyRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ConflictException("El nombre de la facultad ya existe.");
        }

        faculty.setName(request.getName().toUpperCase());
        faculty.setCode(request.getCode().toUpperCase());
        faculty.setDescription(request.getDescription());
        faculty.setUpdatedAt(LocalDateTime.now());

        facultyRepository.save(faculty);

        return toFacultyDTO(faculty, null);

    }

    @Transactional
    public void deactivateFaculty(Long id) {
        Faculty faculty = facultyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Facultad no encontrada"));

        boolean hasActivePrograms = faculty.getPrograms().stream().anyMatch(AcademicProgram::isActive);
        if (hasActivePrograms) {
            throw new ValidationException("No se puede desactivar la facultad porque tiene programas académicos activos");
        }

        faculty.setActive(false);
        faculty.setUpdatedAt(LocalDateTime.now());

        facultyRepository.save(faculty);
    }

    @Transactional(readOnly = true)
    public FacultyDTO getFacultyDetail(Long facultyId) {

        Faculty faculty = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new NotFoundException("Facultad no encontrada"));

        List<ProgramDTO> programs =
                faculty.getPrograms()
                        .stream()
                        .map(p -> ProgramDTO.builder()
                                .id(p.getId())
                                .name(p.getName())
                                .code(p.getCode())
                                .totalCredits(p.getTotalCredits())
                                .active(p.isActive())
                                .build()
                        )
                        .toList();

        return FacultyDTO.builder()
                .id(faculty.getId())
                .name(faculty.getName())
                .code(faculty.getCode())
                .description(faculty.getDescription())
                .active(faculty.isActive())
                .academicPrograms(programs)
                .build();
    }

    private FacultyDTO toFacultyDTO(Faculty faculty, List<ProgramDTO> programs) {
        return FacultyDTO.builder()
                .id(faculty.getId())
                .name(faculty.getName())
                .code(faculty.getCode())
                .description(faculty.getDescription())
                .active(faculty.isActive())
                .academicPrograms(programs)
                .build();
    }



}
