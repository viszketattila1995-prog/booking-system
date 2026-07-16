-- Fejlesztői admin seed. Nincs self-service admin regisztráció (szándékosan - lásd
-- a Provider jóváhagyási flow-t), ezért enélkül nem lenne mód admin műveletek
-- tesztelésére. Jelszó: "Admin123!" (csak dev környezethez, bcrypt hash).
WITH new_admin AS (
    INSERT INTO app_user (email, password_hash, full_name)
    VALUES ('admin@booking-system.local', '$2a$10$zLKxNO.kQQ5g1E99siYpquTqO93aq0BIZK9BXoQtFMsS375.xl/vq', 'System Admin')
    RETURNING id
)
INSERT INTO user_role (user_id, role_id)
SELECT new_admin.id, app_role.id
FROM new_admin, app_role
WHERE app_role.name = 'ROLE_ADMIN';
