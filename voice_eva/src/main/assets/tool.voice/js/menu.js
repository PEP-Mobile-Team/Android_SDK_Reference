var bookList
var bookFile
var width = $(window).width()
var bid = GetQueryString('bid') || null
var forward = GetQueryString('forward') || null
var uid = null
var userData
var dataUrl = ''
var onlyBookId
var onlyUnitId
var userId
var accessKey
// $(function(){


    CallFunc('getUserConfig','')
    function callHtml(fnname,data){
        eval(fnname+"(data)")
    }
    // GetUserConfig('{}')
    function GetUserConfig(data) {
        if(forward == 'home'){
            var data = JSON.parse(data)
            console.log(data)
            if(data.dataUrl){
                dataUrl = data.dataUrl
            }
            if(data.userId){
                userId= data.userId
            }
            if(JSON.parse(sessionStorage.getItem('menuData'))){
               // console.log("存在session")
                var menuData = JSON.parse(sessionStorage.getItem('menuData'));
                data = menuData;
                menuData = "";
                sessionStorage.setItem('menuData',JSON.stringify(menuData));
            }
            var stick;
            if(data.bookId){
                onlyBookId = data.bookId
            }
            if(data.stick){
                stick = data.stick
                //console.log(stick)
            }
            if(data.unitId){
                onlyUnitId = data.unitId
            }
            $.getJSON(dataUrl+ 'data/booklist.json',function(data){
                bookList = data.bookList
                init(1)
            })

        }else{
            var data = JSON.parse(data)
            console.log(data)
            if(data.dataUrl){
                dataUrl = data.dataUrl
            }
            if(data.userId){
                userId= data.userId
            }
            if(JSON.parse(sessionStorage.getItem('menuData'))){
               // console.log("存在session")
                var menuData = JSON.parse(sessionStorage.getItem('menuData'));
                data = menuData;
                menuData = "";
                sessionStorage.setItem('menuData',JSON.stringify(menuData));
            }
            console.log(data)
            var stick;
            if(data.bookId){
                onlyBookId = data.bookId
            }
            if(data.stick){
                stick = data.stick
                //console.log(stick)
            }
            stick = stick + '';
            var list = stick.split('');
            if(list.length == 1){
                list.splice(0,0,0);
            }
            var showReturn = parseInt(list[0]);
            stick = parseInt(list[1])
            if(showReturn == 1){
                $('.return').css('display','none');
            }
            if(data.unitId){
                onlyUnitId = data.unitId
            }
            if(data.bookId && data.unitId && data.practiceId && data.practiceId!= 'EvaDefaultPractice'){
                var u = navigator.userAgent;
                var isAndroid = u.indexOf('Android') > -1 || u.indexOf('Adr') > -1; //android终端
                var isiOS = !!u.match(/\(i[^;]+;( U;)? CPU.+Mac OS X/); //ios终端
                // if(isAndroid || isiOS){
                //     location.href = './prepare.html?bid='+data.bookId+'&uid='+data.unitId+'&pid='+data.practiceId+'&from=1'
                // }else{
                //     location.href = './prepare.html?bid='+data.bookId+'&uid='+data.unitId+'&pid='+data.practiceId
                // }

                // if(width < 600){
                //     // 竖版
                //     location.href = './prepare.html?bid='+data.bookId+'&uid='+data.unitId+'&pid='+data.practiceId+'&from=1'
                // }else{
                //     location.href = './prepare.html?bid='+data.bookId+'&uid='+data.unitId+'&pid='+data.practiceId
                // }
                $.getJSON(dataUrl+ 'data/booklist.json',function(data){
                    bookList = data.bookList
                    init(1)
                })

            }
            if(data.stick == 0){
                if(JSON.parse(sessionStorage.getItem('stick'))){
                    console.log(JSON.parse(sessionStorage.getItem('stick')))
                    stick = JSON.parse(sessionStorage.getItem('stick'));
                }else{
                    stick = 0;
                }
            }else if (data.stick == 1 || data.stick == 2) {
                sessionStorage.setItem('stick',JSON.stringify(data.stick))
            }
            if(data.bookId && !data.unitId && stick){
                if(stick == 1){
                    $.getJSON(dataUrl+ 'data/booklist.json',function(data){
                        bookList = data.bookList;
                        init(1)
                        return ;
                    })
                }else if(stick == 2){
                    $.getJSON(dataUrl+ 'data/booklist.json',function(data){
                        bookList = data.bookList;
                        init(2)
                        return ;
                    })
                }
            }
            if(!stick || stick == 0 || !data.bookId){
                $.getJSON(dataUrl+ 'data/booklist.json',function(data){
                    bookList = data.bookList
                    init(1)
                })
            }

            if(data.bookId && data.unitId && (!data.practiceId || data.practiceId.indexOf('EvaDefaultPractice') > -1)){
                if(!stick || stick == 0 || stick == 1){
                    $.getJSON(dataUrl+ 'data/booklist.json',function(data){
                        bookList = data.bookList
                        init(3)
                    })
                }else if(stick == 2){
                    $.getJSON(dataUrl+ 'data/booklist.json',function(data){
                        bookList = data.bookList
                        init(4)
                    })
                }

            }
        }

    }

    function TurnTo(data){
        console.log("turnto")
        console.log(data)
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
            //     location.href = './horizontal-menu.html?bid='+data.bookId
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
            console.log(JSON.parse(sessionStorage.getItem('menuData')))
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



    function init(mark){
        var leftHtml = ""
        var bookInfo
        var index
        if(mark == 1){
            for(var i = 0; i < bookList.length; i++){
                var item = bookList[i]
                var str = "<li data-id='"+item.bookId+"' data-file='"+item.file+"'>"+item.bookName+"</li>"
                leftHtml += str
                if(bid && item.bookId == bid){
                    bookInfo = item
                    index = i
                }
            }
            if(!bookInfo && !index){
                bookInfo = bookList[0]
                index = 0
            }
            bid = bookInfo.bookId
            bookFile = bookInfo.file.split('/')[0]
            getUnitInfo(bookInfo.file,true)
            if(width < 600){
                // 竖版
                $('.v-menu-title span').eq(0).html(bookInfo.bookName)
                $('.v-menu-book ul').html(leftHtml)
                $('.v-menu-book ul li').eq(index).addClass('active')
                $('.v-menu-book ul li').each(function(){
                    onBookClick($(this),'v')
                })
            }else{
                $('.menu-title span').html(bookInfo.bookName)
                $('.pull-left ul').html(leftHtml) 
                $('.pull-left ul li').eq(index).addClass('active')
                $('.pull-left ul li').each(function(){
                    onBookClick($(this),'')
                })
            }
        }else if(mark == 2){
            for(var i = 0; i < bookList.length; i++){
                console.log(bookList)
                var item = bookList[i]
                if(item.bookId == onlyBookId){
                    var str = "<li data-id='"+item.bookId+"' data-file='"+item.file+"'>"+item.bookName+"</li>"
                    leftHtml += str
                    if(bid && item.bookId == bid){
                        bookInfo = item
                        index = i
                    }
                }
                console.log(leftHtml)
            }
            if(!bookInfo && !index){
                bookInfo = bookList[0]
                index = 0
            }
            bid = bookInfo.bookId
            bookFile = bookInfo.file.split('/')[0]
            getUnitInfo(bookInfo.file,true)
            if(width < 600){
                // 竖版
                $('.v-menu-title span').eq(0).html(bookInfo.bookName)
                $('.v-menu-book ul').html(leftHtml)
                $('.v-menu-book ul li').eq(0).addClass('active')
                $('.v-menu-book ul li').each(function(){
                    onBookClick($(this),'v')
                })
            }else{
                $('.menu-title span').html(bookInfo.bookName)
                $('.pull-left ul').html(leftHtml) 
                $('.pull-left ul li').eq(0).addClass('active')
                $('.pull-left ul li').each(function(){
                    onBookClick($(this),'')
                })
            } 

        }else if(mark == 3) {
           for(var i = 0; i < bookList.length; i++){
                var item = bookList[i]
                var str = "<li data-id='"+item.bookId+"' data-file='"+item.file+"'>"+item.bookName+"</li>"
                leftHtml += str
                if(bid && item.bookId == bid){
                    bookInfo = item
                    index = i
                }
            }
            if(!bookInfo && !index){
                bookInfo = bookList[0]
                index = 0
            }
            bid = bookInfo.bookId
            bookFile = bookInfo.file.split('/')[0]
            getUnitInfo(bookInfo.file,false)
            if(width < 600){
                // 竖版
                $('.v-menu-title span').eq(0).html(bookInfo.bookName)
                $('.v-menu-book ul').html(leftHtml)
                $('.v-menu-book ul li').eq(index).addClass('active')
                $('.v-menu-book ul li').each(function(){
                    onBookClick($(this),'v')
                })
            }else{
                $('.menu-title span').html(bookInfo.bookName)
                $('.pull-left ul').html(leftHtml) 
                $('.pull-left ul li').eq(index).addClass('active')
                $('.pull-left ul li').each(function(){
                    onBookClick($(this),'')
                })
            }
        }else if(mark == 4) {
            for(var i = 0; i < bookList.length; i++){
                var item = bookList[i]
                if(item.bookId == onlyBookId){
                    var str = "<li data-id='"+item.bookId+"' data-file='"+item.file+"'>"+item.bookName+"</li>"
                    leftHtml += str
                    if(bid && item.bookId == bid){
                        bookInfo = item
                        index = i
                    }
                }
            }
            if(!bookInfo && !index){
                bookInfo = bookList[0]
                index = 0
            }
            bid = bookInfo.bookId
            bookFile = bookInfo.file.split('/')[0]
            getUnitInfo(bookInfo.file,false)
            if(width < 600){
                // 竖版
                $('.v-menu-title span').eq(0).html(bookInfo.bookName)
                $('.v-menu-book ul').html(leftHtml)
                $('.v-menu-book ul li').eq(0).addClass('active')
                $('.v-menu-book ul li').each(function(){
                    onBookClick($(this),'v')
                })
            }else{
                $('.menu-title span').html(bookInfo.bookName)
                $('.pull-left ul').html(leftHtml) 
                $('.pull-left ul li').eq(0).addClass('active')
                $('.pull-left ul li').each(function(){
                    onBookClick($(this),'')
                })
            }        
        }
    };
    // 获取教材单元
    function getUnitInfo(file,flag){
        var unitList
        $.getJSON(dataUrl+'data/'+file,function(data){
            unitList = data.unitList
            var unitHtml = ""
            for(var j = 0; j < unitList.length; j++){
                var unit
                var pageArr
                var pageHtml = ""
                if(flag == false){
                    if(onlyUnitId == unitList[j].unitId){
                        var unit = unitList[j]
                        var pageArr = unitList[j].practiceList;
                        for(var p = 0; p < pageArr.length; p++){
                            if(width < 600){
                                var str = "<li data-id='"+pageArr[p].pageGid+"' data-type='"+pageArr[p].type+"'>"+pageArr[p].pageName+"<span class='pull-right'>></span></li>"
                            }else{
                                var str = "<li data-id='"+pageArr[p].pageGid+"' data-type='"+pageArr[p].type+"'>"+pageArr[p].pageName+"</li>"
                            }   
                            pageHtml += str
                        }
                        if(width < 600){
                            var unitStr = "<div class='v-unit'><div class='v-unit-title' data-id='"+unit.unitId+"'>"+unit.unitName+"</div><div class='v-unit-list'><ul>"+pageHtml+"</ul></div></div>"
                        }else{
                            var unitStr = "<div class='unit'><div class='unit-title' data-id='"+unit.unitId+"'>"+unit.unitName+"</div><div class='unit-list'><ul>"+pageHtml+"</ul></div></div>"
                        }
                        unitHtml += unitStr
                    }
                }else if(flag == true){
                    var unit = unitList[j]
                    var pageArr = unitList[j].practiceList
                    for(var p = 0; p < pageArr.length; p++){
                        if(width < 600){
                            var str = "<li data-id='"+pageArr[p].pageGid+"' data-type='"+pageArr[p].type+"'>"+pageArr[p].pageName+"<span class='pull-right'>></span></li>"
                        }else{
                            var str = "<li data-id='"+pageArr[p].pageGid+"' data-type='"+pageArr[p].type+"'>"+pageArr[p].pageName+"</li>"
                        }   
                        pageHtml += str
                    }
                    if(width < 600){
                        var unitStr = "<div class='v-unit'><div class='v-unit-title' data-id='"+unit.unitId+"'>"+unit.unitName+"</div><div class='v-unit-list'><ul>"+pageHtml+"</ul></div></div>"
                    }else{
                        var unitStr = "<div class='unit'><div class='unit-title' data-id='"+unit.unitId+"'>"+unit.unitName+"</div><div class='unit-list'><ul>"+pageHtml+"</ul></div></div>"
                    }
                    unitHtml += unitStr
                }

            }
            if(width < 600){
                $('.v-menu-unit').html(unitHtml) 
                onPracticeClick($('.v-unit-list li'))
            }else{
                $('.pull-right-unit').html(unitHtml) 
                onPracticeClick($('.unit-list li'))
            }
        })
    };

    function onBookClick(dom,type){
        dom.on('click',function(){
            dom.addClass('active').siblings('li').removeClass('active')
            if(type == 'v'){
                $('.v-menu-title span').eq(0).html($(this).html())
                $('.v-menu-book').hide()
                $('.v-cover').hide()
            }else{
                $('.menu-title span').html(dom.html())
            }
            var id = dom.attr('data-id')
            var file = dom.attr('data-file')
            bookFile = file.split('/')[0]
            bid = id
            getUnitInfo(file,true)
        })
    }

    function onPracticeClick(dom){
        // console.log(dom)
        dom.each(function(){
            $(this).on('click',function(){
                var id = $(this).attr('data-id')
                var name = $(this).text()
                var type = $(this).attr('data-type')
                uid = $(this).parent().parent().siblings('.unit-title').attr('data-id')
                if(!uid){
                    uid = $(this).parent().parent().siblings('.v-unit-title').attr('data-id')
                }
                onClickToPrepare(id,name,type)
            })
        })
    }

    // 跳转准备
    function onClickToPrepare(id,name,type){
        console.log('bid='+bid+'&uid='+uid+'&pid='+id);
        location.href = './prepare.html?bid='+bid+'&uid='+uid+'&pid='+id+'&forward=menu'
    }

    // 竖版标题点击
    $('.v-menu-title').on('click',function(){
        if($('.v-menu-book').css('display') == 'block'){
            $('.v-menu-book').hide()
            $('.v-cover').hide()
        }else{
            $('.v-menu-book').show()
            $('.v-cover').show()
        }
    })
    $('.v-cover').on('click',function(){
        $('.v-menu-book').hide()
        $('.v-cover').hide()
    })

    // 返回
    $('.return').on('click',function(){
        var back = 'menu';//从菜单页调回，不需要再次执行GetUserConfig函数
        sessionStorage.setItem('back',JSON.stringify(back));
        window.location.href="./index.html"
    }) 
// })
// function callHtml(fnname,data){
//     eval(fnname+"(data)")
// }

// function PageRotate(data){
//     var type = JSON.parse(data).type
//     if(type == 'ver'){
//         // 旋转为竖
//         location.href = "./vertical-menu.html?bid="+bid
//     }
//     if(type == 'hor'){
//         // 旋转为横
//         location.href = "./horizontal-menu.html?bid="+bid
//     }
// }