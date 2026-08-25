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

	@Test
	void headCutsAtTheLastCompleteLine(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("big.log");
		Files.writeString(file, "alpha\nbravo\ncharlie\ndelta\n");

		// 14 bytes lands in the middle of "charlie".
		var snippet = TextFileSupport.head(resource(file), 14);

		assertEquals("alpha\nbravo\n", snippet.text());
		assertEquals(2, snippet.lines());
		assertEquals(12, snippet.bytes());
	}

	@Test
	void headKeepsTheWholeFileWhenItFits(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("small.log");
		Files.writeString(file, "alpha\nbravo\n");

		var snippet = TextFileSupport.head(resource(file), 4096);

		assertEquals("alpha\nbravo\n", snippet.text());
		assertEquals(2, snippet.lines());
	}

	@Test
	void headKeepsAPartialLineWhenThereIsNoLineBreak(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("oneline.json");
		Files.writeString(file, "{\"a\":1,\"b\":2,\"c\":3}");

		var snippet = TextFileSupport.head(resource(file), 8);

		// Cutting at a line break would leave nothing to show for a single-line file.
		assertEquals("{\"a\":1,\"", snippet.text());
		assertEquals(1, snippet.lines());
	}

	@Test
	void headDropsACharacterSplitByTheCut(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("utf8.txt");
		Files.writeString(file, "héllo");

		// 2 bytes: 'h' plus the first half of the two-byte 'é'.
		var snippet = TextFileSupport.head(resource(file), 2);

		assertEquals("h", snippet.text());
	}

	@Test
	void headOfAnEmptyFileIsEmpty(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("empty.txt");
		Files.writeString(file, "");

		var snippet = TextFileSupport.head(resource(file), 4096);

		assertEquals("", snippet.text());
		assertEquals(0, snippet.lines());
		assertEquals(0, snippet.bytes());
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
