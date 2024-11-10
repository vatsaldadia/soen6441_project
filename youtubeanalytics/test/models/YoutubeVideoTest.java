package models;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Test class for YoutubeVideo.
 * This class contains unit tests for the YoutubeVideo methods.
 *
 * @author Vatsal Dadia
 */
public class YoutubeVideoTest {

    /**
     * Tests the YoutubeVideo constructor and its getter methods.
     * Verifies that the constructor correctly initializes the fields and the getter methods return the expected values.
     *
     * @author Vatsal Dadia
     */
    @Test
    public void testYoutubeVideo() {
        String videoId = "testVideoId";
        String title = "Test Title";
        String description = "Test Description";
        String thumbnailUrl = "http://example.com/thumbnail.jpg";
        String channelTitle = "Test Channel";
        String publishedAt = "2023-10-01T00:00:00Z";
        Long viewCount = 1000L;

        YoutubeVideo video = new YoutubeVideo(videoId, title, description, thumbnailUrl, channelTitle, publishedAt, viewCount);

        assertEquals(videoId, video.getVideoId());
        assertEquals(title, video.getTitle());
        assertEquals(description, video.getDescription());
        assertEquals(thumbnailUrl, video.getThumbnailUrl());
        assertEquals(channelTitle, video.getChannelTitle());
        assertEquals(publishedAt, video.getPublishedAt());
        assertEquals(viewCount, video.getViewCount());
    }
}