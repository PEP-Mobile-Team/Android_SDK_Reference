var bid = GetQueryString('bid')
var uid = GetQueryString('uid')
var pid = GetQueryString('pid')
var width = $(window).width();
var titleText
var bookList
var title
var checkRole
var practice
var type 
var paraIndex
var senIndex
var isrecording = false
var isPause = false
var isTitle = false
var result = []
var pUrl
var time = 1000
var isClickRecord = false
var userData
var dataUrl = ''
localStorage.setItem('result','')

// 点击结束录音
$('.record').on('click',function(){recordClick()})

$('.pulse').on('click',function(){recordClick()})

$('.repeat').on('click',function(){Return()})
$('.action .result').on('click',function(){
    // var data = JSON.stringify({paraIndex: paraIndex,senIndex: senIndex})
    // CallFunc('endRecord',data)
    $('.action .result').unbind('click');
    pauseClick()
    if(!result[0]){
        jump();
    }else{
        init2();
    }
    
    // CallFunc('back','')
    // window.location.href="./result.html?bid="+bid+"&uid="+uid+"&pid="+pid
})
$('.return').on('click',function(){Return()})
$('.pause').on('click',function(){pauseClick()})
$('.play').on('click',function(){playClick()})


function callHtml(fnname,data){
    eval(fnname+"(data)")
}
CallFunc('getUserConfig','')
// GetUserConfig('{}')
function GetUserConfig(data) {
    var data = JSON.parse(data)
    if(data.dataUrl){
        dataUrl = data.dataUrl
    }
    // if(JSON.parse(sessionStorage.getItem('menuData'))){
    //    // console.log("存在session")
    //     var menuData = JSON.parse(sessionStorage.getItem('menuData'));
    //     data = menuData;
    //     menuData = "";
    //     sessionStorage.setItem('menuData',JSON.stringify(menuData));
    // }
    if(data.userId){
        userData = data
    }
    $.getJSON(dataUrl + 'data/booklist.json',function(data){
        bookList = data.bookList
        getPracticeJson()
    })
}
    function TurnTo(data){
        //console.log("turnto")
        var data = JSON.parse(data)
        //console.log(data)
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
            sessionStorage.setItem('menuData',JSON.stringify(data));
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


function getPracticeJson() {
    for(var i = 0; i < bookList.length; i++){
        if(bookList[i].bookId == bid){
            var uFile = bookList[i].file
            $.getJSON(dataUrl + 'data/'+uFile,function(data){
                unitList = data.unitList
                for(var j = 0; j < unitList.length; j++){
                    if(unitList[j].unitId == uid){
                        pFile = uFile.split('/')[0]
                        var page = unitList[j].practiceList
                        for(var p = 0; p < page.length; p++){
                            if(page[p].pageGid == pid){
                                titleText = page[p].pageName
                                $('.title').text(titleText)
                                var pageUrl = dataUrl + 'data/' + pFile + '/' + page[p].content
                                var index = parseInt(pageUrl.lastIndexOf('/'))
                                pUrl = pageUrl.substring(0,index)
                                $.getJSON(pageUrl,function(data){
                                    practice = data.practice
                                    type = practice.type
                                    init()
                                })
                            }
                        }
                    }
                }
            }) 
        }
    }   
}

var hornInterval 
function hornSetInterval(dom,bol){
    if(!bol){
        clearInterval(hornInterval)
    }
    if(dom != null && dom != undefined){
        hornInterval = setInterval(function(){
            dom.attr('src','./images/icon_horn_0.png')
            setTimeout(function(){
                dom.attr('src','./images/icon_horn_1.png')
            },500)
            setTimeout(function(){
                dom.attr('src','./images/icon_horn.png')
            },900)
        },1500)
    }
}

function init() {
    if(type == '对话'){
        checkRole = JSON.parse(localStorage.getItem('checkrole'))
    }
    if(type == '篇章'){
        $('.content').addClass('sentence')
    }
    var html = ""
    for(var i = 0; i < practice.paragraphList.length; i++){
        var item = practice.paragraphList[i]
        var liHtml = ""
        var title = item.title    
        if(title && title.audioUrl){
            var liStr = '<span class="span-title" data-url="'+title.audioUrl+'">'+title.showText+'</span><br />'
            liHtml += liStr
            isTitle = true
        }
        var list = item.sentenceList
        for(var j = 0; j < list.length ;j++){
            list[j].showText = list[j].showText.replace(/{/g,"")
            list[j].showText = list[j].showText.replace(/}/g," ")
            var liStr = '<span data-url="'+(pUrl+list[j].audioUrl)+'">'+list[j].showText +' </span>'
            liHtml += liStr
        } 
        var str = '<li class="clearfix"><img src="./images/icon_horn.png" alt=""><div class="sentenceText">'+liHtml+'</div><div class="border-line"></li></div>'
        html += str
    }
    $('.content ul').html(html)
    $('.content ul li').each(function(){
        var index = $(this).index()
        $(this).find('img').on('click',function(){
            if(isrecording && !isPause){
                return
            }
            hornSetInterval(null,false)
            var dom = $(this)
            dom.attr('src','./images/icon_horn_0.png')
            setTimeout(function(){
                dom.attr('src','./images/icon_horn_1.png')
            },500)
            setTimeout(function(){
                dom.attr('src','./images/icon_horn.png')
            },900)
            hornSetInterval(dom,true)
            paraIndex = index
            // if(isTitle){
            if(practice.paragraphList[paraIndex].title && practice.paragraphList[paraIndex].title.audioUrl){
                senIndex = -1
            }else{
                senIndex = 0
            }
            startMp3PlayPara()
        })
    })
    countDown()
}
// 倒计时
var countNum = $('.countdown').text()
function countDown() {
    var timer = setInterval(function(){
        if(countNum == 1){
            clearInterval(timer)
            $('.cover').css('display','none');
            $('.countdown').css('display','none');
            paraIndex = 0
            senIndex = isTitle ? -1 : 0
            initAudioPlay()
        }
        countNum = parseInt(countNum) - 1
        $('.countdown').text(countNum)
    },1000)
}

// 初始化播放
var recordList = []
var curListIndex = 0
function initAudioPlay(){
    if(type == '对话'){
        recordList = []
        curListIndex = 0
        var item = practice.paragraphList[paraIndex]
        var list = item.sentenceList
        $('.content ul li').eq(paraIndex).addClass('active').siblings('li').removeClass('active')
        // var role = list[senIndex].role.split('/')
        var role = []
        if(list[senIndex]){
            if(list[senIndex].role){
                var role = list[senIndex].role.split('/')
            }else{
                startMp3PlayPara()
                return
            }
        }else{
            startMp3PlayPara()
            return
        }
        var isExist = false
        for(var i = 0; i < checkRole.length; i++){
            for(j = 0; j < role.length; j++){
                if(role[j] == checkRole[i]){
                    isExist = true
                    var length = list.length
                    handleRoleRecord(list,length)
                    // $('#audio').attr('src',pUrl + list[senIndex].audioUrl)
                    // var audio = document.getElementById('audio')
                    // var duration
                    // audio.load();
                    // audio.oncanplay = function () {
                    //     duration = audio.duration * 1000
                    //     var data = JSON.stringify({url: '/' + pUrl+list[senIndex].audioUrl,sentence: list[senIndex].audioText,paraIndex: paraIndex,senIndex: senIndex,duration: duration})
                    //     CallFunc('startRecord',data)
                    // }
                    // setTimeout(function(){
                        blink()
                    // },500)
                    break
                }
            }
            if(isExist){
                break
            }
        }   
        if(!isExist){
            var dom = $('.content ul li').eq(paraIndex).find('img')
            dom.attr('src','./images/icon_horn_0.png')
            setTimeout(function(){
                dom.attr('src','./images/icon_horn_1.png')
            },500)
            setTimeout(function(){
                dom.attr('src','./images/icon_horn.png')
            },900)
            hornSetInterval(dom,true)
            startMp3PlayPara()
        }
    }
    if(type == '篇章'){
        // setTimeout(function(){
            $('.sentence ul li').eq(paraIndex).siblings('li').find('.sentenceText span').each(function(){
                $(this).removeClass('active')
            })
            var item = practice.paragraphList[paraIndex]
            if(item.title && item.title.audioUrl){
                $('.sentence ul li').eq(paraIndex).find('.sentenceText span').eq(senIndex + 1).addClass('active').siblings('span').removeClass('active')
            }else{
                $('.sentence ul li').eq(paraIndex).find('.sentenceText span').eq(senIndex).addClass('active').siblings('span').removeClass('active')
            }
            if(senIndex == -1){
                // title
                $('#audio').attr('src',pUrl + item.title.audioUrl)
                var audio = document.getElementById('audio')
                var duration
                audio.load();
                audio.oncanplay = function () {
                    duration = audio.duration * 1000
                    var data = JSON.stringify({url: '/' + pUrl+item.title.audioUrl,sentence: item.title.audioText,paraIndex: paraIndex,senIndex: senIndex,duration: duration})
                    CallFunc('startRecord',data)
                }
            }else{
                var list = item.sentenceList
                $('#audio').attr('src',pUrl + list[senIndex].audioUrl)
                var audio = document.getElementById('audio')
                var duration
                audio.load();
                audio.oncanplay = function () {
                    duration = audio.duration * 2000
                    var data = JSON.stringify({url: '/' + pUrl+list[senIndex].audioUrl,sentence: list[senIndex].audioText,paraIndex: paraIndex,senIndex: senIndex,duration: duration})
                    CallFunc('startRecord',data)
                }
            }
        // },1000)
        blink()
    }
    playOrPause(true)
}

// 处理分角色开始录音
function handleRoleRecord(list,length){
    var val = list[curListIndex]
    $('#audio').attr('src',pUrl + val.audioUrl)
    var audio = document.getElementById('audio')
    var duration
    audio.load();
    audio.oncanplay = function () {
        duration = audio.duration * 1000
        var sdata = {url: '/' + pUrl+val.audioUrl,sentence: val.audioText,duration: duration}
        recordList.push(sdata)
        if(curListIndex < length-1){
            curListIndex++;
            handleRoleRecord(list,length)
        }else{
            var data = JSON.stringify({recordList: recordList,paraIndex: paraIndex, senIndex: 0})
            CallFunc('startRecord2',data)
        }
    }
}

// 播放分角色原音
function startMp3PlayRole(){
    playOrPause(true)
    isrecording = false
    isRecord(false)
    var list = practice.paragraphList[0].sentenceList
    var data = JSON.stringify({url: '/' + pUrl+list[senIndex].audioUrl,paraIndex: paraIndex,senIndex: senIndex})
    CallFunc('startMp3Play',data)
}   

// 播放段落原音
function startMp3PlayPara(){
    playOrPause(true)
    isrecording = false
    isRecord(false)
    var item = practice.paragraphList[paraIndex]
    var list = item.sentenceList
    if(type == '对话'){
        $('.content ul li').eq(paraIndex).addClass('active').siblings('li').removeClass('active')
    }else{
        $('.content ul li').eq(paraIndex).siblings('li').find('.sentenceText span').each(function(){
            $(this).removeClass('active')
        })
        if(item.title && item.title.audioUrl){
            $('.content ul li').eq(paraIndex).find('.sentenceText span').eq(senIndex+1).addClass('active').siblings('span').removeClass('active')
        }else{
            $('.content ul li').eq(paraIndex).find('.sentenceText span').eq(senIndex).addClass('active').siblings('span').removeClass('active')
        }
    }
    var data
    if(senIndex == -1){
        // title
        data = JSON.stringify({url: '/' + pUrl+item.title.audioUrl,paraIndex: paraIndex,senIndex: senIndex})
    }else{
        data = JSON.stringify({url: '/' + pUrl+list[senIndex].audioUrl,paraIndex: paraIndex,senIndex: senIndex})
    }
    CallFunc('startMp3Play',data)
}

// 原音播放结束
function EndMp3Play(data){
    var data = JSON.parse(data)
    var length = practice.paragraphList[paraIndex].sentenceList.length
    if(senIndex == length - 1){
        if(paraIndex == practice.paragraphList.length - 1){
            // 练习结束
            hornSetInterval(null,false)
            endPractice()
        } else{
            // 下一段
            hornSetInterval(null,false)
            if(type == '对话'){
                if(paraIndex > 2){
                    var top = $('.content').scrollTop()
                    top = top + 37
                    $('.content').animate({scrollTop:top})
                }
            }else{
                var top = $('.content').scrollTop()
                top = top + $('.content ul li').eq(paraIndex).height()
                $('.content').animate({scrollTop:top})
            }
            paraIndex = paraIndex + 1
            senIndex = 0
            initAudioPlay()
        }
    }else{
        // 下一句 
        senIndex = senIndex + 1
        startMp3PlayPara() 
    }
}

// 录音测评结束
function EndRecord(data) {
    data = JSON.parse(data)
    // console.log(data)
    var task = pid + "" + data.paraIndex + "" + data.senIndex
    data.task = task
    var isExist = false
    for(var i = 0; i < result.length; i++){
        if(result[i].task == data.task){
            result[i] = data
            isExist = true
        }
    }
    if(!isExist){
        result.push(data)
    }
    // console.log(result)
    localStorage.setItem('result',JSON.stringify(result))
    // if(type == '对话' && isClickRecord){
    if(type == '对话'){
        // 通过点击录音按钮结束录音
        // isClickRecord = false
        if(parseInt(paraIndex) == practice.paragraphList.length - 1){
            // 练习结束
            endPractice()
        }else{
            // 下一段
            if(paraIndex > 2){
                var top = $('.content').scrollTop()
                var tempHeight1 = $('.content ul li').eq(paraIndex-1).height();
                var tempHeight2 = $('.content ul li').eq(paraIndex).height();
                var tempHeight3 = $('.content ul li').eq(paraIndex +1).height();
                console.log('tempHeight1+'+tempHeight1)
                console.log('tempHeight2+'+tempHeight2)
                console.log('tempHeight3+'+tempHeight3)
                if(tempHeight1 > 37){
                    top += tempHeight1
                }else if(tempHeight2 > 37){
                    top += tempHeight2
                }else if(tempHeight3 > 37){
                    top += tempHeight3
                }else{
                    top += 37
                }
                $('.content').animate({scrollTop:top})
            }
            isRecord(false)
            paraIndex = paraIndex + 1
            senIndex = 0
            initAudioPlay()
        }
    }else{
        var length = practice.paragraphList[paraIndex].sentenceList.length
        if(parseInt(senIndex) == length - 1){
            if(parseInt(paraIndex) == practice.paragraphList.length - 1){
                // 练习结束
                endPractice()
            }else{
                // 下一段
                var top = $('.content').scrollTop();
                var tempHeight = $('.content ul li').eq(paraIndex).height();
                console.log('tempHeightPARA='+tempHeight)
                if(tempHeight > 39){
                    top += tempHeight
                }else{
                    console.log(top)
                    top = top +55;
                }
                // top = top + $('.content ul li').eq(paraIndex).height()
                // top = top + $('.content ul li').eq(paraIndex).height()
                $('.content').animate({scrollTop:top})
                isRecord(false)
                paraIndex = paraIndex + 1
                senIndex = 0
                initAudioPlay()
            }
        }else{
            // 下一句
            senIndex = senIndex + 1
            initAudioPlay()
        }
    }
}

// 录音按钮闪动动画
function blink(){
    setTimeout(function(){
        $('.pulse').css('display','block')
        isRecord(true)
        $('.notice').css('visibility','visible')
    },100)
    isrecording = true
}

// 暂停or播放
function playOrPause(bool){
    isPause = !bool
    if(bool){
        // 播放
        $('.pause').css('display','inline-block')
        $('.play').css('display','none')
    }else{
        // 暂停
        $('.play').css('display','inline-block')
        $('.pause').css('display','none')
    }
}

// 暂停
function pauseClick(){
    playOrPause(false)
    if(isrecording){
        // 暂停录音，重新开始
        $('.pulse').css('display','none')
        $('.notice').css('visibility','hidden')
        var data = JSON.stringify({paraIndex: paraIndex,senIndex: senIndex})
        CallFunc('pauseRecord',data)
    }else{
        // 暂停原音播放
        hornSetInterval(null,false)
        var data = JSON.stringify({paraIndex: paraIndex,senIndex: senIndex})
        CallFunc('pauseMp3Play',data)
    }
}

function playClick(){
    playOrPause(true)
    if(isrecording){
        // 重新开始本句录音
        initAudioPlay()
    }else{
        // 继续播放原音
        var dom = $('.content ul li').eq(paraIndex).find('img')
        dom.attr('src','./images/icon_horn_0.png')
        setTimeout(function(){
            dom.attr('src','./images/icon_horn_1.png')
        },500)
        setTimeout(function(){
            dom.attr('src','./images/icon_horn.png')
        },900)
        hornSetInterval(dom,true)
        var data = JSON.stringify({paraIndex: paraIndex,senIndex: senIndex})
        CallFunc('continueMp3Play',data)
    }
}
function recordClick(){
    // if(time < 1000){
    //     return
    // }
    if(isrecording && isPause){
        // 重新开始本句录音
        initAudioPlay()
    }else{
        // 录音结束
        // isClickRecord = true
        $('.pulse').css('display','none')
        $('.notice').css('visibility','hidden')
        var data = JSON.stringify({paraIndex: paraIndex,senIndex: senIndex})
        CallFunc('endRecord',data)
    }
    // var timer = setInterval(function(){
    //     time--
    //     if(time == 0){
    //         clearInterval(timer)
    //     }
    // },1)
}

function isRecord(bool){
    if(bool){
        $('.norecord').css('display','none')
        $('.record').css('display','inline-block')
    }else{
        $('.norecord').css('display','inline-block')
        $('.record').css('display','none')
        $('.pulse').css('display','none')
        $('.notice').css('visibility','hidden')
    }
}
// 练习结束
function endPractice(){
    paraIndex = 0
    senIndex = isTitle ? -1 : 0
    playOrPause(false)
    isRecord(false)
    if(type == '对话'){
        $('.content ul li').removeClass('active')
        $('.content').animate({scrollTop: 0})
    }else{
        $('.sentence ul li').find('.sentenceText span').removeClass('active')
    }
    init2();

    // CallFunc('back','')
    // window.location.href="./result.html?bid="+bid+"&uid="+uid+"&pid="+pid
}

// 返回
function Return(){
    CallFunc('back','')
    window.location.href="./prepare.html?bid="+bid+"&uid="+uid+"&pid="+pid
}


var limitLength=0;
var readPara=0;
function init2() {
    console.log(result)
    for (var i = 0; i < practice.paragraphList.length; i++) {
         var item = practice.paragraphList[i]
        var list = item.sentenceList
        var paraArr = []
        if (result) {
            if (result.length && result.length > 0) {
                limitLength = result.length
                for (var m = 0; m < result.length; m++) {
                    if (result[m].paraIndex == i) {
                        paraArr.push(result[m])
                    }
                }
            }
        }
        if (paraArr.length > 0) {
            readPara += 1
            cHtml = handleParaScore(paraArr)
        }
    }
    if (result) {
        getOverallEvaluation()
    }

}

var totalIntegrity =0;
var totalAccuracy = 0;
var totalFluency = 0;
var overall = 0;
// 处理段落测评数据
function handleParaScore(arr) {

    // 完整度
    var integrity = 0
        // 准确度
    var accuracy = 0
        // 流畅度
    var fluency = 0

    var length = arr.length
    var arrIntegrity = 0
    for (var i = 0; i < arr.length; i++) {
        var senAccuracy = arr[i].average
        accuracy = accuracy + parseFloat(arr[i].average)
        var list = arr[i].wordList
        var senIntegrity = getIntegrity(list)
        integrity = integrity + parseFloat(senIntegrity)
        var recDuration = parseFloat(arr[i].recDuration)
        var mp3Duration = parseFloat(arr[i].mp3Duration)
        var recScore = getRecScore(recDuration, mp3Duration)
        if(integrity == 0 && accuracy == 0){
            fluency = 0
        }else{
            var senFluency = recScore * 0.3 + senAccuracy * 0.2 + senIntegrity * 0.5
            fluency = fluency + parseFloat(senFluency)
        }
    }
    accuracy = (accuracy / length).toFixed(0)
    integrity = (integrity / length).toFixed(0)
    fluency = (fluency / length).toFixed(0)

    totalIntegrity = parseInt(totalIntegrity,10)
    totalAccuracy = parseInt(totalAccuracy,10)
    totalFluency = parseInt(totalFluency,10)

    totalIntegrity = totalIntegrity + parseInt(integrity,10)
    totalAccuracy = totalAccuracy + parseInt(accuracy,10)
    totalFluency = totalFluency + parseInt(fluency,10)
}

// 整体评价
function getOverallEvaluation() {
    var length;
    if(type == '对话'){
     length = limitLength;
    }else{
        length = readPara;
    }    

    if(length == 0){
        return
    }

    totalIntegrity = (totalIntegrity / length).toFixed(0)
    totalAccuracy = (totalAccuracy / length).toFixed(0)
    totalFluency = (totalFluency / length).toFixed(0)
    totalIntegrity = $.isNumeric(totalIntegrity) ? totalIntegrity : 0
    totalAccuracy = $.isNumeric(totalAccuracy) ? totalAccuracy : 0
    totalAccuracy = $.isNumeric(totalAccuracy) ? totalAccuracy : 0
    if(totalIntegrity >100){
        totalIntegrity = 100;
    }
    if(totalAccuracy > 100) {
        totalAccuracy = 100;
    }
    if(totalAccuracy >100) {
        totalAccuracy =100
    }
     overall = (totalIntegrity * 0.5 + totalFluency * 0.3 + totalAccuracy * 0.2).toFixed(0)
    if(overall > 100) {
        overall = 100;        
    }
    if(totalFluency > 100){
        totalFluency = 100
    }
    var scoreList = [overall,totalIntegrity,totalFluency,totalAccuracy];
    sessionStorage.setItem('scoreList',JSON.stringify(scoreList));
    updateDayScore(overall,totalIntegrity,totalFluency,totalAccuracy)
}


function updateDayScore(score,totalIntegrity,totalFluency,totalAccuracy){ 
  
    console.log('score='+score)
    var content;
    var totalScore; 
    var average;
    var id;   
    var curDate = new Date();
    curDate = curDate.getFullYear() + '-' + (curDate.getMonth() + 1) + '-' + curDate.getDate();
    var jsonName = curDate + '.json'
    var filename ="https://szgs-100.oss-cn-beijing.aliyuncs.com/tools/"+userId+"/"+accessKey+"/eval_tools/"+jsonName;
    console.log(filename)
    var pracScore = score ? score : 0
    // 判断是否存在当天的json文件
    $.ajax({
        url: filename,
        async: false,
        success: function(res){
            console.log(res)
            if(res){
            id = res.id + 1;
            totalScore = res.total + parseInt(pracScore);
            average = (totalScore / id).toFixed(2);
            }
            content = JSON.stringify({id:id,total:parseInt(totalScore),score:parseInt(pracScore),average:Math.round(average)})

        },
        error: function(res){
            console.log(res);
            content = JSON.stringify({id:1,total:parseInt(pracScore),score:parseInt(pracScore),average:parseInt(pracScore)})
        }
    })
    //content = JSON.stringify({id:1,total:80,score:80,average:80})
    console.log(content)
    // 如果不加延时，上传会不生效
    setTimeout(function(){
        // console.log("上传文件")
        writeFile(jsonName,content,'application/json');
        document.getElementById('postfiles').click();
        jump();
    },100)
}

function jump() {
    setTimeout(function(){
        CallFunc('back','')
        window.location.href="./result.html?bid="+bid+"&uid="+uid+"&pid="+pid 
    },100)
}


function getIntegrity(list) {
    var integrity = 0
    var totalNum = list.length
    var readNum = list.length
    for (var i = 0; i < list.length; i++) {
        if (list[i].grade == -1) {
            readNum -= 1
        }
    }
    integrity = (readNum / totalNum * 100).toFixed(0)
    return integrity
}

// 计算单句的录音时长分
function getRecScore(recDuration, mp3Duration) {
    var recScore = 0
    if (recDuration <= mp3Duration * 1.6) {
        recScore = 100
    } else if (recDuration <= mp3Duration * 1.7) {
        recScore = 95
    } else if (recDuration <= mp3Duration * 1.8) {
        recScore = 85
    } else if (recDuration <= mp3Duration * 2.0) {
        recScore = 75
    } else if (recDuration <= mp3Duration * 3.0) {
        recScore = 65
    }  else {
        recScore = 50
    }
    return recScore
}


