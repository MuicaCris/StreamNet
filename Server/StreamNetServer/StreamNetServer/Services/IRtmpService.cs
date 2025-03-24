using StreamNetServer.Models;

namespace StreamNetServer.Services
{
    public interface IRtmpService
    {
        Task<List<LiveStream>> GetActiveStreamsAsync();
        Task<LiveStream> GetStreamByKeyAsync(string streamKey);
        Task<bool> StartStreamAsync(string streamKey, string title, int streamerId);
        Task<bool> StopStreamAsync(string streamKey);
        Task<bool> IsStreamActiveAsync(string streamKey);
    }
}
