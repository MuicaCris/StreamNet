using StreamNetServer.Services;
using System.Data.SqlClient;

class Program
{
    public static async Task Main(string[] args)
    {
        var connectionString = "Server=localhost;Database=StreamNetDb;Integrated Security=true";
        using (SqlConnection conn = new SqlConnection(connectionString))
        {
            conn.Open();
            var sql = "CREATE TABLE TestTable (id INT PRIMARY KEY, name VARCHAR(100))";
            SqlCommand command = new SqlCommand(sql, conn);
            command.ExecuteNonQuery();
            Console.WriteLine("Inserted TestTable");
        }
        
        WebSocketService webSocketService = new WebSocketService();
        webSocketService.Start("http://localhost:5050/ws/");

        Console.WriteLine("Server ruleaza. Apasa ENTER pentru a inchide.");
        Console.ReadLine();
    }
}