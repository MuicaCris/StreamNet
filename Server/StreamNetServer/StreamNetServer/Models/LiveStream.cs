namespace StreamNetServer.Models;

public class LiveStream
{
    public int Id { get; set; }
    public string Title { get; set; }
    public int StreamerId { get; set; }
    public string StreamKey { get; set; }
    public string Thumbnail { get; set; }
    public DateTime StartTime { get; set; }
    public bool IsActive { get; set; }
}