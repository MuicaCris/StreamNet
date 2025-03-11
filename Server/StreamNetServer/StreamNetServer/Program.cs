using Microsoft.AspNetCore.Mvc;
using StreamNetServer.Services;
using Microsoft.Data.SqlClient;
using StreamNetServer.Auth;
using StreamNetServer.Controllers;
using StreamNetServer.Models;

class Program
{
    public static async Task Main(string[] args)
    {
        var connectionString = "Server=localhost;Database=StreamNetServer;Trusted_Connection=True;TrustServerCertificate=True;";
        
        WebSocketService webSocketService = new WebSocketService();
        webSocketService.Start("http://localhost:5050/ws/");

        Console.WriteLine("Server ruleaza. Apasa ENTER pentru a inchide.");
        
        using (SqlConnection conn = new SqlConnection(connectionString))
        {
            try
            {
                conn.Open();
                Console.WriteLine("Conectat la SQL Server!.");
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Error: {ex.Message}");
            }
        }
        Console.ReadLine();
    }
}