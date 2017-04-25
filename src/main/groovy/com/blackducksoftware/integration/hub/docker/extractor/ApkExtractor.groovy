/*
 * Copyright (C) 2017 Black Duck Software Inc.
 * http://www.blackducksoftware.com/
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Black Duck Software ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Black Duck Software.
 */
package com.blackducksoftware.integration.hub.docker.extractor

import javax.annotation.PostConstruct

import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.bdio.simple.model.BdioComponent
import com.blackducksoftware.integration.hub.docker.OperatingSystemEnum
import com.blackducksoftware.integration.hub.docker.PackageManagerEnum
import com.blackducksoftware.integration.hub.docker.executor.ApkExecutor

@Component
class ApkExtractor extends Extractor {

    @Autowired
    ApkExecutor executor

    @PostConstruct
    void init() {
        def forges = [
            OperatingSystemEnum.ALPINE.forge
        ]
        initValues(PackageManagerEnum.APK, executor, forges)
    }

    List<BdioComponent> extractComponents(String[] packageList) {
        def components = []
        packageList.each { packageLine ->
            if (!packageLine.toLowerCase().startsWith('warning')) {
                String[] parts = packageLine.split('-')
                def version = parts[parts.length -2]
                def component = ''
                parts = parts.take(parts.length - 2)
                for(String part : parts){
                    if(StringUtils.isNotBlank(component)){
                        component += "-${part}"
                    } else{
                        component = part
                    }
                }
                String externalId = "${component}/${version}"
                components.addAll(createBdioComponent(component, version, externalId))
            }
        }
        components
    }
}