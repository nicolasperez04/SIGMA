package com.SIGMA.USCO.Modalities.entity;

import com.SIGMA.USCO.Modalities.entity.enums.InvitationStatus;
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
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "modality_invitations")
public class ModalityInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_modality_id")
    private StudentModality studentModality;


    @ManyToOne(optional = false)
    @JoinColumn(name = "inviter_id")
    private User inviter;

    @ManyToOne(optional = false)
    @JoinColumn(name = "invitee_id")
    private User invitee;

    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    @ToString.Include
    private InvitationStatus status;

    @Column(nullable = false)
    @ToString.Include
    private LocalDateTime invitedAt;

    @ToString.Include
    private LocalDateTime respondedAt;


}

