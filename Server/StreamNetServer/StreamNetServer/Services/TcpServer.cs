using System.Net;
using System.Net.Sockets;
using System.Text;

namespace StreamNetServer.Services;

public class TcpServer
{
    public void Start(int port)
    {
        TcpListener server = new TcpListener(IPAddress.Any, port);
        server.Start();
        Console.WriteLine($"Server is running on port {port}");

        Task.Run(async () =>
        {
            while (true)
            {
                TcpClient client = await server.AcceptTcpClientAsync();
                Task.Run(() => HandleClient(client));
            }
        });
    }
    
    private async Task HandleClient(TcpClient client)
    {
        NetworkStream stream = client.GetStream();
        byte[] buffer = new byte[1024];
        int bytesRead;

        while ((bytesRead = await stream.ReadAsync(buffer, 0, buffer.Length)) != 0)
        {
            var receivedData = Encoding.UTF8.GetString(buffer, 0, bytesRead);
            Console.WriteLine($"Received TCP: {receivedData}");
        }
        
        client.Close();
    }
}