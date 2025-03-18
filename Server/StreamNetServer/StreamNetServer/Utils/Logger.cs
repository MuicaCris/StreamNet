using System;
using System.IO;

namespace StreamNetServer.Utils;

public static class Logger
{
    private static readonly string logFile = "server.log";

    public static void Log(string message)
    {
        var logEntry = $"{DateTime.Now}: {message}";
        Console.WriteLine(logEntry);
        File.AppendAllText(logFile, logEntry + Environment.NewLine);
    }
}