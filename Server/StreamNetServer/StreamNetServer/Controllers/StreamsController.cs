using Microsoft.AspNetCore.Mvc;
using StreamNetServer.Models;
using StreamNetServer.Services;
using Microsoft.AspNetCore.SignalR;
using StreamNetServer.Hubs;

namespace StreamNetServer.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class StreamsController : ControllerBase
    {
        private readonly ILogger<StreamsController> _logger;
        private readonly IRtmpService _rtmpService;
        private readonly IHubContext<StreamHub> _hubContext;

        public StreamsController(
            ILogger<StreamsController> logger,
            IRtmpService rtmpService,
            IHubContext<StreamHub> hubContext)
        {
            _logger = logger;
            _rtmpService = rtmpService;
            _hubContext = hubContext;
        }

        [HttpGet]
        public async Task<ActionResult<IEnumerable<LiveStream>>> GetActiveStreams()
        {
            try
            {
                var streams = await _rtmpService.GetActiveStreamsAsync();
                return Ok(streams);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Eroare la obtinerea stream-urilor active");
                return StatusCode(500, "Eroare interna de server");
            }
        }

        [HttpGet("{streamKey}")]
        public async Task<ActionResult<LiveStream>> GetStream(string streamKey)
        {
            try
            {
                var stream = await _rtmpService.GetStreamByKeyAsync(streamKey);
                if (stream == null)
                {
                    return NotFound();
                }
                
                return Ok(stream);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, $"Eroare la obtinerea stream-ului cu cheia {streamKey}");
                return StatusCode(500, "Eroare internă de server");
            }
        }

        [HttpPost("start")]
        public async Task<ActionResult> StartStream([FromBody] StartStreamRequest request)
        {
            try
            {
                if (string.IsNullOrEmpty(request.StreamKey))
                {
                    return BadRequest("Stream key este obligatoriu");
                }
                
                var result = await _rtmpService.StartStreamAsync(
                    request.StreamKey,
                    request.Title ?? $"Stream {request.StreamKey}",
                    request.StreamerId);
                
                if (result)
                {
                    await _hubContext.Clients.All.SendAsync("StreamStarted", request.StreamKey);
                    return Ok(new { success = true });
                }
                
                return BadRequest("Nu s-a putut porni stream-ul");
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Eroare la pornirea stream-ului");
                return StatusCode(500, "Eroare internă de server");
            }
        }

        [HttpPost("stop")]
        public async Task<ActionResult> StopStream([FromBody] StopStreamRequest request)
        {
            try
            {
                if (string.IsNullOrEmpty(request.StreamKey))
                {
                    return BadRequest("Stream key este obligatoriu");
                }
                
                var result = await _rtmpService.StopStreamAsync(request.StreamKey);
                
                if (result)
                {
                    await _hubContext.Clients.All.SendAsync("StreamStopped", request.StreamKey);
                    return Ok(new { success = true });
                }
                
                return BadRequest("Nu s-a putut opri stream-ul");
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Eroare la oprirea stream-ului");
                return StatusCode(500, "Eroare internă de server");
            }
        }

        [HttpGet("active/{streamKey}")]
        public async Task<ActionResult<bool>> IsStreamActive(string streamKey)
        {
            try
            {
                var isActive = await _rtmpService.IsStreamActiveAsync(streamKey);
                return Ok(isActive);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, $"Eroare la verificarea stării stream-ului {streamKey}");
                return StatusCode(500, "Eroare internă de server");
            }
        }
    }

    public class StartStreamRequest
    {
        public string StreamKey { get; set; }
        public string Title { get; set; }
        public int StreamerId { get; set; }
    }

    public class StopStreamRequest
    {
        public string StreamKey { get; set; }
    }
}