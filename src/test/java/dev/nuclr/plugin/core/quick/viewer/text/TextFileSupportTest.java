package dev.nuclr.plugin.core.quick.viewer.text;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.nuclr.platform.plugin.NuclrResource;

class TextFileSupportTest {

	@Test
	void supportsExtensionlessUtf8Text(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("WHATSNEW");
		Files.writeString(file, "Nuclr Commander\n\n- Added quick view improvements\n");

		assertTrue(TextFileSupport.supports(resource(file)));
	}

	@Test
	void supportsShebangScriptWithoutExtension(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("deploy");
		Files.writeString(file, "#!/bin/sh\necho hello\n");

		assertTrue(TextFileSupport.supports(resource(file)));
	}

	@Test
	void rejectsKnownBinarySignature(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("archive");
		Files.write(file, new byte[] { 'P', 'K', 3, 4, 20, 0 });

		assertFalse(TextFileSupport.supports(resource(file)));
	}

	private static NuclrResource resource(Path path) {
		NuclrResource resource = new NuclrResource(path) {
			@Override
			public java.io.InputStream openInputStream(java.nio.file.OpenOption... options) throws Exception {
				return Files.newInputStream(getPath(), options);
			}
		};
		resource.setName(path.getFileName().toString());
		return resource;
	}
}
