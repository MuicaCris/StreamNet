using System;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.Data.SqlClient;
using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Hosting;
using Microsoft.Extensions.Hosting;
using StreamNetServer.Services;

class Program
{
    public static async Task Main(string[] args)
    {
        var connectionString = "Server=localhost;Database=StreamNetServer;Trusted_Connection=True;TrustServerCertificate=True;";

        using var cts = new CancellationTokenSource();

        WebSocketService webSocketService = new WebSocketService();
        webSocketService.Start("http://localhost:5050/ws/");

        var host = CreateHost();
        var hostTask = host.RunAsync(cts.Token);

        Console.WriteLine("Server rulează. Apasă ENTER pentru a închide.");
        
        await TestDatabaseConnection(connectionString);

        Console.ReadLine();

        cts.Cancel();
        await hostTask;
    }

    private static async Task TestDatabaseConnection(string connectionString)
    {
        try
        {
            await using SqlConnection conn = new(connectionString);
            await conn.OpenAsync();
            Console.WriteLine("Conectat la SQL Server!");
        }
        catch (Exception ex)
        {
            Console.WriteLine($"Eroare la conectare: {ex.Message}");
        }
    }

    private static IHost CreateHost()
    {
        return Host.CreateDefaultBuilder()
            .ConfigureWebHostDefaults(webBuilder =>
            {
                webBuilder.Configure(app => { app.UseRouting(); });
            })
            .Build();
    }
}