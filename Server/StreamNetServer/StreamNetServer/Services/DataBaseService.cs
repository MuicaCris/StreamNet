using System.Data.SqlClient;
using System.Data.SqlTypes;
using Microsoft.AspNetCore.Mvc;

namespace StreamNetServer.Services;

[ApiController]
[Route("api/user")]
public class DataBaseService : ControllerBase
{
    private readonly string connectionString = "your_connection_string_here";
    
    public IActionResult SearchUsers(string query)
    {
        List<string> users = new List<string>();
        using (SqlConnection conn = new SqlConnection(connectionString))
        {
            conn.Open();
            var sql = "SELECT Username FROM Users WHERE Username LIKE @query";
            using (SqlCommand cmd = new SqlCommand(sql, conn))
            {
                cmd.Parameters.AddWithValue("@query", "%" + query + "%");
                using (SqlDataReader reader = cmd.ExecuteReader())
                {
                    while (reader.Read())
                    {
                        users.Add(reader["Username"].ToString());
                    }
                }
            }
        }
        return Ok(users);
    }
}