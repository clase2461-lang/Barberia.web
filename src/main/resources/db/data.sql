-- ============================================================
-- DATA INICIAL - Sistema Barbería Juan
-- Ejecutar DESPUÉS de schema.sql
-- ============================================================

USE barberia_juan;

-- Usuario admin por defecto: admin / Admin123!
-- Hash BCrypt generado para 'Admin123!'
INSERT INTO usuarios (username, password_hash, nombre, apellido, telefono, email, rol, activo)
VALUES
    ('admin', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TiGc8E29xvHJfkJHJJA0gYhUG6mG',
     'Juan', 'García', '+52XXXXXXXXXX', 'admin@barberiajuan.com', 'ADMIN', TRUE);

-- Servicios iniciales de barbería
INSERT INTO servicios (nombre, descripcion, precio, duracion_minutos, activo) VALUES
    ('Corte de cabello',     'Corte de cabello estilo clásico o moderno',  150.00, 30, TRUE),
    ('Corte y barba',        'Corte de cabello más arreglo de barba',       200.00, 45, TRUE),
    ('Afeitado clásico',     'Afeitado con navaja y toalla caliente',       120.00, 30, TRUE),
    ('Arreglo de barba',     'Perfilado y arreglo de barba',                 80.00, 20, TRUE),
    ('Tinte de cabello',     'Aplicación de tinte o decoloración',          350.00, 90, TRUE),
    ('Tratamiento capilar',  'Hidratación y nutrición del cabello',          250.00, 60, TRUE),
    ('Corte infantil',       'Corte de cabello para niños (hasta 12 años)', 100.00, 25, TRUE);

-- Configuración inicial del sistema
INSERT INTO configuracion (clave, valor, descripcion) VALUES
    ('nombre_barberia',     'Barbería Juan',                    'Nombre de la barbería'),
    ('telefono_barberia',   '+52XXXXXXXXXX',                    'Teléfono de contacto'),
    ('direccion_barberia',  'Calle Principal #123, Ciudad',     'Dirección física'),
    ('horario_apertura',    '09:00',                            'Hora de apertura (HH:mm)'),
    ('horario_cierre',      '20:00',                            'Hora de cierre (HH:mm)'),
    ('duracion_slot',       '30',                               'Duración de cada slot en minutos'),
    ('dias_laborables',     '1,2,3,4,5,6',                      'Días laborables (1=Lun...7=Dom)'),
    ('twilio_account_sid',  'CONFIGURA_TU_ACCOUNT_SID',         'Twilio Account SID'),
    ('twilio_auth_token',   'CONFIGURA_TU_AUTH_TOKEN',          'Twilio Auth Token'),
    ('twilio_whatsapp_from','whatsapp:+14155238886',            'Número WhatsApp Twilio Sandbox'),
    ('recordatorio_horas',  '24',                               'Horas antes para enviar recordatorio'),
    ('webhook_port',        '8080',                             'Puerto del servidor webhook');
