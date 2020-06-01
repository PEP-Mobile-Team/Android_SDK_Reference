 var width = $(window).width();
$.ajaxSetup ({

    cache: false //close AJAX cache

});
 // 折线图的配置项和数据
    var option1 = {
        legend: {
                data: ["line1"],
                top: 30,
                // right: "6.5%",
                formatter: function (name) {
                    return  name;
                },
                itemGap: 30
            },
        tooltip:{
            triggerOn:'mousemove|click',
            formatter: function (data){
                    var html = '';
                    if(data.componentType == 'markPoint'){
                        html = data.seriesName;
                    }else{
                        html = data.name + '<br />' + data.value + '分';
                    }
                    return html
                }
        },
        xAxis: {
            type: 'category',
            name:'测评日期',
            data: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
            axisLine:{
                symbol:['none','path://M5,20 L5,5 L8,8 L5,2 L2,8 L5,5 L5.3,6 L5.3,20'],
                symbolOffset:-5,
                symbolSize:[35,38]
            }
        },
        yAxis: {
            type: 'value',
            name:'测评分数',
            min:0,
            max:100,
            axisLine:{
                symbol:['none','path://M5,20 L5,5 L8,8 L5,2 L2,8 L5,5 L5.3,6 L5.3,20'],
                symbolOffset:-5,
                symbolSize:[35,35]
            }
        },
        series: [{
            data: [0, 0, 0, 0, 0, 0, 0],
            type: 'line',
             symbolSize:10
        }]
    };

   // 雷达图的配置项和数据
    // var option2 = {
    //     tooltip:{

    //     },
    //     legend: {
    //         data: ['平均值', '最高值','最低值']
    //     }, 
    //     radar: {
    //         // shape: 'circle',
    //         name: {
    //             textStyle: {
    //                 color: '#fff',
    //                 backgroundColor: '#999',
    //                 borderRadius: 3,
    //                 padding: [1, 5]
    //            }
    //         },
    //         indicator: [
    //            { name: '完整度', max: 100},
    //            { name: '流畅度', max: 100},
    //            { name: '准确度', max: 100},
    //         ],
    //         center:['50%','55%'],
    //         splitArea: {
    //             show: false
    //         },
    //     },
    //     series: [{
    //         // name: '预算 vs 开销（Budget vs spending）',
    //         type: 'radar',
    //         // areaStyle: {normal: {}},
    //         data : [
    //             {
    //                 value:[],
    //                 name : '最高值'
    //             },
    //             {
    //                 value:[],
    //                 name : '平均值'
    //             },
    //             {
    //                 value:[],
    //                 name : '最低值'
    //             }
    //         ]
    //     }]
    // };

