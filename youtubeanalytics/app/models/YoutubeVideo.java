package models;

/**
 * Model class representing a YouTube video.
 * This class encapsulates the details of a YouTube video.
 *
 * @author Mohnish Mirchandani
 */
public class YoutubeVideo {

    private final String videoId;
    private final String title;
    private final String description;
    private final String thumbnailUrl;
    private final String channelTitle;
    private final String publishedAt;
    private final Long viewCount;

    /**
     * Constructor for YoutubeVideo.
     *
     * @param videoId The ID of the video.
     * @param title The title of the video.
     * @param description The description of the video.
     * @param thumbnailUrl The URL of the video's thumbnail.
     * @param channelTitle The title of the channel that uploaded the video.
     * @param publishedAt The publication date of the video.
     * @param viewCount The view count of the video.
     * @author Mohnish Mirchandani
     */
    public YoutubeVideo(
        String videoId,
        String title,
        String description,
        String thumbnailUrl,
        String channelTitle,
        String publishedAt,
        Long viewCount
    ) {
        this.videoId = videoId;
        this.title = title;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.channelTitle = channelTitle;
        this.publishedAt = publishedAt;
        this.viewCount = viewCount;
    }

    /**
     * Gets the description of the video.
     *
     * @return The description of the video.
     * @author Mohnish Mirchandani
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the ID of the video.
     *
     * @return The ID of the video.
     * @author Mohnish Mirchandani
     */
    public String getVideoId() {
        return videoId;
    }

    /**
     * Gets the title of the video.
     *
     * @return The title of the video.
     * @author Mohnish Mirchandani
     */
    public String getTitle() {
        return title;
    }

    /**
     * Gets the URL of the video's thumbnail.
     *
     * @return The URL of the video's thumbnail.
     * @author Mohnish Mirchandani
     */
    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    /**
     * Gets the title of the channel that uploaded the video.
     *
     * @return The title of the channel that uploaded the video.
     * @author Mohnish Mirchandani
     */
    public String getChannelTitle() {
        return channelTitle;
    }

    /**
     * Gets the publication date of the video.
     *
     * @return The publication date of the video.
     * @author Mohnish Mirchandani
     */
    public String getPublishedAt() {
        return publishedAt;
    }

    /**
     * Gets the view count of the video.
     *
     * @return The view count of the video.
     * @author Mohnish Mirchandani
     */
    public Long getViewCount() {
        return viewCount;
    }
}