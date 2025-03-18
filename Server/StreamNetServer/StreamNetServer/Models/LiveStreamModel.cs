using System;

namespace StreamNetServer.Models;

public class LiveStreamModel
{
    public int StreamerId { get; set; }
    public string Title { get; set; }
    public string? Thumbnail { get; set; }
    public DateTime Timestamp { get; set; }
}