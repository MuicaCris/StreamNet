using Microsoft.AspNetCore.Mvc;

namespace StreamNetServer.Controllers;

    [Microsoft.AspNetCore.Components.Route("api/stream")]
    [ApiController]

    public class StreamController : ControllerBase
    {
        [HttpGet("status")]
        public IActionResult GetStatus()
        {
            return Ok(new { status = "Server is running" });
        }
    }
