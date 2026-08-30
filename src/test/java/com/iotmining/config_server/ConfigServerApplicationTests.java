package com.iotmining.config_server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Previously ran with no active profile at all - which now correctly
 * fails, since application.yml's real default is "prod" (the comment used
 * to claim "dev" but the code always defaulted to prod - see that file's
 * own fix), and prod requires a real SPRING_SECURITY_CONFIG_PASSWORD with
 * no fallback. That's the credential-can't-silently-default-to-something-
 * insecure behavior working as intended, not a bug to route around -
 * fixed by giving this test a real profile instead, same as
 * SecurityConfigTest.
 */
@SpringBootTest
@ActiveProfiles({"test", "native"})
class ConfigServerApplicationTests {

	@Test
	void contextLoads() {
	}

}
