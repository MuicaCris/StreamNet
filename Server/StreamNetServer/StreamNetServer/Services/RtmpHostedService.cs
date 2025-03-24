namespace StreamNetServer.Services
{
    public class RtmpHostedService : IHostedService
    {
        private readonly ILogger<RtmpHostedService> _logger;
        private readonly RtmpService _rtmpService;

        public RtmpHostedService(ILogger<RtmpHostedService> logger, IRtmpService rtmpService)
        {
            _logger = logger;
            _rtmpService = rtmpService as RtmpService;
        }

        public async Task StartAsync(CancellationToken cancellationToken)
        {
            _logger.LogInformation("Serviciu RTMP începe...");
            await _rtmpService.StartRtmpServerAsync();
        }

        public async Task StopAsync(CancellationToken cancellationToken)
        {
            _logger.LogInformation("Serviciu RTMP se oprește...");
            await _rtmpService.StopRtmpServerAsync();
        }
    }
}