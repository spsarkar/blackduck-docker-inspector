/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2019 Synopsys, Inc.
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
package com.synopsys.integration.blackduck.dockerinspector.httpclient;

import com.synopsys.integration.blackduck.dockerinspector.programversion.ProgramVersion;
import com.synopsys.integration.blackduck.imageinspector.api.ImageInspectorOsEnum;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.dockerinspector.config.Config;

@Component
public class InspectorImages {

    @Autowired
    private ProgramVersion programVersion;

    @Autowired
    private Config config;

    private final Map<ImageInspectorOsEnum, InspectorImage> inspectorImageMap = new HashMap<>();

    @PostConstruct
    void init() {
        String repoWithSeparator;
        final String repo = config.getInspectorRepository();
        if (StringUtils.isBlank(repo)) {
            repoWithSeparator = "";
        } else if (StringUtils.isNotBlank(repo) && repo.endsWith("/")) {
            repoWithSeparator = repo;
        } else {
            repoWithSeparator = String.format("%s/", repo);
        }
        String inspectorImageFamily = config.getInspectorImageFamily();
        if (StringUtils.isBlank(inspectorImageFamily)) {
            inspectorImageFamily = programVersion.getInspectorImageFamily();
        }
        String inspectorImageVersion = config.getInspectorImageVersion();
        if (StringUtils.isBlank(inspectorImageVersion)) {
            inspectorImageVersion = programVersion.getInspectorImageVersion();
        }
        inspectorImageMap.put(ImageInspectorOsEnum.CENTOS, new InspectorImage(ImageInspectorOsEnum.CENTOS, String.format("%s%s-centos", repoWithSeparator, inspectorImageFamily), inspectorImageVersion));
        inspectorImageMap.put(ImageInspectorOsEnum.UBUNTU, new InspectorImage(ImageInspectorOsEnum.UBUNTU, String.format("%s%s-ubuntu", repoWithSeparator, inspectorImageFamily), inspectorImageVersion));
        inspectorImageMap.put(ImageInspectorOsEnum.ALPINE, new InspectorImage(ImageInspectorOsEnum.ALPINE, String.format("%s%s-alpine", repoWithSeparator, inspectorImageFamily), inspectorImageVersion));
    }

    public String getInspectorImageName(final ImageInspectorOsEnum targetImageOs) {
        final InspectorImage image = inspectorImageMap.get(targetImageOs);
        if (image == null) {
            return null;
        }
        return image.getImageName();
    }

    public String getInspectorImageTag(final ImageInspectorOsEnum targetImageOs) {
        final InspectorImage image = inspectorImageMap.get(targetImageOs);
        if (image == null) {
            return null;
        }
        return image.getImageVersion();
    }
}
