# FilePicker

A modern Android File Picker library built with Kotlin and Material 3, providing a clean and consistent experience for selecting files, images, videos, and documents on Android.

## Features

* 📸 Camera Capture
* 🎥 Video Capture
* 🖼 Gallery Picker
* 📄 Document Picker
* 📁 All Files Picker
* 🔄 Recent Files Support
* 🎨 Material 3 Design
* 🔒 Modern Android Permission Handling
* ⚡ Lightweight and Easy to Integrate

## Requirements

* Android 8.0 (API 26) or higher
* Kotlin support
* AndroidX

## Installation

### Gradle

```gradle
implementation("com.yourpackage:filepicker:x.x.x")
```

## Usage

### Open Gallery

```kotlin
launchGalleryPicker()
```

### Open Documents

```kotlin
launchDocumentPicker()
```

### Open All Files

```kotlin
launchAllFilePicker()
```

## Permissions

The library follows modern Android storage and media access guidelines.

| Feature             | Permission                     |
| ------------------- | ------------------------------ |
| Camera Capture      | CAMERA                         |
| Gallery Selection   | Android Photo Picker           |
| Document Selection  | Storage Access Framework (SAF) |
| All Files Selection | Storage Access Framework (SAF) |

No broad storage permissions are required for supported Android versions.

## Screenshots

*Add screenshots and GIF demonstrations here.*

## Roadmap

* [ ] Multi-file selection
* [ ] Jetpack Compose support
* [ ] Cloud storage integration
* [ ] File preview support
* [ ] Custom themes and styling

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

* Follow Kotlin coding conventions.
* Keep pull requests focused on a single feature or fix.
* Update documentation when necessary.
* Ensure the project builds successfully before submitting a PR.

## Support

If you encounter any issues or have feature requests:

1. Check existing Issues.
2. Create a new Issue with detailed information.
3. Include steps to reproduce bugs whenever possible.

## License

This project is licensed under the Apache License 2.0.

See the LICENSE file for details.

## Maintainer

Maintained by **Shivang Modi**.

* GitHub: https://github.com/shivangvrinsoft

If you have questions, suggestions, or encounter any issues, please open an Issue or Discussion in this repository.
