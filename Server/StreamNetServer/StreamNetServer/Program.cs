using StreamNetServer.Services;

class Program
{
    public static async Task Main(string[] args)
    {
        WebSocketService webSocketService = new WebSocketService();
        webSocketService.Start("http://localhost:5050/ws/");

        Console.WriteLine("Server ruleaza. Apasa ENTER pentru a inchide.");
        Console.ReadLine();
    }
}