package com.synopsys.integration.blackduck.dockerinspector.httpclient;

import static org.junit.Assert.assertEquals;

import com.synopsys.integration.rest.client.IntHttpClient;
import java.io.IOException;
import java.net.URI;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.SpringRunner;

import com.github.dockerjava.api.model.Container;
import com.synopsys.integration.blackduck.dockerinspector.InspectorImages;
import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.dockerinspector.dockerclient.DockerClientManager;
import com.synopsys.integration.blackduck.dockerinspector.httpclient.response.SimpleResponse;
import com.synopsys.integration.blackduck.imageinspector.api.ImageInspectorOsEnum;
import com.synopsys.integration.blackduck.imageinspector.lib.OperatingSystemEnum;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.rest.RestConstants;

@RunWith(SpringRunner.class)
public class IiClientContainersStartedAsNeededTest {

    @InjectMocks
    private ImageInspectorClientStartServices imageInspectorClientContainersStartedAsNeeded;

    @Mock
    private Config config;

    @Mock
    private ImageInspectorServices imageInspectorPorts;

    @Mock
    private RestConnectionCreator restConnectionCreator;

    @Mock
    private HttpRequestor httpRequestor;

    @Mock
    private InspectorImages inspectorImages;

    @Mock
    private DockerClientManager dockerClientManager;

    @Mock
    private ContainerPaths containerPaths;

    @Test
    public void test() throws IntegrationException, IOException {
        Mockito.when(config.isImageInspectorServiceStart()).thenReturn(true);
        Mockito.when(imageInspectorPorts.getDefaultImageInspectorHostPortBasedOnDistro()).thenReturn(8080);
        Mockito.when(config.getCommandTimeout()).thenReturn(5000L);
        Mockito.when(config.getImageInspectorDefaultDistro()).thenReturn("ubuntu");

        final Container targetContainer = Mockito.mock(Container.class);
        Mockito.when(targetContainer.getImage()).thenReturn("target");
        Mockito.when(dockerClientManager.getRunningContainerByAppName(Mockito.anyString(), Mockito.any(ImageInspectorOsEnum.class))).thenReturn(targetContainer);

        final IntHttpClient restConnection = Mockito.mock(IntHttpClient.class);
        Mockito.when(restConnectionCreator.createNonRedirectingConnection(Mockito.any(URI.class), Mockito.anyInt())).thenReturn(restConnection);

        Mockito.when(httpRequestor.executeSimpleGetRequest(Mockito.any(IntHttpClient.class), Mockito.any(URI.class), Mockito.anyString())).thenReturn("{\"status\":\"UP\"}");
        // Mockito.when(restRequestor.executeSimpleGetRequest(Mockito.any(RestConnection.class), Mockito.anyString(), Mockito.anyString())).thenReturn("testResponse");
        final SimpleResponse response = new SimpleResponse(RestConstants.OK_200, null, "testResult");
        Mockito.when(httpRequestor
            .executeGetBdioRequest(Mockito.any(IntHttpClient.class), Mockito.any(URI.class), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.anyBoolean())).thenReturn(response);

        Mockito.when(inspectorImages.getInspectorImageName(Mockito.any(OperatingSystemEnum.class))).thenReturn("blackduck/blackduck-imageinspector");
        Mockito.when(inspectorImages.getInspectorImageTag(Mockito.any(OperatingSystemEnum.class))).thenReturn("1.1.1");

        assertEquals(true, imageInspectorClientContainersStartedAsNeeded.isApplicable());
        assertEquals("testResult", imageInspectorClientContainersStartedAsNeeded.getBdio("/tmp/t.tar", null, null, "/tmp/t.tar", "containerFileSystemFilename", true, false, false));
    }

}