package com.SIGMA.USCO.notifications.listeners;

public class NotificationMessageTemplates {

    public static String greeting(String name) {
        return "Estimado(a) " + name + ":\n\nReciba un cordial saludo.\n\n";
    }

    public static String closing() {
        return "\n\nAtentamente,\n\nSistema de Gestión Académica";
    }

    public static String universityClosing() {
        return "\n\nAtentamente,\n\nSistema de Gestión Académica\nUniversidad Surcolombiana";
    }

    public static String closingSigma() {
        return "\n\nAtentamente,\n\nSistema de Gestión Académica – SIGMA";
    }

    public static String closingDirector() {
        return "\n\nAtentamente,\n\nSistema de Gestión de Modalidades de Grado\nUniversidad Surcolombiana";
    }
}
