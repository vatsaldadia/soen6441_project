public class YouTubeVideo {
    private String videoId;
    private String title;
    private String description;
    private String thumbnailUrl;
    private String channelTitle;
    private String publishedAt;
    private Long viewCount;

    // Constructor
    public YouTubeVideo(String videoId, String title, String description,
                       String thumbnailUrl, String channelTitle,
                       String publishedAt, Long viewCount) {
        this.videoId = videoId;
        this.title = title;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.channelTitle = channelTitle;
        this.publishedAt = publishedAt;
        this.viewCount = viewCount;
    }
}
