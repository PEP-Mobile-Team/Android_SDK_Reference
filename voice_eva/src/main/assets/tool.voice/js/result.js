var bid = GetQueryString('bid')
var uid = GetQueryString('uid')
var pid = GetQueryString('pid')
var title
// var bookList
var width = $(window).width();
var unitList
var practice
var type
var paraIndex
var senIndex
var pUrl
var practiceId = pid
var hasNext = false
var nextpid
    // var isTitle
var totalIntegrity = 0
var totalAccuracy = 0
var totalFluency = 0
var overall = 0;
var readPara = 0
var roleIndex
// var userData
// var dataUrl = ''
var result = null

if(localStorage.getItem('result')){
    result = JSON.parse(localStorage.getItem('result'))
}
// function callHtml(fnname, data) {
//     eval(fnname + "(data)")
// }


$.getJSON(dataUrl + 'data/booklist.json', function(res) {
    bookList = res.bookList
    getPracticeJson()
})         
    


    function TurnTo(data){
        //console.log("turnto")
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
    // console.log(dataUrl)
    for (var i = 0; i < bookList.length; i++) {
        if (bookList[i].bookId == bid) {
            var uFile = bookList[i].file
            $.getJSON(dataUrl + 'data/' + uFile, function(data) {
                // console.log(dataUrl + 'data/' + uFile)
                // console.log(data)
                unitList = data.unitList
                for (var j = 0; j < unitList.length; j++) {
                    if (unitList[j].unitId == uid) {
                        var pFile = uFile.split('/')[0]
                        var page = unitList[j].practiceList
                        for (var p = 0; p < page.length; p++) {
                            if (page[p].pageGid == pid) {
                                if (page[p + 1]) {
                                    nextpid = page[p + 1].pageGid
                                    hasNext = true
                                } else {
                                    $('.next').css('background', '#D6D6D6')
                                }
                                title = page[p].pageName
                                $('.title').text(title)
                                var pageUrl = dataUrl + 'data/' + pFile + '/' + page[p].content
                                var index = parseFloat(pageUrl.lastIndexOf('/'))
                                pUrl = pageUrl.substring(0, index)
                                $.getJSON(pageUrl, function(data) {
                                    // console.log(data)
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

var limitLength;
var count = 1;
function init() {
    var html = ""
    var num = 0;
    console.dir(practice.paragraphList)
    for (var i = 0; i < practice.paragraphList.length; i++) {
        num++;
        console.log(limitLength)
        var item = practice.paragraphList[i]
        var liHtml = ""
        var title = item.title
        var cHtml = ''
        var paraArr = []
        if (result) {
            console.log(result)
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
            // console.log(cHtml)
        }

        if (title && title.audioUrl) {
            // var titleItem = paraArr.filter((item) => {return item.senIndex == -1})
            var titleItem
            for (var p = 0; p < paraArr.length; p++) {
                if (paraArr[p].senIndex == -1) {
                    titleItem = paraArr[p]
                }
            }
            var titleText = title.showText
            if (titleItem && titleItem.wordList.length > 0) {
                titleText = handleParaShowText(titleItem.wordList, titleText)
            }
            var liStr = '<span class="span-title" data-url="' + title.audioUrl + '">' + titleText + '</span><br />'
            liHtml += liStr
                // isTitle = true
        }
        var list = item.sentenceList
        console.log(item)
        for (var j = 0; j < list.length; j++) {
            console.log(list[j].showText)
            var showtext = list[j].showText
            var temptext = showtext;
            console.log(showtext)
            for (var n = 0; n < paraArr.length; n++) { 
                if (type == '对话') {
                    if (paraArr[n].paraIndex == i) {
                        wordlist = paraArr[n].wordList
                        showtext = handleRoleShowText(paraArr[n].paraIndex,wordlist, showtext)
                    }
                } else {
                    if (paraArr[n].senIndex == j) {
                        console.log(showtext)
                        wordlist = paraArr[n].wordList
                        console.log(wordlist)
                        showtext = handleParaShowText(wordlist, showtext)
                        console.log(showtext)
                    }
                }
            }
            showtext = showtext.replace(/{/g,"")
            showtext = showtext.replace(/}/g,"")
             var liStr;
             if(type == '篇章'){
                console.log("-----------------------------")
                console.log(showtext)
                var listss = temptext.split(' ')
                console.log(listss)
                console.log(listss.length)

                if(listss.length == 1){
                    liStr = '<span data-url="' + (pUrl + list[j].audioUrl) + '">' + showtext + ' </span>'
                }else{

                    if(showtext.indexOf('<span') == -1){
                        console.log(showtext)
                        var tempList= showtext.split(' ');
                        console.log(tempList)
                        console.log(tempList.length)
                        var tempStr ='';
                        for(var k=0;k<tempList.length;k++){
                            tempStr += '<span>'+tempList[k]+'&nbsp;</span>'
                            // tempList[k] = '<span>'+tempList[k]+'&nbsp;</span>'
                        }
                        // // showtext=tempList.join('')
                        showtext = tempStr;
                        console.log(showtext)
                        // console.log(showtext)
                         liStr = '<span data-url="' + (pUrl + list[j].audioUrl) + '">' + showtext + ' </span>'
                    }
                    else{
                        liStr = '<span data-url="' + (pUrl + list[j].audioUrl) + '">' + showtext + ' </span>'
                    } 
                }   
             }else{
                liStr = '<span data-url="' + (pUrl + list[j].audioUrl) + '">' + showtext + ' </span>'
             }  
             // liStr = '<span data-url="' + (pUrl + list[j].audioUrl) + '">' + showtext + ' </span>'        
            liHtml += liStr
        }

        var str = '<li class="clearfix"><img class="horn" src="./images/icon_horn.png" alt=""><div class="sentenceText">' + liHtml + '</div><div class="list-res-class clearfix">' + cHtml + '</div><div class="border-line"></div></li>'
        html += str
        // console.log(html)
    }

    console.log('type='+type)
    $('.content .list ul').html(html);
    if(type == '篇章'){
        addText(num,practice.paragraphList.length,practice.paragraphList)
    }
    var tempWidth = $('.sentenceText').width();
    var spanWidth = 0;
    $('li').eq(0).find('.sentenceText .span-title').find('span').each(function(){
        spanWidth += $(this).width()
        if(spanWidth>tempWidth){
            $(this).parents('.span-title').find('span').css('float','left');
        }
    })
    
    // console.log('num='+num)
    // console.log(html)

    if (result) {
        getOverallEvaluation()
    }
    $('.content .list ul li').each(function() {
        var index = $(this).index()
        $(this).find('.horn').on('click', function() {
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
            if (practice.paragraphList[paraIndex].title && practice.paragraphList[paraIndex].title.audioUrl) {
                senIndex = -1
            } else {
                senIndex = 0
            }
            startMp3PlayPara()
        })
        $(this).find('.playR').on('click', function() {
            event.stopPropagation()
            hornSetInterval(null,false)
            paraIndex = index
                // if(isTitle){
            if (practice.paragraphList[paraIndex].title && practice.paragraphList[paraIndex].title.audioUrl) {
                senIndex = -1
            } else {
                senIndex = 0
            }
            var data = JSON.stringify({ paraIndex: paraIndex, senIndex: senIndex })
            startRecPlay(data)
        })
    })
}


function addText(a,b,list){
        var addHtml ='';
        if(a<b){
            for(var i=a;i<b;i++){
                var liHtml = '';
                list[i].sentenceList.forEach(function(item){
                    var showtext = item.showText;
                     if(showtext.indexOf('<span') == -1){
                        var tempList= showtext.split(' ');
                        var tempStr ='';
                        for(var i=0;i<tempList.length;i++){
                            tempStr += '<span>'+tempList[i]+'&nbsp;</span>'
                        }
                        showtext = tempStr;
                        var liStr = '<span data-url="' + (pUrl + item.audioUrl) + '">' + showtext + ' </span>'
                        liHtml += liStr
                    }                  
                })
                var cHtml = '';
                var str = '<li class="clearfix"><img class="horn" src="./images/icon_horn.png" alt=""><div class="sentenceText">' + liHtml + '</div><div class="list-res-class clearfix">' + cHtml + '</div><div class="border-line"></div></li>';
                addHtml += str;
                console.log(addHtml)
            }
            $('.content .list ul').append(addHtml);
        }
    }
// 播放原音
function startMp3PlayPara() {
    var item = practice.paragraphList[paraIndex]
    var list = item.sentenceList
    var data
    if (senIndex == -1) {
        // title
        data = JSON.stringify({ url: '/' + pUrl + item.title.audioUrl, paraIndex: paraIndex, senIndex: senIndex })
    } else {
        data = JSON.stringify({ url: '/' + pUrl + list[senIndex].audioUrl, paraIndex: paraIndex, senIndex: senIndex })
    }
    CallFunc('startMp3Play', data)
}
// 原音播放结束
function EndMp3Play(data) {
    var data = JSON.parse(data)
    var length = practice.paragraphList[paraIndex].sentenceList.length
    if (senIndex != length - 1) {
        senIndex = senIndex + 1
        startMp3PlayPara()
    }else{
        hornSetInterval(null,false)
    }
}

// 播放录音
function startRecPlay(data) {
    CallFunc('startRecPlay', data)
}
// 播放录音结束
function EndRecPlay(data) {
    if (type == '对话') {
        return
    }
    var length = practice.paragraphList[paraIndex].sentenceList.length
    if (senIndex != length - 1) {
        // 下一句 
        senIndex = senIndex + 1
        var data = JSON.stringify({ paraIndex: paraIndex, senIndex: senIndex })
        startRecPlay(data)
    }
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

// 计算单句的完整度
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

// 处理对话数据
function handleRoleScore(data) {
    console.log(data);
    // 完整度
    var list = data.wordList
    var integrity = getIntegrity(list)

    // 准确度
    var accuracy = data.average

    // 流畅度
    var fluency = 0
    var recDuration = parseFloat(data.recDuration)
    var mp3Duration = parseFloat(data.mp3Duration)
    var recScore = getRecScore(recDuration, mp3Duration)
    fluency = recScore * 0.3 + accuracy * 0.2 + integrity * 0.5
    fluency = fluency.toFixed(0)

    totalIntegrity = parseInt(totalIntegrity,10)
    totalAccuracy = parseInt(totalAccuracy,10)
    totalFluency = parseInt(totalFluency,10)

    totalIntegrity = totalIntegrity + parseInt(integrity,10)
    totalAccuracy = totalAccuracy + parseInt(accuracy,10)
    totalFluency = totalFluency + parseInt(fluency,10)

    return '<div>完整度： ' + integrity + '</div><div>流畅度： ' + fluency + '</div><div>准确度： ' + accuracy + '</div><div class="playR"><img src="./images/icon_headset.png" alt=""> <span>听录音</span></div>'
}

// 处理段落测评数据
function handleParaScore(arr) {

    // if(count > limitLength){
    //     count++;
    //     return;
    // }
    // count++;
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
    return '<div>完整度： ' + integrity + '</div><div>流畅度： ' + fluency + '</div><div>准确度： ' + accuracy + '</div><div class="playR"><img src="./images/icon_headset.png" alt=""> <span>听录音</span></div>'
}

// 整体评价
function getOverallEvaluation() {
    if(sessionStorage.getItem('scoreList')){
        var list = JSON.parse(sessionStorage.getItem('scoreList'));
    }
    $('.overall .res-class1').html('完整度： ' + list[1])
    $('.overall .res-class2').html('流畅度： ' + list[2])
    $('.overall .res-class3').html('准确度： ' + list[3])
    $('.overall .res-txt span').html(list[0])
    if (list[0] > 85) {
        $('.overall .icon2').css('display', 'block')
        $('.overall .icon1').css('display', 'none')
        $('.overall .icon0').css('display', 'none')
        $('.overall .res-desc').html('你真棒！')
    } else if (list[0] > 60) {
        $('.overall .icon1').css('display', 'block')
        $('.overall .icon0').css('display', 'none')
        $('.overall .icon2').css('display', 'none')
        $('.overall .res-desc').html('还不错哦！')
    } else {
        $('.overall .icon0').css('display', 'block')
        $('.overall .icon1').css('display', 'none')
        $('.overall .icon2').css('display', 'none')
        $('.overall .res-desc').html('要加油哦！')
    }
    console.log('overall='+overall)

     updateAnalysis(list[1],list[2],list[3])
}

// 处理录音文字颜色显示
function handleShowText1(wordlist, showtext) {
    var roleTxt = ''
    var roleIndex = showtext.indexOf(':') + 1
    if (type == '对话') {
        roleTxt = showtext.substring(0, roleIndex)
        showtext = showtext.slice(roleIndex)
    }
    if (wordlist.length > 0) {
        for (var n = 0; n < wordlist.length; n++) {
            var wordindex = showtext.indexOf(" " + wordlist[n].word)
            var wordlastindex = wordindex + wordlist[n].word.length + 1
            if (wordindex == -1) {
                wordindex = showtext.indexOf(wordlist[n].word)
                wordlastindex = wordindex + wordlist[n].word.length
            }
            if (wordindex != -1) {
                // 存在此字符
                var st = showtext.substring(0, wordindex)
                var end = showtext.substring(wordlastindex, showtext.length)
                var readText = showtext.substring(wordindex, wordlastindex)
                var rgrade = parseFloat(wordlist[n].grade)
                rgrade = $.isNumeric(rgrade) ? rgrade : 0
                if(readText.indexOf('{')>-1 || readText.indexOf('}')>-1){
                    readText = readText.replace(/{/g,"")
                    readText = readText.replace(/}/g," ")
                }
                var rclass = rgrade >= 85 ? "绿色" : ((rgrade >= 60 && rgrade <= 84) ? "橙色" : (rgrade >= 0 ? "红色" : ""))
                readText = '<标签 类名="' + rclass + '">' + readText + '</标签>'
                showtext = st + readText + end
            }
        }
    }
    showtext = showtext.replace(/标签/g, 'span')
    showtext = showtext.replace(/类名/g, 'class')
    showtext = showtext.replace(/绿色/g, 'green')
    showtext = showtext.replace(/橙色/g, 'orange')
    showtext = showtext.replace(/红色/g, 'red')
    if (type == '对话') {
        return roleTxt + showtext
    }
    return showtext
}
function handleParaShowText(wordlist, showtext) {
        var htmlText = ''
    if (wordlist.length > 0) {
        for (var n = 0; n < wordlist.length; n++) {
            if(wordlist[n].word.indexOf('{') != -1){
                var start = showtext.indexOf('{')
                var end = showtext.indexOf('}')
                if(start != -1 && end != -1){
                    var hbEng = showtext.substring(start,end+1)
                    wordlist[n].word = hbEng
                }
            }
            var resText = ""
            var wordindex = showtext.indexOf(wordlist[n].word)
            var wordlastindex = wordindex + wordlist[n].word.length
            var minStr = showtext.substring(0,1);
            var patt1 = new RegExp(/[\ |\~|\`|\!|\@|\#|\$|\%|\^|\&|\*|\(|\)|\-|\_|\+|\=|\||\\|\[|\]|\{|\}|\;|\:|\"|\'|\,|\<|\.|\>|\/|\?]/);
            if(patt1.test(minStr)){
                wordindex = 0;
            }
             console.log('wordindex='+wordindex)
            var readText = showtext.substring(wordindex, wordlastindex)
            if(wordlist[n+1]){
                var nextShowText = showtext.replace(wordlist[n].word,'')
                var wordindex1 = nextShowText.indexOf(wordlist[n+1].word)
                if(wordindex1 == -1){
                    wordindex1 = showtext.length
                }else{
                    wordindex1 = wordlist[n].word.length + wordindex1

                }
            }else{
                var wordindex1 = showtext.length
            }
            console.log('-----------------------------------------')
            console.log('showtext='+showtext)
            console.log('readText='+readText)
            var symbol = showtext.substring(wordlastindex,wordindex1)
            console.log('symbol='+symbol)
            var tempList = symbol.split(' ');
            console.log(tempList)
            if(tempList.length > 1 && (tempList[0]!= '.' && tempList[0]!= '$' && tempList[0]!= '?' && tempList[0]!= '!' && tempList[0]!= ':' && tempList[0]!= ',')){
                for(var p = 0;p<tempList.length;p++){
                    if(tempList[p] == ''){
                        tempList.shift()
                    }else{
                        break;
                    }
                }
                symbol = '';
            }else{
                symbol = tempList[0];
                for(var m =1; m<tempList.length;m++){
                    var minStr = tempList[m];
                    var patt1 = new RegExp(/[\ |\~|\`|\!|\@|\#|\$|\%|\^|\&|\*|\(|\)|\-|\_|\+|\=|\||\\|\[|\]|\{|\}|\;|\:|\"|\'|\,|\<|\.|\>|\/|\?]/);
                    if(patt1.test(minStr)){
                        symbol +=  minStr;
                    }
                }
            }
            console.log('symbol='+symbol)
            var used = readText+ symbol
            showtext = showtext.replace(used,'')
            var aNum = n + 1; 
            console.log('aNum='+aNum)
            console.log(showtext)
            console.log('wordlist.length='+wordlist.length)
            console.log('readText='+readText)
            if((aNum == wordlist.length && showtext.indexOf('(')>-1 && showtext.indexOf(')')>-1) || (aNum == wordlist.length && (showtext.indexOf('?')>-1 || (showtext.indexOf('.')>-1 && showtext.indexOf('..') == -1) || (showtext.indexOf('...')>-1 && showtext.indexOf(')') == -1) || showtext.indexOf('!')>-1))){
                symbol += showtext
            }
            // if(aNum == wordlist.length && showtext.split(' ').length>1){
            //     console.log(showtext.split(' '))
            //     console.log(showtext.split(' ').length)
            //     console.log("---------------------------------------------------------------")
            //     if(showtext.indexOf(readText) > -1){
            //         return
            //     }else{
            //         symbol += showtext  
            //     }
            // }
            console.log('symbol222='+symbol)
            if (wordindex != -1) {
                // 存在此字符
                var rgrade = parseFloat(wordlist[n].grade)
                rgrade = $.isNumeric(rgrade) ? rgrade : 0
                if(readText.indexOf('{')>-1 || readText.indexOf('}')>-1){
                    readText = readText.replace(/{/g,"")
                    readText = readText.replace(/}/g," ")
                }
                var rclass = rgrade >= 85 ? "green" : ((rgrade >= 60 && rgrade <= 84) ? "orange" : (rgrade >= 0 ? "red" : ""))
                readText = '<span class="' + rclass + '">' + readText + symbol + '&nbsp;</span>'
                resText = readText 
            }
            htmlText += resText
        }
    }
    return htmlText
}
var curParaIndex = null
var curWordList = null
function handleRoleShowText(paraIndex,wordlist, showtext) {
    console.log("role")
    if(curParaIndex == null){
        curParaIndex = paraIndex
        curWordList = wordlist
    }else{
        if(paraIndex == curParaIndex){
            wordlist = curWordList
        }else{
            curParaIndex = paraIndex
            curWordList = wordlist
        }
    }
    var roleTxt = ''
    var roleIndex = showtext.indexOf(':') + 1
    // if (type == '对话') {
        roleTxt = showtext.substring(0, roleIndex)
        showtext = showtext.slice(roleIndex)
    // }
    var htmlText = ''
    if (wordlist.length > 0) {
        for (var n = 0; n < wordlist.length; n++) {
            if(wordlist[n].word.indexOf('{') != -1){
                var start = showtext.indexOf('{')
                var end = showtext.indexOf('}')
                if(start != -1 && end != -1){
                    var hbEng = showtext.substring(start,end+1)
                    wordlist[n].word = hbEng
                }
            }
            if(wordlist[n+1]){
                if(wordlist[n+1].word.indexOf('{') != -1){
                    var start = showtext.indexOf('{')
                    var end = showtext.indexOf('}')
                    if(start != -1 && end != -1){
                        var hbEng = showtext.substring(start,end+1)
                        wordlist[n+1].word = hbEng
                    }
                }
            }
            var resText = ""
            var wordindex = showtext.indexOf(wordlist[n].word)
            if (wordindex != -1) {
                // 存在此字符
                if(wordlist[n+1]){
                    // var wordindex1 = showtext.indexOf(wordlist[n+1].word)
                    var nextShowText = showtext.replace(wordlist[n].word,'')
                    var wordindex1 = nextShowText.indexOf(wordlist[n+1].word)
                    if(wordindex1 == -1){
                        wordindex1 = showtext.length
                    }else{
                        wordindex1 = wordlist[n].word.length + wordindex1
                    }
                }else{
                    var wordindex1 = showtext.length
                }
                var wordlastindex = wordindex + wordlist[n].word.length
                var symbol = showtext.substring(wordlastindex,wordindex1)
                var readText = showtext.substring(wordindex, wordlastindex)
                var used = readText + symbol
                showtext = showtext.replace(used,'')
                var rgrade = parseFloat(wordlist[n].grade)
                rgrade = $.isNumeric(rgrade) ? rgrade : 0
                if(readText.indexOf('{')>-1 || readText.indexOf('}')>-1){
                    readText = readText.replace(/{/g,"")
                    readText = readText.replace(/}/g," ")
                }
                // if(type=='对话'){
                    
                // }
                var rclass = rgrade >= 85 ? "green" : ((rgrade >= 60 && rgrade <= 84) ? "orange" : (rgrade >= 0 ? "red" : ""))
                readText = '<span style="float:none !important" class="' + rclass + '">' + used + '</span>'
                resText = readText
            }else{
                curWordList.splice(0,n)
                break
            }
            htmlText += resText
        }
    }
    // htmlText = htmlText.replace(/标签/g, 'span')
    // htmlText = htmlText.replace(/类名/g, 'class')
    // htmlText = htmlText.replace(/绿色/g, 'green')
    // htmlText = htmlText.replace(/橙色/g, 'orange')
    // htmlText = htmlText.replace(/红色/g, 'red')
    // if (type == '对话') {
        return roleTxt + ' ' + htmlText
    // }
    return htmlText
}

    // function updateDayScore(score,totalIntegrity,totalFluency,totalAccuracy){ 
      
    //     console.log('score='+score)
    //             var content;
    //             var totalScore; 
    //             var average;
    //             var id;   
    //             var curDate = new Date();
    //             curDate = curDate.getFullYear() + '-' + (curDate.getMonth() + 1) + '-' + curDate.getDate();
    //             var jsonName = curDate + '.json'
    //             console.log(userId)
    //             var filename ="https://szgs-100.oss-cn-beijing.aliyuncs.com/tools/"+userId+"/"+accessKey+"/eval_tools/"+jsonName;
    //             console.log(filename)
    //             var pracScore = score ? score : 0
    //             // 判断是否存在当天的json文件
    //             $.ajax({
    //                 url: filename,
    //                 async: false,
    //                 success: function(res){
    //                     console.log(res)
    //                     if(res){
    //                     id = res.id + 1;
    //                     totalScore = res.total + parseInt(pracScore);
    //                     average = (totalScore / id).toFixed(2);
    //                     }
    //                     content = JSON.stringify({id:id,total:parseInt(totalScore),score:parseInt(pracScore),average:Math.round(average)})

    //                 },
    //                 error: function(res){
    //                     console.log(res);
    //                     content = JSON.stringify({id:1,total:parseInt(pracScore),score:parseInt(pracScore),average:parseInt(pracScore)})
    //                 }
    //             })
    //             //content = JSON.stringify({id:1,total:80,score:80,average:80})
    //             console.log(content)
    //             // 如果不加延时，上传会不生效
    //             setTimeout(function(){
    //                 console.log("上传文件")
    //                 writeFile(jsonName,content,'application/json');
    //                 document.getElementById('postfiles').click();
    //                 updateAnalysis(totalIntegrity,totalFluency,totalAccuracy);
    //             },100) 
    //         }
            
            // 更新测评分析
            // 测评分析保存的数据结构：

            function updateAnalysis(totalIntegrity,totalFluency,totalAccuracy){

                // 判断是否存在测评分析的json文件
                var aFilename ="https://szgs-100.oss-cn-beijing.aliyuncs.com/tools/"+userId+"/"+accessKey+"/eval_tools/analysis.json";
                var aRes = {}
                var totalAllIntegrity,totalAllFluency,totalAllAccuracy,id,averageIntegrity,averageFluency,averageAccuracy,IntegrityMax,FluencyMax,AccuracyMax,IntegrityMin,FluencyMin,AccuracyMin;  
                $.ajax({
                    url: aFilename,
                    async: false,
                    success: function(res){
                        console.log(res)
                        // if(JSON.stringify(res) != '{}'){

                        if(res){
                            totalAllIntegrity = parseInt(res.totalAllIntegrity) + parseInt(totalIntegrity);
                            totalAllFluency = parseInt(res.totalAllFluency) + parseInt(totalFluency);
                            totalAllAccuracy = parseInt(res.totalAllAccuracy) + parseInt(totalAccuracy);
                            id = res.id +1;
                            averageIntegrity = (totalAllIntegrity / id).toFixed(2);
                            averageFluency = (totalAllFluency / id).toFixed(2);
                            averageAccuracy = (totalAllAccuracy / id).toFixed(2);
                            IntegrityMax = parseInt(res.IntegrityMax) < parseInt(totalIntegrity) ? parseInt(totalIntegrity) : parseInt(res.IntegrityMax);
                            FluencyMax = parseInt(res.FluencyMax) < parseInt(totalFluency) ? parseInt(totalFluency) : parseInt(res.FluencyMax);
                            AccuracyMax = parseInt(res.AccuracyMax) < parseInt(totalAccuracy) ? parseInt(totalAccuracy) : parseInt(res.AccuracyMax);
                            IntegrityMin = parseInt(res.IntegrityMin) > parseInt(totalIntegrity) ? parseInt(totalIntegrity) : parseInt(res.IntegrityMin);
                            FluencyMin = parseInt(res.FluencyMin) > parseInt(totalFluency) ? parseInt(totalFluency) : parseInt(res.FluencyMin);
                            AccuracyMin = parseInt(res.AccuracyMin) > parseInt(totalAccuracy) ? parseInt(totalAccuracy) : parseInt(res.AccuracyMin);
                        }

                        aRes = {
                                id:parseInt(id),
                                totalAllIntegrity:parseInt(totalAllIntegrity),
                                totalAllFluency:parseInt(totalAllFluency),
                                totalAllAccuracy:parseInt(totalAllAccuracy),
                                averageIntegrity:Math.round(averageIntegrity),
                                averageFluency:Math.round(averageFluency),
                                averageAccuracy:Math.round(averageAccuracy),
                                IntegrityMax:parseInt(IntegrityMax),
                                FluencyMax:parseInt(FluencyMax),
                                AccuracyMax:parseInt(AccuracyMax),
                                IntegrityMin:parseInt(IntegrityMin),
                                FluencyMin:parseInt(FluencyMin),
                                AccuracyMin:parseInt(AccuracyMin)       
                        }

                    },
                    error: function(res){
                        console.log(res)
                        aRes = {
                                id:1,
                                totalAllIntegrity:parseInt(totalIntegrity),
                                totalAllFluency:parseInt(totalFluency),
                                totalAllAccuracy:parseInt(totalAccuracy),
                                averageIntegrity:parseInt(totalIntegrity),
                                averageFluency:parseInt(totalFluency),
                                averageAccuracy:parseInt(totalAccuracy),
                                IntegrityMax:parseInt(totalIntegrity),
                                FluencyMax:parseInt(totalFluency),
                                AccuracyMax:parseInt(totalAccuracy),
                                IntegrityMin:parseInt(totalIntegrity),
                                FluencyMin:parseInt(totalFluency),
                                AccuracyMin:parseInt(totalAccuracy)
                        }
                    }
                })

                var content = JSON.stringify(aRes);  // 存储到服务器的json数据
                console.log(content);
                // 如果不加延时，上传会不生效
                setTimeout(function(){
                    writeFile('analysis.json',content,'application/json');
                    document.getElementById('postfiles').click();
                },100) 
            }
      


// 再来一次
$('.once').on('click', function() { window.location.href = "./prepare.html?bid=" + bid + "&uid=" + uid + "&pid=" + pid })

// 返回列表
$('.a-return').on('click', function() {
    var width = $(window).width()
    if (width < 600) {
        // 竖版
        window.location.href = "./vertical-menu.html?bid=" + bid
    } else {
        window.location.href = "./horizontal-menu.html?bid=" + bid
    }
})

// 下一个练习
$('.next').on('click', function() {
    if (hasNext) {
        window.location.href = './prepare.html?bid=' + bid + '&uid=' + uid + '&pid=' + nextpid
    }
})

