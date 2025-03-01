using System.Collections.Concurrent;
using System.Net;
using System.Net.WebSockets;
using System.Text;
using StreamNetServer.Utils;

namespace StreamNetServer.Services;

public class WebSocketService
{
    private readonly ConcurrentDictionary<WebSocket, bool> _clients = new();

    public async Task Start(string url)
    {
        HttpListener server = new HttpListener();
        server.Prefixes.Add(url);
        server.Start();
        Logger.Log($"Server WebSocket pornit la {url}");

        while (true)
        {
            var context = await server.GetContextAsync();
            if (context.Request.IsWebSocketRequest)
            {
                var wsContext = await context.AcceptWebSocketAsync(null);
                WebSocket socket = wsContext.WebSocket;
                _clients.TryAdd(socket, true);
                Logger.Log("🔵 Client conectat.");

                _ = HandleWebSocket(socket);
            }
            else
            {
                Logger.Log("Conexiune HTTP refuzată (nu este WebSocket)");
                context.Response.StatusCode = 400;
                context.Response.Close();
            }
        }
    }

    private async Task HandleWebSocket(WebSocket socket)
    {
        var buffer = new byte[1024 * 1024];

        try
        {
            while (socket.State == WebSocketState.Open)
            {
                WebSocketReceiveResult result = await socket.ReceiveAsync(new ArraySegment<byte>(buffer), CancellationToken.None);
                if (result.MessageType == WebSocketMessageType.Close)
                {
                    Logger.Log("Client deconectat.");
                    _clients.TryRemove(socket, out _);
                    await socket.CloseAsync(WebSocketCloseStatus.NormalClosure, "Closed", CancellationToken.None);
                }
                else if (result.MessageType == WebSocketMessageType.Text)
                {
                    var message = Encoding.UTF8.GetString(buffer, 0, result.Count);
                    Logger.Log($"Mesaj primit: {message}");

                    // Trimite raspuns catre client
                    var response = "Răspuns de la server: " + message;
                    var responseBytes = Encoding.UTF8.GetBytes(response);
                    await socket.SendAsync(new ArraySegment<byte>(responseBytes), WebSocketMessageType.Text, true, CancellationToken.None);
                }
                else if (result.MessageType == WebSocketMessageType.Binary)
                {
                    Logger.Log($"📦 {result.Count} bytes primiți (binary)");
                    await BroadcastData(buffer, result.Count);
                }
            }
        }
        catch (Exception ex)
        {
            Logger.Log($"Eroare conexiune WebSocket: {ex.Message}");
        }
        finally
        {
            _clients.TryRemove(socket, out _);
            if (socket.State == WebSocketState.Open)
            {
                await socket.CloseAsync(WebSocketCloseStatus.NormalClosure, "Closed", CancellationToken.None);
            }
        }
    }

    private async Task BroadcastData(byte[] data, int count)
    {
        foreach (var client in _clients.Keys)
        {
            if (client.State == WebSocketState.Open)
            {
                try
                {
                    await client.SendAsync(new ArraySegment<byte>(data, 0, count), WebSocketMessageType.Binary, true, CancellationToken.None);
                    Logger.Log($"Mesaj transmis către client ({count} bytes).");
                }
                catch (Exception ex)
                {
                    Logger.Log($"Eroare la trimiterea datelor: {ex.Message}");
                }
            }
        }
    }
}
