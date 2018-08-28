package com.synopsys.integration.blackduck.dockerinspector;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.test.annotation.IntegrationTest;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

@Category(IntegrationTest.class)
public class InMinikubeTest {
    private static KubernetesClient client;
    private static String clusterIp;
    private static List<String> additionalArgsWithServiceUrl;
    private static ProgramVersion programVersion;
    private static Map<String, String> minikubeDockerEnv = new HashMap<>();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        programVersion = new ProgramVersion();
        programVersion.init();

        final String kubeStatusOutputJoined = TestUtils.execCmd(null, "minikube status", 15, true, null);
        System.out.println(String.format("kubeStatusOutputJoined: %s", kubeStatusOutputJoined));
        assertTrue("Minikube is not running", kubeStatusOutputJoined.contains("minikube: Running"));
        assertTrue("Minikube is not running", kubeStatusOutputJoined.contains("cluster: Running"));

        final String[] ipOutput = TestUtils.execCmd("minikube ip", 10, true, null).split("\n");
        clusterIp = ipOutput[0];
        final String serviceUrlArg = String.format("--imageinspector.service.url=http://%s:%d", clusterIp, IntegrationTestCommon.START_AS_NEEDED_IMAGE_INSPECTOR_PORT_ON_HOST_UBUNTU);
        additionalArgsWithServiceUrl = Arrays.asList(serviceUrlArg);
        client = new DefaultKubernetesClient();
        try {
            System.out.printf("API version: %s\n", client.getApiVersion());
        } catch (final Exception e) {
            e.printStackTrace();
        }

        final String[] dockerEnvOutput = TestUtils.execCmd("minikube docker-env", 5, true, null).split("\n");

