# ⚡ Text Quick Viewer

> Syntax-highlighted instant previews for text and source files inside [Nuclr Commander](https://nuclr.dev).

No tab switching. No app hopping. Just hit **Ctrl+Q** and get the file in front of you, fast. 😎

![Text Quick Viewer screenshot](images/screenshots-1.jpg)

## 🚀 What It Does

Text Quick Viewer turns Nuclr Commander into a slick code-and-text preview machine:

- 🔥 Syntax highlighting for 50+ languages and formats via RSyntaxTextArea
- 🧭 Line numbers for quick scanning and reference
- 🪄 Code folding where the language supports it
- 🌑 Dark styling that fits Commander without looking bolted on
- 🛡️ Large-file guard that stops previews over 10 MB from freezing the UI
- 🧪 Binary-file detection that quietly skips files that should not be rendered as text
- 📦 NIO.2 support, including files inside ZIP and JAR archives
- ⛔ Cancellation-aware loading so switching files does not leave stale content behind

## 🎯 Supported File Types

| Category | Extensions |
|----------|-----------|
| Plain text | `txt`, `log`, `csv`, `md` |
| Web | `html`, `htm`, `css`, `js`, `mjs`, `ts`, `tsx`, `jsp` |
| Data / config | `json`, `yaml`, `yml`, `toml`, `ini`, `conf`, `cfg`, `properties`, `prefs`, `pref` |
| JVM | `java`, `kt`, `scala`, `groovy`, `gradle` |
| Systems | `c`, `cpp`, `h`, `hpp`, `cs`, `go`, `rs` |
| Scripting | `py`, `rb`, `php`, `lua`, `perl`, `pl`, `dart`, `sql` |
| Shell / scripts | `sh`, `bash`, `bat`, `cmd`, `ps1` |
| XML / markup | `xml`, `svg`, `html`, `htm`, `jsp`, `csproj`, `classpath`, `project`, `factorypath` |
| IDE / project | `vsconfig`, `firebaserc` |
| Dotfiles | `.gitignore`, `.gitattributes`, `.meta` |
| Container | `dockerfile` |

## ⚙️ Build

Requires **Java 21+** and **Maven 3.9+**.

Install the Nuclr plugin SDK first:

```bash
# 1. One-time SDK install
cd ../../..
mvn clean install

# 2. Build this plugin
cd plugins/core/quick-viewer-text
mvn clean package -Dmaven.verify.skip=true
```

Output:

```text
target/quick-view-text-1.0.0.zip
```

### 🔐 Signed Build

Signing expects the Nuclr keystore at `C:/nuclr/key/nuclr-signing.p12`.

```bash
mvn clean verify -Djarsigner.storepass=<password>
```

Outputs:

```text
target/quick-view-text-1.0.0.zip
target/quick-view-text-1.0.0.zip.sig
```

### 📥 Deploy To Commander

Copy the build artifacts into Commander’s `plugins/` directory:

```bash
cp target/quick-view-text-1.0.0.zip /path/to/commander/plugins/
cp target/quick-view-text-1.0.0.zip.sig /path/to/commander/plugins/
```

## 🧠 How It Works

```text
TextQuickViewProvider  -> decides whether a file looks like supported text
TextQuickViewPanel     -> loads content and renders it in RSyntaxTextArea
```

### Loading Flow

1. `matches(item)` checks the extension with no I/O.
2. `open(item, cancelled)` delegates to `TextQuickViewPanel.load(...)` on a virtual thread.
3. Loading performs:
   - file-size guard
   - binary scan of the first 8 KB
   - content read via `Path` or `InputStream`
   - EDT handoff for final UI update
4. `setText(...)` selects syntax style and swaps in a fresh document.

### Threading Model

- `EDT`: Swing updates, document swaps, repainting
- `Virtual thread`: file I/O and binary detection
- `AtomicBoolean cancelled`: prevents stale UI updates when the user changes selection mid-load

### Bundled Dependency

| Artifact | Version | License |
|----------|---------|---------|
| `rsyntaxtextarea` | 3.6.1 | BSD 3-Clause |

RSyntaxTextArea is already present in the Commander fat JAR. The bundled copy keeps the plugin self-contained if it is deployed into a different host environment.

## 📄 License

Apache 2.0. See [LICENSE](LICENSE).
