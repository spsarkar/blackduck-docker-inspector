/**
 * hub-docker-inspector
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
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
package com.blackducksoftware.integration.hub.docker.imageinspector.linux.extractor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.hub.bdio.graph.MutableDependencyGraph;
import com.blackducksoftware.integration.hub.docker.imageinspector.OperatingSystemEnum;
import com.blackducksoftware.integration.hub.docker.imageinspector.PackageManagerEnum;
import com.blackducksoftware.integration.hub.docker.imageinspector.linux.FileOperations;
import com.blackducksoftware.integration.hub.docker.imageinspector.linux.executor.ApkExecutor;

@Component
class ApkExtractor extends Extractor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    ApkExecutor executor;

    @Override
    @PostConstruct
    public void init() {
        final List<String> forges = new ArrayList<>();
        forges.add(OperatingSystemEnum.ALPINE.getForge());
        initValues(PackageManagerEnum.APK, executor, forges);
    }

    @Override
    public void extractComponents(final MutableDependencyGraph dependencies, final String dockerImageRepo, final String dockerImageTag, final ExtractionDetails extractionDetails, final String[] packageList) {
        for (final String packageLine : packageList) {
            if (!packageLine.toLowerCase().startsWith("warning")) {
                logger.trace(String.format("packageLine: %s", packageLine));
                final String[] parts = packageLine.split("-");
                final String version = String.format("%s-%s", parts[parts.length - 2], parts[parts.length - 1]);
                logger.trace(String.format("version: %s", version));
                String component = "";
                for (int i = 0; i < parts.length - 2; i++) {
                    final String part = parts[i];
                    if (StringUtils.isNotBlank(component)) {
                        component += String.format("-%s", part);
                    } else {
                        component = part;
                    }
                }
                logger.trace(String.format("component: %s", component));
                // if a package starts with a period, we should ignore it because it is a virtual meta package and the version information is missing
                if (!component.startsWith(".")) {
                    final String externalId = String.format("%s/%s/%s", component, version, extractionDetails.getArchitecture());
                    logger.debug(String.format("Constructed externalId: %s", externalId));
                    createBdioComponent(dependencies, component, version, externalId, extractionDetails.getArchitecture());
                }
            }
        }
    }

    @Override
    public String deriveArchitecture(final File targetImageFileSystemRootDir) throws IOException {
        String architecture = null;
        final List<File> etcDirectories = FileOperations.findDirWithName(targetImageFileSystemRootDir, "etc");
        for (final File etc : etcDirectories) {
            File architectureFile = new File(etc, "apk");
            architectureFile = new File(architectureFile, "arch");
            if (architectureFile.exists()) {
                architecture = FileUtils.readLines(architectureFile, "UTF-8").get(0);
                break;
            }
        }
        return architecture;
    }
}