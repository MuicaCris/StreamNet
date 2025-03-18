using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Data.SqlClient;
using StreamNetServer.Models;

namespace StreamNetServer.Controllers
{
    [ApiController]
    [Route("api/stream")]
    public class StreamController : ControllerBase
    {
        private readonly string _connectionString = "Server=localhost;Database=StreamNetServer;Trusted_Connection=True;TrustServerCertificate=True;";

        [HttpGet("activeStreams")]
        public async Task<ActionResult<List<LiveStream>>> GetActiveStreams()
        {
            try
            {
                var streams = new List<LiveStream>();

                await using var conn = new SqlConnection(_connectionString);
                await conn.OpenAsync();

                var query = "SELECT Id, Title, StreamerId, Thumbnail, Timestamp, IsActive FROM LiveStreams WHERE IsActive = 1";
                
                await using var cmd = new SqlCommand(query, conn);
                await using var reader = await cmd.ExecuteReaderAsync();

                while (await reader.ReadAsync())
                {
                    streams.Add(new LiveStream
                    {
                        Id = reader.GetInt32(0),
                        Title = reader.GetString(1),
                        StreamerId = reader.GetInt32(2),
                        Thumbnail = reader.IsDBNull(3) ? null : reader.GetString(3),
                        Timestamp = reader.GetDateTime(4),
                        IsActive = reader.GetBoolean(5)
                    });
                }

                return Ok(streams);
            }
            catch (Exception ex)
            {
                return StatusCode(500, $"Eroare server: {ex.Message}");
            }
        }
    }
}