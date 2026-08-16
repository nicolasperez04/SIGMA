package com.SIGMA.USCO.Modalities.entity;

import com.SIGMA.USCO.Modalities.entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Users.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Table(name = "modality_process_status_history")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModalityProcessStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @ManyToOne(optional = false)
    private StudentModality studentModality;

    @Enumerated(EnumType.STRING)
    @Column(length = 100,nullable = false)
    @ToString.Include
    private ModalityProcessStatus status;

    @ToString.Include
    private LocalDateTime changeDate;

    @ManyToOne
    private User responsible;

    @Column(length = 5000)
    @ToString.Include
    private String observations;

}
