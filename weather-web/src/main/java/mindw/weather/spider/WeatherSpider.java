package mindw.weather.spider;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import mindw.weather.data.WeatherData;
import mindw.weather.data.WeatherItem;

/**
 * 爬取天气数据 http://www.tianqi.com/jiaxing/30/
 * 
 * @author mindw
 */
public class WeatherSpider {

	private static final Logger LOGGER = Logger.getLogger(WeatherSpider.class);

	/** 名字到拼音的映射 */
	private static Map<String, String> cityName2Pingyin;
	
	/** 拼音到名字的映射 */
	private static Map<String, String> pingyin2CityName;

	/** 缓存 */
	private static Map<String,WeatherData> cache;
	
	public static void active() {
		cityName2Pingyin = CitySpider.getCityNameAndPingyin();
		pingyin2CityName = new HashMap<>();
		cache = new HashMap<>();
		for(Map.Entry<String, String> e : cityName2Pingyin.entrySet()) {
			String cityName = e.getKey();
			String pingyin = e.getValue();
			cache.put(pingyin, null);
			pingyin2CityName.put(pingyin, cityName);
		}
	}
		
	/**
	 * 获得一个城市的天气
	 * 
	 * @param queryName
	 * @return
	 */
	public static WeatherData getWeatherItems(String queryName) {
		if(queryName==null||queryName.length()==0) {
			return WeatherData.ERROR_RESULT;
		}
		
		try {
			char ch = queryName.charAt(0);
			String city = null;
			if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) {
				// 英文直接查
				city = queryName.toLowerCase();
			} else {
				// 中文
				if (queryName.endsWith("市")) {
					queryName = queryName.substring(0, queryName.length() - 1);
				}
				// 中文转英文
				city = cityName2Pingyin.get(queryName);
			}
			
			// 判断city是否存在
			if(city==null||(!cache.containsKey(city))){
				return WeatherData.ERROR_RESULT;
			}
			
			// 判断是否在缓存中
			WeatherData pre = cache.get(city);
			String currentDate = currentDate();
			// 如果时间没有过期则返回
			if(pre!=null&&pre.getDate().equals(currentDate)){
				return pre;
			}
			LOGGER.info("缓存没有命中");
			
			String url = String.format("http://www.tianqi.com/%s/30/", city);

			Document document = Jsoup.parse(new URL(url), 3000);
			Element rootElement = null;
			try {
				rootElement = document.getElementsByClass("weaul").get(0);
			} catch (Exception e) {
				LOGGER.warn("failed to getElementsByClass " + "weaul");
			}
			Elements elements = null;
			try {
				elements = rootElement.getElementsByTag("li");
			} catch (Exception e) {
				LOGGER.warn("failed to getElementsByTag " + "li" + " from " + "weaul");
			}


			// <h3><b>12月30日</b> <em>今天</em></h3>
			// <ul>
			// <li class="img"><img
			// src="http://pic9.tianqijun.com/static/tianqi2018/ico2/b8.png"></li>
			// <li class="temp">雨 4~<b>11</b>℃</li>
			// <li>西北风 2级</li>
			// </ul>

			// <li>
			//    <a href="/shanghai/?qd=tq30" title="上海今天天气">
			//        <div class="weaul_q weaul_qblue"><span class="fl">11-27</span>
			//            <span class="fr">今天</span>
			//        </div>
			//        <div class="weaul_a"><img src="//static.tianqistatic.com/static/tianqi2018/ico2/b0.png"></div>
			//        <div class="weaul_z">晴</div>
			//        <div class="weaul_z"><span>12</span>~<span>16</span>℃</div>
			//        <!-- <div class="weaul_w">空气 <span
			//            style="background-color:#79b800;"        >优</span></div>
			//        <div class="weaul_s">东北风 1级</div> -->
			//        <div class="weaul_act">查看天气详情</div>
			//    </a>
			//</li>

			List<WeatherItem> weatherItems = new ArrayList<>(elements.size());
			for (Element element : elements) {

				// 时间
				String timeStr = element.getElementsByClass("fl").get(0).text();
				String[] times = timeStr.trim().split(" ");
				String date = timeStr;
				String dayKind = element.getElementsByClass("fr").get(0).text();

				// 图片
				// http://pic9.tianqijun.com/static/tianqi2018/ico2/b8.png
				String weatherImg = element.getElementsByTag("img").attr("src");
				int index = weatherImg.lastIndexOf("/");
				weatherImg = weatherImg.substring(index + 1);

				// 雨 4~11℃
				Elements weatherAndTemps = element.getElementsByClass("weaul_z");
				String weather = weatherAndTemps.get(0).text();
				Elements temps = weatherAndTemps.get(1).getElementsByTag("span");

				String minTemperature = temps.get(0).text();
				String maxTemperature = temps.get(1).text();

				// 西北风 2级
				String tempWind = element.html().substring(element.html().indexOf("weaul_s") + 9);
				String wind = tempWind.substring(0, tempWind.indexOf("<"));

				WeatherItem weatherItem = new WeatherItem(date, dayKind, weather, weatherImg, minTemperature,
						maxTemperature, wind);
				// System.out.println(weatherItem);
				weatherItems.add(weatherItem);
			}
			
			
			// 数据不一致
			String fisrtDate = weatherItems.get(0).getDate();
			if(!currentDate.endsWith(fisrtDate)) {
				currentDate = currentDate.split(" ")[0] + " "+ fisrtDate;
				LOGGER.info("日期不一致");
			}
			
			WeatherData weatherData = new WeatherData(pingyin2CityName.get(city), currentDate,weatherItems);
			cache.put(city,weatherData);
			return weatherData;

		} catch (Throwable e) {
			LOGGER.error(e);
		}
		return WeatherData.ERROR_RESULT;
	}

	
	/***
	 * 获得当前时间 "2015年 05月26日"
	 */
    private static String currentDate(){
        return new SimpleDateFormat("yyyy MM-dd").format(new Date());
    }
}
