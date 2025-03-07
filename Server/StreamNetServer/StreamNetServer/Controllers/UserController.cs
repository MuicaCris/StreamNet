using System.Data.SqlClient;
using Microsoft.AspNetCore.Mvc;

namespace StreamNetServer.Controllers;

[ApiController]
[Route("api/user")]
public class UserController : ControllerBase
{
    private readonly string connectionString = "your_connection_string_here";

    [HttpPost("login")]
    public IActionResult Login([FromBody] LoginRequest request)
    {
        using (SqlConnection conn = new SqlConnection(connectionString))
        {
            conn.Open();
            var query = "SELECT role FROM users WHERE username = @username AND password = @password";

            using (SqlCommand cmd = new SqlCommand(query, conn))
            {
                cmd.Parameters.AddWithValue("@username", request.Username);
                cmd.Parameters.AddWithValue("@password", request.Password);

                var reader = cmd.ExecuteReader();
                if (reader.Read())
                {
                    string role = reader.GetString(0);
                    return Ok(new { token = GenerateJWT(request.Username, role) });
                }
                else
                {
                    return Unauthorized("Username or password is incorrect");
                }
            }
        }
    }

    private string GenerateJWT(string username, string role)
    {
        return "dummy_token";
    }
}

public class LoginRequest
{
    public string Username { get; set; }
    public string Password { get; set; }
}