using System;
using Microsoft.AspNetCore.Mvc;
using StreamNetServer.Models;
using System.Data.SqlClient;
using System.Threading.Tasks;

namespace StreamNetServer.Controllers
{
    [ApiController]
    [Route("api/auth/")]
    public class AuthController : ControllerBase
    {
        [HttpPost("register")]
        public async Task<IActionResult> Register([FromBody] UserRegisterModel model)
        {
            if (model.Password != model.ConfirmPassword)
                return BadRequest("Passwords do not match.");

            var hashedPassword = BCrypt.Net.BCrypt.HashPassword(model.Password);

            try
            {
                using (SqlConnection conn = new SqlConnection("Server=localhost;Database=StreamNetServer;Trusted_Connection=True;TrustServerCertificate=True;"))
                {
                    await conn.OpenAsync();
                    Console.WriteLine("Database connection opened successfully.");

                    var query = "INSERT INTO Users (Username, Email, PasswordHash) VALUES (@Username, @Email, @PasswordHash)";

                    using (SqlCommand cmd = new SqlCommand(query, conn))
                    {
                        cmd.Parameters.AddWithValue("@Username", model.Username);
                        cmd.Parameters.AddWithValue("@Email", model.Email);
                        cmd.Parameters.AddWithValue("@PasswordHash", hashedPassword);

                        int rowsAffected = await cmd.ExecuteNonQueryAsync();
                        Console.WriteLine($"Rows affected: {rowsAffected}");
                    }
                }

                //return Created("api/auth/register" ,"User registered successfully.");
                return Ok(new { message = "User registered successfully" });
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Error: {ex.Message}");
                return StatusCode(500, "Internal server error.");
            }
        }
    }
}