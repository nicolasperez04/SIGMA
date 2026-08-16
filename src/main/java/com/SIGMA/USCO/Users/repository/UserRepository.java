package com.SIGMA.USCO.Users.repository;


import com.SIGMA.USCO.Users.entity.enums.ProgramRole;
import com.SIGMA.USCO.Users.entity.enums.Status;
import com.SIGMA.USCO.Users.entity.User;
import com.SIGMA.USCO.common.security.Roles;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {
    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    // ponytail: conteo de usuarios activos con un rol dado (los Set de roles no generan filas duplicadas)
    long countByRoles_NameAndStatus(String name, Status status);

    List<User> findAllByRoles_Name(String roleName);

    default List<User> findAllExaminers() {
        return findAllByRoles_Name(Roles.ROLE_EXAMINER);
    }

    default List<User> findAllProgramHeads() {
        return findAllByRoles_Name(Roles.ROLE_PROGRAM_HEAD);
    }

    default List<User> findAllProgramCurriculumCommittee() {
        return findAllByRoles_Name(Roles.ROLE_PROGRAM_CURRICULUM_COMMITTEE);
    }

    @Query("""
            SELECT DISTINCT u FROM User u
            LEFT JOIN u.roles r
            LEFT JOIN StudentProfile sp ON sp.user = u
            WHERE (:status IS NULL OR u.status = :status)
              AND (:role IS NULL OR EXISTS (SELECT 1 FROM u.roles rr WHERE LOWER(rr.name) = LOWER(:role)))
              AND (:academicProgramId IS NULL OR sp.academicProgram.id = :academicProgramId)
              AND (:facultyId IS NULL OR sp.faculty.id = :facultyId)
              AND (:name IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :name, '%')))
              AND (:lastName IS NULL OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :lastName, '%')))
              AND (:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%')))
            ORDER BY u.id ASC
            """)
    Page<User> findUsersByFilters(
            @Param("status") Status status,
            @Param("role") String role,
            @Param("academicProgramId") Long academicProgramId,
            @Param("facultyId") Long facultyId,
            @Param("name") String name,
            @Param("lastName") String lastName,
            @Param("email") String email,
            Pageable pageable
    );
}
