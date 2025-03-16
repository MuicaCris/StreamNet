namespace StreamNetServer.Models;

public class LiveStreamCreateRequest
{
    public string Title { get; set; }
    public String StreamerId { get; set; }
    public String Thumbnail { get; set; }
    public DateTime Timestamp { get; set; } = DateTime.UtcNow;
}