CREATE TABLE public.process_instances(id uuid NOT NULL,
                                      payload bytea,
                                      process_id character varying NOT NULL,
                                      CONSTRAINT process_instances_pkey PRIMARY KEY (id)
                                      );
CREATE INDEX idx_process_instances_process_id ON process_instances
    (
     process_id
    );