namespace StreamNetServer.Models;

public class LiveStream
{
    public int Id { get; set; }
    public string Title { get; set; }
    public int StreamerId { get; set; }
    public string Thumbnail { get; set; }
    public DateTime Timestamp { get; set; } = DateTime.UtcNow;
    public bool IsActive { get; set; }
    
}