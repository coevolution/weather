//index.js
//获取应用实例
const app = getApp();
// const baseUrl = "https://1.117.55.225/john-no-weather/weather/";
const baseUrl = "www.jlu.ink/john-no-weather/weather/";
var WxSearch = require('../../wxSearchView/wxSearchView.js');

Page({
  data: {
    city:'',
    inputShowed: false,
    baseImgPath:"../images/",
    haveWeatherItems:true,
    tmpweatherItems: null,
    isFormSearch : false,
    weatherData: { "queryName": "", "date": "", "weatherItems": [] }
  },

  onShareAppMessage: function (res) {
    return {
      title: '我在用小程序查天气，快一起来用~',
      desc: '及时了解一个月的天气情况！',
      path: 'pages/index/index',
      success: function (res) {
        // 转发成功
      },
      fail: function (res) {
        // 转发失败
      }
    }
  },

  onShow: function () {
    if (this.isFormSearch){
      this.isFormSearch = false;
      return;
    }
    var that = this;
    wx.getLocation({
      type: 'wgs84',
      success: function (res) {
        console.log("latitude=" + res.latitude + "longitude=" + res.longitude)
        // 根据经纬度获取天气情况
        wx.request({
          url: baseUrl + "?latitude=" + res.latitude + "&longitude=" + res.longitude,
          success: (res) => {
            console.log("get location 成功")
             that.setData({  weatherData: res.data });
          },
          fail: (res) => {
            console.log("get location 失败")
           },
          complete: () => { }
        })
      }
    })
  },

  // 搜索页面跳回
  onLoad: function (options) {
    if (options && options.searchValue) {
      var value = options.searchValue;
      if (value.length == 0) {
        return;
      }
      this.isFormSearch = true;
      wx.request({
        url: baseUrl + value,
        success: (res) => {
          this.setData({
            weatherData: res.data
          });
        },
        fail: (res) => {
          this.setData({
            weatherData: {
              "queryName": "杭州", "date": "2021-11-27 00:00:43", "weatherItems": [
                {"date":"2021-11-30","dayKind":"完","minTemperature":20,"maxTemperature":30,"weatherImg":"b1.png","weather":"好天"}
              ]
            }
          });
        }
      })
    }
  },

  // 搜索入口  
  wxSearchTab: function () {
    console.log("開始搜索")
    wx.redirectTo({
      url: '../search/search'
    })
  }

})
