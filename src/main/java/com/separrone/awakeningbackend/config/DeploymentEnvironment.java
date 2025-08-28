package com.separrone.awakeningbackend.config;

public enum DeploymentEnvironment {
    LOCAL_DEV,
    REMOTE_DEV,
    PRODUCTION;

    public static DeploymentEnvironment fromProfiles(String[] activeProfiles) {
        if (activeProfiles.length == 0) {
            return LOCAL_DEV;
        }

        return switch (activeProfiles[0]) {
            case "prod" -> PRODUCTION;
            case "dev-remote" -> REMOTE_DEV;
            default -> LOCAL_DEV;
        };
    }
}