using Microsoft.AspNetCore.Mvc;
using Microsoft.Data.SqlClient;
using StreamNetServer.Models;

namespace StreamNetServer.Controllers;

[ApiController]
[Route("api/stream")]
public class StreamController : ControllerBase
{
    [HttpGet("rtmpUrl/{streamId}")]
    private ActionResult<string> GetRtmpUrl(int streamId)
    {
        var streamUrl = $"rtmp://localhost:{streamId}";
        return Ok(streamUrl);
    }
    
    [HttpPost("createStream")]
    public async Task<ActionResult<int>> CreateStream([FromBody] LiveStreamCreateRequest request)
    {
        if (string.IsNullOrEmpty(request.Title))
        {
            return BadRequest("Title is required.");
        }

        using (SqlConnection conn = new SqlConnection("Server=localhost;Database=StreamNetServer;Trusted_Connection=True;TrustServerCertificate=True;"))
        {
            await conn.OpenAsync();
            var query = "INSERT INTO LiveStreams (Title, StreamerId, Thumbnail, Timestamp, IsActive) " +
                        "OUTPUT INSERTED.Id " +
                        "VALUES (@Title, @StreamerId, @Thumbnail, @Timestamp, 1)";
    
            using (SqlCommand cmd = new SqlCommand(query, conn))
            {
                cmd.Parameters.AddWithValue("@Title", request.Title);
                cmd.Parameters.AddWithValue("@StreamerId", request.StreamerId);
                cmd.Parameters.AddWithValue("@Thumbnail", (object)request.Thumbnail ?? DBNull.Value);
                cmd.Parameters.AddWithValue("@Timestamp", request.Timestamp);

                int streamId = (int)await cmd.ExecuteScalarAsync();

                var rtmpUrl = $"rtmp://localhost:{streamId}";
                return Ok(new { StreamId = streamId, RtmpUrl = rtmpUrl });
            }
        }
    }
    
    [HttpPost("stopStream/{streamId}")]
    public async Task<ActionResult> StopStream(int streamId)
    {
        using (SqlConnection conn = new SqlConnection("Server=localhost;Database=StreamNetServer;Trusted_Connection=True;TrustServerCertificate=True;"))
        {
            await conn.OpenAsync();
            var query = "UPDATE LiveStreams SET IsActive = 0 WHERE Id = @StreamId";
    
            using (SqlCommand cmd = new SqlCommand(query, conn))
            {
                cmd.Parameters.AddWithValue("@StreamId", streamId);
                await cmd.ExecuteNonQueryAsync();
            }
        }
        return Ok();
    }    
}