var option2 = {
    legend: {
        data: ['最高值', '平均值','最低值'],
        // left:'30%'
    },
    radar: [
        {

        },
        {
            indicator: [
                { text: '完整度', max: 100 },
                { text: '流畅度', max: 100 },
                { text: '准确度', max: 100 }
            ],
            nameGap:30,
            name: {
                // formatter:'【{value}】',
                textStyle: {
                    color:'#72ACD1'
                }
            },
            center: ['50%', '55%'],
            radius: 130
        }
    ],
    series: [
        {
            name: '',
            type: 'radar',
            radarIndex: 1,
            data: [
                {   
                    name:'最高值',
                    value: [],
                    label: {
                        normal: {
                            show: true,
                            // position:[-10,-20],
                            formatter:function(params) {
                                // console.log(params)
                                return params.value;
                            }
                        }
                    }
                },
                {
                    name:'平均值',
                    value: [],
                    label: {
                        normal: {
                            show: true,
                            formatter:function(params) {
                                // console.log(params)
                                return params.value;
                            }
                        }
                    }
                },  
                {
                    name:'最低值',
                    value: [],
                    label: {
                        normal: {
                            show: true,
                            formatter:function(params) {
                                return params.value;
                            }
                        }
                    }
                }
            ]
        }
    ]
}
    if(radiusSize == true){
        option2.radar[1].radius = 85;
        option2.radar[1].nameGap = 25;
        // option1.xAxis.name='';
        // option1.yAxis.name='';
    }
    var recordChart = echarts.init(document.getElementById('r-echart')); 
    recordChart.setOption(option1);
    window.onresize = recordChart.resize;
    var analysisChart = echarts.init(document.getElementById('a-echart')); 
    analysisChart.setOption(option2);
    window.onresize = analysisChart.resize;


    function TurnTo(data){
        console.log("turnto")
        var data = JSON.parse(data)
        var bookId;
        var unitId;
        var practiceId;
        var stick;
        var u = navigator.userAgent;
        var isAndroid = u.indexOf('Android') > -1 || u.indexOf('Adr') > -1; //android终端
        var isiOS = !!u.match(/\(i[^;]+;( U;)? CPU.+Mac OS X/); //ios终端
        if(data.bookId){
            bookId = data.bookId
        }
        if(data.unitId){
            unitId = data.unitId
        }
        if(data.practiceId){
            practiceId = data.practiceId
        }
        if(data.stick){
            stick = data.stick
        }
        if(bookId && unitId && practiceId && data.practiceId!= 'EvaDefaultPractice'){
            // if(isAndroid || isiOS){
            //     location.href = './prepare.html?bid='+bookId+'&uid='+unitId+'&pid='+practiceId+'&from=1'
            // }else{
            //     location.href = './prepare.html?bid='+bookId+'&uid='+unitId+'&pid='+practiceId
            // }

            if(width < 600){
                // 竖版
                location.href = './prepare.html?bid='+bookId+'&uid='+unitId+'&pid='+practiceId+'&from=1'
            }else{
                 location.href = './prepare.html?bid='+bookId+'&uid='+unitId+'&pid='+practiceId
            }
        }
        if(bookId && !unitId ){
            sessionStorage.setItem('menuData',JSON.stringify(data));
            
            // if(isAndroid || isiOS){
            //     location.href = './vertical-menu.html?bid='+data.bookId+'&from=1'
            // }else{
            //      location.href = './horizontal-menu.html?bid='+data.bookId
            // }

            if(width < 600){
                // 竖版
                location.href = './vertical-menu.html?bid='+data.bookId+'&from=1'
            }else{
                 location.href = './horizontal-menu.html?bid='+data.bookId
            }
        }
        if(bookId && unitId && (!data.practiceId || data.practiceId.indexOf('EvaDefaultPractice') > -1)){
            sessionStorage.setItem('menuData',JSON.stringify(data));
            // if(isAndroid || isiOS){
            //     location.href = './vertical-menu.html?bid='+data.bookId+'&from=1'
            // }else{
            //      location.href = './horizontal-menu.html?bid='+data.bookId
            // }

            if(width < 600){
                // 竖版
                location.href = './vertical-menu.html?bid='+data.bookId+'&from=1'
            }else{
                 location.href = './horizontal-menu.html?bid='+data.bookId
            }
        }
    }

    //根据具体'2019-8-15'这种格式的日期获取当前日期所在自然周日期
    function getMonDayAndSunDay(datevalue){
    // console.log(datevalue)  
        var dateValue = datevalue;  
        var arr = dateValue.split("-")  
        //月份-1 因为月份从0开始 构造一个Date对象  
        var date = new Date(arr[0],arr[1]-1,arr[2]);  
        var dateOfWeek = date.getDay();//返回当前日期的在当前周的某一天（0～6--周日到周一）  
        var dateOfWeekInt = parseInt(dateOfWeek,10);
        if(dateOfWeekInt==0){//如果是周日  
            dateOfWeekInt=7;  
        }  
        var daysFromSunday = 7-dateOfWeekInt;//当前于周末相差的天数  
      
        var temp2 = parseInt(arr[2],10);//按10进制转换，以免遇到08和09的时候转换成0  
        var sunDay = temp2+daysFromSunday;//当前日期的周日的日期  
        var monDay = sunDay-6//当前日期的周一的日期
        var tueDay = sunDay -5;
        var wedDay = sunDay -4;
        var thuDay = sunDay -3;
        var friDay = sunDay -2;
        var satDay = sunDay -1;  
        var weekdayArr =[monDay,tueDay,wedDay,thuDay,friDay,satDay,sunDay];
        // console.log(weekdayArr)
        var dayArr=[];
        var monthArr=[];
        var result = [];
        var oneWeekDays = [];    
        for(var i=0;i<7;i++){
            dayArr[i]= new Date(arr[0],arr[1]-1,weekdayArr[i]);
            monthArr[i] = parseInt(dayArr[i].getMonth()) + 1;
            // console.log(weekdayArr[i])
            oneWeekDays.push(monthArr[i] +'月'+ dayArr[i].getDate() +'日');
            result.push(dayArr[i].getFullYear() + '-' + monthArr[i] + '-' + dayArr[i].getDate());
        }
        // console.log(result)
        option1.xAxis.data = oneWeekDays;
        getSevDaysData(result);
    }

    var today = new Date();
        today = today.getFullYear() + '-' + (today.getMonth() + 1) + '-' + today.getDate();
        $('.date_picker').val(today);
        getMonDayAndSunDay(today);
        getLDdata();

    //获取自然周数组的相应七天的测评数据
     function getSevDaysData(getdateArr){
        for(var i=0;i<7;i++) {
            var jsonName = getdateArr[i] + '.json';
            var filename = "https://szgs-100.oss-cn-beijing.aliyuncs.com/tools/"+userId+"/"+accessKey+"/eval_tools/"+jsonName;
            $.ajax({
                url:filename,
                async:false,
                success:function(res) {
                    console.log(res)
                    if(res){
                        console.log(res)
                        option1.series[0].data[i] = res.average;
                    }
                },
                error:function(error){
                    console.log(error)
                    option1.series[0].data[i] = 0;
                }
            })
        }
        recordChart.setOption(option1);
    }




    //获取雷达图数据
    function getLDdata(){
        // console.log("重新获取雷达数据!!!")
        var aFilename ="https://szgs-100.oss-cn-beijing.aliyuncs.com/tools/"+userId+"/"+accessKey+"/eval_tools/analysis.json"
        // console.log(aFilename)
        $.ajax({
            url: aFilename,
            async: false,
            success: function(res){
                console.log(res)
                if(res){
                    option2.series[0].data[0].value = [res.IntegrityMax,res.FluencyMax,res.AccuracyMax];
                    option2.series[0].data[1].value = [res.averageIntegrity,res.averageFluency,res.averageAccuracy];
                    option2.series[0].data[2].value = [res.IntegrityMin,res.FluencyMin,res.AccuracyMin];
                    analysisChart.setOption(option2);
                }
                    // 使用刚指定的配置项和数据显示图表。
                    analysisChart.setOption(option2);
                    var $dataDetail = $('.data-detail');
                    $dataDetail.eq(0).html('完整度<br />'+'<span class="redLine">最高值：'+res.IntegrityMax+'</span><br /><span class="greenLine">最低值：'+res.IntegrityMin+'</span><br /><span class="blackLine">平均值：'+res.averageIntegrity+'</span>');
                    $dataDetail.eq(1).html('流畅度<br />'+'<span class="redLine">最高值：'+res.FluencyMax+'</span><br /><span class="greenLine">最低值：'+res.FluencyMin+'</span><br /><span class="blackLine">平均值：'+res.averageFluency+'</span>');
                    $dataDetail.eq(2).html('准确度<br />'+'<span class="redLine">最高值：'+res.AccuracyMax+'</span><br /><span class="greenLine">最低值：'+res.AccuracyMin+'</span><br /><span class="blackLine">平均值：'+res.averageAccuracy+'</span>');
            },
            error: function(error){
                console.log(error)
            }
        })
    }


    // 返回
    $('.return').on('click',function(){
        window.location.href="./index.html"
    })

    //查询
    $('.go').on('click',function(){
        // console.log($('.date_picker').val())
        getMonDayAndSunDay($('.date_picker').val())
    })
