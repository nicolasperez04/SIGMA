package com.SIGMA.USCO.notifications.listeners;

public class NotificationMessageTemplates {

    public static final String EXAMINER_DESIGNATION_SUBJECT = "Designación oficial como Jurado Evaluador – Modalidad de Grado";
    public static final String DIRECTOR_EXAMINERS_ASSIGNED_SUBJECT = "Jurados asignados a modalidad bajo su dirección – SIGMA";
    public static final String EXAMINER_DEFENSE_READY_SUBJECT = "Notificación de modalidad lista para revisión final por parte de los jurados";
    public static final String EXAMINER_FINAL_REVIEW_COMPLETED_SUBJECT = "Aprobación final de documentos – Puede programar la sustentación";
    public static final String EXAMINER_DEFENSE_SCHEDULED_SUBJECT = "Sustentación programada – Modalidad de Grado";
    public static final String EXAMINER_DOCUMENT_EDIT_REQUESTED_SUBJECT = "Solicitud de edición de documento aprobado – Modalidad de grado";
    public static final String EXAMINER_CORRECTION_RESUBMITTED_SUBJECT = "Documento corregido reenviado por el estudiante – Modalidad de grado";
    public static final String EXAMINER_PARTICIPATION_ACT_SUBJECT = "Acta de Participación – Modalidad de Grado Completada";
    public static final String PROGRAM_HEAD_MODALITY_STARTED_SUBJECT = "Nueva modalidad iniciada - Estudiantes asociados";
    public static final String PROGRAM_HEAD_DOCUMENT_UPDATED_SUBJECT = "Documento actualizado por estudiante";
    public static final String PROGRAM_HEAD_DEFENSE_SCHEDULED_SUBJECT = "Sustentación programada - Estudiantes asociados";
    public static final String PROGRAM_HEAD_DIRECTOR_ASSIGNED_SUBJECT = "Nuevo director asignado - Estudiantes asociados";
    public static final String PROGRAM_HEAD_FINAL_DEFENSE_RESULT_SUBJECT = "Resultado de la defensa final - Estudiantes asociados";
    public static final String DIRECTOR_CANCELLATION_SUBJECT = "Concepto del Director de Proyecto sobre solicitud de cancelación de modalidad";
    public static final String DIRECTOR_CANCELLATION_REQUESTED_SUBJECT = "Solicitud de cancelación de modalidad recibida";
    public static final String DIRECTOR_ASSIGNED_SUBJECT = "Asignación como Director de Proyecto a modalidad de grado";
    public static final String DIRECTOR_FINAL_DEFENSE_RESULT_SUBJECT = "Resultado de la sustentación final – Estudiantes asignados";
    public static final String DIRECTOR_STUDENT_DOCUMENT_UPDATED_SUBJECT = "Documento actualizado – Estudiante asignado";
    public static final String COMMITTEE_CANCELLATION_REQUESTED_SUBJECT = "Solicitud de cancelación de modalidad";
    public static final String COMMITTEE_MODALITY_APPROVED_SUBJECT = "Modalidad de grado aprobada por Jefatura de Programa";
    public static final String COMMITTEE_DOCUMENT_UPDATED_SUBJECT = "Documento actualizado – Modalidad en revisión";
    public static final String COMMITTEE_CORRECTION_RESUBMITTED_SUBJECT = "Documento corregido reenviado – Revisión del Comité de Currículo";

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

