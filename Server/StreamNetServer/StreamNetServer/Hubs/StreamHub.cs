using System;
using System.Threading.Tasks;
using Microsoft.AspNetCore.SignalR;
using Microsoft.Extensions.Logging;

namespace StreamNetServer.Hubs
{
    public class StreamHub : Hub
    {
        private readonly ILogger<StreamHub> _logger;

        public StreamHub(ILogger<StreamHub> logger)
        {
            _logger = logger;
        }

        public override async Task OnConnectedAsync()
        {
            _logger.LogInformation($"Client conectat: {Context.ConnectionId}");
            await base.OnConnectedAsync();
        }

        public override async Task OnDisconnectedAsync(Exception exception)
        {
            _logger.LogInformation($"Client deconectat: {Context.ConnectionId}");
            await base.OnDisconnectedAsync(exception);
        }
    }
}