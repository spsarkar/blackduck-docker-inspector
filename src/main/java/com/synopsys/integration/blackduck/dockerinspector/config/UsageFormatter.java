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
package com.synopsys.integration.blackduck.dockerinspector.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.dockerinspector.DockerInspector;
import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.dockerinspector.config.DockerInspectorOption;

@Component
public class UsageFormatter {

    @Autowired
    private Config config;

    public List<String> getStringList() throws IllegalArgumentException, IllegalAccessException, IOException {
        final List<String> usage = new ArrayList<>();
        usage.add(String.format("Usage: %s <options>", DockerInspector.PROGRAM_NAME));
        usage.add("options: any supported property can be set by adding to the command line");
        usage.add("an option of the form:");
        usage.add("--<property name>=<value>");
        usage.add("");
        usage.add("Alternatively, any supported property can be set by adding to a text file named");
        usage.add("application.properties (in the current directory) a line of the form:");
        usage.add("<property name>=<value>");
        usage.add("");
        usage.add("For greater security, the Black Duck password can be set via the environment variable BD_PASSWORD.");
        usage.add("For example:");
        usage.add("  export BD_PASSWORD=mypassword");
        usage.add("  ./blackduck-docker-inspector.sh --blackduck.url=http://blackduck.mydomain.com:8080/ --blackduck.username=myusername --docker.image=ubuntu:latest");
        usage.add("");
        usage.add(String.format("Available properties:"));
        final SortedSet<DockerInspectorOption> configOptions = config.getPublicConfigOptions();
        for (final DockerInspectorOption opt : configOptions) {
            final StringBuilder usageLine = new StringBuilder(String.format("  %s [%s]: %s", opt.getKey(), opt.getValueTypeString(), opt.getDescription()));
            if (!StringUtils.isBlank(opt.getDefaultValue())) {
                usageLine.append(String.format("; default: %s", opt.getDefaultValue()));
            }
            if (opt.isDeprecated()) {
                usageLine.append(String.format("; [DEPRECATED]"));
            }
            usage.add(usageLine.toString());
        }
        usage.add("");
        usage.add("Documentation is under Package Managers > Black Duck Docker Inspector at: https://blackducksoftware.atlassian.net/wiki/spaces/INTDOCS");
        return usage;
    }
}
