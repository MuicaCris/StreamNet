namespace StreamNetServer.Models;

public class LiveStreamCreateRequest
{
    public string Title { get; set; }
    public int StreamerId { get; set; }
    public string? Thumbnail { get; set; }
    public DateTime Timestamp { get; set; }
}