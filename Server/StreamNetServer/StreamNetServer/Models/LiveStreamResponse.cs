namespace StreamNetServer.Models;

public class LiveStreamResponse
{
    public int Id { get; set; }
    public string Title { get; set; }
    public string StreamerName { get; set; }
    public string? Thumbnail { get; set; }
    public DateTime Timestamp { get; set; }
}