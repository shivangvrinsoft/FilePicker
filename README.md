# FilePicker
[![](https://jitpack.io/v/shivangvrinsoft/FilePicker.svg)](https://jitpack.io/#shivangvrinsoft/FilePicker)

A modern Android File Picker library built with Kotlin and Material 3, providing a clean and consistent experience for selecting files, images, videos, and documents on Android.

## Features

- 📸 Camera Capture
- 🎥 Video Capture
- 🖼 Gallery Picker
- 📄 Document Picker
- 📁 All Files Picker
- 🔄 Recent Files Support
- 🎨 Material 3 Design
- 🔒 Modern Android Permission Handling
- ⚡ Lightweight and Easy to Integrate

## Requirements

- Android 8.1 (API 27) or higher
- Kotlin support
- AndroidX

## Installation

### Step 1 — Add JitPack to `settings.gradle.kts`

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### Step 2 — Add the dependency in `app/build.gradle.kts`

```kotlin
dependencies {
    implementation("com.github.shivangvrinsoft:FilePicker:v1.0.1")
}
```

## Usage

### Step 1 — Create a `FilePickerDelegate` at class level

The delegate must be initialized before the Activity is started. Use `by lazy` and touch it in `onCreate`.

```kotlin
class YourActivity : AppCompatActivity() {

    // Register delegate at class level — before STARTED
    private val filePickerDelegate by lazy {
        FilePickerDelegate(
            activity     = this,
            maxSelection = 5,
            onResult     = { files -> onFilesPicked(files) }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Force delegate init before Activity is STARTED
        filePickerDelegate
    }
}
```

### Step 2 — Register a result launcher

```kotlin
private val filePickerLauncher = FilePicker.registerForResult(this) { files ->
    // Handle returned files
    files.forEach { file ->
        Log.d("FilePicker", "Name: ${file.displayName}, Size: ${file.sizeBytes}")
    }
}
```

### Step 3 — Show the Bottom Sheet Picker

```kotlin
binding.btnOpenPicker.setOnClickListener {
    FilePicker.Builder(this)
        .launcher(filePickerLauncher)
        .maxSelection(5)
        .showBottomSheet(this, filePickerDelegate)
}
```

### Step 4 — Handle the result

```kotlin
private fun onFilesPicked(files: List<ResultFileItem>) {
    files.forEach { file ->
        Log.d("FilePicker", "File: ${file.displayName}")
        Log.d("FilePicker", "MIME: ${file.mimeType}")
        Log.d("FilePicker", "Size: ${file.sizeBytes}")
        Log.d("FilePicker", "URI:  ${file.uriString}")
    }
}
```

### Complete Example

```kotlin
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val filePickerDelegate by lazy {
        FilePickerDelegate(
            activity     = this,
            maxSelection = 5,
            onResult     = { files -> onFilesPicked(files) }
        )
    }

    private val filePickerLauncher = FilePicker.registerForResult(this) { files ->
        onFilesPicked(files)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        filePickerDelegate // force init

        binding.btnOpenPicker.setOnClickListener {
            FilePicker.Builder(this)
                .launcher(filePickerLauncher)
                .maxSelection(5)
                .showBottomSheet(this, filePickerDelegate)
        }
    }

    private fun onFilesPicked(files: List<ResultFileItem>) {
        files.forEach { file ->
            Log.d("FilePicker", "${file.displayName} — ${file.mimeType}")
        }
    }
}
```

## ResultFileItem

Each picked file is returned as a `ResultFileItem`:

| Property | Type | Description |
|---|---|---|
| `displayName` | `String` | File name with extension |
| `mimeType` | `String` | MIME type (e.g. `image/jpeg`) |
| `sizeBytes` | `Long` | File size in bytes |
| `uriString` | `String` | Content URI as string |
| `durationMs` | `Long` | Duration in ms (videos only, else 0) |
| `dateAdded` | `Long` | Unix timestamp when file was added |

## Permissions

The library follows modern Android storage and media access guidelines. Permissions are declared in the library manifest and merged automatically into your app — no manual changes needed.

| Feature             | Permission                      |
|---------------------|---------------------------------|
| Camera Capture      | `CAMERA`                        |
| Gallery Selection   | `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO` |
| Document Selection  | Storage Access Framework (SAF)  |
| All Files Selection | Storage Access Framework (SAF)  |

No broad storage permissions are required for supported Android versions.

## Screenshots

*Add screenshots and GIF demonstrations here.*

## Roadmap

- [ ] Multi-file selection
- [ ] Jetpack Compose support
- [ ] Cloud storage integration
- [ ] File preview support
- [ ] Custom themes and styling

## Contributing

Contributions are welcome and appreciated.

### How to Contribute

1. Fork the repository.
2. Create a feature branch:
```bash
git checkout -b feature/your-feature-name
```
3. Commit your changes:
```bash
git commit -m "Add your feature"
```
4. Push the branch:
```bash
git push origin feature/your-feature-name
```
5. Open a Pull Request against the `development` branch.

### Contribution Guidelines

- Follow Kotlin coding conventions.
- Keep pull requests focused on a single feature or fix.
- Update documentation when necessary.
- Ensure the project builds successfully before submitting a PR.

## Support

If you encounter any issues or have feature requests:

1. Check existing [Issues](https://github.com/shivangvrinsoft/FilePicker/issues).
2. Create a new Issue with detailed information.
3. Include steps to reproduce bugs whenever possible.

## License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.

## Maintainer

Maintained by **Shivang Modi**.

- GitHub: [shivangvrinsoft](https://github.com/shivangvrinsoft)

If you have questions, suggestions, or encounter any issues, please open an Issue or Discussion in this repository.
