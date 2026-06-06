package dev.nuclr.plugin.core.quick.viewer.text;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TextFileSupportTest {

	@Test
	void supportsExtensionlessUtf8Text(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("WHATSNEW");
		Files.writeString(file, "Nuclr Commander\n\n- Added quick view improvements\n");

		assertTrue(TextFileSupport.supports(file));
	}

	@Test
	void supportsShebangScriptWithoutExtension(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("deploy");
		Files.writeString(file, "#!/bin/sh\necho hello\n");

		assertTrue(TextFileSupport.supports(file));
	}

	@Test
	void rejectsKnownBinarySignature(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("archive");
		Files.write(file, new byte[] { 'P', 'K', 3, 4, 20, 0 });

		assertFalse(TextFileSupport.supports(file));
	}
}
