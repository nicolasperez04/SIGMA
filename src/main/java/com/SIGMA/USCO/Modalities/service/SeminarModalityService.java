package com.SIGMA.USCO.Modalities.service;

import com.SIGMA.USCO.Modalities.entity.Seminar;
import com.SIGMA.USCO.Modalities.entity.StudentModality;
import com.SIGMA.USCO.Modalities.entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.entity.enums.SeminarStatus;
import com.SIGMA.USCO.Modalities.repository.SeminarRepository;
import com.SIGMA.USCO.Modalities.repository.StudentModalityRepository;
import com.SIGMA.USCO.Modalities.dto.SeminarDTO;
import com.SIGMA.USCO.Modalities.dto.SeminarDetailDTO;
import com.SIGMA.USCO.Modalities.dto.SeminarListDTO;
import com.SIGMA.USCO.Modalities.dto.SeminarResponseDTO;
import com.SIGMA.USCO.Modalities.dto.response.CancelSeminarResponse;
import com.SIGMA.USCO.Modalities.dto.response.CloseRegistrationsResponse;
import com.SIGMA.USCO.Modalities.dto.response.CompleteSeminarResponse;
import com.SIGMA.USCO.Modalities.dto.response.CreateSeminarResponse;
import com.SIGMA.USCO.Modalities.dto.response.EnrollSeminarResponse;
import com.SIGMA.USCO.Modalities.dto.response.SeminarDetailResponse;
import com.SIGMA.USCO.Modalities.dto.response.SeminarListResponse;
import com.SIGMA.USCO.Modalities.dto.response.SeminarResponse;
import com.SIGMA.USCO.Modalities.dto.response.StartSeminarResponse;
import com.SIGMA.USCO.Modalities.dto.response.UpdateSeminarResponse;
import com.SIGMA.USCO.Users.entity.ProgramAuthority;
import com.SIGMA.USCO.Users.entity.User;
import com.SIGMA.USCO.Users.entity.enums.ProgramRole;
import com.SIGMA.USCO.Users.repository.ProgramAuthorityRepository;
import com.SIGMA.USCO.Users.repository.UserRepository;
import com.SIGMA.USCO.academic.entity.AcademicProgram;
import com.SIGMA.USCO.academic.entity.ProgramDegreeModality;
import com.SIGMA.USCO.academic.entity.StudentProfile;
import com.SIGMA.USCO.academic.repository.StudentProfileRepository;
import com.SIGMA.USCO.common.exception.BusinessException;
import com.SIGMA.USCO.common.exception.ConflictException;
import com.SIGMA.USCO.common.exception.InternalException;
import com.SIGMA.USCO.common.exception.ValidationException;
import com.SIGMA.USCO.notifications.entity.enums.NotificationType;
import com.SIGMA.USCO.Modalities.event.ModalityEvent;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
    private final ModalityStatusTransition modalityStatusTransition;

    // ponytail: estados "en curso" de una modalidad de seminario (flujo hasta PROPOSAL_APPROVED);
    // excluye finales (GRADED_*, MODALITY_CLOSED, CORRECTIONS_REJECTED_FINAL) y cancelaciones
    // (CANCELLATION_*, MODALITY_CANCELLED, SEMINAR_CANCELED) — alinear con ReportUtils.ACTIVE_STATUSES si el negocio lo confirma
    private static final List<ModalityProcessStatus> SEMINAR_CANCELLABLE_STATUSES = List.of(
            ModalityProcessStatus.MODALITY_SELECTED,
            ModalityProcessStatus.UNDER_REVIEW_PROGRAM_HEAD,
            ModalityProcessStatus.CORRECTIONS_REQUESTED_PROGRAM_HEAD,
            ModalityProcessStatus.CORRECTIONS_SUBMITTED,
            ModalityProcessStatus.CORRECTIONS_SUBMITTED_TO_PROGRAM_HEAD,
            ModalityProcessStatus.CORRECTIONS_SUBMITTED_TO_COMMITTEE,
            ModalityProcessStatus.READY_FOR_PROGRAM_CURRICULUM_COMMITTEE,
            ModalityProcessStatus.UNDER_REVIEW_PROGRAM_CURRICULUM_COMMITTEE,
            ModalityProcessStatus.CORRECTIONS_REQUESTED_PROGRAM_CURRICULUM_COMMITTEE,
            ModalityProcessStatus.READY_FOR_DIRECTOR_ASSIGNMENT,
            ModalityProcessStatus.READY_FOR_APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE,
            ModalityProcessStatus.APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE,
            ModalityProcessStatus.PROPOSAL_APPROVED);

    @Transactional
    public CreateSeminarResponse createSeminar(SeminarDTO request, User user) {
        try {


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

            return new CreateSeminarResponse(
                    true,
                    "Seminario creado exitosamente",
                    seminar.getId(),
                    academicProgram.getName(),
                    seminar.getName()
            );

        } catch (IllegalArgumentException e) {
            throw new ValidationException(e.getMessage());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new InternalException("Error al crear el seminario", e);
        }
    }

    @Transactional(readOnly = true)
    public SeminarResponse listActiveSeminarsWithSeats(User user) {
        try {


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

            return new SeminarResponse(true, seminarDTOs);

        } catch (IllegalArgumentException e) {
            log.error("Error de validación al listar seminarios: {}", e.getMessage());
            throw new ValidationException(e.getMessage());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado al listar seminarios: {}", e.getMessage(), e);
            throw new InternalException("Error al listar los seminarios", e);
        }
    }

    @Transactional
    public EnrollSeminarResponse enrollInSeminar(Long seminarId, User user) {
        try {


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



            seminar.getEnrolledStudents().add(studentProfile);


            seminar.setCurrentParticipants(seminar.getCurrentParticipants() + 1);
            seminar.setUpdatedAt(LocalDateTime.now());
            seminarRepository.save(seminar);



            int availableSeats = seminar.getMaxParticipants() - seminar.getCurrentParticipants();

            return new EnrollSeminarResponse(
                    true,
                    "Te has inscrito exitosamente en el seminario",
                    seminar.getName(),
                    LocalDateTime.now(),
                    seminar.getCurrentParticipants(),
                    seminar.getMaxParticipants(),
                    availableSeats
            );

        } catch (IllegalArgumentException e) {

            throw new ValidationException(e.getMessage());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {

            throw new InternalException("Error al inscribirse en el seminario", e);
        }
    }


    @Transactional(readOnly = true)
    public SeminarDetailResponse getSeminarDetailForProgramHead(Long seminarId, User user) {
        try {


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
                    .toList();

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

            return new SeminarDetailResponse(true, detailDTO);

        } catch (IllegalArgumentException e) {
            throw new ValidationException(e.getMessage());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new InternalException("Error al obtener el detalle del seminario", e);
        }
    }


    @Transactional(readOnly = true)
    public SeminarListResponse listSeminarsForProgramHead(String status, Boolean active, User user) {
        try {

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
                    .toList();

            return new SeminarListResponse(true, seminarDTOs, seminarDTOs.size());

        } catch (IllegalArgumentException e) {
            throw new ValidationException(e.getMessage());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new InternalException("Error al listar seminarios", e);
        }
    }


    @Transactional
    public StartSeminarResponse startSeminar(Long seminarId, User user) {
        try {

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
                    applicationEventPublisher.publishEvent(new ModalityEvent(NotificationType.SEMINAR_STARTED, null, null, Map.of(
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

            return new StartSeminarResponse(
                    true,
                    "Seminario iniciado exitosamente",
                    seminar.getId(),
                    seminar.getName(),
                    seminar.getStatus().name(),
                    seminar.getStartDate(),
                    enrolledStudents.size(),
                    emailsSent
            );

        } catch (IllegalArgumentException e) {
            throw new ValidationException(e.getMessage());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new InternalException("Error al iniciar el seminario", e);
        }
    }



    @Transactional
    public CancelSeminarResponse cancelSeminar(Long seminarId, String reason, User user) {
        try {

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
                    if (SEMINAR_CANCELLABLE_STATUSES.contains(modality.getStatus())
                            && modality.getProgramDegreeModality().getDegreeModality().getName()
                                    .equalsIgnoreCase("SEMINARIO DE GRADO")) {
                        modalityStatusTransition.transition(modality, ModalityProcessStatus.MODALITY_CANCELLED, null,
                                "Modalidad cancelada por cancelación del seminario: " + seminar.getName());
                    }
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
                    applicationEventPublisher.publishEvent(new ModalityEvent(NotificationType.SEMINAR_CANCELLED, null, null, Map.of(
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

            return new CancelSeminarResponse(
                    true,
                    "Seminario cancelado exitosamente",
                    seminar.getId(),
                    seminar.getName(),
                    seminar.getStatus().name(),
                    enrolledStudents.size(),
                    emailsSent
            );

        } catch (IllegalArgumentException e) {
            throw new ValidationException(e.getMessage());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new InternalException("Error al cancelar el seminario", e);
        }
    }

    @Transactional
    public UpdateSeminarResponse updateSeminar(Long seminarId, SeminarDTO request, User user) {
        try {

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

            UpdateSeminarResponse.SeminarSummary seminarData = new UpdateSeminarResponse.SeminarSummary(
                    seminar.getId(),
                    seminar.getName(),
                    seminar.getDescription() != null ? seminar.getDescription() : "",
                    seminar.getTotalCost(),
                    seminar.getMinParticipants(),
                    seminar.getMaxParticipants(),
                    seminar.getCurrentParticipants(),
                    seminar.getTotalHours(),
                    seminar.getStatus().name(),
                    seminar.isActive(),
                    seminar.getUpdatedAt()
            );

            return new UpdateSeminarResponse(
                    true,
                    "Seminario actualizado exitosamente",
                    seminarData
            );

        } catch (IllegalArgumentException e) {
            throw new ValidationException(e.getMessage());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new InternalException("Error al actualizar el seminario", e);
        }
    }

    @Transactional
    public CloseRegistrationsResponse closeRegistrations(Long seminarId, User user) {
        try {

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

            return new CloseRegistrationsResponse(
                    true,
                    "Inscripciones cerradas exitosamente",
                    seminar.getId(),
                    seminar.getName(),
                    seminar.getStatus().name(),
                    seminar.getCurrentParticipants(),
                    seminar.getMaxParticipants(),
                    seminar.getUpdatedAt()
            );

        } catch (IllegalArgumentException e) {
            throw new ValidationException(e.getMessage());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new InternalException("Error al cerrar inscripciones del seminario", e);
        }
    }

    @Transactional
    public CompleteSeminarResponse completeSeminar(Long seminarId, User user) {
        try {

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

            return new CompleteSeminarResponse(
                    true,
                    "Seminario completado exitosamente",
                    seminar.getId(),
                    seminar.getName(),
                    seminar.getStatus().name(),
                    seminar.getStartDate(),
                    seminar.getEndDate(),
                    seminar.getCurrentParticipants()
            );

        } catch (IllegalArgumentException e) {
            throw new ValidationException(e.getMessage());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new InternalException("Error al completar el seminario", e);
        }
    }
}