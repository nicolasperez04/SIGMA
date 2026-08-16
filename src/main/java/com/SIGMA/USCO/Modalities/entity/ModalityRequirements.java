package com.SIGMA.USCO.Modalities.entity;

import com.SIGMA.USCO.Modalities.entity.enums.RuleType;
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
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "modality_requirements")
public class ModalityRequirements {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "modality_id")
    private DegreeModality modality;

    @Column(nullable = false)
    @ToString.Include
    private String requirementName;

    @Column(length = 4000)
    @ToString.Include
    private String description;

    @Enumerated(EnumType.STRING)
    @ToString.Include
    private RuleType ruleType;

    @Column(nullable = false)
    @ToString.Include
    private String expectedValue;

    @ToString.Include
    private boolean active = true;

    @ToString.Include
    private LocalDateTime createdAt;
    @ToString.Include
    private LocalDateTime updatedAt;

}
