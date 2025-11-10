package fr.anisekai.media.enums;

import org.jetbrains.annotations.Nullable;

/**
 * Represents various multimedia codecs used for video, audio, and subtitle encoding/decoding.
 * <p>
 * Each codec is associated with a {@link CodecType}, a default file extension, and the corresponding library codec
 * name.
 */
public enum Codec {

    // <editor-fold desc="Video Codecs">
    /**
     * H264 Video Codec
     */
    H264(CodecType.VIDEO),
    /**
     * H265 Video Codec
     */
    H265(CodecType.VIDEO),
    /**
     * VP9 Video Codec
     */
    VP9(CodecType.VIDEO),
    /**
     * AV1 Video Codec
     */
    AV1(CodecType.VIDEO),
    /**
     * HEVC Video Codec
     */
    HEVC(CodecType.VIDEO),
    /**
     * PNG Video Codec
     */
    PNG(CodecType.VIDEO),
    /**
     * MJPEG Video Codec
     */
    MJPEG(CodecType.VIDEO),
    /**
     * Special codec meant to be used with ffmpeg to copy the input without conversion.
     */
    VIDEO_COPY(CodecType.VIDEO),
    // </editor-fold>

    // <editor-fold desc="Audio Codecs">
    /**
     * AAC Audio Codec
     */
    AAC(CodecType.AUDIO),
    /**
     * MP3 Audio Codec
     */
    MP3(CodecType.AUDIO),
    /**
     * OPUS Audio Codec
     */
    OPUS(CodecType.AUDIO),
    /**
     * VORBIS Audio Codec
     */
    VORBIS(CodecType.AUDIO),
    /**
     * FLAC Audio Codec
     */
    FLAC(CodecType.AUDIO),
    /**
     * AC3 Audio Codec
     */
    AC3(CodecType.AUDIO),
    /**
     * EAC3 Audio Codec
     */
    EAC3(CodecType.AUDIO),
    /**
     * DTS Audio Codec
     */
    DTS(CodecType.AUDIO),
    /**
     * TRUEHD Audio Codec
     */
    TRUEHD(CodecType.AUDIO),
    /**
     * ALAC Audio Codec
     */
    ALAC(CodecType.AUDIO),
    /**
     * Special codec meant to be used with ffmpeg to copy the input without conversion.
     */
    AUDIO_COPY(CodecType.AUDIO),
    // </editor-fold>

    // <editor-fold desc="Subtitles Codecs">
    /**
     * SUBRIP Subtitles Codec
     */
    SUBRIP(CodecType.SUBTITLE),
    /**
     * ASS Subtitles Codec
     */
    ASS(CodecType.SUBTITLE),
    /**
     * PGS Subtitles Codec
     */
    PGS(CodecType.SUBTITLE),
    /**
     * DVB_SUB Subtitles Codec
     */
    DVB_SUB(CodecType.SUBTITLE),
    /**
     * SSA Subtitles Codec
     */
    SSA(CodecType.SUBTITLE),
    /**
     * Special codec meant to be used with ffmpeg to copy the input without conversion.
     */
    SUBTITLE_COPY(CodecType.SUBTITLE);
    // </editor-fold>

    private final CodecType type;

    Codec(CodecType type) {

        this.type = type;
    }

    /**
     * Retrieve the type of this codec (video, audio, or subtitle).
     *
     * @return The {@link CodecType} associated with this codec
     */
    public CodecType getType() {

        return this.type;
    }

    /**
     * Returns the most common file extension associated with this codec.
     *
     * @return The default file extension (e.g., "mp4", "mp3")
     */
    public String getExtension() {

        return switch (this) {
            case H264, H265, HEVC -> "mp4";
            case VP9 -> "webm";
            case AV1 -> "mkv";
            case AAC, ALAC -> "m4a";
            case MP3 -> "mp3";
            case OPUS, VORBIS -> "ogg";
            case FLAC -> "flac";
            case AC3 -> "ac3";
            case EAC3 -> "eac3";
            case DTS -> "dts";
            case TRUEHD -> "mlp";
            case SUBRIP -> "srt";
            case ASS -> "ass";
            case PGS -> "sup";
            case DVB_SUB -> "sub";
            case SSA -> "ssa";
            case AUDIO_COPY, VIDEO_COPY, SUBTITLE_COPY -> throw new IllegalStateException(
                    "*_COPY codecs are special codec and thus does not support file extensions");
            // This feel so wrong
            case PNG -> "png";
            case MJPEG -> "jpg";
        };
    }

    /**
     * Retrieve the system library name used for this codec.
     *
     * @return The encoder library name (e.g., "libx264", "aac")
     */

    public String getLibName() {

        return switch (this) {
            case H264 -> "libx264";
            case H265 -> "libx265";
            case HEVC -> "hevc";
            case VP9 -> "libvpx-vp9";
            case AV1 -> "libaom-av1";
            case AAC -> "aac";
            case MP3 -> "libmp3lame";
            case OPUS -> "libopus";
            case VORBIS -> "libvorbis";
            case FLAC -> "flac";
            case AC3 -> "ac3";
            case EAC3 -> "eac3";
            case DTS -> "libdts";
            case TRUEHD -> "truedts";
            case ALAC -> "alac";
            case SUBRIP -> "srt";
            case ASS -> "ass";
            case PGS -> "pgssub";
            case DVB_SUB -> "dvbsub";
            case SSA -> "ssa";
            case VIDEO_COPY, AUDIO_COPY, SUBTITLE_COPY -> "copy";
            // This feel so wrong
            case PNG -> "libpng";
            case MJPEG -> "libjpeg";
        };
    }

    /**
     * Attempts to resolve a {@link Codec} from a given string.
     *
     * @param codecName
     *         The name to match (case-insensitive)
     *
     * @return The matching {@link Codec}, or {@code null} if no match is found
     */
    public static @Nullable Codec from(String codecName) {

        for (Codec codec : values()) {
            if (codec.name().equalsIgnoreCase(codecName)) {
                return codec;
            }
        }
        return null;
    }

    /**
     * Retrieve the mimeType associated to the current {@link Codec}.
     *
     * @return The mimeType
     */
    public String getMimeType() {

        return switch (this) {
            case H264, H265, HEVC -> "video/mp4";
            case VP9, AV1 -> "video/webm";
            case AAC, ALAC -> "audio/mp4";
            case MP3 -> "audio/mpeg";
            case OPUS, VORBIS -> "audio/ogg";
            case FLAC -> "audio/flac";
            case AC3 -> "audio/ac3";
            case EAC3 -> "audio/eac3";
            case DTS -> "audio/vnd.dts";
            case TRUEHD -> "audio/mlp";
            case SUBRIP -> "application/x-subrip";
            case ASS, SSA -> "text/x-ssa";
            case PGS -> "application/pgs";
            case DVB_SUB -> "application/dvbsubs";
            case VIDEO_COPY, AUDIO_COPY, SUBTITLE_COPY -> throw new IllegalStateException(
                    "*_COPY codecs are special codec and thus does not support mime type");
            // This feel so wrong
            case PNG -> "image/png";
            case MJPEG -> "image/jpeg";
        };
    }

    /**
     * Check if the current {@link Codec} is a special copy codec.
     *
     * @return True if the current {@link Codec} is a copy codec, false otherwise.
     */
    public boolean isCopyCodec() {

        return switch (this) {
            case VIDEO_COPY, AUDIO_COPY, SUBTITLE_COPY -> true;
            default -> false;
        };
    }
}
