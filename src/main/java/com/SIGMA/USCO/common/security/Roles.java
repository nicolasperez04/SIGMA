package com.SIGMA.USCO.common.security;

/**
 * Roles del sistema.
 *
 * El valor es el nombre EXACTO sembrado en BD (DataInitializer, dev) y
 * almacenado en {@code Role.name} — SIN prefijo {@code ROLE_}:
 * - {@code User} concede la autoridad {@code "ROLE_" + role.getName()}.
 * - {@code hasRole('STUDENT')} resuelve a la autoridad {@code ROLE_STUDENT}
 *   (Spring Security antepone {@code ROLE_} al argumento).
 */
public final class Roles {

    private Roles() {
    }

    public static final String ROLE_SUPERADMIN = "SUPERADMIN";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_PROGRAM_HEAD = "PROGRAM_HEAD";
    public static final String ROLE_PROGRAM_CURRICULUM_COMMITTEE = "PROGRAM_CURRICULUM_COMMITTEE";
    public static final String ROLE_PROJECT_DIRECTOR = "PROJECT_DIRECTOR";
    public static final String ROLE_EXAMINER = "EXAMINER";
    public static final String ROLE_JURY = "JURY";
    public static final String ROLE_STUDENT = "STUDENT";
}