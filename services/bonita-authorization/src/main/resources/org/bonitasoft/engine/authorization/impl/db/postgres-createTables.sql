CREATE TABLE authorized_program (
  tenantId INT8 NOT NULL,
  id INT8 NOT NULL,
  programName VARCHAR(255) NOT NULL,
  programSecurityToken VARCHAR(512) NOT NULL,
  creationDate TIMESTAMP NOT NULL,
  UNIQUE (tenantId, programName),
  PRIMARY KEY (tenantId, id)
);
