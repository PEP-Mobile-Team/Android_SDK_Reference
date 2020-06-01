
    
    // 获取url中的参数
    function GetQueryString(name) {
        var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)","i");
        var r = window.location.search.substr(1).match(reg);
        if (r!=null){
            return (r[2]);
        }
        return null;
    }
    // 客户端交互
    function CallFunc(fnname,param){
        var u = navigator.userAgent;
        var isAndroid = u.indexOf('Android') > -1 || u.indexOf('Adr') > -1; //android终端
        var isiOS = !!u.match(/\(i[^;]+;( U;)? CPU.+Mac OS X/); //ios终端
        var fn;
        if(isAndroid){
            fn = window.android.callClient(fnname,param)
        }else if(isiOS){
            fn = JSCallObjectiveC(fnname,param)
        }else{
            var evaSvr = InitEvaSvr(1, 1);
            fn = evaSvr.callClient(fnname,param);
        }
        return fn
    }

    $('.close-btn').on('click',function(){Close()})
    // 关闭
    function Close(){
        CallFunc('close',"")
    }
    function callHtml(fnname,data){
        eval(fnname+"(data)")
    }

var responseCallbacks = {};
var uniqueId = 1;

//function JSCallObjectiveC(functionName, params, responseCallback) {
function JSCallObjectiveC(functionName, params) {
//    alert(params);
    if (params) {
        var dict = JSON.parse(params);
            var callbackId = '';
            if (dict.callback) {
                callbackId = 'cb_'+(uniqueId++)+'_'+new Date().getTime();
                responseCallbacks[callbackId] = dict.callback;
                dict['callId'] = callbackId;
            }
        window.webkit.messageHandlers.jsCallOC.postMessage({
            functionName:functionName,
            data:dict
        });
    } else {
        window.webkit.messageHandlers.jsCallOC.postMessage({
                functionName:functionName,
                data:null
        });
    }
}

function objectiveCCallJS(params) {
    var message = JSON.parse(params);
    var callback = responseCallbacks[message.callId];
    callback(message.responseData);
}