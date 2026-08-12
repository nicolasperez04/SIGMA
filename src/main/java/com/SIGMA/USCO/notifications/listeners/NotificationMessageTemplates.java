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

    public static String modalityStarted(String studentName, String modalidadInfo, String estado) {
        String body = """
                Nos permitimos informarle que su modalidad de grado ha sido registrada e iniciada oficialmente en el sistema institucional. A continuación, se relaciona la información correspondiente:

                Modalidad de grado: "%s".
                Estado actual del proceso: %s.

                Actualmente, la modalidad se encuentra en etapa de revisión y evaluación por parte de la Jefatura de Programa y del Comité de Currículo correspondiente.

                Se recomienda consultar periódicamente el sistema y mantenerse atento(a) a las notificaciones institucionales, ya que a través de este medio se comunicarán solicitudes, observaciones o decisiones relacionadas con su proceso académico.
                """.formatted(
                modalidadInfo,
                estado
        );
        return greeting(studentName) + body + closing();
    }

    public static String correctionsRequested(String studentName, String requestedByText, String documentName, String observations) {
        String body = """
        Nos permitimos informarle que %s ha solicitado la realización de correcciones en uno de los documentos asociados a su modalidad de grado, en el marco del proceso de revisión académica.

        A continuación, se detalla la información correspondiente:

        Documento: "%s".
        Observaciones registradas: %s.

        En este sentido, se solicita ingresar a la plataforma institucional, revisar detalladamente las observaciones indicadas y efectuar los ajustes correspondientes, con el fin de dar continuidad al proceso académico dentro de los plazos establecidos.

        Este mensaje constituye una notificación automática generada para efectos de control y trazabilidad del proceso.
        """.formatted(
                    requestedByText,
                    documentName,
                    observations
            );
        return greeting(studentName) + body + closing();
    }

    public static String cancellationRequested(String studentName, String modalidadInfo) {
        String body = """
        Nos permitimos informarle que su solicitud de cancelación de la modalidad de grado ha sido registrada correctamente en el sistema institucional.

        A continuación, se relaciona la información correspondiente:

        Modalidad de grado: "%s".

        La solicitud será evaluada inicialmente por el director del proyecto y, posteriormente, por el Comité de Currículo del programa académico correspondiente.

        Una vez se emita una decisión oficial, esta le será notificada oportunamente a través de la plataforma institucional.

        Este mensaje constituye una notificación automática generada para efectos de control y trazabilidad del proceso académico.
        """.formatted(
                    modalidadInfo
            );
        return greeting(studentName) + body + closing();
    }

    public static String cancellationApproved(String studentName, String modalidadInfo) {
        String body = """
        Nos permitimos informarle que el Comité de Currículo del programa académico ha aprobado oficialmente su solicitud de cancelación de la modalidad de grado.

        A continuación, se relaciona la información correspondiente:

        Modalidad de grado: "%s".

        En consecuencia, la modalidad queda cerrada de manera oficial y el proceso académico asociado finaliza a partir de la fecha en que se emite la presente decisión.

        En caso de requerir orientación adicional o información complementaria sobre su situación académica, podrá comunicarse con la Jefatura de Programa.

        Este mensaje constituye una notificación automática generada para efectos de control y trazabilidad institucional.
        """.formatted(
                    modalidadInfo
            );
        return greeting(studentName) + body + closingSigma();
    }

    public static String cancellationRejected(String studentName, String modalidadInfo, String reason) {
        String body = """
        Nos permitimos informarle que el Comité de Currículo del programa académico ha decidido no aprobar su solicitud de cancelación de la modalidad de grado.

        A continuación, se relaciona la información correspondiente:

        Modalidad de grado: "%s".
        Motivo de la decisión: %s.

        En consecuencia, la modalidad de grado continúa activa bajo las condiciones previamente establecidas dentro del proceso académico.

        En caso de requerir mayor claridad sobre la presente decisión o desear orientación adicional, podrá comunicarse con la Jefatura de Programa.

        Este mensaje constituye una notificación automática generada para efectos de control y trazabilidad institucional.
        """.formatted(
                    modalidadInfo,
                    reason
            );
        return greeting(studentName) + body + closing();
    }

    public static String defenseScheduled(String studentName, String modalidadInfo, String defenseDate, String defenseLocation, String directorName) {
        String body = """
        Nos permitimos informarle que la sustentación correspondiente a su modalidad de grado ha sido programada, conforme al proceso académico establecido.

        A continuación, se relaciona la información correspondiente:

        Modalidad de grado: "%s".
        Fecha y hora: %s.
        Lugar: %s.
        Director asignado: %s.

        De acuerdo con la normativa institucional vigente, deberá realizar la divulgación pública de su proyecto con al menos tres (3) días hábiles de anticipación a la fecha programada para la sustentación, en los espacios definidos por el programa académico.

        Se recomienda presentarse con la debida antelación y cumplir estrictamente con los lineamientos académicos establecidos para el desarrollo de la sesión de sustentación.

        Este mensaje constituye una notificación automática generada para efectos de control y trazabilidad del proceso académico.
        """.formatted(
                    modalidadInfo,
                    defenseDate,
                    defenseLocation,
                    directorName
            );
        return greeting(studentName) + body + closing();
    }

    public static String directorAssigned(String studentName, String modalidadInfo, String directorName, String directorEmail) {
        String body = """
        Nos permitimos informarle que ha sido designado oficialmente un Director de Proyecto para su modalidad de grado, conforme a los lineamientos académicos vigentes.

        A continuación, se relaciona la información correspondiente:

        Modalidad de grado: "%s".
        Director asignado: %s.
        Correo electrónico: %s.

        A partir de este momento, el director asignado actuará como su orientador académico principal durante el desarrollo de la modalidad de grado y será responsable del seguimiento y acompañamiento del proceso.

        Se recomienda establecer contacto oportunamente con el director, con el fin de coordinar las actividades iniciales y definir el plan de trabajo correspondiente.

        Este mensaje constituye una notificación automática generada para efectos de control y trazabilidad del proceso académico.
        """.formatted(
                    modalidadInfo,
                    directorName,
                    directorEmail
            );
        return greeting(studentName) + body + closing();
    }

    public static String defenseResultApproved(String studentName, String modalidadInfo, String distinction, String observations) {
        String body = """
            Nos permitimos informarle que, una vez realizada la sustentación y evaluado el resultado por los jurados designados, ha aprobado oficialmente la modalidad de grado.

            A continuación, se relaciona la información correspondiente:

            Modalidad de grado: "%s".
            Mención académica: %s.
            Observaciones registradas: %s.

            Se adjunta a este correo el acta de aprobación en formato PDF, documento oficial que certifica la culminación satisfactoria de su modalidad de grado, conforme a la normatividad académica vigente.

            Para finalizar su proceso académico, deberá comunicarse con la Jefatura de Programa, con el fin de adelantar los trámites administrativos correspondientes.

            Reciba un reconocimiento institucional por este importante logro académico.

            Este mensaje constituye una notificación automática generada para efectos de control y trazabilidad del proceso académico.
            """.formatted(
                modalidadInfo,
                distinction,
                observations
        );
        return greeting(studentName) + body + universityClosing();
    }

    public static String defenseResultRejected(String studentName, String modalidadInfo, String observations) {
        String body = """
            Nos permitimos informarle que, una vez realizada la sustentación y evaluado el resultado por los jurados designados, no se ha determinado la aprobación de la modalidad de grado en la presente oportunidad.

            A continuación, se relaciona la información correspondiente:

            Modalidad de grado: "%s".
            Observaciones de los jurados: %s.

            De acuerdo con la normativa académica vigente, se recomienda revisar detenidamente las observaciones consignadas y establecer comunicación con el Director de Proyecto, así como con la Jefatura de Programa, con el fin de definir las acciones a seguir dentro del proceso académico.

            Este mensaje constituye una notificación automática generada para efectos de control y trazabilidad del proceso académico.
            """.formatted(
                modalidadInfo,
                observations
        );
        return greeting(studentName) + body + closingSigma() + "\nUniversidad Surcolombiana";
    }

    public static String modalityApprovedByCommittee(String studentName, String modalidadInfo, String directorName, String approvalDate) {
        String body = """
        Nos permitimos informarle que la modalidad de grado ha sido aprobada oficialmente por el Comité de Currículo del programa académico.

        A continuación, se relaciona la información correspondiente:

        Modalidad de grado: "%s".
        Estado del proceso: Propuesta aprobada por el Comité de Currículo.
        Director de Proyecto: %s.
        Fecha de aprobación: %s.

        En virtud de esta decisión, la modalidad de grado continúa con la siguiente etapa del proceso académico, correspondiente a la evaluación y aprobación por parte del jurado designado.

        Se recomienda mantenerse atento(a) a las notificaciones del sistema institucional y conservar comunicación permanente con el Director de Proyecto y la Jefatura de Programa, con el fin de garantizar el adecuado desarrollo y seguimiento del proceso.

        Este mensaje constituye una notificación automática generada para efectos de control y trazabilidad del proceso académico.
        """.formatted(
                    modalidadInfo,
                    directorName,
                    approvalDate
            );
        return greeting(studentName) + body + universityClosing();
    }

    public static String modalityApprovedByProgramHead(String studentName, String modalidadInfo) {
        String body = """
        Nos permitimos informarle que la modalidad de grado ha sido aprobada oficialmente por la Jefatura de Programa y/o la coordinación de modalidades del programa académico.

        A continuación, se relaciona la información correspondiente:

        Modalidad de grado: "%s".
        Estado del proceso: Aprobada por la Jefatura de Programa.

        En virtud de esta decisión, la modalidad de grado continuará con la etapa de evaluación por parte del Comité de Currículo del programa académico, instancia encargada de emitir la decisión correspondiente para la continuidad del proceso.

        Se recomienda mantenerse atento(a) a las notificaciones del sistema institucional y conservar comunicación con la Jefatura de Programa, en caso de requerir información adicional o aclaraciones relacionadas con el trámite.

        Este mensaje constituye una notificación automática generada para efectos de control y trazabilidad del proceso académico.
        """.formatted(
                    modalidadInfo
            );
        return greeting(studentName) + body + universityClosing();
    }

    public static String correctionDeadlineReminder(String studentName, String modalidadInfo, int daysRemaining, String deadline) {
        String body = """
        Nos permitimos recordarle que actualmente presenta correcciones pendientes asociadas a su modalidad de grado, conforme al proceso de revisión académica.

        A continuación, se relaciona la información correspondiente:

        Modalidad de grado: "%s".
        Días restantes: %d.
        Fecha límite de entrega: %s.

        En este sentido, es indispensable realizar las correcciones solicitadas y efectuar la carga de la versión ajustada del documento antes de la fecha indicada. En caso de no cumplir con el plazo establecido, el sistema podrá proceder con la cancelación automática de la modalidad, de conformidad con la normativa académica vigente.

        Para realizar la carga del documento, deberá seguir el siguiente procedimiento:

        1. Realizar las correcciones indicadas en el documento.
        2. Ingresar a la plataforma institucional.
        3. Acceder al módulo "Mis Documentos".
        4. Seleccionar el documento correspondiente y cargar la versión corregida.

        En caso de presentar alguna dificultad o requerir orientación adicional, podrá comunicarse a la mayor brevedad con la Jefatura de Programa.

        Este mensaje constituye una notificación automática generada como recordatorio preventivo y para efectos de control y trazabilidad del proceso académico.
        """.formatted(
                    modalidadInfo,
                    daysRemaining,
                    deadline
            );
        return greeting(studentName) + body + closingSigma() + "\nUniversidad Surcolombiana";
    }

    public static String correctionDeadlineExpired(String studentName, String modalidadInfo, String requestDate) {
        String body = """
            Nos permitimos informarle que la modalidad de grado relacionada a continuación ha sido cancelada de manera automática, debido al vencimiento del plazo establecido para la entrega de las correcciones solicitadas, sin que se haya efectuado la carga del documento ajustado dentro del término reglamentario.

            A continuación, se relaciona la información correspondiente:

            Modalidad de grado: "%s".
            Fecha de solicitud de correcciones: %s.
            Plazo máximo otorgado: 30 días calendario.
            Estado final del proceso: Cancelada.

            La presente decisión se adopta de conformidad con la normativa académica vigente y el reglamento institucional aplicable a las modalidades de grado.

            Para dar continuidad a su proceso académico, deberá postular una nueva modalidad de grado e iniciar nuevamente el procedimiento desde su etapa inicial, cumpliendo con los requisitos y tiempos establecidos por el programa académico.

            Se recomienda comunicarse con la Jefatura de Programa, con el fin de recibir orientación sobre los pasos a seguir.

            Este mensaje constituye una notificación automática generada como constancia del cierre del proceso y para efectos de control y trazabilidad institucional.
            """.formatted(
                    modalidadInfo,
                    requestDate
            );
        return greeting(studentName) + body + universityClosing();
    }

    public static String correctionResubmitted(String studentName, String modalidadInfo, String documentName, String submissionDate) {
        String body = """
            Nos permitimos informarle que la carga del documento corregido ha sido registrada correctamente en el Sistema de Gestión Académica, en el marco del proceso de revisión de su modalidad de grado.

            A continuación, se relaciona la información correspondiente:

            Modalidad de grado: "%s".
            Nombre del archivo: %s.
            Fecha de envío: %s.
            Estado del proceso: Correcciones enviadas – pendiente de revisión.

            El documento será evaluado por las instancias académicas competentes. Una vez finalizada la revisión, le será notificado el resultado correspondiente a través de la plataforma institucional.

            Se recomienda permanecer atento(a) a las comunicaciones emitidas por el sistema, con el fin de garantizar la adecuada continuidad del proceso académico.

            Este mensaje constituye una notificación automática generada como constancia del registro de la nueva versión del documento y para efectos de control y trazabilidad institucional.
            """.formatted(
                    modalidadInfo,
                    documentName,
                    submissionDate
            );
        return greeting(studentName) + body + universityClosing();
    }

    public static String correctionApproved(String studentName, String modalidadInfo, String documentName) {
        String body = """
            Nos permitimos informarle que las correcciones remitidas han sido aprobadas por el jurado evaluador, en el marco del proceso de revisión académica de su modalidad de grado.

            A continuación, se relaciona la información correspondiente:

            Modalidad de grado: "%s".
            Documento evaluado: %s.
            Estado del proceso: Correcciones aprobadas.

            En virtud de esta decisión, la modalidad de grado continúa con el desarrollo normal del proceso académico, conforme a las disposiciones institucionales vigentes.

            La siguiente actuación dentro del proceso será notificada oportunamente a través de la plataforma institucional.

            Este mensaje constituye una notificación automática generada como constancia de la decisión registrada y para efectos de control y trazabilidad institucional.
            """.formatted(
                    modalidadInfo,
                    documentName
            );
        return greeting(studentName) + body + universityClosing();
    }

    public static String correctionRejectedFinal(String studentName, String modalidadInfo, String documentName, String reason) {
        String body = """
            Nos permitimos informarle que, como resultado de la evaluación realizada por el jurado designado, no se ha determinado la aprobación de uno o más documentos asociados a su modalidad de grado. En consecuencia, se ha dispuesto la cancelación definitiva del proceso académico correspondiente.

            A continuación, se relaciona la información pertinente:

            Modalidad de grado: "%s".
            Documento evaluado: %s.
            Estado final del proceso: Rechazado – modalidad cancelada.
            Motivo registrado: %s.

            La presente decisión se adopta de conformidad con la normativa académica vigente aplicable a las modalidades de grado.

            Para dar continuidad a su proceso académico, deberá postular una nueva modalidad de grado e iniciar nuevamente el procedimiento desde su etapa inicial, cumpliendo con los requisitos y términos establecidos por el programa académico.

            Se recomienda comunicarse con la Jefatura de Programa, con el fin de recibir orientación sobre las alternativas disponibles.

            Este mensaje constituye una notificación automática generada como constancia del cierre definitivo del proceso y para efectos de control y trazabilidad institucional.
            """.formatted(
                    modalidadInfo,
                    documentName,
                    reason
            );
        return greeting(studentName) + body + closingSigma() + "\nUniversidad Surcolombiana";
    }

    public static String modalityClosedByCommittee(String studentName, String modalidadInfo, String programName,
                                                   String committeeMemberName, String committeeMemberLastName,
                                                   String decisionDate, String reason) {
        String body = """
            Nos permitimos informarle que el Comité de Currículo del programa académico ha decidido el cierre de la modalidad de grado, conforme a sus competencias y a la normativa académica vigente.

            A continuación, se relaciona la información correspondiente:

            Modalidad de grado: "%s".
            Programa académico: %s.
            Estado del proceso: Modalidad cerrada.
            Decisión adoptada por: %s %s.
            Fecha de registro de la decisión: %s.
            Motivo del cierre: %s.

            La presente decisión se adopta de conformidad con la normativa académica vigente y las disposiciones institucionales aplicables.

            Para dar continuidad a su proceso académico, se recomienda solicitar orientación ante la Jefatura de Programa, con el fin de recibir asesoría sobre las alternativas disponibles y, en caso de ser procedente, iniciar una nueva modalidad de grado conforme al reglamento institucional.

            Este mensaje constituye una notificación automática generada como constancia de la decisión registrada y para efectos de control y trazabilidad institucional.
            """.formatted(
                    modalidadInfo,
                    programName,
                    committeeMemberName,
                    committeeMemberLastName,
                    decisionDate,
                    reason
            );
        return greeting(studentName) + body + universityClosing();
    }

    public static String modalityInvitationSent(String inviteeName, String modalidadInfo, String programName,
                                                String inviterFullName, String invitationDate, String inviterFirstName) {
        String body = """
                Nos permitimos informarle que ha recibido una invitación para integrarse a una modalidad de grado en la modalidad grupal, conforme a los lineamientos académicos vigentes.

                A continuación, se relaciona la información correspondiente:

                Modalidad de grado: "%s".
                Programa académico: %s.
                Invitación realizada por: %s.
                Fecha de invitación: %s.

                La presente invitación tiene como propósito vincularle a un equipo de trabajo para el desarrollo conjunto de la modalidad de grado. En caso de aceptar, adquirirá los compromisos académicos correspondientes y participará de manera colaborativa en las actividades y entregables definidos dentro del proceso.

                Se recuerda que, de acuerdo con la normativa institucional, solo es posible estar vinculado(a) a una modalidad de grado a la vez.

                Para gestionar la invitación, deberá ingresar a la plataforma institucional, dirigirse a la sección de invitaciones o notificaciones, revisar la información correspondiente y registrar su decisión de aceptación o rechazo.

                Se recomienda establecer comunicación previa con %s, con el fin de asegurar la alineación de expectativas, responsabilidades y objetivos del proyecto académico.

                Este mensaje constituye una notificación automática generada para efectos de control y trazabilidad del proceso académico.
                """.formatted(
                modalidadInfo,
                programName,
                inviterFullName,
                invitationDate,
                inviterFirstName
        );
        return greeting(inviteeName) + body + closingSigma() + "\nUniversidad Surcolombiana";
    }

    public static String modalityInvitationAccepted(String leaderName, String acceptedByFullName, String modalidadInfo,
                                                    String programName, String acceptanceDate) {
        String body = """
                Nos permitimos informarle que un estudiante ha aceptado su invitación para integrarse a la modalidad de grado en la modalidad grupal.

                A continuación, se relaciona la información correspondiente:

                Estudiante: %s.
                Modalidad de grado: "%s".
                Programa académico: %s.
                Fecha de aceptación: %s.

                En consecuencia, el estudiante mencionado ha sido vinculado formalmente a su grupo de trabajo, adquiriendo los derechos y responsabilidades establecidos para el desarrollo de la modalidad de grado.

                Se recomienda coordinar con los integrantes del grupo la asignación de roles, la definición de responsabilidades y la planificación de las actividades académicas, con el fin de garantizar el adecuado desarrollo del proceso.

                Así mismo, se sugiere establecer mecanismos de comunicación efectivos y realizar seguimiento permanente a los avances del proyecto, conforme a los lineamientos institucionales.

                Este mensaje constituye una notificación automática generada como constancia de la vinculación del estudiante y para efectos de control y trazabilidad del proceso académico.
                """.formatted(
                acceptedByFullName,
                modalidadInfo,
                programName,
                acceptanceDate
        );
        return greeting(leaderName) + body + closingSigma() + "\nUniversidad Surcolombiana";
    }

    public static String modalityInvitationRejected(String leaderName, String rejectedByFullName, String modalidadInfo,
                                                    String programName, String rejectionDate, int maxStudents, long activeStudents) {
        String body = """
                Nos permitimos informarle que un estudiante ha registrado el rechazo de la invitación para integrarse a la modalidad de grado en la modalidad grupal.

                A continuación, se relaciona la información correspondiente:

                Estudiante: %s.
                Modalidad de grado: "%s".
                Programa académico: %s.
                Fecha de rechazo: %s.

                En consecuencia, el estudiante mencionado no ha sido vinculado al grupo de trabajo asociado a la modalidad de grado.

                En caso de requerir la conformación o ajuste del grupo, podrá gestionar nuevas invitaciones a estudiantes que cumplan con las condiciones establecidas, o continuar con el desarrollo de la modalidad conforme a la estructura actual del equipo.

                Se recuerda que el número máximo de integrantes permitidos para la modalidad es de %d estudiante(s), incluido usted. Actualmente, el grupo cuenta con %d integrante(s) activo(s).

                Se recomienda coordinar con los integrantes actuales del grupo y definir las acciones pertinentes para garantizar la continuidad y adecuado desarrollo del proceso académico.

                Este mensaje constituye una notificación automática generada como constancia del registro de la decisión y para efectos de control y trazabilidad institucional.
                """.formatted(
                rejectedByFullName,
                modalidadInfo,
                programName,
                rejectionDate,
                maxStudents,
                activeStudents
        );
        return greeting(leaderName) + body + closingSigma() + "\nUniversidad Surcolombiana";
    }

    public static String modalityFinalApprovedByCommittee(String studentName, String studentLastName, String modalidadInfo,
                                                          String programName, String facultyName, String committeeMemberName,
                                                          String committeeMemberLastName, String approvalDate, String observations) {
        return """
                Estimado(a) %s %s:

                Reciba un cordial saludo.

                Nos permitimos informarle que la modalidad de grado relacionada a continuación ha sido aprobada de manera definitiva por el Comité de Currículo del programa académico, conforme a la normativa institucional vigente.

                A continuación, se relaciona la información correspondiente:

                Modalidad de grado: "%s".
                Programa académico: %s.
                Facultad: %s.
                Decisión adoptada por: %s %s (Comité de Currículo).
                Fecha de aprobación: %s.
                %s
                Se adjunta a este correo el acta de aprobación en formato PDF, documento oficial que certifica la culminación satisfactoria de su proceso académico.

                Para la finalización de su proceso de grado, deberá comunicarse con la Jefatura de Programa, con el fin de adelantar los trámites administrativos correspondientes.

                Reciba un reconocimiento institucional por este logro académico.

                Este mensaje constituye una notificación automática generada como constancia de la decisión registrada y para efectos de control y trazabilidad institucional.

                Atentamente,

                Comité de Currículo del Programa Académico
                Sistema de Gestión Académica
                Universidad Surcolombiana
                """.formatted(
                studentName,
                studentLastName,
                modalidadInfo,
                programName,
                facultyName,
                committeeMemberName,
                committeeMemberLastName,
                approvalDate,
                observations
        );
    }

    public static String modalityRejectedByCommittee(String studentName, String modalidadInfo, String programName,
                                                     String decisionDate, String reason) {
        return """
                Estimado(a) %s:

                Reciba un cordial saludo.

                Nos permitimos informarle que, una vez realizada la evaluación por parte del Comité de Currículo del programa académico, no se ha determinado la aprobación de la modalidad de grado.

                A continuación, se relaciona la información correspondiente:

                Modalidad de grado: "%s".
                Programa académico: %s.
                Estado del proceso: No aprobado.
                Fecha de la decisión: %s.
                Motivo de la decisión: %s.

                La presente decisión se adopta de conformidad con la normativa académica vigente y las disposiciones institucionales aplicables.

                Para dar continuidad a su proceso académico, podrá postular una nueva modalidad de grado o solicitar orientación ante la Jefatura de Programa, con el fin de definir las alternativas disponibles conforme a su situación académica.

                Se recomienda revisar los requisitos establecidos para las modalidades de grado y, en caso de requerirlo, solicitar retroalimentación adicional que le permita fortalecer una nueva postulación.

                Este mensaje constituye una notificación automática generada como constancia de la decisión registrada y para efectos de control y trazabilidad institucional.

                Atentamente,

                Comité de Currículo del Programa Académico
                Sistema de Gestión Académica
                Universidad Surcolombiana
                """.formatted(
                studentName,
                modalidadInfo,
                programName,
                decisionDate,
                reason
        );
    }

    public static String seminarStarted(String recipientName, String seminarName, String programName,
                                        String startDate, int totalHours) {
        return String.format("""
                Estimado/a %s,
                
                Le informamos que el seminario "%s" ha iniciado oficialmente.
                
                Detalles del seminario:
                - Nombre: %s
                - Programa: %s
                - Fecha de inicio: %s
                - Intensidad horaria: %d horas
                
                Es importante que esté atento/a a las indicaciones y horarios del seminario.
                Le recordamos que la asistencia es obligatoria (mínimo 80%% de la intensidad horaria).
                
                Cualquier duda o consulta, puede comunicarse con la jefatura del programa.
                
                Cordialmente,
                Sistema de Gestión de Modalidades de Grado - SIGMA
                %s
                Universidad Surcolombiana
                """,
                recipientName,
                seminarName,
                seminarName,
                programName,
                startDate,
                totalHours,
                programName
        );
    }

    public static String seminarCancelled(String recipientName, String seminarName, String programName,
                                          String cancelledDate, String reasonText) {
        return String.format("""
                Estimado/a %s,
                
                Le informamos que el seminario "%s" ha sido CANCELADO.
                
                Detalles del seminario:
                - Nombre: %s
                - Programa: %s
                - Fecha de cancelación: %s
                %s
                
                La inscripción al seminario ha sido suspendida automáticamente.
                Podrá inscribirse a otro seminario disponible cuando lo desee.
                
                Lamentamos los inconvenientes que esto pueda causar.
                
                Cordialmente,
                Sistema de Gestión de Modalidades de Grado - SIGMA
                %s
                Universidad Surcolombiana
                """,
                recipientName,
                seminarName,
                seminarName,
                programName,
                cancelledDate,
                reasonText,
                programName
        );
    }

    public static String modalityApprovedByExaminers(String studentName, String modalityName, String programName,
                                                     String approvalDate) {
        return String.format("""
            Estimado/a %s,

            Reciba un cordial saludo.

            Por medio de la presente se le informa que la siguiente modalidad de grado:

            ───────────────────────────────
            "%s"
            ───────────────────────────────

            ha sido APROBADA por el jurado evaluador designado.

            Programa académico:
            %s

            Estado actual del proceso:
            PROPUESTA APROBADA POR JURADO

            Fecha de aprobación:
            %s

    
            En consecuencia, la modalidad continúa con el desarrollo
            normal del procedimiento académico conforme a los
            lineamientos institucionales vigentes.

            Esta notificación es generada automáticamente por el
            Sistema de Gestión Académica como constancia de la decisión registrada.

            Sistema de Gestión Académica – SIGMA
            Universidad Surcolombiana
            """,
                studentName,
                modalityName,
                programName,
                approvalDate
        );
    }

    public static String examinersAssigned(String studentFullName, String modalidadInfo, String programName,
                                           String jurados, String assignmentDate) {
        String body = """
            Nos permitimos informarle que han sido designados oficialmente los jurados evaluadores para su modalidad de grado, conforme al proceso académico establecido.

            A continuación, se relaciona la información correspondiente:

            Modalidad de grado: "%s".
            Programa académico: %s.
            Jurados asignados: %s.
            Fecha de asignación: %s.

            Los jurados designados serán responsables de la evaluación académica de su trabajo, conforme a los lineamientos institucionales vigentes.

            Se recomienda consultar periódicamente la plataforma institucional, con el fin de hacer seguimiento al estado y avance del proceso académico.

            Este mensaje constituye una notificación automática generada como constancia de la asignación realizada y para efectos de control y trazabilidad institucional.
            """;
        body = String.format(body,
                modalidadInfo,
                programName,
                jurados,
                assignmentDate
        );
        return greeting(studentFullName) + body + universityClosing();
    }

    public static String documentEditApproved(String studentName, String documentName, String resolutionNotes) {
        String body = """
        Nos permitimos informarle que la solicitud de edición del documento ha sido aprobada por el jurado evaluador, conforme al proceso de revisión académica.

        A continuación, se relaciona la información correspondiente:

        Documento: "%s".
        Observaciones del jurado: %s.

        En virtud de esta decisión, podrá ingresar a la plataforma institucional y realizar la carga de la versión actualizada del documento. Una vez registrada, la nueva versión será objeto de evaluación por parte del jurado designado.

        Este mensaje constituye una notificación automática generada como constancia de la decisión adoptada y para efectos de control y trazabilidad institucional.
        """.formatted(
                        documentName,
                        resolutionNotes
                );
        return greeting(studentName) + body + closingSigma() + "\nUniversidad Surcolombiana";
    }

    public static String documentEditRejected(String studentName, String documentName, String resolutionNotes) {
        String body = """
        Nos permitimos informarle que la solicitud de edición del documento no ha sido aprobada por el jurado evaluador, conforme al proceso de revisión académica.

        A continuación, se relaciona la información correspondiente:

        Documento: "%s".
        Motivo de la decisión: %s.

        En consecuencia, el documento conserva su estado actual dentro del proceso académico. En caso de requerir aclaraciones adicionales, podrá comunicarse con la Jefatura de Programa o con el Director de Proyecto.

        Este mensaje constituye una notificación automática generada como constancia de la decisión adoptada y para efectos de control y trazabilidad institucional.
        """.formatted(
                        documentName,
                        resolutionNotes
                );
        return greeting(studentName) + body + closingSigma() + "\nUniversidad Surcolombiana";
    }
}
