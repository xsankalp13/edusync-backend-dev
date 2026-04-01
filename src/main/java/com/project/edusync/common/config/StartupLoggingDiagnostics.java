package com.project.edusync.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
@Slf4j
public class StartupLoggingDiagnostics implements ApplicationRunner {

    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) {
        String[] activeProfiles = environment.getActiveProfiles();
        String profiles = activeProfiles.length == 0 ? "default" : String.join(",", activeProfiles);
        String logFile = environment.getProperty("logging.file.name", "logs/edusync-dev.log");

        log.info("Startup diagnostics: activeProfiles={} logFile={}", profiles, logFile);

        if (activeProfiles.length > 0) {
            log.debug("Startup diagnostics detail: activeProfilesArray={}", Arrays.toString(activeProfiles));
        }
    }
}

