/*
 * Copyright (C) 2017 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.blackducksoftware.integration.hub.docker.config;

import org.apache.commons.lang3.builder.RecursiveToStringStyle;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DockerInspectorOption {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String key;
    private final String fieldName;
    private final String description;
    private final Class<?> valueType;
    private final String valueTypeString;
    private final String group;
    private final String defaultValue;
    private String resolvedValue;
    private final boolean deprecated;

    public DockerInspectorOption(final String key, final String fieldName, final String resolvedValue, final String description, final Class<?> valueType, final String defaultValue, final String group, final boolean deprecated) {
        this.key = key;
        this.description = description;
        this.valueType = valueType;
        this.group = group;
        this.defaultValue = defaultValue;
        this.fieldName = fieldName;
        this.resolvedValue = resolvedValue;
        this.deprecated = deprecated;

        final String[] parts = valueType.toString().split("\\.");
        logger.trace(String.format("Split %s into %d parts", valueType.toString(), parts.length));
        this.valueTypeString = parts[parts.length - 1];
    }

    public String getKey() {
        return key;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getDescription() {
        return description;
    }

    public Class<?> getValueType() {
        return valueType;
    }

    public String getValueTypeString() {
        return valueTypeString;
    }

    public String getGroup() {
        return group;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getResolvedValue() {
        return resolvedValue;
    }

    public void setResolvedValue(final String resolvedValue) {
        this.resolvedValue = resolvedValue;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, RecursiveToStringStyle.JSON_STYLE);
    }
}
