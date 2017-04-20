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
package com.blackducksoftware.integration.hub.docker.client

import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.docker.HubDockerClient
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.PullImageCmd
import com.github.dockerjava.api.command.SaveImageCmd
import com.github.dockerjava.core.command.PullImageResultCallback

@Component
class DockerClientManager {
    private final Logger logger = LoggerFactory.getLogger(DockerClientManager.class)

    @Autowired
    HubDockerClient hubDockerClient

    @Value('${working.directory}')
    String workingDirectoryPath

    File getTarFileFromDockerImage(String imageName, String tagName) {
        // use docker to pull image if necessary
        // use docker to save image to tar
        // performExtractFromDockerTar()
        DockerClient client = hubDockerClient.getDockerClient()
        File imageTarDirectory = new File(new File(workingDirectoryPath), 'tarDirectory')
        logger.info("Pulling image ${imageName}:${tagName}")
        PullImageCmd pull = client.pullImageCmd("${imageName}").withTag(tagName)
        pull.exec(new PullImageResultCallback()).awaitSuccess()
        File imageTarFile = new File(imageTarDirectory, "${imageName}_${tagName}.tar")
        InputStream tarInputStream = null
        try{
            logger.info("Saving the docker image to : ${imageTarFile.getAbsolutePath()}")
            SaveImageCmd saveCommand = client.saveImageCmd(imageName)
            saveCommand.withTag(tagName)
            tarInputStream = saveCommand.exec()
            FileUtils.copyInputStreamToFile(tarInputStream, imageTarFile);
        } finally{
            IOUtils.closeQuietly(tarInputStream);
        }
        imageTarFile
    }

}