    public static String directorCorrectionsRequested(String directorName, String studentName, String documentName, String observations) {
        String body = """
        Nos permitimos informarle que el jurado evaluador ha solicitado la realización de correcciones en uno de los documentos asociados a la modalidad de grado del estudiante %s, en el marco del proceso de revisión académica.

        A continuación, se detalla la información correspondiente:

        Estudiante: %s.
        Documento: "%s".
        Observaciones registradas: %s.

        En este sentido, se solicita orientar al estudiante en el cumplimiento de las observaciones indicadas, con el fin de dar continuidad al proceso académico dentro de los plazos establecidos.

        Este mensaje constituye una notificación automática generada para efectos de control y trazabilidad del proceso.
        """.formatted(
                    studentName,
                    studentName,
                    documentName,
                    observations
            );
        return greeting(directorName) + body + closing();
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

    public static String directorCorrectionRejectedFinal(String directorName, String studentName, String modalidadInfo, String documentName, String reason) {
        String body = """
            Nos permitimos informarle que, como resultado de la evaluación realizada por el jurado designado, no se ha determinado la aprobación de uno o más documentos asociados a la modalidad de grado del estudiante %s. En consecuencia, se ha dispuesto la cancelación definitiva del proceso académico correspondiente.

            A continuación, se relaciona la información pertinente:

            Modalidad de grado: "%s".
            Documento evaluado: %s.
            Estado final del proceso: Rechazado – modalidad cancelada.
            Motivo registrado: %s.

            La presente decisión se adopta de conformidad con la normativa académica vigente aplicable a las modalidades de grado.

            En este sentido, se solicita orientar al estudiante sobre las alternativas disponibles para dar continuidad a su proceso académico, de conformidad con los lineamientos institucionales.

            Este mensaje constituye una notificación automática generada como constancia del cierre definitivo del proceso y para efectos de control y trazabilidad institucional.
            """.formatted(
                    studentName,
                    modalidadInfo,
                    documentName,
                    reason
            );
        return greeting(directorName) + body + closingSigma() + "\nUniversidad Surcolombiana";
    }

    public static String tiebreakerRequired(String studentName, String documentName) {
        String body = """
            Nos permitimos informarle que, durante la evaluación del documento "%s" asociado a su modalidad de grado, los jurados evaluadores han emitido decisiones divergentes.

            En consecuencia, se ha dispuesto la intervención de un jurado de desempate, quien emitirá la decisión definitiva conforme a la normativa académica vigente.

            Se recomienda consultar periódicamente la plataforma institucional, ya que a través de este medio se comunicará oportunamente el resultado de dicha evaluación.

            Este mensaje constituye una notificación automática generada para efectos de control y trazabilidad del proceso.
            """.formatted(
                    documentName
            );
        return greeting(studentName) + body + closingSigma();
    }

    public static String directorTiebreakerRequired(String directorName, String studentName, String documentName) {
        String body = """
            Nos permitimos informarle que, durante la evaluación del documento "%s" asociado a la modalidad de grado del estudiante %s, los jurados evaluadores han emitido decisiones divergentes.

            En consecuencia, se ha dispuesto la intervención de un jurado de desempate, quien emitirá la decisión definitiva conforme a la normativa académica vigente.

            En este sentido, se solicita orientar al estudiante durante el desarrollo de esta etapa del proceso académico.

            Este mensaje constituye una notificación automática generada para efectos de control y trazabilidad del proceso.
            """.formatted(
                    documentName,
                    studentName
            );
        return greeting(directorName) + body + closingSigma();
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
                                                     String approvalDate, String studentList) {
        String studentsSection = studentList == null || studentList.isBlank()
                ? ""
                : """
                
                Estudiantes asociados a la modalidad (nombre y correo electrónico):
                %s

                """;
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
                studentsSection.formatted(studentList),
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

    public static String examinerDesignation(String examinerName, String examinerLastName, String examinerRoleLabel,
                                             String modalidadInfo, String programName, String facultyName,
                                             String studentsString, String directorName, String fechaString) {
        return """
        Estimado(a) %s %s:

        Reciba un cordial saludo.

        Nos permitimos informarle que ha sido designado(a) oficialmente como %s en el proceso de evaluación de la modalidad de grado, conforme a las disposiciones académicas vigentes.

        A continuación, se relaciona la información pertinente:

        Modalidad de grado: "%s".
        Programa académico: %s.
        Facultad: %s.
        Estudiantes asociados: %s.
        Director de proyecto: %s.
        Fecha de asignación: %s.

        En el marco de esta designación, le corresponde realizar la evaluación académica de la modalidad de grado, conforme a los lineamientos institucionales establecidos y dentro de los plazos definidos por el programa académico.

        Podrá consultar la información completa de la modalidad y gestionar las actividades asociadas a su rol a través de la plataforma institucional.

        Este mensaje constituye una notificación automática generada como constancia de la designación realizada y para efectos de control y trazabilidad institucional.

        Atentamente,

        Sistema de Gestión Académica
        Universidad Surcolombiana
        """.formatted(
                examinerName,
                examinerLastName,
                examinerRoleLabel,
                modalidadInfo,
                programName,
                facultyName,
                studentsString,
                directorName,
                fechaString
        );
    }

    public static String directorExaminersAssigned(String directorFullName, String modalidadInfo, String programName,
                                                   String facultyName, String studentsString, String examinersList,
                                                   String fechaString) {
        return """
        Estimado(a) %s:

        Reciba un cordial saludo.

        Nos permitimos informarle que el Comité de Currículo del programa académico ha designado oficialmente los jurados evaluadores para la modalidad de grado bajo su dirección, conforme a la normativa institucional vigente.

        A continuación, se relaciona la información pertinente:

        Modalidad de grado: "%s".
        Programa académico: %s.
        Facultad: %s.
        Estudiantes asociados: %s.
        Jurados asignados: %s.
        Fecha de asignación: %s.

        En virtud de esta designación, los jurados iniciarán el proceso de revisión y evaluación de la documentación académica asociada a la modalidad de grado, conforme a los lineamientos institucionales establecidos.

        En su calidad de Director de Proyecto, se recomienda realizar el seguimiento académico correspondiente, con el fin de garantizar el cumplimiento de los requisitos y la adecuada atención a las observaciones que se deriven del proceso evaluativo.

        Podrá consultar el detalle completo a través de la plataforma institucional.

        Este mensaje constituye una notificación automática generada como constancia de la asignación realizada y para efectos de control y trazabilidad institucional.

        Atentamente,

        Sistema de Gestión Académica
        Universidad Surcolombiana
        """.formatted(
                directorFullName,
                modalidadInfo,
                programName,
                facultyName,
                studentsString,
                examinersList,
                fechaString
        );
    }

    public static String examinerDefenseReady(String examinerName, String examinerLastName, String miembros, String modalidadInfo) {
        return """
        Estimado(a) %s %s:

        Reciba un cordial saludo.

        Nos permitimos informarle que la modalidad de grado relacionada a continuación ha sido registrada como lista para revisión final por parte de los jurados, conforme al proceso académico establecido.

        A continuación, se relaciona la información pertinente:

        Estudiantes asociados: %s.
        Modalidad de grado: "%s".

        En virtud de esta actuación, el proceso se encuentra disponible para su revisión en calidad de jurado evaluador, conforme a los lineamientos institucionales vigentes.

        Podrá consultar la documentación final presentada y realizar el proceso de evaluación correspondiente a través de la plataforma institucional.

        Este mensaje constituye una notificación automática generada como constancia del estado registrado y para efectos de control y trazabilidad institucional.

        Atentamente,

        Sistema de Gestión Académica
        Universidad Surcolombiana
        """.formatted(
                examinerName,
                examinerLastName,
                miembros,
                modalidadInfo
        );
    }

    public static String examinerFinalReviewCompleted(String examinerName, String examinerLastName, String miembros, String modalidadInfo) {
        return """
        Estimado(a) %s %s:

        Reciba un cordial saludo.

        Nos permitimos informarle que el jurado evaluador ha aprobado la totalidad de los documentos requeridos para la modalidad de grado, conforme al proceso académico establecido.

        A continuación, se relaciona la información pertinente:

        Estudiantes asociados: %s.
        Modalidad de grado: "%s".

        En virtud de esta aprobación, el proceso académico cumple con los requisitos necesarios para avanzar a la etapa de sustentación.

        En su calidad de Director de Proyecto, corresponde continuar con la gestión académica asociada a la programación y desarrollo de la sustentación, conforme a los lineamientos institucionales vigentes.

        Podrá realizar las acciones correspondientes y consultar el detalle del proceso a través de la plataforma institucional.

        Este mensaje constituye una notificación automática generada como constancia del estado registrado y para efectos de control y trazabilidad institucional.

        Atentamente,

        Sistema de Gestión Académica
        Universidad Surcolombiana
        """.formatted(
                examinerName,
                examinerLastName,
                miembros,
                modalidadInfo
        );
    }

    public static String examinerDefenseScheduled(String examinerName, String examinerLastName, String modalidadInfo,
                                                  String defenseDate, String defenseLocation, String directorName,
                                                  String studentsList) {
        return """
        Estimado(a) %s %s:

        Reciba un cordial saludo.

        Nos permitimos informarle que ha sido programada la sustentación correspondiente a la modalidad de grado, conforme al proceso académico establecido.

        A continuación, se relaciona la información pertinente:

        Modalidad de grado: "%s".
        Fecha y hora de la sustentación: %s.
        Lugar: %s.
        Director de proyecto: %s.
        Estudiantes asociados: %s.

        En virtud de esta programación, la sustentación se desarrollará conforme a los lineamientos institucionales vigentes, en el marco del proceso de evaluación académica.

        Podrá consultar la documentación final y el detalle del proceso a través de la plataforma institucional.

        Este mensaje constituye una notificación automática generada como constancia de la programación registrada y para efectos de control y trazabilidad institucional.

        Atentamente,

        Sistema de Gestión Académica
        Universidad Surcolombiana
        """.formatted(
                examinerName,
                examinerLastName,
                modalidadInfo,
                defenseDate,
                defenseLocation,
                directorName,
                studentsList
        );
    }

    public static String examinerDocumentEditRequested(String examinerName, String examinerLastName, String modalidadInfo,
                                                       String programName, String studentNames, String documentName,
                                                       long editRequestId, String reason) {
        return """
        Estimado(a) %s %s:

        Reciba un cordial saludo.

        Nos permitimos informarle que los estudiantes asociados a la modalidad de grado han registrado una solicitud de edición sobre un documento previamente aprobado, conforme al procedimiento académico establecido.

        A continuación, se relaciona la información pertinente:

        Modalidad de grado: "%s".
        Programa académico: "%s".
        Estudiantes asociados: %s.
        Documento: "%s".
        Identificador de la solicitud: %d.
        Motivo de la solicitud: %s.

        En virtud de esta solicitud, el caso se encuentra disponible para su revisión en calidad de jurado evaluador, conforme a los lineamientos institucionales vigentes.

        Podrá consultar el detalle de la solicitud y emitir el concepto correspondiente a través de la plataforma institucional.

        Este mensaje constituye una notificación automática generada como constancia del registro efectuado y para efectos de control y trazabilidad institucional.

        Atentamente,

        Sistema de Gestión Académica
        Universidad Surcolombiana
        """.formatted(
                examinerName,
                examinerLastName,
                modalidadInfo,
                programName,
                studentNames,
                documentName,
                editRequestId,
                reason
        );
    }

    public static String examinerCorrectionResubmitted(String examinerName, String examinerLastName, String modalidadInfo,
                                                       String programName, String studentNames, String documentName) {
        return """
        Estimado(a) %s %s:

        Reciba un cordial saludo.

        Nos permitimos informarle que el estudiante ha registrado el envío de un documento corregido, en el marco del proceso de revisión académica de su modalidad de grado.

        A continuación, se relaciona la información pertinente:

        Modalidad de grado: "%s".
        Programa académico: "%s".
        Estudiantes asociados: %s.
        Documento corregido: "%s".

        En virtud de este envío, el documento corregido se encuentra disponible para su revisión en calidad de jurado evaluador, conforme a los lineamientos institucionales vigentes.

        Podrá consultar el documento y emitir el concepto correspondiente a través de la plataforma institucional.

        Este mensaje constituye una notificación automática generada como constancia del envío efectuado y para efectos de control y trazabilidad institucional.

        Atentamente,

        Sistema de Gestión Académica
        Universidad Surcolombiana
        """.formatted(
                examinerName,
                examinerLastName,
                modalidadInfo,
                programName,
                studentNames,
                documentName
        );
    }

    public static String examinerParticipationAct(String examinerName, String examinerRole, String modalidadInfo,
                                                  String programName, String facultyName, String studentNames) {
        return """
        Estimado(a) %s:

        Reciba un cordial saludo.

        Nos permitimos informarle que la sustentación correspondiente a la modalidad de grado en la cual usted participó en calidad de %s ha finalizado y su resultado ha sido registrado oficialmente en el sistema.

        A continuación, se relaciona la información pertinente:

        Modalidad de grado: "%s".
        Programa académico: %s.
        Facultad: %s.
        Resultado: APROBADA.
        Estudiantes asociados: %s.

        En el marco de este proceso, su participación como %s quedó registrada en las diferentes etapas de evaluación académica, conforme a los lineamientos institucionales vigentes.

        Se adjunta a la presente comunicación el acta de participación en formato PDF, documento oficial que certifica su intervención en el proceso evaluativo y que forma parte del registro institucional de control y trazabilidad académica.

        Este mensaje constituye una notificación automática generada como constancia del cierre del proceso.

        Atentamente,

        Sistema de Gestión Académica
        Universidad Surcolombiana
        """.formatted(
                examinerName,
                examinerRole,
                modalidadInfo,
                programName,
                facultyName,
                studentNames,
                examinerRole
        );
    }

    public static String programHeadModalityStarted(String modalidadInfo, String studentList) {
        return """
         Estimado/a Jefatura de Programa,

        Reciba un cordial saludo.

        Le informamos que ha sido registrada oficialmente en el sistema una nueva modalidad de grado con el siguiente detalle:

        Modalidad de grado:
        "%s"

        Estudiantes asociados:
        %s

        A partir de este registro, el proceso académico correspondiente queda activo y disponible para su revisión.

        Se solicita amablemente verificar la información ingresada y proceder con la validación institucional conforme a la normativa vigente.

        Puede consultar los detalles completos en la plataforma.

        Cordialmente,
        Sistema de Gestión Académica
        """.formatted(
                modalidadInfo,
                studentList
        );
    }

    public static String programHeadDocumentUpdated(String studentFullName, String studentEmail, String modalidadInfo,
                                                    String docName, String statusText) {
        return """
        Estimado(a) Jefatura de Programa:

        Reciba un cordial saludo.

        Nos permitimos informar que un estudiante ha realizado la actualización de un documento previamente solicitado, en el marco del proceso académico correspondiente.

        A continuación, se relaciona la información pertinente:

        Nombre del estudiante: %s.
        Correo institucional: %s.
        Modalidad de grado: "%s".
        Documento actualizado: "%s".
        Estado actual del documento: %s.

        En este sentido, se solicita ingresar a la plataforma institucional, con el fin de revisar el documento actualizado y continuar con el trámite correspondiente, conforme a la normativa académica vigente.

        Este mensaje constituye una notificación automática generada como constancia de la actualización registrada y para efectos de control y trazabilidad institucional.

        Atentamente,

        Sistema de Gestión Académica
        Universidad Surcolombiana
        """.formatted(
                studentFullName,
                studentEmail,
                modalidadInfo,
                docName,
                statusText
        );
    }

    public static String programHeadDefenseScheduled(String modalidadInfo, String studentList, String defenseDateText, String defenseLocation) {
        return """
        Estimada Jefatura de Programa:

        Reciba un cordial saludo.

        Nos permitimos informar que ha sido programada oficialmente la sustentación correspondiente a la modalidad de grado, conforme al proceso académico establecido.

        A continuación, se relaciona la información pertinente:

        Modalidad de grado: "%s".
        Estudiantes asociados: %s.
        Fecha y hora de la sustentación: %s.
        Lugar: %s.

        En este sentido, se solicita adoptar las medidas académicas y logísticas necesarias, con el fin de garantizar el adecuado desarrollo de la sustentación conforme a la normativa institucional vigente.

        Podrá consultar información adicional a través de la plataforma institucional.

        Este mensaje constituye una notificación automática generada como constancia de la programación realizada y para efectos de control y trazabilidad institucional.

Atentamente,

        Sistema de Gestión Académica
        Universidad Surcolombiana
        """.formatted(
                modalidadInfo,
                studentList,
                defenseDateText,
                defenseLocation
        );
    }

    public static String studentFinalReviewCompleted(String studentName, String modalidadInfo) {
        String body = """
        Nos permitimos informarle que el jurado evaluador ha aprobado la totalidad de los documentos requeridos para su modalidad de grado, conforme al proceso académico establecido.

        A continuación, se relaciona la información pertinente:

        Modalidad de grado: "%s".

        En virtud de esta aprobación, el proceso académico cumple con los requisitos necesarios para avanzar a la etapa de sustentación.

        El Director de Proyecto podrá proceder con la programación y desarrollo de la sustentación, conforme a los lineamientos institucionales vigentes.

        Se recomienda consultar periódicamente la plataforma institucional para conocer la fecha y demás detalles de la sustentación, así como las novedades del proceso.

        Este mensaje constituye una notificación automática generada como constancia del estado registrado y para efectos de control y trazabilidad institucional.
        """.formatted(
                modalidadInfo
        );
        return greeting(studentName) + body + universityClosing();
    }

    public static String programHeadDirectorAssigned(String modalidadInfo, String studentList, String directorName) {
        return """
        Estimada Jefatura de Programa:

        Reciba un cordial saludo.

        Nos permitimos informar que ha sido registrada la asignación de un director para la modalidad de grado, conforme al proceso académico establecido.

        A continuación, se relaciona la información pertinente:

        Modalidad de grado: "%s".
        Estudiantes asociados: %s.
        Director asignado: %s.

        En virtud de esta asignación, el director designado podrá iniciar el acompañamiento académico correspondiente, conforme a los lineamientos institucionales vigentes.

        Podrá consultar el detalle completo a través de la plataforma institucional.

        Este mensaje constituye una notificación automática generada como constancia de la asignación realizada y para efectos de control y trazabilidad institucional.

        Atentamente,

        Sistema de Gestión Académica
        Universidad Surcolombiana
        """.formatted(
                modalidadInfo,
                studentList,
                directorName
        );
    }

    public static String programHeadFinalDefenseResult(String directorName, String studentList, String modalidadInfo,
                                                       String statusText, String distinctionText, String observations) {
        return """
        Estimado(a) %s:

        Reciba un cordial saludo.

        Nos permitimos informar que ha concluido la sustentación final correspondiente a la modalidad de grado bajo su dirección, conforme al proceso académico establecido.

        A continuación, se relaciona la información pertinente:

        Estudiantes asociados: %s.
        Modalidad de grado: "%s".
        Resultado de la sustentación: %s.
        Distinción académica: %s.
        Observaciones del jurado: %s.

        El resultado ha sido registrado oficialmente en el sistema. Podrá consultar el detalle completo y la documentación asociada a través de la plataforma institucional.

        Este mensaje constituye una notificación automática generada como constancia del resultado registrado y para efectos de control y trazabilidad institucional.

        Atentamente,

        Sistema de Gestión Académica
        Universidad Surcolombiana
        """.formatted(
                directorName,
                studentList,
                modalidadInfo,
                statusText,
                distinctionText,
                observations
        );
    }

    public static String programHeadModalityApprovedSubject(String leaderFullName) {
        return "Modalidad aprobada por el comité de currículo de programa - Estudiante: " + leaderFullName;
    }

    public static String programHeadModalityApproved(String leaderName, String leaderEmail, String modalidadInfo, String selectionDate) {
        return """
        Estimada Jefatura de Programa:

        Reciba un cordial saludo.

        Nos permitimos informar que la modalidad de grado ha sido aprobada oficialmente por el Comité de Currículo del programa académico, conforme a la normativa institucional vigente.

        A continuación, se relaciona la información pertinente:

        Nombre del estudiante: %s.
        Correo institucional: %s.
        Modalidad de grado: "%s".
        Fecha de aprobación: %s.

        La decisión ha sido registrada en el sistema y el proceso académico continúa conforme a los lineamientos establecidos.

        Podrá consultar el detalle completo a través de la plataforma institucional.

        Este mensaje constituye una notificación automática generada como constancia de la decisión registrada y para efectos de control y trazabilidad institucional.

        Atentamente,

        Sistema de Gestión Académica – SIGMA
        Universidad Surcolombiana
        """.formatted(
                leaderName,
                leaderEmail,
                modalidadInfo,
                selectionDate
        );
    }

    public static String programHeadFinalReviewReadySubject(String modalidadInfo) {
        return "Documentos finales listos para revisión - " + modalidadInfo;
    }

    public static String programHeadFinalReviewReady(String directorNombre, String modalidadInfo, String studentList) {
        return """
        Estimada Jefatura de Programa:

        Reciba un cordial saludo.

        Nos permitimos informar que el Director de Proyecto %s ha registrado que los documentos finales de la modalidad de grado se encuentran disponibles para su revisión institucional previa a la sustentación, conforme al proceso académico establecido.

        A continuación, se relaciona la información pertinente:

        Modalidad de grado: "%s".
        Estudiantes asociados: %s.

        En este sentido, se solicita ingresar a la plataforma institucional con el fin de verificar la documentación final y, una vez validada, proceder con la notificación a los jurados evaluadores para dar continuidad al proceso de sustentación.

        Este mensaje constituye una notificación automática generada como constancia de la actuación registrada y para efectos de control y trazabilidad institucional.

        Atentamente,

        Sistema de Gestión Académica
        Universidad Surcolombiana
        """.formatted(
                directorNombre,
                modalidadInfo,
                studentList
        );
    }

    public static String directorCancellationApproved(String directorName, String modalidadInfo, String miembros) {
        return """
                Estimado/a %s,

                        Reciba un cordial saludo.

                        Le informamos que el/la Director/a del proyecto ha emitido un concepto favorable
                        respecto a la solicitud de cancelación de la siguiente modalidad de grado:

                        Modalidad:
                        "%s"

                        Estudiantes vinculados al proceso:
                        %s

                        De acuerdo con el procedimiento académico institucional, la solicitud será ahora
                        remitida al Comité de Currículo del programa académico, instancia que realizará
                        la evaluación correspondiente y emitirá la decisión definitiva sobre la cancelación
                        de la modalidad de grado.

                        El comité podrá determinar la aprobación o el rechazo de la solicitud, decisión
                        que será notificada oportunamente a través del sistema institucional.

                        Esta comunicación se emite con el fin de mantener la trazabilidad y el registro
                        formal del proceso académico asociado a las modalidades de grado.

                        Atentamente,

                        Sistema de Gestión de Modalidades de Grado
                        Universidad Surcolombiana


                """.formatted(
                directorName,
                modalidadInfo,
                miembros
        );
    }

    public static String directorCancellationRejected(String directorName, String modalidadInfo, String miembros) {
        return """
                Estimado/a %s,

                        Reciba un cordial saludo.

                        Le informamos que el/la Director/a del proyecto ha evaluado la solicitud de
                        cancelación correspondiente a la siguiente modalidad de grado:

                        Modalidad:
                        "%s"

                        Estudiantes vinculados al proceso:
                        %s

                        Después de realizar la revisión correspondiente, el/la Director/a del proyecto
                        ha emitido un concepto no favorable respecto a la solicitud de cancelación.

                        En consecuencia, la solicitud no será remitida al Comité de Currículo y
                        la modalidad de grado continuará su desarrollo conforme al estado académico
                        vigente y a los lineamientos institucionales establecidos para el proceso
                        de modalidades de grado.

                        Esta notificación se emite con el fin de mantener la trazabilidad y el
                        registro formal del proceso académico dentro del sistema institucional.

                        Si requiere información adicional o desea realizar seguimiento al caso,
                        puede comunicarse con la Jefatura del Programa Académico correspondiente.

                        Atentamente,

                        Sistema de Gestión de Modalidades de Grado 
                        Universidad Surcolombiana


                """.formatted(
                directorName,
                modalidadInfo,
                miembros
        );
    }

    public static String directorCancellationRequested(String directorName, String modalidadInfo, String programName,
                                                       String leaderName, String semester, String studentCode,
                                                       String leaderEmail, String miembros) {
        return """
                Estimado/a %s,

                Reciba un cordial saludo.

                Le informamos que se ha registrado una solicitud formal de cancelación
                correspondiente a la siguiente modalidad de grado:

                ───────────────────────────────
                Modalidad: "%s"
                ───────────────────────────────

                Programa académico: %s

                Datos del estudiante (líder):
                - Nombre completo: %s
                - Semestre: %s
                - Código estudiantil: %s
                - Correo electrónico: %s

                Estudiantes vinculados al proceso:
                %s

                De acuerdo con el procedimiento académico establecido, esta solicitud
                requiere su revisión y concepto en calidad de Director/a de Proyecto.
                Una vez emitida su valoración, el caso será remitido al Comité de
                Currículo del Programa para su análisis y decisión final.

                Le agradecemos realizar la revisión correspondiente dentro de los
                plazos institucionales establecidos y efectuar el seguimiento del
                proceso a través del sistema.

                Atentamente,

                Sistema SIGMA
                Sistema de Gestión de Modalidades de Grado
                Universidad Surcolombiana

                """.formatted(
                directorName,
                modalidadInfo,
                programName,
                leaderName,
                semester,
                studentCode,
                leaderEmail,
                miembros
        );
    }

    public static String directorAssigned(String directorName, String modalidadInfo, String programName,
                                          String miembros, String updatedAtText) {
        return """
                Estimado/a %s,

                Reciba un cordial saludo.

                Le informamos que ha sido designado/a oficialmente como Director/a de Proyecto
                para la siguiente modalidad de grado, conforme al registro realizado en el
                sistema institucional.

                A continuación, se presentan los datos asociados al proceso:

                Modalidad de grado:
                "%s"

                Programa académico:
                "%s"

                Estudiantes vinculados al proyecto:
                %s

                Fecha de asignación:
                %s

                A partir de esta designación, usted asume la responsabilidad de orientar,
                supervisar y acompañar el desarrollo académico del proyecto de grado,
                garantizando el cumplimiento de los lineamientos, cronogramas y criterios
                de evaluación establecidos por el programa académico.

                Le recomendamos ingresar al sistema para consultar la información completa
                de la modalidad y realizar el seguimiento correspondiente al proceso.

                Atentamente,


                Sistema de Gestión de Modalidades de Grado
                Universidad Surcolombiana

                """.formatted(
                directorName,
                modalidadInfo,
                programName,
                miembros,
                updatedAtText
        );
    }

    public static String directorFinalDefenseResult(String directorName, String studentList, String modalidadInfo,
                                                    String statusText, String distinctionText, String observations) {
        return """
                Estimado/a %s,

                Reciba un cordial saludo.

                Le informamos que la sustentación final correspondiente a la modalidad
                de grado bajo su dirección académica ha sido realizada y registrada
                oficialmente en el sistema.

                A continuación, se presentan los detalles del proceso:

                Modalidad de grado:
                "%s"

                Estudiantes:
                %s

                Resultado final:
                %s

                Distinción académica:
                %s

                Observaciones del jurado o comité evaluador:
                %s

                Este resultado marca la finalización del proceso académico de
                sustentación. En su calidad de Director/a de Proyecto, le recomendamos
                verificar el estado actualizado de la modalidad en el sistema y, si
                corresponde, coordinar los trámites académicos y administrativos
                posteriores con la jefatura del programa.

                Agradecemos el acompañamiento y la orientación brindados durante el
                desarrollo del proyecto de grado.

                Atentamente,

                Sistema SIGMA
                Sistema de Gestión de Modalidades de Grado
                Universidad Surcolombiana
                """.formatted(
                directorName,
                studentList,
                modalidadInfo,
                statusText,
                distinctionText,
                observations
        );
    }

    public static String directorStudentDocumentUpdated(String directorName, String studentFullName, String modalidadInfo,
                                                        String docName, String statusText) {
        return """
                Estimado/a %s,

                Reciba un cordial saludo.

                Le informamos que el estudiante %s ha realizado una actualización en uno de los documentos asociados a la modalidad de grado que actualmente se encuentra bajo su dirección académica.

                A continuación, se detallan los datos correspondientes:

                Modalidad de grado:
                "%s"

                Documento actualizado:
                "%s"

                Estado actual del documento:
                %s

                Esta actualización puede requerir su revisión, validación o retroalimentación según el estado reportado y la fase del proceso académico.

                Le invitamos a ingresar a la plataforma institucional para consultar la versión más reciente del documento y continuar con el seguimiento académico correspondiente.

                Cordialmente,
                Sistema de Gestión Académica
                """.formatted(
                directorName,
                studentFullName,
                modalidadInfo,
                docName,
                statusText
        );
    }

    public static String committeeCancellationRequested(String modalidadInfo, String leaderName, String programName,
                                                        String semester, String studentCode, String leaderEmail,
                                                        String studentList) {
        return """
                Estimado(a) Comité de Currículo del Programa,

                Reciba un cordial saludo.

                Se informa que el siguiente estudiante ha solicitado la cancelación
                de su modalidad de grado. A continuación se detalla la información
                del estudiante y de la modalidad:

                ───────────────────────────────
                Modalidad de grado: "%s"
                ───────────────────────────────

                Datos del estudiante (líder):
                - Nombre completo: %s
                - Programa académico: %s
                - Semestre: %s
                - Código estudiantil: %s
                - Correo electrónico: %s

                Estudiantes integrantes de la modalidad:
                %s

                En consecuencia, la solicitud será revisada y gestionada por el
                Comité de Currículo del Programa.

                Se solicita a los miembros del comité proceder con las etapas
                correspondientes del proceso académico, de acuerdo con las funciones
                y responsabilidades establecidas.

                Por favor, ingrese al sistema para consultar los detalles de la
                modalidad registrada y continuar con el flujo de evaluación y seguimiento.

                Sistema SIGMA
                Plataforma de Gestión de Modalidades de Grado

                """.formatted(
                modalidadInfo,
                leaderName,
                programName,
                semester,
                studentCode,
                leaderEmail,
                studentList
        );
    }

    public static String committeeModalityApproved(String modalidadInfo, String leaderName, String programName,
                                                  String semester, String studentCode, String leaderEmail,
                                                  String studentList) {
        return """
                Estimado(a) Comité de Currículo del Programa,

                Reciba un cordial saludo.

                Se informa que la siguiente modalidad de grado ha sido aprobada
                por la Jefatura del Programa y queda a disposición del Comité
                para continuar con las etapas correspondientes del proceso académico.
                A continuación se detalla la información del estudiante y de la modalidad:

                ───────────────────────────────
                Modalidad de grado: "%s"
                ───────────────────────────────

                Datos del estudiante (líder):
                - Nombre completo: %s
                - Programa académico: %s
                - Semestre: %s
                - Código estudiantil: %s
                - Correo electrónico: %s

                Estudiantes integrantes de la modalidad:
                %s

                En consecuencia, se solicita a los miembros del comité proceder
                con las siguientes etapas del proceso académico, de acuerdo con las
                funciones y responsabilidades establecidas.

                Por favor, ingrese al sistema para consultar los detalles de la
                modalidad registrada y continuar con el flujo de evaluación y seguimiento.

                Sistema SIGMA
                Plataforma de Gestión de Modalidades de Grado

                """.formatted(
                modalidadInfo,
                leaderName,
                programName,
                semester,
                studentCode,
                leaderEmail,
                studentList
        );
    }

    public static String committeeDocumentUpdated(String studentFullName, String modalidadInfo, String docName, String statusText) {
        return """
                Se informa que el estudiante:

                "%s"

                ha realizado la actualización de un documento asociado a su modalidad de grado,
                la cual actualmente se encuentra en una etapa activa de revisión por parte del
                Comité de Currículo del Programa.

                Información del proceso:

                Programa académico:
                "%s"

                Documento actualizado:
                "%s"

                Estado actual del documento:
                %s

                Debido a que la modalidad se encuentra en fase de evaluación, se solicita a los
                miembros del Comité de Currículo revisar la nueva versión del documento,
                verificar que su contenido cumpla con los lineamientos académicos establecidos
                y continuar con el procedimiento correspondiente dentro del flujo de evaluación
                definido para las modalidades de grado.

                Para consultar el documento actualizado y realizar el seguimiento respectivo,
                por favor ingrese al sistema.

                Plataforma de Gestión de Modalidades de Grado
                """.formatted(
                studentFullName,
                modalidadInfo,
                docName,
                statusText
        );
    }

    public static String committeeCorrectionResubmitted(String studentFullName, String modalidadInfo, String docName, String submissionDate) {
        return """
                Se informa que el estudiante:

                "%s"

                ha reenviado la versión corregida del documento asociado a su modalidad de grado,
                después de la solicitud de correcciones realizada por el Comité de Currículo del Programa.

                Información del proceso:

                Modalidad de grado:
                "%s"

                Documento corregido reenviado:
                "%s"

                Fecha de envío:
                %s

                Se solicita a los miembros del Comité de Currículo revisar la nueva versión del
                documento, verificar que las correcciones hayan sido atendidas conforme a las
                observaciones realizadas y continuar con el procedimiento correspondiente dentro
                del flujo de evaluación definido para las modalidades de grado.

                Para consultar el documento y realizar el seguimiento respectivo, por favor ingrese al sistema.

                Plataforma de Gestión de Modalidades de Grado
                """.formatted(
                studentFullName,
                modalidadInfo,
                docName,
                submissionDate
        );
    }

    public static String finalDocumentsSentToProgramHead(String studentName, String directorNombre, String modalidadInfo) {
        String body = """
        Nos permitimos informarle que el Director de Proyecto %s ha registrado que los documentos finales de su modalidad de grado se encuentran disponibles para la revisión institucional previa a la sustentación.

        A continuación, se relaciona la información pertinente:

        Modalidad de grado: "%s".

        Los documentos finales serán verificados por la Jefatura de Programa y, una vez aprobados, se notificará a los jurados evaluadores para dar continuidad al proceso de sustentación.

        Se recomienda consultar periódicamente la plataforma institucional, ya que a través de este medio se comunicarán las decisiones relacionadas con esta etapa del proceso.

        Este mensaje constituye una notificación automática generada para efectos de control y trazabilidad del proceso.
        """.formatted(
                directorNombre,
                modalidadInfo
        );
        return greeting(studentName) + body + universityClosing();
    }

    public static String modalityReadyForDefense(String studentName, String modalidadInfo) {
        String body = """
        Nos permitimos informarle que los documentos finales de su modalidad de grado han sido revisados y aprobados por la Jefatura de Programa, conforme al proceso académico establecido.

        A continuación, se relaciona la información pertinente:

        Modalidad de grado: "%s".

        En virtud de esta actuación, la modalidad ha sido registrada como lista para revisión final por parte de los jurados, quienes procederán con la revisión de la documentación final dentro de los plazos institucionales.

        Se recomienda consultar periódicamente la plataforma institucional para conocer las novedades del proceso y dar continuidad a la etapa de sustentación.

        Este mensaje constituye una notificación automática generada para efectos de control y trazabilidad del proceso.
        """.formatted(
                modalidadInfo
        );
        return greeting(studentName) + body + universityClosing();
    }

    public static String directorModalityReadyForDefense(String directorName, String studentList, String modalidadInfo) {
        String body = """
        Nos permitimos informarle que los documentos finales de la modalidad de grado han sido revisados y aprobados por la Jefatura de Programa, conforme al proceso académico establecido.

        A continuación, se relaciona la información pertinente:

        Modalidad de grado: "%s".
        Estudiantes asociados: %s.

        En virtud de esta actuación, la modalidad ha sido registrada como lista para revisión final por parte de los jurados, quienes procederán con la revisión de la documentación final dentro de los plazos institucionales.

        Se recomienda orientar a los estudiantes durante esta etapa del proceso y consultar periódicamente la plataforma institucional para el seguimiento correspondiente.

        Este mensaje constituye una notificación automática generada para efectos de control y trazabilidad del proceso.
        """.formatted(
                modalidadInfo,
                studentList
        );
        return greeting(directorName) + body + universityClosing();
    }
}
