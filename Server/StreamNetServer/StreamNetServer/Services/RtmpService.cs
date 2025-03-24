using System.Diagnostics;
using StreamNetServer.Models;
using System.Collections.Concurrent;

namespace StreamNetServer.Services
{
    public class RtmpService : IRtmpService
    {
        private readonly ILogger<RtmpService> _logger;
        private readonly ConcurrentDictionary<string, LiveStream> _activeStreams;
        private readonly string _rtmpServerPath;
        private Process _rtmpServerProcess;
        
        private int _nextStreamId = 1;

        public RtmpService(ILogger<RtmpService> logger)
        {
            _logger = logger;
            _activeStreams = new ConcurrentDictionary<string, LiveStream>();
            
            _rtmpServerPath = "C:\\nginx\\nginx-rtmp\\nginx.exe";
        }

        public async Task<List<LiveStream>> GetActiveStreamsAsync()
        {
            return _activeStreams.Values.ToList();
        }

        public async Task<LiveStream> GetStreamByKeyAsync(string streamKey)
        {
            if (_activeStreams.TryGetValue(streamKey, out var stream))
            {
                return stream;
            }
            
            return null;
        }

        public async Task<bool> StartStreamAsync(string streamKey, string title, int streamerId)
        {
            try
            {
                var stream = new LiveStream
                {
                    Id = _nextStreamId++,
                    Title = title,
                    StreamerId = streamerId,
                    StreamKey = streamKey,
                    StartTime = DateTime.UtcNow,
                    IsActive = true
                };
                
                return _activeStreams.TryAdd(streamKey, stream);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, $"Eroare la pornirea stream-ului: {ex.Message}");
                return false;
            }
        }

        public async Task<bool> StopStreamAsync(string streamKey)
        {
            return _activeStreams.TryRemove(streamKey, out _);
        }

        public async Task<bool> IsStreamActiveAsync(string streamKey)
        {
            return _activeStreams.ContainsKey(streamKey);
        }
        
        public async Task StartRtmpServerAsync()
        {
            try
            {
                if (File.Exists(_rtmpServerPath))
                {
                    _rtmpServerProcess = new Process
                    {
                        StartInfo = new ProcessStartInfo
                        {
                            FileName = _rtmpServerPath,
                            Arguments = "-c rtmp_server_config.conf",
                            UseShellExecute = false,
                            RedirectStandardOutput = true,
                            CreateNoWindow = true
                        }
                    };
                    
                    _rtmpServerProcess.Start();
                    _logger.LogInformation("Server RTMP pornit cu succes");
                }
                else
                {
                    _logger.LogError("Fișierul executabil al serverului RTMP nu a fost găsit");
                }
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, $"Eroare la pornirea serverului RTMP: {ex.Message}");
            }
        }
        
        public async Task StopRtmpServerAsync()
        {
            try
            {
                if (_rtmpServerProcess != null && !_rtmpServerProcess.HasExited)
                {
                    _rtmpServerProcess.Kill();
                    _rtmpServerProcess.Dispose();
                    _logger.LogInformation("Server RTMP oprit cu succes");
                }
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, $"Eroare la oprirea serverului RTMP: {ex.Message}");
            }
        }
    }
}