using System;

/// <summary>
/// Summary description for Class1
/// </summary>
public class Class1
{
	public Class1()
	{
		//
		// TODO: Add constructor logic here
		//
	}

  static async Task Main()
  {
    using (var httpClient = new HttpClient())
    {
      double totalUsage;
      double allowableUsage;
      ServicePointManager.SecurityProtocol = SecurityProtocolType.Tls12; // this line can be removed in .NET Core
      using (var request = new HttpRequestMessage(new HttpMethod("POST"), "https://login.xfinity.com/login"))
      {
        var data = new Dictionary<string, string> {
                {"user", Username}
                , {"passwd", Password}
                , {"s", "oauth"}
                , {"continue", "https://oauth.xfinity.com/oauth/authorize?client_id=my-account-web&prompt=login&redirect_uri=https%3A%2F%2Fcustomer.xfinity.com%2Foauth%2Fcallback&response_type=code"}
            };
        var content = string.Join("&", data.Select(x => $"{x.Key}={WebUtility.UrlEncode(x.Value)}"));
        request.Content = new StringContent(content, Encoding.UTF8, "application/x-www-form-urlencoded");
        await httpClient.SendAsync(request);
      }
      using (var request = new HttpRequestMessage(new HttpMethod("GET"), "https://customer.xfinity.com/apis/services/internet/usage"))
      {
        var response = await httpClient.SendAsync(request);
        var responseStream = await response.Content.ReadAsStreamAsync();
        var streamReader = new StreamReader(responseStream);
        var responseContent = streamReader.ReadToEnd();
        var parsedResponse = JObject.Parse(responseContent);
        var usageMonths = parsedResponse["usageMonths"];
        var currentMonthUsage = usageMonths.Last;
        totalUsage = currentMonthUsage.Value<double?>("totalUsage") ?? 0;
        allowableUsage = currentMonthUsage.Value<double?>("allowableUsage") ?? 0;
      }
      Console.WriteLine($"Allowable: {allowableUsage}");
      Console.WriteLine($"Total    : {totalUsage}");
      Console.ReadKey();
    }
  }
}

/* python example 
 * 
import mechanize, time, datetime
import Adafruit_CharLCD as LCD
from datetime import date
lcd = LCD.Adafruit_CharLCDPlate()
lcd.set_color(1.0, 1.0, 0.0)
lcd.clear()

br=mechanize.Browser()
br.set_handle_robots(False)
login_url="https://login.xfinity.com/login?r=comcast.net&s=oauth&continue=https%3A%2F%2Foauth.xfinity.com%2Foauth%2Fauthorize%3Fclient_id%3Dmy-account-web%26prompt%3Dlogin%26redirect_uri%3Dhttps%253A%252F%252Fcustomer.xfinity.com%252Foauth%252Fcallback%26response_type%3Dcode%26state%3D%2523%252F%26response%3D1&forceAuthn=1&client_id=my-account-web&reqId="

response = br.open('https://auth.xfinity.com/oauth/login?state=https%3A%2F%2Fcustomer.xfinity.com%2F%23%2F%3FCMP%3DILC_signin_myxfinity_re')
#print response.code

br.select_form("signin")
reqIdControl = br.form.find_control("reqId")
reqId = reqIdControl.value
login_url = login_url+reqId

response = br.open(login_url)
#print response.code

br.select_form(name="signin")
br[ "user" ]= "user@comcast.net"
br[ "passwd" ]= "your_password"
response = br.submit()
#print response.code

br.retrieve('https://customer.xfinity.com/apis/services/internet/usage', '/home/pi/usage.json')

usage_file = open("/home/pi/usage.json", "r")
string = "usage"
usage_file_string = usage_file.read()
start = usage_file_string.rfind(string)+7
end = start + 3
usage = usage_file_string[start:end:]

lcd.message(usage+" GB used"+'\n'+str(datetime.datetime.now().strftime("%H:%M")))

usage_file.close()
exit()


  */


/* 
 * and another 
 * https://github.com/lachesis/comcast/blob/master/comcast.py
 * 
 * and another
 * 
 * https://github.com/jantman/xfinity-usage
 * 
 * and another
 * 
 * https://github.com/robert-alfaro/xfinity-usage/blob/master/custom_components/xfinity/sensor.py

*/
