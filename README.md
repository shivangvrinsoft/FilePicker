# FilePicker

A modern Android File Picker library built with Kotlin and Material 3.

## Features

* 📸 Camera Capture
* 🎥 Video Capture
* 🖼 Gallery Picker
* 📄 Document Picker
* 📁 All Files Picker
* 🔄 Recent Files Support
* 🎨 Material 3 UI
* 🔒 Modern Android Permission Handling
* 📱 Android 8.0+ Support

## Screenshots

*Add screenshots here.*

## Installation

### Gradle

```gradle
implementation("com.yourpackage:filepicker:x.x.x")
```

## Usage

### Open Gallery

```kotlin
// Example
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

The library follows modern Android guidelines:

* Camera → CAMERA
* Gallery → Android Photo Picker
* Documents → Storage Access Framework (SAF)

No broad storage permissions required.

## Roadmap

* [ ] Multi-file selection
* [ ] Compose support
* [ ] Cloud provider support
* [ ] File preview support

## Contributing

Pull requests are welcome. For major changes, please open an issue first.

## License

Licensed under the Apache License 2.0.
