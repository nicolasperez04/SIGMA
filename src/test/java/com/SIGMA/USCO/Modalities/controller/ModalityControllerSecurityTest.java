package com.SIGMA.USCO.Modalities.controller;

import com.SIGMA.USCO.Users.entity.Role;
import com.SIGMA.USCO.Users.entity.User;
import com.SIGMA.USCO.Users.entity.enums.Status;
import com.SIGMA.USCO.Users.repository.RoleRepository;
import com.SIGMA.USCO.Users.repository.UserRepository;
import com.SIGMA.USCO.academic.entity.AcademicProgram;
import com.SIGMA.USCO.academic.entity.Faculty;
import com.SIGMA.USCO.academic.entity.StudentProfile;
import com.SIGMA.USCO.academic.repository.AcademicProgramRepository;
import com.SIGMA.USCO.academic.repository.FacultyRepository;
import com.SIGMA.USCO.academic.repository.StudentProfileRepository;
import com.SIGMA.USCO.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Regresión T0.1: endpoints de seminario accesibles para rol STUDENT con JWT real")
class ModalityControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private AcademicProgramRepository academicProgramRepository;

    @Autowired
    private FacultyRepository facultyRepository;

    @Test
    @DisplayName("GET /modalities/seminar/available responde 200 con token de estudiante")
    void studentCanListAvailableSeminars() throws Exception {
        User student = createStudentWithProfile();

        mockMvc.perform(get("/modalities/seminar/available")
                        .header("Authorization", "Bearer " + authToken(student)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /modalities/seminar/{seminarId}/enroll no devuelve 403: responde 400 de negocio por modalidad no iniciada")
    void studentCanEnrollInSeminar() throws Exception {
        // Regresión T0.1: antes del fix el endpoint daba 403 permanente por hasRole('ROLE_STUDENT').
        // Con el fix pasa la autorización y el service responde 400 (ValidationException: exige
        // la modalidad 'SEMINARIO DE GRADO' iniciada antes de validar el seminario).
        User student = createStudent();

        mockMvc.perform(post("/modalities/seminar/9999/enroll")
                        .header("Authorization", "Bearer " + authToken(student)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /modalities/seminar/available sin token responde 401 (endpoint protegido, F4: entry point JSON)")
    void seminarEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/modalities/seminar/available"))
                .andExpect(status().isUnauthorized());
    }

    private User createStudent() {
        Role studentRole = roleRepository.save(
                Role.builder().name("STUDENT").permissions(new HashSet<>()).build()
        );
        User student = User.builder()
                .name("Estudiante")
                .lastName("Test")
                .email("estudiante.test@usco.edu.co")
                .password(passwordEncoder.encode("password123"))
                .status(Status.ACTIVE)
                .roles(new HashSet<>(Set.of(studentRole)))
                .build();
        return userRepository.save(student);
    }

    private User createStudentWithProfile() {
        User student = createStudent();
        Faculty faculty = facultyRepository.save(
                Faculty.builder().name("Facultad Test").code("FAC-TEST").build()
        );
        AcademicProgram program = academicProgramRepository.save(
                AcademicProgram.builder()
                        .name("Programa Test")
                        .code("PROG-TEST")
                        .totalCredits(160L)
                        .faculty(faculty)
                        .build()
        );
        studentProfileRepository.save(
                StudentProfile.builder()
                        .user(student)
                        .academicProgram(program)
                        .faculty(faculty)
                        .studentCode("2025001")
                        .build()
        );
        return student;
    }

    private String authToken(User user) {
        return jwtService.generateToken(user);
    }
}