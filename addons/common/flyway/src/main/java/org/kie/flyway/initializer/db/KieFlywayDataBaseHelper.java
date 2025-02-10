/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kie.flyway.initializer.db;

import java.sql.Connection;

import javax.sql.DataSource;

import org.kie.flyway.KieFlywayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KieFlywayDataBaseHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(KieFlywayDataBaseHelper.class);

    private KieFlywayDataBaseHelper() {
    }

    public static DataBaseInfo readDataBaseInfo(DataSource ds) {
        try (Connection con = ds.getConnection()) {
            String name = con.getMetaData().getDatabaseProductName();
            String version = con.getMetaData().getDatabaseProductVersion();
            String flywayName = normalizeName(name);

            LOGGER.info("Reading DataBase Product: '{}' Version: '{}' (Flyway name: {})", name, version, flywayName);

            return new DataBaseInfo(name, version, flywayName);
        } catch (Exception e) {
            LOGGER.error("Kie Flyway: Couldn't extract database product name from datasource ", e);
            throw new KieFlywayException("Kie Flyway: Couldn't extract database product name from datasource.", e);
        }
    }

    public static String normalizeName(String name) {
        final String NORMALIZATION_REGEX = "[^a-zA-Z0-9]+";
        String[] fragments = name.split(NORMALIZATION_REGEX);
        return String.join("-", fragments).toLowerCase();
    }

}
