var bid = GetQueryString('bid')
var uid = GetQueryString('uid')
var pid = GetQueryString('pid')
var from = GetQueryString('from')
var width = $(window).width()
var title
var bookList
var pageName
var practice
var userData
var dataUrl = ''
var forward = GetQueryString('forward');
CallFunc('getUserConfig','')
function callHtml(fnname, data) {
    eval(fnname + "(data)")
}
// GetUserConfig('{}')
function GetUserConfig(data) {
    var data = JSON.parse(data)
    console.log(data)
    var stick = data.stick;
    stick = stick + '';
    var list = stick.split('');
    if(list.length == 1){
        list.splice(0,0,0);
    }
    var showReturn = parseInt(list[0]);
    if(showReturn == 1){
        $('.return').css('display','none');
    }
    if(data.dataUrl){
        dataUrl = data.dataUrl
    }
    if(forward != 'menu'){
        if(JSON.parse(sessionStorage.getItem('menuData'))){
           // console.log("存在session")
            var menuData = JSON.parse(sessionStorage.getItem('menuData'));
            data = menuData;
            menuData = "";
            sessionStorage.setItem('menuData',JSON.stringify(menuData));
        }
        if (data.bookId && data.unitId && data.practiceId && data.practiceId!= 'EvaDefaultPractice') {
            bid = data.bookId
            uid = data.unitId
            pid = data.practiceId
        }
    }
    if(data.userId){
        userData = data
    }
    getJSON()
}
function TurnTo(data){
    console.log(JSON.parse(sessionStorage.getItem('menuData')))
    var data = JSON.parse(data)
    console.log(data)
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


function getJSON() {
    $.getJSON(dataUrl + 'data/booklist.json', function(data) {
        bookList = data.bookList
        getPracticeJson()
    })
}

function getPracticeJson() {
        //     console.log('bid=' + bid)
        // console.log('uid=' + uid)
        // console.log('pid=' + pid)
    for (var i = 0; i < bookList.length; i++) {
        if (bookList[i].bookId == bid) {
            var uFile = bookList[i].file
            $.getJSON(dataUrl + 'data/' + uFile, function(data) {
                unitList = data.unitList
                for (var j = 0; j < unitList.length; j++) {
                    if (unitList[j].unitId == uid) {
                        var pFile = uFile.split('/')[0]
                        var page = unitList[j].practiceList
                        for (var p = 0; p < page.length; p++) {
                            if (page[p].pageGid == pid) {
                                pageName = page[p].pageName
                                title = pageName
                                $('.title').text(title)
                                var pageUrl = dataUrl + 'data/' + pFile + '/' + page[p].content
                                $.getJSON(pageUrl, function(data) {
                                    practice = data.practice
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

function init() {
    var mode = practice.type == "对话" ? "rolePlay" : "senRead"
    var data = JSON.stringify({ mode: mode, name: pageName, practiceId: pid })
    CallFunc('startPractice', data)
    if (practice.type == '对话') {
        $('.desc').html('这是一个分角色练习，你可以选择其中一个或多个角色，朗读屏幕上与该角色一致的文本，朗读完成将自动跳转到下一句，你可以选择暂停或重新开始。')
        var html = ""
        if (practice.roleList) {
            for (var i = 0; i < practice.roleList.length; i++) {
                if (practice.roleList[i].indexOf('/') == -1) {
                    var str = '<div class="checkWrap"><div class="check"><div class="checked"><img src="./images/icon_checked.png" alt=""></div></div><div class="label-txt">' + practice.roleList[i] + '</div></div>'
                    html += str
                }
            }
        }
        //console.log(html)
        $('.role').html(html)
            // if($('.role .checkWrap').length > 2){
            //     $('.role').css('')
            // }
        $('.role .checkWrap').each(function() {
            $(this).on('click', function() {
                $(this).find('.check').toggleClass('active')
            })
        })
    }
    if (practice.type == '篇章') {
        $('.desc').text('这是一个逐句朗读，你需要按顺序朗读屏幕上的文本，朗读完成后将自动跳转到下一句，你可以选择暂停或重新开始。')
    }
}

$('.go').on('click', function() {
    var checkRole = []
        // 判断是否选中角色
    if (practice.type == "对话") {
        $('.role .checkWrap').each(function() {
            if ($(this).find('.check').hasClass('active')) {
                checkRole.push($(this).find('.label-txt').html())
            }
        })
        if (checkRole.length < 1) {
            alert('请至少选择一个角色！')
            return
        } else {
            localStorage.setItem('checkrole', JSON.stringify(checkRole))
        }
    }
    window.location.href = "./practice.html?bid=" + bid + '&uid=' + uid + "&pid=" + pid
})

// 返回
$('.return').on('click',function(){
    from = JSON.parse(sessionStorage.getItem('from'));
    console.log('from='+from)
    // if(from == 1){
    //     CallFunc('close',"")
    // }else{
        if(width < 600){
            // 竖版
            window.location.href="./vertical-menu.html?bid="+bid
        }else{
            window.location.href="./horizontal-menu.html?bid="+bid
        }
    // }
})