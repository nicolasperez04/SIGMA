package com.SIGMA.USCO.Modalities.service;

import com.SIGMA.USCO.Modalities.Entity.Seminar;
import com.SIGMA.USCO.Modalities.Entity.StudentModality;
import com.SIGMA.USCO.Modalities.Entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.Entity.enums.SeminarStatus;
import com.SIGMA.USCO.Modalities.Repository.SeminarRepository;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityRepository;
import com.SIGMA.USCO.Modalities.dto.SeminarDTO;
import com.SIGMA.USCO.Modalities.dto.SeminarDetailDTO;
import com.SIGMA.USCO.Modalities.dto.SeminarListDTO;
import com.SIGMA.USCO.Modalities.dto.SeminarResponseDTO;
import com.SIGMA.USCO.Users.Entity.ProgramAuthority;
import com.SIGMA.USCO.Users.Entity.User;
import com.SIGMA.USCO.Users.Entity.enums.ProgramRole;
import com.SIGMA.USCO.Users.repository.ProgramAuthorityRepository;
import com.SIGMA.USCO.Users.repository.UserRepository;
import com.SIGMA.USCO.academic.entity.AcademicProgram;
import com.SIGMA.USCO.academic.entity.ProgramDegreeModality;
import com.SIGMA.USCO.academic.entity.StudentProfile;
import com.SIGMA.USCO.academic.repository.StudentProfileRepository;
import com.SIGMA.USCO.common.exception.BusinessException;
import com.SIGMA.USCO.common.exception.ConflictException;
import com.SIGMA.USCO.common.exception.ValidationException;
import com.SIGMA.USCO.notifications.entity.enums.NotificationType;
import com.SIGMA.USCO.notifications.event.ModalityEvent;
import com.SIGMA.USCO.security.SecurityUtils;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeminarModalityService {

    private final SeminarRepository seminarRepository;
    private final StudentModalityRepository studentModalityRepository;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ProgramAuthorityRepository programAuthorityRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public Map<String, Object> createSeminar(SeminarDTO request) {
        try {

            User user = SecurityUtils.getCurrentUser();


            ProgramAuthority programAuthority = programAuthorityRepository
                    .findByUser_IdAndRole(user.getId(), ProgramRole.PROGRAM_HEAD)
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new ValidationException(
                            "El usuario no tiene el rol de jefe de programa (PROGRAM_HEAD)"
                    ));

            AcademicProgram academicProgram = programAuthority.getAcademicProgram();


            if (seminarRepository.existsByNameIgnoreCaseAndAcademicProgramId(request.getName(), academicProgram.getId())) {
                throw new ConflictException("Ya existe un seminario con ese nombre en este programa académico.");
            }

            if (request.getMinParticipants() < 15) {
                throw new ValidationException(
                        "El número mínimo de participantes debe ser al menos 15 según el Artículo 43"
                );
            }

            if (request.getMaxParticipants() > 35) {
                throw new ValidationException(
                        "El número máximo de participantes no puede exceder 35 según el Artículo 43"
                );
            }

            if (request.getMinParticipants() > request.getMaxParticipants()) {
                throw new ValidationException(
                        "El número mínimo de participantes no puede ser mayor al máximo"
                );
            }

            if (request.getTotalHours() < 160) {
                throw new ValidationException(
                        "La intensidad horaria mínima debe ser de 160 horas según el Artículo 42"
                );
            }


            Seminar seminar = Seminar.builder()
                    .academicProgram(academicProgram)
                    .name(request.getName())
                    .description(request.getDescription())
                    .totalCost(request.getTotalCost())
                    .minParticipants(request.getMinParticipants())
                    .maxParticipants(request.getMaxParticipants())
                    .totalHours(request.getTotalHours())
                    .currentParticipants(0)
                    .active(true)
                    .status(SeminarStatus.OPEN)
                    .startDate(request.getStartDate())
                    .endDate(request.getEndDate())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            seminar = seminarRepository.save(seminar);

            return Map.of(
                    "success", true,
                    "message", "Seminario creado exitosamente",
                    "seminarId", seminar.getId(),
                    "programName", academicProgram.getName(),
                    "seminarName", seminar.getName()
            );

        } catch (IllegalArgumentException e) {
            throw new ValidationException(e.getMessage());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error al crear el seminario: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listActiveSeminarsWithSeats() {
        try {

            User user = SecurityUtils.getCurrentUser();


            StudentProfile studentProfile = studentProfileRepository.findById(user.getId())
                    .orElseThrow(() -> new ValidationException(
                            "No se encontró el perfil de estudiante para este usuario"
                    ));

            AcademicProgram studentProgram = studentProfile.getAcademicProgram();

            List<Seminar> seminars = seminarRepository.findActiveWithAvailableSeatsByProgram(
                    studentProgram.getId()
            );


            List<SeminarResponseDTO> seminarDTOs = seminars.stream()
                    .map(seminar -> {
                        int availableSeats = seminar.getMaxParticipants() - seminar.getCurrentParticipants();

                        String statusDescription;
                        if (availableSeats > 0) {
                            statusDescription = "Cupos disponibles: " + availableSeats;
                        } else {
                            statusDescription = "Sin cupos disponibles";
                        }

                        return SeminarResponseDTO.builder()
                                .id(seminar.getId())
                                .name(seminar.getName())
                                .description(seminar.getDescription())
                                .totalCost(seminar.getTotalCost())
                                .minParticipants(seminar.getMinParticipants())
                                .maxParticipants(seminar.getMaxParticipants())
                                .currentParticipants(seminar.getCurrentParticipants())
                                .availableSpots(availableSeats)
                                .totalHours(seminar.getTotalHours())
                                .status(seminar.getStatus() != null ? seminar.getStatus().name() : null)
                                .statusDescription(statusDescription)
                                .academicProgramId(seminar.getAcademicProgram().getId())
                                .academicProgramName(seminar.getAcademicProgram().getName())
                                .facultyName(seminar.getAcademicProgram().getFaculty().getName())
                                .startDate(seminar.getStartDate())
                                .endDate(seminar.getEndDate())
                                .createdAt(seminar.getCreatedAt())
                                .updatedAt(seminar.getUpdatedAt())
                                .canEnroll(null)
                                .build();
                    })
                    .toList();

            return Map.of(
                    "success", true,
                    "seminars", seminarDTOs
            );

        } catch (IllegalArgumentException e) {
            log.error("Error de validación al listar seminarios: {}", e.getMessage());
            throw new ValidationException(e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado al listar seminarios: {}", e.getMessage(), e);
            throw new RuntimeException("Error al listar los seminarios: " + e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> enrollInSeminar(Long seminarId) {
        try {

            User user = SecurityUtils.getCurrentUser();


            StudentProfile studentProfile = studentProfileRepository.findById(user.getId())
                    .orElseThrow(() -> new ValidationException(
                            "No se encontró el perfil de estudiante para este usuario"
                    ));


            List<StudentModality> leaderModalities = studentModalityRepository
                    .findByLeaderId(studentProfile.getId());

            boolean hasSeminarioGradoModality = leaderModalities.stream()
                    .anyMatch(sm -> {
                        ProgramDegreeModality pdm = sm.getProgramDegreeModality();
                        String modalityName = pdm.getDegreeModality().getName();
                        ModalityProcessStatus status = sm.getStatus();

                        return modalityName.equalsIgnoreCase("SEMINARIO DE GRADO") &&
                               (status == ModalityProcessStatus.MODALITY_SELECTED ||
                               status == ModalityProcessStatus.UNDER_REVIEW_PROGRAM_HEAD);
                    });

            if (!hasSeminarioGradoModality) {
                throw new ValidationException(
                        "Para inscribirse en un seminario, debes tener iniciada la modalidad 'SEMINARIO DE GRADO'. " +
                        "Por favor, solicita primero esta modalidad de grado."
                );
            }

            Seminar seminar = seminarRepository.findById(seminarId)
                    .orElseThrow(() -> new ValidationException(
                            "El seminario con ID " + seminarId + " no existe"
                    ));


            if (!seminar.isActive()) {
                throw new ValidationException("El seminario no está activo");
            }


            if (seminar.getStatus() != SeminarStatus.OPEN) {
                throw new ValidationException(
                        "El seminario no está abierto para inscripciones. Estado actual: " + seminar.getStatus()
                );
            }


            if (!seminar.getAcademicProgram().getId().equals(studentProfile.getAcademicProgram().getId())) {
                throw new ValidationException(
                        "El seminario no pertenece a tu programa académico"
                );
            }


            boolean alreadyEnrolled = seminarRepository.isStudentEnrolled(seminarId, studentProfile.getId());
            if (alreadyEnrolled) {

                throw new ValidationException("Ya estás inscrito en este seminario");
            }


            if (seminar.getCurrentParticipants() >= seminar.getMaxParticipants()) {
                throw new ValidationException(
                        "No hay cupos disponibles. El seminario ha alcanzado el máximo de " +
                        seminar.getMaxParticipants() + " participantes"
                );
            }



            seminarRepository.enrollStudent(seminarId, studentProfile.getId());


            seminar.setCurrentParticipants(seminar.getCurrentParticipants() + 1);
            seminar.setUpdatedAt(LocalDateTime.now());
            seminarRepository.save(seminar);



            int availableSeats = seminar.getMaxParticipants() - seminar.getCurrentParticipants();

            return Map.of(
                    "success", true,
                    "message", "Te has inscrito exitosamente en el seminario",
                    "seminarName", seminar.getName(),
                    "enrollmentDate", LocalDateTime.now(),
                    "currentParticipants", seminar.getCurrentParticipants(),
                    "maxParticipants", seminar.getMaxParticipants(),
                    "availableSeats", availableSeats
            );

        } catch (IllegalArgumentException e) {

            throw new ValidationException(e.getMessage());
        } catch (Exception e) {

            throw new RuntimeException("Error al inscribirse en el seminario: " + e.getMessage());
        }
    }


    @Transactional(readOnly = true)
    public Map<String, Object> FgetSeminarDetailForProgramHead(Long seminarId) {
        try {

            User user = SecurityUtils.getCurrentUser();


            Seminar seminar = seminarRepository.findById(seminarId)
                    .orElseThrow(() -> new ValidationException(
                            "El seminario con ID " + seminarId + " no existe"
                    ));


            Long userProgramId = programAuthorityRepository
                    .findByUser_IdAndRole(user.getId(), ProgramRole.PROGRAM_HEAD)
                    .stream()
                    .findFirst()
                    .map(pa -> pa.getAcademicProgram().getId())
                    .orElseThrow(() -> new ValidationException(
                            "No tienes permisos de jefe de programa"
                    ));

            if (!seminar.getAcademicProgram().getId().equals(userProgramId)) {
                throw new ValidationException(
                        "Este seminario no pertenece a tu programa académico"
                );
            }


            List<StudentProfile> enrolledStudents = seminarRepository.findEnrolledStudentsBySeminarId(seminarId);


            List<SeminarDetailDTO.EnrolledStudentDTO> enrolledStudentDTOs = enrolledStudents.stream()
                    .map(studentProfile -> {
                        User student = userRepository.findById(studentProfile.getId())
                                .orElse(null);

                        if (student == null) {
                            return null;
                        }


                        List<StudentModality> modalityList = studentModalityRepository
                                .findByLeaderId(studentProfile.getId());

                        StudentModality seminarioModality = modalityList.stream()
                                .filter(sm -> {
                                    String modalityName = sm.getProgramDegreeModality()
                                            .getDegreeModality().getName();
                                    return modalityName.equalsIgnoreCase("SEMINARIO DE GRADO");
                                })
                                .findFirst()
                                .orElse(null);

                        SeminarDetailDTO.ModalityInfoDTO modalityInfo = null;
                        if (seminarioModality != null) {
                            modalityInfo = SeminarDetailDTO.ModalityInfoDTO.builder()
                                    .modalityId(seminarioModality.getId())
                                    .modalityName(seminarioModality.getProgramDegreeModality()
                                            .getDegreeModality().getName())
                                    .modalityType(seminarioModality.getModalityType().name())
                                    .status(seminarioModality.getStatus().name())
                                    .selectionDate(seminarioModality.getSelectionDate())
                                    .build();
                        }

                        return SeminarDetailDTO.EnrolledStudentDTO.builder()
                                .studentId(studentProfile.getId())
                                .studentCode(studentProfile.getStudentCode())
                                .name(student.getName())
                                .lastName(student.getLastName())
                                .email(student.getEmail())



                                .approvedCredits(studentProfile.getApprovedCredits() != null ? studentProfile.getApprovedCredits().intValue() : null)

                                .build();
                    })
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());

            int availableSeats = seminar.getMaxParticipants() - seminar.getCurrentParticipants();
            double fillPercentage = (seminar.getCurrentParticipants() * 100.0) / seminar.getMaxParticipants();
            boolean hasMinimumParticipants = seminar.getCurrentParticipants() >= seminar.getMinParticipants();

            SeminarDetailDTO detailDTO = SeminarDetailDTO.builder()
                    .id(seminar.getId())
                    .name(seminar.getName())
                    .description(seminar.getDescription())
                    .totalCost(seminar.getTotalCost())
                    .minParticipants(seminar.getMinParticipants())
                    .maxParticipants(seminar.getMaxParticipants())
                    .currentParticipants(seminar.getCurrentParticipants())
                    .totalHours(seminar.getTotalHours())
                    .active(seminar.isActive())
                    .status(seminar.getStatus() != null ? seminar.getStatus().name() : null)
                    .startDate(seminar.getStartDate())
                    .endDate(seminar.getEndDate())
                    .createdAt(seminar.getCreatedAt())
                    .updatedAt(seminar.getUpdatedAt())
                    .academicProgramId(seminar.getAcademicProgram().getId())
                    .academicProgramName(seminar.getAcademicProgram().getName())
                    .facultyName(seminar.getAcademicProgram().getFaculty().getName())
                    .availableSeats(availableSeats)
                    .fillPercentage(fillPercentage)
                    .hasMinimumParticipants(hasMinimumParticipants)
                    .enrolledStudents(enrolledStudentDTOs)
                    .build();

            return Map.of(
                    "success", true,
                    "seminar", detailDTO
            );

        } catch (IllegalArgumentException e) {
            throw new ValidationException(e.getMessage());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener el detalle del seminario: " + e.getMessage());
        }
    }


    @Transactional(readOnly = true)
    public Map<String, Object> listSeminarsForProgramHead(String status, Boolean active) {
        try {
            User user = SecurityUtils.getCurrentUser();

            Long userProgramId = programAuthorityRepository
                    .findByUser_IdAndRole(user.getId(), ProgramRole.PROGRAM_HEAD)
                    .stream()
                    .findFirst()
                    .map(pa -> pa.getAcademicProgram().getId())
                    .orElseThrow(() -> new ValidationException(
                            "No tienes permisos de jefe de programa"
                    ));

            List<Seminar> seminars;

            if (status != null && active != null) {
                SeminarStatus seminarStatus = SeminarStatus.valueOf(status.toUpperCase());
                seminars = seminarRepository.findByAcademicProgramIdAndStatusAndActiveOrderByCreatedAtDesc(
                        userProgramId, seminarStatus, active
                );
            } else if (status != null) {
                SeminarStatus seminarStatus = SeminarStatus.valueOf(status.toUpperCase());
                seminars = seminarRepository.findByAcademicProgramIdAndStatusOrderByCreatedAtDesc(
                        userProgramId, seminarStatus
                );
            } else if (active != null) {
                seminars = seminarRepository.findByAcademicProgramIdAndActiveOrderByCreatedAtDesc(
                        userProgramId, active
                );
            } else {
                seminars = seminarRepository.findByAcademicProgramIdOrderByCreatedAtDesc(userProgramId);
            }

            List<SeminarListDTO> seminarDTOs = seminars.stream()
                    .map(seminar -> {
                        int availableSeats = seminar.getMaxParticipants() - seminar.getCurrentParticipants();
                        double fillPercentage = (seminar.getCurrentParticipants() * 100.0) / seminar.getMaxParticipants();
                        boolean hasMinimumParticipants = seminar.getCurrentParticipants() >= seminar.getMinParticipants();
                        boolean isFull = seminar.getCurrentParticipants() >= seminar.getMaxParticipants();

                        return SeminarListDTO.builder()
                                .id(seminar.getId())
                                .name(seminar.getName())
                                .description(seminar.getDescription())
                                .totalCost(seminar.getTotalCost())
                                .minParticipants(seminar.getMinParticipants())
                                .maxParticipants(seminar.getMaxParticipants())
                                .currentParticipants(seminar.getCurrentParticipants())
                                .totalHours(seminar.getTotalHours())
                                .active(seminar.isActive())
                                .status(seminar.getStatus() != null ? seminar.getStatus().name() : null)
                                .startDate(seminar.getStartDate())
                                .endDate(seminar.getEndDate())
                                .createdAt(seminar.getCreatedAt())
                                .updatedAt(seminar.getUpdatedAt())
                                .availableSeats(availableSeats)
                                .fillPercentage(fillPercentage)
                                .hasMinimumParticipants(hasMinimumParticipants)
                                .isFull(isFull)
                                .build();
                    })
                    .collect(Collectors.toList());

            return Map.of(
                    "success", true,
                    "seminars", seminarDTOs,
                    "total", seminarDTOs.size()
            );

        } catch (IllegalArgumentException e) {
            throw new ValidationException(e.getMessage());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error al listar seminarios: " + e.getMessage());
        }
    }


    @Transactional
    public Map<String, Object> startSeminar(Long seminarId) {
        try {
            User user = SecurityUtils.getCurrentUser();

            Long userProgramId = programAuthorityRepository
.findByUser_IdAndRole(user.getId(), ProgramRole.PROGRAM_HEAD)
                    .stream()
                    .findFirst()
                    .map(pa -> pa.getAcademicProgram().getId())
                    .orElseThrow(() -> new ValidationException(
                            "No tienes permisos de jefe de programa"
                    ));

            Seminar seminar = seminarRepository.findById(seminarId)
                    .orElseThrow(() -> new ValidationException(
                            "El seminario con ID " + seminarId + " no existe"
                    ));

            if (!seminar.getAcademicProgram().getId().equals(userProgramId)) {
                throw new ValidationException(
                        "Este seminario no pertenece a tu programa académico"
                );
            }

            if (seminar.getStatus() == SeminarStatus.IN_PROGRESS) {
                throw new ValidationException("El seminario ya está en progreso");
            }

            if (seminar.getStatus() == SeminarStatus.COMPLETED) {
                throw new ValidationException("El seminario ha sido completado");
            }

            if (seminar.getCurrentParticipants() < seminar.getMinParticipants()) {
                throw new ValidationException(
                        "No se puede iniciar el seminario. Se requieren al menos " +
                        seminar.getMinParticipants() + " participantes. Actualmente hay " +
                        seminar.getCurrentParticipants()
                );
            }

            seminar.setStatus(SeminarStatus.IN_PROGRESS);
            seminar.setStartDate(LocalDateTime.now());
            seminar.setUpdatedAt(LocalDateTime.now());
            seminarRepository.save(seminar);

            List<StudentProfile> enrolledStudents = seminarRepository.findEnrolledStudentsBySeminarId(seminarId);

            int emailsSent = 0;
            for (StudentProfile studentProfile : enrolledStudents) {
                User student = userRepository.findById(studentProfile.getId()).orElse(null);
                if (student != null && student.getEmail() != null) {
                    applicationEventPublisher.publishEvent(new ModalityEvent(NotificationType.SEMINAR_STARTED, 0L, null, Map.of(
                            ModalityEvent.KEY_RECIPIENT_EMAIL, student.getEmail(),
                            ModalityEvent.KEY_RECIPIENT_NAME, student.getName() + " " + student.getLastName(),
                            ModalityEvent.KEY_SEMINAR_NAME, seminar.getName(),
                            ModalityEvent.KEY_START_DATE, seminar.getStartDate(),
                            ModalityEvent.KEY_TOTAL_HOURS, seminar.getTotalHours(),
                            ModalityEvent.KEY_PROGRAM_NAME, seminar.getAcademicProgram().getName()
                    )));
                    emailsSent++;
                }
            }

            return Map.of(
                    "success", true,
                    "message", "Seminario iniciado exitosamente",
                    "seminarId", seminar.getId(),
                    "seminarName", seminar.getName(),
                    "status", seminar.getStatus().name(),
                    "startDate", seminar.getStartDate(),
                    "enrolledStudents", enrolledStudents.size(),
                    "emailsSent", emailsSent
            );

        } catch (IllegalArgumentException e) {
            throw new ValidationException(e.getMessage());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error al iniciar el seminario: " + e.getMessage());
        }
    }



    @Transactional
    public Map<String, Object> cancelSeminar(Long seminarId, String reason) {
        try {
            User user = SecurityUtils.getCurrentUser();

            Long userProgramId = programAuthorityRepository
                    .findByUser_IdAndRole(user.getId(), ProgramRole.PROGRAM_HEAD)
                    .stream()
                    .findFirst()
                    .map(pa -> pa.getAcademicProgram().getId())
                    .orElseThrow(() -> new ValidationException(
                            "No tienes permisos de jefe de programa"
                    ));

            Seminar seminar = seminarRepository.findById(seminarId)
                    .orElseThrow(() -> new ValidationException(
                            "El seminario con ID " + seminarId + " no existe"
                    ));

            if (!seminar.getAcademicProgram().getId().equals(userProgramId)) {
                throw new ValidationException(
                        "Este seminario no pertenece a tu programa académico"
                );
            }

            if (seminar.getStatus() != SeminarStatus.OPEN) {
                throw new ValidationException(
                        "Solo se pueden cancelar seminarios que estén en estado ABIERTO (OPEN). " +
                        "Estado actual: " + seminar.getStatus() + ". " +
                        "No se puede cancelar un seminario que ya ha iniciado o está completado."
                );
            }

            List<StudentProfile> enrolledStudents = seminarRepository.findEnrolledStudentsBySeminarId(seminarId);

            // Cambiar el status de la modalidad de cada estudiante a SEMINAR_CANCELED
            for (StudentProfile studentProfile : enrolledStudents) {
                List<StudentModality> modalities = studentModalityRepository.findByLeaderId(studentProfile.getId());
                for (StudentModality modality : modalities) {
                    modality.setStatus(ModalityProcessStatus.MODALITY_CANCELLED);
                    studentModalityRepository.save(modality);
                }
            }

            seminar.setStatus(SeminarStatus.CLOSED);
            seminar.setActive(false);
            seminar.setUpdatedAt(LocalDateTime.now());
            seminar.getEnrolledStudents().clear();
            seminarRepository.save(seminar);

            int emailsSent = 0;
            for (StudentProfile studentProfile : enrolledStudents) {
                User student = userRepository.findById(studentProfile.getId()).orElse(null);
                if (student != null && student.getEmail() != null) {
                    applicationEventPublisher.publishEvent(new ModalityEvent(NotificationType.SEMINAR_CANCELLED, 0L, null, Map.of(
                            ModalityEvent.KEY_RECIPIENT_EMAIL, student.getEmail(),
                            ModalityEvent.KEY_RECIPIENT_NAME, student.getName() + " " + student.getLastName(),
                            ModalityEvent.KEY_SEMINAR_NAME, seminar.getName(),
                            ModalityEvent.KEY_CANCELLED_DATE, LocalDateTime.now(),
                            ModalityEvent.KEY_PROGRAM_NAME, seminar.getAcademicProgram().getName(),
                            ModalityEvent.KEY_REASON, reason
                    )));
                    emailsSent++;
                }
            }

            return Map.of(
                    "success", true,
                    "message", "Seminario cancelado exitosamente",
                    "seminarId", seminar.getId(),
                    "seminarName", seminar.getName(),
                    "status", seminar.getStatus().name(),
                    "previouslyEnrolledStudents", enrolledStudents.size(),
                    "emailsSent", emailsSent
            );

        } catch (IllegalArgumentException e) {
            throw new ValidationException(e.getMessage());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error al cancelar el seminario: " + e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> updateSeminar(Long seminarId, SeminarDTO request) {
        try {
            User user = SecurityUtils.getCurrentUser();

            Long userProgramId = programAuthorityRepository
                    .findByUser_IdAndRole(user.getId(), ProgramRole.PROGRAM_HEAD)
                    .stream()
                    .findFirst()
                    .map(pa -> pa.getAcademicProgram().getId())
                    .orElseThrow(() -> new ValidationException(
                            "No tienes permisos de jefe de programa"
                    ));

            Seminar seminar = seminarRepository.findById(seminarId)
                    .orElseThrow(() -> new ValidationException(
                            "El seminario con ID " + seminarId + " no existe"
                    ));

            if (!seminar.getAcademicProgram().getId().equals(userProgramId)) {
                throw new ValidationException(
                        "Este seminario no pertenece a tu programa académico"
                );
            }

            if (seminar.getStatus() == SeminarStatus.COMPLETED) {
                throw new ValidationException(
                        "No se puede editar un seminario que ya ha sido completado"
                );
            }

            if (seminar.getStatus() == SeminarStatus.CLOSED) {
                throw new ValidationException(
                        "No se puede editar un seminario que ha sido cancelado"
                );
            }

            if (request.getMinParticipants() != null && request.getMaxParticipants() != null) {
                if (request.getMinParticipants() > request.getMaxParticipants()) {
                    throw new ValidationException(
                            "El mínimo de participantes no puede ser mayor al máximo"
                    );
                }
            }

            if (request.getMaxParticipants() != null && seminar.getCurrentParticipants() > request.getMaxParticipants()) {
                throw new ValidationException(
                        "No se puede reducir el máximo de participantes por debajo del número actual de inscritos (" +
                        seminar.getCurrentParticipants() + ")"
                );
            }

            if (request.getName() != null && !request.getName().isBlank()) {
                seminar.setName(request.getName());
            }

            if (request.getDescription() != null) {
                seminar.setDescription(request.getDescription());
            }

            if (request.getTotalCost() != null) {
                seminar.setTotalCost(request.getTotalCost());
            }

            if (request.getMinParticipants() != null) {
                seminar.setMinParticipants(request.getMinParticipants());
            }

            if (request.getMaxParticipants() != null) {
                seminar.setMaxParticipants(request.getMaxParticipants());
            }

            if (request.getTotalHours() != null) {
                seminar.setTotalHours(request.getTotalHours());
            }

            seminar.setUpdatedAt(LocalDateTime.now());
            seminarRepository.save(seminar);

            Map<String, Object> seminarData = new HashMap<>();
            seminarData.put("id", seminar.getId());
            seminarData.put("name", seminar.getName());
            seminarData.put("description", seminar.getDescription() != null ? seminar.getDescription() : "");
            seminarData.put("totalCost", seminar.getTotalCost());
            seminarData.put("minParticipants", seminar.getMinParticipants());
            seminarData.put("maxParticipants", seminar.getMaxParticipants());
            seminarData.put("currentParticipants", seminar.getCurrentParticipants());
            seminarData.put("totalHours", seminar.getTotalHours());
            seminarData.put("status", seminar.getStatus().name());
            seminarData.put("active", seminar.isActive());
            seminarData.put("updatedAt", seminar.getUpdatedAt());

            return Map.of(
                    "success", true,
                    "message", "Seminario actualizado exitosamente",
                    "seminar", seminarData
            );

        } catch (IllegalArgumentException e) {
            throw new ValidationException(e.getMessage());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error al actualizar el seminario: " + e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> closeRegistrations(Long seminarId) {
        try {
            User user = SecurityUtils.getCurrentUser();

            Long userProgramId = programAuthorityRepository
                    .findByUser_IdAndRole(user.getId(), ProgramRole.PROGRAM_HEAD)
                    .stream()
                    .findFirst()
                    .map(pa -> pa.getAcademicProgram().getId())
                    .orElseThrow(() -> new ValidationException(
                            "No tienes permisos de jefe de programa"
                    ));

            Seminar seminar = seminarRepository.findById(seminarId)
                    .orElseThrow(() -> new ValidationException(
                            "El seminario con ID " + seminarId + " no existe"
                    ));

            if (!seminar.getAcademicProgram().getId().equals(userProgramId)) {
                throw new ValidationException(
                        "Este seminario no pertenece a tu programa académico"
                );
            }

            if (seminar.getStatus() == SeminarStatus.COMPLETED) {
                throw new ValidationException(
                        "No se pueden cerrar inscripciones de un seminario ya completado"
                );
            }

            if (seminar.getStatus() == SeminarStatus.CLOSED) {
                throw new ValidationException(
                        "No se pueden cerrar inscripciones de un seminario cancelado"
                );
            }

            if (seminar.getStatus() == SeminarStatus.REGISTRATION_CLOSED) {
                throw new ValidationException(
                        "Las inscripciones de este seminario ya están cerradas"
                );
            }

            seminar.setStatus(SeminarStatus.REGISTRATION_CLOSED);
            seminar.setUpdatedAt(LocalDateTime.now());
            seminarRepository.save(seminar);

            return Map.of(
                    "success", true,
                    "message", "Inscripciones cerradas exitosamente",
                    "seminarId", seminar.getId(),
                    "seminarName", seminar.getName(),
                    "status", seminar.getStatus().name(),
                    "currentParticipants", seminar.getCurrentParticipants(),
                    "maxParticipants", seminar.getMaxParticipants(),
                    "updatedAt", seminar.getUpdatedAt()
            );

        } catch (IllegalArgumentException e) {
            throw new ValidationException(e.getMessage());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error al cerrar inscripciones del seminario: " + e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> completeSeminar(Long seminarId) {
        try {
            User user = SecurityUtils.getCurrentUser();

            Long userProgramId = programAuthorityRepository
                    .findByUser_IdAndRole(user.getId(), ProgramRole.PROGRAM_HEAD)
                    .stream()
                    .findFirst()
                    .map(pa -> pa.getAcademicProgram().getId())
                    .orElseThrow(() -> new ValidationException(
                            "No tienes permisos de jefe de programa"
                    ));

            Seminar seminar = seminarRepository.findById(seminarId)
                    .orElseThrow(() -> new ValidationException(
                            "El seminario con ID " + seminarId + " no existe"
                    ));

            if (!seminar.getAcademicProgram().getId().equals(userProgramId)) {
                throw new ValidationException(
                        "Este seminario no pertenece a tu programa académico"
                );
            }

            if (seminar.getStatus() != SeminarStatus.IN_PROGRESS) {
                throw new ValidationException(
                        "Solo se pueden completar seminarios que estén en estado EN PROGRESO (IN_PROGRESS). " +
                        "Estado actual: " + seminar.getStatus()
                );
            }

            seminar.setStatus(SeminarStatus.COMPLETED);
            seminar.setActive(false);
            seminar.setEndDate(LocalDateTime.now());
            seminar.setUpdatedAt(LocalDateTime.now());
            seminarRepository.save(seminar);

            return Map.of(
                    "success", true,
                    "message", "Seminario completado exitosamente",
                    "seminarId", seminar.getId(),
                    "seminarName", seminar.getName(),
                    "status", seminar.getStatus().name(),
                    "startDate", seminar.getStartDate(),
                    "endDate", seminar.getEndDate(),
                    "totalParticipants", seminar.getCurrentParticipants()
            );

        } catch (IllegalArgumentException e) {
            throw new ValidationException(e.getMessage());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error al completar el seminario: " + e.getMessage());
        }
    }
}