ALTER TABLE actormember ADD CONSTRAINT fk_actormember_actorId FOREIGN KEY (actorId, tenantid) REFERENCES actor(id, tenantid);
ALTER TABLE queriablelog_p ADD CONSTRAINT fk_queriablelog_p_tenantId FOREIGN KEY (tenantid) REFERENCES tenant(id);
ALTER TABLE event_trigger_instance ADD CONSTRAINT fk_EvtTrig_tenId FOREIGN KEY (tenantid) REFERENCES tenant(id);
ALTER TABLE external_identity_mapping ADD CONSTRAINT fk_extIdMap_tenId FOREIGN KEY (tenantid) REFERENCES tenant(id);
ALTER TABLE custom_usr_inf_def ADD CONSTRAINT fk_custom_usr_inf_def_tenantId FOREIGN KEY (tenantid) REFERENCES tenant(id);
ALTER TABLE custom_usr_inf_val ADD CONSTRAINT fk_custom_usr_inf_val_tenantId FOREIGN KEY (tenantid) REFERENCES tenant(id);
ALTER TABLE pending_mapping ADD CONSTRAINT fk_pending_mapping_flownode_instanceId FOREIGN KEY (activityId, tenantid) REFERENCES flownode_instance(id, tenantid);
ALTER TABLE processcategorymapping ADD CONSTRAINT fk_procCatMap_tenId FOREIGN KEY (tenantid) REFERENCES tenant(id);
ALTER TABLE multi_biz_data ADD CONSTRAINT fk_multi_biz_data_tenantId FOREIGN KEY (tenantid) REFERENCES tenant(id);
ALTER TABLE ref_biz_data_inst ADD CONSTRAINT fk_ref_biz_data_inst_tenantId FOREIGN KEY (tenantId) REFERENCES tenant(id);
ALTER TABLE theme ADD CONSTRAINT fk_theme_tenantId FOREIGN KEY (tenantId) REFERENCES tenant(id);


ALTER TABLE business_app ADD CONSTRAINT fk_app_tenantId FOREIGN KEY (tenantid) REFERENCES tenant(id);
ALTER TABLE business_app_page ADD CONSTRAINT fk_app_page_tenantId FOREIGN KEY (tenantid) REFERENCES tenant(id);
ALTER TABLE business_app_page ADD CONSTRAINT fk_bus_app_id FOREIGN KEY (tenantid, applicationId) REFERENCES business_app (tenantid, id) ON DELETE CASCADE;
ALTER TABLE business_app_page ADD CONSTRAINT fk_page_id FOREIGN KEY (tenantid, pageId) REFERENCES page (tenantid, id);

ALTER TABLE arch_document_mapping ADD CONSTRAINT fk_ADocMap_tenId FOREIGN KEY (tenantid) REFERENCES tenant(id);
ALTER TABLE arch_flownode_instance ADD CONSTRAINT fk_AFln_tenId FOREIGN KEY (tenantid) REFERENCES tenant(id);
ALTER TABLE arch_process_comment ADD CONSTRAINT fk_AProcCom_tenId FOREIGN KEY (tenantid) REFERENCES tenant(id);
ALTER TABLE arch_process_instance ADD CONSTRAINT fk_AProc_tenId FOREIGN KEY (tenantid) REFERENCES tenant(id);
ALTER TABLE arch_transition_instance ADD CONSTRAINT fk_ATrans_tenId FOREIGN KEY (tenantid) REFERENCES tenant(id);
