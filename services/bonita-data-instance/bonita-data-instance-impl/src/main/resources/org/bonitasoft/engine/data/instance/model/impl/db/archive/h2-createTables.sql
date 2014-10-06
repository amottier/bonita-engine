CREATE TABLE arch_data_instance (
    tenantId BIGINT NOT NULL,
	id BIGINT NOT NULL,
	name VARCHAR(50),
	description VARCHAR(50),
	transientData BOOLEAN,
	className VARCHAR(100),
	containerId BIGINT,
	containerType VARCHAR(60),
	namespace VARCHAR(100),
	element VARCHAR(60),
	intValue INT,
	longValue BIGINT,
	shortTextValue VARCHAR(255),
	booleanValue BOOLEAN,
	doubleValue NUMERIC(19,5),
	floatValue REAL,
	blobValue MEDIUMBLOB,
	clobValue CLOB,
	discriminant VARCHAR(50) NOT NULL,
	archiveDate BIGINT NOT NULL,
	sourceObjectId BIGINT NOT NULL,
	PRIMARY KEY (tenantid, id)
);

CREATE INDEX idx1_arch_data_instance ON arch_data_instance (tenantId,containerId, sourceObjectId);

CREATE TABLE arch_data_mapping (
    tenantid BIGINT NOT NULL,
	id BIGINT NOT NULL,
	containerId BIGINT,
	containerType VARCHAR(60),
	dataName VARCHAR(50),
	dataInstanceId BIGINT NOT NULL,
	archiveDate BIGINT NOT NULL,
	sourceObjectId BIGINT NOT NULL,
	PRIMARY KEY (tenantid, id)
);

CREATE INDEX idx1_arch_data_mapping ON arch_data_mapping (tenantId,containerId, dataInstanceId, sourceObjectId);
CREATE INDEX idx2_arch_data_mapping ON arch_data_mapping (tenantid, containerId, containerType);
