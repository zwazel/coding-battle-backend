CREATE TABLE bots
(
    id          UUID        NOT NULL,
    name        VARCHAR(40) NOT NULL,
    source_path VARCHAR(255),
    wasm_path   VARCHAR(255),
    user_id     UUID        NOT NULL,
    language    VARCHAR(20) NOT NULL,
    CONSTRAINT pk_bots PRIMARY KEY (id)
);

CREATE TABLE roles
(
    id   UUID        NOT NULL,
    name VARCHAR(30) NOT NULL,
    CONSTRAINT pk_roles PRIMARY KEY (id)
);

CREATE TABLE user_roles
(
    role_id UUID NOT NULL,
    user_id UUID NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (role_id, user_id)
);

CREATE TABLE users
(
    id       UUID        NOT NULL,
    username VARCHAR(40) NOT NULL,
    password VARCHAR(60) NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id)
);

ALTER TABLE bots
    ADD CONSTRAINT uc_318be23aa22523e6852b893eb UNIQUE (user_id, name);

ALTER TABLE roles
    ADD CONSTRAINT uc_roles_name UNIQUE (name);

ALTER TABLE users
    ADD CONSTRAINT uk_username UNIQUE (username);

ALTER TABLE bots
    ADD CONSTRAINT FK_BOTS_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE user_roles
    ADD CONSTRAINT fk_userol_on_role FOREIGN KEY (role_id) REFERENCES roles (id);

ALTER TABLE user_roles
    ADD CONSTRAINT fk_userol_on_user FOREIGN KEY (user_id) REFERENCES users (id);