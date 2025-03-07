using System.Collections.Concurrent;
using System.Net;
using System.Net.WebSockets;
using System.Text;
using System.IdentityModel.Tokens.Jwt;
using StreamNetServer.Utils;

namespace StreamNetServer.Services;

public class WebSocketService
{
    private readonly ConcurrentDictionary<WebSocket, string> _clients = new(); // Clienți autentificați (WebSocket -> username)

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
                if (!context.Request.Headers.AllKeys.Contains("Authorization"))
                {
                    Logger.Log("Conexiune WebSocket refuzată: Lipsă token JWT");
                    context.Response.StatusCode = 401;
                    context.Response.Close();
                    continue;
                }

                string token = context.Request.Headers["Authorization"]?.Replace("Bearer ", "");
                string username = ValidateJWT(token);

                if (string.IsNullOrEmpty(username))
                {
                    Logger.Log("Conexiune WebSocket refuzată: Token invalid");
                    context.Response.StatusCode = 403;
                    context.Response.Close();
                    continue;
                }

                var wsContext = await context.AcceptWebSocketAsync(null);
                WebSocket socket = wsContext.WebSocket;
                _clients.TryAdd(socket, username);
                Logger.Log($"{username} s-a conectat.");

                _ = HandleWebSocket(socket, username);
            }
            else
            {
                Logger.Log("Conexiune HTTP refuzată (nu este WebSocket)");
                context.Response.StatusCode = 400;
                context.Response.Close();
            }
        }
    }

    private async Task HandleWebSocket(WebSocket socket, string username)
    {
        var buffer = new byte[1024 * 1024];

        try
        {
            while (socket.State == WebSocketState.Open)
            {
                WebSocketReceiveResult result = await socket.ReceiveAsync(new ArraySegment<byte>(buffer), CancellationToken.None);
                if (result.MessageType == WebSocketMessageType.Close)
                {
                    Logger.Log($"{username} s-a deconectat.");
                    _clients.TryRemove(socket, out _);
                    await socket.CloseAsync(WebSocketCloseStatus.NormalClosure, "Closed", CancellationToken.None);
                }
                else if (result.MessageType == WebSocketMessageType.Text)
                {
                    var message = Encoding.UTF8.GetString(buffer, 0, result.Count);
                    Logger.Log($"{username}: {message}");

                    // Transmite mesajul tuturor clienților
                    await BroadcastTextMessage($"{username}: {message}");
                }
                else if (result.MessageType == WebSocketMessageType.Binary)
                {
                    Logger.Log($"{result.Count} bytes primiți de la {username} (binary)");
                    await BroadcastBinaryData(buffer, result.Count);
                }
            }
        }
        catch (Exception ex)
        {
            Logger.Log($"Eroare WebSocket ({username}): {ex.Message}");
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

    private async Task BroadcastTextMessage(string message)
    {
        var data = Encoding.UTF8.GetBytes(message);
        foreach (var client in _clients.Keys)
        {
            if (client.State == WebSocketState.Open)
            {
                try
                {
                    await client.SendAsync(new ArraySegment<byte>(data), WebSocketMessageType.Text, true, CancellationToken.None);
                }
                catch (Exception ex)
                {
                    Logger.Log($"Eroare trimitere mesaj: {ex.Message}");
                }
            }
        }
    }

    private async Task BroadcastBinaryData(byte[] data, int count)
    {
        foreach (var client in _clients.Keys)
        {
            if (client.State == WebSocketState.Open)
            {
                try
                {
                    await client.SendAsync(new ArraySegment<byte>(data, 0, count), WebSocketMessageType.Binary, true, CancellationToken.None);
                    Logger.Log($"Transmis {count} bytes către clienți.");
                }
                catch (Exception ex)
                {
                    Logger.Log($"Eroare trimitere date binare: {ex.Message}");
                }
            }
        }
    }

    private string ValidateJWT(string token)
    {
        try
        {
            var handler = new JwtSecurityTokenHandler();
            var jwtToken = handler.ReadJwtToken(token);
            return jwtToken.Claims.FirstOrDefault(c => c.Type == "username")?.Value;
        }
        catch
        {
            return null;
        }
    }
}
