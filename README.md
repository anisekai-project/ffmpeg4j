# FFMpeg4j - A Java Wrapper for FFmpeg

FFMpeg4j is a lightweight Java library that provides a clean, fluent builder API for interacting with the `ffmpeg` and
`ffprobe` command-line tools. It is designed to simplify common multimedia tasks by abstracting away the complexity of
command-line arguments into type-safe, readable Java code.

**Important Note:** This is a **wrapper**, not a complete Java implementation of FFmpeg. It exposes a focused subset of
FFmpeg's vast capabilities, tailored for common operations like probing, converting, splitting, and combining media
files. It is not a 1-to-1 adaptation of all FFmpeg features.

---

## Features

* **Fluent Builder API**: Construct complex `ffmpeg` commands with a chained, easy-to-read syntax.
* **Media Probing**: Easily parse media file metadata using `ffprobe` and work with the results as structured Java
  objects (`MediaFile`, `MediaStream`).
* **Media Conversion**:
    * Transcode video, audio, and subtitle streams to different formats.
    * Copy streams without re-encoding (remuxing) for fast, lossless operations.
    * Selectively include or exclude entire stream types (e.g., remove all subtitles).
* **Stream Splitting**: Extract all (or selected) streams from a media container into individual files.
* **Media Combining**: Merge separate video, audio, and subtitle files into a single output file with specified
  metadata.
* **MPEG-DASH Support**: Generate MPEG-DASH manifests (`.mpd`) and corresponding media segments for adaptive streaming.
* **Type-Safe Enums**: Use enums for `Codec`, `CodecType`, and `Disposition` to avoid errors from raw string
  manipulation.
* **Extensible**: Customize command generation with functional interfaces like `MediaStreamMapper`.

---

## Prerequisites

You **must** have `ffmpeg` and `ffprobe` installed on your system and accessible in your shell's `PATH`. This library
simply builds and executes commands for these binaries; it does not include them.

You can download FFmpeg from the official website: [ffmpeg.org](https://ffmpeg.org/download.html)

---

## Installation

**Maven:**

```xml

<dependency>
  <groupId>fr.anisekai</groupId>
  <artifactId>ffmpeg4j</artifactId>
  <version>1.0.0</version>
</dependency>
```

**Gradle:**

```groovy
implementation 'fr.anisekai:ffmpeg4j:1.0.0'
```

---

## Usage

The primary entry point is the `FFMpeg` utility class, which provides static methods to start building a command.

### 1. Probing a Media File

Use `FFMpeg.probe()` to get detailed information about the streams within a file.

```java
import fr.anisekai.media.MediaFile;
import fr.anisekai.media.MediaStream;
import fr.anisekai.media.bin.FFMpeg;
import fr.anisekai.media.enums.CodecType;

import java.nio.file.Path;
import java.nio.file.Paths;

// ...

try{
  Path inputFile = Paths.get("path/to/your/video.mkv");
  MediaFile mediaFile = MediaFile.of(inputFile);

  System.out.println("File: " + mediaFile.getPath());
  System.out.println("Total streams: " + mediaFile.getStreams().size());

  // Get specific stream types
  System.out.println("Audio streams: " + mediaFile.getStreams(CodecType.AUDIO).size());

  // Iterate over all streams
  for(MediaStream stream : mediaFile.getStreams()){
    System.out.printf(
      "  Stream #%d: Type=%s, Codec=%s, Language=%s%n",
      stream.getId(),
      stream.getCodec().getType(),
      stream.getCodec().name(),
      stream.getMetadata().getOrDefault("language","N/A"));
  }
} catch(IOException | InterruptedException e){
  e.printStackTrace();
}
```

### 2. Converting a File

Use `FFMpeg.convert()` to transcode a file. The builder lets you specify codecs for each stream type.

```java
MediaFile mediaFile = MediaFile.of(Paths.get("input.mkv"));
Path outputFile = Paths.get("output.mp4");

// Convert video to H.264, audio to AAC, and remove all subtitles
Path resultPath = FFMpeg.convert(mediaFile)
                        .video(Codec.H264)
                        .audio(Codec.AAC)
                        .noSubtitle()
                        .file(outputFile)
                        .timeout(5, TimeUnit.MINUTES) // Set a 5-minute timeout
                        .run();

System.out.println("Conversion complete. Output file: " + resultPath);
```

### 3. Splitting a File into Streams (Remuxing)

To quickly extract all streams without re-encoding, use the "copy" codecs and the `split()` terminal operation.

```java
MediaFile mediaFile = MediaFile.of(Paths.get("input.mkv"));
Path outputDir = Paths.get("extracted_streams");

// Create the output directory if it doesn't exist
Files.createDirectories(outputDir);

// Copy all streams into separate files
Map<MediaStream, Path> outputFiles = FFMpeg.convert(mediaFile)
                                           .copyVideo()
                                           .copyAudio()
                                           .copySubtitle()
                                           .into(outputDir) // Set the output directory
                                           .split() // Specify that we want separate files for each stream
                                           .run();

outputFiles.forEach((stream, path) -> {
  System.out.printf("Extracted stream %d to %s%n",stream.getId(),path);
});
```

### 4. Combining Separate Files

Use `FFMpeg.combine()` to merge multiple tracks into a single container. You create `MediaMeta` objects to describe each
input file.

```java
Path videoFile = Paths.get("track0.mp4");
Path audioFile1 = Paths.get("track1.m4a");
Path audioFile2 = Paths.get("track2.opus");
Path subtitleFile = Paths.get("track3.srt");
Path outputFile = Paths.get("combined.mkv");

// Describe each input stream with its type and optional metadata
MediaMeta videoMeta = new MediaMeta(videoFile, CodecType.VIDEO, null, null);
MediaMeta audioMeta1 = new MediaMeta(audioFile1, CodecType.AUDIO, "Stereo", "eng");
MediaMeta audioMeta2 = new MediaMeta(audioFile2, CodecType.AUDIO, "Surround", "eng");
MediaMeta subtitleMeta = new MediaMeta(subtitleFile, CodecType.SUBTITLE, "English Subs", "eng");

// Build and run the combine command
Path resultPath = FFMpeg.combine(videoMeta)
                        .with(audioMeta1)
                        .with(audioMeta2)
                        .with(subtitleMeta)
                        .file(outputFile)
                        .run();

System.out.println("Successfully combined files into: "+resultPath);
```

### 5. Creating an MPEG-DASH Manifest

Use `FFMpeg.mdp()` to generate chunks and a manifest for adaptive streaming.

```java
MediaFile mediaFile = MediaFile.of(Paths.get("source.mp4"));
Path outputDir = Paths.get("dash_output");

// This command will create 'outputDir' and populate it with
// a 'manifest.mpd' file and corresponding media segments.
Path manifestPath = FFMpeg.mdp(mediaFile)
                          .into(outputDir)
                          .as("manifest.mpd")
                          .run();

System.out.println("DASH manifest created at: "+manifestPath);
```

---

## Dependencies

* [org.json:json](https://github.com/stleary/JSON-java): For parsing the JSON output from `ffprobe`.

---

## License

This project is licensed under the Apache License 2.0. See the `LICENSE` file for details.
