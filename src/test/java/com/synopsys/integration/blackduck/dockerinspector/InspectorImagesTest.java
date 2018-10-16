package com.synopsys.integration.blackduck.dockerinspector;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.SpringRunner;

import com.synopsys.integration.blackduck.dockerinspector.config.Config;
import com.synopsys.integration.blackduck.imageinspector.lib.OperatingSystemEnum;

@RunWith(SpringRunner.class)
public class InspectorImagesTest {
    private static final String PROGRAM_VERSION = "1.2.3";

    @Mock
    private Config config;

    @Mock
    private ProgramVersion programVersion;

    @InjectMocks
    private InspectorImages osMapper;

    @Test
    public void testBasic() throws IOException {

        Mockito.when(config.getInspectorRepository()).thenReturn("blackducksoftware");
        Mockito.when(config.getInspectorImageFamily()).thenReturn("blackduck-imageinspector");
        Mockito.when(config.getInspectorImageVersion()).thenReturn(PROGRAM_VERSION);
        Mockito.when(programVersion.getProgramVersion()).thenReturn(PROGRAM_VERSION);
        osMapper.init();

        assertEquals("blackducksoftware/blackduck-imageinspector-centos", osMapper.getInspectorImageName(OperatingSystemEnum.CENTOS));
        assertEquals(PROGRAM_VERSION, osMapper.getInspectorImageTag(OperatingSystemEnum.CENTOS));
        assertEquals(OperatingSystemEnum.CENTOS, osMapper.getInspectorImageOs(OperatingSystemEnum.CENTOS));

        assertEquals("blackducksoftware/blackduck-imageinspector-ubuntu", osMapper.getInspectorImageName(OperatingSystemEnum.UBUNTU));
        assertEquals(PROGRAM_VERSION, osMapper.getInspectorImageTag(OperatingSystemEnum.UBUNTU));
        assertEquals(OperatingSystemEnum.UBUNTU, osMapper.getInspectorImageOs(OperatingSystemEnum.UBUNTU));

        assertEquals("blackducksoftware/blackduck-imageinspector-alpine", osMapper.getInspectorImageName(OperatingSystemEnum.ALPINE));
        assertEquals(PROGRAM_VERSION, osMapper.getInspectorImageTag(OperatingSystemEnum.ALPINE));
        assertEquals(OperatingSystemEnum.ALPINE, osMapper.getInspectorImageOs(OperatingSystemEnum.ALPINE));
    }

    @Test
    public void testAlternateRepoWithoutSlash() throws IOException {
        Mockito.when(config.getInspectorRepository()).thenReturn("myrepo");
        Mockito.when(config.getInspectorImageFamily()).thenReturn("blackduck-imageinspector");
        Mockito.when(config.getInspectorImageVersion()).thenReturn(PROGRAM_VERSION);
        Mockito.when(programVersion.getProgramVersion()).thenReturn(PROGRAM_VERSION);
        osMapper.init();

        assertEquals("myrepo/blackduck-imageinspector-centos", osMapper.getInspectorImageName(OperatingSystemEnum.CENTOS));
    }

    @Test
    public void testAlternateRepoWithSlash() throws IOException {
        Mockito.when(config.getInspectorRepository()).thenReturn("myrepo/");
        Mockito.when(config.getInspectorImageFamily()).thenReturn("blackduck-imageinspector");
        Mockito.when(config.getInspectorImageVersion()).thenReturn(PROGRAM_VERSION);
        Mockito.when(programVersion.getProgramVersion()).thenReturn(PROGRAM_VERSION);
        osMapper.init();

        assertEquals("myrepo/blackduck-imageinspector-centos", osMapper.getInspectorImageName(OperatingSystemEnum.CENTOS));
    }

    @Test
    public void testNoRepo() throws IOException {
        Mockito.when(config.getInspectorRepository()).thenReturn("");
        Mockito.when(config.getInspectorImageFamily()).thenReturn("blackduck-imageinspector");
        Mockito.when(config.getInspectorImageVersion()).thenReturn(PROGRAM_VERSION);
        Mockito.when(programVersion.getProgramVersion()).thenReturn(PROGRAM_VERSION);
        osMapper.init();

        assertEquals("blackduck-imageinspector-centos", osMapper.getInspectorImageName(OperatingSystemEnum.CENTOS));
    }
}