        for (final String line : dockerEnvOutput) {
            if (line.startsWith("export")) {
                final String envVariableName = line.substring("export".length() + 1, line.indexOf("="));
                final String envVariableValue = line.substring(line.indexOf("=") + 2, line.length() - 1);
                System.out.println(String.format("env var assignment: %s=%s", envVariableName, envVariableValue));
                minikubeDockerEnv.put(envVariableName, envVariableValue);
            }
        }

    }

    @AfterClass
    public static void tearDownAfterClass() {
        if (client != null) {
            client.close();
        }
        System.out.println("Test has completed");
    }

    @Test
    public void testUbuntuStartContainer() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(programVersion, "ubuntu:17.04", "ubuntu", "17.04", "var_lib_dpkg", false, true, "dpkg", 10, additionalArgsWithServiceUrl, minikubeDockerEnv);
    }

    @Test
    public void testAlpineStartContainer() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(programVersion, "alpine:3.6", "alpine", "3.6", "lib_apk", false, true, "apk-", 5, additionalArgsWithServiceUrl, minikubeDockerEnv);
    }

    @Test
    public void testBusyboxStartContainer() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(programVersion, "busybox:latest", "busybox", "latest", "noPkgMgr", false, true, null, 0, additionalArgsWithServiceUrl, minikubeDockerEnv);
    }

    @Test
    public void testAlpineLatestStartContainer() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(programVersion, "alpine", "alpine", "latest", "lib_apk", false, true, "apk-", 5, additionalArgsWithServiceUrl, minikubeDockerEnv);
    }

    @Test
    public void testCentosStartContainer() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(programVersion, "centos:7.3.1611", "centos", "7.3.1611", "var_lib_rpm", false, true, "rpm", 15, additionalArgsWithServiceUrl, minikubeDockerEnv);
    }

    @Test
    public void testBlackDuckWebappStartContainer() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(programVersion, "blackducksoftware/hub-webapp:4.0.0", "blackducksoftware_hub-webapp", "4.0.0", "lib_apk", true, true, "apk-", 5, additionalArgsWithServiceUrl, minikubeDockerEnv);
    }

    @Test
    public void testBlackDuckZookeeperStartContainer() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(programVersion, "blackducksoftware/hub-zookeeper:4.0.0", "blackducksoftware_hub-zookeeper", "4.0.0", "lib_apk", true, true, "apk-", 5, additionalArgsWithServiceUrl, minikubeDockerEnv);
    }

    @Test
    public void testTomcatStartContainer() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(programVersion, "tomcat:6.0.53-jre7", "tomcat", "6.0.53-jre7", "var_lib_dpkg", false, true, "dpkg", 5, additionalArgsWithServiceUrl, minikubeDockerEnv);
    }

    @Test
    public void testRhelStartContainer() throws IOException, InterruptedException, IntegrationException {
        IntegrationTestCommon.testImage(programVersion, "dnplus/rhel:6.5", "dnplus_rhel", "6.5", "var_lib_rpm", false, true, "rpm", 10, additionalArgsWithServiceUrl, minikubeDockerEnv);
    }

    @Test
    public void testWhiteoutStartContainer() throws IOException, InterruptedException, IntegrationException {
        final String repo = "blackducksoftware/whiteouttest";
        final String tag = "1.0";
        final File outputContainerFileSystemFile = IntegrationTestCommon.getOutputContainerFileSystemFileFromTarFilename("whiteouttest.tar");
        IntegrationTestCommon.testTar(programVersion, "build/images/test/whiteouttest.tar", repo.replaceAll("/", "_"), repo, tag, tag, "var_lib_dpkg", true, true, additionalArgsWithServiceUrl, true, outputContainerFileSystemFile,
                minikubeDockerEnv);
    }

    @Test
    public void testAggregateTarfileImageOneStartContainer() throws IOException, InterruptedException, IntegrationException {
        final String repo = "blackducksoftware/whiteouttest";
        final String tag = "1.0";
        final File outputContainerFileSystemFile = IntegrationTestCommon.getOutputContainerFileSystemFileFromTarFilename("aggregated.tar");
        IntegrationTestCommon.testTar(programVersion, "build/images/test/aggregated.tar", repo.replaceAll("/", "_"), repo, tag, tag, "var_lib_dpkg", true, true, additionalArgsWithServiceUrl, true, outputContainerFileSystemFile,
                minikubeDockerEnv);
    }

    @Test
    public void testAggregateTarfileImageTwoStartContainer() throws IOException, InterruptedException, IntegrationException {
        final String repo = "blackducksoftware/centos_minus_vim_plus_bacula";
        final String tag = "1.0";
        final File outputContainerFileSystemFile = IntegrationTestCommon.getOutputContainerFileSystemFileFromTarFilename("aggregated.tar");
        IntegrationTestCommon.testTar(programVersion, "build/images/test/aggregated.tar", repo.replaceAll("/", "_"), repo, tag, tag, "var_lib_rpm", true, true, additionalArgsWithServiceUrl, true, outputContainerFileSystemFile,
                minikubeDockerEnv);
    }

    @Test
    public void testAlpineLatestTarRepoTagSpecifiedStartContainer() throws IOException, InterruptedException, IntegrationException {
        final String repo = "alpine";
        final String tag = "latest";
        final File outputContainerFileSystemFile = IntegrationTestCommon.getOutputContainerFileSystemFileFromTarFilename("alpine.tar");
        IntegrationTestCommon.testTar(programVersion, "build/images/test/alpine.tar", repo.replaceAll("/", "_"), repo, tag, tag, "lib_apk", false, true, additionalArgsWithServiceUrl, true, outputContainerFileSystemFile, minikubeDockerEnv);
    }

    @Test
    public void testAlpineLatestTarRepoTagNotSpecifiedStartContainer() throws IOException, InterruptedException, IntegrationException {
        final String repo = "alpine";
        final String tag = null;
        final File outputContainerFileSystemFile = IntegrationTestCommon.getOutputContainerFileSystemFileFromTarFilename("alpine.tar");
        IntegrationTestCommon.testTar(programVersion, "build/images/test/alpine.tar", repo, tag, null, "latest", "lib_apk", false, true, additionalArgsWithServiceUrl, true, outputContainerFileSystemFile, minikubeDockerEnv);
    }
}
