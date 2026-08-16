package com.SIGMA.USCO.academic.service;

import com.SIGMA.USCO.academic.entity.AcademicProgram;
import com.SIGMA.USCO.academic.entity.Faculty;
import com.SIGMA.USCO.academic.repository.FacultyRepository;
import com.SIGMA.USCO.common.exception.NotFoundException;
import com.SIGMA.USCO.common.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("F11.4 - No se desactiva una facultad con programas académicos activos (A-07)")
class FacultyDeactivateTest {

    @Mock
    private FacultyRepository facultyRepository;

    @InjectMocks
    private FacultyService service;

    @Test
    @DisplayName("Facultad con un programa activo lanza ValidationException y no desactiva")
    void deactivateFacultyWithActiveProgramThrows() {
        Faculty faculty = Faculty.builder()
                .id(1L)
                .name("Ingeniería")
                .active(true)
                .programs(new ArrayList<>(List.of(
                        AcademicProgram.builder().id(1L).active(true).build()
                )))
                .build();
        when(facultyRepository.findById(1L)).thenReturn(Optional.of(faculty));

        assertThatThrownBy(() -> service.deactivateFaculty(1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("programas académicos activos");

        assertThat(faculty.isActive()).isTrue();
    }

    @Test
    @DisplayName("Facultad sin programas activos se desactiva correctamente")
    void deactivateFacultyWithoutActiveProgramsSucceeds() {
        Faculty faculty = Faculty.builder()
                .id(2L)
                .name("Ciencias Básicas")
                .programs(new ArrayList<>(List.of(
                        AcademicProgram.builder().id(2L).active(false).build()
                )))
                .build();
        when(facultyRepository.findById(2L)).thenReturn(Optional.of(faculty));

        service.deactivateFaculty(2L);

        assertThat(faculty.isActive()).isFalse();
        verify(facultyRepository).save(faculty);
    }

    @Test
    @DisplayName("Facultad inexistente lanza NotFoundException")
    void deactivateUnknownFacultyThrows() {
        when(facultyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivateFaculty(99L))
                .isInstanceOf(NotFoundException.class);
    }
}