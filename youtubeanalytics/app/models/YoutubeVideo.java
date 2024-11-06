package models;

public class YoutubeVideo {

    private final String videoId;
    private final String title;
    private final String description;
    private final String thumbnailUrl;
    private final String channelTitle;
    private final String publishedAt;
    private final Long viewCount;

    // Constructor
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

    public String getDescription() {
        return description;
    }
}
