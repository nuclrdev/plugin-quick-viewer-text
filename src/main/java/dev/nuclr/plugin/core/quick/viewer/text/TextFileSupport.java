package dev.nuclr.plugin.core.quick.viewer.text;

import java.util.Map;
import java.util.Set;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

final class TextFileSupport {

	private static final Set<String> TEXT_EXTENSIONS = Set.of(
			// Plain text / markup
			"txt", "md", "log", "csv",
			"html", "htm", "css", "xml", "jsp",
			// Data / config
			"json", "yaml", "yml", "toml",
			"ini", "conf", "cfg", "properties", "prefs", "pref",
			// IDE / project files
			"classpath", "project", "factorypath",
			"csproj", "vsconfig", "firebaserc",
			// Web / scripting
			"js", "mjs", "ts", "tsx",
			// System languages
			"java", "py", "rb",
			"c", "cpp", "h", "hpp",
			"cs", "go", "rs", "php",
			"kt", "scala", "groovy", "gradle",
			"lua", "perl", "pl", "dart", "sql",
			// Crypto
			"pub", "ppk",
			// Shells / scripts
			"sh", "bash", "bat", "cmd", "ps1");

	private static final Set<String> TEXT_FILENAMES = Set.of(
			"license", "mf",
			"dockerfile", "mvnw",
			"gitignore", "gitattributes", "meta");

	private static final Map<String, String> SYNTAX_BY_EXTENSION = Map.ofEntries(
			Map.entry("java",        SyntaxConstants.SYNTAX_STYLE_JAVA),
			Map.entry("js",          SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT),
			Map.entry("mjs",         SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT),
			Map.entry("ts",          SyntaxConstants.SYNTAX_STYLE_TYPESCRIPT),
			Map.entry("tsx",         SyntaxConstants.SYNTAX_STYLE_TYPESCRIPT),
			Map.entry("json",        SyntaxConstants.SYNTAX_STYLE_JSON),
			Map.entry("xml",         SyntaxConstants.SYNTAX_STYLE_XML),
			Map.entry("classpath",   SyntaxConstants.SYNTAX_STYLE_XML),
			Map.entry("factorypath", SyntaxConstants.SYNTAX_STYLE_XML),
			Map.entry("project",     SyntaxConstants.SYNTAX_STYLE_XML),
			Map.entry("csproj",      SyntaxConstants.SYNTAX_STYLE_XML),
			Map.entry("html",        SyntaxConstants.SYNTAX_STYLE_HTML),
			Map.entry("htm",         SyntaxConstants.SYNTAX_STYLE_HTML),
			Map.entry("jsp",         SyntaxConstants.SYNTAX_STYLE_HTML),
			Map.entry("css",         SyntaxConstants.SYNTAX_STYLE_CSS),
			Map.entry("py",          SyntaxConstants.SYNTAX_STYLE_PYTHON),
			Map.entry("rb",          SyntaxConstants.SYNTAX_STYLE_RUBY),
			Map.entry("sh",          SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL),
			Map.entry("bash",        SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL),
			Map.entry("bat",         SyntaxConstants.SYNTAX_STYLE_WINDOWS_BATCH),
			Map.entry("cmd",         SyntaxConstants.SYNTAX_STYLE_WINDOWS_BATCH),
			Map.entry("sql",         SyntaxConstants.SYNTAX_STYLE_SQL),
			Map.entry("c",           SyntaxConstants.SYNTAX_STYLE_C),
			Map.entry("h",           SyntaxConstants.SYNTAX_STYLE_C),
			Map.entry("cpp",         SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS),
			Map.entry("hpp",         SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS),
			Map.entry("cs",          SyntaxConstants.SYNTAX_STYLE_CSHARP),
			Map.entry("go",          SyntaxConstants.SYNTAX_STYLE_GO),
			Map.entry("rs",          SyntaxConstants.SYNTAX_STYLE_RUST),
			Map.entry("php",         SyntaxConstants.SYNTAX_STYLE_PHP),
			Map.entry("yaml",        SyntaxConstants.SYNTAX_STYLE_YAML),
			Map.entry("yml",         SyntaxConstants.SYNTAX_STYLE_YAML),
			Map.entry("toml",        SyntaxConstants.SYNTAX_STYLE_YAML),
			Map.entry("md",          SyntaxConstants.SYNTAX_STYLE_MARKDOWN),
			Map.entry("properties",  SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE),
			Map.entry("ini",         SyntaxConstants.SYNTAX_STYLE_INI),
			Map.entry("prefs",       SyntaxConstants.SYNTAX_STYLE_INI),
			Map.entry("cfg",         SyntaxConstants.SYNTAX_STYLE_INI),
			Map.entry("groovy",      SyntaxConstants.SYNTAX_STYLE_GROOVY),
			Map.entry("gradle",      SyntaxConstants.SYNTAX_STYLE_GROOVY),
			Map.entry("kt",          SyntaxConstants.SYNTAX_STYLE_KOTLIN),
			Map.entry("scala",       SyntaxConstants.SYNTAX_STYLE_SCALA),
			Map.entry("lua",         SyntaxConstants.SYNTAX_STYLE_LUA),
			Map.entry("perl",        SyntaxConstants.SYNTAX_STYLE_PERL),
			Map.entry("pl",          SyntaxConstants.SYNTAX_STYLE_PERL),
			Map.entry("dart",        SyntaxConstants.SYNTAX_STYLE_DART),
			Map.entry("csv",         SyntaxConstants.SYNTAX_STYLE_CSV),
			Map.entry("vsconfig",    SyntaxConstants.SYNTAX_STYLE_JSON),
			Map.entry("firebaserc",  SyntaxConstants.SYNTAX_STYLE_JSON)
	);

	private static final Map<String, String> SYNTAX_BY_FILENAME = Map.ofEntries(
			Map.entry("dockerfile",  SyntaxConstants.SYNTAX_STYLE_DOCKERFILE),
			Map.entry("classpath",   SyntaxConstants.SYNTAX_STYLE_XML),
			Map.entry("factorypath", SyntaxConstants.SYNTAX_STYLE_XML),
			Map.entry("project",     SyntaxConstants.SYNTAX_STYLE_XML),
			Map.entry("firebaserc",  SyntaxConstants.SYNTAX_STYLE_JSON),
			Map.entry("vsconfig",    SyntaxConstants.SYNTAX_STYLE_JSON)
	);

	private TextFileSupport() {
	}

	static boolean matches(String filename) {
		if (filename == null || filename.isBlank()) {
			return false;
		}

		String normalizedName = normalizeFileName(filename);
		return TEXT_FILENAMES.contains(normalizedName) || TEXT_EXTENSIONS.contains(extension(filename));
	}

	static boolean matchesExtension(String extension) {
		if (extension == null || extension.isBlank()) {
			return false;
		}
		return TEXT_EXTENSIONS.contains(extension.toLowerCase()); 
	}

	static String syntaxStyle(String filename) {
		String style = SYNTAX_BY_FILENAME.get(normalizeFileName(filename));
		if (style != null) {
			return style;
		}
		return SYNTAX_BY_EXTENSION.getOrDefault(extension(filename), SyntaxConstants.SYNTAX_STYLE_NONE);
	}

	private static String normalizeFileName(String filename) {
		String normalized = filename.toLowerCase();
		if (normalized.startsWith(".")) {
			return normalized.substring(1);
		}
		return normalized;
	}

	private static String extension(String filename) {
		if (filename == null || filename.isBlank()) {
			return "";
		}
		int dot = filename.lastIndexOf('.');
		return dot > 0 ? filename.substring(dot + 1).toLowerCase() : "";
	}
}
