package fr.anisekai.media;

import fr.anisekai.media.enums.Codec;
import fr.anisekai.media.enums.Disposition;
import org.json.JSONObject;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a single media stream extracted from a container file.
 */
public class MediaStream {

    private final int                  id;
    private final Codec                codec;
    private final EnumSet<Disposition> dispositions;
    private final Map<String, String>  metadata;

    /**
     * Create a new instance of {@link MediaStream}.
     *
     * @param codec
     *         The {@link Codec} for this {@link MediaStream}.
     * @param json
     *         The {@link JSONObject} containing a {@link MediaStream} data.
     */
    public MediaStream(Codec codec, JSONObject json) {

        this.id           = json.getInt("index");
        this.codec        = codec;
        this.dispositions = EnumSet.noneOf(Disposition.class);
        this.metadata     = new HashMap<>();

        JSONObject dispositionJson = json.getJSONObject("disposition");

        for (String key : dispositionJson.keySet()) {
            if (dispositionJson.getInt(key) == 1) {
                Disposition disposition = Disposition.from(key);
                if (disposition != null) {
                    this.dispositions.add(disposition);
                }
            }
        }

        JSONObject tagsJson = json.getJSONObject("tags");

        for (String key : tagsJson.keySet()) {
            this.metadata.put(key.toLowerCase(), tagsJson.getString(key));
        }

    }

    /**
     * Retrieve this {@link MediaStream}'s ID
     *
     * @return An id
     */
    public int getId() {

        return this.id;
    }

    /**
     * Retrieve this {@link MediaStream}'s ID
     *
     * @return A {@link Codec}
     */
    public Codec getCodec() {

        return this.codec;
    }

    /**
     * Retrieve this {@link MediaStream}'s {@link EnumSet} of {@link Disposition}s.
     *
     * @return An {@link EnumSet} of {@link Disposition}s.
     */
    public EnumSet<Disposition> getDispositions() {

        return this.dispositions;
    }

    /**
     * Retrieve this {@link MediaStream} metadata.
     *
     * @return A {@link Map}.
     */
    public Map<String, String> getMetadata() {

        return this.metadata;
    }

}
