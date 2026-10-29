-- Usuarios de demostración. Contraseña de todos: Demo1234* (cifrada con BCrypt).
-- En una base nueva reciben los ids 1 a 5; el id 5 tiene rol SOPORTE, como en el ejemplo del enunciado.
INSERT INTO usuario (nombre, correo, password, rol) VALUES
    ('Laura Gómez',   'laura.gomez@empresa.com',   '$2a$10$9i1IhoILl/tP9tbWNcFcXOyuG3SuK1nAK9pqB1isS7HiX9CRR0QMO', 'USUARIO'),
    ('Carlos Pérez',  'carlos.perez@empresa.com',  '$2a$10$9i1IhoILl/tP9tbWNcFcXOyuG3SuK1nAK9pqB1isS7HiX9CRR0QMO', 'USUARIO'),
    ('Andrea Ruiz',   'andrea.ruiz@empresa.com',   '$2a$10$9i1IhoILl/tP9tbWNcFcXOyuG3SuK1nAK9pqB1isS7HiX9CRR0QMO', 'ADMIN'),
    ('Miguel Torres', 'miguel.torres@empresa.com', '$2a$10$9i1IhoILl/tP9tbWNcFcXOyuG3SuK1nAK9pqB1isS7HiX9CRR0QMO', 'SOPORTE'),
    ('Sofía Ramírez', 'sofia.ramirez@empresa.com', '$2a$10$9i1IhoILl/tP9tbWNcFcXOyuG3SuK1nAK9pqB1isS7HiX9CRR0QMO', 'SOPORTE')
ON CONFLICT (correo) DO NOTHING;
