package com.blackducksoftware.integration.hub.docker.imageinspector.config;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

public class ProgramPathUtils {
    public static String getCodeLocationName(final String codelocationPrefix, final String imageName, final String imageTag, final String pkgMgrFilePath, final String pkgMgrName) {
        if (!StringUtils.isBlank(codelocationPrefix)) {
            return String.format("%s_%s_%s_%s_%s", codelocationPrefix, slashesToUnderscore(imageName), imageTag, slashesToUnderscore(pkgMgrFilePath), pkgMgrName);
        }
        return String.format("%s_%s_%s_%s", slashesToUnderscore(imageName), imageTag, slashesToUnderscore(pkgMgrFilePath), pkgMgrName);
    }

    private static String slashesToUnderscore(final String imageName) {
        return imageName.replaceAll("/", "_");
    }

    public static String getBdioFilename(final String imageName, final String pkgMgrFilePath, final String hubProjectName, final String hubVersionName) {
        return createBdioFilename(cleanImageName(imageName), cleanPath(pkgMgrFilePath), cleanHubProjectName(hubProjectName), hubVersionName);
    }

    private static String createBdioFilename(final String cleanImageName, final String cleanPkgMgrFilePath, final String cleanHubProjectName, final String hubVersionName) {
        final String[] parts = new String[4];
        parts[0] = cleanImageName;
        parts[1] = cleanPkgMgrFilePath;
        parts[2] = cleanHubProjectName;
        parts[3] = hubVersionName;

        String filename = generateFilename(cleanImageName, cleanPkgMgrFilePath, cleanHubProjectName, hubVersionName);
        for (int i = 0; (filename.length() >= 255) && (i < 4); i++) {
            parts[i] = DigestUtils.sha1Hex(parts[i]);
            if (parts[i].length() > 15) {
                parts[i] = parts[i].substring(0, 15);
            }

            filename = generateFilename(parts[0], parts[1], parts[2], parts[3]);
        }
        return filename;
    }

    public static String cleanImageName(final String imageName) {
        return colonsToUnderscores(slashesToUnderscore(imageName));
    }

    private static String cleanHubProjectName(final String hubProjectName) {
        return slashesToUnderscore(hubProjectName);
    }

    public static String getContainerFileSystemTarFilename(final String imageName, final String tagName) {
        return String.format("%s_%s_containerfilesystem.tar.gz", slashesToUnderscore(imageName), tagName);
    }

    private static String colonsToUnderscores(final String imageName) {
        return imageName.replaceAll(":", "_");
    }

    private static String generateFilename(final String cleanImageName, final String cleanPkgMgrFilePath, final String cleanHubProjectName, final String hubVersionName) {
        return String.format("%s_%s_%s_%s_bdio.jsonld", cleanImageName, cleanPkgMgrFilePath, cleanHubProjectName, hubVersionName);
    }

    private static String cleanPath(final String path) {
        return slashesToUnderscore(path);
    }
}
